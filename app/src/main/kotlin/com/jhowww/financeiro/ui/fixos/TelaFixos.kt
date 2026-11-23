package com.jhowww.financeiro.ui.fixos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.GastoFixo
import com.jhowww.financeiro.domain.motor.MotorFinanceiro
import com.jhowww.financeiro.domain.motor.OrigemDespesa
import com.jhowww.financeiro.domain.soma
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.Bloco
import com.jhowww.financeiro.ui.comum.DialogoConfirmacao
import com.jhowww.financeiro.ui.comum.LinhaValor
import com.jhowww.financeiro.ui.comum.PontoCor
import com.jhowww.financeiro.ui.comum.SeletorMes
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.curta
import com.jhowww.financeiro.ui.lancamentos.corEstado
import com.jhowww.financeiro.ui.lancamentos.textoEstado
import com.jhowww.financeiro.ui.theme.Cores

private sealed interface Dialogo {
    data object NovoFixo : Dialogo
    data class MenuFixo(val gasto: GastoFixo) : Dialogo
    data class EditarFixo(val gasto: GastoFixo) : Dialogo
    data class NovoValor(val gasto: GastoFixo) : Dialogo
    data class Encerrar(val gasto: GastoFixo) : Dialogo
    data class ExcluirFixo(val gasto: GastoFixo) : Dialogo
    data class Ocorrencia(val gasto: GastoFixo) : Dialogo
    data object NovaCompra : Dialogo
    data class Compra(val compra: CompraParcelada) : Dialogo
}

@Composable
fun TelaFixos(vm: FinanceiroViewModel, estado: EstadoApp, mes: Competencia) {
    var secao by rememberSaveable { mutableIntStateOf(0) }
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    val dados = estado.retrato.dados
    val resumo = estado.motor.resumoMes(mes)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { SeletorMes(mes, estado.competenciaAtual, vm::mudarMes, vm::irParaMesAtual) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = secao == 0, onClick = { secao = 0 }, label = { Text("Gastos fixos") })
                    FilterChip(selected = secao == 1, onClick = { secao = 1 }, label = { Text("Parcelamentos") })
                }
            }
            if (secao == 0) {
                item {
                    Bloco {
                        LinhaValor("Gastos fixos de ${mes.curta()}", resumo.despesasFixas, destaque = true)
                        TextoSecundario("Toque num gasto para pagar, pular, mudar o valor ou encerrar.")
                    }
                }
                if (dados.gastosFixos.isEmpty()) {
                    item { TextoSecundario("Nenhum gasto fixo. Toque em + para cadastrar aluguel, internet, assinaturas…", Modifier.padding(vertical = 16.dp)) }
                }
                items(dados.gastosFixos, key = { it.id }) { gasto ->
                    LinhaGasto(gasto, estado, mes) { dialogo = Dialogo.MenuFixo(gasto) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            } else {
                val parcelasPorCompra = dados.parcelas.groupBy { it.compraId }
                val emAndamento = dados.compras.filter { c -> parcelasPorCompra[c.id].orEmpty().any { it.competencia >= mes } }
                val concluidas = dados.compras.filter { it !in emAndamento }
                val aPagarDepois = dados.parcelas.filter { it.competencia > mes }.map { it.valor }.soma()
                item {
                    Bloco {
                        LinhaValor("Parcelas de ${mes.curta()}", resumo.despesasParceladas, destaque = true)
                        LinhaValor("Parcelas nos meses seguintes", aPagarDepois, cor = Cores.TextoSecundario)
                    }
                }
                if (dados.compras.isEmpty()) {
                    item { TextoSecundario("Nenhuma compra parcelada. Toque em + para lançar.", Modifier.padding(vertical = 16.dp)) }
                }
                items(emAndamento, key = { it.id }) { compra ->
                    LinhaCompra(compra, estado, mes) { dialogo = Dialogo.Compra(compra) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (concluidas.isNotEmpty()) {
                    item { Text("Concluídas", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp)) }
                    items(concluidas, key = { "c${it.id}" }) { compra ->
                        LinhaCompra(compra, estado, mes) { dialogo = Dialogo.Compra(compra) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { dialogo = if (secao == 0) Dialogo.NovoFixo else Dialogo.NovaCompra },
            containerColor = Cores.Destaque,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Outlined.Add, if (secao == 0) "Novo gasto fixo" else "Nova compra parcelada") }
    }

    val fechar = { dialogo = null }
    when (val d = dialogo) {
        null -> Unit
        Dialogo.NovoFixo -> DialogoGastoFixo(vm, estado, null, fechar)
        is Dialogo.MenuFixo -> MenuGasto(
            gasto = d.gasto,
            temOcorrencia = estado.motor.itensDoMes(mes).any { it.origem == OrigemDespesa.FIXO && it.referenciaId == d.gasto.id } ||
                dados.ocorrencias.any { it.gastoFixoId == d.gasto.id && it.competencia == mes },
            mes = mes,
            onEscolher = { dialogo = it },
            onFechar = fechar,
        )
        is Dialogo.EditarFixo -> DialogoGastoFixo(vm, estado, d.gasto, fechar)
        is Dialogo.NovoValor -> DialogoNovoValorFixo(vm, estado, d.gasto, fechar)
        is Dialogo.Encerrar -> DialogoEncerrarFixo(vm, estado, d.gasto, fechar)
        is Dialogo.Ocorrencia -> DialogoOcorrencia(vm, estado, d.gasto, mes, fechar)
        is Dialogo.ExcluirFixo -> DialogoConfirmacao(
            titulo = "Excluir ${d.gasto.nome}?",
            texto = "Apaga o gasto e todo o histórico de pagamentos dele, inclusive dos meses anteriores. " +
                "Para apenas parar de cobrar, use \"Encerrar\".",
            textoConfirmar = "Excluir",
            onConfirmar = { vm.executar("Gasto fixo excluído") { gastosFixos.excluir(d.gasto.id) } },
            onFechar = fechar,
        )
        Dialogo.NovaCompra -> DialogoNovaCompra(vm, estado, fechar)
        is Dialogo.Compra -> DialogoCompra(vm, estado, d.compra, fechar)
    }
}

@Composable
private fun MenuGasto(
    gasto: GastoFixo,
    temOcorrencia: Boolean,
    mes: Competencia,
    onEscolher: (Dialogo) -> Unit,
    onFechar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(gasto.nome) },
        text = {
            Column {
                if (temOcorrencia) {
                    TextButton(onClick = { onEscolher(Dialogo.Ocorrencia(gasto)) }) { Text("Situação de ${mes.curta()}: pagar, pular, valor do mês") }
                }
                TextButton(onClick = { onEscolher(Dialogo.EditarFixo(gasto)) }) { Text("Editar") }
                TextButton(onClick = { onEscolher(Dialogo.NovoValor(gasto)) }) { Text("Novo valor a partir de um mês") }
                TextButton(onClick = { onEscolher(Dialogo.Encerrar(gasto)) }) { Text(if (gasto.fim == null) "Encerrar" else "Mudar encerramento") }
                TextButton(onClick = { onEscolher(Dialogo.ExcluirFixo(gasto)) }) { Text("Excluir", color = Cores.Negativo) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onFechar) { Text("Fechar") } },
    )
}

@Composable
private fun LinhaGasto(gasto: GastoFixo, estado: EstadoApp, mes: Competencia, onClick: () -> Unit) {
    val dados = estado.retrato.dados
    val item = estado.motor.itensDoMes(mes).firstOrNull { it.origem == OrigemDespesa.FIXO && it.referenciaId == gasto.id }
    val valor = item?.valor ?: MotorFinanceiro.valorVigente(dados.versoesFixo[gasto.id], mes) ?: Dinheiro.ZERO
    val categoria = estado.categoriasPorId[gasto.categoriaId]
    val pagamento = gasto.cartaoId?.let { estado.cartoesPorId[it]?.nome } ?: estado.formasPorId[gasto.formaPagamentoId]?.nome
    val detalhes = buildList {
        add(nomePeriodicidade(gasto.intervaloMeses))
        add("dia ${gasto.diaVencimento}")
        pagamento?.let { add(it) }
        if (gasto.debitoAutomatico) add("débito automático")
        gasto.fim?.let { add("até ${it.curta()}") }
        if (!gasto.ativo) add("desativado")
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PontoCor(categoria?.let { Color(it.cor) } ?: Cores.TextoSecundario)
        Column(Modifier.weight(1f)) {
            Text(gasto.nome, style = MaterialTheme.typography.bodyLarge)
            TextoSecundario(detalhes.joinToString(" · "))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(valor.formatar(), color = if (item == null) Cores.TextoSecundario else Cores.Texto)
            if (item != null) {
                Text(textoEstado(item), style = MaterialTheme.typography.labelSmall, color = corEstado(item))
            } else {
                Text("fora deste mês", style = MaterialTheme.typography.labelSmall, color = Cores.TextoSecundario)
            }
        }
    }
}

@Composable
private fun LinhaCompra(compra: CompraParcelada, estado: EstadoApp, mes: Competencia, onClick: () -> Unit) {
    val parcelas = estado.retrato.dados.parcelas.filter { it.compraId == compra.id }
    val doMes = parcelas.firstOrNull { it.competencia == mes }
    val restante = parcelas.filter { it.competencia > mes }.map { it.valor }.soma()
    val categoria = estado.categoriasPorId[compra.categoriaId]
    val detalhes = buildList {
        add(doMes?.let { "parcela ${it.numero}/${compra.numeroParcelas} em ${mes.curta()}" } ?: "${compra.numeroParcelas}× desde ${compra.primeiraCompetencia.curta()}")
        compra.cartaoId?.let { id -> estado.cartoesPorId[id]?.let { add(it.nome) } }
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PontoCor(categoria?.let { Color(it.cor) } ?: Cores.TextoSecundario)
        Column(Modifier.weight(1f)) {
            Text(compra.descricao, style = MaterialTheme.typography.bodyLarge)
            TextoSecundario(detalhes.joinToString(" · "))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(doMes?.valor?.formatar() ?: compra.valorTotal.formatar())
            TextoSecundario(if (restante > Dinheiro.ZERO) "restam ${restante.formatar()}" else "total")
        }
    }
}
