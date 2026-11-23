package com.jhowww.financeiro.ui.navegacao

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** As cinco abas principais do app, na ordem da barra inferior. */
enum class Aba(
    val rota: String,
    val rotulo: String,
    val titulo: String,
    val icone: ImageVector,
    val etapa: Int,
) {
    INICIO("inicio", "Início", "Início", Icons.Outlined.Home, 4),
    FIXOS("fixos", "Fixos", "Gastos fixos e parcelamentos", Icons.Outlined.CalendarMonth, 4),
    LANCAMENTOS("lancamentos", "Lançamentos", "Lançamentos", Icons.Outlined.Receipt, 4),
    RELATORIOS("relatorios", "Relatórios", "Relatórios", Icons.Outlined.BarChart, 4),
    AJUSTES("ajustes", "Ajustes", "Ajustes", Icons.Outlined.Settings, 4),
}
