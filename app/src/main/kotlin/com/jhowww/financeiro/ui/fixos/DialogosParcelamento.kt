package com.jhowww.financeiro.ui.fixos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
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
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.motor.EstadoDespesa
import com.jhowww.financeiro.domain.motor.OrigemDespesa
import com.jhowww.financeiro.domain.motor.Parcelamento
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.CampoData
import com.jhowww.financeiro.ui.comum.CampoTexto
import com.jhowww.financeiro.ui.comum.CampoValor
import com.jhowww.financeiro.ui.comum.DialogoConfirmacao
import com.jhowww.financeiro.ui.comum.DialogoFormulario
import com.jhowww.financeiro.ui.comum.Seletor
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.completa
import com.jhowww.financeiro.ui.comum.curta
import com.jhowww.financeiro.ui.comum.nome
import com.jhowww.financeiro.ui.theme.Cores
import java.time.LocalDate

/** Nova compra parcelada: gera todas as parcelas de uma vez (primeira no mês da compra, ajustável). */
@Composable
fun DialogoNovaCompra(vm: FinanceiroViewModel, estado: EstadoApp, onFechar: () -> Unit) {
    val categorias = estado.retrato.categorias.filter { it.ativa }
    val formas = estado.retrato.formas.filter { it.ativa }
    val cartoes = estado.retrato.dados.cartoes.filter { it.ativo }

    var descricao by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var data by remember { mutableStateOf(LocalDate.now()) }
    var primeira by remember { mutableStateOf(Parcelamento.primeiraCompetenciaPadrao(LocalDate.now())) }
    var categoria by remember { mutableStateOf(categorias.firstOrNull()) }
    var forma by remember { mutableStateOf(formas.firstOrNull()) }
    var cartao by remember { mutableStateOf(cartoes.firstOrNull()) }
    var comJuros by remember { mutableStateOf(false) }
    var valorParcela by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }

    if (categorias.isEmpty() || formas.isEmpty()) {
        DialogoConfirmacao(
            titulo = "Falta configurar",
            texto = "Cadastre pelo menos uma categoria e uma forma de pagamento em Ajustes.",
            textoConfirmar = "Entendi",
            onConfirmar = {},
            onFechar = onFechar,
        )
        return
    }

    val valorTotal = Dinheiro.interpretar(total)
    val n = numero.trim().toIntOrNull()?.takeIf { it in 1..120 }
    val parcela = if (comJuros) Dinheiro.interpretar(valorParcela) else null
    val usaCartao = forma?.tipo?.viaFatura == true
    val previa = if (valorTotal != null && valorTotal > Dinheiro.ZERO && n != null && (!comJuros || (parcela != null && parcela > Dinheiro.ZERO))) {
        Parcelamento.gerar(valorTotal, n, primeira, parcela)
    } else {
        null
    }
    val valido = previa != null && descricao.isNotBlank() && categoria != null && forma != null && (!usaCartao || cartao != null)

    DialogoFormulario(
        titulo = "Nova compra parcelada",
        onFechar = onFechar,
        podeConfirmar = valido,
        onConfirmar = {
            val cat = categoria
            val fp = forma
            if (valorTotal != null && n != null && cat != null && fp != null) {
                val compra = CompraParcelada(
                    descricao = descricao.trim(),
                    valorTotal = valorTotal,
                    numeroParcelas = n,
                    dataCompra = data,
                    primeiraCompetencia = primeira,
                    categoriaId = cat.id,
                    formaPagamentoId = fp.id,
                    cartaoId = if (fp.tipo.viaFatura) cartao?.id else null,
                    observacao = observacao.trim().ifEmpty { null },
                )
                vm.executar("Compra parcelada lançada") { operacoes.lancarCompraParcelada(compra, parcela) }
            }
            onFechar()
        },
    ) {
        CampoTexto("Descrição", descricao, { descricao = it })
        CampoValor("Valor total", total, { total = it })
        CampoTexto("Número de parcelas", numero, { numero = it }, numerico = true)
        CampoData("Data da compra", data, {
            data = it
            primeira = Parcelamento.primeiraCompetenciaPadrao(it)
        })
        Seletor("Primeira parcela em", mesesParaEscolha(Competencia.de(data)), primeira, { it.nome() }, { primeira = it })
        Seletor("Categoria", categorias, categoria, { it.nome }, { categoria = it })
        SeletorPagamento(formas, cartoes, forma, cartao, { forma = it }, { cartao = it })
        LinhaInterruptor("Parcela com juros", comJuros) { comJuros = it }
        if (comJuros) {
            CampoValor("Valor de cada parcela", valorParcela, { valorParcela = it })
            TextoSecundario("Com juros, o total e o valor da parcela são guardados como informados, sem ajuste.")
        }
        CampoTexto("Observação (opcional)", observacao, { observacao = it })
        if (previa != null) {
            val ultima = previa.last()
            val texto = if (previa.size > 1 && ultima.valor != previa.first().valor) {
                "${previa.size - 1} × ${previa.first().valor.formatar()} + 1 × ${ultima.valor.formatar()}"
            } else {
                "${previa.size} × ${previa.first().valor.formatar()}"
            }
            TextoSecundario("$texto, de ${previa.first().competencia.curta()} a ${ultima.competencia.curta()}")
        }
    }
}

/** Detalhes de uma compra parcelada: parcelas, pagamento manual (fora do cartão), mudar a 1ª parcela, editar, excluir. */
@Composable
fun DialogoCompra(vm: FinanceiroViewModel, estado: EstadoApp, compra: CompraParcelada, onFechar: () -> Unit) {
    val parcelas = estado.retrato.dados.parcelas.filter { it.compraId == compra.id }.sortedBy { it.numero }
    val categorias = estado.retrato.categorias.filter { it.ativa || it.id == compra.categoriaId }
    val noCartao = compra.cartaoId != null
    var descricao by remember { mutableStateOf(compra.descricao) }
    var categoria by remember { mutableStateOf(categorias.firstOrNull { it.id == compra.categoriaId }) }
    var primeira by remember { mutableStateOf(compra.primeiraCompetencia) }
    var excluir by remember { mutableStateOf(false) }

    if (excluir) {
        DialogoConfirmacao(
            titulo = "Excluir ${compra.descricao}?",
            texto = "A compra e todas as ${compra.numeroParcelas} parcelas serão apagadas, inclusive dos meses anteriores.",
            textoConfirmar = "Excluir",
            onConfirmar = { vm.executar("Compra excluída") { parcelamentos.excluir(compra.id) } },
            onFechar = onFechar,
        )
        return
    }

    DialogoFormulario(
        titulo = compra.descricao,
        onFechar = onFechar,
        podeConfirmar = descricao.isNotBlank() && categoria != null,
        onConfirmar = {
            val cat = categoria
            if (cat != null && (descricao.trim() != compra.descricao || cat.id != compra.categoriaId)) {
                vm.executar("Compra atualizada") {
                    parcelamentos.substituir(compra.copy(descricao = descricao.trim(), categoriaId = cat.id), parcelas)
                }
            }
            onFechar()
        },
        acaoExtra = { TextButton(onClick = { excluir = true }) { Text("Excluir", color = Cores.Negativo) } },
    ) {
        TextoSecundario(
            "${compra.valorTotal.formatar()} em ${compra.numeroParcelas}× · compra em ${compra.dataCompra.completa()}" +
                (compra.cartaoId?.let { id -> estado.cartoesPorId[id]?.let { " · ${it.nome}" } } ?: ""),
        )
        CampoTexto("Descrição", descricao, { descricao = it })
        Seletor("Categoria", categorias, categoria, { it.nome }, { categoria = it })

        Text("Parcelas", style = MaterialTheme.typography.labelLarge)
        parcelas.forEach { p ->
            val item = estado.motor.itensDoMes(p.competencia).firstOrNull { it.origem == OrigemDespesa.PARCELA && it.referenciaId == p.id }
            val situacao = when (item?.estado) {
                EstadoDespesa.PAGA -> "paga"
                EstadoDespesa.REGISTRADA -> if (item?.faturaPagaParcialmente == true) "fatura paga parcialmente" else "na fatura"
                else -> "prevista"
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${p.numero}/${compra.numeroParcelas} · ${p.competencia.curta()}", modifier = Modifier.weight(1f))
                Text(p.valor.formatar())
                if (noCartao) {
                    TextoSecundario("  $situacao")
                } else {
                    Checkbox(
                        checked = p.pagaEm != null,
                        onCheckedChange = { marcado ->
                            val atualizada = p.copy(pagaEm = if (marcado) estado.hoje else null)
                            vm.executar(if (marcado) "Parcela paga" else "Pagamento desfeito") { parcelamentos.atualizarParcela(atualizada) }
                        },
                    )
                }
            }
        }

        Seletor("Primeira parcela em", mesesParaEscolha(compra.primeiraCompetencia), primeira, { it.nome() }, { primeira = it })
        if (primeira != compra.primeiraCompetencia) {
            TextButton(onClick = {
                vm.executar("Parcelas deslocadas") { operacoes.mudarPrimeiraParcela(compra, parcelas, primeira) }
                onFechar()
            }) { Text("Deslocar todas as parcelas para começar em ${primeira.curta()}") }
        }
    }
}
