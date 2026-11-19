package com.jhowww.meufinanceiro.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")

object Money {
    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(LocaleBR)

    /** 123456 -> "R$ 1.234,56" */
    fun format(cents: Long): String =
        currency.format(BigDecimal.valueOf(cents).movePointLeft(2)).replace(' ', ' ')

    /** Formato para preencher campo de edição: 123456 -> "1234,56" */
    fun toInput(cents: Long): String {
        if (cents == 0L) return ""
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2).toPlainString().replace('.', ',')
    }

    /**
     * Converte texto digitado em centavos. Aceita: "1500", "1.500", "1.500,90", "1500,9", "1500.90", "R$ 20".
     * Retorna null se não for um valor válido.
     */
    fun parse(input: String): Long? {
        var s = input.trim().replace("R$", "").replace(" ", "").replace(" ", "")
        if (s.isEmpty()) return null
        if (s.contains(',')) {
            s = s.replace(".", "").replace(',', '.')
        } else if (s.contains('.')) {
            val parts = s.split('.')
            // "1.500" ou "1.500.000" -> separador de milhar (padrão brasileiro)
            val isThousands = parts.size > 2 || parts.last().length == 3
            if (isThousands) s = s.replace(".", "")
        }
        if (s.count { it == '.' } > 1) return null
        val value = s.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
    }
}

object Dates {
    private val dayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", LocaleBR)
    private val shortDay = DateTimeFormatter.ofPattern("dd/MM", LocaleBR)

    fun format(date: LocalDate): String = date.format(dayFormat)
    fun formatShort(date: LocalDate): String = date.format(shortDay)

    /** "novembro de 2026" -> "Novembro 2026" */
    fun monthLabel(month: YearMonth): String {
        val name = month.month.getDisplayName(TextStyle.FULL, LocaleBR)
        return name.replaceFirstChar { it.titlecase(LocaleBR) } + " " + month.year
    }

    fun monthShort(month: YearMonth): String {
        val name = month.month.getDisplayName(TextStyle.SHORT, LocaleBR).removeSuffix(".")
        return name.replaceFirstChar { it.titlecase(LocaleBR) }
    }

    /** Serialização para o banco: "2026-11" */
    fun monthKey(month: YearMonth): String = month.toString()
    fun parseMonthKey(key: String): YearMonth = YearMonth.parse(key)
}
