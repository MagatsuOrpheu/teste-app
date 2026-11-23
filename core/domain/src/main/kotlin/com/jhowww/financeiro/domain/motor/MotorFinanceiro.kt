package com.jhowww.financeiro.domain.motor

import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.GastoFixo
import com.jhowww.financeiro.domain.modelo.OcorrenciaFixo
import com.jhowww.financeiro.domain.modelo.Parcela
import com.jhowww.financeiro.domain.modelo.StatusOcorrencia
import com.jhowww.financeiro.domain.modelo.StatusReceita
import com.jhowww.financeiro.domain.modelo.TipoReserva
import com.jhowww.financeiro.domain.modelo.VersaoValor
import com.jhowww.financeiro.domain.soma
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Motor financeiro: calcula todos os indicadores a partir dos registros, sem gravar nada.
 * Uma instância vale para um retrato dos dados e um dia ([hoje]); quando algo muda,
 * cria-se outra. Guarda resultados intermediários em memória, por isso não deve ser
 * compartilhada entre threads.
 */
class MotorFinanceiro(private val dados: DadosFinanceiros, val hoje: LocalDate) {

    val competenciaAtual: Competencia = Competencia.de(hoje)

    private val cartoes = dados.cartoes.associateBy { it.id }
    private val faturasPorId = dados.faturas.associateBy { it.id }
    private val faturasPorChave = dados.faturas.associateBy { FaturaChave(it.cartaoId, it.competencia) }
    private val comprasPorId = dados.compras.associateBy { it.id }
    private val ocorrenciasPorChave = dados.ocorrencias.associateBy { it.gastoFixoId to it.competencia }
    private val recebimentosPorChave = dados.recebimentos.associateBy { it.rendaId to it.competencia }
    private val metasPorCompetencia = dados.metasReserva.associateBy { it.competencia }
    private val pagamentosPorFatura: Map<FaturaChave, Dinheiro> = dados.pagamentosFatura
        .mapNotNull { p -> chaveDe(p.faturaId)?.let { it to p.valor } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, valores) -> valores.soma() }

    private val cacheFaturas = HashMap<FaturaChave, ResumoFatura>()
    private val cacheItens = HashMap<Competencia, List<ItemDespesa>>()
    private val cacheResumos = HashMap<Competencia, ResumoMes>()

    private fun chaveDe(faturaId: Long?): FaturaChave? =
        faturaId?.let { faturasPorId[it] }?.let { FaturaChave(it.cartaoId, it.competencia) }

    // ============================================================ despesas

    /** Todas as despesas da competência, com o estado final (inclusive o pagamento via fatura). */
    fun itensDoMes(competencia: Competencia): List<ItemDespesa> = cacheItens.getOrPut(competencia) {
        itensBrutos(competencia).map { item -> item.fatura?.let { finalizar(item, fatura(it)) } ?: item }
    }

    /** Itens antes de olhar a situação da fatura. */
    private fun itensBrutos(competencia: Competencia): List<ItemDespesa> =
        itensFixos(competencia) +
            dados.parcelas.filter { it.competencia == competencia }.mapNotNull(::itemParcela) +
            dados.despesas.filter { it.competencia == competencia }.map(::itemPontual)

    /** Um gasto fixo ocorre na competência se ela cai na periodicidade, entre o início e o fim. */
    fun ocorre(gasto: GastoFixo, competencia: Competencia): Boolean {
        if (competencia < gasto.inicio) return false
        gasto.fim?.let { if (competencia > it) return false }
        // Desativado: para de gerar previsões dali em diante; o passado fica como estava.
        if (!gasto.ativo && competencia >= competenciaAtual) return false
        return competencia.mesesDesde(gasto.inicio) % gasto.intervaloMeses == 0L
    }

    /** Ocorrências virtuais, substituídas pela linha gravada quando existe. Mês pulado não conta. */
    private fun itensFixos(competencia: Competencia): List<ItemDespesa> = dados.gastosFixos.mapNotNull { gasto ->
        val gravada = ocorrenciasPorChave[gasto.id to competencia]
        if (gravada == null && !ocorre(gasto, competencia)) null else itemFixo(gasto, competencia, gravada)
    }

    private fun itemFixo(gasto: GastoFixo, competencia: Competencia, gravada: OcorrenciaFixo?): ItemDespesa? {
        if (gravada?.status == StatusOcorrencia.PULADA) return null
        val valor = gravada?.valor ?: valorVigente(dados.versoesFixo[gasto.id], competencia) ?: Dinheiro.ZERO
        val vencimento = competencia.diaEfetivo(gasto.diaVencimento)
        val cartao = gasto.cartaoId?.let { cartoes[it] }
        val fatura = cartao?.let { chaveDe(gravada?.faturaId) ?: CalendarioCartao.faturaDaCompra(it, vencimento) }
        val estado = when {
            gravada?.status == StatusOcorrencia.PAGA -> EstadoDespesa.PAGA
            // No cartão: vira registrada na data de cobrança e fica paga com a fatura.
            cartao != null -> if (hoje >= vencimento) EstadoDespesa.REGISTRADA else EstadoDespesa.PREVISTA
            gravada?.status == StatusOcorrencia.NAO_DEBITADA -> EstadoDespesa.PREVISTA
            gasto.debitoAutomatico && hoje >= vencimento -> EstadoDespesa.PAGA
            else -> EstadoDespesa.PREVISTA
        }
        return ItemDespesa(
            origem = OrigemDespesa.FIXO,
            referenciaId = gasto.id,
            descricao = gasto.nome,
            valor = valor,
            competencia = competencia,
            data = vencimento,
            categoriaId = gasto.categoriaId,
            estado = estado,
            fatura = fatura,
            debitoAutomatico = gasto.debitoAutomatico,
            ocorrenciaGravada = gravada != null,
        )
    }

    private fun itemParcela(parcela: Parcela): ItemDespesa? {
        val compra: CompraParcelada = comprasPorId[parcela.compraId] ?: return null
        val cartao = compra.cartaoId?.let { cartoes[it] }
        val fatura = cartao?.let { c ->
            chaveDe(parcela.faturaId) ?: run {
                // Cada parcela cai uma fatura depois da anterior, acompanhando o deslocamento da competência.
                val primeira = CalendarioCartao.faturaDaCompra(c, compra.dataCompra)
                val meses = parcela.competencia.mesesDesde(Competencia.de(compra.dataCompra))
                primeira.copy(competencia = primeira.competencia.mais(meses))
            }
        }
        val estado = when {
            cartao != null ->
                if (parcela.competencia > competenciaAtual || compra.dataCompra > hoje) EstadoDespesa.PREVISTA
                else EstadoDespesa.REGISTRADA
            parcela.pagaEm != null -> EstadoDespesa.PAGA
            else -> EstadoDespesa.PREVISTA
        }
        return ItemDespesa(
            origem = OrigemDespesa.PARCELA,
            referenciaId = parcela.id,
            descricao = compra.descricao,
            valor = parcela.valor,
            competencia = parcela.competencia,
            data = compra.dataCompra,
            categoriaId = compra.categoriaId,
            estado = estado,
            fatura = fatura,
            compraId = compra.id,
            numeroParcela = parcela.numero,
            totalParcelas = compra.numeroParcelas,
        )
    }

    private fun itemPontual(despesa: DespesaPontual): ItemDespesa {
        val cartao = despesa.cartaoId?.let { cartoes[it] }
        val fatura = cartao?.let { chaveDe(despesa.faturaId) ?: CalendarioCartao.faturaDaCompra(it, despesa.data) }
        val estado = when {
            despesa.data > hoje -> EstadoDespesa.PREVISTA
            cartao != null -> EstadoDespesa.REGISTRADA
            else -> EstadoDespesa.PAGA // dinheiro, Pix e débito: registrada e paga no mesmo ato
        }
        return ItemDespesa(
            origem = OrigemDespesa.PONTUAL,
            referenciaId = despesa.id,
            descricao = despesa.descricao,
            valor = despesa.valor,
            competencia = despesa.competencia,
            data = despesa.data,
            categoriaId = despesa.categoriaId,
            estado = estado,
            fatura = fatura,
            ehEstorno = despesa.ehEstorno,
        )
    }

    /** Item do cartão só fica pago quando a fatura dele é quitada. Previsto nunca aparece como pago. */
    private fun finalizar(item: ItemDespesa, fatura: ResumoFatura): ItemDespesa = when {
        item.estado == EstadoDespesa.PREVISTA -> item
        fatura.quitada -> item.copy(estado = EstadoDespesa.PAGA)
        fatura.pago > Dinheiro.ZERO -> item.copy(faturaPagaParcialmente = true)
        else -> item
    }

    // ============================================================ faturas

    /** A fatura não tem valor próprio: total = itens ligados a ela; restante = total − pagamentos. */
    fun fatura(chave: FaturaChave): ResumoFatura = cacheFaturas[chave] ?: calcularFatura(chave).also { cacheFaturas[chave] = it }

    private fun calcularFatura(chave: FaturaChave): ResumoFatura {
        val gravada = faturasPorChave[chave]
        val cartao = cartoes[chave.cartaoId]
        val fechamento = gravada?.dataFechamento
            ?: CalendarioCartao.dataFechamento(requireNotNull(cartao) { "Cartão ${chave.cartaoId} não encontrado" }, chave.competencia)
        val vencimento = gravada?.dataVencimento ?: CalendarioCartao.dataVencimento(cartao!!, chave.competencia)

        // Gastos fixos que podem cair nesta fatura: meses próximos e os corrigidos à mão para ela.
        val mesesFixos = (0L..3L).map { chave.competencia.mais(-it) }.toSet() +
            dados.ocorrencias.filter { chaveDe(it.faturaId) == chave }.map { it.competencia }
        val brutos = (
            mesesFixos.flatMap(::itensFixos) +
                dados.parcelas.mapNotNull(::itemParcela) +
                dados.despesas.map(::itemPontual)
            ).filter { it.fatura == chave }

        val total = brutos.map { it.valor }.soma()
        val pago = pagamentosPorFatura[chave] ?: Dinheiro.ZERO
        val restante = total - pago
        val status = when {
            restante <= Dinheiro.ZERO && (pago > Dinheiro.ZERO || hoje > fechamento) -> StatusFatura.PAGA
            hoje <= fechamento -> StatusFatura.ABERTA
            hoje > vencimento -> StatusFatura.VENCIDA
            pago > Dinheiro.ZERO -> StatusFatura.PAGA_PARCIALMENTE
            else -> StatusFatura.FECHADA
        }
        val provisoria = ResumoFatura(chave, gravada?.id, fechamento, vencimento, emptyList(), total, pago, status)
        return provisoria.copy(itens = brutos.map { finalizar(it, provisoria) }.sortedBy { it.data })
    }

    /** Faturas de um cartão cuja competência está no intervalo, gravadas ou só calculadas, sem as vazias. */
    fun faturasDoCartao(cartaoId: Long, de: Competencia, ate: Competencia): List<ResumoFatura> =
        generateSequence(de) { it.proxima() }.takeWhile { it <= ate }
            .map { fatura(FaturaChave(cartaoId, it)) }
            .filter { it.itens.isNotEmpty() || it.pago > Dinheiro.ZERO || it.id != null }
            .toList()

    /**
     * Correção manual da fatura de destino: só no mesmo cartão e nunca saindo de uma fatura
     * quitada ou entrando nela (antes, desfaz-se o pagamento).
     */
    fun validarMudancaDeFatura(atual: FaturaChave, destino: FaturaChave): ResultadoValidacao {
        if (atual.cartaoId != destino.cartaoId) return ResultadoValidacao.Recusado("A fatura de destino precisa ser do mesmo cartão")
        if (atual == destino) return ResultadoValidacao.Recusado("O item já está nessa fatura")
        val origem = fatura(atual)
        if (origem.pago > Dinheiro.ZERO && origem.restante <= Dinheiro.ZERO) {
            return ResultadoValidacao.Recusado("A fatura atual já está quitada; desfaça o pagamento antes de mover o item")
        }
        val alvo = fatura(destino)
        if (alvo.pago > Dinheiro.ZERO && alvo.restante <= Dinheiro.ZERO) {
            return ResultadoValidacao.Recusado("A fatura de destino já está quitada; desfaça o pagamento antes de mover o item")
        }
        return ResultadoValidacao.Valido
    }

    // ============================================================ renda e reserva

    private class RendaDoMes(val principal: Dinheiro, val confirmada: Dinheiro, val aConfirmar: Boolean)

    private fun rendaDoMes(competencia: Competencia): RendaDoMes {
        var principal = Dinheiro.ZERO
        var confirmada = Dinheiro.ZERO
        var aConfirmar = false
        for (renda in dados.rendas) {
            val recebimento = recebimentosPorChave[renda.id to competencia]
            if (recebimento != null) {
                // Valor diferente do previsto vale só neste mês; a versão da renda não muda.
                principal += recebimento.valorRecebido
                confirmada += recebimento.valorRecebido
                continue
            }
            if (!renda.ativa && competencia >= competenciaAtual) continue
            val prevista = valorVigente(dados.versoesRenda[renda.id], competencia) ?: continue
            principal += prevista
            aConfirmar = true
        }
        return RendaDoMes(principal, confirmada, aConfirmar)
    }

    /** Meta do mês: ajuste daquele mês, ou o padrão (fixo, ou % só sobre a renda principal). */
    fun reservaPlanejada(competencia: Competencia): Dinheiro =
        reservaPlanejada(competencia, rendaDoMes(competencia).principal)

    private fun reservaPlanejada(competencia: Competencia, rendaPrincipal: Dinheiro): Dinheiro {
        metasPorCompetencia[competencia]?.let { return it.valorPlanejado }
        val config = dados.configuracao
        return when (config.tipoReserva) {
            TipoReserva.FIXO -> Dinheiro(config.valorReserva)
            TipoReserva.PERCENTUAL -> percentual(rendaPrincipal, config.valorReserva)
        }
    }

    /** Aportes menos retiradas até a competência, inclusive. */
    fun reservaAcumulada(ate: Competencia): Dinheiro =
        dados.aportes.filter { Competencia.de(it.data) <= ate }.map { it.valor }.soma() -
            dados.retiradas.filter { Competencia.de(it.data) <= ate }.map { it.valor }.soma()

    // ============================================================ resumo do mês

    fun resumoMes(competencia: Competencia): ResumoMes = cacheResumos.getOrPut(competencia) {
        val itens = itensDoMes(competencia)
        fun soma(origem: OrigemDespesa) = itens.filter { it.origem == origem }.map { it.valor }.soma()
        val fixas = soma(OrigemDespesa.FIXO)
        val parceladas = soma(OrigemDespesa.PARCELA)
        val variaveis = soma(OrigemDespesa.PONTUAL)
        val despesas = fixas + parceladas + variaveis

        val renda = rendaDoMes(competencia)
        val receitasRecebidas = dados.receitas
            .filter { it.status == StatusReceita.RECEBIDA && it.competencia == competencia }.map { it.valor }.soma()
        val receitasAReceber = dados.receitas
            .filter { it.status == StatusReceita.A_RECEBER && it.competencia == competencia }.map { it.valor }.soma()
        val rendaProjetada = renda.principal + receitasRecebidas
        val rendaConfirmada = renda.confirmada + receitasRecebidas

        val reserva = reservaPlanejada(competencia, renda.principal)
        val orcamentoProjetado = rendaProjetada - reserva
        val orcamentoConfirmado = rendaConfirmada - reserva
        val saldoDisponivel = orcamentoProjetado - despesas
        val estimativa = estimativaVariaveis(competencia)

        val aportes = dados.aportes.filter { Competencia.de(it.data) == competencia }.map { it.valor }.soma()
        val retiradas = dados.retiradas.filter { Competencia.de(it.data) == competencia }.map { it.valor }.soma()
        val reservaRealizada = aportes - retiradas

        ResumoMes(
            competencia = competencia,
            rendaPrincipal = renda.principal,
            rendaPrincipalConfirmada = renda.confirmada,
            salarioAConfirmar = renda.aConfirmar,
            receitasRecebidas = receitasRecebidas,
            receitasAReceber = receitasAReceber,
            rendaProjetada = rendaProjetada,
            rendaConfirmada = rendaConfirmada,
            reservaPlanejada = reserva,
            orcamentoProjetado = orcamentoProjetado,
            orcamentoConfirmado = orcamentoConfirmado,
            despesasFixas = fixas,
            despesasParceladas = parceladas,
            despesasVariaveis = variaveis,
            despesas = despesas,
            despesasPagas = itens.filter { it.estado == EstadoDespesa.PAGA }.map { it.valor }.soma(),
            comprometimentoBp = pontosBase(despesas, orcamentoProjetado),
            saldoDisponivel = saldoDisponivel,
            saldoConfirmado = orcamentoConfirmado - despesas,
            estimativaVariaveis = estimativa,
            saldoProjetado = saldoDisponivel - estimativa.valor,
            aportesReserva = aportes,
            retiradasReserva = retiradas,
            reservaRealizada = reservaRealizada,
            sobra = rendaConfirmada - despesas - reservaRealizada,
            incompleto = mesIncompleto(competencia),
        )
    }

    fun evolucao(de: Competencia, ate: Competencia): List<ResumoMes> =
        generateSequence(de) { it.proxima() }.takeWhile { it <= ate }.map(::resumoMes).toList()

    /** Total por categoria no mês (estornos entram negativos). */
    fun gastosPorCategoria(competencia: Competencia): Map<Long, Dinheiro> =
        itensDoMes(competencia).groupBy { it.categoriaId }.mapValues { (_, itens) -> itens.map { it.valor }.soma() }

    // ============================================================ gastos variáveis e histórico

    /**
     * Gasto variável (despesas pontuais) da competência. Com [dia] e [diasDoMes], conta só até a
     * mesma fração do mês: dia 15 de 30 equivale ao dia 15,5 de 31.
     */
    fun gastoVariavel(competencia: Competencia, dia: Int? = null, diasDoMes: Int? = null): Dinheiro =
        dados.despesas.filter { it.competencia == competencia }
            .filter { d ->
                if (dia == null || diasDoMes == null) return@filter true
                val diaNoMes = when {
                    Competencia.de(d.data) < competencia -> 1
                    Competencia.de(d.data) > competencia -> competencia.dias
                    else -> d.data.dayOfMonth
                }
                diaNoMes.toLong() * diasDoMes <= dia.toLong() * competencia.dias
            }
            .map { it.valor }.soma()

    private val primeiroMesDeUso: Competencia? by lazy {
        (
            dados.despesas.map { it.competencia } +
                dados.gastosFixos.map { it.inicio } +
                dados.compras.map { it.primeiraCompetencia } +
                dados.versoesRenda.values.flatten().map { it.vigenteDesde } +
                dados.recebimentos.map { it.competencia } +
                dados.receitas.map { it.competencia } +
                dados.aportes.map { Competencia.de(it.data) } +
                dados.retiradas.map { Competencia.de(it.data) }
            ).minOrNull()
    }

    /**
     * Fora da base dos alertas e marcado nos relatórios: mês atual ou futuro, mês de início de uso
     * (e anteriores), mês sem lançamento variável e mês com salário não confirmado.
     */
    fun mesIncompleto(competencia: Competencia): Boolean {
        if (competencia >= competenciaAtual) return true
        val primeiro = primeiroMesDeUso ?: return true
        if (competencia <= primeiro) return true
        if (dados.despesas.none { it.competencia == competencia }) return true
        return rendaDoMes(competencia).aConfirmar
    }

    /** Até 6 meses completos anteriores ao atual. */
    fun mesesBase(): List<Competencia> =
        (1L..MESES_JANELA).map { competenciaAtual.mais(-it) }.filterNot(::mesIncompleto)

    /** Quanto ainda deve sair em gastos variáveis até o fim do mês. */
    fun estimativaVariaveis(competencia: Competencia): Estimativa {
        if (competencia < competenciaAtual) return Estimativa(Dinheiro.ZERO, QualidadeEstimativa.MES_ENCERRADO)
        val base = mesesBase()
        val temHistorico = base.size >= MINIMO_MESES_BASE
        if (competencia > competenciaAtual) {
            return if (temHistorico) {
                Estimativa(mediana(base.map(::gastoVariavel)).naoNegativo(), QualidadeEstimativa.HISTORICO)
            } else {
                Estimativa(Dinheiro.ZERO, QualidadeEstimativa.SEM_DADOS)
            }
        }
        val dia = hoje.dayOfMonth
        val dias = competencia.dias
        if (temHistorico) {
            val restantes = base.map { (gastoVariavel(it) - gastoVariavel(it, dia, dias)).naoNegativo() }
            return Estimativa(mediana(restantes), QualidadeEstimativa.HISTORICO)
        }
        // Sem histórico: projeta o ritmo do próprio mês para os dias que faltam.
        val ate = gastoVariavel(competencia, dia, dias).naoNegativo()
        val projetado = Dinheiro(Math.multiplyExact(ate.centavos, (dias - dia).toLong()) / dia)
        return Estimativa(projetado, QualidadeEstimativa.RITMO_DO_MES)
    }

    // ============================================================ linha do saldo

    /**
     * Saldo restante dia a dia: começa no orçamento menos fixos e parcelas e desce com cada gasto
     * variável. Depois de hoje, a linha é projetada: lançamentos futuros mais a estimativa,
     * distribuída por igual. O último ponto é o saldo projetado do mês.
     */
    fun serieSaldo(competencia: Competencia): List<PontoSaldo> {
        val resumo = resumoMes(competencia)
        val inicial = resumo.orcamentoProjetado - resumo.despesasFixas - resumo.despesasParceladas
        val dias = competencia.dias
        val ultimoReal = when {
            competencia < competenciaAtual -> dias
            competencia > competenciaAtual -> 0
            else -> hoje.dayOfMonth
        }
        val porDia = LongArray(dias + 1)
        dados.despesas.filter { it.competencia == competencia }.forEach { d ->
            val dia = if (Competencia.de(d.data) == competencia) d.data.dayOfMonth else if (Competencia.de(d.data) < competencia) 1 else dias
            porDia[dia] += d.valor.centavos
        }
        val estimativa = resumo.estimativaVariaveis.valor.centavos
        val pontos = ArrayList<PontoSaldo>(dias + 1)
        var saldo = inicial.centavos
        pontos += PontoSaldo(0, inicial, projetado = ultimoReal == 0)
        for (dia in 1..dias) {
            saldo -= porDia[dia]
            if (dia <= ultimoReal) {
                pontos += PontoSaldo(dia, Dinheiro(saldo), projetado = false)
            } else {
                val estimadoAte = estimativa * (dia - ultimoReal) / (dias - ultimoReal)
                pontos += PontoSaldo(dia, Dinheiro(saldo - estimadoAte), projetado = true)
            }
        }
        return pontos
    }

    // ============================================================ alertas

    /** Alertas do mês atual, cada um com o motivo em números. */
    fun alertas(): List<Alerta> {
        if (!dados.configuracao.alertasAtivos) return emptyList()
        val comp = competenciaAtual
        val resumo = resumoMes(comp)
        val alertas = mutableListOf<Alerta>()

        for (renda in dados.rendas) {
            if (!renda.ativa || recebimentosPorChave.containsKey(renda.id to comp)) continue
            val prevista = valorVigente(dados.versoesRenda[renda.id], comp) ?: continue
            val dia = comp.diaEfetivo(renda.diaRecebimento)
            if (hoje >= dia) {
                alertas += Alerta(
                    TipoAlerta.SALARIO_A_CONFIRMAR, Severidade.ATENCAO,
                    "Confirme o recebimento de ${renda.descricao} (${prevista.formatar()}), previsto para ${dia.format(DIA_MES)}.",
                    renda.id,
                )
            }
        }

        itensDoMes(comp)
            .filter { it.origem == OrigemDespesa.FIXO && it.debitoAutomatico && !it.viaCartao && !it.ocorrenciaGravada }
            .filter { it.estado == EstadoDespesa.PAGA && hoje > it.data }
            .forEach {
                alertas += Alerta(
                    TipoAlerta.DEBITO_A_CONFIRMAR, Severidade.INFO,
                    "Confirme se ${it.descricao} (${it.valor.formatar()}) foi debitado em ${it.data.format(DIA_MES)}.",
                    it.referenciaId,
                )
            }

        for (cartao in dados.cartoes) {
            faturasDoCartao(cartao.id, comp.mais(-6), comp)
                .filter { it.status == StatusFatura.VENCIDA }
                .forEach {
                    alertas += Alerta(
                        TipoAlerta.FATURA_VENCIDA, Severidade.CRITICO,
                        "A fatura ${cartao.nome} de ${it.chave.competencia.mes.format(MES_ANO)} venceu em " +
                            "${it.dataVencimento.format(DIA_MES)} com ${it.restante.formatar()} em aberto.",
                        it.id,
                    )
                }
        }

        if (resumo.saldoProjetado.ehNegativo) {
            alertas += Alerta(
                TipoAlerta.RISCO_DE_ESTOURO, Severidade.CRITICO,
                "Pelo ritmo atual o mês deve fechar em ${resumo.saldoProjetado.formatar()}: saldo disponível de " +
                    "${resumo.saldoDisponivel.formatar()} menos ${resumo.estimativaVariaveis.valor.formatar()} estimados em gastos variáveis.",
            )
        }

        val limite = dados.configuracao.limiteComprometimentoBp
        resumo.comprometimentoBp?.let { bp ->
            if (bp > limite) {
                alertas += Alerta(
                    TipoAlerta.COMPROMETIMENTO_ALTO, Severidade.ATENCAO,
                    "Você já comprometeu ${porcento(bp)} do orçamento (${resumo.despesas.formatar()} de " +
                        "${resumo.orcamentoProjetado.formatar()}); o limite é ${porcento(limite)}.",
                )
            }
        }

        val base = mesesBase()
        if (base.size < MINIMO_MESES_BASE) {
            alertas += Alerta(
                TipoAlerta.DADOS_INSUFICIENTES, Severidade.INFO,
                "Ainda não há histórico para comparar seu ritmo de gastos: são necessários $MINIMO_MESES_BASE meses " +
                    "completos e há ${base.size}.",
            )
        } else {
            val dia = hoje.dayOfMonth
            val atual = gastoVariavel(comp, dia, comp.dias)
            val mediana = mediana(base.map { gastoVariavel(it, dia, comp.dias) })
            if (mediana > Dinheiro.ZERO) {
                val variacao = BigDecimal.valueOf((atual - mediana).centavos * 100)
                    .divide(BigDecimal.valueOf(mediana.centavos), 0, RoundingMode.HALF_UP).toInt()
                val texto = "Até o dia $dia você gastou ${atual.formatar()}, contra mediana de ${mediana.formatar()}"
                if (atual.centavos * 10 > mediana.centavos * 12) {
                    alertas += Alerta(TipoAlerta.RITMO_ACIMA, Severidade.ATENCAO, "$texto (+$variacao%).")
                } else if (atual.centavos * 10 <= mediana.centavos * 9) {
                    alertas += Alerta(TipoAlerta.RITMO_SAUDAVEL, Severidade.INFO, "$texto ($variacao%). Bom ritmo.")
                }
            }
        }
        return alertas
    }

    companion object {
        /** Meses completos exigidos para comparar o ritmo de gastos. */
        const val MINIMO_MESES_BASE = 2
        const val MESES_JANELA = 6L

        private val DIA_MES = DateTimeFormatter.ofPattern("dd/MM")
        private val MES_ANO = DateTimeFormatter.ofPattern("MM/yyyy")

        /** Valor da versão vigente na competência (a mais recente que já começou). */
        fun valorVigente(versoes: List<VersaoValor>?, competencia: Competencia): Dinheiro? =
            versoes?.filter { it.vigenteDesde <= competencia }?.maxByOrNull { it.vigenteDesde }?.valor

        /** [pontosBase] de um valor, arredondado ao centavo mais próximo. */
        fun percentual(valor: Dinheiro, pontosBase: Long): Dinheiro = Dinheiro(
            BigDecimal.valueOf(valor.centavos).multiply(BigDecimal.valueOf(pontosBase))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP).longValueExact(),
        )

        fun pontosBase(parte: Dinheiro, total: Dinheiro): Int? {
            if (total <= Dinheiro.ZERO) return null
            return BigDecimal.valueOf(parte.centavos).multiply(BigDecimal.valueOf(10_000))
                .divide(BigDecimal.valueOf(total.centavos), 0, RoundingMode.HALF_UP).toInt()
        }

        fun mediana(valores: List<Dinheiro>): Dinheiro {
            if (valores.isEmpty()) return Dinheiro.ZERO
            val ordenados = valores.map { it.centavos }.sorted()
            val meio = ordenados.size / 2
            return if (ordenados.size % 2 == 1) {
                Dinheiro(ordenados[meio])
            } else {
                Dinheiro(Math.floorDiv(ordenados[meio - 1] + ordenados[meio], 2L))
            }
        }

        private fun porcento(bp: Int): String {
            val inteiro = bp / 100
            val resto = bp % 100
            return if (resto == 0) "$inteiro%" else "$inteiro,${resto.toString().padStart(2, '0').trimEnd('0')}%"
        }

        private fun Dinheiro.naoNegativo(): Dinheiro = if (ehNegativo) Dinheiro.ZERO else this
    }
}
