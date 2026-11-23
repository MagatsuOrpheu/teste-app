package com.jhowww.financeiro.domain.motor

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
import com.jhowww.financeiro.domain.modelo.VersaoValor

/**
 * Tudo o que o motor precisa, já lido do banco. Os mapas de versões são indexados pelo id
 * da renda ou do gasto fixo. Em escala pessoal cabe inteiro na memória.
 */
data class DadosFinanceiros(
    val configuracao: Configuracao = Configuracao(),
    val rendas: List<RendaPrincipal> = emptyList(),
    val versoesRenda: Map<Long, List<VersaoValor>> = emptyMap(),
    val recebimentos: List<RecebimentoRenda> = emptyList(),
    val receitas: List<ReceitaAdicional> = emptyList(),
    val metasReserva: List<ReservaMes> = emptyList(),
    val aportes: List<AporteReserva> = emptyList(),
    val retiradas: List<RetiradaReserva> = emptyList(),
    val gastosFixos: List<GastoFixo> = emptyList(),
    val versoesFixo: Map<Long, List<VersaoValor>> = emptyMap(),
    val ocorrencias: List<OcorrenciaFixo> = emptyList(),
    val compras: List<CompraParcelada> = emptyList(),
    val parcelas: List<Parcela> = emptyList(),
    val despesas: List<DespesaPontual> = emptyList(),
    val cartoes: List<Cartao> = emptyList(),
    val faturas: List<Fatura> = emptyList(),
    val pagamentosFatura: List<PagamentoFatura> = emptyList(),
)
