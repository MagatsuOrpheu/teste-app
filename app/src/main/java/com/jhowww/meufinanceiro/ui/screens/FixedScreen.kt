package com.jhowww.meufinanceiro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.ui.MonthUiState
import com.jhowww.meufinanceiro.ui.components.ColorDot
import com.jhowww.meufinanceiro.ui.components.SectionTitle
import com.jhowww.meufinanceiro.ui.theme.expenseColor
import com.jhowww.meufinanceiro.ui.theme.incomeColor
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FixedScreen(
    state: MonthUiState,
    onEdit: (RecurringItem) -> Unit,
    onAdd: (EntryType) -> Unit,
    onTogglePaid: (RecurringItem, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = state.summary
    val today = LocalDate.now()
    val isCurrent = state.month == YearMonth.from(today)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    SummaryLine("Rendas fixas", s.recurringIncomeCents, incomeColor())
                    SummaryLine("Gastos fixos", s.fixedExpensesCents, expenseColor())
                    SummaryLine(
                        "Sobra depois dos fixos",
                        s.recurringIncomeCents - s.fixedExpensesCents,
                        MaterialTheme.colorScheme.onSurface,
                        bold = true,
                    )
                    if (s.recurringIncomeCents > 0) {
                        Text(
                            "Os fixos levam ${(s.fixedFraction * 100).toInt()}% da sua renda (ideal: até 50%).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Rendas fixas", Modifier.weight(1f))
                OutlinedButton(onClick = { onAdd(EntryType.INCOME) }) { Text("+ Renda") }
            }
        }
        if (state.incomes.isEmpty()) {
            item { HintText("Adicione seu salário ou outra renda mensal.") }
        }
        items(state.incomes, key = { it.id }) { r ->
            RecurringRow(r, null, state, isCurrent, today, onEdit, onTogglePaid)
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Gastos fixos", Modifier.weight(1f))
                OutlinedButton(onClick = { onAdd(EntryType.EXPENSE) }) { Text("+ Gasto fixo") }
            }
        }
        if (state.fixedExpenses.isEmpty()) {
            item { HintText("Aluguel, luz, água, internet, celular, academia, streaming…") }
        }
        items(state.fixedExpenses, key = { it.id }) { r ->
            RecurringRow(r, r.id in state.paidIds, state, isCurrent, today, onEdit, onTogglePaid)
        }
    }
}

@Composable
private fun SummaryLine(label: String, cents: Long, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(Money.format(cents), color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun RecurringRow(
    r: RecurringItem,
    paid: Boolean?,
    state: MonthUiState,
    isCurrent: Boolean,
    today: LocalDate,
    onEdit: (RecurringItem) -> Unit,
    onTogglePaid: (RecurringItem, Boolean) -> Unit,
) {
    val income = r.type == EntryType.INCOME
    val category = state.category(r.categoryId)
    val day = r.effectiveDay(state.month)
    val overdue = paid == false && isCurrent && day < today.dayOfMonth
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(r) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (paid != null) {
            Checkbox(checked = paid, onCheckedChange = { onTogglePaid(r, it) })
        } else {
            Spacer(Modifier.width(12.dp))
            ColorDot(incomeColor())
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                r.name,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (paid == true) TextDecoration.LineThrough else null,
            )
            val subtitle = buildString {
                append(if (income) "Cai dia $day" else "Vence dia $day")
                if (category != null) append(" · ${category.name}")
                if (overdue) append(" · ATRASADO")
                else if (paid == true) append(" · pago")
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) expenseColor() else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            Money.format(r.amountCents),
            color = if (income) incomeColor() else expenseColor(),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
