package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.MaintenanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FinancialData(
    val collectionRecords: List<CollectionRecord>,
    val collectionRecord: CollectionRecord?,
    val expenses: List<ExpenseRecord>,
    val yearlyContributions: List<YearlyContribution>,
    val yearlyCategories: List<YearlyExpenseCategory>,
    val majorWorks: List<MajorWork>
)

data class DirectoryData(
    val ownerContacts: List<OwnerContact>,
    val serviceContacts: List<ServiceContact>,
    val config: GoogleSheetConfig
)

data class MainUiState(
    val collectionRecords: List<CollectionRecord> = emptyList(),
    val collectionRecord: CollectionRecord? = null,
    val expenses: List<ExpenseRecord> = emptyList(),
    val filteredExpenses: List<ExpenseRecord> = emptyList(),
    val yearlyContributions: List<YearlyContribution> = emptyList(),
    val yearlyCategories: List<YearlyExpenseCategory> = emptyList(),
    val majorWorks: List<MajorWork> = emptyList(),
    val ownerContacts: List<OwnerContact> = emptyList(),
    val serviceContacts: List<ServiceContact> = emptyList(),
    val config: GoogleSheetConfig = GoogleSheetConfig(),
    val searchQuery: String = "",
    val selectedCategoryFilter: String = "All",
    val selectedMonthFilter: String = "All Months",
    val isLoading: Boolean = false,
    val syncMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MaintenanceRepository

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _selectedMonth = MutableStateFlow("All Months")
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MaintenanceRepository(db)

        val collectionsFlow = repository.collectionRecords
        val expensesFlow = repository.expenseRecords
        val contributionsFlow = repository.yearlyContributions
        val categoriesFlow = repository.yearlyExpenseCategories
        val worksFlow = repository.majorWorks
        val ownersFlow = repository.ownerContacts
        val servicesFlow = repository.serviceContacts
        val configFlow = repository.googleSheetConfig.map { config ->
            val c = config ?: GoogleSheetConfig()
            val defaultSheetId = extractSpreadsheetId(getDefaultSheetLinkEnv())
            if (c.spreadsheetId.isBlank() && defaultSheetId.isNotBlank()) {
                c.copy(spreadsheetId = defaultSheetId)
            } else c
        }

        val financialFlow = combine(
            collectionsFlow,
            expensesFlow,
            contributionsFlow,
            categoriesFlow,
            worksFlow
        ) { colList, exp, contrib, cat, works ->
            // If contributions are empty in DB, compute from collection records for all flats
            val effectiveContribs = if (contrib.isNotEmpty()) {
                contrib
            } else {
                val f1a = colList.sumOf { it.flat1AAmount }
                val f1b = colList.sumOf { it.flat1BAmount }
                val f2a = colList.sumOf { it.flat2AAmount }
                val f2b = colList.sumOf { it.flat2BAmount }
                val f3a = colList.sumOf { it.flat3AAmount }
                val f3b = colList.sumOf { it.flat3BAmount }
                listOf(
                    YearlyContribution("1A", "M.Madhan Raj", f1a),
                    YearlyContribution("1B", "S.Vasikaran", f1b),
                    YearlyContribution("2A", "S. Hariprasad", f2a),
                    YearlyContribution("2B", "P.Seenivasan", f2b),
                    YearlyContribution("3A", "A. Venkatesh Kumar", f3a),
                    YearlyContribution("3B", "M.Mohan", f3b)
                )
            }
            FinancialData(colList, colList.firstOrNull(), exp, effectiveContribs, cat, works)
        }

        val directoryFlow = combine(
            ownersFlow,
            servicesFlow,
            configFlow
        ) { owners, services, cfg ->
            DirectoryData(owners, services, cfg)
        }

        val baseDataFlow = combine(financialFlow, directoryFlow) { fin, dir ->
            MainUiState(
                collectionRecords = fin.collectionRecords,
                collectionRecord = fin.collectionRecord,
                expenses = fin.expenses,
                filteredExpenses = fin.expenses,
                yearlyContributions = fin.yearlyContributions,
                yearlyCategories = fin.yearlyCategories,
                majorWorks = fin.majorWorks,
                ownerContacts = dir.ownerContacts,
                serviceContacts = dir.serviceContacts,
                config = dir.config
            )
        }

        data class FilterState(val query: String, val category: String, val month: String)
        val filterFlow = combine(_searchQuery, _selectedCategory, _selectedMonth) { q, c, m ->
            FilterState(q, c, m)
        }

        val statusFlow = combine(_syncMessage, _isLoading) { msg, loading ->
            msg to loading
        }

        uiState = combine(
            baseDataFlow,
            filterFlow,
            statusFlow
        ) { state, filter, status ->
            val (msg, loading) = status
            val filtered = state.expenses.filter { record ->
                val matchesCat = (filter.category == "All" || record.category.equals(filter.category, ignoreCase = true))
                val matchesMonth = (filter.month == "All Months" || record.month.equals(filter.month, ignoreCase = true))
                val matchesQuery = filter.query.isEmpty() ||
                        record.particulars.contains(filter.query, ignoreCase = true) ||
                        record.vendorPayee.contains(filter.query, ignoreCase = true) ||
                        record.remarks.contains(filter.query, ignoreCase = true) ||
                        record.month.contains(filter.query, ignoreCase = true)
                matchesCat && matchesMonth && matchesQuery
            }
            state.copy(
                searchQuery = filter.query,
                selectedCategoryFilter = filter.category,
                selectedMonthFilter = filter.month,
                filteredExpenses = filtered,
                syncMessage = msg,
                isLoading = loading
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState()
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterSelected(category: String) {
        _selectedCategory.value = category
    }

    fun onMonthFilterSelected(month: String) {
        _selectedMonth.value = month
    }

    fun triggerSync() {
        viewModelScope.launch {
            val currentConfig = repository.googleSheetConfig.firstOrNull() ?: uiState.value.config
            if (!currentConfig.isLoggedIn || currentConfig.userEmail.isBlank()) {
                return@launch
            }
            _isLoading.value = true
            val defaultSheetId = extractSpreadsheetId(getDefaultSheetLinkEnv())
            val effectiveSheetId = currentConfig.spreadsheetId.ifBlank { defaultSheetId }
            val effectiveConfig = currentConfig.copy(spreadsheetId = effectiveSheetId)
            _syncMessage.value = "Checking Google Sheet ($effectiveSheetId)..."
            val result = repository.syncGoogleSheet(effectiveConfig)
            _isLoading.value = false
            result.fold(
                onSuccess = { msg ->
                    _syncMessage.value = msg
                },
                onFailure = { err ->
                    _syncMessage.value = "Google Sheet Sync Reason: ${err.message ?: "Failed to sync"}"
                }
            )
        }
    }

    fun updateSheetUrl(newUrlOrId: String) {
        viewModelScope.launch {
            val currentConfig = repository.googleSheetConfig.firstOrNull() ?: uiState.value.config
            val extractedId = extractSpreadsheetId(newUrlOrId).trim()
            val updated = currentConfig.copy(spreadsheetId = extractedId)
            repository.updateConfig(updated)
            if (extractedId.isNotBlank()) {
                _isLoading.value = true
                _syncMessage.value = "Verifying updated Google Sheet ($extractedId)..."
                val result = repository.syncGoogleSheet(updated)
                _isLoading.value = false
                result.fold(
                    onSuccess = { msg ->
                        _syncMessage.value = msg
                    },
                    onFailure = { err ->
                        _syncMessage.value = "Google Sheet Validation Reason: ${err.message ?: "Failed to validate updated Google Sheet link"}"
                    }
                )
            } else {
                _syncMessage.value = "Google Sheet link removed"
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun updateGcpConfig(
        spreadsheetId: String,
        apiKey: String,
        gcpProject: String,
        serviceAccount: String,
        userEmail: String,
        webClientId: String = ""
    ) {
        viewModelScope.launch {
            val updated = uiState.value.config.copy(
                spreadsheetId = spreadsheetId,
                apiKey = apiKey,
                gcpProjectId = gcpProject,
                serviceAccountEmail = serviceAccount,
                userEmail = userEmail,
                webClientId = webClientId,
                isLoggedIn = userEmail.isNotEmpty()
            )
            repository.updateConfig(updated)
            // Immediately run validation and data loading
            _isLoading.value = true
            _syncMessage.value = "Validating Google Sheet connection..."
            val syncResult = repository.syncGoogleSheet(updated)
            _isLoading.value = false
            syncResult.fold(
                onSuccess = { msg ->
                    _syncMessage.value = msg
                },
                onFailure = { err ->
                    _syncMessage.value = "Google Sheet Validation Reason: ${err.message}"
                }
            )
        }
    }

    fun loginWithGoogle(userEmail: String, webClientId: String = "") {
        viewModelScope.launch {
            if (userEmail.isEmpty()) {
                val updated = uiState.value.config.copy(
                    userEmail = "",
                    isLoggedIn = false
                )
                repository.updateConfig(updated)
                _syncMessage.value = null
            } else {
                val currentConfig = repository.googleSheetConfig.firstOrNull() ?: uiState.value.config
                val defaultSheetId = extractSpreadsheetId(getDefaultSheetLinkEnv())
                val effectiveSheetId = currentConfig.spreadsheetId.ifBlank { defaultSheetId }
                
                val updated = currentConfig.copy(
                    userEmail = userEmail,
                    webClientId = if (webClientId.isNotEmpty()) webClientId else currentConfig.webClientId,
                    spreadsheetId = effectiveSheetId,
                    isLoggedIn = true,
                    lastSyncTime = System.currentTimeMillis()
                )
                repository.updateConfig(updated)
                
                if (effectiveSheetId.isNotBlank()) {
                    _isLoading.value = true
                    _syncMessage.value = "Checking Google Sheet data ($effectiveSheetId)..."
                    val result = repository.syncGoogleSheet(updated)
                    _isLoading.value = false
                    result.fold(
                        onSuccess = { msg ->
                            _syncMessage.value = msg
                        },
                        onFailure = { err ->
                            _syncMessage.value = "Google Sheet Sync Reason: ${err.message ?: "Failed to sync"}"
                        }
                    )
                } else {
                    _syncMessage.value = "Signed in as $userEmail (No Google Sheet configured)"
                }
            }
        }
    }

    fun logout(context: android.content.Context? = null) {
        viewModelScope.launch {
            val currentConfig = repository.googleSheetConfig.firstOrNull() ?: uiState.value.config
            val updated = currentConfig.copy(
                userEmail = "",
                isLoggedIn = false
            )
            repository.updateConfig(updated)
            _syncMessage.value = null
        }
        context?.let { ctx ->
            try {
                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                ).build()
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(ctx, gso).signOut()
            } catch (_: Exception) {}
        }
    }

    fun addExpense(
        particulars: String,
        amount: Double,
        category: String,
        vendorPayee: String,
        dateDay: String = "",
        month: String = "August",
        year: String = "2026",
        remarks: String = "",
        billAvailable: String = "N/A",
        picture: String = "N/A"
    ) {
        viewModelScope.launch {
            val record = ExpenseRecord(
                year = year,
                month = month,
                dateDay = dateDay,
                particulars = particulars,
                remarks = remarks,
                amount = amount,
                vendorPayee = vendorPayee,
                billAvailable = billAvailable,
                picture = picture,
                balance = 0.0,
                category = category
            )
            repository.addExpense(record)
            _syncMessage.value = "Expense saved: $particulars (₹$amount)"
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteExpense(id)
            _syncMessage.value = "Expense deleted"
        }
    }

    fun addCollectionRecord(
        year: String,
        month: String,
        particulars: String,
        remarks: String,
        flat1A: Double,
        flat1B: Double,
        flat2A: Double,
        flat2B: Double,
        flat3A: Double,
        flat3B: Double
    ) {
        viewModelScope.launch {
            val total = flat1A + flat1B + flat2A + flat2B + flat3A + flat3B
            val record = CollectionRecord(
                year = year,
                month = month,
                particulars = particulars,
                remarks = remarks,
                flat1AAmount = flat1A,
                flat1BAmount = flat1B,
                flat2AAmount = flat2A,
                flat2BAmount = flat2B,
                flat3AAmount = flat3A,
                flat3BAmount = flat3B,
                totalAmount = total
            )
            repository.addCollectionRecord(record)
            _syncMessage.value = "Collection recorded for $month $year (Total: ₹$total)"
        }
    }

    fun updateCollectionRecord(record: CollectionRecord) {
        viewModelScope.launch {
            repository.updateCollectionRecord(record)
            _syncMessage.value = "Collection updated for ${record.month} ${record.year} (Total: ₹${record.totalAmount.toInt()})"
        }
    }

    fun deleteCollectionRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteCollectionRecord(id)
            _syncMessage.value = "Collection record removed"
        }
    }
}
