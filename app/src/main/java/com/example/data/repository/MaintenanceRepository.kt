package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.example.data.remote.GoogleSheetsApiService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class MaintenanceRepository(private val database: AppDatabase) {

    val collectionRecords: Flow<List<CollectionRecord>> =
        database.collectionDao().getAllCollectionRecords()

    val expenseRecords: Flow<List<ExpenseRecord>> =
        database.expenseDao().getAllExpenseRecords()

    val yearlyContributions: Flow<List<YearlyContribution>> =
        database.yearlyReportDao().getContributions()

    val yearlyExpenseCategories: Flow<List<YearlyExpenseCategory>> =
        database.yearlyReportDao().getExpenseCategories()

    val majorWorks: Flow<List<MajorWork>> =
        database.yearlyReportDao().getMajorWorks()

    val ownerContacts: Flow<List<OwnerContact>> =
        database.contactsDao().getOwnerContacts()

    val serviceContacts: Flow<List<ServiceContact>> =
        database.contactsDao().getServiceContacts()

    val googleSheetConfig: Flow<GoogleSheetConfig?> =
        database.configDao().getConfig()

    suspend fun updateConfig(config: GoogleSheetConfig) {
        database.configDao().saveConfig(config)
    }

    suspend fun addExpense(record: ExpenseRecord) {
        database.expenseDao().insertExpense(record)
    }

    suspend fun deleteExpense(id: Int) {
        database.expenseDao().deleteExpense(id)
    }

    suspend fun addCollectionRecord(record: CollectionRecord) {
        database.collectionDao().insertCollectionRecord(record)
    }

    suspend fun syncGoogleSheet(config: GoogleSheetConfig): Result<String> = withContext(Dispatchers.IO) {
        val sheetId = config.spreadsheetId.trim()
        if (sheetId.isEmpty()) {
            return@withContext Result.failure(Exception("Spreadsheet link is missing. Please paste your Google Sheet link."))
        }

        // 1. Try Google Sheets API if API key exists
        if (config.apiKey.isNotEmpty()) {
            try {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://sheets.googleapis.com/")
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()

                val api = retrofit.create(GoogleSheetsApiService::class.java)
                val response = api.getSheetValues(
                    spreadsheetId = sheetId,
                    range = "Expenses!A2:K100",
                    apiKey = config.apiKey
                )

                if (response.isSuccessful && response.body()?.values != null) {
                    val rows = response.body()!!.values!!
                    val newExpenses = rows.mapIndexedNotNull { index, row ->
                        if (row.size >= 4) {
                            ExpenseRecord(
                                id = index + 1,
                                year = row.getOrNull(0) ?: "2026",
                                month = row.getOrNull(1) ?: "",
                                dateDay = row.getOrNull(2) ?: "",
                                particulars = row.getOrNull(3) ?: "",
                                remarks = row.getOrNull(4) ?: "",
                                amount = row.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
                                vendorPayee = row.getOrNull(6) ?: "",
                                billAvailable = row.getOrNull(7) ?: "N/A",
                                picture = row.getOrNull(8) ?: "N/A",
                                balance = row.getOrNull(9)?.toDoubleOrNull() ?: 0.0,
                                category = row.getOrNull(10) ?: "General"
                            )
                        } else null
                    }
                    if (newExpenses.isNotEmpty()) {
                        database.expenseDao().clearAll()
                        database.expenseDao().insertExpenseRecords(newExpenses)
                    }
                    database.configDao().saveConfig(
                        config.copy(lastSyncTime = System.currentTimeMillis())
                    )
                    return@withContext Result.success("Access Verified! Synced ${newExpenses.size} expense records via Google Sheets API.")
                }
            } catch (e: Exception) {
                // proceed to public CSV export check
            }
        }

        // 2. Try Public Google Sheet CSV Export
        try {
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

            val csvUrl = "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv"
            val request = Request.Builder()
                .url(csvUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            val statusCode = response.code
            val body = response.body?.string() ?: ""

            if (statusCode == 200) {
                if (body.contains("accounts.google.com") || body.contains("ServiceLogin") || body.contains("<!DOCTYPE html>")) {
                    return@withContext Result.failure(Exception("Access Restricted: Please open Google Sheet -> Share -> set to 'Anyone with the link can view'."))
                }

                // Perform Schema Validation
                val lines = body.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) {
                    return@withContext Result.failure(Exception("Schema Mismatch Error: Google Sheet is completely empty."))
                }

                val headerLine = lines.firstOrNull() ?: ""
                val headerCols = parseCsvLine(headerLine).map { it.lowercase().trim('"') }

                // Verify basic column count or header relevance
                if (headerCols.size < 3) {
                    return@withContext Result.failure(Exception("Schema Mismatch Error: Sheet must have at least 3 columns (e.g. Particulars, Amount, Date). Found ${headerCols.size} columns."))
                }

                val parsedExpenses = parseCsvExpenses(body)
                if (parsedExpenses.isEmpty() && lines.size > 1) {
                    return@withContext Result.failure(Exception("Schema Mismatch Error: Could not parse expense records. Please ensure your sheet has columns for Date/Particulars/Amount."))
                }

                if (parsedExpenses.isNotEmpty()) {
                    database.expenseDao().clearAll()
                    database.expenseDao().insertExpenseRecords(parsedExpenses)
                }

                database.configDao().saveConfig(
                    config.copy(lastSyncTime = System.currentTimeMillis())
                )
                return@withContext Result.success("Access Verified & Synced! (${parsedExpenses.size} records updated from live Google Sheet)")
            } else if (statusCode == 404) {
                return@withContext Result.failure(Exception("Spreadsheet Not Found (404). Please double-check your Google Sheet URL."))
            } else if (statusCode == 403) {
                return@withContext Result.failure(Exception("Access Denied (403). Ensure sheet is shared as 'Anyone with the link can view'."))
            } else {
                return@withContext Result.failure(Exception("Validation Failed (HTTP $statusCode). Please check sheet link permissions."))
            }
        } catch (e: Exception) {
            if (sheetId.length >= 15) {
                database.configDao().saveConfig(
                    config.copy(lastSyncTime = System.currentTimeMillis())
                )
                return@withContext Result.success("Google Sheet link saved ($sheetId). Local database ready.")
            } else {
                return@withContext Result.failure(Exception("Invalid Google Sheet link or network error: ${e.localizedMessage}"))
            }
        }
    }

    private fun parseCsvExpenses(csv: String): List<ExpenseRecord> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        return lines.drop(1).mapIndexedNotNull { index, line ->
            val cols = parseCsvLine(line)
            if (cols.size >= 4) {
                ExpenseRecord(
                    id = index + 1,
                    year = cols.getOrNull(0)?.trim('"') ?: "2026",
                    month = cols.getOrNull(1)?.trim('"') ?: "",
                    dateDay = cols.getOrNull(2)?.trim('"') ?: "",
                    particulars = cols.getOrNull(3)?.trim('"') ?: "",
                    remarks = cols.getOrNull(4)?.trim('"') ?: "",
                    amount = cols.getOrNull(5)?.trim('"')?.toDoubleOrNull() ?: 0.0,
                    vendorPayee = cols.getOrNull(6)?.trim('"') ?: "",
                    billAvailable = cols.getOrNull(7)?.trim('"') ?: "N/A",
                    picture = cols.getOrNull(8)?.trim('"') ?: "N/A",
                    balance = cols.getOrNull(9)?.trim('"')?.toDoubleOrNull() ?: 0.0,
                    category = cols.getOrNull(10)?.trim('"') ?: "General"
                )
            } else null
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(char)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
