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
    version = 3,
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
                        if (currentConfig == null) {
                            populateInitialData(database)
                        } else {
                            // Reset cached login state on launch so real-time Google Sign-In flow is requested
                            database.configDao().saveConfig(currentConfig.copy(userEmail = "", isLoggedIn = false))
                        }
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Save initial Google Sheet Config without pre-logged mock state
            db.configDao().saveConfig(
                GoogleSheetConfig(
                    id = 1,
                    spreadsheetTitle = "Gomathi Ilam Thendral - Maintenance Record Book",
                    spreadsheetId = "",
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
        }
    }
}
