package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_records")
data class CollectionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: String = "2026",
    val month: String = "July",
    val particulars: String = "Monthly Maintenance",
    val remarks: String = "1,000 for Regular Maintenance\n1,000 for Motor Sensor Contribution",
    val flat1AAmount: Double = 2000.0,
    val flat1BAmount: Double = 2000.0,
    val flat2AAmount: Double = 2000.0,
    val flat2BAmount: Double = 2000.0,
    val flat3AAmount: Double = 2000.0,
    val flat3BAmount: Double = 2000.0,
    val totalAmount: Double = 12000.0
)

@Entity(tableName = "expense_records")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: String = "2026",
    val month: String = "July",
    val dateDay: String,
    val particulars: String,
    val remarks: String,
    val amount: Double,
    val vendorPayee: String,
    val billAvailable: String = "N/A",
    val picture: String = "N/A",
    val balance: Double,
    val category: String
)

@Entity(tableName = "yearly_contributions")
data class YearlyContribution(
    @PrimaryKey val flatNo: String,
    val residentName: String,
    val amount2026: Double
)

@Entity(tableName = "yearly_expense_categories")
data class YearlyExpenseCategory(
    @PrimaryKey val category: String,
    val amount2026: Double
)

@Entity(tableName = "major_works")
data class MajorWork(
    @PrimaryKey val description: String,
    val amount2026: Double
)

@Entity(tableName = "owner_contacts")
data class OwnerContact(
    @PrimaryKey val flatNo: String,
    val residentName: String,
    val primaryContactNo: String,
    val emergencyContactNo: String = ""
)

@Entity(tableName = "service_contacts")
data class ServiceContact(
    @PrimaryKey val serviceType: String,
    val contactPerson: String,
    val phoneNo: String,
    val remarks: String
)

@Entity(tableName = "google_sheet_config")
data class GoogleSheetConfig(
    @PrimaryKey val id: Int = 1,
    val spreadsheetTitle: String = "Gomathi Ilam Thendral - Maintenance Record Book",
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
