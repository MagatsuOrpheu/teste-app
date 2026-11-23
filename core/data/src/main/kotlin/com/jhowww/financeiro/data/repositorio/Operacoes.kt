package com.jhowww.financeiro.data.repositorio

import androidx.room.withTransaction
import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.Fatura
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
        return db.withTransaction {
            val cartaoId = compra.cartaoId
            val parcelas = if (cartaoId == null) {
                geradas
            } else {
                val c = cartao(cartaoId)
                val primeira = CalendarioCartao.faturaDaCompra(c, compra.dataCompra)
                geradas.map { p ->
                    val meses = p.competencia.mesesDesde(Competencia.de(compra.dataCompra))
                    p.copy(faturaId = faturaDe(c, primeira.copy(competencia = primeira.competencia.mais(meses))).id)
                }
            }
            parcelamentos.criar(compra, parcelas)
        }
    }
}
