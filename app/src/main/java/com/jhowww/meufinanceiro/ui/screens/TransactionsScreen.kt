package com.jhowww.meufinanceiro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Dates
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.Transaction
import com.jhowww.meufinanceiro.ui.MonthUiState
import com.jhowww.meufinanceiro.ui.components.ColorDot
import com.jhowww.meufinanceiro.ui.components.EmptyState
import com.jhowww.meufinanceiro.ui.theme.expenseColor
import com.jhowww.meufinanceiro.ui.theme.incomeColor
import java.time.format.TextStyle

@Composable
fun TransactionsScreen(
    state: MonthUiState,
    onEdit: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.loaded && state.transactions.isEmpty()) {
        EmptyState(
            "Nenhum lançamento neste mês",
            "Toque em + para registrar um gasto ou uma renda extra.",
            modifier,
        )
        return
    }
    val byDay = state.transactions.groupBy { it.date }.toSortedMap(compareByDescending<java.time.LocalDate> { it })
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
    ) {
        item {
            val s = state.summary
            Text(
                "Gastos variáveis: ${Money.format(s.variableExpensesCents)}  ·  Rendas extras: ${Money.format(s.extraIncomeCents)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        byDay.forEach { (date, list) ->
            item(key = "h$date") {
                val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, com.jhowww.meufinanceiro.domain.LocaleBR)
                val dayTotal = list.filter { it.type == EntryType.EXPENSE }.sumOf { it.amountCents }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
                    Text(
                        "${Dates.formatShort(date)} · $weekday",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (dayTotal > 0) {
                        Text(
                            "-${Money.format(dayTotal)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
            items(list, key = { it.id }) { t ->
                TransactionRow(t, state.category(t.categoryId), onClick = { onEdit(t) })
            }
        }
    }
}

@Composable
fun TransactionRow(t: Transaction, category: Category?, onClick: () -> Unit) {
    val income = t.type == EntryType.INCOME
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(
            when {
                income -> incomeColor()
                category != null -> Color(category.color.toInt())
                else -> Color.Gray
            },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.description, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (income) "Renda extra · ${Dates.format(t.date)}"
                else "${category?.name ?: "Sem categoria"} · ${Dates.format(t.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            (if (income) "+" else "-") + Money.format(t.amountCents),
            color = if (income) incomeColor() else expenseColor(),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
