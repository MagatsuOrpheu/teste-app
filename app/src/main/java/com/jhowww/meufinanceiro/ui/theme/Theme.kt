package com.jhowww.meufinanceiro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Paleta inspirada no Obsidian: fundos grafite, texto suave, destaque roxo
 * e as cores de destaque do tema padrão (verde, vermelho, laranja, ciano...).
 */

// Cores de status (usadas em barras, alertas e valores)
val IncomeGreen = Color(0xFF08B94E)
val IncomeGreenDark = Color(0xFF44CF6E)
val ExpenseRed = Color(0xFFE93147)
val ExpenseRedDark = Color(0xFFFB464C)
val WarningAmber = Color(0xFFE9973F)

private val ObsidianDark = darkColorScheme(
    primary = Color(0xFF8B6CEF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3B2F6B),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFFA882FF),
    onSecondary = Color(0xFF1E1E1E),
    secondaryContainer = Color(0xFF332A52),
    onSecondaryContainer = Color(0xFFE6DEFF),
    tertiary = Color(0xFF53DFDD),
    onTertiary = Color(0xFF00201F),
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFDCDDDE),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFDCDDDE),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFF9A9A9A),
    surfaceContainerLowest = Color(0xFF161616),
    surfaceContainerLow = Color(0xFF202020),
    surfaceContainer = Color(0xFF262626),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF2B2B2B),
    outline = Color(0xFF4A4A4A),
    outlineVariant = Color(0xFF363636),
    error = Color(0xFFFB464C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF4A1E22),
    onErrorContainer = Color(0xFFFFDAD8),
    inverseSurface = Color(0xFFDCDDDE),
    inverseOnSurface = Color(0xFF1E1E1E),
    inversePrimary = Color(0xFF705DCF),
    scrim = Color(0xFF000000),
)

private val ObsidianLight = lightColorScheme(
    primary = Color(0xFF705DCF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAE4FF),
    onPrimaryContainer = Color(0xFF2A1F66),
    secondary = Color(0xFF8A5CF5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE8FF),
    onSecondaryContainer = Color(0xFF2A1F66),
    tertiary = Color(0xFF00A5A3),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF222222),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF222222),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF5C5C5C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF6F6F6),
    surfaceContainerHigh = Color(0xFFF2F2F2),
    surfaceContainerHighest = Color(0xFFF0F0F0),
    outline = Color(0xFFCFCFCF),
    outlineVariant = Color(0xFFE3E3E3),
    error = Color(0xFFE93147),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE3E5),
    onErrorContainer = Color(0xFF5C0A14),
    inverseSurface = Color(0xFF262626),
    inverseOnSurface = Color(0xFFDCDDDE),
    inversePrimary = Color(0xFF8B6CEF),
    scrim = Color(0xFF000000),
)

@Composable
fun MeuFinanceiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) ObsidianDark else ObsidianLight, content = content)
}

@Composable
fun incomeColor(): Color = if (isSystemInDarkTheme()) IncomeGreenDark else IncomeGreen

@Composable
fun expenseColor(): Color = if (isSystemInDarkTheme()) ExpenseRedDark else ExpenseRed
