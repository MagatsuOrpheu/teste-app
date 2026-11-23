package com.jhowww.financeiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jhowww.financeiro.ui.navegacao.AppNavegacao
import com.jhowww.financeiro.ui.theme.FinanceiroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanceiroTheme {
                AppNavegacao()
            }
        }
    }
}
