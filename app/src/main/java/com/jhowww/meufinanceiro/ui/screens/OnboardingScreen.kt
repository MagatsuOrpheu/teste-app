package com.jhowww.meufinanceiro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.ui.components.CategoryPicker
import com.jhowww.meufinanceiro.ui.components.MoneyField
import com.jhowww.meufinanceiro.ui.theme.expenseColor
import java.time.YearMonth

/** Primeira abertura: pergunta a renda e os principais gastos fixos. */
@Composable
fun OnboardingScreen(
    categories: List<Category>,
    onFinish: (salaryCents: Long, payday: Int, fixed: List<RecurringItem>) -> Unit,
) {
    var step by remember { mutableStateOf(0) }
    var salary by remember { mutableStateOf("") }
    var payday by remember { mutableStateOf("5") }
    val fixed = remember { mutableStateListOf<RecurringItem>() }

    val salaryCents = Money.parse(salary)
    val paydayInt = payday.toIntOrNull()?.takeIf { it in 1..31 }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Meu Financeiro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        if (step == 0) {
            Text("Quanto você recebe por mês?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Use o valor líquido (o que cai na conta). Outras rendas fixas você adiciona depois na aba Fixos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoneyField(salary, { salary = it }, "Renda mensal")
            OutlinedTextField(
                value = payday,
                onValueChange = { v -> payday = v.filter { it.isDigit() }.take(2) },
                label = { Text("Dia do pagamento") },
                singleLine = true,
                isError = paydayInt == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { step = 1 },
                enabled = salaryCents != null && salaryCents > 0 && paydayInt != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continuar") }
        } else {
            Text("Quais são seus gastos fixos?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Aluguel, luz, água, internet, celular, academia, streaming… Eles serão descontados todo mês.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            QuickFixedForm(categories) { fixed.add(it) }

            fixed.forEachIndexed { index, item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Medium)
                            Text("Vence dia ${item.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(Money.format(item.amountCents), color = expenseColor())
                        TextButton(onClick = { fixed.removeAt(index) }) { Text("Remover") }
                    }
                }
            }

            val totalFixed = fixed.sumOf { it.amountCents }
            val income = salaryCents ?: 0L
            Text(
                "Renda ${Money.format(income)} − Fixos ${Money.format(totalFixed)} = ${Money.format(income - totalFixed)} livres",
                style = MaterialTheme.typography.titleSmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { step = 0 }) { Text("Voltar") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onFinish(income, paydayInt ?: 5, fixed.toList()) }) {
                    Text(if (fixed.isEmpty()) "Pular e começar" else "Começar")
                }
            }
        }
    }
}

@Composable
private fun QuickFixedForm(categories: List<Category>, onAdd: (RecurringItem) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("10") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    val cents = Money.parse(amount)
    val dayInt = day.toIntOrNull()?.takeIf { it in 1..31 }
    val valid = name.isNotBlank() && cents != null && cents > 0 && dayInt != null

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome (ex.: Aluguel)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            MoneyField(amount, { amount = it }, "Valor")
            OutlinedTextField(
                value = day,
                onValueChange = { v -> day = v.filter { it.isDigit() }.take(2) },
                label = { Text("Dia do vencimento") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            CategoryPicker(categories, categoryId) { categoryId = it }
            Button(
                enabled = valid,
                onClick = {
                    onAdd(
                        RecurringItem(
                            name = name.trim(),
                            amountCents = cents!!,
                            type = EntryType.EXPENSE,
                            categoryId = categoryId,
                            dayOfMonth = dayInt!!,
                            startMonth = YearMonth.now(),
                        ),
                    )
                    name = ""; amount = ""; categoryId = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Adicionar gasto fixo") }
        }
    }
}
