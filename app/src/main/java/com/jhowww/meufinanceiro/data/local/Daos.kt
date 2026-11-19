package com.jhowww.meufinanceiro.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(item: CategoryEntity): Long

    @Update
    suspend fun update(item: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY dayOfMonth, name COLLATE NOCASE")
    fun observeAll(): Flow<List<RecurringEntity>>

    @Insert
    suspend fun insert(item: RecurringEntity): Long

    @Update
    suspend fun update(item: RecurringEntity)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE recurring SET endMonth = :endMonth WHERE id = :id")
    suspend fun setEndMonth(id: Long, endMonth: String)

    @Query("UPDATE recurring SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("SELECT recurringId FROM recurring_paid WHERE month = :month")
    fun observePaid(month: String): Flow<List<Long>>

    @Query("SELECT month FROM recurring_paid WHERE recurringId = :id")
    suspend fun paidMonthsFor(id: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markPaid(item: RecurringPaidEntity)

    @Delete
    suspend fun unmarkPaid(item: RecurringPaidEntity)

    @Query("DELETE FROM recurring_paid WHERE recurringId = :id")
    suspend fun deletePaidFor(id: Long)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE month = :month ORDER BY epochDay DESC, id DESC")
    fun observeByMonth(month: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE month BETWEEN :from AND :to")
    fun observeBetween(from: String, to: String): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(item: TransactionEntity): Long

    @Update
    suspend fun update(item: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)
}
