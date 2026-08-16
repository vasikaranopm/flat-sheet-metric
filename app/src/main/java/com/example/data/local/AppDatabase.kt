package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CollectionRecord::class,
        ExpenseRecord::class,
        YearlyContribution::class,
        YearlyExpenseCategory::class,
        MajorWork::class,
        OwnerContact::class,
        ServiceContact::class,
        GoogleSheetConfig::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun yearlyReportDao(): YearlyReportDao
    abstract fun contactsDao(): ContactsDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "git_maintenance_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentConfig = database.configDao().getConfigSync()
                        val defaultSheetId = extractSpreadsheetId(getDefaultSheetLinkEnv())
                        if (currentConfig == null) {
                            populateInitialData(database)
                        } else if (currentConfig.spreadsheetId.contains("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms")) {
                            // Purge old dummy spreadsheet id and dummy records
                            database.expenseDao().clearAll()
                            database.yearlyReportDao().clearContributions()
                            database.yearlyReportDao().clearCategories()
                            database.yearlyReportDao().clearMajorWorks()
                            database.contactsDao().clearOwners()
                            database.contactsDao().clearServices()
                            database.collectionDao().clearAll()
                            database.configDao().saveConfig(currentConfig.copy(spreadsheetId = "", spreadsheetTitle = "Apartment Maintenance Ledger", lastSyncTime = 0L))
                        } else if (currentConfig.spreadsheetId.isBlank() && defaultSheetId.isNotBlank()) {
                            val updatedConfig = currentConfig.copy(spreadsheetId = defaultSheetId)
                            database.configDao().saveConfig(updatedConfig)
                        }
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val defaultSheetId = extractSpreadsheetId(getDefaultSheetLinkEnv())
            // Save initial Google Sheet Config with preloaded env sheet link if present
            db.configDao().saveConfig(
                GoogleSheetConfig(
                    id = 1,
                    spreadsheetTitle = "Gomathi Ilam Thendral",
                    spreadsheetId = defaultSheetId,
                    gcpProjectId = "",
                    serviceAccountEmail = "",
                    apiKey = "",
                    webClientId = "",
                    userEmail = "",
                    isLoggedIn = false,
                    isReadOnly = true,
                    lastSyncTime = 0L
                )
            )

            // Seed initial 6 Owners of Gomathi Ilam Thendral
            db.contactsDao().insertOwnerContacts(
                listOf(
                    OwnerContact(flatNo = "1A", residentName = "M.Madhan Raj", primaryContactNo = "", emergencyContactNo = ""),
                    OwnerContact(flatNo = "1B", residentName = "S.Vasikaran", primaryContactNo = "9940381669", emergencyContactNo = ""),
                    OwnerContact(flatNo = "2A", residentName = "S. Hariprasad", primaryContactNo = "", emergencyContactNo = ""),
                    OwnerContact(flatNo = "2B", residentName = "P.Seenivasan", primaryContactNo = "", emergencyContactNo = ""),
                    OwnerContact(flatNo = "3A", residentName = "A. Venkatesh Kumar", primaryContactNo = "", emergencyContactNo = ""),
                    OwnerContact(flatNo = "3B", residentName = "M.Mohan", primaryContactNo = "", emergencyContactNo = "")
                )
            )

            // Seed initial Monthly Collection Records as per actual structure
            db.collectionDao().insertCollectionRecords(
                listOf(
                    CollectionRecord(
                        id = 1,
                        year = "2026",
                        month = "July",
                        particulars = "Monthly Maintenance",
                        remarks = "1,000 for Regular Maintenace\n1,000 for Motor Sensor Contribution",
                        flat1AAmount = 2000.0,
                        flat1BAmount = 2000.0,
                        flat2AAmount = 2000.0,
                        flat2BAmount = 2000.0,
                        flat3AAmount = 2000.0,
                        flat3BAmount = 2000.0,
                        totalAmount = 12000.0
                    ),
                    CollectionRecord(
                        id = 2,
                        year = "2026",
                        month = "August",
                        particulars = "Monthly Maintenance",
                        remarks = "Details",
                        flat1AAmount = 2400.0,
                        flat1BAmount = 2400.0,
                        flat2AAmount = 2400.0,
                        flat2BAmount = 2400.0,
                        flat3AAmount = 2400.0,
                        flat3BAmount = 2400.0,
                        totalAmount = 14400.0
                    )
                )
            )

            // Seed Yearly Contributions
            db.yearlyReportDao().insertContributions(
                listOf(
                    YearlyContribution(flatNo = "1A", residentName = "M.Madhan Raj", amount2026 = 4400.0),
                    YearlyContribution(flatNo = "1B", residentName = "S.Vasikaran", amount2026 = 4400.0),
                    YearlyContribution(flatNo = "2A", residentName = "S. Hariprasad", amount2026 = 4400.0),
                    YearlyContribution(flatNo = "2B", residentName = "P.Seenivasan", amount2026 = 4400.0),
                    YearlyContribution(flatNo = "3A", residentName = "A. Venkatesh Kumar", amount2026 = 4400.0),
                    YearlyContribution(flatNo = "3B", residentName = "M.Mohan", amount2026 = 4400.0)
                )
            )
        }
    }
}
