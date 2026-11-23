package com.jhowww.financeiro.data.repositorio

import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.data.local.entidade.AporteReservaEntity
import com.jhowww.financeiro.data.local.entidade.CartaoEntity
import com.jhowww.financeiro.data.local.entidade.CategoriaEntity
import com.jhowww.financeiro.data.local.entidade.CompraParceladaEntity
import com.jhowww.financeiro.data.local.entidade.ConfiguracaoEntity
import com.jhowww.financeiro.data.local.entidade.DespesaPontualEntity
import com.jhowww.financeiro.data.local.entidade.FaturaEntity
import com.jhowww.financeiro.data.local.entidade.FormaPagamentoEntity
import com.jhowww.financeiro.data.local.entidade.GastoFixoEntity
import com.jhowww.financeiro.data.local.entidade.GastoFixoVersaoEntity
import com.jhowww.financeiro.data.local.entidade.OcorrenciaFixoEntity
import com.jhowww.financeiro.data.local.entidade.PagamentoFaturaEntity
import com.jhowww.financeiro.data.local.entidade.ParcelaEntity
import com.jhowww.financeiro.data.local.entidade.ReceitaAdicionalEntity
import com.jhowww.financeiro.data.local.entidade.RecebimentoRendaEntity
import com.jhowww.financeiro.data.local.entidade.RendaPrincipalEntity
import com.jhowww.financeiro.data.local.entidade.RendaPrincipalVersaoEntity
import com.jhowww.financeiro.data.local.entidade.ReservaMesEntity
import com.jhowww.financeiro.data.local.entidade.RetiradaReservaEntity
import com.jhowww.financeiro.domain.modelo.Categoria
import com.jhowww.financeiro.domain.modelo.Configuracao
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.motor.DadosFinanceiros
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Tudo o que as telas precisam: os dados do motor mais os cadastros de apoio. */
data class Retrato(
    val dados: DadosFinanceiros,
    val categorias: List<Categoria>,
    val formas: List<FormaPagamento>,
) {
    val vazio: Boolean get() = dados.rendas.isEmpty() && dados.despesas.isEmpty() && dados.gastosFixos.isEmpty()
}

/** Observa o banco inteiro: qualquer gravação em qualquer tabela gera um retrato novo. */
class RetratoRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {

    @Suppress("UNCHECKED_CAST")
    fun observar(): Flow<Retrato> {
        val dao = db.retratoDao()
        val fluxos: List<Flow<Any?>> = listOf(
            sessao.doUsuario { db.configuracaoDao().observar(it) },
            dao.categorias(), dao.formas(), dao.cartoes(), dao.faturas(), dao.pagamentosFatura(),
            dao.rendas(), dao.versoesRenda(), dao.recebimentos(), dao.receitas(),
            dao.metasReserva(), dao.aportes(), dao.retiradas(),
            dao.gastosFixos(), dao.versoesFixo(), dao.ocorrencias(),
            dao.compras(), dao.parcelas(), dao.despesas(),
        )
        return combine(fluxos) { v ->
            val dados = DadosFinanceiros(
                configuracao = (v[0] as ConfiguracaoEntity?)?.paraDominio() ?: Configuracao(),
                cartoes = (v[3] as List<CartaoEntity>).map { it.paraDominio() },
                faturas = (v[4] as List<FaturaEntity>).map { it.paraDominio() },
                pagamentosFatura = (v[5] as List<PagamentoFaturaEntity>).map { it.paraDominio() },
                rendas = (v[6] as List<RendaPrincipalEntity>).map { it.paraDominio() },
                versoesRenda = (v[7] as List<RendaPrincipalVersaoEntity>).groupBy({ it.rendaId }, { it.paraDominio() }),
                recebimentos = (v[8] as List<RecebimentoRendaEntity>).map { it.paraDominio() },
                receitas = (v[9] as List<ReceitaAdicionalEntity>).map { it.paraDominio() },
                metasReserva = (v[10] as List<ReservaMesEntity>).map { it.paraDominio() },
                aportes = (v[11] as List<AporteReservaEntity>).map { it.paraDominio() },
                retiradas = (v[12] as List<RetiradaReservaEntity>).map { it.paraDominio() },
                gastosFixos = (v[13] as List<GastoFixoEntity>).map { it.paraDominio() },
                versoesFixo = (v[14] as List<GastoFixoVersaoEntity>).groupBy({ it.gastoFixoId }, { it.paraDominio() }),
                ocorrencias = (v[15] as List<OcorrenciaFixoEntity>).map { it.paraDominio() },
                compras = (v[16] as List<CompraParceladaEntity>).map { it.paraDominio() },
                parcelas = (v[17] as List<ParcelaEntity>).map { it.paraDominio() },
                despesas = (v[18] as List<DespesaPontualEntity>).map { it.paraDominio() },
            )
            Retrato(
                dados = dados,
                categorias = (v[1] as List<CategoriaEntity>).map { it.paraDominio() },
                formas = (v[2] as List<FormaPagamentoEntity>).map { it.paraDominio() },
            )
        }
    }
}
