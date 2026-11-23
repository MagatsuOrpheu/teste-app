package com.jhowww.financeiro.ui.comum

import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val LocaleBR: Locale = Locale.forLanguageTag("pt-BR")

private val DiaMes = DateTimeFormatter.ofPattern("dd/MM")
private val DiaMesAno = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "Novembro de 2026" */
fun Competencia.nome(): String =
    mes.month.getDisplayName(TextStyle.FULL, LocaleBR).replaceFirstChar { it.uppercase(LocaleBR) } + " de " + mes.year

/** "nov/26" */
fun Competencia.curta(): String =
    mes.month.getDisplayName(TextStyle.SHORT, LocaleBR).trimEnd('.') + "/" + (mes.year % 100).toString().padStart(2, '0')

fun LocalDate.curta(): String = format(DiaMes)
fun LocalDate.completa(): String = format(DiaMesAno)

/** Pontos-base como percentual: 6000 → "60%", 6050 → "60,5%". */
fun Int.comoPercentual(): String {
    val inteiro = this / 100
    val resto = kotlin.math.abs(this % 100)
    return if (resto == 0) "$inteiro%" else "$inteiro,${resto.toString().padStart(2, '0').trimEnd('0')}%"
}

/** Valor para preencher um campo de edição: "1234,50". */
fun Dinheiro.paraCampo(): String {
    val abs = kotlin.math.abs(centavos)
    val texto = "${abs / 100},${(abs % 100).toString().padStart(2, '0')}"
    return if (centavos < 0) "-$texto" else texto
}

/** Eixo do gráfico: "R$ 1,2 mil", "R$ 850", "-R$ 300". */
fun formatarCompacto(reais: Double): String {
    val abs = kotlin.math.abs(reais)
    val sinal = if (reais < 0) "-" else ""
    return when {
        abs >= 1_000_000 -> sinal + "R$ " + String.format(LocaleBR, "%.1f", abs / 1_000_000) + " mi"
        abs >= 1_000 -> sinal + "R$ " + String.format(LocaleBR, "%.1f", abs / 1_000) + " mil"
        else -> sinal + "R$ " + abs.toLong()
    }
}

/** Percentual digitado ("10", "12,5") em pontos-base; null se inválido. */
fun interpretarPercentual(texto: String): Long? {
    val v = texto.trim().replace("%", "").replace(',', '.').toBigDecimalOrNull() ?: return null
    if (v.signum() < 0 || v > java.math.BigDecimal(100)) return null
    return v.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
}

/** Pontos-base para campo de edição: 1250 → "12,5". */
fun Long.bpParaCampo(): String = toInt().comoPercentual().removeSuffix("%")
