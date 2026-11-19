@file:OptIn(ExperimentalMaterial3Api::class)

package com.jhowww.meufinanceiro.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Dates
import com.jhowww.meufinanceiro.domain.FinanceCalculator
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.CategorySpending
import com.jhowww.meufinanceiro.domain.model.MonthSummary
import com.jhowww.meufinanceiro.domain.model.SpendingPace
import com.jhowww.meufinanceiro.ui.MonthUiState
import com.jhowww.meufinanceiro.ui.components.ColorDot
import com.jhowww.meufinanceiro.ui.components.EmptyState
import com.jhowww.meufinanceiro.ui.theme.expenseColor
import com.jhowww.meufinanceiro.ui.theme.incomeColor
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private val ChartTabs = listOf("Categorias", "Mês a mês", "Ritmo")

@Composable
fun StatsScreen(
    state: MonthUiState,
    history: List<MonthSummary>,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            ChartTabs.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = selected == i,
                    onClick = { selected = i },
                    shape = SegmentedButtonDefaults.itemShape(index = i, count = ChartTabs.size),
                ) { Text(label, maxLines = 1) }
            }
        }
        when (selected) {
            0 -> CategoriesTab(state)
            1 -> MonthsTab(history)
            else -> PaceTab(state)
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ------------------------------------------------------------------ Categorias (rosca)

@Composable
private fun CategoriesTab(state: MonthUiState) {
    val s = state.summary
    if (s.byCategory.isEmpty()) {
        EmptyState("Sem gastos em ${Dates.monthLabel(state.month)}", "Os gastos fixos e variáveis aparecem aqui por categoria.")
        return
    }
    val total = s.totalExpensesCents.coerceAtLeast(1)
    val slices = s.byCategory.map { it to colorOf(it) }

    ChartCard {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(slices.map { (c, color) -> c.totalCents.toFloat() / total to color }, Modifier.size(220.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total gasto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Money.format(s.totalExpensesCents), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (s.totalIncomeCents > 0) {
                    Text(
                        "${(s.committedFraction * 100).roundToInt()}% da renda",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    slices.forEach { (c, color) -> CategoryLegendRow(c, color, c.totalCents.toFloat() / total) }
}

@Composable
private fun Donut(slices: List<Pair<Float, Color>>, modifier: Modifier) {
    val progress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) { progress.animateTo(1f, tween(700)) }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) {
        val stroke = 30.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        val gap = if (slices.size > 1) 2f else 0f
        var start = -90f
        slices.forEach { (share, color) ->
            val sweep = 360f * share * progress.value
            if (sweep > gap) {
                drawArc(color, start + gap / 2, sweep - gap, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
            }
            start += sweep
        }
    }
}

@Composable
private fun CategoryLegendRow(c: CategorySpending, color: Color, share: Float) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(color, size = 12)
            Spacer(Modifier.width(10.dp))
            Text(c.category?.name ?: "Sem categoria", Modifier.weight(1f))
            Text(
                "${(share * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(Money.format(c.totalCents), fontWeight = FontWeight.SemiBold)
        }
        val detail = buildList {
            if (c.fixedCents > 0) add("Fixos ${Money.format(c.fixedCents)}")
            if (c.variableCents > 0) add("Variáveis ${Money.format(c.variableCents)}")
            val usage = c.limitUsage
            if (usage != null) add("${(usage * 100).roundToInt()}% do limite")
        }
        if (detail.isNotEmpty()) {
            Text(
                detail.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ Mês a mês (barras + linha de saldo)

@Composable
private fun MonthsTab(history: List<MonthSummary>) {
    if (history.all { it.totalIncomeCents == 0L && it.totalExpensesCents == 0L }) {
        EmptyState("Ainda sem histórico", "Conforme os meses passam, a comparação aparece aqui.")
        return
    }
    val inColor = incomeColor()
    val outColor = expenseColor()
    val lineColor = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val progress = remember(history) { Animatable(0f) }
    LaunchedEffect(history) { progress.animateTo(1f, tween(700)) }

    ChartCard {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(inColor, "Renda")
            Legend(outColor, "Gastos")
            Legend(lineColor, "Saldo")
        }
        Spacer(Modifier.height(12.dp))
        val maxV = history.maxOf { maxOf(it.totalIncomeCents, it.totalExpensesCents, it.balanceCents) }.coerceAtLeast(1L)
        val minV = minOf(0L, history.minOf { it.balanceCents })
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            val range = (maxV - minV).toFloat()
            fun y(v: Long) = size.height - (v - minV) / range * size.height
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            for (i in 0..4) {
                val gy = size.height * i / 4f
                drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx(), pathEffect = dash)
            }
            drawLine(grid, Offset(0f, y(0)), Offset(size.width, y(0)), 1.5.dp.toPx())

            val n = history.size
            val groupW = size.width / n
            val barW = groupW * 0.26f
            val r = CornerRadius(4.dp.toPx())
            val points = mutableListOf<Offset>()
            history.forEachIndexed { i, m ->
                val cx = groupW * (i + 0.5f)
                val inTop = y(0) - (y(0) - y(m.totalIncomeCents)) * progress.value
                val outTop = y(0) - (y(0) - y(m.totalExpensesCents)) * progress.value
                drawRoundRect(inColor, Offset(cx - barW - 2f, inTop), Size(barW, y(0) - inTop), r)
                drawRoundRect(outColor, Offset(cx + 2f, outTop), Size(barW, y(0) - outTop), r)
                points += Offset(cx, y(0) - (y(0) - y(m.balanceCents)) * progress.value)
            }
            val path = Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            }
            drawPath(path, lineColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            points.forEach {
                drawCircle(lineColor, 4.5.dp.toPx(), it)
                drawCircle(Color.Black.copy(alpha = 0.35f), 2.dp.toPx(), it)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            history.forEach {
                Text(
                    Dates.monthShort(it.month),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    val withData = history.filter { it.totalIncomeCents > 0 || it.totalExpensesCents > 0 }
    if (withData.size >= 2) {
        val avgExpense = withData.sumOf { it.totalExpensesCents } / withData.size
        val avgBalance = withData.sumOf { it.balanceCents } / withData.size
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Gasto médio", Money.format(avgExpense), expenseColor(), Modifier.weight(1f))
            StatTile("Saldo médio", Money.format(avgBalance), if (avgBalance >= 0) incomeColor() else expenseColor(), Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(8.dp))
    history.reversed().forEach { m ->
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
}

// ------------------------------------------------------------------ Ritmo de gastos (linha acumulada)

@Composable
private fun PaceTab(state: MonthUiState) {
    val pace = remember(state) {
        FinanceCalculator.spendingPace(state.month, LocalDate.now(), state.summary, state.transactions)
    }
    if (pace.elapsedDays == 0) {
        EmptyState("Este mês ainda não começou", "O ritmo de gastos aparece a partir do primeiro dia do mês.")
        return
    }
    val over = pace.projectedCents > pace.budgetCents
    val lineColor = MaterialTheme.colorScheme.primary
    val idealColor = MaterialTheme.colorScheme.onSurfaceVariant
    val budgetColor = expenseColor()
    val grid = MaterialTheme.colorScheme.outlineVariant

    ChartCard {
        Text("Gastos variáveis acumulados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Compara o que você já gastou com o ritmo ideal para não estourar o que sobra depois dos fixos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(lineColor, "Gasto real")
            Legend(idealColor, "Ritmo ideal")
            Legend(budgetColor, "Limite")
        }
        Spacer(Modifier.height(12.dp))
        PaceChart(pace, lineColor, idealColor, budgetColor, grid)
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("dia 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("dia ${pace.daysInMonth}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Gasto até agora", Money.format(pace.spentCents), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
        StatTile("Ideal até agora", Money.format(pace.idealSoFarCents), MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("Projeção do mês", Money.format(pace.projectedCents), if (over) expenseColor() else incomeColor(), Modifier.weight(1f))
        StatTile("Disponível p/ variáveis", Money.format(pace.budgetCents), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    val diff = pace.spentCents - pace.idealSoFarCents
    val message = when {
        pace.budgetCents == 0L -> "Cadastre sua renda na aba Fixos para calcular o ritmo ideal."
        abs(diff) < pace.budgetCents / 100 -> "Você está exatamente no ritmo ideal. 👌"
        diff > 0 -> "Você está ${Money.format(diff)} acima do ritmo ideal. " +
            if (over) "Mantendo essa média, o mês fecha ${Money.format(pace.projectedCents - pace.budgetCents)} no vermelho." else "Ainda dá para fechar o mês no azul."
        else -> "Você está ${Money.format(-diff)} abaixo do ritmo ideal. Boa! 🎉"
    }
    Text(message, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun PaceChart(pace: SpendingPace, lineColor: Color, idealColor: Color, budgetColor: Color, grid: Color) {
    val progress = remember(pace) { Animatable(0f) }
    LaunchedEffect(pace) { progress.animateTo(1f, tween(800)) }
    val maxV = maxOf(pace.budgetCents, pace.projectedCents, pace.spentCents).coerceAtLeast(1L).toFloat() * 1.08f
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val days = pace.daysInMonth
        fun x(day: Float) = day / days * size.width
        fun y(v: Long) = size.height - v / maxV * size.height
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))

        for (i in 0..4) {
            val gy = size.height * i / 4f
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
        }
        // Limite (o que sobra depois dos fixos)
        if (pace.budgetCents > 0) {
            drawLine(budgetColor.copy(alpha = 0.8f), Offset(0f, y(pace.budgetCents)), Offset(size.width, y(pace.budgetCents)), 1.5.dp.toPx(), pathEffect = dash)
            // Ritmo ideal: linha reta de 0 até o limite no último dia
            drawLine(idealColor, Offset(0f, y(0)), Offset(size.width, y(pace.budgetCents)), 1.5.dp.toPx(), pathEffect = dash)
        }

        val shown = (pace.elapsedDays * progress.value).coerceAtLeast(1f)
        val pts = pace.cumulativeCents.mapIndexed { i, v -> Offset(x(i + 1f), y(v)) }
            .filterIndexed { i, _ -> i + 1 <= shown }
        val line = Path().apply {
            moveTo(0f, y(0))
            pts.forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(pts.lastOrNull()?.x ?: 0f, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f))))
        drawPath(line, lineColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val last = pts.lastOrNull()
        if (last != null && progress.value >= 1f) {
            if (pace.elapsedDays < days) {
                drawLine(
                    lineColor.copy(alpha = 0.6f), last, Offset(size.width, y(pace.projectedCents)),
                    2.dp.toPx(), pathEffect = dash, cap = StrokeCap.Round,
                )
            }
            drawCircle(lineColor, 6.dp.toPx(), last)
            drawCircle(Color.White, 2.5.dp.toPx(), last)
        }
    }
}

// ------------------------------------------------------------------ peças comuns

@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color, size = 10)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun colorOf(c: CategorySpending): Color =
    c.category?.let { Color(it.color.toInt()) } ?: Color(0xFF999999)
