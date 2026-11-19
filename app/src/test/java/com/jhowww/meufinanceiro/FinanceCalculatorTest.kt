package com.jhowww.meufinanceiro

import com.jhowww.meufinanceiro.domain.FinanceCalculator
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.AlertLevel
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FinanceCalculatorTest {

    private val nov = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 19)
    private val moradia = Category(1, "Moradia", 0xFF000000, 0)
    private val lazer = Category(2, "Lazer", 0xFF000000, 30000)

    private val salario = RecurringItem(1, "Salário", 300000, EntryType.INCOME, null, 5, YearMonth.of(2026, 1))
    private val aluguel = RecurringItem(2, "Aluguel", 120000, EntryType.EXPENSE, 1, 10, YearMonth.of(2026, 1))
    private val internet = RecurringItem(3, "Internet", 10000, EntryType.EXPENSE, 1, 25, YearMonth.of(2026, 1))
    private val antigo = RecurringItem(4, "Curso", 20000, EntryType.EXPENSE, null, 1,
        YearMonth.of(2026, 1), YearMonth.of(2026, 10))

    private fun tx(value: Long, day: Int, cat: Long? = 2, type: EntryType = EntryType.EXPENSE) =
        Transaction(0, "x", value, type, cat, LocalDate.of(2026, 11, day))

    @Test
    fun totaisDoMes() {
        val s = FinanceCalculator.summarize(
            nov, today, listOf(salario, aluguel, internet, antigo), setOf(2L),
            listOf(tx(5000, 3), tx(20000, 15), tx(50000, 10, null, EntryType.INCOME),
                Transaction(0, "outubro", 9999, EntryType.EXPENSE, 2, LocalDate.of(2026, 10, 3))),
            listOf(moradia, lazer),
        )
        assertEquals(300000L, s.recurringIncomeCents)
        assertEquals(50000L, s.extraIncomeCents)
        assertEquals(130000L, s.fixedExpensesCents) // curso terminou em outubro
        assertEquals(120000L, s.fixedPaidCents)
        assertEquals(10000L, s.fixedPendingCents)
        assertEquals(25000L, s.variableExpensesCents)
        assertEquals(195000L, s.balanceCents)
        assertEquals(12, s.daysLeft) // 19..30
        assertEquals(195000L / 12, s.dailyAllowanceCents)
        assertEquals("Moradia", s.byCategory.first().category?.name)
    }

    @Test
    fun alertaDeContaVencidaENaoPaga() {
        val s = FinanceCalculator.summarize(nov, today, listOf(salario, aluguel), emptySet(), emptyList(), listOf(moradia))
        assertTrue(s.insights.any { it.level == AlertLevel.DANGER && it.message.contains("Aluguel venceu dia 10") })
    }

    @Test
    fun alertaDeLimiteDeCategoria() {
        val s = FinanceCalculator.summarize(nov, today, listOf(salario), emptySet(),
            listOf(tx(35000, 2)), listOf(lazer))
        assertTrue(s.insights.any { it.message.startsWith("Lazer passou do limite") })
    }

    @Test
    fun saldoNegativoEMesPassado() {
        val out = YearMonth.of(2026, 10)
        val s = FinanceCalculator.summarize(out, today, listOf(salario),
            emptySet(), listOf(Transaction(0, "x", 400000, EntryType.EXPENSE, null, LocalDate.of(2026, 10, 1))), emptyList())
        assertEquals(-100000L, s.balanceCents)
        assertNull(s.dailyAllowanceCents)
        assertTrue(s.insights.first().level == AlertLevel.DANGER)
    }

    @Test
    fun semRenda() {
        val s = FinanceCalculator.summarize(nov, today, emptyList(), emptySet(), emptyList(), emptyList())
        assertEquals(AlertLevel.INFO, s.insights.single().level)
    }

    @Test
    fun parseDeValores() {
        assertEquals(150000L, Money.parse("1500"))
        assertEquals(150000L, Money.parse("1.500"))
        assertEquals(150090L, Money.parse("1.500,90"))
        assertEquals(150090L, Money.parse("1500,9"))
        assertEquals(150090L, Money.parse("1500.90"))
        assertEquals(2000L, Money.parse("R$ 20"))
        assertEquals(100000000L, Money.parse("1.000.000"))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("-5"))
    }

    @Test
    fun formatacao() {
        assertEquals("R$ 1.234,56", Money.format(123456))
        assertEquals("1234,56", Money.toInput(123456))
    }
}
