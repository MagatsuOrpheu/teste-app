package com.jhowww.meufinanceiro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jhowww.meufinanceiro.AppPrefs
import com.jhowww.meufinanceiro.MeuFinanceiroApp
import com.jhowww.meufinanceiro.data.FinanceRepository
import com.jhowww.meufinanceiro.domain.FinanceCalculator
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.MonthSummary
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.domain.model.Transaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class MonthUiState(
    val month: YearMonth = YearMonth.now(),
    val recurring: List<RecurringItem> = emptyList(),
    val paidIds: Set<Long> = emptySet(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val summary: MonthSummary = MonthSummary(month),
    val loaded: Boolean = false,
) {
    val incomes: List<RecurringItem> get() = recurring.filter { it.type == EntryType.INCOME }
    val fixedExpenses: List<RecurringItem> get() = recurring.filter { it.type == EntryType.EXPENSE }
    fun category(id: Long?): Category? = id?.let { cid -> categories.firstOrNull { it.id == cid } }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val repo: FinanceRepository,
    private val prefs: AppPrefs,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.onboardingDone)
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    val categories: StateFlow<List<Category>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthState: StateFlow<MonthUiState> = _selectedMonth
        .flatMapLatest { month ->
            combine(
                repo.observeRecurring(),
                repo.observePaid(month),
                repo.observeTransactions(month),
                repo.observeCategories(),
            ) { recurring, paid, tx, cats ->
                MonthUiState(
                    month = month,
                    recurring = recurring.filter { it.isActiveIn(month) },
                    paidIds = paid,
                    transactions = tx,
                    categories = cats,
                    summary = FinanceCalculator.summarize(month, LocalDate.now(), recurring, paid, tx, cats),
                    loaded = true,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthUiState())

    /** Últimos 6 meses até o mês selecionado. */
    val history: StateFlow<List<MonthSummary>> = _selectedMonth
        .flatMapLatest { month ->
            val from = month.minusMonths(5)
            combine(
                repo.observeRecurring(),
                repo.observeTransactionsBetween(from, month),
                repo.observeCategories(),
            ) { recurring, tx, cats ->
                val months = (0L..5L).map { from.plusMonths(it) }
                FinanceCalculator.history(months, LocalDate.now(), recurring, tx, cats)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- Navegação entre meses ----------
    fun previousMonth() = _selectedMonth.update { it.minusMonths(1) }
    fun nextMonth() = _selectedMonth.update { it.plusMonths(1) }
    fun goToCurrentMonth() { _selectedMonth.value = YearMonth.now() }

    // ---------- Onboarding ----------
    fun finishOnboarding(salaryCents: Long, payday: Int, fixed: List<RecurringItem>) {
        viewModelScope.launch {
            val now = YearMonth.now()
            if (salaryCents > 0) {
                repo.saveRecurring(
                    RecurringItem(
                        name = "Salário",
                        amountCents = salaryCents,
                        type = EntryType.INCOME,
                        dayOfMonth = payday,
                        startMonth = now,
                    ),
                )
            }
            fixed.forEach { repo.saveRecurring(it.copy(id = 0, startMonth = now)) }
            prefs.onboardingDone = true
            _onboardingDone.value = true
        }
    }

    // ---------- Lançamentos ----------
    fun saveTransaction(t: Transaction) = viewModelScope.launch { repo.saveTransaction(t) }
    fun deleteTransaction(id: Long) = viewModelScope.launch { repo.deleteTransaction(id) }

    // ---------- Fixos ----------
    /** [old] = null para criar um novo fixo a partir do mês selecionado. */
    fun saveRecurring(old: RecurringItem?, new: RecurringItem) = viewModelScope.launch {
        val month = _selectedMonth.value
        if (old == null) repo.saveRecurring(new.copy(id = 0, startMonth = month, endMonth = null))
        else repo.updateRecurringFrom(old, new, month)
    }

    fun removeRecurring(item: RecurringItem) = viewModelScope.launch {
        repo.removeRecurringFrom(item, _selectedMonth.value)
    }

    fun setPaid(item: RecurringItem, paid: Boolean) = viewModelScope.launch {
        repo.setPaid(item.id, _selectedMonth.value, paid)
    }

    // ---------- Categorias ----------
    fun saveCategory(c: Category) = viewModelScope.launch { repo.saveCategory(c) }
    fun deleteCategory(id: Long) = viewModelScope.launch { repo.deleteCategory(id) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MeuFinanceiroApp
                AppViewModel(app.repository, app.prefs)
            }
        }
    }
}
