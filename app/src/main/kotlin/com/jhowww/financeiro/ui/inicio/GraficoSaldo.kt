package com.jhowww.financeiro.ui.inicio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.domain.motor.PontoSaldo
import com.jhowww.financeiro.ui.comum.formatarCompacto
import com.jhowww.financeiro.ui.theme.Cores
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.dashed
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer

private val FormatoEixoY = CartesianValueFormatter { _, valor, _ -> formatarCompacto(valor) }
private val FormatoEixoX = CartesianValueFormatter { _, valor, _ -> valor.toInt().toString() }

/**
 * Linha do saldo restante ao longo do mês (estilo C aprovado): trecho real contínuo e
 * projeção tracejada a partir de hoje, terminando no saldo projetado.
 */
@Composable
fun GraficoSaldo(pontos: List<PontoSaldo>, modifier: Modifier = Modifier) {
    val produtor = remember { CartesianChartModelProducer() }
    LaunchedEffect(pontos) {
        if (pontos.isEmpty()) return@LaunchedEffect
        val reais = pontos.filter { !it.projetado }
        val projetados = pontos.filter { it.projetado }
        // As duas séries sempre existem, para manter cor e estilo de cada uma.
        val serieReal = reais.ifEmpty { listOf(pontos.first()) }
        val serieProjetada = (listOfNotNull(reais.lastOrNull()) + projetados).ifEmpty { listOf(pontos.last()) }
        produtor.runTransaction {
            lineSeries {
                series(serieReal.map { it.dia }, serieReal.map { it.saldo.centavos / 100.0 })
                series(serieProjetada.map { it.dia }, serieProjetada.map { it.saldo.centavos / 100.0 })
            }
        }
    }
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(Cores.Destaque)),
                        ),
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(Cores.Destaque.copy(alpha = 0.55f))),
                            stroke = LineCartesianLayer.LineStroke.dashed(),
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = FormatoEixoY),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = FormatoEixoX,
                    itemPlacer = remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 5 }) },
                ),
            ),
            modelProducer = produtor,
            modifier = modifier.fillMaxWidth().height(200.dp),
            scrollState = rememberVicoScrollState(scrollEnabled = false),
        )
    }
}
