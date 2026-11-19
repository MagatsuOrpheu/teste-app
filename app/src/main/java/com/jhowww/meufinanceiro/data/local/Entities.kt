package com.jhowww.meufinanceiro.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val monthlyLimitCents: Long = 0,
)

/** type = "INCOME" ou "EXPENSE"; meses no formato "yyyy-MM". */
@Entity(tableName = "recurring")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val type: String,
    val categoryId: Long?,
    val dayOfMonth: Int,
    val startMonth: String,
    val endMonth: String?,
)

/** Marca que um gasto fixo foi pago em determinado mês. */
@Entity(tableName = "recurring_paid", primaryKeys = ["recurringId", "month"])
data class RecurringPaidEntity(
    val recurringId: Long,
    val month: String,
)

@Entity(tableName = "transactions", indices = [Index("month")])
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val type: String,
    val categoryId: Long?,
    /** LocalDate.toEpochDay() */
    val epochDay: Long,
    val month: String,
)
