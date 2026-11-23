package com.jhowww.financeiro.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.Categoria
import com.jhowww.financeiro.domain.modelo.Configuracao
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.modelo.RendaPrincipal
import com.jhowww.financeiro.domain.modelo.TipoPagamento
import com.jhowww.financeiro.domain.modelo.TipoReserva
import com.jhowww.financeiro.domain.motor.MotorFinanceiro
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.Bloco
import com.jhowww.financeiro.ui.comum.CampoTexto
import com.jhowww.financeiro.ui.comum.CampoValor
import com.jhowww.financeiro.ui.comum.DialogoConfirmacao
import com.jhowww.financeiro.ui.comum.DialogoFormulario
import com.jhowww.financeiro.ui.comum.PontoCor
import com.jhowww.financeiro.ui.comum.Seletor
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.bpParaCampo
import com.jhowww.financeiro.ui.comum.comoPercentual
import com.jhowww.financeiro.ui.comum.interpretarPercentual
import com.jhowww.financeiro.ui.comum.nome
import com.jhowww.financeiro.ui.comum.paraCampo
import com.jhowww.financeiro.ui.theme.Cores

/** Cores disponíveis para categorias, no tom da paleta do app. */
val PaletaCategorias: List<Long> = listOf(
    0xFF8B6CEF, 0xFF44CF6E, 0xFFE9973F, 0xFFFB464C, 0xFF53DFDD,
    0xFFE0DE71, 0xFFFA99CD, 0xFF027AFF, 0xFFA882FF, 0xFF9A9A9A,
)

fun nomeTipo(tipo: TipoPagamento): String = when (tipo) {
    TipoPagamento.DINHEIRO -> "Dinheiro"
    TipoPagamento.PIX -> "Pix"
    TipoPagamento.DEBITO -> "Débito"
    TipoPagamento.CREDITO -> "Crédito (cartão)"
}

private sealed interface Dialogo {
    data class Renda(val renda: RendaPrincipal?) : Dialogo
    data class NovoValorRenda(val renda: RendaPrincipal) : Dialogo
    data object Reserva : Dialogo
    data class EditarCategoria(val categoria: Categoria?) : Dialogo
    data class ExcluirCategoria(val categoria: Categoria, val usos: Int) : Dialogo
    data class Forma(val forma: FormaPagamento?) : Dialogo
    data class EditarCartao(val cartao: Cartao?) : Dialogo
}

@Composable
fun TelaAjustes(vm: FinanceiroViewModel, estado: EstadoApp) {
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    val dados = estado.retrato.dados
    val atual = estado.competenciaAtual

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Bloco(titulo = "Renda principal", acao = { BotaoAdicionar { dialogo = Dialogo.Renda(null) } }) {
            if (dados.rendas.isEmpty()) TextoSecundario("Nenhuma renda cadastrada. O salário entra como previsto até você confirmar o recebimento.")
            dados.rendas.forEach { renda ->
                val valor = MotorFinanceiro.valorVigente(dados.versoesRenda[renda.id], atual)
                ItemLista(
                    titulo = renda.descricao + if (renda.ativa) "" else " (desativada)",
                    detalhe = "${valor?.formatar() ?: "sem valor"} · recebe no dia ${renda.diaRecebimento}",
                    onClick = { dialogo = Dialogo.Renda(renda) },
                ) {
                    TextButton(onClick = { dialogo = Dialogo.NovoValorRenda(renda) }) { Text("Novo valor") }
                }
            }
        }

        val config = dados.configuracao
        Bloco(titulo = "Reserva e limites", acao = { TextButton(onClick = { dialogo = Dialogo.Reserva }) { Text("Editar") } }) {
            Text(
                when (config.tipoReserva) {
                    TipoReserva.FIXO -> "Meta da reserva: ${Dinheiro(config.valorReserva).formatar()} por mês"
                    TipoReserva.PERCENTUAL -> "Meta da reserva: ${config.valorReserva.toInt().comoPercentual()} da renda principal"
                },
            )
            TextoSecundario("Alerta de comprometimento acima de ${config.limiteComprometimentoBp.comoPercentual()} · alertas ${if (config.alertasAtivos) "ligados" else "desligados"}")
        }

        Bloco(titulo = "Categorias", acao = { BotaoAdicionar { dialogo = Dialogo.EditarCategoria(null) } }) {
            if (estado.retrato.categorias.isEmpty()) TextoSecundario("Nenhuma categoria. Crie as suas, como Mercado, Moradia e Transporte.")
            estado.retrato.categorias.forEach { cat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PontoCor(Color(cat.cor))
                    ItemLista(
                        titulo = cat.nome,
                        detalhe = null,
                        onClick = { dialogo = Dialogo.EditarCategoria(cat) },
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                    ) {
                        IconButton(onClick = { dialogo = Dialogo.ExcluirCategoria(cat, usosDaCategoria(estado, cat.id)) }) {
                            Icon(Icons.Outlined.DeleteOutline, "Excluir")
                        }
                    }
                }
            }
        }

        Bloco(titulo = "Formas de pagamento", acao = { BotaoAdicionar { dialogo = Dialogo.Forma(null) } }) {
            if (estado.retrato.formas.isEmpty()) TextoSecundario("Nenhuma forma cadastrada, como Pix, Débito ou Crédito.")
            estado.retrato.formas.forEach { forma ->
                ItemLista(
                    titulo = forma.nome + if (forma.ativa) "" else " (desativada)",
                    detalhe = nomeTipo(forma.tipo),
                    onClick = { dialogo = Dialogo.Forma(forma) },
                )
            }
        }

        Bloco(titulo = "Cartões de crédito", acao = { BotaoAdicionar { dialogo = Dialogo.EditarCartao(null) } }) {
            if (dados.cartoes.isEmpty()) TextoSecundario("Nenhum cartão. Cadastre para controlar as faturas.")
            dados.cartoes.forEach { cartao ->
                ItemLista(
                    titulo = cartao.nome + if (cartao.ativo) "" else " (desativado)",
                    detalhe = "Fecha dia ${cartao.diaFechamento} · vence dia ${cartao.diaVencimento}" +
                        (cartao.limite?.let { " · limite ${it.formatar()}" } ?: ""),
                    onClick = { dialogo = Dialogo.EditarCartao(cartao) },
                )
            }
        }

        Bloco(titulo = "Backup e segurança") {
            TextoSecundario("Exportar e restaurar backup, senha do backup e bloqueio por biometria chegam na Etapa 5.")
        }
    }

    when (val d = dialogo) {
        null -> Unit
        is Dialogo.Renda -> DialogoRenda(vm, d.renda, atual) { dialogo = null }
        is Dialogo.NovoValorRenda -> DialogoNovoValor(vm, d.renda, atual) { dialogo = null }
        Dialogo.Reserva -> DialogoReserva(vm, dados.configuracao) { dialogo = null }
        is Dialogo.EditarCategoria -> DialogoCategoria(vm, d.categoria) { dialogo = null }
        is Dialogo.ExcluirCategoria -> DialogoExcluirCategoria(vm, estado, d.categoria, d.usos) { dialogo = null }
        is Dialogo.Forma -> DialogoForma(vm, d.forma) { dialogo = null }
        is Dialogo.EditarCartao -> DialogoCartao(vm, d.cartao) { dialogo = null }
    }
}

private fun usosDaCategoria(estado: EstadoApp, id: Long): Int {
    val d = estado.retrato.dados
    return d.despesas.count { it.categoriaId == id } + d.gastosFixos.count { it.categoriaId == id } +
        d.compras.count { it.categoriaId == id } + d.receitas.count { it.categoriaId == id }
}

@Composable
private fun BotaoAdicionar(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Outlined.Add, "Adicionar", tint = Cores.Destaque) }
}

@Composable
private fun ItemLista(
    titulo: String,
    detalhe: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    fim: (@Composable () -> Unit)? = null,
) {
    Row(modifier.clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (detalhe != null) TextoSecundario(detalhe)
        }
        fim?.invoke()
    }
}

private fun diaValido(texto: String): Int? = texto.trim().toIntOrNull()?.takeIf { it in 1..31 }

/** Meses oferecidos para "vale a partir de": do ano anterior ao próximo. */
private fun mesesProximos(atual: Competencia): List<Competencia> = (-12L..12L).map { atual.mais(it) }

@Composable
private fun DialogoRenda(vm: FinanceiroViewModel, renda: RendaPrincipal?, atual: Competencia, onFechar: () -> Unit) {
    var descricao by remember { mutableStateOf(renda?.descricao ?: "Salário") }
    var valor by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf(renda?.diaRecebimento?.toString() ?: "5") }
    var desde by remember { mutableStateOf(atual) }
    var ativa by remember { mutableStateOf(renda?.ativa ?: true) }
    val dinheiro = Dinheiro.interpretar(valor)
    val diaOk = diaValido(dia)
    val valido = descricao.isNotBlank() && diaOk != null && (renda != null || (dinheiro != null && dinheiro > Dinheiro.ZERO))
    DialogoFormulario(
        titulo = if (renda == null) "Nova renda principal" else "Editar renda",
        onFechar = onFechar,
        podeConfirmar = valido,
        onConfirmar = {
            if (diaOk != null) {
                if (renda == null && dinheiro != null) {
                    vm.executar("Renda cadastrada") {
                        rendas.criarRenda(RendaPrincipal(descricao = descricao.trim(), diaRecebimento = diaOk), dinheiro, desde)
                    }
                } else if (renda != null) {
                    vm.executar("Renda atualizada") {
                        rendas.atualizarRenda(renda.copy(descricao = descricao.trim(), diaRecebimento = diaOk, ativa = ativa))
                    }
                }
            }
            onFechar()
        },
    ) {
        CampoTexto("Descrição", descricao, { descricao = it })
        if (renda == null) {
            CampoValor("Valor mensal", valor, { valor = it })
            Seletor("Vale a partir de", mesesProximos(atual), desde, { it.nome() }, { desde = it })
        } else {
            TextoSecundario("Para mudar o valor, use \"Novo valor\": os meses anteriores mantêm o valor antigo.")
        }
        CampoTexto("Dia do recebimento", dia, { dia = it }, numerico = true)
        if (renda != null) LinhaSwitch("Ativa", ativa) { ativa = it }
    }
}

@Composable
private fun DialogoNovoValor(vm: FinanceiroViewModel, renda: RendaPrincipal, atual: Competencia, onFechar: () -> Unit) {
    var valor by remember { mutableStateOf("") }
    var desde by remember { mutableStateOf(atual) }
    val dinheiro = Dinheiro.interpretar(valor)
    DialogoFormulario(
        titulo = "Novo valor: ${renda.descricao}",
        onFechar = onFechar,
        podeConfirmar = dinheiro != null && dinheiro > Dinheiro.ZERO,
        onConfirmar = {
            if (dinheiro != null) vm.executar("Novo valor salvo") { rendas.novoValor(renda.id, dinheiro, desde) }
            onFechar()
        },
    ) {
        CampoValor("Novo valor mensal", valor, { valor = it })
        Seletor("Vale a partir de", mesesProximos(atual), desde, { it.nome() }, { desde = it })
        TextoSecundario("Meses anteriores continuam com o valor antigo.")
    }
}

@Composable
private fun DialogoReserva(vm: FinanceiroViewModel, config: Configuracao, onFechar: () -> Unit) {
    var tipo by remember { mutableStateOf(config.tipoReserva) }
    var valor by remember {
        mutableStateOf(if (config.tipoReserva == TipoReserva.FIXO) Dinheiro(config.valorReserva).paraCampo() else config.valorReserva.bpParaCampo())
    }
    var limite by remember { mutableStateOf(config.limiteComprometimentoBp.toLong().bpParaCampo()) }
    var alertas by remember { mutableStateOf(config.alertasAtivos) }
    val valorReserva: Long? = when (tipo) {
        TipoReserva.FIXO -> Dinheiro.interpretar(valor)?.centavos
        TipoReserva.PERCENTUAL -> interpretarPercentual(valor)
    }
    val limiteBp = interpretarPercentual(limite)
    DialogoFormulario(
        titulo = "Reserva e limites",
        onFechar = onFechar,
        podeConfirmar = valorReserva != null && limiteBp != null && limiteBp > 0,
        onConfirmar = {
            if (valorReserva != null && limiteBp != null) {
                vm.executar("Configuração salva") {
                    configuracao.salvar(
                        config.copy(tipoReserva = tipo, valorReserva = valorReserva, limiteComprometimentoBp = limiteBp.toInt(), alertasAtivos = alertas),
                    )
                }
            }
            onFechar()
        },
    ) {
        Seletor(
            "Tipo de meta",
            TipoReserva.entries.toList(),
            tipo,
            { if (it == TipoReserva.FIXO) "Valor fixo por mês" else "Percentual da renda principal" },
            { tipo = it; valor = "" },
        )
        if (tipo == TipoReserva.FIXO) {
            CampoValor("Valor da meta", valor, { valor = it })
        } else {
            CampoTexto("Percentual (ex.: 10)", valor, { valor = it }, numerico = true)
            TextoSecundario("Calculado só sobre a renda principal; rendas extras não mudam a meta.")
        }
        CampoTexto("Alertar acima de (% do orçamento)", limite, { limite = it }, numerico = true)
        LinhaSwitch("Alertas ligados", alertas) { alertas = it }
    }
}

@Composable
private fun LinhaSwitch(rotulo: String, valor: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onChange)
    }
}

@Composable
private fun DialogoCategoria(vm: FinanceiroViewModel, categoria: Categoria?, onFechar: () -> Unit) {
    var nome by remember { mutableStateOf(categoria?.nome ?: "") }
    var cor by remember { mutableStateOf(categoria?.cor ?: PaletaCategorias.first()) }
    DialogoFormulario(
        titulo = if (categoria == null) "Nova categoria" else "Editar categoria",
        onFechar = onFechar,
        podeConfirmar = nome.isNotBlank(),
        onConfirmar = {
            val nova = (categoria ?: Categoria(nome = "", cor = cor)).copy(nome = nome.trim(), cor = cor)
            vm.executar("Categoria salva") { categorias.salvar(nova) }
            onFechar()
        },
    ) {
        CampoTexto("Nome", nome, { nome = it })
        Text("Cor", style = MaterialTheme.typography.labelLarge)
        PaletaCategorias.chunked(5).forEach { linha ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                linha.forEach { c ->
                    val selecionada = c == cor
                    Box(
                        Modifier.size(32.dp)
                            .background(Color(c), CircleShape)
                            .border(if (selecionada) 3.dp else 0.dp, if (selecionada) Color.White else Color.Transparent, CircleShape)
                            .clickable { cor = c },
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogoExcluirCategoria(vm: FinanceiroViewModel, estado: EstadoApp, categoria: Categoria, usos: Int, onFechar: () -> Unit) {
    if (usos == 0) {
        DialogoConfirmacao(
            titulo = "Excluir ${categoria.nome}?",
            texto = "A categoria não tem registros e será excluída.",
            textoConfirmar = "Excluir",
            onConfirmar = { vm.executar("Categoria excluída") { categorias.excluir(categoria.id) } },
            onFechar = onFechar,
        )
        return
    }
    val outras = estado.retrato.categorias.filter { it.id != categoria.id }
    var destino by remember { mutableStateOf<Categoria?>(null) }
    DialogoFormulario(
        titulo = "Excluir ${categoria.nome}",
        onFechar = onFechar,
        podeConfirmar = destino != null,
        textoConfirmar = "Transferir e excluir",
        onConfirmar = {
            destino?.let { d -> vm.executar("Registros transferidos e categoria excluída") { categorias.transferirEExcluir(categoria.id, d.id) } }
            onFechar()
        },
    ) {
        Text("$usos registro(s) usam esta categoria. Escolha para qual categoria eles vão antes de excluir.")
        if (outras.isEmpty()) TextoSecundario("Crie outra categoria primeiro.")
        Seletor("Transferir para", outras, destino, { it.nome }, { destino = it })
    }
}

@Composable
private fun DialogoForma(vm: FinanceiroViewModel, forma: FormaPagamento?, onFechar: () -> Unit) {
    var nome by remember { mutableStateOf(forma?.nome ?: "") }
    var tipo by remember { mutableStateOf(forma?.tipo ?: TipoPagamento.PIX) }
    var ativa by remember { mutableStateOf(forma?.ativa ?: true) }
    DialogoFormulario(
        titulo = if (forma == null) "Nova forma de pagamento" else "Editar forma de pagamento",
        onFechar = onFechar,
        podeConfirmar = nome.isNotBlank(),
        onConfirmar = {
            val nova = (forma ?: FormaPagamento(nome = "", tipo = tipo)).copy(nome = nome.trim(), tipo = tipo, ativa = ativa)
            vm.executar("Forma de pagamento salva") { pagamentos.salvarForma(nova) }
            onFechar()
        },
    ) {
        CampoTexto("Nome", nome, { nome = it })
        Seletor("Tipo", TipoPagamento.entries.toList(), tipo, ::nomeTipo, { tipo = it })
        TextoSecundario(if (tipo.viaFatura) "Compras nesta forma entram na fatura do cartão." else "Despesas nesta forma saem da conta na hora.")
        if (forma != null) LinhaSwitch("Ativa", ativa) { ativa = it }
    }
}

@Composable
private fun DialogoCartao(vm: FinanceiroViewModel, cartao: Cartao?, onFechar: () -> Unit) {
    var nome by remember { mutableStateOf(cartao?.nome ?: "") }
    var fechamento by remember { mutableStateOf(cartao?.diaFechamento?.toString() ?: "") }
    var vencimento by remember { mutableStateOf(cartao?.diaVencimento?.toString() ?: "") }
    var limite by remember { mutableStateOf(cartao?.limite?.paraCampo() ?: "") }
    var ativo by remember { mutableStateOf(cartao?.ativo ?: true) }
    val fech = diaValido(fechamento)
    val venc = diaValido(vencimento)
    val limiteValor = if (limite.isBlank()) null else Dinheiro.interpretar(limite)
    val limiteOk = limite.isBlank() || limiteValor != null
    DialogoFormulario(
        titulo = if (cartao == null) "Novo cartão" else "Editar cartão",
        onFechar = onFechar,
        podeConfirmar = nome.isNotBlank() && fech != null && venc != null && fech != venc && limiteOk,
        onConfirmar = {
            if (fech != null && venc != null) {
                val novo = (cartao ?: Cartao(nome = "", diaFechamento = fech, diaVencimento = venc))
                    .copy(nome = nome.trim(), diaFechamento = fech, diaVencimento = venc, limite = limiteValor, ativo = ativo)
                vm.executar("Cartão salvo") { pagamentos.salvarCartao(novo) }
            }
            onFechar()
        },
    ) {
        CampoTexto("Nome", nome, { nome = it })
        CampoTexto("Dia do fechamento", fechamento, { fechamento = it }, numerico = true)
        CampoTexto("Dia do vencimento", vencimento, { vencimento = it }, numerico = true)
        CampoValor("Limite (opcional)", limite, { limite = it })
        if (cartao != null) {
            TextoSecundario("Mudar os dias vale para compras novas; faturas e compras já lançadas não mudam.")
            LinhaSwitch("Ativo", ativo) { ativo = it }
        }
    }
}
