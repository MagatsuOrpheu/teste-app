package com.jhowww.financeiro.domain.motor

import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.modelo.Cartao
import java.time.LocalDate

/**
 * Regras de datas do cartão. A competência da fatura é o mês do vencimento.
 * Compra até o dia do fechamento (inclusive) entra na fatura que fecha naquele mês;
 * depois dele, na seguinte. Dias inexistentes (31 em abril) caem no último dia do mês.
 */
object CalendarioCartao {

    fun faturaDaCompra(cartao: Cartao, data: LocalDate): FaturaChave {
        val mes = Competencia.de(data)
        val mesFechamento = if (data <= mes.diaEfetivo(cartao.diaFechamento)) mes else mes.proxima()
        return FaturaChave(cartao.id, competenciaDoVencimento(cartao, mesFechamento))
    }

    /** Vencimento depois do fechamento no mesmo mês; senão, no mês seguinte. */
    fun competenciaDoVencimento(cartao: Cartao, mesFechamento: Competencia): Competencia =
        if (cartao.diaVencimento > cartao.diaFechamento) mesFechamento else mesFechamento.proxima()

    fun mesDoFechamento(cartao: Cartao, competenciaFatura: Competencia): Competencia =
        if (cartao.diaVencimento > cartao.diaFechamento) competenciaFatura else competenciaFatura.anterior()

    fun dataFechamento(cartao: Cartao, competenciaFatura: Competencia): LocalDate =
        mesDoFechamento(cartao, competenciaFatura).diaEfetivo(cartao.diaFechamento)

    fun dataVencimento(cartao: Cartao, competenciaFatura: Competencia): LocalDate =
        competenciaFatura.diaEfetivo(cartao.diaVencimento)
}
