@file:OptIn(ExperimentalMaterial3Api::class)

package com.jhowww.meufinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.domain.model.Transaction
import com.jhowww.meufinanceiro.ui.AppViewModel
import com.jhowww.meufinanceiro.ui.components.CategoryDialog
import com.jhowww.meufinanceiro.ui.components.MonthSelector
import com.jhowww.meufinanceiro.ui.components.RecurringDialog
import com.jhowww.meufinanceiro.ui.components.TransactionDialog
import com.jhowww.meufinanceiro.ui.screens.FixedScreen
import com.jhowww.meufinanceiro.ui.screens.HomeScreen
import com.jhowww.meufinanceiro.ui.screens.OnboardingScreen
import com.jhowww.meufinanceiro.ui.screens.SettingsScreen
import com.jhowww.meufinanceiro.ui.screens.StatsScreen
import com.jhowww.meufinanceiro.ui.screens.TransactionsScreen
import com.jhowww.meufinanceiro.ui.theme.MeuFinanceiroTheme
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeuFinanceiroTheme {
                Surface(Modifier.fillMaxSize()) {
                    val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()
                    val categories by vm.categories.collectAsStateWithLifecycle()
                    if (!onboardingDone) {
                        OnboardingScreen(categories, onFinish = vm::finishOnboarding)
                    } else {
                        MainScaffold(vm)
                    }
                }
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector, val title: String) {
    HOME("Início", Icons.Default.Home, "Meu Financeiro"),
    TRANSACTIONS("Gastos", Icons.Default.List, "Lançamentos"),
    FIXED("Fixos", Icons.Default.DateRange, "Renda e gastos fixos"),
    STATS("Resumo", Icons.Default.Star, "Estatísticas"),
    SETTINGS("Ajustes", Icons.Default.Settings, "Ajustes"),
}

/** Qual diálogo está aberto (só um por vez). */
private sealed interface DialogState {
    data class Tx(val item: Transaction?) : DialogState
    data class Fixed(val item: RecurringItem?, val type: EntryType) : DialogState
    data class Cat(val item: Category?) : DialogState
}

@Composable
private fun MainScaffold(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var dialog by remember { mutableStateOf<DialogState?>(null) }
    val state by vm.monthState.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()

    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }

    val showMonth = tab != Tab.SETTINGS
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(title = { Text(tab.title) })
                if (showMonth) {
                    MonthSelector(
                        month = state.month,
                        onPrevious = vm::previousMonth,
                        onNext = vm::nextMonth,
                        onTitleClick = vm::goToCurrentMonth,
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (tab) {
                Tab.HOME, Tab.TRANSACTIONS -> FloatingActionButton(onClick = { dialog = DialogState.Tx(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Novo lançamento")
                }
                Tab.FIXED -> FloatingActionButton(onClick = { dialog = DialogState.Fixed(null, EntryType.EXPENSE) }) {
                    Icon(Icons.Default.Add, contentDescription = "Novo fixo")
                }
                else -> Unit
            }
        },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            Tab.HOME -> HomeScreen(
                state = state,
                onOpenTransactions = { tab = Tab.TRANSACTIONS },
                onOpenFixed = { tab = Tab.FIXED },
                modifier = m,
            )
            Tab.TRANSACTIONS -> TransactionsScreen(state, onEdit = { dialog = DialogState.Tx(it) }, modifier = m)
            Tab.FIXED -> FixedScreen(
                state = state,
                onEdit = { dialog = DialogState.Fixed(it, it.type) },
                onAdd = { dialog = DialogState.Fixed(null, it) },
                onTogglePaid = { item, paid -> vm.setPaid(item, paid) },
                modifier = m,
            )
            Tab.STATS -> StatsScreen(state, history, modifier = m)
            Tab.SETTINGS -> SettingsScreen(categories, onEditCategory = { dialog = DialogState.Cat(it) }, modifier = m)
        }
    }

    when (val d = dialog) {
        is DialogState.Tx -> TransactionDialog(
            initial = d.item,
            defaultDate = defaultDateFor(state.month),
            categories = categories,
            onDismiss = { dialog = null },
            onSave = { vm.saveTransaction(it) },
            onDelete = { vm.deleteTransaction(it.id) },
        )
        is DialogState.Fixed -> RecurringDialog(
            initial = d.item,
            defaultType = d.type,
            month = state.month,
            categories = categories,
            onDismiss = { dialog = null },
            onSave = { vm.saveRecurring(d.item, it) },
            onDelete = { vm.removeRecurring(it) },
        )
        is DialogState.Cat -> CategoryDialog(
            initial = d.item,
            onDismiss = { dialog = null },
            onSave = { vm.saveCategory(it) },
            onDelete = { vm.deleteCategory(it.id) },
        )
        null -> Unit
    }
}

/** Novo lançamento: hoje se estiver no mês atual; senão, dia 1 do mês selecionado. */
private fun defaultDateFor(month: YearMonth): LocalDate {
    val today = LocalDate.now()
    return if (YearMonth.from(today) == month) today else month.atDay(1)
}
