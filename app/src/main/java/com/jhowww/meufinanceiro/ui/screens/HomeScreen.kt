package com.jhowww.meufinanceiro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.AlertLevel
import com.jhowww.meufinanceiro.domain.model.Insight
import com.jhowww.meufinanceiro.domain.model.MonthSummary
import com.jhowww.meufinanceiro.ui.MonthUiState
import com.jhowww.meufinanceiro.ui.components.ColorDot
import com.jhowww.meufinanceiro.ui.components.SectionTitle
import com.jhowww.meufinanceiro.ui.components.UsageBar
import com.jhowww.meufinanceiro.ui.theme.ExpenseRed
import com.jhowww.meufinanceiro.ui.theme.IncomeGreen
import com.jhowww.meufinanceiro.ui.theme.WarningAmber
import com.jhowww.meufinanceiro.ui.theme.expenseColor
import com.jhowww.meufinanceiro.ui.theme.incomeColor
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: MonthUiState,
    onOpenTransactions: () -> Unit,
    onOpenFixed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = state.summary
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
    ) {
        item { BalanceCard(s) }
        item {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniCard("Renda", s.totalIncomeCents, incomeColor(), Modifier.weight(1f).clickable(onClick = onOpenFixed))
                MiniCard("Fixos", s.fixedExpensesCents, expenseColor(), Modifier.weight(1f).clickable(onClick = onOpenFixed))
                MiniCard("Variáveis", s.variableExpensesCents, expenseColor(), Modifier.weight(1f).clickable(onClick = onOpenTransactions))
            }
        }
        if (s.fixedExpensesCents > 0) {
            item {
                SectionTitle("Contas fixas do mês")
                Card(Modifier.fillMaxWidth().clickable(onClick = onOpenFixed)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pagas: ${Money.format(s.fixedPaidCents)}")
                            Text("Faltam: ${Money.format(s.fixedPendingCents)}", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        UsageBar(
                            fraction = s.fixedPaidCents.toFloat() / s.fixedExpensesCents,
                            baseColor = IncomeGreen,
                        )
                    }
                }
            }
        }
        if (s.insights.isNotEmpty()) {
            item { SectionTitle("Alertas e dicas") }
            items(s.insights) { InsightRow(it) }
        }
        if (s.byCategory.isNotEmpty()) {
            item { SectionTitle("Onde o dinheiro está indo") }
            items(s.byCategory.take(5)) { c ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(c.category?.let { Color(it.color.toInt()) } ?: Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text(c.category?.name ?: "Sem categoria", Modifier.weight(1f))
                        Text(Money.format(c.totalCents), fontWeight = FontWeight.Medium)
                    }
                    val usage = c.limitUsage
                    if (usage != null) {
                        Spacer(Modifier.height(4.dp))
                        UsageBar(usage)
                        Text(
                            "${(usage * 100).roundToInt()}% de ${Money.format(c.limitCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (state.transactions.isNotEmpty()) {
            item { SectionTitle("Últimos lançamentos") }
            items(state.transactions.take(5), key = { it.id }) { t ->
                TransactionRow(t, state.category(t.categoryId), onClick = onOpenTransactions)
            }
        }
    }
}

@Composable
private fun BalanceCard(s: MonthSummary) {
    val negative = s.balanceCents < 0
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (negative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                if (negative) "Você passou da renda em" else "Livre para gastar",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                Money.format(if (negative) -s.balanceCents else s.balanceCents),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            val daily = s.dailyAllowanceCents
            if (daily != null && !negative && s.totalIncomeCents > 0) {
                Text(
                    "≈ ${Money.format(daily)} por dia · ${s.daysLeft} ${if (s.daysLeft == 1) "dia restante" else "dias restantes"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (s.totalIncomeCents > 0) {
                Spacer(Modifier.height(12.dp))
                UsageBar(s.committedFraction)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(s.committedFraction * 100).roundToInt()}% da renda comprometida",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MiniCard(label: String, cents: Long, color: Color, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                Money.format(cents),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InsightRow(insight: Insight) {
    val (emoji, color) = when (insight.level) {
        AlertLevel.DANGER -> "⛔" to ExpenseRed
        AlertLevel.WARNING -> "⚠️" to WarningAmber
        AlertLevel.GOOD -> "✅" to IncomeGreen
        AlertLevel.INFO -> "💡" to MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji)
            Spacer(Modifier.width(10.dp))
            Text(insight.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
