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

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.expenseDao().clearAll()
        database.yearlyReportDao().clearContributions()
        database.yearlyReportDao().clearCategories()
        database.yearlyReportDao().clearMajorWorks()
        database.contactsDao().clearOwners()
        database.contactsDao().clearServices()
        database.collectionDao().clearAll()
    }

    suspend fun syncGoogleSheet(config: GoogleSheetConfig): Result<String> = withContext(Dispatchers.IO) {
        val rawInput = config.spreadsheetId.trim()
        val sheetId = extractSpreadsheetId(rawInput)
        val explicitGid = extractGid(rawInput)

        if (sheetId.isEmpty()) {
            return@withContext Result.failure(Exception("No Google Sheet ID or URL configured. Please enter your Google Sheet link."))
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
                        database.configDao().saveConfig(
                            config.copy(lastSyncTime = System.currentTimeMillis())
                        )
                        return@withContext Result.success("Access Verified! Synced ${newExpenses.size} expense records via Google Sheets API.")
                    }
                }
            } catch (e: Exception) {
                // proceed to public CSV export check
            }
        }

        // 2. Try Public Google Sheet CSV Export with detailed diagnostics
        try {
            val urlsToTry = mutableListOf<String>()
            if (explicitGid != null) {
                urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&gid=$explicitGid")
            }
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv")
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=Expenses")
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=Expense")
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=July")
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=August")
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=Maintenance")
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=Sheet1")
            urlsToTry.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=Data")

            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()

            var allExpenses = mutableListOf<ExpenseRecord>()
            var extractedTitle = config.spreadsheetTitle
            var successfulFetch = false
            var diagnosedReason: String? = null

            for (targetUrl in urlsToTry.distinct()) {
                try {
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                        .build()
                    val response = client.newCall(request).execute()
                    val statusCode = response.code
                    val body = response.body?.string() ?: ""

                    if (statusCode == 200) {
                        if (body.contains("accounts.google.com") || body.contains("ServiceLogin") || body.contains("<!DOCTYPE html>") || body.contains("<html>")) {
                            diagnosedReason = "Access Restricted: Google Sheet requires sign-in. To allow the app to read it, open your Google Sheet -> Click 'Share' (top right) -> Under 'General access', change from 'Restricted' to 'Anyone with the link' (Viewer role)."
                            continue
                        }
                        successfulFetch = true
                        val parsed = parseCsvExpenses(body)
                        if (parsed.isNotEmpty()) {
                            allExpenses.addAll(parsed)
                            val firstLine = body.lines().firstOrNull() ?: ""
                            val cellA1 = parseCsvLine(firstLine).firstOrNull()?.trim('"')?.trim() ?: ""
                            if (cellA1.isNotBlank() && cellA1.length in 3..50 && !cellA1.contains("date", ignoreCase = true) && !cellA1.contains("particular", ignoreCase = true)) {
                                extractedTitle = cellA1
                            }
                            if (targetUrl.contains("gid=") || targetUrl.contains("sheet=Expenses")) {
                                break
                            }
                        }
                    } else if (statusCode == 404) {
                        diagnosedReason = "Spreadsheet Not Found (HTTP 404): The Google Sheet ID '$sheetId' does not exist or URL is invalid. Please check the URL."
                    } else if (statusCode == 403) {
                        diagnosedReason = "Access Denied (HTTP 403): Permissions are restricted. In Google Sheets, tap Share -> General Access -> 'Anyone with the link can view'."
                    } else if (statusCode == 401) {
                        diagnosedReason = "Unauthorized (HTTP 401): Google Sheet is private and requires viewer access for link sharing."
                    }
                } catch (e: Exception) {
                    if (diagnosedReason == null) {
                        diagnosedReason = "Network Connection Issue: ${e.localizedMessage ?: "Could not connect to Google Sheets servers"}"
                    }
                }
            }

            if (allExpenses.isNotEmpty()) {
                val distinctExpenses = allExpenses.distinctBy { "${it.month}_${it.dateDay}_${it.particulars}_${it.amount}_${it.vendorPayee}" }
                    .mapIndexed { index, record -> record.copy(id = index + 1) }

                database.expenseDao().clearAll()
                database.expenseDao().insertExpenseRecords(distinctExpenses)

                // 1. Dynamic Yearly Categories
                val categoriesMap = distinctExpenses.groupBy { it.category.ifBlank { "General" } }
                val categoryRecords = categoriesMap.map { (catName, list) ->
                    YearlyExpenseCategory(
                        category = catName,
                        amount2026 = list.sumOf { it.amount }
                    )
                }
                database.yearlyReportDao().clearCategories()
                database.yearlyReportDao().insertExpenseCategories(categoryRecords)

                // 2. Dynamic Major Capital Works
                val majorKeywords = listOf("sensor", "motor", "alteration", "repair", "paint", "replace", "plumbing", "electrical", "tank", "clean", "work", "purchase", "installation", "service", "capital", "upgrade")
                val majorWorkRecords = distinctExpenses.filter { exp ->
                    exp.amount >= 1000 || majorKeywords.any { exp.particulars.contains(it, ignoreCase = true) || exp.category.contains(it, ignoreCase = true) }
                }.map { exp ->
                    MajorWork(
                        description = exp.particulars,
                        amount2026 = exp.amount
                    )
                }
                database.yearlyReportDao().clearMajorWorks()
                if (majorWorkRecords.isNotEmpty()) {
                    database.yearlyReportDao().insertMajorWorks(majorWorkRecords)
                }

                // 3. Dynamic Flat Collections & Yearly Contributions
                val flatContributionsMap = mutableMapOf<String, Pair<String, Double>>()
                val flatRegex = Regex("""(?i)\b(?:flat|unit|door|apt|villa|no\.?|#)?\s*([0-9]{1,4}[a-zA-Z]?|[a-zA-Z][0-9]{1,3})\b""")

                for (exp in distinctExpenses) {
                    val combined = "${exp.particulars} ${exp.vendorPayee} ${exp.remarks}"
                    val match = flatRegex.find(combined)
                    if (match != null) {
                        val flatKey = match.groupValues[1].uppercase()
                        val current = flatContributionsMap[flatKey]
                        val residentName = if (exp.vendorPayee.isNotBlank() && exp.vendorPayee != "--" && exp.vendorPayee != "General Vendor") exp.vendorPayee else "Flat $flatKey Resident"
                        val addedAmt = (current?.second ?: 0.0) + exp.amount
                        flatContributionsMap[flatKey] = residentName to addedAmt
                    }
                }

                val flatContributions = flatContributionsMap.map { (flatNo, pair) ->
                    YearlyContribution(
                        flatNo = flatNo,
                        residentName = pair.first,
                        amount2026 = pair.second
                    )
                }

                database.yearlyReportDao().clearContributions()
                if (flatContributions.isNotEmpty()) {
                    database.yearlyReportDao().insertContributions(flatContributions)
                }

                val totalCollectedVal = flatContributions.sumOf { it.amount2026 }
                database.collectionDao().insertCollectionRecord(
                    CollectionRecord(
                        id = 1,
                        year = distinctExpenses.firstOrNull()?.year ?: "",
                        month = distinctExpenses.firstOrNull()?.month ?: "",
                        particulars = "Maintenance Fund & Collections",
                        remarks = if (flatContributions.isNotEmpty()) "${flatContributions.size} Flats Recorded" else "Direct Live Sync",
                        totalAmount = totalCollectedVal
                    )
                )

                // 4. Dynamic Contacts (Vendors & Payees from live sheet)
                val extractedServices = distinctExpenses
                    .filter { it.vendorPayee.isNotBlank() && it.vendorPayee != "--" && it.vendorPayee != "N/A" && it.vendorPayee != "General Vendor" }
                    .distinctBy { it.vendorPayee.lowercase() }
                    .map { exp ->
                        ServiceContact(
                            serviceType = exp.category.ifBlank { "Maintenance Service" },
                            contactPerson = exp.vendorPayee,
                            phoneNo = "",
                            remarks = "Vendor/Payee for ${exp.particulars}"
                        )
                    }

                database.contactsDao().clearServices()
                if (extractedServices.isNotEmpty()) {
                    database.contactsDao().insertServiceContacts(extractedServices)
                }

                val extractedOwners = flatContributions.map { c ->
                    OwnerContact(
                        flatNo = c.flatNo,
                        residentName = c.residentName.ifBlank { "Flat ${c.flatNo}" },
                        primaryContactNo = "",
                        emergencyContactNo = ""
                    )
                }
                database.contactsDao().clearOwners()
                if (extractedOwners.isNotEmpty()) {
                    database.contactsDao().insertOwnerContacts(extractedOwners)
                }

                val finalTitle = if (extractedTitle.isNotBlank() && extractedTitle.length in 3..60) extractedTitle else config.spreadsheetTitle.ifBlank { "Apartment Maintenance Ledger" }
                database.configDao().saveConfig(
                    config.copy(
                        spreadsheetTitle = finalTitle,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )
                return@withContext Result.success("Success: Synced ${distinctExpenses.size} live expenses & ${flatContributions.size} flat collections from Google Sheet!")
            } else if (successfulFetch) {
                return@withContext Result.failure(Exception("Sheet connected, but 0 expense records found. Please check that your Google Sheet has header columns (e.g., Date, Description/Particulars, Amount)."))
            } else {
                val failureMessage = diagnosedReason ?: "Unable to fetch data from Google Sheet. Please check the sheet link and verify it is shared with 'Anyone with the link can view'."
                return@withContext Result.failure(Exception(failureMessage))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Sync Error: ${e.localizedMessage ?: "Unknown error while reading Google Sheet"}"))
        }
    }

    private fun parseMonthAndYearFromDate(dateRaw: String, monthRaw: String, yearRaw: String): Pair<String, String> {
        val monthFromCol = monthRaw.trim()
        val yearFromCol = yearRaw.trim()

        val combined = "$dateRaw $monthRaw $yearRaw".lowercase()
        var parsedMonth = if (monthFromCol.isNotBlank()) monthFromCol else ""
        var parsedYear = if (yearFromCol.isNotBlank()) yearFromCol else "2026"

        if (parsedMonth.isBlank()) {
            if (combined.contains("jan")) parsedMonth = "January"
            else if (combined.contains("feb")) parsedMonth = "February"
            else if (combined.contains("mar")) parsedMonth = "March"
            else if (combined.contains("apr")) parsedMonth = "April"
            else if (combined.contains("may")) parsedMonth = "May"
            else if (combined.contains("jun")) parsedMonth = "June"
            else if (combined.contains("jul")) parsedMonth = "July"
            else if (combined.contains("aug")) parsedMonth = "August"
            else if (combined.contains("sep")) parsedMonth = "September"
            else if (combined.contains("oct")) parsedMonth = "October"
            else if (combined.contains("nov")) parsedMonth = "November"
            else if (combined.contains("dec")) parsedMonth = "December"
            else {
                val parts = dateRaw.split("/", "-", ".", " ")
                if (parts.size >= 2) {
                    val nums = parts.mapNotNull { it.toIntOrNull() }
                    if (nums.size >= 2) {
                        val mNum = nums.firstOrNull { it in 1..12 }
                        if (mNum != null) {
                            val mArray = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                            parsedMonth = mArray[mNum - 1]
                        }
                        val yNum = nums.firstOrNull { it in 2020..2030 || it in 24..30 }
                        if (yNum != null) {
                            parsedYear = if (yNum < 100) "20$yNum" else "$yNum"
                        }
                    }
                }
            }
        }
        if (parsedMonth.isBlank()) parsedMonth = "July"
        return parsedMonth to parsedYear
    }

    private fun parseCsvExpenses(csv: String): List<ExpenseRecord> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val firstLine = lines.first()
        val lowerFirstLine = firstLine.lowercase()
        val headerKeywordCount0 = listOf("particular", "amount", "date", "description", "cost", "item", "year", "month", "vendor", "payee", "balance", "s.no", "sl.no", "sl", "rs", "debit", "credit").count { lowerFirstLine.contains(it) }

        val hasTitleRow = headerKeywordCount0 < 2 && lines.size > 1
        val headerLine = if (hasTitleRow) lines[1] else lines[0]
        val dataLines = if (hasTitleRow) lines.drop(2) else lines.drop(1)

        val headerCols = parseCsvLine(headerLine).map { it.lowercase().trim('"').trim() }

        var yearCol = headerCols.indexOfFirst { it.contains("year") || it.contains("yr") }
        var monthCol = headerCols.indexOfFirst { it.contains("month") || it.contains("mth") }
        var dateCol = headerCols.indexOfFirst { it.contains("date") || it.contains("day") || it.contains("dt") || it.contains("s.no") || it.contains("sl.no") || it.contains("sl") || it.contains("no") }
        var particularsCol = headerCols.indexOfFirst {
            it.contains("particular") || it.contains("description") || it.contains("item") ||
                    it.contains("detail") || it.contains("purpose") || it.contains("name") || it.contains("title") ||
                    it.contains("expense") || it.contains("work") || it.contains("head")
        }
        var remarksCol = headerCols.indexOfFirst { it.contains("remark") || it.contains("note") || it.contains("comment") }
        var amountCol = headerCols.indexOfFirst {
            it.contains("amount") || it.contains("cost") || it.contains("price") ||
                    it.contains("rs") || it.contains("inr") || it.contains("₹") || it.contains("spent") || it.contains("total") || it.contains("debit") || it.contains("expenditure")
        }
        var vendorCol = headerCols.indexOfFirst {
            it.contains("vendor") || it.contains("payee") || it.contains("paid") || it.contains("by") || it.contains("person")
        }
        var billCol = headerCols.indexOfFirst { it.contains("bill") || it.contains("doc") || it.contains("receipt") }
        var pictureCol = headerCols.indexOfFirst { it.contains("picture") || it.contains("photo") || it.contains("image") }
        var balanceCol = headerCols.indexOfFirst { it.contains("balance") || it.contains("bal") }
        var categoryCol = headerCols.indexOfFirst { it.contains("category") || it.contains("type") || it.contains("head") }

        val sampleRows = dataLines.take(15).map { parseCsvLine(it) }

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

        if (particularsCol == -1 && sampleRows.isNotEmpty()) {
            val maxCols = sampleRows.maxOfOrNull { it.size } ?: 0
            for (colIdx in 0 until maxCols) {
                if (colIdx != amountCol && colIdx != dateCol && colIdx != monthCol && colIdx != yearCol) {
                    particularsCol = colIdx
                    break
                }
            }
        }

        if (particularsCol == -1) particularsCol = if (headerCols.size > 1) 1 else 0
        if (amountCol == -1) amountCol = (headerCols.size - 1).coerceAtLeast(0)

        var runningBalance = 12000.0

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

            val particulars = getVal(particularsCol).ifBlank {
                cols.firstOrNull { it.isNotBlank() && parseAmountVal(cols.indexOf(it)) == 0.0 }?.trim('"') ?: "Expense Item #${index + 1}"
            }

            val pLower = particulars.lowercase()
            if (pLower.contains("total") || pLower.contains("subtotal") || pLower.contains("closing balance") || pLower.contains("opening balance") || pLower.contains("balance b/f") || pLower.contains("balance c/f")) {
                return@mapIndexedNotNull null
            }

            val amount = parseAmountVal(amountCol)
            val dateRaw = getVal(dateCol).ifBlank { "01" }
            val monthRaw = getVal(monthCol)
            val yearRaw = getVal(yearCol)

            val (monthStr, yearStr) = parseMonthAndYearFromDate(dateRaw, monthRaw, yearRaw)

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
                dateDay = dateRaw,
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
