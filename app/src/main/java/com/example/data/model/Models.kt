package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_records")
data class CollectionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: String = "",
    val month: String = "",
    val particulars: String = "Maintenance Collections",
    val remarks: String = "",
    val flat1AAmount: Double = 0.0,
    val flat1BAmount: Double = 0.0,
    val flat2AAmount: Double = 0.0,
    val flat2BAmount: Double = 0.0,
    val flat3AAmount: Double = 0.0,
    val flat3BAmount: Double = 0.0,
    val totalAmount: Double = 0.0
)

@Entity(tableName = "expense_records")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: String = "",
    val month: String = "",
    val dateDay: String = "",
    val particulars: String = "",
    val remarks: String = "",
    val amount: Double = 0.0,
    val vendorPayee: String = "",
    val billAvailable: String = "N/A",
    val picture: String = "N/A",
    val balance: Double = 0.0,
    val category: String = "General"
)

@Entity(tableName = "yearly_contributions")
data class YearlyContribution(
    @PrimaryKey val flatNo: String,
    val residentName: String = "",
    val amount2026: Double = 0.0
)

@Entity(tableName = "yearly_expense_categories")
data class YearlyExpenseCategory(
    @PrimaryKey val category: String,
    val amount2026: Double = 0.0
)

@Entity(tableName = "major_works")
data class MajorWork(
    @PrimaryKey val description: String,
    val amount2026: Double = 0.0
)

@Entity(tableName = "owner_contacts")
data class OwnerContact(
    @PrimaryKey val flatNo: String,
    val residentName: String = "",
    val primaryContactNo: String = "",
    val emergencyContactNo: String = ""
)

@Entity(tableName = "service_contacts")
data class ServiceContact(
    @PrimaryKey val serviceType: String,
    val contactPerson: String = "",
    val phoneNo: String = "",
    val remarks: String = ""
)

@Entity(tableName = "google_sheet_config")
data class GoogleSheetConfig(
    @PrimaryKey val id: Int = 1,
    val spreadsheetTitle: String = "Apartment Maintenance Ledger",
    val spreadsheetId: String = "",
    val gcpProjectId: String = "",
    val serviceAccountEmail: String = "",
    val apiKey: String = "",
    val webClientId: String = "",
    val userEmail: String = "",
    val isLoggedIn: Boolean = false,
    val isReadOnly: Boolean = true,
    val lastSyncTime: Long = 0L
)

fun extractSpreadsheetId(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    val pattern = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
    val match = pattern.find(trimmed)
    return match?.groupValues?.get(1) ?: trimmed
}

fun extractGid(input: String): String? {
    val trimmed = input.trim()
    val pattern = Regex("[?&#]gid=([0-9]+)")
    val match = pattern.find(trimmed)
    return match?.groupValues?.get(1)
}

fun getDefaultSheetLinkEnv(): String {
    return try {
        val link = com.example.BuildConfig.DEFAULT_SHEET_LINK
        if (link.isNullOrBlank() ||
            link == "DEFAULT_SHEET_LINK_PLACEHOLDER" ||
            link.contains("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms")
        ) "" else link.trim()
    } catch (e: Exception) {
        ""
    }
}

