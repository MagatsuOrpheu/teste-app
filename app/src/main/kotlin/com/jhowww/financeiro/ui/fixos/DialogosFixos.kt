package com.jhowww.financeiro.ui.fixos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
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
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.Categoria
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.modelo.GastoFixo
import com.jhowww.financeiro.domain.modelo.OcorrenciaFixo
import com.jhowww.financeiro.domain.modelo.StatusOcorrencia
import com.jhowww.financeiro.domain.motor.EstadoDespesa
import com.jhowww.financeiro.domain.motor.MotorFinanceiro
import com.jhowww.financeiro.domain.motor.OrigemDespesa
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.CampoTexto
import com.jhowww.financeiro.ui.comum.CampoValor
import com.jhowww.financeiro.ui.comum.DialogoConfirmacao
import com.jhowww.financeiro.ui.comum.DialogoFormulario
import com.jhowww.financeiro.ui.comum.Seletor
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.curta
import com.jhowww.financeiro.ui.comum.nome
import com.jhowww.financeiro.ui.comum.paraCampo
import com.jhowww.financeiro.ui.theme.Cores

/** Periodicidades oferecidas; "a cada N meses" usa o campo livre. */
val Periodicidades: List<Pair<Int, String>> = listOf(
    1 to "Mensal", 2 to "Bimestral", 3 to "Trimestral", 6 to "Semestral", 12 to "Anual", 0 to "A cada N meses",
)

fun nomePeriodicidade(intervalo: Int): String =
    Periodicidades.firstOrNull { it.first == intervalo && it.first != 0 }?.second ?: "A cada $intervalo meses"

/** Meses oferecidos nos seletores de competência: do ano anterior aos próximos dois anos. */
fun mesesParaEscolha(referencia: Competencia): List<Competencia> = (-12L..24L).map { referencia.mais(it) }

@Composable
fun LinhaInterruptor(rotulo: String, valor: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onChange)
    }
}

/** Forma de pagamento e, se for crédito, o cartão. */
@Composable
fun SeletorPagamento(
    formas: List<FormaPagamento>,
    cartoes: List<Cartao>,
    forma: FormaPagamento?,
    cartao: Cartao?,
    onForma: (FormaPagamento) -> Unit,
    onCartao: (Cartao) -> Unit,
) {
    Seletor("Forma de pagamento", formas, forma, { it.nome }, onForma)
    if (forma?.tipo?.viaFatura == true) {
        if (cartoes.isEmpty()) TextoSecundario("Cadastre um cartão em Ajustes para usar o crédito.")
        Seletor("Cartão", cartoes, cartao, { it.nome }, onCartao)
    }
}

/** Cadastro ou edição de gasto fixo. O valor só é pedido na criação; depois, use "Novo valor". */
@Composable
fun DialogoGastoFixo(vm: FinanceiroViewModel, estado: EstadoApp, gasto: GastoFixo?, onFechar: () -> Unit) {
    val categorias: List<Categoria> = estado.retrato.categorias.filter { it.ativa || it.id == gasto?.categoriaId }
    val formas = estado.retrato.formas.filter { it.ativa || it.id == gasto?.formaPagamentoId }
    val cartoes = estado.retrato.dados.cartoes.filter { it.ativo || it.id == gasto?.cartaoId }

    var nome by remember { mutableStateOf(gasto?.nome ?: "") }
    var valor by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(categorias.firstOrNull { it.id == gasto?.categoriaId } ?: categorias.firstOrNull()) }
    var forma by remember { mutableStateOf(formas.firstOrNull { it.id == gasto?.formaPagamentoId } ?: formas.firstOrNull()) }
    var cartao by remember { mutableStateOf(cartoes.firstOrNull { it.id == gasto?.cartaoId } ?: cartoes.firstOrNull()) }
    var dia by remember { mutableStateOf(gasto?.diaVencimento?.toString() ?: "10") }
    val intervaloInicial = gasto?.intervaloMeses ?: 1
    var periodicidade by remember {
        mutableStateOf(Periodicidades.firstOrNull { it.first == intervaloInicial } ?: Periodicidades.last())
    }
    var intervaloLivre by remember { mutableStateOf(if (Periodicidades.any { it.first == intervaloInicial }) "" else intervaloInicial.toString()) }
    var inicio by remember { mutableStateOf(gasto?.inicio ?: estado.competenciaAtual) }
    var debito by remember { mutableStateOf(gasto?.debitoAutomatico ?: false) }
    var ativo by remember { mutableStateOf(gasto?.ativo ?: true) }

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

    val dinheiro = Dinheiro.interpretar(valor)
    val diaOk = dia.trim().toIntOrNull()?.takeIf { it in 1..31 }
    val intervalo = if (periodicidade.first == 0) intervaloLivre.trim().toIntOrNull()?.takeIf { it in 1..60 } else periodicidade.first
    val usaCartao = forma?.tipo?.viaFatura == true
    val valido = nome.isNotBlank() && diaOk != null && intervalo != null && categoria != null && forma != null &&
        (!usaCartao || cartao != null) && (gasto != null || (dinheiro != null && dinheiro > Dinheiro.ZERO))

    DialogoFormulario(
        titulo = if (gasto == null) "Novo gasto fixo" else "Editar gasto fixo",
        onFechar = onFechar,
        podeConfirmar = valido,
        onConfirmar = {
            val cat = categoria
            val fp = forma
            if (diaOk != null && intervalo != null && cat != null && fp != null) {
                val base = gasto ?: GastoFixo(nome = "", categoriaId = cat.id, formaPagamentoId = fp.id, diaVencimento = diaOk, inicio = inicio)
                val novo = base.copy(
                    nome = nome.trim(),
                    categoriaId = cat.id,
                    formaPagamentoId = fp.id,
                    cartaoId = if (fp.tipo.viaFatura) cartao?.id else null,
                    diaVencimento = diaOk,
                    intervaloMeses = intervalo,
                    inicio = inicio,
                    debitoAutomatico = debito && !fp.tipo.viaFatura,
                    ativo = ativo,
                )
                if (gasto == null) {
                    if (dinheiro != null) vm.executar("Gasto fixo cadastrado") { gastosFixos.criar(novo, dinheiro) }
                } else {
                    vm.executar("Gasto fixo atualizado") { gastosFixos.atualizar(novo) }
                }
            }
            onFechar()
        },
    ) {
        CampoTexto("Nome", nome, { nome = it })
        if (gasto == null) CampoValor("Valor", valor, { valor = it })
        Seletor("Categoria", categorias, categoria, { it.nome }, { categoria = it })
        SeletorPagamento(formas, cartoes, forma, cartao, { forma = it }, { cartao = it })
        CampoTexto(if (usaCartao) "Dia da cobrança no cartão" else "Dia do vencimento", dia, { dia = it }, numerico = true)
        Seletor("Periodicidade", Periodicidades, periodicidade, { it.second }, { periodicidade = it })
        if (periodicidade.first == 0) CampoTexto("A cada quantos meses", intervaloLivre, { intervaloLivre = it }, numerico = true)
        Seletor("Começa em", mesesParaEscolha(estado.competenciaAtual), inicio, { it.nome() }, { inicio = it })
        if (!usaCartao) {
            LinhaInterruptor("Débito automático", debito) { debito = it }
            if (debito) TextoSecundario("No vencimento ele é considerado pago sozinho; se não for debitado, marque \"Não debitado\".")
        }
        if (gasto != null) {
            LinhaInterruptor("Ativo", ativo) { ativo = it }
            TextoSecundario("Desativado, para de gerar previsões a partir deste mês. Para mudar o valor, use \"Novo valor\".")
        }
    }
}

@Composable
fun DialogoNovoValorFixo(vm: FinanceiroViewModel, estado: EstadoApp, gasto: GastoFixo, onFechar: () -> Unit) {
    var valor by remember { mutableStateOf("") }
    var desde by remember { mutableStateOf(estado.competenciaAtual) }
    val dinheiro = Dinheiro.interpretar(valor)
    DialogoFormulario(
        titulo = "Novo valor: ${gasto.nome}",
        onFechar = onFechar,
        podeConfirmar = dinheiro != null && dinheiro > Dinheiro.ZERO && desde >= gasto.inicio,
        onConfirmar = {
            if (dinheiro != null) vm.executar("Novo valor salvo") { gastosFixos.novoValor(gasto.id, dinheiro, desde) }
            onFechar()
        },
    ) {
        CampoValor("Novo valor", valor, { valor = it })
        Seletor("Vale a partir de", mesesParaEscolha(estado.competenciaAtual).filter { it >= gasto.inicio }, desde, { it.nome() }, { desde = it })
        TextoSecundario("Meses anteriores e pagamentos já feitos mantêm o valor antigo.")
    }
}

/** Encerrar (ou reabrir) um gasto fixo. Ocorrências pagas depois do fim ficam no histórico. */
@Composable
fun DialogoEncerrarFixo(vm: FinanceiroViewModel, estado: EstadoApp, gasto: GastoFixo, onFechar: () -> Unit) {
    var fim by remember { mutableStateOf(gasto.fim ?: estado.competenciaAtual) }
    DialogoFormulario(
        titulo = "Encerrar ${gasto.nome}",
        onFechar = onFechar,
        podeConfirmar = fim >= gasto.inicio,
        textoConfirmar = "Encerrar",
        onConfirmar = {
            vm.executar("Gasto fixo encerrado") { gastosFixos.encerrar(gasto, fim) }
            onFechar()
        },
        acaoExtra = {
            if (gasto.fim != null) {
                TextButton(onClick = {
                    vm.executar("Gasto fixo reaberto") { gastosFixos.atualizar(gasto.copy(fim = null)) }
                    onFechar()
                }) { Text("Reabrir") }
            }
        },
    ) {
        Seletor("Último mês", mesesParaEscolha(estado.competenciaAtual).filter { it >= gasto.inicio }, fim, { it.nome() }, { fim = it })
        TextoSecundario("Depois desse mês o gasto deixa de aparecer. Pagamentos já registrados continuam no histórico.")
    }
}

/**
 * Ações sobre a ocorrência de um mês: pagar, pular, valor só neste mês, não debitado, desfazer.
 * Só grava uma linha quando há um fato do usuário; "desfazer" volta a ocorrência a ser virtual.
 */
@Composable
fun DialogoOcorrencia(vm: FinanceiroViewModel, estado: EstadoApp, gasto: GastoFixo, competencia: Competencia, onFechar: () -> Unit) {
    val dados = estado.retrato.dados
    val gravada = dados.ocorrencias.firstOrNull { it.gastoFixoId == gasto.id && it.competencia == competencia }
    val item = estado.motor.itensDoMes(competencia).firstOrNull { it.referenciaId == gasto.id && it.origem == OrigemDespesa.FIXO }
    val vigente = MotorFinanceiro.valorVigente(dados.versoesFixo[gasto.id], competencia) ?: Dinheiro.ZERO
    val noCartao = gasto.cartaoId != null
    var valorDoMes by remember { mutableStateOf((gravada?.valor ?: vigente).paraCampo()) }
    val dinheiro = Dinheiro.interpretar(valorDoMes)

    fun salvar(status: StatusOcorrencia, mensagem: String) {
        val base = gravada ?: OcorrenciaFixo(gastoFixoId = gasto.id, competencia = competencia, status = status)
        val valorProprio = dinheiro?.takeIf { it != vigente }
        val nova = base.copy(
            id = 0,
            status = status,
            valor = valorProprio,
            dataPagamento = if (status == StatusOcorrencia.PAGA) estado.hoje else null,
        )
        vm.executar(mensagem) { gastosFixos.salvarOcorrencia(nova) }
        onFechar()
    }

    DialogoFormulario(
        titulo = "${gasto.nome} · ${competencia.curta()}",
        onFechar = onFechar,
        podeConfirmar = dinheiro != null && dinheiro > Dinheiro.ZERO,
        textoConfirmar = if (noCartao) "Salvar valor" else "Marcar como pago",
        onConfirmar = {
            if (noCartao) salvar(gravada?.status ?: StatusOcorrencia.REGISTRADA, "Valor do mês salvo")
            else salvar(StatusOcorrencia.PAGA, "Marcado como pago")
        },
        acaoExtra = {
            if (gravada != null) {
                TextButton(onClick = {
                    vm.executar("Ocorrência desfeita") { gastosFixos.descartarOcorrencia(gasto.id, competencia) }
                    onFechar()
                }) { Text("Desfazer") }
            }
        },
    ) {
        val situacao = when {
            gravada?.status == StatusOcorrencia.PULADA -> "Pulado neste mês"
            gravada?.status == StatusOcorrencia.NAO_DEBITADA -> "Marcado como não debitado"
            item?.estado == EstadoDespesa.PAGA && gravada == null && gasto.debitoAutomatico -> "Pago por débito automático (não confirmado)"
            item?.estado == EstadoDespesa.PAGA -> "Pago"
            item?.estado == EstadoDespesa.REGISTRADA -> "Na fatura"
            else -> "Previsto"
        }
        Text("Situação: $situacao")
        CampoValor("Valor deste mês", valorDoMes, { valorDoMes = it })
        TextoSecundario("Um valor diferente vale só para ${competencia.nome()}; os outros meses seguem ${vigente.formatar()}.")
        if (noCartao) {
            TextoSecundario("No cartão, o gasto fica pago quando a fatura é quitada.")
        } else if (gasto.debitoAutomatico) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { salvar(StatusOcorrencia.NAO_DEBITADA, "Marcado como não debitado") }) {
                    Text("Não debitado", color = Cores.Atencao)
                }
            }
        }
        if (gravada?.status != StatusOcorrencia.PULADA) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { salvar(StatusOcorrencia.PULADA, "Mês pulado") }) { Text("Pular este mês") }
            }
        }
    }
}
