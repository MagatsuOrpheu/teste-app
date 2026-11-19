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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhowww.meufinanceiro.domain.Money
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.ui.components.ColorDot
import com.jhowww.meufinanceiro.ui.components.SectionTitle

@Composable
fun SettingsScreen(
    categories: List<Category>,
    onEditCategory: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Categorias e limites", Modifier.weight(1f))
                OutlinedButton(onClick = { onEditCategory(null) }) { Text("+ Categoria") }
            }
            Text(
                "Defina um limite mensal para receber alerta quando uma categoria passar de 80%.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(categories, key = { it.id }) { c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEditCategory(c) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(Color(c.color.toInt()), size = 16)
                Spacer(Modifier.width(12.dp))
                Text(c.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (c.monthlyLimitCents > 0) "Limite ${Money.format(c.monthlyLimitCents)}" else "Sem limite",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionTitle("Como funciona")
            Column {
                listOf(
                    "• Renda: cadastre salário e outras rendas mensais na aba Fixos. Rendas pontuais (freela, 13º) entram em Gastos como “Renda extra”.",
                    "• Gastos fixos repetem todo mês automaticamente. Marque como pago para acompanhar o que falta pagar.",
                    "• Livre para gastar = renda − gastos fixos − gastos variáveis.",
                    "• Mudar o valor de um fixo vale a partir do mês selecionado, sem alterar o histórico.",
                    "• Todos os dados ficam só no seu celular (sem internet, sem login).",
                ).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
