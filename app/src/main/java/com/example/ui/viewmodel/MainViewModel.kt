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
    val isLoading: Boolean = false,
    val syncMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MaintenanceRepository

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MaintenanceRepository(db)

        val collectionsFlow = repository.collectionRecords.map { it.firstOrNull() }
        val expensesFlow = repository.expenseRecords
        val contributionsFlow = repository.yearlyContributions
        val categoriesFlow = repository.yearlyExpenseCategories
        val worksFlow = repository.majorWorks
        val ownersFlow = repository.ownerContacts
        val servicesFlow = repository.serviceContacts
        val configFlow = repository.googleSheetConfig.map { config ->
            val c = config ?: GoogleSheetConfig()
            if (c.userEmail.isBlank()) c.copy(isLoggedIn = false) else c
        }

        val financialFlow = combine(
            collectionsFlow,
            expensesFlow,
            contributionsFlow,
            categoriesFlow,
            worksFlow
        ) { col, exp, contrib, cat, works ->
            FinancialData(col, exp, contrib, cat, works)
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

        uiState = combine(
            baseDataFlow,
            _searchQuery,
            _selectedCategory,
            _syncMessage,
            _isLoading
        ) { state, query, category, msg, loading ->
            val filtered = state.expenses.filter { record ->
                val matchesCat = (category == "All" || record.category.equals(category, ignoreCase = true))
                val matchesQuery = query.isEmpty() ||
                        record.particulars.contains(query, ignoreCase = true) ||
                        record.vendorPayee.contains(query, ignoreCase = true) ||
                        record.remarks.contains(query, ignoreCase = true)
                matchesCat && matchesQuery
            }
            state.copy(
                searchQuery = query,
                selectedCategoryFilter = category,
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

    fun triggerSync() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.syncGoogleSheet(uiState.value.config)
            _isLoading.value = false
            _syncMessage.value = result.getOrDefault("Synced with Google Sheet")
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
                isLoggedIn = userEmail.isNotEmpty(),
                lastSyncTime = System.currentTimeMillis()
            )
            repository.updateConfig(updated)
            _syncMessage.value = "Updated GCP Service Account & Google Sheet details"
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
                _syncMessage.value = "Signed out"
            } else {
                val updated = uiState.value.config.copy(
                    userEmail = userEmail,
                    webClientId = if (webClientId.isNotEmpty()) webClientId else uiState.value.config.webClientId,
                    isLoggedIn = true,
                    lastSyncTime = System.currentTimeMillis()
                )
                repository.updateConfig(updated)
                _syncMessage.value = "Logged in as $userEmail"
            }
        }
    }

    fun logout() {
        loginWithGoogle("")
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
}
