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

    suspend fun updateCollectionRecord(record: CollectionRecord) {
        database.collectionDao().updateCollectionRecord(record)
    }

    suspend fun deleteCollectionRecord(id: Int) {
        database.collectionDao().deleteCollectionRecord(id)
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

        if (sheetId.isEmpty() || sheetId == "DEFAULT_SHEET_LINK_PLACEHOLDER") {
            return@withContext Result.failure(Exception("No Google Sheet configured. Please enter your Google Sheet link in Settings or Dashboard."))
        }

        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()

        fun fetchCsvForTab(tabNames: List<String>): Pair<String?, String?> {
            var lastError: String? = null
            for (tab in tabNames) {
                val candidateUrls = mutableListOf<String>()
                if (tab.isEmpty()) {
                    if (explicitGid != null) {
                        candidateUrls.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&gid=$explicitGid")
                        candidateUrls.add("https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&gid=$explicitGid")
                    } else {
                        candidateUrls.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv")
                        candidateUrls.add("https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv")
                    }
                } else {
                    val encodedTab = java.net.URLEncoder.encode(tab, "UTF-8")
                    candidateUrls.add("https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=$encodedTab")
                    candidateUrls.add("https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&sheet=$encodedTab")
                }

                for (url in candidateUrls) {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                            .build()
                        val response = client.newCall(request).execute()
                        val code = response.code
                        val body = response.body?.string() ?: ""

                        if (code == 200) {
                            if (body.contains("accounts.google.com") || body.contains("ServiceLogin") || body.contains("<!DOCTYPE html>") || body.contains("<html>")) {
                                lastError = "Google Sheet is in Restricted Access mode. To allow the app to read it: In Google Sheets, tap 'Share' (top right) -> Under 'General access', change from 'Restricted' to 'Anyone with the link' (Viewer role)."
                                continue
                            }
                            if (body.isNotBlank() && !body.startsWith("{\"status\":\"error\"")) {
                                return body to null
                            }
                        } else if (code == 401) {
                            lastError = "Google Sheet requires Link Sharing (HTTP 401): Sharing to user email only protects web-browser sign-in. To allow the app to sync, open the sheet -> tap 'Share' -> change 'General access' from 'Restricted' to 'Anyone with the link' (Viewer)."
                        } else if (code == 403) {
                            lastError = "Access Denied (HTTP 403): In Google Sheets, tap Share -> General Access -> set to 'Anyone with the link can view'."
                        } else if (code == 404) {
                            lastError = "Spreadsheet Not Found (HTTP 404): The Google Sheet ID '$sheetId' was not found. Please verify the URL."
                        }
                    } catch (e: Exception) {
                        lastError = "Network Connection Issue: ${e.localizedMessage ?: "Could not connect to Google Sheets"}"
                    }
                }
            }
            return null to lastError
        }

        try {
            // 1. Fetch Tab: Collection Record
            val (collectionCsv, colErr) = fetchCsvForTab(listOf("Collection Record", "Collection", "Collections", "Maintenance Collection Record"))
            
            // 2. Fetch Tab: Expense Record
            val (expenseCsv, expErr) = fetchCsvForTab(listOf("Expense Record", "Expenses", "Expense", "Common Expense Record", "July", "August"))
            
            // 3. Fetch Tab: Contacts
            val (contactsCsv, _) = fetchCsvForTab(listOf("Contacts", "Contact", "Resident Contacts", "Owners Contacts"))
            
            // 4. Fetch Tab: Yearly Report
            val (yearlyCsv, _) = fetchCsvForTab(listOf("Yearly Report", "Yearly", "Report", "Summary", "Annual Report"))

            // Fallback: If specific tabs failed, try root sheet
            val (rootCsv, rootErr) = if (collectionCsv == null && expenseCsv == null) {
                fetchCsvForTab(listOf(""))
            } else null to null

            if (collectionCsv == null && expenseCsv == null && rootCsv == null) {
                val failureMsg = colErr ?: expErr ?: rootErr ?: "Unable to fetch data from Google Sheet. Ensure your Google Sheet is shared with 'Anyone with the link can view'."
                return@withContext Result.failure(Exception(failureMsg))
            }

            var extractedTitle = config.spreadsheetTitle

            // Parse Collection Records
            val parsedCollections = mutableListOf<CollectionRecord>()
            val parsedYearlyContribFromCollection = mutableListOf<YearlyContribution>()
            val parsedOwnersFromCollection = mutableListOf<OwnerContact>()

            if (collectionCsv != null) {
                val (collections, contribs, owners, title) = parseCollectionSheet(collectionCsv)
                parsedCollections.addAll(collections)
                parsedYearlyContribFromCollection.addAll(contribs)
                parsedOwnersFromCollection.addAll(owners)
                if (title.isNotBlank()) extractedTitle = title
            }

            // Parse Expense Records
            val parsedExpenses = mutableListOf<ExpenseRecord>()
            if (expenseCsv != null) {
                val (expenses, title) = parseExpenseSheet(expenseCsv)
                parsedExpenses.addAll(expenses)
                if (title.isNotBlank() && extractedTitle.isBlank()) extractedTitle = title
            } else if (rootCsv != null) {
                val (expenses, title) = parseExpenseSheet(rootCsv)
                parsedExpenses.addAll(expenses)
                if (title.isNotBlank()) extractedTitle = title
            }

            // Parse Contacts
            val parsedOwners = mutableListOf<OwnerContact>()
            val parsedServices = mutableListOf<ServiceContact>()
            if (contactsCsv != null) {
                val (owners, services) = parseContactsSheet(contactsCsv)
                parsedOwners.addAll(owners)
                parsedServices.addAll(services)
            }
            // Merge owners from collection headers if contacts tab had fewer
            if (parsedOwners.isEmpty() && parsedOwnersFromCollection.isNotEmpty()) {
                parsedOwners.addAll(parsedOwnersFromCollection)
            } else if (parsedOwnersFromCollection.isNotEmpty()) {
                for (owner in parsedOwnersFromCollection) {
                    val existingIndex = parsedOwners.indexOfFirst { it.flatNo.equals(owner.flatNo, ignoreCase = true) }
                    if (existingIndex >= 0) {
                        val curr = parsedOwners[existingIndex]
                        if (curr.residentName.isBlank() && owner.residentName.isNotBlank()) {
                            parsedOwners[existingIndex] = curr.copy(residentName = owner.residentName)
                        }
                    } else {
                        parsedOwners.add(owner)
                    }
                }
            }

            // Parse Yearly Report
            val parsedYearlyCategories = mutableListOf<YearlyExpenseCategory>()
            val parsedMajorWorks = mutableListOf<MajorWork>()
            val parsedYearlyContribFromReport = mutableListOf<YearlyContribution>()

            if (yearlyCsv != null) {
                val (contribs, cats, works) = parseYearlyReportSheet(yearlyCsv)
                parsedYearlyContribFromReport.addAll(contribs)
                parsedYearlyCategories.addAll(cats)
                parsedMajorWorks.addAll(works)
            }

            // If yearly categories empty, compute directly from parsed expenses
            if (parsedYearlyCategories.isEmpty() && parsedExpenses.isNotEmpty()) {
                val grouped = parsedExpenses.groupBy { it.category.ifBlank { "General" } }
                parsedYearlyCategories.addAll(grouped.map { (cat, list) ->
                    YearlyExpenseCategory(category = cat, amount2026 = list.sumOf { it.amount })
                })
            }

            // If major works empty, detect high-value or alteration expenses
            if (parsedMajorWorks.isEmpty() && parsedExpenses.isNotEmpty()) {
                val majorKeywords = listOf("sensor", "motor", "alteration", "installation", "cable", "repair", "amc", "service")
                val foundWorks = parsedExpenses.filter { exp ->
                    exp.amount >= 1000 || majorKeywords.any { exp.particulars.contains(it, ignoreCase = true) }
                }.map { exp ->
                    MajorWork(description = exp.particulars, amount2026 = exp.amount)
                }
                parsedMajorWorks.addAll(foundWorks)
            }

            // Final Contributions: prioritize report, then collection calculation
            val finalContributions = if (parsedYearlyContribFromReport.isNotEmpty()) {
                parsedYearlyContribFromReport
            } else if (parsedYearlyContribFromCollection.isNotEmpty()) {
                parsedYearlyContribFromCollection
            } else emptyList()

            // Save all to database
            database.expenseDao().clearAll()
            if (parsedExpenses.isNotEmpty()) {
                database.expenseDao().insertExpenseRecords(parsedExpenses)
            }

            database.collectionDao().clearAll()
            if (parsedCollections.isNotEmpty()) {
                for (col in parsedCollections) {
                    database.collectionDao().insertCollectionRecord(col)
                }
            }

            database.yearlyReportDao().clearContributions()
            if (finalContributions.isNotEmpty()) {
                database.yearlyReportDao().insertContributions(finalContributions)
            }

            database.yearlyReportDao().clearCategories()
            if (parsedYearlyCategories.isNotEmpty()) {
                database.yearlyReportDao().insertExpenseCategories(parsedYearlyCategories)
            }

            database.yearlyReportDao().clearMajorWorks()
            if (parsedMajorWorks.isNotEmpty()) {
                database.yearlyReportDao().insertMajorWorks(parsedMajorWorks)
            }

            database.contactsDao().clearOwners()
            if (parsedOwners.isNotEmpty()) {
                database.contactsDao().insertOwnerContacts(parsedOwners)
            }

            database.contactsDao().clearServices()
            if (parsedServices.isNotEmpty()) {
                database.contactsDao().insertServiceContacts(parsedServices)
            }

            val finalTitle = if (extractedTitle.isNotBlank() && extractedTitle.length in 3..60) extractedTitle else "Gomathi Ilam Thendral"
            database.configDao().saveConfig(
                config.copy(
                    spreadsheetTitle = finalTitle,
                    lastSyncTime = System.currentTimeMillis()
                )
            )

            val summaryStats = mutableListOf<String>()
            if (parsedCollections.isNotEmpty()) summaryStats.add("${parsedCollections.size} collection months")
            if (parsedExpenses.isNotEmpty()) summaryStats.add("${parsedExpenses.size} expense entries")
            if (parsedOwners.isNotEmpty()) summaryStats.add("${parsedOwners.size} resident contacts")

            val successDetails = if (summaryStats.isNotEmpty()) summaryStats.joinToString(", ") else "0 records found"
            return@withContext Result.success("Success: Synced $successDetails from Google Sheet!")
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Sync Error: ${e.localizedMessage ?: "Unknown error while reading Google Sheet"}"))
        }
    }

    private data class CollectionParseResult(
        val collections: List<CollectionRecord>,
        val contributions: List<YearlyContribution>,
        val owners: List<OwnerContact>,
        val title: String
    )

    private fun parseCollectionSheet(csv: String): CollectionParseResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return CollectionParseResult(emptyList(), emptyList(), emptyList(), "")

        var title = ""
        var headerIndex = -1

        for (i in 0 until minOf(lines.size, 5)) {
            val cols = parseCsvLine(lines[i]).map { it.lowercase().trim('"').trim() }
            if (cols.any { it.contains("year") || it.contains("month") || it.contains("particular") || it.contains("1a") }) {
                headerIndex = i
                break
            } else if (i == 0 && lines[0].isNotBlank()) {
                val firstCell = parseCsvLine(lines[0]).firstOrNull()?.trim('"')?.trim() ?: ""
                if (firstCell.isNotBlank() && firstCell.length in 3..60) {
                    title = firstCell.split("-").first().trim()
                }
            }
        }

        if (headerIndex == -1) headerIndex = 0

        val headerCols = parseCsvLine(lines[headerIndex]).map { it.trim('"').trim() }
        val dataLines = lines.drop(headerIndex + 1)

        val yearCol = headerCols.indexOfFirst { it.contains("year", ignoreCase = true) }
        val monthCol = headerCols.indexOfFirst { it.contains("month", ignoreCase = true) }
        val particularsCol = headerCols.indexOfFirst { it.contains("particular", ignoreCase = true) }
        val remarksCol = headerCols.indexOfFirst { it.contains("remark", ignoreCase = true) }

        // Detect Flat columns (1A, 1B, 2A, 2B, 3A, 3B) and their resident names from header e.g. "1A - M.Madhan Raj"
        fun findFlatCol(flat: String): Pair<Int, String> {
            val idx = headerCols.indexOfFirst { it.contains(flat, ignoreCase = true) }
            if (idx == -1) return -1 to ""
            val fullHeader = headerCols[idx]
            val residentName = if (fullHeader.contains("-")) {
                fullHeader.substringAfter("-").trim()
            } else ""
            return idx to residentName
        }

        val (col1A, name1A) = findFlatCol("1A")
        val (col1B, name1B) = findFlatCol("1B")
        val (col2A, name2A) = findFlatCol("2A")
        val (col2B, name2B) = findFlatCol("2B")
        val (col3A, name3A) = findFlatCol("3A")
        val (col3B, name3B) = findFlatCol("3B")

        val ownerContacts = mutableListOf<OwnerContact>()
        if (col1A != -1 && name1A.isNotBlank()) ownerContacts.add(OwnerContact("1A", name1A))
        if (col1B != -1 && name1B.isNotBlank()) ownerContacts.add(OwnerContact("1B", name1B))
        if (col2A != -1 && name2A.isNotBlank()) ownerContacts.add(OwnerContact("2A", name2A))
        if (col2B != -1 && name2B.isNotBlank()) ownerContacts.add(OwnerContact("2B", name2B))
        if (col3A != -1 && name3A.isNotBlank()) ownerContacts.add(OwnerContact("3A", name3A))
        if (col3B != -1 && name3B.isNotBlank()) ownerContacts.add(OwnerContact("3B", name3B))

        val collections = mutableListOf<CollectionRecord>()
        val flatTotals = mutableMapOf<String, Double>("1A" to 0.0, "1B" to 0.0, "2A" to 0.0, "2B" to 0.0, "3A" to 0.0, "3B" to 0.0)

        var currentYear = "2026"

        for (line in dataLines) {
            val cols = parseCsvLine(line)
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            fun getVal(idx: Int): String {
                if (idx == -1) return ""
                return cols.getOrNull(idx)?.trim('"')?.trim() ?: ""
            }

            fun parseAmt(idx: Int): Double {
                val str = getVal(idx).replace("₹", "").replace(",", "").replace("Rs.", "").trim()
                return str.toDoubleOrNull() ?: 0.0
            }

            val yearRaw = getVal(yearCol).replace(".0", "").trim()
            val monthRaw = getVal(monthCol)
            val particularsRaw = getVal(particularsCol)
            val remarksRaw = getVal(remarksCol)

            if (yearRaw.isNotBlank() && yearRaw.toDoubleOrNull() != null) {
                currentYear = yearRaw.toDouble().toInt().toString()
            }

            if (particularsRaw.contains("total", ignoreCase = true) || monthRaw.contains("total", ignoreCase = true)) {
                continue
            }

            if (monthRaw.isBlank() && particularsRaw.isBlank()) {
                continue
            }

            val amt1A = parseAmt(col1A)
            val amt1B = parseAmt(col1B)
            val amt2A = parseAmt(col2A)
            val amt2B = parseAmt(col2B)
            val amt3A = parseAmt(col3A)
            val amt3B = parseAmt(col3B)

            val total = amt1A + amt1B + amt2A + amt2B + amt3A + amt3B
            if (total > 0 || monthRaw.isNotBlank()) {
                collections.add(
                    CollectionRecord(
                        id = collections.size + 1,
                        year = currentYear,
                        month = monthRaw.ifBlank { "Month ${collections.size + 1}" },
                        particulars = particularsRaw.ifBlank { "Monthly Maintenance" },
                        remarks = remarksRaw,
                        flat1AAmount = amt1A,
                        flat1BAmount = amt1B,
                        flat2AAmount = amt2A,
                        flat2BAmount = amt2B,
                        flat3AAmount = amt3A,
                        flat3BAmount = amt3B,
                        totalAmount = total
                    )
                )

                flatTotals["1A"] = (flatTotals["1A"] ?: 0.0) + amt1A
                flatTotals["1B"] = (flatTotals["1B"] ?: 0.0) + amt1B
                flatTotals["2A"] = (flatTotals["2A"] ?: 0.0) + amt2A
                flatTotals["2B"] = (flatTotals["2B"] ?: 0.0) + amt2B
                flatTotals["3A"] = (flatTotals["3A"] ?: 0.0) + amt3A
                flatTotals["3B"] = (flatTotals["3B"] ?: 0.0) + amt3B
            }
        }

        val contributions = flatTotals.map { (flat, total) ->
            val residentName = ownerContacts.firstOrNull { it.flatNo.equals(flat, ignoreCase = true) }?.residentName ?: "Flat $flat"
            YearlyContribution(
                flatNo = flat,
                residentName = residentName,
                amount2026 = total
            )
        }

        return CollectionParseResult(collections, contributions, ownerContacts, title)
    }

    private data class ExpenseParseResult(
        val expenses: List<ExpenseRecord>,
        val title: String
    )

    private fun parseExpenseSheet(csv: String): ExpenseParseResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ExpenseParseResult(emptyList(), "")

        var title = ""
        var headerIndex = -1

        for (i in 0 until minOf(lines.size, 5)) {
            val cols = parseCsvLine(lines[i]).map { it.lowercase().trim('"').trim() }
            if (cols.any { it.contains("particular") || it.contains("amount") || it.contains("vendor") || it.contains("payee") || it.contains("date") }) {
                headerIndex = i
                break
            } else if (i == 0 && lines[0].isNotBlank()) {
                val firstCell = parseCsvLine(lines[0]).firstOrNull()?.trim('"')?.trim() ?: ""
                if (firstCell.isNotBlank() && firstCell.length in 3..60) {
                    title = firstCell.split("-").first().trim()
                }
            }
        }

        if (headerIndex == -1) headerIndex = 0

        val headerCols = parseCsvLine(lines[headerIndex]).map { it.lowercase().trim('"').trim() }
        val dataLines = lines.drop(headerIndex + 1)

        val yearCol = headerCols.indexOfFirst { it.contains("year") || it.contains("yr") }
        val monthCol = headerCols.indexOfFirst { it.contains("month") || it.contains("mth") }
        val dateCol = headerCols.indexOfFirst { it.contains("date") || it.contains("day") || it.contains("dt") || it.contains("sl") || it.contains("s.no") }
        val particularsCol = headerCols.indexOfFirst { it.contains("particular") || it.contains("description") || it.contains("item") || it.contains("detail") }
        val remarksCol = headerCols.indexOfFirst { it.contains("remark") || it.contains("note") || it.contains("comment") }
        val amountCol = headerCols.indexOfFirst { it.contains("amount") || it.contains("cost") || it.contains("spent") || it.contains("price") || it.contains("₹") }
        val vendorCol = headerCols.indexOfFirst { it.contains("vendor") || it.contains("payee") || it.contains("person") || it.contains("paid") }
        val billCol = headerCols.indexOfFirst { it.contains("bill") || it.contains("receipt") }
        val pictureCol = headerCols.indexOfFirst { it.contains("picture") || it.contains("photo") || it.contains("image") }
        val balanceCol = headerCols.indexOfFirst { it.contains("balance") || it.contains("bal") }
        val categoryCol = headerCols.indexOfFirst { it.contains("category") || it.contains("type") }

        var activeYear = "2026"
        var activeMonth = "July"
        var activeBalance = 12000.0
        val records = mutableListOf<ExpenseRecord>()

        for (line in dataLines) {
            val cols = parseCsvLine(line)
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            fun getVal(idx: Int): String {
                if (idx == -1) return ""
                return cols.getOrNull(idx)?.trim('"')?.trim() ?: ""
            }

            fun parseAmt(idx: Int): Double {
                val str = getVal(idx).replace("₹", "").replace(",", "").replace("Rs.", "").trim()
                return str.toDoubleOrNull() ?: 0.0
            }

            val firstCell = cols.firstOrNull()?.trim('"')?.trim() ?: ""
            // Check if row is Opening Balance header e.g. "Opening Balance - 1 Jul 2026"
            if (firstCell.contains("Opening Balance", ignoreCase = true) || line.contains("Opening Balance", ignoreCase = true)) {
                if (firstCell.contains("Jul", ignoreCase = true)) activeMonth = "July"
                else if (firstCell.contains("Aug", ignoreCase = true)) activeMonth = "August"
                else if (firstCell.contains("Sep", ignoreCase = true)) activeMonth = "September"
                else if (firstCell.contains("Oct", ignoreCase = true)) activeMonth = "October"
                else if (firstCell.contains("Nov", ignoreCase = true)) activeMonth = "November"
                else if (firstCell.contains("Dec", ignoreCase = true)) activeMonth = "December"
                else if (firstCell.contains("Jan", ignoreCase = true)) activeMonth = "January"
                else if (firstCell.contains("Feb", ignoreCase = true)) activeMonth = "February"
                else if (firstCell.contains("Mar", ignoreCase = true)) activeMonth = "March"

                val opBal = parseAmt(balanceCol).let { if (it > 0) it else parseAmt(amountCol) }
                if (opBal > 0) activeBalance = opBal
                continue
            }

            val yearRaw = getVal(yearCol).replace(".0", "").trim()
            val monthRaw = getVal(monthCol)
            if (yearRaw.isNotBlank() && yearRaw.toDoubleOrNull() != null) {
                activeYear = yearRaw.toDouble().toInt().toString()
            }
            if (monthRaw.isNotBlank() && !monthRaw.contains("total", ignoreCase = true)) {
                activeMonth = monthRaw
            }

            val particulars = getVal(particularsCol)
            val pLower = particulars.lowercase()
            val remarksLower = getVal(remarksCol).lowercase()

            // Skip total and summary rows
            if (pLower.contains("total") || remarksLower.contains("total") || pLower.startsWith("=sum") || pLower.contains("subtotal") || pLower.contains("closing balance")) {
                continue
            }

            val amount = parseAmt(amountCol)
            if (amount <= 0 && particulars.isBlank()) continue

            var dateRaw = getVal(dateCol)
            if (dateRaw.endsWith(".0")) {
                dateRaw = dateRaw.substringBefore(".0")
            }
            if (dateRaw.length == 1) {
                dateRaw = "0$dateRaw"
            }

            val vendorStr = getVal(vendorCol).ifBlank { "--" }
            val remarksStr = getVal(remarksCol).ifBlank { "--" }
            val billStr = getVal(billCol).ifBlank { "N/A" }
            val pictureStr = getVal(pictureCol).ifBlank { "N/A" }

            val explicitBalance = parseAmt(balanceCol)
            val finalBalance = if (explicitBalance > 0) {
                activeBalance = explicitBalance
                explicitBalance
            } else {
                activeBalance -= amount
                activeBalance
            }

            // Derive accurate category based on actual schema
            val explicitCategory = getVal(categoryCol)
            val finalCategory = if (explicitCategory.isNotBlank()) {
                explicitCategory
            } else {
                val combinedText = "$particulars $remarksStr $vendorStr".lowercase()
                when {
                    combinedText.contains("clean") || combinedText.contains("salary") || combinedText.contains("chithra") || combinedText.contains("kola maavu") -> "Cleaning"
                    combinedText.contains("eb") || combinedText.contains("electricity") || combinedText.contains("tneb") || combinedText.contains("meter") -> "Common Line EB"
                    combinedText.contains("sensor") || combinedText.contains("motor") || combinedText.contains("installation") || combinedText.contains("cable") -> "Alteration/Additional work"
                    combinedText.contains("supplies") || combinedText.contains("letter box") || combinedText.contains("stores") || combinedText.contains("purchases") -> "Common Purchases"
                    combinedText.contains("lift") || combinedText.contains("amc") -> "Lift (AMC)"
                    combinedText.contains("repair") || combinedText.contains("plumb") || combinedText.contains("leak") -> "Repair Work"
                    else -> "Miscellaneous"
                }
            }

            records.add(
                ExpenseRecord(
                    id = records.size + 1,
                    year = activeYear,
                    month = activeMonth,
                    dateDay = dateRaw,
                    particulars = particulars.ifBlank { "Expense item" },
                    remarks = remarksStr,
                    amount = amount,
                    vendorPayee = vendorStr,
                    billAvailable = billStr,
                    picture = pictureStr,
                    balance = finalBalance,
                    category = finalCategory
                )
            )
        }

        return ExpenseParseResult(records, title)
    }

    private data class ContactsParseResult(
        val owners: List<OwnerContact>,
        val services: List<ServiceContact>
    )

    private fun parseContactsSheet(csv: String): ContactsParseResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ContactsParseResult(emptyList(), emptyList())

        val owners = mutableListOf<OwnerContact>()
        val services = mutableListOf<ServiceContact>()

        var currentSection = "" // "OWNERS" or "SERVICES"
        var ownerHeaderCols = listOf<String>()
        var serviceHeaderCols = listOf<String>()

        for (line in lines) {
            val cols = parseCsvLine(line).map { it.trim('"').trim() }
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            val first = cols.first().lowercase()
            if (first.contains("owner") || first.contains("resident") && !first.contains("name")) {
                currentSection = "OWNERS"
                continue
            } else if (first.contains("service") || first.contains("vendor") && !first.contains("type")) {
                currentSection = "SERVICES"
                continue
            }

            if (cols.any { it.contains("flat", ignoreCase = true) || it.contains("resident name", ignoreCase = true) }) {
                currentSection = "OWNERS"
                ownerHeaderCols = cols.map { it.lowercase() }
                continue
            } else if (cols.any { it.contains("service type", ignoreCase = true) || it.contains("contact person", ignoreCase = true) }) {
                currentSection = "SERVICES"
                serviceHeaderCols = cols.map { it.lowercase() }
                continue
            }

            if (currentSection == "OWNERS") {
                val flatNoCol = ownerHeaderCols.indexOfFirst { it.contains("flat") }.let { if (it == -1) 0 else it }
                val nameCol = ownerHeaderCols.indexOfFirst { it.contains("name") || it.contains("resident") }.let { if (it == -1) 1 else it }
                val primaryPhoneCol = ownerHeaderCols.indexOfFirst { it.contains("primary") || it.contains("phone") || it.contains("contact") }.let { if (it == -1) 2 else it }
                val emergencyPhoneCol = ownerHeaderCols.indexOfFirst { it.contains("emergency") }.let { if (it == -1) 3 else it }

                val flatNo = cols.getOrNull(flatNoCol) ?: ""
                val name = cols.getOrNull(nameCol) ?: ""
                val primaryPhone = cols.getOrNull(primaryPhoneCol) ?: ""
                val emergencyPhone = cols.getOrNull(emergencyPhoneCol) ?: ""

                if (flatNo.isNotBlank() && flatNo.length <= 6 && !flatNo.contains("flat", ignoreCase = true)) {
                    owners.add(
                        OwnerContact(
                            flatNo = flatNo,
                            residentName = name,
                            primaryContactNo = primaryPhone.replace(".0", ""),
                            emergencyContactNo = emergencyPhone.replace(".0", "")
                        )
                    )
                }
            } else if (currentSection == "SERVICES") {
                val typeCol = serviceHeaderCols.indexOfFirst { it.contains("type") || it.contains("service") }.let { if (it == -1) 0 else it }
                val personCol = serviceHeaderCols.indexOfFirst { it.contains("person") || it.contains("name") || it.contains("contact") }.let { if (it == -1) 1 else it }
                val phoneCol = serviceHeaderCols.indexOfFirst { it.contains("phone") || it.contains("mobile") || it.contains("no") }.let { if (it == -1) 2 else it }
                val remarksCol = serviceHeaderCols.indexOfFirst { it.contains("remark") || it.contains("detail") || it.contains("note") }.let { if (it == -1) 3 else it }

                val sType = cols.getOrNull(typeCol) ?: ""
                val sPerson = cols.getOrNull(personCol) ?: ""
                val sPhone = cols.getOrNull(phoneCol) ?: ""
                val sRemarks = cols.getOrNull(remarksCol) ?: ""

                if (sType.isNotBlank() && !sType.contains("service type", ignoreCase = true)) {
                    services.add(
                        ServiceContact(
                            serviceType = sType,
                            contactPerson = sPerson,
                            phoneNo = sPhone.replace(".0", ""),
                            remarks = sRemarks
                        )
                    )
                }
            }
        }

        return ContactsParseResult(owners, services)
    }

    private data class YearlyReportParseResult(
        val contributions: List<YearlyContribution>,
        val categories: List<YearlyExpenseCategory>,
        val majorWorks: List<MajorWork>
    )

    private fun parseYearlyReportSheet(csv: String): YearlyReportParseResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return YearlyReportParseResult(emptyList(), emptyList(), emptyList())

        val contributions = mutableListOf<YearlyContribution>()
        val categories = mutableListOf<YearlyExpenseCategory>()
        val majorWorks = mutableListOf<MajorWork>()

        var currentSection = "" // "CONTRIBUTION", "EXPENSE", "MAJOR_WORKS"

        for (line in lines) {
            val cols = parseCsvLine(line).map { it.trim('"').trim() }
            if (cols.isEmpty() || cols.all { it.isBlank() }) continue

            val first = cols.first().lowercase()
            if (first.contains("contribution summary") || first.contains("contribution")) {
                currentSection = "CONTRIBUTION"
                continue
            } else if (first.contains("expense summary") || first.contains("expense category")) {
                currentSection = "EXPENSE"
                continue
            } else if (first.contains("major work") || first.contains("capital work")) {
                currentSection = "MAJOR_WORKS"
                continue
            }

            if (cols.any { it.contains("flat", ignoreCase = true) || it.contains("payee", ignoreCase = true) }) {
                currentSection = "CONTRIBUTION"
                continue
            } else if (cols.any { it.contains("category", ignoreCase = true) || it.contains("service", ignoreCase = true) }) {
                currentSection = "EXPENSE"
                continue
            }

            fun parseAmount(idx: Int): Double {
                val cell = cols.getOrNull(idx)?.replace("₹", "")?.replace(",", "")?.trim() ?: ""
                return cell.toDoubleOrNull() ?: 0.0
            }

            if (currentSection == "CONTRIBUTION") {
                val flatRaw = cols.getOrNull(0) ?: ""
                val amt = parseAmount(1).let { if (it > 0) it else parseAmount(2) }
                if (flatRaw.isNotBlank() && !flatRaw.contains("total", ignoreCase = true)) {
                    val flatNo = if (flatRaw.contains("-")) flatRaw.substringBefore("-").trim() else flatRaw
                    val residentName = if (flatRaw.contains("-")) flatRaw.substringAfter("-").trim() else ""
                    contributions.add(
                        YearlyContribution(
                            flatNo = flatNo,
                            residentName = residentName,
                            amount2026 = amt
                        )
                    )
                }
            } else if (currentSection == "EXPENSE") {
                val catRaw = cols.getOrNull(0) ?: ""
                val amt = parseAmount(1).let { if (it > 0) it else parseAmount(2) }
                if (catRaw.isNotBlank() && !catRaw.contains("total", ignoreCase = true)) {
                    categories.add(
                        YearlyExpenseCategory(
                            category = catRaw,
                            amount2026 = amt
                        )
                    )
                }
            } else if (currentSection == "MAJOR_WORKS") {
                val workRaw = cols.getOrNull(0) ?: ""
                val amt = parseAmount(1).let { if (it > 0) it else parseAmount(2) }
                if (workRaw.isNotBlank() && !workRaw.contains("total", ignoreCase = true)) {
                    majorWorks.add(
                        MajorWork(
                            description = workRaw,
                            amount2026 = amt
                        )
                    )
                }
            }
        }

        return YearlyReportParseResult(contributions, categories, majorWorks)
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
