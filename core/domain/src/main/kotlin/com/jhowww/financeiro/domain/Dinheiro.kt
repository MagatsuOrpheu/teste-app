package com.jhowww.financeiro.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Valor monetário em centavos de real. Toda conta do app é feita em inteiros,
 * para não haver erro de arredondamento de ponto flutuante.
 */
@JvmInline
value class Dinheiro(val centavos: Long) : Comparable<Dinheiro> {

    operator fun plus(outro: Dinheiro) = Dinheiro(Math.addExact(centavos, outro.centavos))
    operator fun minus(outro: Dinheiro) = Dinheiro(Math.subtractExact(centavos, outro.centavos))
    operator fun times(fator: Int) = Dinheiro(Math.multiplyExact(centavos, fator.toLong()))
    operator fun unaryMinus() = Dinheiro(Math.negateExact(centavos))
    override fun compareTo(other: Dinheiro): Int = centavos.compareTo(other.centavos)

    val ehNegativo: Boolean get() = centavos < 0
    val ehZero: Boolean get() = centavos == 0L

    /** "R$ 1.234,56" (negativos como "-R$ 1.234,56"). */
    fun formatar(): String {
        val texto = formato.format(BigDecimal.valueOf(kotlin.math.abs(centavos)).movePointLeft(2))
            .replace(' ', ' ')
        return if (centavos < 0) "-$texto" else texto
    }

    override fun toString(): String = formatar()

    companion object {
        val ZERO = Dinheiro(0)
        private val localeBR: Locale = Locale.forLanguageTag("pt-BR")
        private val formato: NumberFormat = NumberFormat.getCurrencyInstance(localeBR)

        fun reais(valor: Long) = Dinheiro(Math.multiplyExact(valor, 100L))

        /**
         * Converte o texto digitado em valor. Aceita "1500", "1.500", "1.500,90", "1500,9",
         * "1500.90" e "R$ 20". Retorna null se não for um valor válido ou se for negativo.
         */
        fun interpretar(texto: String): Dinheiro? {
            var s = texto.trim().replace("R$", "").replace(" ", "").replace(" ", "")
            if (s.isEmpty()) return null
            if (s.contains(',')) {
                s = s.replace(".", "").replace(',', '.')
            } else if (s.contains('.')) {
                val partes = s.split('.')
                val ehMilhar = partes.size > 2 || partes.last().length == 3
                if (ehMilhar) s = s.replace(".", "")
            }
            val valor = s.toBigDecimalOrNull() ?: return null
            if (valor.signum() < 0) return null
            return Dinheiro(valor.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact())
        }
    }
}

fun Iterable<Dinheiro>.soma(): Dinheiro = fold(Dinheiro.ZERO) { acc, d -> acc + d }
