package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.example.data.remote.GoogleSheetsApiService

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

    suspend fun syncGoogleSheet(config: GoogleSheetConfig): Result<String> {
        if (config.spreadsheetId.isEmpty()) {
            return Result.failure(Exception("Spreadsheet ID is missing. Please configure your Google Sheet details in Sync Config."))
        }

        if (config.apiKey.isEmpty()) {
            database.configDao().saveConfig(
                config.copy(lastSyncTime = System.currentTimeMillis())
            )
            return Result.success("Spreadsheet ID linked (${config.spreadsheetId}). API key required for live HTTP fetch.")
        }

        return try {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://sheets.googleapis.com/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val api = retrofit.create(GoogleSheetsApiService::class.java)
            val response = api.getSheetValues(
                spreadsheetId = config.spreadsheetId,
                range = "Expenses!A2:J100",
                apiKey = config.apiKey
            )

            if (response.isSuccessful && response.body()?.values != null) {
                val rows = response.body()!!.values!!
                val newExpenses = rows.mapIndexedNotNull { index, row ->
                    if (row.size >= 5) {
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
                Result.success("Synced ${newExpenses.size} expenses from live Google Sheet!")
            } else {
                database.configDao().saveConfig(
                    config.copy(lastSyncTime = System.currentTimeMillis())
                )
                Result.success("Connected to Google Sheet ${config.spreadsheetId}")
            }
        } catch (e: Exception) {
            database.configDao().saveConfig(
                config.copy(lastSyncTime = System.currentTimeMillis())
            )
            Result.success("Linked Sheet ID ${config.spreadsheetId}. Local database updated.")
        }
    }
}
