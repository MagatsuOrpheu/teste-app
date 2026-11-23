package com.jhowww.financeiro.ui.lancamentos

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.motor.EstadoDespesa
import com.jhowww.financeiro.domain.motor.ItemDespesa
import com.jhowww.financeiro.domain.motor.OrigemDespesa
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.Bloco
import com.jhowww.financeiro.ui.comum.CampoData
import com.jhowww.financeiro.ui.comum.CampoTexto
import com.jhowww.financeiro.ui.comum.CampoValor
import com.jhowww.financeiro.ui.comum.DialogoConfirmacao
import com.jhowww.financeiro.ui.comum.DialogoFormulario
import com.jhowww.financeiro.ui.comum.LinhaValor
import com.jhowww.financeiro.ui.comum.PontoCor
import com.jhowww.financeiro.ui.comum.Seletor
import com.jhowww.financeiro.ui.comum.SeletorMes
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.curta
import com.jhowww.financeiro.ui.comum.paraCampo
import com.jhowww.financeiro.ui.theme.Cores
import java.time.LocalDate

private sealed interface Dialogo {
    data class Despesa(val despesa: DespesaPontual?) : Dialogo
    data class Estorno(val original: DespesaPontual) : Dialogo
    data class Excluir(val despesa: DespesaPontual) : Dialogo
}

fun textoEstado(item: ItemDespesa): String = when {
    item.estado == EstadoDespesa.PAGA -> "Paga"
    item.estado == EstadoDespesa.PREVISTA -> "Prevista"
    item.faturaPagaParcialmente -> "Fatura paga parcialmente"
    item.viaCartao -> "Na fatura"
    else -> "A pagar"
}

fun corEstado(item: ItemDespesa): Color = when (item.estado) {
    EstadoDespesa.PAGA -> Cores.Positivo
    EstadoDespesa.REGISTRADA -> Cores.Atencao
    EstadoDespesa.PREVISTA -> Cores.TextoSecundario
}

@Composable
fun TelaLancamentos(vm: FinanceiroViewModel, estado: EstadoApp, mes: Competencia) {
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    val itens = estado.motor.itensDoMes(mes).sortedWith(compareByDescending<ItemDespesa> { it.data }.thenByDescending { it.referenciaId })
    val resumo = estado.motor.resumoMes(mes)
    val despesasPorId = estado.retrato.dados.despesas.associateBy { it.id }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { SeletorMes(mes, estado.competenciaAtual, vm::mudarMes, vm::irParaMesAtual) }
            item {
                Bloco {
                    LinhaValor("Total do mês", resumo.despesas, destaque = true)
                    LinhaValor("Já pago", resumo.despesasPagas, cor = Cores.TextoSecundario)
                }
            }
            if (itens.isEmpty()) {
                item { TextoSecundario("Nenhuma despesa neste mês. Toque em + para lançar.", Modifier.padding(vertical = 16.dp)) }
            }
            items(itens, key = { "${it.origem}-${it.referenciaId}-${it.competencia}" }) { item ->
                LinhaItem(item, estado) {
                    if (item.origem == OrigemDespesa.PONTUAL) {
                        despesasPorId[item.referenciaId]?.let { dialogo = Dialogo.Despesa(it) }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        FloatingActionButton(
            onClick = { dialogo = Dialogo.Despesa(null) },
            containerColor = Cores.Destaque,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Outlined.Add, "Nova despesa") }
    }

    when (val d = dialogo) {
        null -> Unit
        is Dialogo.Despesa -> DialogoDespesa(
            vm, estado, d.despesa,
            onEstornar = { dialogo = Dialogo.Estorno(it) },
            onExcluir = { dialogo = Dialogo.Excluir(it) },
            onFechar = { dialogo = null },
        )
        is Dialogo.Estorno -> DialogoEstorno(vm, d.original) { dialogo = null }
        is Dialogo.Excluir -> {
            val temEstorno = estado.retrato.dados.despesas.any { it.estornoDe == d.despesa.id }
            if (temEstorno) {
                DialogoConfirmacao(
                    titulo = "Não é possível excluir",
                    texto = "Esta compra tem estorno ligado a ela. Exclua o estorno antes.",
                    textoConfirmar = "Entendi",
                    onConfirmar = {},
                    onFechar = { dialogo = null },
                )
            } else {
                DialogoConfirmacao(
                    titulo = "Excluir despesa?",
                    texto = "${d.despesa.descricao}: ${d.despesa.valor.formatar()}",
                    textoConfirmar = "Excluir",
                    onConfirmar = { vm.executar("Despesa excluída") { despesas.excluir(d.despesa) } },
                    onFechar = { dialogo = null },
                )
            }
        }
    }
}

@Composable
private fun LinhaItem(item: ItemDespesa, estado: EstadoApp, onClick: () -> Unit) {
    val categoria = estado.categoriasPorId[item.categoriaId]
    val detalhes = buildList {
        add(item.data.curta())
        categoria?.let { add(it.nome) }
        when (item.origem) {
            OrigemDespesa.FIXO -> add("Fixo")
            OrigemDespesa.PARCELA -> add("Parcela ${item.numeroParcela}/${item.totalParcelas}")
            OrigemDespesa.PONTUAL -> Unit
        }
        item.fatura?.let { f -> estado.cartoesPorId[f.cartaoId]?.let { add("${it.nome} ${f.competencia.curta()}") } }
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PontoCor(categoria?.let { Color(it.cor) } ?: Cores.TextoSecundario)
        Column(Modifier.weight(1f)) {
            Text(item.descricao, style = MaterialTheme.typography.bodyLarge)
            TextoSecundario(detalhes.joinToString(" · "))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.valor.formatar(), color = if (item.ehEstorno) Cores.Positivo else Cores.Texto)
            Text(textoEstado(item), style = MaterialTheme.typography.labelSmall, color = corEstado(item))
        }
    }
}

@Composable
private fun DialogoDespesa(
    vm: FinanceiroViewModel,
    estado: EstadoApp,
    despesa: DespesaPontual?,
    onEstornar: (DespesaPontual) -> Unit,
    onExcluir: (DespesaPontual) -> Unit,
    onFechar: () -> Unit,
) {
    val categorias = estado.retrato.categorias.filter { it.ativa || it.id == despesa?.categoriaId }
    val formas = estado.retrato.formas.filter { it.ativa || it.id == despesa?.formaPagamentoId }
    val cartoes = estado.retrato.dados.cartoes.filter { it.ativo || it.id == despesa?.cartaoId }
    val ehEstorno = despesa?.ehEstorno == true

    var valor by remember { mutableStateOf(despesa?.valor?.let { if (it.ehNegativo) (-it).paraCampo() else it.paraCampo() } ?: "") }
    var descricao by remember { mutableStateOf(despesa?.descricao ?: "") }
    var categoria by remember { mutableStateOf(categorias.firstOrNull { it.id == despesa?.categoriaId } ?: categorias.firstOrNull()) }
    var forma by remember { mutableStateOf<FormaPagamento?>(formas.firstOrNull { it.id == despesa?.formaPagamentoId } ?: formas.firstOrNull()) }
    var cartao by remember { mutableStateOf(cartoes.firstOrNull { it.id == despesa?.cartaoId } ?: cartoes.firstOrNull()) }
    var data by remember { mutableStateOf(despesa?.data ?: LocalDate.now()) }
    var observacao by remember { mutableStateOf(despesa?.observacao ?: "") }

    val dinheiro = Dinheiro.interpretar(valor)
    val usaCartao = forma?.tipo?.viaFatura == true
    val valido = dinheiro != null && dinheiro > Dinheiro.ZERO && descricao.isNotBlank() &&
        categoria != null && forma != null && (!usaCartao || cartao != null)

    if (categorias.isEmpty() || formas.isEmpty()) {
        DialogoConfirmacao(
            titulo = "Falta configurar",
            texto = "Cadastre pelo menos uma categoria e uma forma de pagamento em Ajustes antes de lançar despesas.",
            textoConfirmar = "Entendi",
            onConfirmar = {},
            onFechar = onFechar,
        )
        return
    }

    DialogoFormulario(
        titulo = when {
            despesa == null -> "Nova despesa"
            ehEstorno -> "Editar estorno"
            else -> "Editar despesa"
        },
        onFechar = onFechar,
        podeConfirmar = valido,
        onConfirmar = {
            val cat = categoria
            val fp = forma
            if (dinheiro != null && cat != null && fp != null) {
                val cartaoId = if (fp.tipo.viaFatura) cartao?.id else null
                val base = despesa ?: DespesaPontual(descricao = "", valor = dinheiro, data = data, categoriaId = cat.id, formaPagamentoId = fp.id)
                // Mudar data ou cartão recalcula a fatura, exceto se ela foi corrigida à mão.
                val mudouFatura = despesa != null && (despesa.data != data || despesa.cartaoId != cartaoId)
                val nova = base.copy(
                    descricao = descricao.trim(),
                    valor = if (ehEstorno) -dinheiro else dinheiro,
                    data = data,
                    competencia = Competencia.de(data),
                    categoriaId = cat.id,
                    formaPagamentoId = fp.id,
                    cartaoId = cartaoId,
                    faturaId = if (mudouFatura) null else base.faturaId,
                    faturaManual = if (mudouFatura) false else base.faturaManual,
                    observacao = observacao.trim().ifEmpty { null },
                )
                vm.executar(if (despesa == null) "Despesa lançada" else "Despesa atualizada") { operacoes.salvarDespesa(nova) }
            }
            onFechar()
        },
        acaoExtra = {
            if (despesa != null) {
                if (!ehEstorno) TextButton(onClick = { onEstornar(despesa) }) { Text("Estornar") }
                TextButton(onClick = { onExcluir(despesa) }) { Text("Excluir", color = Cores.Negativo) }
            }
        },
    ) {
        CampoValor(if (ehEstorno) "Valor estornado" else "Valor", valor, { valor = it })
        CampoTexto("Descrição", descricao, { descricao = it })
        Seletor("Categoria", categorias, categoria, { it.nome }, { categoria = it })
        Seletor("Forma de pagamento", formas, forma, { it.nome }, { forma = it })
        if (usaCartao) {
            if (cartoes.isEmpty()) TextoSecundario("Cadastre um cartão em Ajustes para lançar no crédito.")
            Seletor("Cartão", cartoes, cartao, { it.nome }, { cartao = it })
        }
        CampoData("Data", data, { data = it })
        CampoTexto("Observação (opcional)", observacao, { observacao = it })
    }
}

@Composable
private fun DialogoEstorno(vm: FinanceiroViewModel, original: DespesaPontual, onFechar: () -> Unit) {
    var valor by remember { mutableStateOf(original.valor.paraCampo()) }
    var data by remember { mutableStateOf(LocalDate.now()) }
    val dinheiro = Dinheiro.interpretar(valor)
    DialogoFormulario(
        titulo = "Estornar ${original.descricao}",
        onFechar = onFechar,
        podeConfirmar = dinheiro != null && dinheiro > Dinheiro.ZERO && dinheiro <= original.valor,
        textoConfirmar = "Estornar",
        onConfirmar = {
            if (dinheiro != null) vm.executar("Estorno lançado") { operacoes.estornar(original, dinheiro, data) }
            onFechar()
        },
    ) {
        TextoSecundario("O estorno entra no mês em que aconteceu e reduz a fatura dessa data. O mês da compra não muda.")
        CampoValor("Valor estornado (até ${original.valor.formatar()})", valor, { valor = it })
        CampoData("Data do estorno", data, { data = it })
    }
}
