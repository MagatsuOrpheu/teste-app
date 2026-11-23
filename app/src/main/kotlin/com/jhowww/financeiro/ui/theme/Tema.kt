package com.jhowww.financeiro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaEscuro = darkColorScheme(
    primary = Cores.Destaque,
    onPrimary = Color.White,
    primaryContainer = Cores.DestaqueSuave,
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFA882FF),
    onSecondary = Cores.Fundo,
    background = Cores.Fundo,
    onBackground = Cores.Texto,
    surface = Cores.Fundo,
    onSurface = Cores.Texto,
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Cores.TextoSecundario,
    surfaceContainerLowest = Color(0xFF161616),
    surfaceContainerLow = Color(0xFF202020),
    surfaceContainer = Cores.Superficie,
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Cores.SuperficieAlta,
    outline = Color(0xFF4A4A4A),
    outlineVariant = Cores.Borda,
    error = Cores.Negativo,
    onError = Color.White,
)

/** Tema escuro fixo, conforme o conceito visual aprovado. */
@Composable
fun FinanceiroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaEscuro, content = content)
}
