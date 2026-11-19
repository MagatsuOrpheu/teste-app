package com.jhowww.meufinanceiro.domain

import com.jhowww.meufinanceiro.domain.model.AlertLevel
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.CategorySpending
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.Insight
import com.jhowww.meufinanceiro.domain.model.MonthSummary
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.domain.model.Transaction
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** Toda a regra de negócio do controle financeiro mora aqui (sem Android), o que facilita testar. */
object FinanceCalculator {

    /** Acima disso os gastos fixos começam a apertar (referência da regra 50/30/20). */
    const val FIXED_HEALTHY_LIMIT = 0.5f
    const val COMMITTED_WARNING = 0.9f
    const val CATEGORY_WARNING = 0.8f

    fun summarize(
        month: YearMonth,
        today: LocalDate,
        recurring: List<RecurringItem>,
        paidRecurringIds: Set<Long>,
        transactions: List<Transaction>,
        categories: List<Category>,
    ): MonthSummary {
        val activeRecurring = recurring.filter { it.isActiveIn(month) }
        val monthTx = transactions.filter { YearMonth.from(it.date) == month }

        val recurringIncome = activeRecurring.filter { it.type == EntryType.INCOME }.sumOf { it.amountCents }
        val fixedExpenses = activeRecurring.filter { it.type == EntryType.EXPENSE }
        val fixedTotal = fixedExpenses.sumOf { it.amountCents }
        val fixedPaid = fixedExpenses.filter { it.id in paidRecurringIds }.sumOf { it.amountCents }
        val extraIncome = monthTx.filter { it.type == EntryType.INCOME }.sumOf { it.amountCents }
        val variableExpenses = monthTx.filter { it.type == EntryType.EXPENSE }
        val variableTotal = variableExpenses.sumOf { it.amountCents }

        val currentMonth = YearMonth.from(today)
        val daysLeft = when {
            month == currentMonth -> month.lengthOfMonth() - today.dayOfMonth + 1
            month.isAfter(currentMonth) -> month.lengthOfMonth()
            else -> 0
        }

        val byCategory = categoryBreakdown(fixedExpenses, variableExpenses, categories)

        val base = MonthSummary(
            month = month,
            recurringIncomeCents = recurringIncome,
            extraIncomeCents = extraIncome,
            fixedExpensesCents = fixedTotal,
            fixedPaidCents = fixedPaid,
            variableExpensesCents = variableTotal,
            daysLeft = daysLeft,
            byCategory = byCategory,
        )
        val daily = if (daysLeft > 0) maxOf(0L, base.balanceCents) / daysLeft else null

        val withDaily = base.copy(dailyAllowanceCents = daily)
        return withDaily.copy(
            insights = buildInsights(withDaily, today, fixedExpenses, paidRecurringIds),
        )
    }

    private fun categoryBreakdown(
        fixed: List<RecurringItem>,
        variable: List<Transaction>,
        categories: List<Category>,
    ): List<CategorySpending> {
        val byId = categories.associateBy { it.id }
        val ids = (fixed.map { it.categoryId } + variable.map { it.categoryId }).toSet()
        return ids.map { id ->
            CategorySpending(
                category = id?.let { byId[it] },
                fixedCents = fixed.filter { it.categoryId == id }.sumOf { it.amountCents },
                variableCents = variable.filter { it.categoryId == id }.sumOf { it.amountCents },
            )
        }
            .filter { it.totalCents > 0 }
            .sortedByDescending { it.totalCents }
    }

    private fun pct(f: Float): String = "${(f * 100).roundToInt()}%"

    private fun buildInsights(
        s: MonthSummary,
        today: LocalDate,
        fixedExpenses: List<RecurringItem>,
        paidIds: Set<Long>,
    ): List<Insight> {
        val list = mutableListOf<Insight>()
        val currentMonth = YearMonth.from(today)
        val isPast = s.month.isBefore(currentMonth)

        if (s.totalIncomeCents == 0L) {
            list += Insight(AlertLevel.INFO, "Cadastre sua renda mensal na aba Fixos para o app calcular seu saldo.")
            return list
        }

        if (s.balanceCents < 0) {
            list += Insight(
                AlertLevel.DANGER,
                "Você está ${Money.format(-s.balanceCents)} acima da sua renda neste mês.",
            )
        } else if (s.committedFraction >= COMMITTED_WARNING) {
            list += Insight(
                AlertLevel.WARNING,
                "Atenção: ${pct(s.committedFraction)} da renda já está comprometida.",
            )
        }

        if (s.fixedFraction > FIXED_HEALTHY_LIMIT) {
            list += Insight(
                AlertLevel.WARNING,
                "Seus gastos fixos levam ${pct(s.fixedFraction)} da renda. O recomendado é até 50%.",
            )
        }

        if (s.month == currentMonth) {
            fixedExpenses
                .filter { it.id !in paidIds && it.effectiveDay(s.month) < today.dayOfMonth }
                .sortedBy { it.dayOfMonth }
                .forEach {
                    list += Insight(
                        AlertLevel.DANGER,
                        "${it.name} venceu dia ${it.effectiveDay(s.month)} e não está marcado como pago.",
                    )
                }
            fixedExpenses
                .filter { it.id !in paidIds && it.effectiveDay(s.month) in today.dayOfMonth..(today.dayOfMonth + 3) }
                .sortedBy { it.dayOfMonth }
                .forEach {
                    val d = it.effectiveDay(s.month)
                    val quando = if (d == today.dayOfMonth) "vence hoje" else "vence dia $d"
                    list += Insight(AlertLevel.WARNING, "${it.name} ($quando): ${Money.format(it.amountCents)}.")
                }
        }

        s.byCategory.forEach { c ->
            val usage = c.limitUsage ?: return@forEach
            val name = c.category?.name ?: return@forEach
            if (usage > 1f) {
                list += Insight(
                    AlertLevel.DANGER,
                    "$name passou do limite em ${Money.format(c.totalCents - c.limitCents)}.",
                )
            } else if (usage >= CATEGORY_WARNING) {
                list += Insight(AlertLevel.WARNING, "$name já usou ${pct(usage)} do limite do mês.")
            }
        }

        if (isPast && s.balanceCents > 0) {
            list += Insight(AlertLevel.GOOD, "Neste mês sobraram ${Money.format(s.balanceCents)}. Boa!")
        } else if (!isPast && s.balanceCents > 0 && list.none { it.level != AlertLevel.GOOD }) {
            list += Insight(AlertLevel.GOOD, "Tudo sob controle. Continue assim!")
        }
        return list
    }

    /** Resumo de vários meses (para o histórico). Não considera "pago/pendente". */
    fun history(
        months: List<YearMonth>,
        today: LocalDate,
        recurring: List<RecurringItem>,
        transactions: List<Transaction>,
        categories: List<Category>,
    ): List<MonthSummary> = months.map {
        summarize(it, today, recurring, emptySet(), transactions, categories).copy(insights = emptyList())
    }
}
