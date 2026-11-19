@file:OptIn(ExperimentalMaterial3Api::class)

package com.jhowww.meufinanceiro.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.domain.model.Transaction
import com.jhowww.meufinanceiro.ui.theme.ExpenseRed
import java.time.LocalDate
import java.time.YearMonth

@Composable
private fun TypeSelector(type: EntryType, onChange: (EntryType) -> Unit, expenseLabel: String, incomeLabel: String) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = type == EntryType.EXPENSE,
            onClick = { onChange(EntryType.EXPENSE) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text(expenseLabel) }
        SegmentedButton(
            selected = type == EntryType.INCOME,
            onClick = { onChange(EntryType.INCOME) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text(incomeLabel) }
    }
}

/** Criar/editar um gasto do dia a dia ou uma renda extra. */
@Composable
fun TransactionDialog(
    initial: Transaction?,
    defaultDate: LocalDate,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: ((Transaction) -> Unit)?,
) {
    var type by remember { mutableStateOf(initial?.type ?: EntryType.EXPENSE) }
    var amount by remember { mutableStateOf(initial?.let { Money.toInput(it.amountCents) } ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var date by remember { mutableStateOf(initial?.date ?: defaultDate) }
    var showDate by remember { mutableStateOf(false) }
    var triedSave by remember { mutableStateOf(false) }
    val cents = Money.parse(amount)
    val valid = cents != null && cents > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo lançamento" else "Editar lançamento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeSelector(type, { type = it }, "Gasto", "Renda extra")
                MoneyField(amount, { amount = it }, "Valor", isError = triedSave && !valid)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (type == EntryType.EXPENSE) "Descrição (ex.: iFood, Uber)" else "Descrição (ex.: freela)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == EntryType.EXPENSE) {
                    CategoryPicker(categories, categoryId) { categoryId = it }
                }
                DateField(date, onClick = { showDate = true })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                triedSave = true
                if (valid) {
                    onSave(
                        Transaction(
                            id = initial?.id ?: 0,
                            description = description.ifBlank { if (type == EntryType.EXPENSE) "Gasto" else "Renda extra" },
                            amountCents = cents!!,
                            type = type,
                            categoryId = if (type == EntryType.EXPENSE) categoryId else null,
                            date = date,
                        ),
                    )
                    onDismiss()
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { onDelete(initial); onDismiss() }) {
                        Text("Excluir", color = ExpenseRed)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )

    if (showDate) {
        AppDatePickerDialog(date, onDismiss = { showDate = false }, onConfirm = { date = it })
    }
}

/** Criar/editar renda fixa ou gasto fixo. */
@Composable
fun RecurringDialog(
    initial: RecurringItem?,
    defaultType: EntryType,
    month: YearMonth,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (RecurringItem) -> Unit,
    onDelete: ((RecurringItem) -> Unit)?,
) {
    var type by remember { mutableStateOf(initial?.type ?: defaultType) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.let { Money.toInput(it.amountCents) } ?: "") }
    var day by remember { mutableStateOf((initial?.dayOfMonth ?: 5).toString()) }
    var categoryId by remember { mutableStateOf(initial?.categoryId) }
    var triedSave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val cents = Money.parse(amount)
    val dayInt = day.toIntOrNull()
    val validDay = dayInt != null && dayInt in 1..31
    val valid = cents != null && cents > 0 && name.isNotBlank() && validDay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo fixo" else "Editar fixo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeSelector(type, { type = it }, "Gasto fixo", "Renda fixa")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (type == EntryType.EXPENSE) "Nome (ex.: Aluguel, Internet)" else "Nome (ex.: Salário, VR)") },
                    singleLine = true,
                    isError = triedSave && name.isBlank(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyField(amount, { amount = it }, "Valor mensal", isError = triedSave && (cents == null || cents <= 0))
                OutlinedTextField(
                    value = day,
                    onValueChange = { v -> day = v.filter { it.isDigit() }.take(2) },
                    label = { Text(if (type == EntryType.EXPENSE) "Dia do vencimento" else "Dia que cai na conta") },
                    singleLine = true,
                    isError = triedSave && !validDay,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == EntryType.EXPENSE) {
                    CategoryPicker(categories, categoryId) { categoryId = it }
                }
                if (initial != null) {
                    Text(
                        "Alterar o valor vale a partir do mês selecionado; os meses anteriores ficam como estavam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                triedSave = true
                if (valid) {
                    onSave(
                        RecurringItem(
                            id = initial?.id ?: 0,
                            name = name,
                            amountCents = cents!!,
                            type = type,
                            categoryId = if (type == EntryType.EXPENSE) categoryId else null,
                            dayOfMonth = dayInt!!,
                            startMonth = initial?.startMonth ?: month,
                            endMonth = initial?.endMonth,
                        ),
                    )
                    onDismiss()
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Excluir", color = ExpenseRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )

    if (confirmDelete && initial != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir ${initial.name}?") },
            text = { Text("Ele deixa de contar a partir deste mês. Os meses anteriores continuam no histórico.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(initial)
                    confirmDelete = false
                    onDismiss()
                }) { Text("Excluir", color = ExpenseRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

val CategoryPalette: List<Long> = listOf(
    0xFF5C6BC0, 0xFFEF6C00, 0xFF43A047, 0xFF1E88E5, 0xFF8E24AA, 0xFFE53935,
    0xFF00897B, 0xFFFDD835, 0xFF6D4C41, 0xFFD81B60, 0xFF757575, 0xFF00ACC1,
)

@Composable
fun CategoryDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: ((Category) -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var limit by remember { mutableStateOf(initial?.let { Money.toInput(it.monthlyLimitCents) } ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: CategoryPalette.first()) }
    var triedSave by remember { mutableStateOf(false) }
    val limitCents = if (limit.isBlank()) 0L else Money.parse(limit)
    val valid = name.isNotBlank() && limitCents != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova categoria" else "Editar categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    isError = triedSave && name.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyField(limit, { limit = it }, "Limite mensal (opcional)", isError = triedSave && limitCents == null)
                Text("Cor", style = MaterialTheme.typography.labelLarge)
                CategoryPalette.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { c ->
                            val selected = c == color
                            ColorDot(
                                Color(c.toInt()),
                                size = 30,
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape,
                                    )
                                    .clickable { color = c },
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                triedSave = true
                if (valid) {
                    onSave(Category(initial?.id ?: 0, name, color, limitCents!!))
                    onDismiss()
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                if (initial != null && onDelete != null) {
                    TextButton(onClick = { onDelete(initial); onDismiss() }) { Text("Excluir", color = ExpenseRed) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}
