package com.jhowww.financeiro.domain.motor

import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.Parcela
import java.time.LocalDate

object Parcelamento {

    /** Por padrão a primeira parcela cai no mês da compra; o usuário pode ajustar. */
    fun primeiraCompetenciaPadrao(dataCompra: LocalDate): Competencia = Competencia.de(dataCompra)

    /**
     * Gera as parcelas, uma por mês a partir de [primeira].
     * Sem [valorParcela]: divide o total e a diferença do arredondamento vai para a última
     * (100 em 3× = 33,33 + 33,33 + 33,34).
     * Com [valorParcela] (compra com juros): todas usam esse valor, sem corrigir para o total.
     */
    fun gerar(
        valorTotal: Dinheiro,
        numeroParcelas: Int,
        primeira: Competencia,
        valorParcela: Dinheiro? = null,
    ): List<Parcela> {
        require(numeroParcelas >= 1) { "O número de parcelas precisa ser pelo menos 1" }
        require(valorTotal > Dinheiro.ZERO) { "O valor total precisa ser maior que zero" }
        if (valorParcela != null) require(valorParcela > Dinheiro.ZERO) { "O valor da parcela precisa ser maior que zero" }

        val base = valorParcela ?: Dinheiro(valorTotal.centavos / numeroParcelas)
        val ultima = valorParcela ?: (valorTotal - base * (numeroParcelas - 1))
        return (1..numeroParcelas).map { n ->
            Parcela(
                numero = n,
                competencia = primeira.mais(n - 1L),
                valor = if (n == numeroParcelas) ultima else base,
            )
        }
    }

    /** Muda a competência da primeira parcela; todas se deslocam juntas e a fatura volta a ser calculada. */
    fun deslocar(parcelas: List<Parcela>, novaPrimeira: Competencia): List<Parcela> {
        val ordenadas = parcelas.sortedBy { it.numero }
        val atual = ordenadas.firstOrNull()?.competencia ?: return parcelas
        val meses = novaPrimeira.mesesDesde(atual)
        return ordenadas.map { it.copy(competencia = it.competencia.mais(meses), faturaId = null, faturaManual = false) }
    }
}
