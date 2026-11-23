package com.jhowww.financeiro.data.repositorio

import androidx.room.withTransaction
import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.Fatura
import com.jhowww.financeiro.domain.modelo.GastoFixo
import com.jhowww.financeiro.domain.modelo.OcorrenciaFixo
import com.jhowww.financeiro.domain.modelo.PagamentoFatura
import com.jhowww.financeiro.domain.modelo.Parcela
import com.jhowww.financeiro.domain.modelo.StatusOcorrencia
import com.jhowww.financeiro.domain.motor.CalendarioCartao
import com.jhowww.financeiro.domain.motor.FaturaChave
import com.jhowww.financeiro.domain.motor.Parcelamento
import java.time.LocalDate

/**
 * Gravações que dependem de regra de negócio. Uma compra no cartão fica ligada à fatura em que
 * caiu no momento do lançamento; assim, mudar depois os dias do cartão não move compras antigas.
 */
class Operacoes(
    private val db: FinanceiroDatabase,
    private val sessao: Sessao,
    private val pagamentos: PagamentoRepositorio,
    private val despesas: DespesaRepositorio,
    private val parcelamentos: ParcelamentoRepositorio,
    private val gastosFixos: GastoFixoRepositorio = GastoFixoRepositorio(db, sessao),
) {

    private suspend fun cartao(id: Long): Cartao =
        requireNotNull(db.retratoDao().cartao(id)) { "Cartão não encontrado" }.paraDominio()

    /** Devolve a fatura gravada da chave, criando-a com as datas atuais do cartão se preciso. */
    suspend fun faturaDe(cartao: Cartao, chave: FaturaChave): Fatura = pagamentos.obterOuCriarFatura(
        Fatura(
            cartaoId = cartao.id,
            competencia = chave.competencia,
            dataFechamento = CalendarioCartao.dataFechamento(cartao, chave.competencia),
            dataVencimento = CalendarioCartao.dataVencimento(cartao, chave.competencia),
        ),
    )

    /** Inclui ou edita uma despesa pontual (inclusive estorno). No cartão, liga à fatura do dia. */
    suspend fun salvarDespesa(despesa: DespesaPontual): Long {
        sessao.usuarioId()
        return db.withTransaction {
            val cartaoId = despesa.cartaoId
            val pronta = when {
                cartaoId == null -> despesa.copy(faturaId = null, faturaManual = false)
                despesa.faturaManual && despesa.faturaId != null -> despesa
                else -> {
                    val c = cartao(cartaoId)
                    despesa.copy(faturaId = faturaDe(c, CalendarioCartao.faturaDaCompra(c, despesa.data)).id)
                }
            }
            despesas.salvar(pronta)
        }
    }

    /** Estorno: valor negativo ligado à compra original, na data em que aconteceu. */
    suspend fun estornar(original: DespesaPontual, valor: Dinheiro, data: LocalDate): Long {
        require(original.id != 0L && !original.ehEstorno) { "Só é possível estornar uma compra gravada" }
        require(valor > Dinheiro.ZERO) { "O valor do estorno precisa ser maior que zero" }
        require(valor <= original.valor) { "O estorno não pode ser maior que a compra" }
        return salvarDespesa(
            original.copy(
                id = 0,
                descricao = "Estorno: ${original.descricao}",
                valor = -valor,
                data = data,
                competencia = Competencia.de(data),
                faturaId = null,
                faturaManual = false,
                estornoDe = original.id,
                observacao = null,
            ),
        )
    }

    /**
     * Cria a compra com todas as parcelas. [valorParcela] só quando há juros.
     * No cartão, cada parcela fica ligada à sua fatura (uma por mês, a partir da fatura da compra).
     */
    suspend fun lancarCompraParcelada(compra: CompraParcelada, valorParcela: Dinheiro? = null): Long {
        sessao.usuarioId()
        val geradas = Parcelamento.gerar(compra.valorTotal, compra.numeroParcelas, compra.primeiraCompetencia, valorParcela)
        return db.withTransaction { parcelamentos.criar(compra, ligarAsFaturas(compra, geradas)) }
    }

    /**
     * Muda a competência da primeira parcela: todas se deslocam juntas e, no cartão, voltam a ser
     * ligadas às faturas correspondentes. Recusa se alguma parcela já foi marcada como paga.
     */
    suspend fun mudarPrimeiraParcela(compra: CompraParcelada, parcelas: List<Parcela>, novaPrimeira: Competencia) {
        require(parcelas.none { it.pagaEm != null }) { "Desmarque as parcelas pagas antes de mudar a primeira parcela" }
        sessao.usuarioId()
        val deslocadas = Parcelamento.deslocar(parcelas, novaPrimeira)
        val atualizada = compra.copy(primeiraCompetencia = novaPrimeira)
        db.withTransaction { parcelamentos.substituir(atualizada, ligarAsFaturas(atualizada, deslocadas)) }
    }

    /** No cartão, cada parcela vai para uma fatura por mês, a partir da fatura da compra. */
    private suspend fun ligarAsFaturas(compra: CompraParcelada, parcelas: List<Parcela>): List<Parcela> {
        val cartaoId = compra.cartaoId ?: return parcelas.map { it.copy(faturaId = null, faturaManual = false) }
        val c = cartao(cartaoId)
        val primeira = CalendarioCartao.faturaDaCompra(c, compra.dataCompra)
        return parcelas.map { p ->
            if (p.faturaManual && p.faturaId != null) return@map p
            val meses = p.competencia.mesesDesde(Competencia.de(compra.dataCompra))
            p.copy(faturaId = faturaDe(c, primeira.copy(competencia = primeira.competencia.mais(meses))).id)
        }
    }

    // ------------------------------------------------------------ faturas

    /** Pagamento (parcial ou integral) de uma fatura; cria a fatura gravada se ela ainda só existia no cálculo. */
    suspend fun pagarFatura(chave: FaturaChave, valor: Dinheiro, data: LocalDate): Long {
        require(valor > Dinheiro.ZERO) { "O pagamento precisa ser maior que zero" }
        sessao.usuarioId()
        return db.withTransaction {
            val fatura = faturaDe(cartao(chave.cartaoId), chave)
            pagamentos.registrarPagamento(PagamentoFatura(faturaId = fatura.id, data = data, valor = valor))
        }
    }

    /**
     * Correção manual da fatura de destino (a validação de faturas quitadas fica com o motor, antes da chamada).
     * Com [destino] nulo, volta à fatura calculada automaticamente. A competência da despesa nunca muda.
     */
    suspend fun moverDespesa(despesa: DespesaPontual, destino: FaturaChave?): Long {
        val cartaoId = requireNotNull(despesa.cartaoId) { "A despesa não é do cartão" }
        if (destino == null) return salvarDespesa(despesa.copy(faturaId = null, faturaManual = false))
        require(destino.cartaoId == cartaoId) { "A fatura de destino precisa ser do mesmo cartão" }
        sessao.usuarioId()
        return db.withTransaction {
            val fatura = faturaDe(cartao(cartaoId), destino)
            salvarDespesa(despesa.copy(faturaId = fatura.id, faturaManual = true))
        }
    }

    suspend fun moverParcela(compra: CompraParcelada, parcela: Parcela, destino: FaturaChave?) {
        val cartaoId = requireNotNull(compra.cartaoId) { "A compra não é do cartão" }
        sessao.usuarioId()
        db.withTransaction {
            val atualizada = if (destino == null) {
                ligarAsFaturas(compra, listOf(parcela.copy(faturaId = null, faturaManual = false))).single()
            } else {
                require(destino.cartaoId == cartaoId) { "A fatura de destino precisa ser do mesmo cartão" }
                parcela.copy(faturaId = faturaDe(cartao(cartaoId), destino).id, faturaManual = true)
            }
            parcelamentos.atualizarParcela(atualizada)
        }
    }

    /** Gasto fixo no cartão: grava a fatura corrigida na ocorrência do mês (ou a desfaz). */
    suspend fun moverFixo(gasto: GastoFixo, competencia: Competencia, gravada: OcorrenciaFixo?, destino: FaturaChave?) {
        val cartaoId = requireNotNull(gasto.cartaoId) { "O gasto fixo não é do cartão" }
        sessao.usuarioId()
        db.withTransaction {
            if (destino == null) {
                if (gravada == null) return@withTransaction
                // Só existia para guardar a fatura: volta a ser virtual.
                if (gravada.status == StatusOcorrencia.REGISTRADA && gravada.valor == null) {
                    gastosFixos.descartarOcorrencia(gasto.id, competencia)
                } else {
                    gastosFixos.salvarOcorrencia(gravada.copy(id = 0, faturaId = null, faturaManual = false))
                }
            } else {
                require(destino.cartaoId == cartaoId) { "A fatura de destino precisa ser do mesmo cartão" }
                val fatura = faturaDe(cartao(cartaoId), destino)
                val base = gravada ?: OcorrenciaFixo(gastoFixoId = gasto.id, competencia = competencia, status = StatusOcorrencia.REGISTRADA)
                gastosFixos.salvarOcorrencia(base.copy(id = 0, faturaId = fatura.id, faturaManual = true))
            }
        }
    }
}
