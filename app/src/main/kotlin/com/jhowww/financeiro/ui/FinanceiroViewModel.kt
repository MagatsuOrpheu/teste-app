package com.jhowww.financeiro.ui

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jhowww.financeiro.AppContainer
import com.jhowww.financeiro.FinanceiroApp
import com.jhowww.financeiro.data.repositorio.Retrato
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.modelo.Categoria
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.motor.MotorFinanceiro
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Retrato do banco num dia, com o motor pronto para calcular. */
class EstadoApp(val retrato: Retrato, val hoje: LocalDate) {
    val motor = MotorFinanceiro(retrato.dados, hoje)
    val competenciaAtual: Competencia = Competencia.de(hoje)
    val categoriasPorId: Map<Long, Categoria> = retrato.categorias.associateBy { it.id }
    val formasPorId: Map<Long, FormaPagamento> = retrato.formas.associateBy { it.id }
    val cartoesPorId = retrato.dados.cartoes.associateBy { it.id }
}

/**
 * Único ViewModel do app, compartilhado pelas abas: todas leem o mesmo retrato, então
 * indicadores, listas e gráficos nunca divergem.
 */
class FinanceiroViewModel(val container: AppContainer) : ViewModel() {

    private val hoje = MutableStateFlow(LocalDate.now())

    val estado: StateFlow<EstadoApp?> = combine(container.retrato.observar(), hoje) { retrato, dia -> EstadoApp(retrato, dia) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _mes = MutableStateFlow(Competencia.de(LocalDate.now()))
    val mes: StateFlow<Competencia> = _mes.asStateFlow()

    private val _mensagens = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensagens: SharedFlow<String> = _mensagens

    fun mudarMes(meses: Long) {
        _mes.value = _mes.value.mais(meses)
    }

    fun irParaMesAtual() {
        _mes.value = Competencia.de(hoje.value)
    }

    /** Ao voltar ao app num outro dia, recalcula estados (débitos, faturas, alertas). */
    fun atualizarDia() {
        val agora = LocalDate.now()
        if (agora != hoje.value) hoje.value = agora
    }

    /** Executa uma gravação; erros de validação viram mensagem na tela em vez de travar o app. */
    fun executar(sucesso: String? = null, acao: suspend AppContainer.() -> Unit) {
        viewModelScope.launch {
            try {
                container.acao()
                sucesso?.let { _mensagens.emit(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalArgumentException) {
                _mensagens.emit(e.message ?: "Dados inválidos")
            } catch (e: SQLiteConstraintException) {
                _mensagens.emit("Não foi possível salvar: o registro está em uso ou já existe")
            } catch (e: Exception) {
                _mensagens.emit("Não foi possível salvar (${e.javaClass.simpleName})")
            }
        }
    }

    companion object {
        val Fabrica = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FinanceiroApp
                FinanceiroViewModel(app.container)
            }
        }
    }
}
