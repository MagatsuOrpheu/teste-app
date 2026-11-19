package com.jhowww.meufinanceiro.domain.model

import java.time.LocalDate
import java.time.YearMonth

/** Valores monetários são sempre guardados em centavos (Long) para evitar erros de arredondamento. */

enum class EntryType { INCOME, EXPENSE }

data class Category(
    val id: Long = 0,
    val name: String,
    val color: Long,
    /** Limite mensal em centavos. 0 = sem limite. */
    val monthlyLimitCents: Long = 0,
)

/** Item que se repete todo mês: salário, aluguel, internet, academia... */
data class RecurringItem(
    val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val type: EntryType,
    val categoryId: Long? = null,
    /** Dia do mês em que entra/vence (1..31). */
    val dayOfMonth: Int = 1,
    val startMonth: YearMonth,
    /** Último mês em que vale. null = continua valendo. */
    val endMonth: YearMonth? = null,
) {
    fun isActiveIn(month: YearMonth): Boolean =
        !startMonth.isAfter(month) && (endMonth == null || !month.isAfter(endMonth))

    /** Dia efetivo no mês (ex.: dia 31 em fevereiro vira 28/29). */
    fun effectiveDay(month: YearMonth): Int = dayOfMonth.coerceIn(1, month.lengthOfMonth())
}

/** Lançamento avulso: um gasto do dia a dia ou uma renda extra. */
data class Transaction(
    val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val type: EntryType,
    val categoryId: Long? = null,
    val date: LocalDate,
)

data class CategorySpending(
    val category: Category?,
    val fixedCents: Long,
    val variableCents: Long,
) {
    val totalCents: Long get() = fixedCents + variableCents
    val limitCents: Long get() = category?.monthlyLimitCents ?: 0
    /** Fração do limite usada (null se a categoria não tem limite). */
    val limitUsage: Float? get() = if (limitCents > 0) totalCents.toFloat() / limitCents else null
}

enum class AlertLevel { INFO, WARNING, DANGER, GOOD }

data class Insight(val level: AlertLevel, val message: String)

data class MonthSummary(
    val month: YearMonth,
    val recurringIncomeCents: Long = 0,
    val extraIncomeCents: Long = 0,
    val fixedExpensesCents: Long = 0,
    val fixedPaidCents: Long = 0,
    val variableExpensesCents: Long = 0,
    val daysLeft: Int = 0,
    /** Quanto dá para gastar por dia até o fim do mês (null para meses passados). */
    val dailyAllowanceCents: Long? = null,
    val byCategory: List<CategorySpending> = emptyList(),
    val insights: List<Insight> = emptyList(),
) {
    val totalIncomeCents: Long get() = recurringIncomeCents + extraIncomeCents
    val totalExpensesCents: Long get() = fixedExpensesCents + variableExpensesCents
    val fixedPendingCents: Long get() = fixedExpensesCents - fixedPaidCents
    /** Saldo livre: renda - fixos (pagos ou não) - variáveis. */
    val balanceCents: Long get() = totalIncomeCents - totalExpensesCents
    /** Parte da renda já comprometida (0.0 a 1.0+). */
    val committedFraction: Float
        get() = if (totalIncomeCents > 0) totalExpensesCents.toFloat() / totalIncomeCents else 0f
    val fixedFraction: Float
        get() = if (totalIncomeCents > 0) fixedExpensesCents.toFloat() / totalIncomeCents else 0f
}
