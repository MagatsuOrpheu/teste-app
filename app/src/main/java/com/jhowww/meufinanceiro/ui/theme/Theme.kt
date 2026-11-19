package com.jhowww.meufinanceiro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val IncomeGreen = Color(0xFF2E7D32)
val IncomeGreenDark = Color(0xFF81C784)
val ExpenseRed = Color(0xFFC62828)
val ExpenseRedDark = Color(0xFFEF9A9A)
val WarningAmber = Color(0xFFF9A825)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    tertiary = Color(0xFF456179),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFFB1CCC6),
    tertiary = Color(0xFFADCAE6),
)

@Composable
fun MeuFinanceiroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun incomeColor(): Color = if (isSystemInDarkTheme()) IncomeGreenDark else IncomeGreen

@Composable
fun expenseColor(): Color = if (isSystemInDarkTheme()) ExpenseRedDark else ExpenseRed
