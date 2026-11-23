package com.jhowww.financeiro.ui.relatorios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.ui.comum.PontoCor
import com.jhowww.financeiro.ui.comum.formatarCompacto
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.grouped
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

/** Uma série de um gráfico: nome (legenda), cor e um valor em reais por rótulo do eixo X. */
data class Serie(val nome: String, val cor: Color, val valores: List<Double>)

private val FormatoReais = CartesianValueFormatter { _, valor, _ -> formatarCompacto(valor) }

@Composable
private fun formatoRotulos(rotulos: List<String>): CartesianValueFormatter = remember(rotulos) {
    CartesianValueFormatter { _, valor, _ -> rotulos.getOrNull(valor.toInt()) ?: "" }
}

@Composable
fun Legenda(series: List<Serie>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PontoCor(s.cor)
                Text(s.nome, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** Colunas por mês, lado a lado ou empilhadas. */
@Composable
fun GraficoColunas(rotulos: List<String>, conjunto: List<Serie>, empilhado: Boolean = false, modifier: Modifier = Modifier) {
    if (rotulos.isEmpty() || conjunto.isEmpty()) return
    val produtor = remember { CartesianChartModelProducer() }
    LaunchedEffect(rotulos, conjunto) {
        produtor.runTransaction {
            columnSeries { conjunto.forEach { s -> series(rotulos.indices.toList(), s.valores) } }
        }
    }
    val colunas = conjunto.map { s -> rememberLineComponent(fill(s.cor), if (empilhado) 18.dp else 10.dp) }
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(colunas),
                    mergeMode = { if (empilhado) ColumnCartesianLayer.MergeMode.Stacked else ColumnCartesianLayer.MergeMode.grouped() },
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = FormatoReais),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = formatoRotulos(rotulos)),
            ),
            modelProducer = produtor,
            modifier = modifier.fillMaxWidth().height(200.dp),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
    Legenda(conjunto)
}

/** Linhas por mês, uma por série. */
@Composable
fun GraficoLinhas(rotulos: List<String>, conjunto: List<Serie>, modifier: Modifier = Modifier) {
    if (rotulos.isEmpty() || conjunto.isEmpty()) return
    val produtor = remember { CartesianChartModelProducer() }
    LaunchedEffect(rotulos, conjunto) {
        produtor.runTransaction {
            lineSeries { conjunto.forEach { s -> series(rotulos.indices.toList(), s.valores) } }
        }
    }
    val linhas = conjunto.map { s -> LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(fill(s.cor))) }
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(lineProvider = LineCartesianLayer.LineProvider.series(linhas)),
                startAxis = VerticalAxis.rememberStart(valueFormatter = FormatoReais),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = formatoRotulos(rotulos),
                    itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 2 }) },
                ),
            ),
            modelProducer = produtor,
            modifier = modifier.fillMaxWidth().height(200.dp),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
    Legenda(conjunto)
}

/** Barra horizontal com a parte de cada valor em relação ao total (usada em categorias e no comprometimento). */
@Composable
fun BarraProporcional(partes: List<Pair<Color, Double>>, total: Double, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (total > 0) {
            var usado = 0.0
            partes.filter { it.second > 0 }.forEach { (cor, valor) ->
                val fracao = (valor / total).coerceIn(0.0, 1.0 - usado)
                if (fracao > 0) {
                    Box(Modifier.weight(fracao.toFloat()).fillMaxHeight().background(cor))
                    usado += fracao
                }
            }
            if (usado < 1.0) Box(Modifier.weight((1.0 - usado).toFloat()).fillMaxHeight())
        }
    }
}

/** Linha de uma categoria: nome, barra da participação e valor. */
@Composable
fun LinhaParticipacao(nome: String, cor: Color, valor: String, fracao: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PontoCor(cor)
            Text("  $nome", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(valor, style = MaterialTheme.typography.bodyMedium)
        }
        BarraProporcional(listOf(cor to fracao), 1.0)
    }
}
