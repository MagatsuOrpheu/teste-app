package com.jhowww.financeiro.ui.navegacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavegacao() {
    val nav = rememberNavController()
    val entradaAtual by nav.currentBackStackEntryAsState()
    val destino = entradaAtual?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Aba.entries.forEach { aba ->
                    val selecionada = destino?.hierarchy?.any { it.route == aba.rota } == true
                    NavigationBarItem(
                        selected = selecionada,
                        onClick = {
                            nav.navigate(aba.rota) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(aba.icone, contentDescription = aba.rotulo) },
                        label = { Text(aba.rotulo, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Aba.INICIO.rota,
            modifier = Modifier.padding(padding),
        ) {
            Aba.entries.forEach { aba ->
                composable(aba.rota) { TelaEmConstrucao(aba) }
            }
        }
    }
}

/** Marcador temporário: deixa claro que a tela ainda não existe, sem exibir dados fictícios. */
@Composable
private fun TelaEmConstrucao(aba: Aba) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(aba.titulo, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Esta tela será implementada na Etapa ${aba.etapa}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
