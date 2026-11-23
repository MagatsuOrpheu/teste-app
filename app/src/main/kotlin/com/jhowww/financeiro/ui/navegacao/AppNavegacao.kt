package com.jhowww.financeiro.ui.navegacao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.ajustes.TelaAjustes
import com.jhowww.financeiro.ui.fixos.TelaFixos
import com.jhowww.financeiro.ui.inicio.TelaInicio
import com.jhowww.financeiro.ui.lancamentos.TelaLancamentos
import com.jhowww.financeiro.ui.relatorios.TelaRelatorios

@Composable
fun AppNavegacao() {
    val vm: FinanceiroViewModel = viewModel(factory = FinanceiroViewModel.Fabrica)
    val carregado by vm.estado.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entradaAtual by nav.currentBackStackEntryAsState()
    val destino = entradaAtual?.destination
    val avisos = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.atualizarDia() }
    LaunchedEffect(vm) { vm.mensagens.collect { avisos.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        bottomBar = {
            NavigationBar {
                Aba.entries.forEach { aba ->
                    val selecionada = destino?.hierarchy?.any { it.route == aba.rota } == true
                    NavigationBarItem(
                        selected = selecionada,
                        onClick = { irPara(nav, aba) },
                        icon = { Icon(aba.icone, contentDescription = aba.rotulo) },
                        label = { Text(aba.rotulo, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        if (carregado == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else NavHost(
            navController = nav,
            startDestination = Aba.INICIO.rota,
            modifier = Modifier.padding(padding),
        ) {
            // As telas leem o estado dentro do destino: o grafo não depende dos dados e não é refeito a cada gravação.
            composable(Aba.INICIO.rota) { ComEstado(vm) { e, m -> TelaInicio(vm, e, m, onIrParaAjustes = { irPara(nav, Aba.AJUSTES) }) } }
            composable(Aba.FIXOS.rota) { ComEstado(vm) { e, m -> TelaFixos(vm, e, m) } }
            composable(Aba.LANCAMENTOS.rota) { ComEstado(vm) { e, m -> TelaLancamentos(vm, e, m) } }
            composable(Aba.RELATORIOS.rota) { ComEstado(vm) { e, m -> TelaRelatorios(vm, e, m) } }
            composable(Aba.AJUSTES.rota) { ComEstado(vm) { e, _ -> TelaAjustes(vm, e) } }
        }
    }
}

@Composable
private fun ComEstado(vm: FinanceiroViewModel, conteudo: @Composable (EstadoApp, Competencia) -> Unit) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val mes by vm.mes.collectAsStateWithLifecycle()
    estado?.let { conteudo(it, mes) }
}

private fun irPara(nav: NavHostController, aba: Aba) {
    nav.navigate(aba.rota) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
