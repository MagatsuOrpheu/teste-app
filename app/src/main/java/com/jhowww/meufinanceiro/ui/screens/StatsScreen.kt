package com.jhowww.meufinanceiro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Dates
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.MonthSummary
import com.jhowww.meufinanceiro.ui.MonthUiState
import com.jhowww.meufinanceiro.ui.components.ColorDot
import com.jhowww.meufinanceiro.ui.components.EmptyState
import com.jhowww.meufinanceiro.ui.components.SectionTitle
import com.jhowww.meufinanceiro.ui.theme.expenseColor
import com.jhowww.meufinanceiro.ui.theme.incomeColor
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    state: MonthUiState,
    history: List<MonthSummary>,
    modifier: Modifier = Modifier,
) {
    val s = state.summary
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        item { SectionTitle("Gastos por categoria em ${Dates.monthLabel(state.month)}") }
        if (s.byCategory.isEmpty()) {
            item { EmptyState("Sem gastos neste mês", "Os gastos fixos e variáveis aparecem aqui por categoria.") }
        } else {
            val total = s.totalExpensesCents.coerceAtLeast(1)
            items(s.byCategory) { c ->
                val color = c.category?.let { Color(it.color.toInt()) } ?: Color.Gray
                val share = c.totalCents.toFloat() / total
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(color)
                        Spacer(Modifier.width(8.dp))
                        Text(c.category?.name ?: "Sem categoria", Modifier.weight(1f))
                        Text("${(share * 100).roundToInt()}%  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Money.format(c.totalCents), fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(share.coerceIn(0.01f, 1f))
                                .fillMaxHeight()
                                .background(color),
                        )
                    }
                    if (c.fixedCents > 0 && c.variableCents > 0) {
                        Text(
                            "Fixos ${Money.format(c.fixedCents)} · Variáveis ${Money.format(c.variableCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item { SectionTitle("Últimos 6 meses") }
        item { HistoryChart(history) }
        items(history.reversed()) { m -> HistoryRow(m) }
    }
}

@Composable
private fun HistoryChart(history: List<MonthSummary>) {
    val max = history.maxOfOrNull { maxOf(it.totalIncomeCents, it.totalExpensesCents) }?.coerceAtLeast(1L) ?: 1L
    val inColor = incomeColor()
    val outColor = expenseColor()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                history.forEach { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.height(116.dp)) {
                            Bar(m.totalIncomeCents.toFloat() / max, inColor)
                            Spacer(Modifier.width(3.dp))
                            Bar(m.totalExpensesCents.toFloat() / max, outColor)
                        }
                        Text(Dates.monthShort(m.month), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(inColor); Text("  Renda    ", style = MaterialTheme.typography.bodySmall)
                ColorDot(outColor); Text("  Gastos", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .width(14.dp)
            .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(color),
    )
}

@Composable
private fun HistoryRow(m: MonthSummary) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(Dates.monthLabel(m.month), style = MaterialTheme.typography.bodyLarge)
            Text(
                "Renda ${Money.format(m.totalIncomeCents)} · Gastos ${Money.format(m.totalExpensesCents)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            Money.format(m.balanceCents),
            color = if (m.balanceCents >= 0) incomeColor() else expenseColor(),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
