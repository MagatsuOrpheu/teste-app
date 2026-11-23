package com.jhowww.financeiro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.jhowww.financeiro.data.local.entidade.AporteReservaEntity
import com.jhowww.financeiro.data.local.entidade.CartaoEntity
import com.jhowww.financeiro.data.local.entidade.CategoriaEntity
import com.jhowww.financeiro.data.local.entidade.CompraParceladaEntity
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
import kotlinx.coroutines.flow.Flow

/** Leitura completa de cada tabela, para montar o retrato que o motor financeiro usa. */
@Dao
interface RetratoDao {
    @Query("SELECT * FROM categoria ORDER BY nome COLLATE NOCASE")
    fun categorias(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM forma_pagamento ORDER BY nome COLLATE NOCASE")
    fun formas(): Flow<List<FormaPagamentoEntity>>

    @Query("SELECT * FROM cartao ORDER BY nome COLLATE NOCASE")
    fun cartoes(): Flow<List<CartaoEntity>>

    @Query("SELECT * FROM cartao WHERE id = :id")
    suspend fun cartao(id: Long): CartaoEntity?

    @Query("SELECT * FROM fatura")
    fun faturas(): Flow<List<FaturaEntity>>

    @Query("SELECT * FROM pagamento_fatura ORDER BY data")
    fun pagamentosFatura(): Flow<List<PagamentoFaturaEntity>>

    @Query("SELECT * FROM renda_principal ORDER BY id")
    fun rendas(): Flow<List<RendaPrincipalEntity>>

    @Query("SELECT * FROM renda_principal_versao ORDER BY vigenteDesde")
    fun versoesRenda(): Flow<List<RendaPrincipalVersaoEntity>>

    @Query("SELECT * FROM recebimento_renda")
    fun recebimentos(): Flow<List<RecebimentoRendaEntity>>

    @Query("SELECT * FROM receita_adicional ORDER BY data DESC")
    fun receitas(): Flow<List<ReceitaAdicionalEntity>>

    @Query("SELECT * FROM reserva_mes")
    fun metasReserva(): Flow<List<ReservaMesEntity>>

    @Query("SELECT * FROM aporte_reserva ORDER BY data")
    fun aportes(): Flow<List<AporteReservaEntity>>

    @Query("SELECT * FROM retirada_reserva ORDER BY data")
    fun retiradas(): Flow<List<RetiradaReservaEntity>>

    @Query("SELECT * FROM gasto_fixo ORDER BY diaVencimento, nome COLLATE NOCASE")
    fun gastosFixos(): Flow<List<GastoFixoEntity>>

    @Query("SELECT * FROM gasto_fixo_versao ORDER BY vigenteDesde")
    fun versoesFixo(): Flow<List<GastoFixoVersaoEntity>>

    @Query("SELECT * FROM ocorrencia_fixo")
    fun ocorrencias(): Flow<List<OcorrenciaFixoEntity>>

    @Query("SELECT * FROM compra_parcelada ORDER BY dataCompra DESC")
    fun compras(): Flow<List<CompraParceladaEntity>>

    @Query("SELECT * FROM parcela ORDER BY compraId, numero")
    fun parcelas(): Flow<List<ParcelaEntity>>

    @Query("SELECT * FROM despesa_pontual ORDER BY data DESC, id DESC")
    fun despesas(): Flow<List<DespesaPontualEntity>>
}
