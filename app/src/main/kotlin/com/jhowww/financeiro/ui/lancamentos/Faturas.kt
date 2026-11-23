package com.jhowww.financeiro.ui.lancamentos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.motor.EstadoDespesa
import com.jhowww.financeiro.domain.motor.FaturaChave
import com.jhowww.financeiro.domain.motor.ItemDespesa
import com.jhowww.financeiro.domain.motor.OrigemDespesa
import com.jhowww.financeiro.domain.motor.ResultadoValidacao
import com.jhowww.financeiro.domain.motor.ResumoFatura
import com.jhowww.financeiro.domain.motor.StatusFatura
import com.jhowww.financeiro.domain.soma
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.CampoData
import com.jhowww.financeiro.ui.comum.CampoValor
import com.jhowww.financeiro.ui.comum.DialogoFormulario
import com.jhowww.financeiro.ui.comum.LinhaValor
import com.jhowww.financeiro.ui.comum.Seletor
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.curta
import com.jhowww.financeiro.ui.comum.paraCampo
import com.jhowww.financeiro.ui.theme.Cores

fun nomeStatus(status: StatusFatura): String = when (status) {
    StatusFatura.ABERTA -> "Aberta"
    StatusFatura.FECHADA -> "Fechada"
    StatusFatura.PAGA_PARCIALMENTE -> "Paga parcialmente"
    StatusFatura.PAGA -> "Paga"
    StatusFatura.VENCIDA -> "Vencida"
}

fun corStatus(status: StatusFatura): Color = when (status) {
    StatusFatura.ABERTA -> Cores.Destaque
    StatusFatura.FECHADA, StatusFatura.PAGA_PARCIALMENTE -> Cores.Atencao
    StatusFatura.PAGA -> Cores.Positivo
    StatusFatura.VENCIDA -> Cores.Negativo
}

/** Faturas do cartão em volta do mês escolhido, com total, pago e restante. */
@Composable
fun ListaFaturas(vm: FinanceiroViewModel, estado: EstadoApp, mes: Competencia, cabecalho: @Composable () -> Unit) {
    val cartoes = estado.retrato.dados.cartoes
    var cartaoId by rememberSaveable { mutableLongStateOf(cartoes.firstOrNull()?.id ?: 0L) }
    var aberta by remember { mutableStateOf<FaturaChave?>(null) }
    val cartao = cartoes.firstOrNull { it.id == cartaoId } ?: cartoes.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item { cabecalho() }
        if (cartao == null) {
            item { TextoSecundario("Nenhum cartão cadastrado. Cadastre em Ajustes para acompanhar as faturas.", Modifier.padding(vertical = 16.dp)) }
        } else {
            if (cartoes.size > 1) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cartoes.forEach { c ->
                            FilterChip(selected = c.id == cartao.id, onClick = { cartaoId = c.id }, label = { Text(c.nome) })
                        }
                    }
                }
            }
            val faturas = estado.motor.faturasDoCartao(cartao.id, mes.mais(-6), mes.mais(3)).sortedByDescending { it.chave.competencia }
            if (faturas.isEmpty()) {
                item { TextoSecundario("Nenhuma fatura com lançamentos entre ${mes.mais(-6).curta()} e ${mes.mais(3).curta()}.", Modifier.padding(vertical = 16.dp)) }
            }
            items(faturas, key = { it.chave.competencia.chave }) { f ->
                LinhaFatura(f, destaque = f.chave.competencia == mes) { aberta = f.chave }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    aberta?.let { chave -> DialogoFatura(vm, estado, chave) { aberta = null } }
}

@Composable
private fun LinhaFatura(f: ResumoFatura, destaque: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Fatura de ${f.chave.competencia.curta()}",
                style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            )
            TextoSecundario("Fecha ${f.dataFechamento.curta()} · vence ${f.dataVencimento.curta()} · ${f.itens.size} item(ns)")
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(f.total.formatar())
            Text(nomeStatus(f.status), style = MaterialTheme.typography.labelSmall, color = corStatus(f.status))
            if (f.pago > Dinheiro.ZERO && f.restante > Dinheiro.ZERO) TextoSecundario("resta ${f.restante.formatar()}")
        }
    }
}

/** Detalhe da fatura: totais, pagamentos (com exclusão), itens (com correção da fatura) e registro de pagamento. */
@Composable
private fun DialogoFatura(vm: FinanceiroViewModel, estado: EstadoApp, chave: FaturaChave, onFechar: () -> Unit) {
    val f = estado.motor.fatura(chave)
    val cartao = estado.cartoesPorId[chave.cartaoId]
    val listaPagamentos = estado.retrato.dados.pagamentosFatura.filter { p -> f.id != null && p.faturaId == f.id }
    var pagar by remember { mutableStateOf(false) }
    var mover by remember { mutableStateOf<ItemDespesa?>(null) }

    val itemMovido = mover
    if (pagar) {
        DialogoPagamentoFatura(vm, estado, f) { pagar = false }
        return
    }
    if (itemMovido != null) {
        DialogoMoverFatura(vm, estado, itemMovido) { mover = null }
        return
    }

    // Restante de faturas anteriores do mesmo cartão: só informativo (o banco costuma somar na seguinte).
    val anteriores = estado.motor.faturasDoCartao(chave.cartaoId, chave.competencia.mais(-12), chave.competencia.anterior())
        .filter { it.status != StatusFatura.PAGA && it.restante > Dinheiro.ZERO && it.dataFechamento < estado.hoje }
    val restanteAnterior = anteriores.map { it.restante }.soma()

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("${cartao?.nome ?: "Cartão"} · ${chave.competencia.curta()}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(nomeStatus(f.status), color = corStatus(f.status), style = MaterialTheme.typography.titleSmall)
                TextoSecundario("Fecha ${f.dataFechamento.curta()} · vence ${f.dataVencimento.curta()}")
                LinhaValor("Total", f.total, destaque = true)
                LinhaValor("Pago", f.pago)
                LinhaValor("Restante", f.restante, cor = if (f.restante > Dinheiro.ZERO) Cores.Atencao else Cores.Positivo)
                if (restanteAnterior > Dinheiro.ZERO) {
                    TextoSecundario(
                        "Faturas anteriores ainda têm ${restanteAnterior.formatar()} em aberto. Se pagar esse valor junto, " +
                            "registre a parte de cada fatura na fatura dela.",
                    )
                }

                if (listaPagamentos.isNotEmpty()) {
                    Text("Pagamentos", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    listaPagamentos.forEach { p ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${p.data.curta()} · ${p.valor.formatar()}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.executar("Pagamento removido") { pagamentos.excluirPagamento(p.id) } }) {
                                Text("Remover", color = Cores.Negativo)
                            }
                        }
                    }
                }

                Text("Itens", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                if (f.itens.isEmpty()) TextoSecundario("Nenhum item nesta fatura.")
                f.itens.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { mover = item }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.descricao)
                            TextoSecundario(
                                buildList {
                                    add(item.data.curta())
                                    if (item.origem == OrigemDespesa.PARCELA) add("parcela ${item.numeroParcela}/${item.totalParcelas}")
                                    if (item.origem == OrigemDespesa.FIXO) add("fixo")
                                    if (item.estado == EstadoDespesa.PREVISTA) add("ainda não cobrado")
                                }.joinToString(" · "),
                            )
                        }
                        Text(item.valor.formatar(), color = if (item.ehEstorno) Cores.Positivo else Cores.Texto)
                    }
                }
                TextoSecundario("Toque num item para mudá-lo de fatura, quando o banco processar em outra data.")
            }
        },
        confirmButton = { TextButton(onClick = { pagar = true }) { Text("Registrar pagamento") } },
        dismissButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
    )
}

@Composable
private fun DialogoPagamentoFatura(vm: FinanceiroViewModel, estado: EstadoApp, f: ResumoFatura, onFechar: () -> Unit) {
    var valor by remember { mutableStateOf(if (f.restante > Dinheiro.ZERO) f.restante.paraCampo() else "") }
    var data by remember { mutableStateOf(estado.hoje) }
    val dinheiro = Dinheiro.interpretar(valor)
    DialogoFormulario(
        titulo = "Pagar fatura de ${f.chave.competencia.curta()}",
        onFechar = onFechar,
        podeConfirmar = dinheiro != null && dinheiro > Dinheiro.ZERO,
        textoConfirmar = "Registrar",
        onConfirmar = {
            if (dinheiro != null) vm.executar("Pagamento registrado") { operacoes.pagarFatura(f.chave, dinheiro, data) }
            onFechar()
        },
    ) {
        TextoSecundario("Restante: ${f.restante.formatar()}. Pode ser um pagamento parcial; as compras só ficam pagas quando o restante zera.")
        CampoValor("Valor pago", valor, { valor = it })
        CampoData("Data do pagamento", data, { data = it })
        TextoSecundario("Pagar a fatura não altera as despesas do mês: elas já contam desde a data da compra.")
    }
}

/** Correção manual da fatura de um item do cartão. A competência da despesa não muda. */
@Composable
private fun DialogoMoverFatura(vm: FinanceiroViewModel, estado: EstadoApp, item: ItemDespesa, onFechar: () -> Unit) {
    val atual = item.fatura ?: return
    val dados = estado.retrato.dados
    val opcoes = (-3L..3L).map { FaturaChave(atual.cartaoId, atual.competencia.mais(it)) }.filter { it != atual }
    var destino by remember { mutableStateOf<FaturaChave?>(null) }
    val validacao = destino?.let { estado.motor.validarMudancaDeFatura(atual, it) }

    val manual = when (item.origem) {
        OrigemDespesa.PONTUAL -> dados.despesas.firstOrNull { it.id == item.referenciaId }?.faturaManual == true
        OrigemDespesa.PARCELA -> dados.parcelas.firstOrNull { it.id == item.referenciaId }?.faturaManual == true
        OrigemDespesa.FIXO -> dados.ocorrencias.firstOrNull { it.gastoFixoId == item.referenciaId && it.competencia == item.competencia }?.faturaManual == true
    }

    fun mover(alvo: FaturaChave?, mensagem: String) {
        vm.executar(mensagem) {
            when (item.origem) {
                OrigemDespesa.PONTUAL -> dados.despesas.firstOrNull { it.id == item.referenciaId }?.let { operacoes.moverDespesa(it, alvo) }
                OrigemDespesa.PARCELA -> {
                    val parcela = dados.parcelas.firstOrNull { it.id == item.referenciaId }
                    val compra = dados.compras.firstOrNull { it.id == item.compraId }
                    if (parcela != null && compra != null) operacoes.moverParcela(compra, parcela, alvo)
                }
                OrigemDespesa.FIXO -> {
                    val gasto = dados.gastosFixos.firstOrNull { it.id == item.referenciaId }
                    val gravada = dados.ocorrencias.firstOrNull { it.gastoFixoId == item.referenciaId && it.competencia == item.competencia }
                    if (gasto != null) operacoes.moverFixo(gasto, item.competencia, gravada, alvo)
                }
            }
        }
        onFechar()
    }

    DialogoFormulario(
        titulo = "Mudar de fatura",
        onFechar = onFechar,
        podeConfirmar = validacao == ResultadoValidacao.Valido,
        textoConfirmar = "Mover",
        onConfirmar = { destino?.let { mover(it, "Item movido para a fatura de ${it.competencia.curta()}") } },
        acaoExtra = {
            if (manual) TextButton(onClick = { mover(null, "Item voltou para a fatura automática") }) { Text("Automática") }
        },
    ) {
        Text("${item.descricao} · ${item.valor.formatar()}")
        TextoSecundario("Hoje na fatura de ${atual.competencia.curta()}${if (manual) " (corrigida à mão)" else ""}. A despesa continua contando em ${item.competencia.curta()}.")
        Seletor("Fatura de destino", opcoes, destino, { "Fatura de ${it.competencia.curta()}" }, { destino = it })
        if (validacao is ResultadoValidacao.Recusado) Text(validacao.motivo, color = Cores.Negativo)
    }
}
