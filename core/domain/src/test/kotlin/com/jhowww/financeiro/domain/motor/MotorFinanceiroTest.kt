package com.jhowww.financeiro.domain.motor

import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.AporteReserva
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.Configuracao
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.Fatura
import com.jhowww.financeiro.domain.modelo.GastoFixo
import com.jhowww.financeiro.domain.modelo.OcorrenciaFixo
import com.jhowww.financeiro.domain.modelo.PagamentoFatura
import com.jhowww.financeiro.domain.modelo.Parcela
import com.jhowww.financeiro.domain.modelo.ReceitaAdicional
import com.jhowww.financeiro.domain.modelo.RecebimentoRenda
import com.jhowww.financeiro.domain.modelo.RendaPrincipal
import com.jhowww.financeiro.domain.modelo.ReservaMes
import com.jhowww.financeiro.domain.modelo.RetiradaReserva
import com.jhowww.financeiro.domain.modelo.StatusOcorrencia
import com.jhowww.financeiro.domain.modelo.StatusReceita
import com.jhowww.financeiro.domain.modelo.TipoReserva
import com.jhowww.financeiro.domain.modelo.VersaoValor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Cenários obrigatórios da arquitetura. Os nomes seguem a tabela "Cenários de teste obrigatórios".
 * Valores em reais via [r]; datas em 2026/2027.
 */
class MotorFinanceiroTest {

    private val out = Competencia.de(2026, 10)
    private val nov = Competencia.de(2026, 11)
    private val dez = Competencia.de(2026, 12)
    private val jan = Competencia.de(2027, 1)

    private fun r(reais: Long) = Dinheiro.reais(reais)
    private fun d(ano: Int, mes: Int, dia: Int) = LocalDate.of(ano, mes, dia)

    // ---------------------------------------------------------------- montagem

    private val salario = RendaPrincipal(id = 1, descricao = "Salário", diaRecebimento = 5)
    private fun versoesSalario(vararg v: Pair<Competencia, Long>) =
        mapOf(1L to v.map { VersaoValor(vigenteDesde = it.first, valor = r(it.second)) })

    private val nubank = Cartao(id = 1, nome = "Nubank", diaFechamento = 3, diaVencimento = 10)

    private fun fixo(
        id: Long, valor: Long, dia: Int = 10, inicio: Competencia = Competencia.de(2026, 1),
        intervalo: Int = 1, fim: Competencia? = null, debito: Boolean = false, cartao: Long? = null,
    ) = GastoFixo(
        id = id, nome = "Fixo $id", categoriaId = 1, formaPagamentoId = 1, cartaoId = cartao,
        diaVencimento = dia, intervaloMeses = intervalo, inicio = inicio, fim = fim, debitoAutomatico = debito,
    ) to VersaoValor(vigenteDesde = inicio, valor = r(valor))

    private fun pontual(id: Long, valor: Long, data: LocalDate, cartao: Long? = null, categoria: Long = 2) =
        DespesaPontual(id = id, descricao = "Despesa $id", valor = r(valor), data = data, categoriaId = categoria, formaPagamentoId = 1, cartaoId = cartao)

    private fun compra(id: Long, total: Long, n: Int, data: LocalDate, cartao: Long? = null, primeira: Competencia = Competencia.de(data)): Pair<CompraParcelada, List<Parcela>> {
        val c = CompraParcelada(
            id = id, descricao = "Compra $id", valorTotal = r(total), numeroParcelas = n, dataCompra = data,
            primeiraCompetencia = primeira, categoriaId = 3, formaPagamentoId = 1, cartaoId = cartao,
        )
        val parcelas = Parcelamento.gerar(c.valorTotal, n, primeira).map { it.copy(id = id * 100 + it.numero, compraId = id) }
        return c to parcelas
    }

    private fun base(
        hoje: LocalDate = d(2026, 11, 20),
        configuracao: Configuracao = Configuracao(tipoReserva = TipoReserva.FIXO, valorReserva = r(500).centavos),
        salarioDesde: Long = 3_300,
    ) = DadosFinanceiros(
        configuracao = configuracao,
        rendas = listOf(salario),
        versoesRenda = versoesSalario(Competencia.de(2026, 1) to salarioDesde),
    ) to hoje

    private fun motor(dados: DadosFinanceiros, hoje: LocalDate) = MotorFinanceiro(dados, hoje)

    private fun DadosFinanceiros.comFixos(vararg f: Pair<GastoFixo, VersaoValor>) = copy(
        gastosFixos = gastosFixos + f.map { it.first },
        versoesFixo = versoesFixo + f.associate { it.first.id to listOf(it.second) },
    )

    private fun DadosFinanceiros.comCompras(vararg c: Pair<CompraParcelada, List<Parcela>>) = copy(
        compras = compras + c.map { it.first },
        parcelas = parcelas + c.flatMap { it.second },
    )

    // ---------------------------------------------------------------- orçamento

    @Test
    fun exemploDoEscopo() {
        val (dados, hoje) = base()
        val m = motor(
            dados.comFixos(fixo(1, 800))
                .comCompras(compra(1, 2_400, 6, d(2026, 9, 15)))
                .copy(
                    despesas = listOf(pontual(1, 600, d(2026, 11, 8))),
                    receitas = listOf(ReceitaAdicional(descricao = "Extra", valor = r(200), data = d(2026, 11, 12), status = StatusReceita.RECEBIDA, recebidaEm = d(2026, 11, 12))),
                ),
            hoje,
        )
        val resumo = m.resumoMes(nov)
        assertEquals(r(3_500), resumo.rendaProjetada)
        assertEquals(r(3_000), resumo.orcamentoProjetado)
        assertEquals(r(800), resumo.despesasFixas)
        assertEquals(r(400), resumo.despesasParceladas)
        assertEquals(r(600), resumo.despesasVariaveis)
        assertEquals(r(1_800), resumo.despesas)
        assertEquals(6_000, resumo.comprometimentoBp)
        assertEquals(r(1_200), resumo.saldoDisponivel)
    }

    @Test
    fun rendaAdicionalRecebidaContaSoNoMesDoRecebimento() {
        val (dados, hoje) = base()
        val extra = ReceitaAdicional(descricao = "Freela", valor = r(700), data = d(2026, 10, 25), status = StatusReceita.RECEBIDA, recebidaEm = d(2026, 11, 2))
        val m = motor(dados.copy(receitas = listOf(extra)), hoje)
        assertEquals(Dinheiro.ZERO, m.resumoMes(out).receitasRecebidas)
        assertEquals(r(700), m.resumoMes(nov).receitasRecebidas)
        assertEquals(Dinheiro.ZERO, m.resumoMes(dez).receitasRecebidas)
    }

    @Test
    fun rendaAdicionalAReceberNaoAlteraOrcamento() {
        val (dados, hoje) = base()
        val semExtra = motor(dados, hoje).resumoMes(nov)
        val aReceber = ReceitaAdicional(id = 1, descricao = "Venda", valor = r(400), data = d(2026, 11, 25), status = StatusReceita.A_RECEBER)
        val com = motor(dados.copy(receitas = listOf(aReceber)), hoje).resumoMes(nov)
        assertEquals(semExtra.rendaProjetada, com.rendaProjetada)
        assertEquals(semExtra.orcamentoProjetado, com.orcamentoProjetado)
        assertEquals(semExtra.saldoDisponivel, com.saldoDisponivel)
        assertEquals(r(400), com.receitasAReceber)

        // Recebida em dezembro: entra em dezembro, não em novembro.
        val recebida = aReceber.copy(status = StatusReceita.RECEBIDA, recebidaEm = d(2026, 12, 3))
        val depois = motor(dados.copy(receitas = listOf(recebida)), d(2026, 12, 3))
        assertEquals(Dinheiro.ZERO, depois.resumoMes(nov).receitasRecebidas)
        assertEquals(r(400), depois.resumoMes(dez).receitasRecebidas)
    }

    @Test
    fun aumentoSalarialNaoMudaMesesAnteriores() {
        val (dados, hoje) = base()
        val m = motor(dados.copy(versoesRenda = versoesSalario(Competencia.de(2026, 1) to 3_000, Competencia.de(2026, 6) to 3_500)), hoje)
        assertEquals(r(3_000), m.resumoMes(Competencia.de(2026, 5)).rendaPrincipal)
        assertEquals(r(3_500), m.resumoMes(Competencia.de(2026, 6)).rendaPrincipal)
        assertEquals(r(3_500), m.resumoMes(nov).rendaPrincipal)
    }

    @Test
    fun salarioNaoConfirmadoFicaSoNoProjetado() {
        val (dados, hoje) = base()
        val m = motor(dados, hoje)
        val resumo = m.resumoMes(nov)
        assertTrue(resumo.salarioAConfirmar)
        assertEquals(r(3_300), resumo.rendaProjetada)
        assertEquals(Dinheiro.ZERO, resumo.rendaConfirmada)
        assertEquals(r(2_800), resumo.orcamentoProjetado)
        assertEquals(r(-500), resumo.orcamentoConfirmado)
        val alerta = m.alertas().single { it.tipo == TipoAlerta.SALARIO_A_CONFIRMAR }
        assertTrue(alerta.mensagem.contains("05/11"))

        val confirmado = motor(dados.copy(recebimentos = listOf(RecebimentoRenda(rendaId = 1, competencia = nov, valorRecebido = r(3_300), confirmadoEm = d(2026, 11, 5)))), hoje)
        assertTrue(!confirmado.resumoMes(nov).salarioAConfirmar)
        assertEquals(r(3_300), confirmado.resumoMes(nov).rendaConfirmada)
        assertTrue(confirmado.alertas().none { it.tipo == TipoAlerta.SALARIO_A_CONFIRMAR })
    }

    @Test
    fun salarioConfirmadoComValorDiferenteValeSoNoMes() {
        val (dados, hoje) = base(hoje = d(2026, 12, 1))
        val m = motor(dados.copy(recebimentos = listOf(RecebimentoRenda(rendaId = 1, competencia = nov, valorRecebido = r(3_150), confirmadoEm = d(2026, 11, 5)))), hoje)
        assertEquals(r(3_150), m.resumoMes(nov).rendaPrincipal)
        assertEquals(r(3_150), m.resumoMes(nov).rendaConfirmada)
        assertEquals(r(3_300), m.resumoMes(dez).rendaPrincipal)
    }

    @Test
    fun reservaPercentualIncideSoSobreARendaPrincipal() {
        val config = Configuracao(tipoReserva = TipoReserva.PERCENTUAL, valorReserva = 1_000) // 10%
        val (dados, hoje) = base(configuracao = config)
        val extra = ReceitaAdicional(descricao = "Bônus", valor = r(1_000), data = d(2026, 11, 10), status = StatusReceita.RECEBIDA, recebidaEm = d(2026, 11, 10))
        val ajusteOutubro = ReservaMes(out, r(200))
        val m = motor(dados.copy(receitas = listOf(extra), metasReserva = listOf(ajusteOutubro)), hoje)
        assertEquals(r(330), m.resumoMes(nov).reservaPlanejada)
        assertEquals(r(200), m.resumoMes(out).reservaPlanejada)
        assertEquals(r(330), m.resumoMes(dez).reservaPlanejada)
        assertEquals(r(4_300) - r(330), m.resumoMes(nov).orcamentoProjetado)
    }

    @Test
    fun aporteERetiradaNaoAlteramDespesas() {
        val (dados, hoje) = base()
        val semReserva = motor(dados.comFixos(fixo(1, 800)), hoje).resumoMes(nov)
        val m = motor(
            dados.comFixos(fixo(1, 800)).copy(
                aportes = listOf(AporteReserva(data = d(2026, 11, 6), valor = r(300)), AporteReserva(data = d(2026, 11, 20), valor = r(100))),
                retiradas = listOf(RetiradaReserva(data = d(2026, 11, 15), valor = r(150))),
            ),
            hoje,
        ).resumoMes(nov)
        assertEquals(r(400), m.aportesReserva)
        assertEquals(r(150), m.retiradasReserva)
        assertEquals(r(250), m.reservaRealizada)
        assertEquals(semReserva.despesas, m.despesas)
        assertEquals(semReserva.saldoDisponivel, m.saldoDisponivel)
    }

    @Test
    fun sobraDoMesEhSeparadaDaReserva() {
        val (dados, hoje) = base()
        val m = motor(
            dados.comFixos(fixo(1, 1_000)).copy(
                recebimentos = listOf(RecebimentoRenda(rendaId = 1, competencia = nov, valorRecebido = r(3_300), confirmadoEm = d(2026, 11, 5))),
                aportes = listOf(AporteReserva(data = d(2026, 11, 6), valor = r(500))),
                retiradas = listOf(RetiradaReserva(data = d(2026, 11, 9), valor = r(100))),
            ),
            hoje,
        )
        val resumo = m.resumoMes(nov)
        assertEquals(r(3_300) - r(1_000) - r(400), resumo.sobra)
        // A sobra nunca vira reserva: a acumulada é só aportes − retiradas.
        assertEquals(r(400), m.reservaAcumulada(nov))
    }

    // ---------------------------------------------------------------- gastos fixos

    @Test
    fun gastoFixoSemOcorrenciaContaComoPrevisto() {
        val (dados, hoje) = base()
        val item = motor(dados.comFixos(fixo(1, 120, dia = 25)), hoje).itensDoMes(nov).single()
        assertEquals(EstadoDespesa.PREVISTA, item.estado)
        assertEquals(r(120), item.valor)
        assertTrue(!item.ocorrenciaGravada)
    }

    @Test
    fun marcarGastoFixoComoPagoNaoDuplica() {
        val (dados, hoje) = base()
        val comFixo = dados.comFixos(fixo(1, 120))
        val antes = motor(comFixo, hoje).resumoMes(nov)
        val pago = OcorrenciaFixo(gastoFixoId = 1, competencia = nov, status = StatusOcorrencia.PAGA, dataPagamento = d(2026, 11, 10))
        val m = motor(comFixo.copy(ocorrencias = listOf(pago)), hoje)
        val itens = m.itensDoMes(nov)
        assertEquals(1, itens.size)
        assertEquals(EstadoDespesa.PAGA, itens.single().estado)
        assertEquals(antes.despesas, m.resumoMes(nov).despesas)
    }

    @Test
    fun novoValorDeGastoFixoValeDaCompetenciaEmDiante() {
        val (dados, hoje) = base()
        val (gasto, primeira) = fixo(1, 100)
        val m = motor(
            dados.copy(
                gastosFixos = listOf(gasto),
                versoesFixo = mapOf(1L to listOf(primeira, VersaoValor(vigenteDesde = dez, valor = r(130)))),
                ocorrencias = listOf(OcorrenciaFixo(gastoFixoId = 1, competencia = out, status = StatusOcorrencia.PAGA)),
            ),
            hoje,
        )
        assertEquals(r(100), m.itensDoMes(out).single().valor)
        assertEquals(r(100), m.itensDoMes(nov).single().valor)
        assertEquals(r(130), m.itensDoMes(dez).single().valor)
    }

    @Test
    fun debitoAutomaticoViraPagoNoVencimentoSemGravar() {
        val (dados, _) = base()
        val comFixo = dados.comFixos(fixo(1, 90, dia = 15, debito = true))
        assertEquals(EstadoDespesa.PREVISTA, motor(comFixo, d(2026, 11, 14)).itensDoMes(nov).single().estado)
        val noDia = motor(comFixo, d(2026, 11, 15)).itensDoMes(nov).single()
        assertEquals(EstadoDespesa.PAGA, noDia.estado)
        assertTrue(!noDia.ocorrenciaGravada)

        // No dia seguinte, o app pede confirmação.
        assertTrue(motor(comFixo, d(2026, 11, 16)).alertas().any { it.tipo == TipoAlerta.DEBITO_A_CONFIRMAR })

        val naoDebitado = OcorrenciaFixo(gastoFixoId = 1, competencia = nov, status = StatusOcorrencia.NAO_DEBITADA)
        val m = motor(comFixo.copy(ocorrencias = listOf(naoDebitado)), d(2026, 11, 16))
        assertEquals(EstadoDespesa.PREVISTA, m.itensDoMes(nov).single().estado)
        assertTrue(m.alertas().none { it.tipo == TipoAlerta.DEBITO_A_CONFIRMAR })
    }

    @Test
    fun encerrarGastoFixoMantemHistoricoPago() {
        val (dados, hoje) = base()
        val (gasto, versao) = fixo(1, 200, fim = nov)
        val pagoDepois = OcorrenciaFixo(gastoFixoId = 1, competencia = jan, status = StatusOcorrencia.PAGA)
        val m = motor(dados.copy(gastosFixos = listOf(gasto), versoesFixo = mapOf(1L to listOf(versao)), ocorrencias = listOf(pagoDepois)), hoje)
        assertEquals(1, m.itensDoMes(nov).size)
        assertTrue(m.itensDoMes(dez).isEmpty())
        assertEquals(EstadoDespesa.PAGA, m.itensDoMes(jan).single().estado)
    }

    @Test
    fun pularMesTiraODaqueleMes() {
        val (dados, hoje) = base()
        val pulado = OcorrenciaFixo(gastoFixoId = 1, competencia = nov, status = StatusOcorrencia.PULADA)
        val m = motor(dados.comFixos(fixo(1, 50)).copy(ocorrencias = listOf(pulado)), hoje)
        assertTrue(m.itensDoMes(nov).isEmpty())
        assertEquals(1, m.itensDoMes(dez).size)
    }

    @Test
    fun gastoFixoAnualSoNoMesCerto() {
        val (dados, hoje) = base()
        val m = motor(dados.comFixos(fixo(1, 1_200, inicio = Competencia.de(2026, 3), intervalo = 12)), hoje)
        val meses = (0L..24L).map { Competencia.de(2026, 1).mais(it) }.filter { m.itensDoMes(it).isNotEmpty() }
        assertEquals(listOf(Competencia.de(2026, 3), Competencia.de(2027, 3)), meses)
    }

    @Test
    fun gastoFixoTrimestral() {
        val (dados, hoje) = base()
        val m = motor(dados.comFixos(fixo(1, 300, inicio = out, intervalo = 3)), hoje)
        assertEquals(1, m.itensDoMes(out).size)
        assertTrue(m.itensDoMes(nov).isEmpty())
        assertEquals(1, m.itensDoMes(Competencia.de(2027, 1)).size)
    }

    @Test
    fun vencimentoNoDia31CaiNoUltimoDiaDoMes() {
        val (dados, hoje) = base()
        val m = motor(dados.comFixos(fixo(1, 100, dia = 31)), hoje)
        assertEquals(d(2027, 2, 28), m.itensDoMes(Competencia.de(2027, 2)).single().data)
        assertEquals(d(2028, 2, 29), m.itensDoMes(Competencia.de(2028, 2)).single().data)
        assertEquals(d(2026, 11, 30), m.itensDoMes(nov).single().data)
        assertEquals(d(2026, 12, 31), m.itensDoMes(dez).single().data)
    }

    // ---------------------------------------------------------------- parcelamentos

    @Test
    fun parcelada1200Em6() {
        val (dados, hoje) = base()
        val (c, parcelas) = compra(1, 1_200, 6, d(2026, 11, 10))
        assertEquals(List(6) { r(200) }, parcelas.map { it.valor })
        assertEquals(nov, parcelas.first().competencia)
        val m = motor(dados.comCompras(c to parcelas), hoje)
        val meses = (0L..7L).map { nov.mais(it) }.filter { m.itensDoMes(it).isNotEmpty() }
        assertEquals((0L..5L).map { nov.mais(it) }, meses)
        assertEquals("1/6", m.itensDoMes(nov).single().let { "${it.numeroParcela}/${it.totalParcelas}" })
    }

    @Test
    fun primeiraParcelaAjustadaDeslocaTodas() {
        val parcelas = Parcelamento.gerar(r(600), 3, nov)
        val deslocadas = Parcelamento.deslocar(parcelas, dez)
        assertEquals(listOf(dez, jan, Competencia.de(2027, 2)), deslocadas.map { it.competencia })
        assertEquals(parcelas.map { it.valor }, deslocadas.map { it.valor })
    }

    @Test
    fun parceladaComRestoVaiParaAUltima() {
        val parcelas = Parcelamento.gerar(r(100), 3, nov)
        assertEquals(listOf(Dinheiro(3_333), Dinheiro(3_333), Dinheiro(3_334)), parcelas.map { it.valor })
    }

    @Test
    fun parceladaComJurosGuardaValorDaParcela() {
        val parcelas = Parcelamento.gerar(r(1_000), 4, nov, valorParcela = r(270))
        assertEquals(List(4) { r(270) }, parcelas.map { it.valor })
    }

    @Test(expected = IllegalArgumentException::class)
    fun parcelamentoSemParcelasEhRecusado() {
        Parcelamento.gerar(r(100), 0, nov)
    }

    @Test
    fun parcelaForaDoCartaoSoFicaPagaQuandoMarcada() {
        val (dados, hoje) = base()
        val (c, parcelas) = compra(1, 300, 3, d(2026, 11, 2))
        val marcadas = parcelas.map { if (it.numero == 1) it.copy(pagaEm = d(2026, 11, 10)) else it }
        val m = motor(dados.comCompras(c to marcadas), hoje)
        assertEquals(EstadoDespesa.PAGA, m.itensDoMes(nov).single().estado)
        assertEquals(EstadoDespesa.PREVISTA, m.itensDoMes(dez).single().estado)
    }

    // ---------------------------------------------------------------- cartão e faturas

    @Test
    fun compraAntesEDepoisDoFechamento() {
        assertEquals(FaturaChave(1, nov), CalendarioCartao.faturaDaCompra(nubank, d(2026, 11, 3)))
        assertEquals(FaturaChave(1, dez), CalendarioCartao.faturaDaCompra(nubank, d(2026, 11, 4)))
        // Fechamento depois do vencimento no calendário: vence no mês seguinte ao fechamento.
        val inter = Cartao(id = 2, nome = "Inter", diaFechamento = 25, diaVencimento = 5)
        assertEquals(FaturaChave(2, dez), CalendarioCartao.faturaDaCompra(inter, d(2026, 11, 20)))
        assertEquals(FaturaChave(2, jan), CalendarioCartao.faturaDaCompra(inter, d(2026, 11, 26)))

        val (dados, hoje) = base()
        val m = motor(dados.copy(cartoes = listOf(nubank), despesas = listOf(pontual(1, 50, d(2026, 11, 3), 1), pontual(2, 70, d(2026, 11, 4), 1))), hoje)
        // A competência continua sendo o mês da compra.
        assertEquals(r(120), m.resumoMes(nov).despesasVariaveis)
        assertEquals(r(50), m.fatura(FaturaChave(1, nov)).total)
        assertEquals(r(70), m.fatura(FaturaChave(1, dez)).total)
    }

    @Test
    fun correcaoManualDeFatura() {
        val (dados, hoje) = base()
        val faturaJan = Fatura(id = 9, cartaoId = 1, competencia = jan, dataFechamento = d(2027, 1, 3), dataVencimento = d(2027, 1, 10))
        val compraNov = pontual(1, 80, d(2026, 11, 10), 1)
        val antes = motor(dados.copy(cartoes = listOf(nubank), despesas = listOf(compraNov)), hoje)
        assertEquals(r(80), antes.fatura(FaturaChave(1, dez)).total)
        assertEquals(ResultadoValidacao.Valido, antes.validarMudancaDeFatura(FaturaChave(1, dez), FaturaChave(1, jan)))
        assertTrue(antes.validarMudancaDeFatura(FaturaChave(1, dez), FaturaChave(2, jan)) is ResultadoValidacao.Recusado)

        val corrigida = compraNov.copy(faturaId = 9, faturaManual = true)
        val m = motor(dados.copy(cartoes = listOf(nubank), faturas = listOf(faturaJan), despesas = listOf(corrigida)), hoje)
        assertEquals(Dinheiro.ZERO, m.fatura(FaturaChave(1, dez)).total)
        assertEquals(r(80), m.fatura(FaturaChave(1, jan)).total)
        assertEquals(nov, m.itensDoMes(nov).single().competencia)
        assertEquals(antes.resumoMes(nov).despesas, m.resumoMes(nov).despesas)
    }

    @Test
    fun faturaQuitadaRecusaMudanca() {
        val (dados, _) = base()
        val faturaNov = Fatura(id = 5, cartaoId = 1, competencia = nov, dataFechamento = d(2026, 11, 3), dataVencimento = d(2026, 11, 10))
        val m = motor(
            dados.copy(
                cartoes = listOf(nubank), faturas = listOf(faturaNov),
                despesas = listOf(pontual(1, 100, d(2026, 10, 20), 1)),
                pagamentosFatura = listOf(PagamentoFatura(faturaId = 5, data = d(2026, 11, 9), valor = r(100))),
            ),
            d(2026, 11, 20),
        )
        assertTrue(m.fatura(FaturaChave(1, nov)).quitada)
        assertTrue(m.validarMudancaDeFatura(FaturaChave(1, nov), FaturaChave(1, dez)) is ResultadoValidacao.Recusado)
        assertTrue(m.validarMudancaDeFatura(FaturaChave(1, dez), FaturaChave(1, nov)) is ResultadoValidacao.Recusado)
    }

    /** Cartão com compras de 300 + 200 em outubro (depois do fechamento), na fatura que vence em 10/11. */
    private fun cenarioFatura(pagamentos: List<Long>, hoje: LocalDate): MotorFinanceiro {
        val (dados, _) = base(hoje = hoje)
        val faturaNov = Fatura(id = 5, cartaoId = 1, competencia = nov, dataFechamento = d(2026, 11, 3), dataVencimento = d(2026, 11, 10))
        return motor(
            dados.copy(
                cartoes = listOf(nubank), faturas = listOf(faturaNov),
                despesas = listOf(pontual(1, 300, d(2026, 10, 10), 1), pontual(2, 200, d(2026, 10, 20), 1)),
                pagamentosFatura = pagamentos.mapIndexed { i, v -> PagamentoFatura(id = i + 1L, faturaId = 5, data = d(2026, 11, 5 + i), valor = r(v)) },
            ),
            hoje,
        )
    }

    @Test
    fun pagamentoIntegralDaFatura() {
        val semPagar = cenarioFatura(emptyList(), d(2026, 11, 6))
        assertEquals(StatusFatura.FECHADA, semPagar.fatura(FaturaChave(1, nov)).status)
        assertTrue(semPagar.itensDoMes(out).all { it.estado == EstadoDespesa.REGISTRADA })

        val m = cenarioFatura(listOf(500), d(2026, 11, 6))
        val f = m.fatura(FaturaChave(1, nov))
        assertEquals(StatusFatura.PAGA, f.status)
        assertEquals(Dinheiro.ZERO, f.restante)
        assertTrue(m.itensDoMes(out).all { it.estado == EstadoDespesa.PAGA })
        assertEquals(semPagar.resumoMes(out).despesas, m.resumoMes(out).despesas)
    }

    @Test
    fun pagamentoParcialDaFatura() {
        val m = cenarioFatura(listOf(300), d(2026, 11, 6))
        val f = m.fatura(FaturaChave(1, nov))
        assertEquals(r(500), f.total)
        assertEquals(r(300), f.pago)
        assertEquals(r(200), f.restante)
        assertEquals(StatusFatura.PAGA_PARCIALMENTE, f.status)
        assertTrue(m.itensDoMes(out).all { it.estado == EstadoDespesa.REGISTRADA && it.faturaPagaParcialmente })
        assertEquals(r(500), m.resumoMes(out).despesasVariaveis)

        val vencida = cenarioFatura(listOf(300), d(2026, 11, 12))
        assertEquals(StatusFatura.VENCIDA, vencida.fatura(FaturaChave(1, nov)).status)
        assertTrue(vencida.alertas().any { it.tipo == TipoAlerta.FATURA_VENCIDA && it.mensagem.contains("R$ 200,00") })
    }

    @Test
    fun restantePagoDepoisNaFaturaOriginal() {
        val m = cenarioFatura(listOf(300, 200), d(2026, 11, 20))
        assertEquals(StatusFatura.PAGA, m.fatura(FaturaChave(1, nov)).status)
        assertEquals(Dinheiro.ZERO, m.fatura(FaturaChave(1, dez)).total)
        assertTrue(m.alertas().none { it.tipo == TipoAlerta.FATURA_VENCIDA })
    }

    @Test
    fun estornoReduzFaturaEMesDoEstorno() {
        val (dados, _) = base()
        val original = pontual(1, 800, d(2026, 11, 10), 1)
        val estorno = pontual(2, -800, d(2026, 12, 5), 1).copy(estornoDe = 1)
        val m = motor(dados.copy(cartoes = listOf(nubank), despesas = listOf(original, estorno)), d(2026, 12, 6))
        assertEquals(r(800), m.resumoMes(nov).despesasVariaveis)
        assertEquals(r(-800), m.resumoMes(dez).despesasVariaveis)
        assertEquals(r(800), m.fatura(FaturaChave(1, dez)).total)
        assertEquals(r(-800), m.fatura(FaturaChave(1, jan)).total)
        assertTrue(m.itensDoMes(dez).single().ehEstorno)
    }

    @Test
    fun assinaturaFixaNoCartaoContaUmaVez() {
        val (dados, hoje) = base()
        val m = motor(dados.comFixos(fixo(1, 40, dia = 15, cartao = 1)).copy(cartoes = listOf(nubank)), hoje)
        val itens = m.itensDoMes(nov)
        assertEquals(1, itens.size)
        assertEquals(EstadoDespesa.REGISTRADA, itens.single().estado)
        assertEquals(FaturaChave(1, dez), itens.single().fatura)
        assertEquals(r(40), m.resumoMes(nov).despesas)
        assertEquals(1, m.fatura(FaturaChave(1, dez)).itens.size)
        assertEquals(r(40), m.fatura(FaturaChave(1, dez)).total)
    }

    @Test
    fun parcelasNoCartaoCaemEmFaturasSeguidas() {
        val (dados, hoje) = base()
        val (c, parcelas) = compra(1, 300, 3, d(2026, 11, 10), cartao = 1)
        val m = motor(dados.comCompras(c to parcelas).copy(cartoes = listOf(nubank)), hoje)
        assertEquals(listOf(FaturaChave(1, dez), FaturaChave(1, jan), FaturaChave(1, Competencia.de(2027, 2))),
            listOf(nov, dez, jan).map { m.itensDoMes(it).single().fatura })
        assertEquals(EstadoDespesa.REGISTRADA, m.itensDoMes(nov).single().estado)
        assertEquals(EstadoDespesa.PREVISTA, m.itensDoMes(dez).single().estado)
    }

    @Test
    fun pixDebitoEDinheiroNascemPagos() {
        val (dados, hoje) = base()
        val m = motor(dados.copy(despesas = listOf(pontual(1, 30, d(2026, 11, 2)), pontual(2, 15, d(2026, 11, 19)))), hoje)
        assertTrue(m.itensDoMes(nov).all { it.estado == EstadoDespesa.PAGA })
        assertEquals(r(45), m.resumoMes(nov).despesasPagas)
    }

    @Test
    fun edicaoEExclusaoRefletemNaHora() {
        val (dados, hoje) = base()
        val original = dados.copy(despesas = listOf(pontual(1, 100, d(2026, 11, 2))))
        assertEquals(r(100), motor(original, hoje).resumoMes(nov).despesas)
        val editada = original.copy(despesas = listOf(pontual(1, 250, d(2026, 11, 2))))
        assertEquals(r(250), motor(editada, hoje).resumoMes(nov).despesas)
        assertEquals(Dinheiro.ZERO, motor(original.copy(despesas = emptyList()), hoje).resumoMes(nov).despesas)
    }

    // ---------------------------------------------------------------- histórico, alertas e projeção

    /** Histórico com gastos variáveis confirmados em ago, set e out (início de uso em julho). */
    private fun comHistorico(hoje: LocalDate, novembro: List<DespesaPontual>): MotorFinanceiro {
        val (dados, _) = base(hoje = hoje)
        val historico = listOf(7, 8, 9, 10).flatMap { mes ->
            listOf(pontual(mes * 10L, 300, d(2026, mes, 5)), pontual(mes * 10L + 1, 300, d(2026, mes, 25)))
        }
        val recebimentos = listOf(7, 8, 9, 10).map {
            RecebimentoRenda(rendaId = 1, competencia = Competencia.de(2026, it), valorRecebido = r(3_300), confirmadoEm = d(2026, it, 5))
        }
        return motor(dados.copy(versoesRenda = versoesSalario(Competencia.de(2026, 7) to 3_300), despesas = historico + novembro, recebimentos = recebimentos), hoje)
    }

    @Test
    fun poucoHistoricoAvisaDadosInsuficientes() {
        val (dados, hoje) = base()
        val m = motor(dados.copy(despesas = listOf(pontual(1, 100, d(2026, 11, 2)))), hoje)
        assertTrue(m.alertas().any { it.tipo == TipoAlerta.DADOS_INSUFICIENTES })
        assertTrue(m.alertas().none { it.tipo == TipoAlerta.RITMO_ACIMA || it.tipo == TipoAlerta.RITMO_SAUDAVEL })
        assertEquals(QualidadeEstimativa.RITMO_DO_MES, m.resumoMes(nov).estimativaVariaveis.qualidade)
    }

    @Test
    fun mesesIncompletosFicamForaDaBase() {
        val m = comHistorico(d(2026, 11, 20), emptyList())
        assertEquals(listOf(out, Competencia.de(2026, 9), Competencia.de(2026, 8)), m.mesesBase())
        assertTrue(m.mesIncompleto(Competencia.de(2026, 7))) // início de uso
        assertTrue(m.mesIncompleto(nov)) // mês atual
    }

    @Test
    fun ritmoAcimaDaMedianaGeraAlertaComNumeros() {
        val m = comHistorico(d(2026, 11, 12), listOf(pontual(500, 820, d(2026, 11, 4))))
        val alerta = m.alertas().single { it.tipo == TipoAlerta.RITMO_ACIMA }
        assertEquals("Até o dia 12 você gastou R$ 820,00, contra mediana de R$ 300,00 (+173%).", alerta.mensagem)
    }

    @Test
    fun ritmoAbaixoDaMedianaEhSaudavel() {
        val m = comHistorico(d(2026, 11, 12), listOf(pontual(500, 100, d(2026, 11, 4))))
        assertTrue(m.alertas().any { it.tipo == TipoAlerta.RITMO_SAUDAVEL })
    }

    @Test
    fun estimativaUsaMedianaDoQueFaltaNoMes() {
        val m = comHistorico(d(2026, 11, 12), listOf(pontual(500, 100, d(2026, 11, 4))))
        val resumo = m.resumoMes(nov)
        assertEquals(Estimativa(r(300), QualidadeEstimativa.HISTORICO), resumo.estimativaVariaveis)
        assertEquals(resumo.saldoDisponivel - r(300), resumo.saldoProjetado)
    }

    @Test
    fun linhaDoSaldoTerminaNoSaldoProjetado() {
        val m = comHistorico(d(2026, 11, 12), listOf(pontual(500, 100, d(2026, 11, 4))))
        val serie = m.serieSaldo(nov)
        assertEquals(31, serie.size)
        assertEquals(m.resumoMes(nov).saldoProjetado, serie.last().saldo)
        assertTrue(serie.filter { it.dia <= 12 }.none { it.projetado })
        assertTrue(serie.filter { it.dia > 12 }.all { it.projetado })
        assertEquals(serie[3].saldo - r(100), serie[4].saldo)
    }

    @Test
    fun riscoDeEstouroQuandoSaldoProjetadoNegativo() {
        val (dados, hoje) = base()
        val m = motor(dados.comFixos(fixo(1, 2_900)), hoje)
        assertTrue(m.resumoMes(nov).saldoProjetado.ehNegativo)
        assertTrue(m.alertas().any { it.tipo == TipoAlerta.RISCO_DE_ESTOURO && it.severidade == Severidade.CRITICO })
        assertTrue(m.alertas().any { it.tipo == TipoAlerta.COMPROMETIMENTO_ALTO })
    }

    @Test
    fun alertasDesligadosNaoAparecem() {
        val (dados, hoje) = base(configuracao = Configuracao(alertasAtivos = false))
        assertTrue(motor(dados.comFixos(fixo(1, 9_000)), hoje).alertas().isEmpty())
    }

    @Test
    fun mesFuturoMostraSoPrevistos() {
        val (dados, hoje) = base()
        val (c, parcelas) = compra(1, 600, 6, d(2026, 11, 10), cartao = 1)
        val m = motor(
            dados.comFixos(fixo(1, 90, debito = true), fixo(2, 40, cartao = 1)).comCompras(c to parcelas)
                .copy(cartoes = listOf(nubank), despesas = listOf(pontual(9, 200, d(2027, 1, 15)))),
            hoje,
        )
        val itens = m.itensDoMes(jan)
        assertEquals(4, itens.size)
        assertTrue(itens.all { it.estado == EstadoDespesa.PREVISTA })
        assertEquals(Dinheiro.ZERO, m.resumoMes(jan).despesasPagas)
    }

    @Test
    fun comprometimentoSemOrcamentoEhNulo() {
        val (dados, hoje) = base(salarioDesde = 400)
        assertNull(motor(dados, hoje).resumoMes(nov).comprometimentoBp)
    }

    @Test
    fun medianaEPercentual() {
        assertEquals(r(5), MotorFinanceiro.mediana(listOf(r(9), r(1), r(5))))
        assertEquals(Dinheiro(550), MotorFinanceiro.mediana(listOf(r(5), r(6))))
        assertEquals(Dinheiro(33_333), MotorFinanceiro.percentual(Dinheiro(333_333), 1_000))
    }
}
