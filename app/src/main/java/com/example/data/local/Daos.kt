package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection_records ORDER BY id ASC")
    fun getAllCollectionRecords(): Flow<List<CollectionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionRecords(records: List<CollectionRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionRecord(record: CollectionRecord)

    @Update
    suspend fun updateCollectionRecord(record: CollectionRecord)

    @Query("DELETE FROM collection_records WHERE id = :id")
    suspend fun deleteCollectionRecord(id: Int)

    @Query("DELETE FROM collection_records")
    suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_records ORDER BY id ASC")
    fun getAllExpenseRecords(): Flow<List<ExpenseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseRecords(records: List<ExpenseRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(record: ExpenseRecord)

    @Query("DELETE FROM expense_records WHERE id = :id")
    suspend fun deleteExpense(id: Int)

    @Query("DELETE FROM expense_records")
    suspend fun clearAll()
}

@Dao
interface YearlyReportDao {
    @Query("SELECT * FROM yearly_contributions ORDER BY flatNo ASC")
    fun getContributions(): Flow<List<YearlyContribution>>

    @Query("SELECT * FROM yearly_expense_categories")
    fun getExpenseCategories(): Flow<List<YearlyExpenseCategory>>

    @Query("SELECT * FROM major_works")
    fun getMajorWorks(): Flow<List<MajorWork>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<YearlyContribution>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategories(categories: List<YearlyExpenseCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMajorWorks(works: List<MajorWork>)

    @Query("DELETE FROM yearly_contributions")
    suspend fun clearContributions()

    @Query("DELETE FROM yearly_expense_categories")
    suspend fun clearCategories()

    @Query("DELETE FROM major_works")
    suspend fun clearMajorWorks()
}

@Dao
interface ContactsDao {
    @Query("SELECT * FROM owner_contacts ORDER BY flatNo ASC")
    fun getOwnerContacts(): Flow<List<OwnerContact>>

    @Query("SELECT * FROM service_contacts")
    fun getServiceContacts(): Flow<List<ServiceContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerContacts(contacts: List<OwnerContact>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceContacts(contacts: List<ServiceContact>)

    @Query("DELETE FROM owner_contacts")
    suspend fun clearOwners()

    @Query("DELETE FROM service_contacts")
    suspend fun clearServices()
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM google_sheet_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<GoogleSheetConfig?>

    @Query("SELECT * FROM google_sheet_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): GoogleSheetConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: GoogleSheetConfig)
}
