package com.jhowww.financeiro.domain

import java.time.LocalDate
import java.time.YearMonth

/** Mês de competência financeira ("AAAA-MM"), independente da data de pagamento. */
@JvmInline
value class Competencia(val mes: YearMonth) : Comparable<Competencia> {

    val chave: String get() = mes.toString()
    val dias: Int get() = mes.lengthOfMonth()

    fun mais(meses: Long) = Competencia(mes.plusMonths(meses))
    fun anterior() = mais(-1)
    fun proxima() = mais(1)

    /** Dia efetivo no mês: vencimento no dia 31 vira 30, 29 ou 28 conforme o mês. */
    fun diaEfetivo(dia: Int): LocalDate = mes.atDay(dia.coerceIn(1, dias))

    /** Quantos meses separam esta competência de [outra] (positivo se esta for depois). */
    fun mesesDesde(outra: Competencia): Long =
        (mes.year - outra.mes.year) * 12L + (mes.monthValue - outra.mes.monthValue)

    override fun compareTo(other: Competencia): Int = mes.compareTo(other.mes)
    override fun toString(): String = chave

    companion object {
        fun de(ano: Int, mes: Int) = Competencia(YearMonth.of(ano, mes))
        fun de(data: LocalDate) = Competencia(YearMonth.from(data))
        fun interpretar(chave: String) = Competencia(YearMonth.parse(chave))
    }
}
