package com.jhowww.financeiro.ui.relatorios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.motor.ResumoMes
import com.jhowww.financeiro.ui.EstadoApp
import com.jhowww.financeiro.ui.FinanceiroViewModel
import com.jhowww.financeiro.ui.comum.Bloco
import com.jhowww.financeiro.ui.comum.LinhaValor
import com.jhowww.financeiro.ui.comum.PontoCor
import com.jhowww.financeiro.ui.comum.SeletorMes
import com.jhowww.financeiro.ui.comum.TextoSecundario
import com.jhowww.financeiro.ui.comum.comoPercentual
import com.jhowww.financeiro.ui.comum.curta
import com.jhowww.financeiro.ui.comum.nome
import com.jhowww.financeiro.ui.theme.Cores

private val CorFixos = Color(0xFF8B6CEF)
private val CorParcelas = Color(0xFF53DFDD)
private val CorVariaveis = Color(0xFFE9973F)
private val CorRenda = Color(0xFF44CF6E)
private val CorDespesas = Color(0xFFFB464C)
private val CorPlanejada = Color(0xFF6B6B6B)

private fun Dinheiro.reais(): Double = centavos / 100.0

/** Rótulo do mês no eixo; meses com dados incompletos ganham "*". */
private fun rotulo(r: ResumoMes, atual: Competencia): String =
    r.competencia.curta() + if (r.incompleto && r.competencia < atual) "*" else ""

@Composable
fun TelaRelatorios(vm: FinanceiroViewModel, estado: EstadoApp, mes: Competencia) {
    val motor = estado.motor
    val resumo = motor.resumoMes(mes)
    val ultimos6 = motor.evolucao(mes.mais(-5), mes)
    val ultimos12 = motor.evolucao(mes.mais(-11), mes)
    val atual = estado.competenciaAtual

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SeletorMes(mes, atual, vm::mudarMes, vm::irParaMesAtual)

        ResumoMensal(resumo, mes, atual)

        Bloco(titulo = "Orçamento × despesas") {
            GraficoColunas(
                ultimos6.map { rotulo(it, atual) },
                listOf(
                    Serie("Orçamento", CorRenda, ultimos6.map { it.orcamentoProjetado.reais() }),
                    Serie("Despesas", CorDespesas, ultimos6.map { it.despesas.reais() }),
                ),
            )
            val diferenca = resumo.orcamentoProjetado - resumo.despesas
            TextoSecundario(
                if (diferenca.ehNegativo) "Em ${mes.curta()}, as despesas passaram o orçamento em ${(-diferenca).formatar()}."
                else "Em ${mes.curta()}, sobram ${diferenca.formatar()} do orçamento.",
            )
        }

        DistribuicaoPorCategoria(estado, mes, resumo)

        Bloco(titulo = "Evolução mensal") {
            GraficoLinhas(
                ultimos12.map { rotulo(it, atual) },
                listOf(
                    Serie("Receitas", CorRenda, ultimos12.map { it.rendaProjetada.reais() }),
                    Serie("Despesas", CorDespesas, ultimos12.map { it.despesas.reais() }),
                ),
            )
            TextoSecundario("Receitas = renda principal (prevista ou confirmada) + receitas adicionais recebidas.")
        }

        Comprometimento(resumo, ultimos6, atual)

        Bloco(titulo = "Reserva financeira") {
            GraficoColunas(
                ultimos6.map { rotulo(it, atual) },
                listOf(
                    Serie("Planejada", CorPlanejada, ultimos6.map { it.reservaPlanejada.reais() }),
                    Serie("Realizada", CorFixos, ultimos6.map { it.reservaRealizada.reais() }),
                ),
            )
            LinhaValor("Total acumulado até ${mes.curta()}", motor.reservaAcumulada(mes), destaque = true)
            TextoSecundario("Realizada = aportes − retiradas do mês. A sobra não entra aqui sozinha.")
        }

        Comparacao(resumo, motor.resumoMes(mes.anterior()))

        if (ultimos12.any { it.incompleto && it.competencia < atual }) {
            TextoSecundario("* Mês com dados incompletos: início de uso, sem lançamentos variáveis ou salário não confirmado.")
        }
    }
}

@Composable
private fun ResumoMensal(r: ResumoMes, mes: Competencia, atual: Competencia) {
    Bloco(titulo = "Resumo de ${mes.nome()}") {
        when {
            mes > atual -> TextoSecundario("Mês futuro: só previsões.")
            mes == atual -> TextoSecundario("Mês em andamento.")
            r.incompleto -> TextoSecundario("Mês com dados incompletos.")
        }
        LinhaValor("Renda principal", r.rendaPrincipal)
        LinhaValor("Rendas adicionais", r.receitasRecebidas)
        LinhaValor("Renda total", r.rendaProjetada, destaque = true)
        if (r.salarioAConfirmar) TextoSecundario("Salário ainda não confirmado neste mês.")
        LinhaValor("Reserva planejada", r.reservaPlanejada)
        LinhaValor("Reserva realizada", r.reservaRealizada)
        LinhaValor("Orçamento disponível", r.orcamentoProjetado, destaque = true)
        LinhaValor("Gastos fixos", r.despesasFixas)
        LinhaValor("Parcelamentos", r.despesasParceladas)
        LinhaValor("Gastos variáveis", r.despesasVariaveis)
        LinhaValor("Total de despesas", r.despesas, destaque = true)
        LinhaValor("Saldo restante", r.saldoDisponivel, destaque = true)
    }
}

@Composable
private fun DistribuicaoPorCategoria(estado: EstadoApp, mes: Competencia, r: ResumoMes) {
    val porCategoria = estado.motor.gastosPorCategoria(mes).entries.sortedByDescending { it.value }
    val positivas = porCategoria.filter { it.value > Dinheiro.ZERO }
    val total = positivas.fold(0.0) { acc, e -> acc + e.value.reais() }
    Bloco(titulo = "Distribuição por categoria") {
        if (positivas.isEmpty()) {
            TextoSecundario("Sem despesas em ${mes.curta()}.")
        }
        positivas.forEach { (id, valor) ->
            val categoria = estado.categoriasPorId[id]
            val fracao = if (total > 0) valor.reais() / total else 0.0
            val bp = (fracao * 10_000).toInt()
            LinhaParticipacao(
                nome = categoria?.nome ?: "Sem categoria",
                cor = categoria?.let { Color(it.cor) } ?: Cores.TextoSecundario,
                valor = "${valor.formatar()} · ${bp.comoPercentual()}",
                fracao = fracao,
            )
        }
        porCategoria.filter { it.value.ehNegativo }.forEach { (id, valor) ->
            TextoSecundario("${estado.categoriasPorId[id]?.nome ?: "Sem categoria"}: ${valor.formatar()} (estornos maiores que as compras do mês)")
        }
        if (positivas.isNotEmpty()) TextoSecundario("Total: ${r.despesas.formatar()}")
    }
}

@Composable
private fun Comprometimento(r: ResumoMes, ultimos6: List<ResumoMes>, atual: Competencia) {
    val orcamento = r.orcamentoProjetado.reais()
    Bloco(titulo = "Comprometimento financeiro") {
        if (orcamento <= 0) {
            TextoSecundario("Sem orçamento positivo neste mês.")
        } else {
            BarraProporcional(
                listOf(
                    CorFixos to r.despesasFixas.reais(),
                    CorParcelas to r.despesasParceladas.reais(),
                    CorVariaveis to r.despesasVariaveis.reais(),
                ),
                total = orcamento,
            )
            listOf(
                Triple("Gastos fixos", CorFixos, r.despesasFixas),
                Triple("Parcelamentos", CorParcelas, r.despesasParceladas),
                Triple("Gastos variáveis", CorVariaveis, r.despesasVariaveis),
            ).forEach { (nome, cor, valor) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    PontoCor(cor)
                    Text("  $nome", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    val bp = (valor.reais() / orcamento * 10_000).toInt()
                    Text("${valor.formatar()} · ${bp.comoPercentual()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            r.comprometimentoBp?.let { TextoSecundario("Total comprometido: ${it.comoPercentual()} do orçamento de ${r.orcamentoProjetado.formatar()}.") }
        }
        GraficoColunas(
            ultimos6.map { rotulo(it, atual) },
            listOf(
                Serie("Fixos", CorFixos, ultimos6.map { it.despesasFixas.reais() }),
                Serie("Parcelas", CorParcelas, ultimos6.map { it.despesasParceladas.reais() }),
                Serie("Variáveis", CorVariaveis, ultimos6.map { it.despesasVariaveis.reais() }),
            ),
            empilhado = true,
        )
    }
}

/** Mês escolhido contra o anterior: variação de cada indicador. */
@Composable
private fun Comparacao(r: ResumoMes, anterior: ResumoMes) {
    Bloco(titulo = "Comparação com ${anterior.competencia.curta()}") {
        Row(Modifier.fillMaxWidth()) {
            Text("", modifier = Modifier.weight(1.4f))
            Text(anterior.competencia.curta(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(r.competencia.curta(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text("Variação", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.8f))
        }
        listOf(
            Triple("Renda total", anterior.rendaProjetada, r.rendaProjetada) to true,
            Triple("Despesas", anterior.despesas, r.despesas) to false,
            Triple("Fixos", anterior.despesasFixas, r.despesasFixas) to false,
            Triple("Parcelas", anterior.despesasParceladas, r.despesasParceladas) to false,
            Triple("Variáveis", anterior.despesasVariaveis, r.despesasVariaveis) to false,
            Triple("Reserva", anterior.reservaRealizada, r.reservaRealizada) to true,
            Triple("Saldo", anterior.saldoDisponivel, r.saldoDisponivel) to true,
        ).forEach { (linha, subirEhBom) ->
            val (nome, antes, agora) = linha
            val delta = agora - antes
            val cor = when {
                delta.ehZero -> Cores.TextoSecundario
                (delta > Dinheiro.ZERO) == subirEhBom -> Cores.Positivo
                else -> Cores.Negativo
            }
            val variacao = when {
                delta.ehZero -> "="
                antes.ehZero || antes.ehNegativo -> (if (delta > Dinheiro.ZERO) "+" else "") + delta.formatar()
                else -> {
                    val bp = (delta.reais() / antes.reais() * 10_000).toInt()
                    (if (bp > 0) "+" else "") + bp.comoPercentual()
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(nome, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.4f))
                Text(antes.formatar(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(agora.formatar(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(variacao, style = MaterialTheme.typography.bodySmall, color = cor, modifier = Modifier.weight(0.8f))
            }
        }
        TextoSecundario("Use as setas no topo para escolher outro mês; a comparação é sempre com o mês anterior.")
    }
}
