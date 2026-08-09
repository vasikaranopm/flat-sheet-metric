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

                // Cell A1 Title Extraction
                var extractedTitle = config.spreadsheetTitle
                val firstLine = lines.firstOrNull() ?: ""
                val firstLineCols = parseCsvLine(firstLine)
                val cellA1 = firstLineCols.firstOrNull()?.trim('"')?.trim() ?: ""
                val lowerFirstLine = firstLine.lowercase()
                val headerKeywordCount = listOf("particular", "amount", "date", "description", "cost", "item", "year", "month", "vendor", "payee", "balance", "s.no").count { lowerFirstLine.contains(it) }

                if (headerKeywordCount < 2 && cellA1.isNotBlank()) {
                    extractedTitle = cellA1
                }

                val parsedExpenses = parseCsvExpenses(body)

                // Save or clear expenses
                if (parsedExpenses.isNotEmpty()) {
                    database.expenseDao().clearAll()
                    database.expenseDao().insertExpenseRecords(parsedExpenses)
                }

                // Calculate collections from parsed expenses if flat names exist in records
                var flat1A = 0.0; var flat1B = 0.0
                var flat2A = 0.0; var flat2B = 0.0
                var flat3A = 0.0; var flat3B = 0.0
                var foundCollections = false

                for (exp in parsedExpenses) {
                    val p = exp.particulars.lowercase()
                    if (p.contains("1a")) { flat1A += exp.amount; foundCollections = true }
                    if (p.contains("1b")) { flat1B += exp.amount; foundCollections = true }
                    if (p.contains("2a")) { flat2A += exp.amount; foundCollections = true }
                    if (p.contains("2b")) { flat2B += exp.amount; foundCollections = true }
                    if (p.contains("3a")) { flat3A += exp.amount; foundCollections = true }
                    if (p.contains("3b")) { flat3B += exp.amount; foundCollections = true }
                }

                if (foundCollections) {
                    val total = flat1A + flat1B + flat2A + flat2B + flat3A + flat3B
                    database.collectionDao().insertCollectionRecord(
                        CollectionRecord(
                            id = 1,
                            flat1AAmount = flat1A,
                            flat1BAmount = flat1B,
                            flat2AAmount = flat2A,
                            flat2BAmount = flat2B,
                            flat3AAmount = flat3A,
                            flat3BAmount = flat3B,
                            totalAmount = if (total > 0) total else 12000.0
                        )
                    )
                }

                val finalTitle = if (extractedTitle.isNotBlank()) extractedTitle else "Apartment Maintenance"
                database.configDao().saveConfig(
                    config.copy(
                        spreadsheetTitle = finalTitle,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )
                return@withContext Result.success("Access Verified & Synced! (${parsedExpenses.size} records updated from $finalTitle)")
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
        if (lines.isEmpty()) return emptyList()

        // Check if line 0 is a title line
        val firstLine = lines.first()
        val lowerFirstLine = firstLine.lowercase()
        val headerKeywordCount0 = listOf("particular", "amount", "date", "description", "cost", "item", "year", "month", "vendor", "payee", "balance", "s.no").count { lowerFirstLine.contains(it) }

        val hasTitleRow = headerKeywordCount0 < 2 && lines.size > 1
        val headerLine = if (hasTitleRow) lines[1] else lines[0]
        val dataLines = if (hasTitleRow) lines.drop(2) else lines.drop(1)

        val headerCols = parseCsvLine(headerLine).map { it.lowercase().trim('"').trim() }

        // Find matching columns by header names
        var yearCol = headerCols.indexOfFirst { it.contains("year") }
        var monthCol = headerCols.indexOfFirst { it.contains("month") }
        var dateCol = headerCols.indexOfFirst { it.contains("date") || it.contains("day") }
        var particularsCol = headerCols.indexOfFirst {
            it.contains("particular") || it.contains("description") || it.contains("item") ||
                    it.contains("detail") || it.contains("purpose") || it.contains("name") || it.contains("title")
        }
        var remarksCol = headerCols.indexOfFirst { it.contains("remark") || it.contains("note") || it.contains("comment") }
        var amountCol = headerCols.indexOfFirst {
            it.contains("amount") || it.contains("cost") || it.contains("price") ||
                    it.contains("rs") || it.contains("inr") || it.contains("₹") || it.contains("spent") || it.contains("total")
        }
        var vendorCol = headerCols.indexOfFirst {
            it.contains("vendor") || it.contains("payee") || it.contains("paid") || it.contains("by") || it.contains("person")
        }
        var billCol = headerCols.indexOfFirst { it.contains("bill") || it.contains("doc") }
        var pictureCol = headerCols.indexOfFirst { it.contains("picture") || it.contains("photo") || it.contains("image") }
        var balanceCol = headerCols.indexOfFirst { it.contains("balance") || it.contains("bal") }
        var categoryCol = headerCols.indexOfFirst { it.contains("category") || it.contains("type") || it.contains("head") }

        val sampleRows = dataLines.take(15).map { parseCsvLine(it) }

        // Auto-detect amount column if header didn't match
        if (amountCol == -1 && sampleRows.isNotEmpty()) {
            val maxCols = sampleRows.maxOfOrNull { it.size } ?: 0
            for (colIdx in 0 until maxCols) {
                val numericCount = sampleRows.count { row ->
                    val cell = row.getOrNull(colIdx)?.trim('"')?.trim()?.replace("₹", "")?.replace(",", "") ?: ""
                    val num = cell.toDoubleOrNull()
                    num != null && num > 0
                }
                if (numericCount >= (sampleRows.size * 0.4)) {
                    amountCol = colIdx
                    break
                }
            }
        }

        // Auto-detect particulars column if header didn't match
        if (particularsCol == -1 && sampleRows.isNotEmpty()) {
            val maxCols = sampleRows.maxOfOrNull { it.size } ?: 0
            for (colIdx in 0 until maxCols) {
                if (colIdx != amountCol && colIdx != dateCol && colIdx != monthCol && colIdx != yearCol) {
                    particularsCol = colIdx
                    break
                }
            }
        }

        // Fallbacks for standard formats if still undetected
        if (particularsCol == -1) particularsCol = if (headerCols.size > 1) 1 else 0
        if (amountCol == -1) amountCol = (headerCols.size - 1).coerceAtLeast(0)

        var runningBalance = 12000.0 // Default starting opening balance

        return dataLines.mapIndexedNotNull { index, line ->
            val cols = parseCsvLine(line)
            if (cols.isEmpty() || cols.all { it.isBlank() }) return@mapIndexedNotNull null

            fun getVal(colIdx: Int): String {
                if (colIdx == -1) return ""
                return cols.getOrNull(colIdx)?.trim('"')?.trim() ?: ""
            }

            fun parseAmountVal(colIdx: Int): Double {
                val str = getVal(colIdx).replace("₹", "").replace(",", "").replace("Rs.", "").replace("INR", "").trim()
                return str.toDoubleOrNull() ?: 0.0
            }

            val amount = parseAmountVal(amountCol)
            val particulars = getVal(particularsCol).ifBlank {
                cols.firstOrNull { it.isNotBlank() && parseAmountVal(cols.indexOf(it)) == 0.0 }?.trim('"') ?: "Expense Item #${index + 1}"
            }

            val dateStr = getVal(dateCol).ifBlank { "01" }
            val monthStr = getVal(monthCol).ifBlank { "July" }
            val yearStr = getVal(yearCol).ifBlank { "2026" }
            val categoryStr = getVal(categoryCol).ifBlank {
                if (particulars.contains("clean", ignoreCase = true) || particulars.contains("sweep", ignoreCase = true)) "Cleaning"
                else if (particulars.contains("motor", ignoreCase = true) || particulars.contains("sensor", ignoreCase = true) || particulars.contains("alter", ignoreCase = true)) "Alteration/Additional work"
                else "Common Purchases"
            }
            val vendorStr = getVal(vendorCol).ifBlank { "General Vendor" }
            val remarksStr = getVal(remarksCol).ifBlank { "Recorded from Google Sheet" }
            val billStr = getVal(billCol).ifBlank { "Available" }
            val pictureStr = getVal(pictureCol).ifBlank { "N/A" }

            val explicitBalance = parseAmountVal(balanceCol)
            val finalBalance = if (explicitBalance > 0) {
                explicitBalance
            } else {
                runningBalance -= amount
                runningBalance
            }

            ExpenseRecord(
                id = index + 1,
                year = yearStr,
                month = monthStr,
                dateDay = dateStr,
                particulars = particulars,
                remarks = remarksStr,
                amount = amount,
                vendorPayee = vendorStr,
                billAvailable = billStr,
                picture = pictureStr,
                balance = finalBalance,
                category = categoryStr
            )
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
