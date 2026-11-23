package com.jhowww.financeiro.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.AporteReserva
import com.jhowww.financeiro.domain.modelo.RecebimentoRenda
import com.jhowww.financeiro.domain.modelo.RendaPrincipal
import com.jhowww.financeiro.domain.modelo.RetiradaReserva
import com.jhowww.financeiro.domain.motor.Alerta
import com.jhowww.financeiro.domain.motor.MotorFinanceiro
import com.jhowww.financeiro.domain.motor.QualidadeEstimativa
import com.jhowww.financeiro.domain.motor.ResumoMes
import com.jhowww.financeiro.domain.motor.Severidade
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.Bloco
import com.jhowww.financeiro.ui.comum.CampoData
import com.jhowww.financeiro.ui.comum.CampoTexto
import com.jhowww.financeiro.ui.comum.CampoValor
import com.jhowww.financeiro.ui.comum.DialogoFormulario
import com.jhowww.financeiro.ui.comum.LinhaValor
import com.jhowww.financeiro.ui.comum.PontoCor
import com.jhowww.financeiro.ui.comum.SeletorMes
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.comoPercentual
import com.jhowww.financeiro.ui.comum.curta
import com.jhowww.financeiro.ui.comum.paraCampo
import com.jhowww.financeiro.ui.theme.Cores
import java.time.LocalDate

@Composable
fun TelaInicio(vm: FinanceiroViewModel, estado: EstadoApp, mes: Competencia, onIrParaAjustes: () -> Unit) {
    val resumo = estado.motor.resumoMes(mes)
    var confirmarSalario by remember { mutableStateOf(false) }
    var movimentoReserva by remember { mutableStateOf<Boolean?>(null) } // true = aporte, false = retirada

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SeletorMes(mes, estado.competenciaAtual, vm::mudarMes, vm::irParaMesAtual)

        if (estado.retrato.dados.rendas.isEmpty()) {
            Bloco(titulo = "Comece por aqui") {
                Text("Cadastre sua renda principal, a meta da reserva, as categorias e as formas de pagamento em Ajustes.")
                Button(onClick = onIrParaAjustes) { Text("Ir para Ajustes") }
            }
        }

        DestaqueSaldo(resumo, mes, estado.competenciaAtual, onConfirmar = { confirmarSalario = true })
        Comprometimento(resumo, estado.retrato.dados.configuracao.limiteComprometimentoBp)

        Bloco(titulo = "Saldo ao longo do mês") {
            GraficoSaldo(estado.motor.serieSaldo(mes))
            TextoSecundario("Linha contínua: o que já aconteceu. Tracejada: projeção até o fim do mês.")
        }

        Bloco(titulo = "Orçamento") {
            LinhaValor("Renda projetada", resumo.rendaProjetada)
            if (resumo.receitasRecebidas > Dinheiro.ZERO) {
                TextoSecundario("Inclui ${resumo.receitasRecebidas.formatar()} em receitas adicionais recebidas")
            }
            LinhaValor("Reserva planejada", -resumo.reservaPlanejada)
            LinhaValor("Orçamento do mês", resumo.orcamentoProjetado, destaque = true)
            if (resumo.receitasAReceber > Dinheiro.ZERO) {
                TextoSecundario("A receber (fora do orçamento): ${resumo.receitasAReceber.formatar()}")
            }
        }

        Bloco(titulo = "Despesas do mês") {
            LinhaValor("Gastos fixos", resumo.despesasFixas)
            LinhaValor("Parcelas", resumo.despesasParceladas)
            LinhaValor("Gastos variáveis", resumo.despesasVariaveis)
            LinhaValor("Total", resumo.despesas, destaque = true)
            LinhaValor("Já pago", resumo.despesasPagas, cor = Cores.TextoSecundario)
            LinhaValor("Saldo disponível", resumo.saldoDisponivel, destaque = true)
        }

        Bloco(titulo = "Reserva") {
            LinhaValor("Meta do mês", resumo.reservaPlanejada)
            LinhaValor("Guardado no mês", resumo.reservaRealizada, destaque = true)
            TextoSecundario("Aportes ${resumo.aportesReserva.formatar()} · retiradas ${resumo.retiradasReserva.formatar()}")
            LinhaValor("Total acumulado", estado.motor.reservaAcumulada(mes))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { movimentoReserva = true }) { Text("Aporte") }
                OutlinedButton(onClick = { movimentoReserva = false }) { Text("Retirada") }
            }
        }

        Bloco(titulo = "Sobra do mês") {
            LinhaValor("Renda confirmada − despesas − reserva guardada", resumo.sobra, destaque = true)
            TextoSecundario("A sobra não vira reserva sozinha: para guardar, registre um aporte.")
        }

        if (mes == estado.competenciaAtual) {
            val alertas = estado.motor.alertas()
            if (alertas.isNotEmpty()) {
                Bloco(titulo = "Alertas") { alertas.forEach { LinhaAlerta(it) } }
            }
        }
    }

    if (confirmarSalario) {
        DialogoConfirmarSalario(vm, estado, mes) { confirmarSalario = false }
    }
    movimentoReserva?.let { aporte ->
        DialogoMovimentoReserva(vm, aporte) { movimentoReserva = null }
    }
}

@Composable
private fun DestaqueSaldo(resumo: ResumoMes, mes: Competencia, atual: Competencia, onConfirmar: () -> Unit) {
    Bloco(titulo = "Saldo projetado do mês") {
        Text(
            resumo.saldoProjetado.formatar(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (resumo.saldoProjetado.ehNegativo) Cores.Negativo else Cores.Positivo,
        )
        val est = resumo.estimativaVariaveis
        when (est.qualidade) {
            QualidadeEstimativa.HISTORICO ->
                TextoSecundario("Considera ${est.valor.formatar()} estimados em gastos variáveis até o fim do mês (mediana dos meses anteriores).")
            QualidadeEstimativa.RITMO_DO_MES ->
                TextoSecundario("Considera ${est.valor.formatar()} estimados pelo ritmo deste mês. Estimativa fraca: ainda há pouco histórico.")
            QualidadeEstimativa.SEM_DADOS ->
                TextoSecundario("Sem histórico para estimar os gastos variáveis deste mês.")
            QualidadeEstimativa.MES_ENCERRADO -> TextoSecundario("Mês encerrado.")
        }
        if (resumo.salarioAConfirmar) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoCor(Cores.Atencao)
                Text(
                    "  Salário a confirmar · saldo confirmado ${resumo.saldoConfirmado.formatar()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cores.Atencao,
                    modifier = Modifier.weight(1f),
                )
                if (mes <= atual) TextButton(onClick = onConfirmar) { Text("Confirmar") }
            }
        }
    }
}

@Composable
private fun Comprometimento(resumo: ResumoMes, limiteBp: Int) {
    val bp = resumo.comprometimentoBp
    Bloco(titulo = "Comprometimento do orçamento") {
        if (bp == null) {
            TextoSecundario("Sem orçamento positivo neste mês para calcular o comprometimento.")
        } else {
            val cor = when {
                bp > limiteBp -> Cores.Negativo
                bp > limiteBp * 8 / 10 -> Cores.Atencao
                else -> Cores.Positivo
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bp.comoPercentual(), style = MaterialTheme.typography.titleMedium, color = cor, modifier = Modifier.weight(1f))
                TextoSecundario("${resumo.despesas.formatar()} de ${resumo.orcamentoProjetado.formatar()} · limite ${limiteBp.comoPercentual()}")
            }
            LinearProgressIndicator(
                progress = { (bp / 10_000f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = cor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun LinhaAlerta(alerta: Alerta) {
    val cor = when (alerta.severidade) {
        Severidade.CRITICO -> Cores.Negativo
        Severidade.ATENCAO -> Cores.Atencao
        Severidade.INFO -> Cores.Destaque
    }
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PontoCor(cor, Modifier.padding(top = 5.dp))
        Text(alerta.mensagem, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Rendas com previsão no mês e ainda sem recebimento confirmado. */
private fun rendasAConfirmar(estado: EstadoApp, mes: Competencia): List<Pair<RendaPrincipal, Dinheiro>> {
    val dados = estado.retrato.dados
    return dados.rendas.mapNotNull { renda ->
        if (dados.recebimentos.any { it.rendaId == renda.id && it.competencia == mes }) return@mapNotNull null
        MotorFinanceiro.valorVigente(dados.versoesRenda[renda.id], mes)?.let { renda to it }
    }
}

@Composable
private fun DialogoConfirmarSalario(vm: FinanceiroViewModel, estado: EstadoApp, mes: Competencia, onFechar: () -> Unit) {
    val pendente = rendasAConfirmar(estado, mes).firstOrNull()
    if (pendente == null) {
        LaunchedEffect(Unit) { onFechar() }
        return
    }
    val (renda, previsto) = pendente
    var valor by remember(renda.id) { mutableStateOf(previsto.paraCampo()) }
    var data by remember(renda.id) { mutableStateOf(if (mes == estado.competenciaAtual) estado.hoje else mes.diaEfetivo(renda.diaRecebimento)) }
    val dinheiro = Dinheiro.interpretar(valor)
    DialogoFormulario(
        titulo = "Confirmar ${renda.descricao}",
        onFechar = onFechar,
        podeConfirmar = dinheiro != null && dinheiro > Dinheiro.ZERO,
        textoConfirmar = "Confirmar",
        onConfirmar = {
            if (dinheiro != null) {
                vm.executar("Recebimento confirmado") {
                    rendas.confirmarRecebimento(RecebimentoRenda(rendaId = renda.id, competencia = mes, valorRecebido = dinheiro, confirmadoEm = data))
                }
            }
            onFechar()
        },
    ) {
        TextoSecundario("Previsto: ${previsto.formatar()}. Se veio diferente, ajuste o valor: vale só para ${mes.curta()}.")
        CampoValor("Valor recebido", valor, { valor = it })
        CampoData("Data do recebimento", data, { data = it })
    }
}

@Composable
private fun DialogoMovimentoReserva(vm: FinanceiroViewModel, aporte: Boolean, onFechar: () -> Unit) {
    var valor by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(LocalDate.now()) }
    var nota by remember { mutableStateOf("") }
    val dinheiro = Dinheiro.interpretar(valor)
    DialogoFormulario(
        titulo = if (aporte) "Aporte na reserva" else "Retirada da reserva",
        onFechar = onFechar,
        podeConfirmar = dinheiro != null && dinheiro > Dinheiro.ZERO,
        onConfirmar = {
            val texto = nota.trim().ifEmpty { null }
            if (dinheiro != null) {
                vm.executar(if (aporte) "Aporte registrado" else "Retirada registrada") {
                    if (aporte) reserva.registrarAporte(AporteReserva(data = data, valor = dinheiro, observacao = texto))
                    else reserva.registrarRetirada(RetiradaReserva(data = data, valor = dinheiro, motivo = texto))
                }
            }
            onFechar()
        },
    ) {
        CampoValor("Valor", valor, { valor = it })
        CampoData("Data", data, { data = it })
        CampoTexto(if (aporte) "Observação (opcional)" else "Motivo (opcional)", nota, { nota = it })
    }
}
