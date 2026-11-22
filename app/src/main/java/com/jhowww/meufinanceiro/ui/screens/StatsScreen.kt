package com.jhowww.meufinanceiro.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Dates
import com.jhowww.meufinanceiro.domain.FinanceCalculator
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.BalanceTimeline
import com.jhowww.meufinanceiro.domain.model.MonthSummary
import com.jhowww.meufinanceiro.ui.MonthUiState
import com.jhowww.meufinanceiro.ui.components.ColorDot
import com.jhowww.meufinanceiro.ui.components.EmptyState
import com.jhowww.meufinanceiro.ui.components.SectionTitle
import com.jhowww.meufinanceiro.ui.theme.expenseColor
import com.jhowww.meufinanceiro.ui.theme.incomeColor
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    state: MonthUiState,
    history: List<MonthSummary>,
    modifier: Modifier = Modifier,
) {
    val timeline = remember(state) {
        FinanceCalculator.balanceTimeline(state.month, LocalDate.now(), state.recurring, state.transactions)
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        if (timeline.startCents == 0L && timeline.dailyCents.all { it == 0L }) {
            EmptyState("Cadastre sua renda", "A linha do saldo aparece quando houver renda na aba Fixos.")
        } else {
            BalanceCard(timeline)
        }

        if (state.summary.byCategory.isNotEmpty()) {
            SectionTitle("Para onde foi o dinheiro")
            val total = state.summary.totalExpensesCents.coerceAtLeast(1)
            state.summary.byCategory.forEach { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorDot(c.category?.let { Color(it.color.toInt()) } ?: Color(0xFF999999), size = 10)
                    Spacer(Modifier.width(10.dp))
                    Text(c.category?.name ?: "Sem categoria", Modifier.weight(1f))
                    Text(
                        "${(c.totalCents * 100f / total).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(Money.format(c.totalCents), fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        val withData = history.filter { it.totalIncomeCents > 0 || it.totalExpensesCents > 0 }
        if (withData.isNotEmpty()) {
            SectionTitle("Como fecharam os últimos meses")
            withData.reversed().forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(Dates.monthLabel(m.month), Modifier.weight(1f))
                    Text(
                        (if (m.balanceCents >= 0) "+" else "") + Money.format(m.balanceCents),
                        color = if (m.balanceCents >= 0) incomeColor() else expenseColor(),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BalanceCard(t: BalanceTimeline) {
    var selectedDay by remember(t) { mutableStateOf<Int?>(null) }
    val isPast = t.projectedEndCents == null
    val shownDay = selectedDay
    val headline = when {
        shownDay != null -> "Dia $shownDay"
        isPast -> "Sobrou no fim do mês"
        t.elapsedDays == 0 -> "Começa o mês com"
        else -> "Sobra hoje"
    }
    val headValue = when {
        shownDay == null -> t.currentCents
        shownDay <= t.elapsedDays -> t.valueAt(shownDay)
        else -> projectedAt(t, shownDay)
    }
    val headIsProjection = shownDay != null && shownDay > t.elapsedDays

    Card(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                headline + if (headIsProjection) " (previsão)" else "",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                Money.format(headValue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (headValue < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
            )
            val projected = t.projectedEndCents
            if (projected != null && shownDay == null) {
                Text(
                    "Previsão para o dia ${t.daysInMonth}: ${Money.format(projected)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (projected < 0) expenseColor() else incomeColor(),
                )
            }
            Spacer(Modifier.height(16.dp))
            BalanceChart(t, selectedDay) { selectedDay = it }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text("dia 1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("dia ${t.daysInMonth}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Toque ou arraste no gráfico para ver o saldo de cada dia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MiniStat("Começou com", Money.format(t.startCents), Modifier.weight(1f))
        MiniStat(
            if (isPast) "Gastou no mês" else "Gastou até hoje",
            Money.format((t.startCents - t.currentCents).coerceAtLeast(0)),
            Modifier.weight(1f),
        )
    }
}

/** Valor da linha de previsão em um dia futuro (reta entre hoje e o fim do mês). */
private fun projectedAt(t: BalanceTimeline, day: Int): Long {
    val end = t.projectedEndCents ?: return t.currentCents
    val from = t.elapsedDays
    val span = (t.daysInMonth - from).coerceAtLeast(1)
    return t.currentCents + (end - t.currentCents) * (day - from) / span
}

@Composable
private fun BalanceChart(t: BalanceTimeline, selectedDay: Int?, onSelect: (Int?) -> Unit) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val danger = expenseColor()
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    val progress = remember(t) { Animatable(0f) }
    LaunchedEffect(t) { progress.animateTo(1f, tween(800)) }

    val values = listOf(t.startCents) + t.dailyCents + listOfNotNull(t.projectedEndCents)
    val maxV = values.max().coerceAtLeast(1L).toFloat()
    val minV = minOf(0L, values.min()).toFloat()

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
            .pointerInput(t) {
                fun dayAt(x: Float) = ((x / size.width) * t.daysInMonth).roundToInt().coerceIn(1, t.daysInMonth)
                detectTapGestures { onSelect(dayAt(it.x)) }
            }
            .pointerInput(t) {
                fun dayAt(x: Float) = ((x / size.width) * t.daysInMonth).roundToInt().coerceIn(1, t.daysInMonth)
                detectDragGestures(
                    onDragStart = { onSelect(dayAt(it.x)) },
                    onDragEnd = { onSelect(null) },
                    onDragCancel = { onSelect(null) },
                ) { change, _ -> onSelect(dayAt(change.position.x)) }
            },
    ) {
        val pad = 8.dp.toPx()
        val h = size.height - pad * 2
        val range = (maxV - minV).coerceAtLeast(1f)
        fun x(day: Int) = day.toFloat() / t.daysInMonth * size.width
        fun y(v: Long) = pad + (maxV - v) / range * h

        for (i in 0..3) {
            val gy = pad + h * i / 3f
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx())
        }
        if (minV < 0f) {
            drawLine(danger.copy(alpha = 0.7f), Offset(0f, y(0)), Offset(size.width, y(0)), 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        }

        val shownDays = (t.elapsedDays * progress.value).roundToInt()
        val path = Path().apply {
            moveTo(0f, y(t.startCents))
            for (d in 1..shownDays) {
                // degraus: o saldo muda no dia em que o gasto acontece
                lineTo(x(d - 1), y(t.valueAt(d - 1)))
                lineTo(x(d), y(t.valueAt(d)))
            }
        }
        val area = Path().apply {
            addPath(path)
            lineTo(x(shownDays), size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, line.copy(alpha = 0.12f))
        drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val projected = t.projectedEndCents
        val todayPoint = Offset(x(t.elapsedDays), y(t.currentCents))
        if (progress.value >= 1f) {
            if (projected != null) {
                drawLine(
                    line.copy(alpha = 0.55f), todayPoint, Offset(size.width, y(projected)), 2.dp.toPx(),
                    cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                )
            }
            if (t.elapsedDays > 0) {
                drawCircle(surface, 7.dp.toPx(), todayPoint)
                drawCircle(line, 5.dp.toPx(), todayPoint)
            }
        }

        if (selectedDay != null) {
            val sx = x(selectedDay)
            val v = if (selectedDay <= t.elapsedDays) t.valueAt(selectedDay) else projectedAt(t, selectedDay)
            drawLine(muted, Offset(sx, 0f), Offset(sx, size.height), 1.dp.toPx())
            drawCircle(surface, 6.dp.toPx(), Offset(sx, y(v)))
            drawCircle(muted, 4.dp.toPx(), Offset(sx, y(v)))
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
