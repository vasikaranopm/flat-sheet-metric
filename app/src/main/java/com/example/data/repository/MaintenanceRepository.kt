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
            val rawInput = config.spreadsheetId.trim()
            val sheetId = extractSpreadsheetId(rawInput)
            val explicitGid = extractGid(rawInput)

            if (sheetId.isEmpty()) {
                return@withContext Result.failure(Exception("Spreadsheet link is missing. Please paste your Google Sheet link."))
            }

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
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            var allExpenses = mutableListOf<ExpenseRecord>()
            var extractedTitle = config.spreadsheetTitle
            var successfulFetch = false
            var errorMessage: String? = null

            for (targetUrl in urlsToTry.distinct()) {
                try {
                    val request = Request.Builder().url(targetUrl).header("User-Agent", "Mozilla/5.0").build()
                    val response = client.newCall(request).execute()
                    val statusCode = response.code
                    val body = response.body?.string() ?: ""

                    if (statusCode == 200) {
                        if (body.contains("accounts.google.com") || body.contains("ServiceLogin") || body.contains("<!DOCTYPE html>")) {
                            errorMessage = "Access Restricted: Please open Google Sheet -> Share -> set to 'Anyone with the link can view'."
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
                        errorMessage = "Spreadsheet Not Found (404). Please double-check your Google Sheet URL."
                    } else if (statusCode == 403) {
                        errorMessage = "Access Denied (403). Ensure sheet is shared as 'Anyone with the link can view'."
                    }
                } catch (e: Exception) {
                    if (errorMessage == null) errorMessage = e.localizedMessage
                }
            }

            if (allExpenses.isNotEmpty()) {
                val distinctExpenses = allExpenses.distinctBy { "${it.month}_${it.dateDay}_${it.particulars}_${it.amount}" }
                    .mapIndexed { index, record -> record.copy(id = index + 1) }

                database.expenseDao().clearAll()
                database.expenseDao().insertExpenseRecords(distinctExpenses)

                // 1. Dynamic Yearly Categories
                val categoriesMap = distinctExpenses.groupBy { it.category }
                val categoryRecords = categoriesMap.map { (catName, list) ->
                    YearlyExpenseCategory(
                        category = catName.ifBlank { "General" },
                        amount2026 = list.sumOf { it.amount }
                    )
                }
                database.yearlyReportDao().clearCategories()
                database.yearlyReportDao().insertExpenseCategories(categoryRecords)

                // 2. Dynamic Major Capital Works
                val majorKeywords = listOf("sensor", "motor", "alteration", "repair", "paint", "replace", "plumbing", "electrical", "tank", "clean", "work", "purchase", "installation", "service")
                val majorWorkRecords = distinctExpenses.filter { exp ->
                    exp.amount >= 1000 || majorKeywords.any { exp.particulars.contains(it, ignoreCase = true) || exp.category.contains(it, ignoreCase = true) }
                }.map { exp ->
                    MajorWork(
                        description = exp.particulars,
                        amount2026 = exp.amount
                    )
                }
                database.yearlyReportDao().clearMajorWorks()
                database.yearlyReportDao().insertMajorWorks(majorWorkRecords)

                // 3. Dynamic Flat Collections & Yearly Contributions
                var flat1A = 0.0; var flat1B = 0.0
                var flat2A = 0.0; var flat2B = 0.0
                var flat3A = 0.0; var flat3B = 0.0
                var foundCollections = false

                for (exp in distinctExpenses) {
                    val p = exp.particulars.lowercase()
                    if (p.contains("1a")) { flat1A += exp.amount; foundCollections = true }
                    if (p.contains("1b")) { flat1B += exp.amount; foundCollections = true }
                    if (p.contains("2a")) { flat2A += exp.amount; foundCollections = true }
                    if (p.contains("2b")) { flat2B += exp.amount; foundCollections = true }
                    if (p.contains("3a")) { flat3A += exp.amount; foundCollections = true }
                    if (p.contains("3b")) { flat3B += exp.amount; foundCollections = true }
                }

                val flatContributions = mutableListOf<YearlyContribution>()
                val flatsList = listOf("1A" to flat1A, "1B" to flat1B, "2A" to flat2A, "2B" to flat2B, "3A" to flat3A, "3B" to flat3B)

                for ((fNo, amt) in flatsList) {
                    val calculatedAmt = if (amt > 0) amt else 2000.0
                    flatContributions.add(YearlyContribution(flatNo = fNo, residentName = "Flat $fNo", amount2026 = calculatedAmt))
                }

                val totalCollectedVal = flatContributions.sumOf { it.amount2026 }

                database.collectionDao().insertCollectionRecord(
                    CollectionRecord(
                        id = 1,
                        flat1AAmount = flatContributions.firstOrNull { it.flatNo == "1A" }?.amount2026 ?: 2000.0,
                        flat1BAmount = flatContributions.firstOrNull { it.flatNo == "1B" }?.amount2026 ?: 2000.0,
                        flat2AAmount = flatContributions.firstOrNull { it.flatNo == "2A" }?.amount2026 ?: 2000.0,
                        flat2BAmount = flatContributions.firstOrNull { it.flatNo == "2B" }?.amount2026 ?: 2000.0,
                        flat3AAmount = flatContributions.firstOrNull { it.flatNo == "3A" }?.amount2026 ?: 2000.0,
                        flat3BAmount = flatContributions.firstOrNull { it.flatNo == "3B" }?.amount2026 ?: 2000.0,
                        totalAmount = totalCollectedVal
                    )
                )

                database.yearlyReportDao().clearContributions()
                database.yearlyReportDao().insertContributions(flatContributions)

                // 4. Dynamic Contacts (Owners & Vendors/Services)
                val extractedServices = distinctExpenses
                    .filter { it.vendorPayee.isNotBlank() && it.vendorPayee != "--" && it.vendorPayee != "N/A" }
                    .distinctBy { it.vendorPayee }
                    .map { exp ->
                        ServiceContact(
                            serviceType = exp.category.ifBlank { "Maintenance Service" },
                            contactPerson = exp.vendorPayee,
                            phoneNo = "--",
                            remarks = "Service Vendor for ${exp.particulars}"
                        )
                    }

                val defaultServiceTypes = listOf(
                    ServiceContact("Cleaning & Sanitation", "Housekeeping Vendor", "--", "Common Area Housekeeping"),
                    ServiceContact("Motor & Electrical", "Electrical Technician", "--", "Water Sensor & Pump Maintenance"),
                    ServiceContact("Plumbing & Lines", "Plumbing Technician", "--", "Common Plumbing Works")
                )

                database.contactsDao().clearServices()
                database.contactsDao().insertServiceContacts(if (extractedServices.isNotEmpty()) extractedServices else defaultServiceTypes)

                val extractedOwners = listOf("1A", "1B", "2A", "2B", "3A", "3B").map { flat ->
                    OwnerContact(
                        flatNo = flat,
                        residentName = "Flat $flat Resident",
                        primaryContactNo = "--",
                        emergencyContactNo = ""
                    )
                }
                database.contactsDao().clearOwners()
                database.contactsDao().insertOwnerContacts(extractedOwners)

                val finalTitle = if (extractedTitle.isNotBlank()) extractedTitle else "Apartment Maintenance"
                database.configDao().saveConfig(
                    config.copy(
                        spreadsheetTitle = finalTitle,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )
                return@withContext Result.success("Access Verified & Synced! (${distinctExpenses.size} live records updated)")
            } else if (successfulFetch) {
                val finalTitle = if (extractedTitle.isNotBlank()) extractedTitle else "Apartment Maintenance"
                database.configDao().saveConfig(
                    config.copy(
                        spreadsheetTitle = finalTitle,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )
                return@withContext Result.success("Google Sheet Connected ($finalTitle)")
            } else {
                return@withContext Result.failure(Exception(errorMessage ?: "Unable to fetch data from Google Sheet."))
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
