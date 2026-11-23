package com.jhowww.financeiro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import com.jhowww.financeiro.data.local.entidade.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("SELECT * FROM usuario ORDER BY id LIMIT 1")
    suspend fun primeiro(): UsuarioEntity?

    @Insert
    suspend fun inserir(usuario: UsuarioEntity): Long
}

@Dao
interface ConfiguracaoDao {
    @Query("SELECT * FROM configuracao WHERE usuarioId = :usuarioId")
    fun observar(usuarioId: Long): Flow<ConfiguracaoEntity?>

    @Query("SELECT * FROM configuracao WHERE usuarioId = :usuarioId")
    suspend fun obter(usuarioId: Long): ConfiguracaoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(configuracao: ConfiguracaoEntity)
}

@Dao
abstract class CategoriaDao {
    @Query("SELECT * FROM categoria WHERE usuarioId = :usuarioId ORDER BY nome COLLATE NOCASE")
    abstract fun observarTodas(usuarioId: Long): Flow<List<CategoriaEntity>>

    @Insert
    abstract suspend fun inserir(categoria: CategoriaEntity): Long

    @Update
    abstract suspend fun atualizar(categoria: CategoriaEntity)

    @Query("DELETE FROM categoria WHERE id = :id")
    abstract suspend fun excluir(id: Long)

    /** Quantos registros usam a categoria; precisa ser zero para excluir. */
    @Query(
        """SELECT (SELECT COUNT(*) FROM despesa_pontual WHERE categoriaId = :id)
                + (SELECT COUNT(*) FROM gasto_fixo WHERE categoriaId = :id)
                + (SELECT COUNT(*) FROM compra_parcelada WHERE categoriaId = :id)
                + (SELECT COUNT(*) FROM receita_adicional WHERE categoriaId = :id)""",
    )
    abstract suspend fun contarUsos(id: Long): Int

    @Query("UPDATE despesa_pontual SET categoriaId = :destino WHERE categoriaId = :origem")
    abstract suspend fun moverDespesas(origem: Long, destino: Long)

    @Query("UPDATE gasto_fixo SET categoriaId = :destino WHERE categoriaId = :origem")
    abstract suspend fun moverGastosFixos(origem: Long, destino: Long)

    @Query("UPDATE compra_parcelada SET categoriaId = :destino WHERE categoriaId = :origem")
    abstract suspend fun moverCompras(origem: Long, destino: Long)

    @Query("UPDATE receita_adicional SET categoriaId = :destino WHERE categoriaId = :origem")
    abstract suspend fun moverReceitas(origem: Long, destino: Long)

    /** Transfere todos os registros para [destino] e só então exclui [origem]. Tudo ou nada. */
    @Transaction
    open suspend fun transferirEExcluir(origem: Long, destino: Long) {
        require(origem != destino) { "A categoria de destino deve ser diferente da origem" }
        moverDespesas(origem, destino)
        moverGastosFixos(origem, destino)
        moverCompras(origem, destino)
        moverReceitas(origem, destino)
        excluir(origem)
    }
}

@Dao
interface FormaPagamentoDao {
    @Query("SELECT * FROM forma_pagamento WHERE usuarioId = :usuarioId ORDER BY nome COLLATE NOCASE")
    fun observarTodas(usuarioId: Long): Flow<List<FormaPagamentoEntity>>

    @Insert
    suspend fun inserir(forma: FormaPagamentoEntity): Long

    @Update
    suspend fun atualizar(forma: FormaPagamentoEntity)

    @Query("DELETE FROM forma_pagamento WHERE id = :id")
    suspend fun excluir(id: Long)
}

@Dao
interface CartaoDao {
    @Query("SELECT * FROM cartao WHERE usuarioId = :usuarioId ORDER BY nome COLLATE NOCASE")
    fun observarCartoes(usuarioId: Long): Flow<List<CartaoEntity>>

    @Insert
    suspend fun inserirCartao(cartao: CartaoEntity): Long

    @Update
    suspend fun atualizarCartao(cartao: CartaoEntity)

    @Query("SELECT * FROM fatura WHERE cartaoId = :cartaoId AND competencia = :competencia")
    suspend fun fatura(cartaoId: Long, competencia: String): FaturaEntity?

    @Query("SELECT * FROM fatura WHERE cartaoId = :cartaoId ORDER BY competencia DESC")
    fun observarFaturas(cartaoId: Long): Flow<List<FaturaEntity>>

    @Insert
    suspend fun inserirFatura(fatura: FaturaEntity): Long

    @Query("SELECT * FROM pagamento_fatura WHERE faturaId = :faturaId ORDER BY data")
    fun observarPagamentos(faturaId: Long): Flow<List<PagamentoFaturaEntity>>

    @Query("SELECT * FROM pagamento_fatura WHERE faturaId IN (SELECT id FROM fatura WHERE competencia BETWEEN :de AND :ate)")
    fun observarPagamentosEntre(de: String, ate: String): Flow<List<PagamentoFaturaEntity>>

    @Insert
    suspend fun inserirPagamento(pagamento: PagamentoFaturaEntity): Long

    @Query("DELETE FROM pagamento_fatura WHERE id = :id")
    suspend fun excluirPagamento(id: Long)

    @Query("SELECT COALESCE(SUM(valor), 0) FROM pagamento_fatura WHERE faturaId = :faturaId")
    suspend fun totalPago(faturaId: Long): Long
}

@Dao
interface RendaDao {
    @Query("SELECT * FROM renda_principal WHERE usuarioId = :usuarioId ORDER BY id")
    fun observarRendas(usuarioId: Long): Flow<List<RendaPrincipalEntity>>

    @Insert
    suspend fun inserirRenda(renda: RendaPrincipalEntity): Long

    @Update
    suspend fun atualizarRenda(renda: RendaPrincipalEntity)

    @Query("SELECT * FROM renda_principal_versao ORDER BY vigenteDesde")
    fun observarVersoes(): Flow<List<RendaPrincipalVersaoEntity>>

    /** Uma nova versão na mesma competência substitui a anterior. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarVersao(versao: RendaPrincipalVersaoEntity): Long

    @Query("SELECT * FROM recebimento_renda WHERE competencia BETWEEN :de AND :ate")
    fun observarRecebimentos(de: String, ate: String): Flow<List<RecebimentoRendaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun confirmarRecebimento(recebimento: RecebimentoRendaEntity): Long

    @Query("DELETE FROM recebimento_renda WHERE rendaId = :rendaId AND competencia = :competencia")
    suspend fun desfazerRecebimento(rendaId: Long, competencia: String)

    @Query("SELECT * FROM receita_adicional WHERE usuarioId = :usuarioId AND competencia BETWEEN :de AND :ate ORDER BY data DESC")
    fun observarReceitas(usuarioId: Long, de: String, ate: String): Flow<List<ReceitaAdicionalEntity>>

    @Insert
    suspend fun inserirReceita(receita: ReceitaAdicionalEntity): Long

    @Update
    suspend fun atualizarReceita(receita: ReceitaAdicionalEntity)

    @Query("DELETE FROM receita_adicional WHERE id = :id")
    suspend fun excluirReceita(id: Long)
}

@Dao
interface ReservaDao {
    @Query("SELECT * FROM reserva_mes WHERE usuarioId = :usuarioId AND competencia BETWEEN :de AND :ate")
    fun observarMetas(usuarioId: Long, de: String, ate: String): Flow<List<ReservaMesEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarMeta(meta: ReservaMesEntity)

    @Query("DELETE FROM reserva_mes WHERE usuarioId = :usuarioId AND competencia = :competencia")
    suspend fun removerMeta(usuarioId: Long, competencia: String)

    @Query("SELECT * FROM aporte_reserva WHERE usuarioId = :usuarioId AND competencia BETWEEN :de AND :ate ORDER BY data")
    fun observarAportes(usuarioId: Long, de: String, ate: String): Flow<List<AporteReservaEntity>>

    @Insert
    suspend fun inserirAporte(aporte: AporteReservaEntity): Long

    @Query("DELETE FROM aporte_reserva WHERE id = :id")
    suspend fun excluirAporte(id: Long)

    @Query("SELECT * FROM retirada_reserva WHERE usuarioId = :usuarioId AND competencia BETWEEN :de AND :ate ORDER BY data")
    fun observarRetiradas(usuarioId: Long, de: String, ate: String): Flow<List<RetiradaReservaEntity>>

    @Insert
    suspend fun inserirRetirada(retirada: RetiradaReservaEntity): Long

    @Query("DELETE FROM retirada_reserva WHERE id = :id")
    suspend fun excluirRetirada(id: Long)
}

@Dao
abstract class GastoFixoDao {
    @Query("SELECT * FROM gasto_fixo WHERE usuarioId = :usuarioId ORDER BY diaVencimento, nome COLLATE NOCASE")
    abstract fun observarGastos(usuarioId: Long): Flow<List<GastoFixoEntity>>

    @Insert
    abstract suspend fun inserirGasto(gasto: GastoFixoEntity): Long

    @Update
    abstract suspend fun atualizarGasto(gasto: GastoFixoEntity)

    @Query("DELETE FROM gasto_fixo WHERE id = :id")
    abstract suspend fun excluirGasto(id: Long)

    @Query("SELECT * FROM gasto_fixo_versao ORDER BY vigenteDesde")
    abstract fun observarVersoes(): Flow<List<GastoFixoVersaoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun salvarVersao(versao: GastoFixoVersaoEntity): Long

    /** Cria o gasto fixo já com seu primeiro valor, numa transação. */
    @Transaction
    open suspend fun inserirComValor(gasto: GastoFixoEntity, valor: Long): Long {
        val id = inserirGasto(gasto)
        salvarVersao(GastoFixoVersaoEntity(gastoFixoId = id, vigenteDesde = gasto.inicio, valor = valor))
        return id
    }

    @Query("SELECT * FROM ocorrencia_fixo WHERE competencia BETWEEN :de AND :ate")
    abstract fun observarOcorrencias(de: String, ate: String): Flow<List<OcorrenciaFixoEntity>>

    /** Uma ocorrência por gasto e competência: gravar de novo substitui a anterior. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun salvarOcorrencia(ocorrencia: OcorrenciaFixoEntity): Long

    @Query("DELETE FROM ocorrencia_fixo WHERE gastoFixoId = :gastoFixoId AND competencia = :competencia")
    abstract suspend fun excluirOcorrencia(gastoFixoId: Long, competencia: String)

    /** Ao encerrar um gasto: apaga ocorrências gravadas depois do fim, exceto as pagas. */
    @Query("DELETE FROM ocorrencia_fixo WHERE gastoFixoId = :gastoFixoId AND competencia > :fim AND status != 'PAGA'")
    abstract suspend fun limparOcorrenciasApos(gastoFixoId: Long, fim: String)

    @Transaction
    open suspend fun encerrar(gasto: GastoFixoEntity, fim: String) {
        atualizarGasto(gasto.copy(fim = fim))
        limparOcorrenciasApos(gasto.id, fim)
    }
}

@Dao
abstract class ParcelamentoDao {
    @Query("SELECT * FROM compra_parcelada WHERE usuarioId = :usuarioId ORDER BY dataCompra DESC")
    abstract fun observarCompras(usuarioId: Long): Flow<List<CompraParceladaEntity>>

    @Insert
    abstract suspend fun inserirCompra(compra: CompraParceladaEntity): Long

    @Insert
    abstract suspend fun inserirParcelas(parcelas: List<ParcelaEntity>)

    @Query("DELETE FROM parcela WHERE compraId = :compraId")
    abstract suspend fun excluirParcelasDaCompra(compraId: Long)

    @Update
    abstract suspend fun atualizarCompra(compra: CompraParceladaEntity)

    /** Compra e todas as parcelas entram juntas ou nenhuma entra. */
    @Transaction
    open suspend fun inserirCompraComParcelas(compra: CompraParceladaEntity, parcelas: List<ParcelaEntity>): Long {
        require(parcelas.size == compra.numeroParcelas) { "Quantidade de parcelas não confere com a compra" }
        val id = inserirCompra(compra)
        inserirParcelas(parcelas.map { it.copy(compraId = id) })
        return id
    }

    /** Edita a compra regerando as parcelas (ex.: primeira competência ajustada). */
    @Transaction
    open suspend fun substituirCompra(compra: CompraParceladaEntity, parcelas: List<ParcelaEntity>) {
        require(parcelas.size == compra.numeroParcelas) { "Quantidade de parcelas não confere com a compra" }
        atualizarCompra(compra)
        excluirParcelasDaCompra(compra.id)
        inserirParcelas(parcelas.map { it.copy(compraId = compra.id) })
    }

    @Query("DELETE FROM compra_parcelada WHERE id = :id")
    abstract suspend fun excluirCompra(id: Long)

    @Query("SELECT * FROM parcela WHERE competencia BETWEEN :de AND :ate ORDER BY competencia, numero")
    abstract fun observarParcelasEntre(de: String, ate: String): Flow<List<ParcelaEntity>>

    @Query("SELECT * FROM parcela WHERE compraId = :compraId ORDER BY numero")
    abstract fun observarParcelasDaCompra(compraId: Long): Flow<List<ParcelaEntity>>

    @Update
    abstract suspend fun atualizarParcela(parcela: ParcelaEntity)
}

@Dao
interface DespesaDao {
    @Query("SELECT * FROM despesa_pontual WHERE usuarioId = :usuarioId AND competencia BETWEEN :de AND :ate ORDER BY data DESC, id DESC")
    fun observarEntre(usuarioId: Long, de: String, ate: String): Flow<List<DespesaPontualEntity>>

    @Query("SELECT * FROM despesa_pontual WHERE faturaId = :faturaId ORDER BY data")
    fun observarDaFatura(faturaId: Long): Flow<List<DespesaPontualEntity>>

    @Insert
    suspend fun inserir(despesa: DespesaPontualEntity): Long

    @Update
    suspend fun atualizar(despesa: DespesaPontualEntity)

    @Delete
    suspend fun excluir(despesa: DespesaPontualEntity)

    @Query("SELECT * FROM despesa_pontual WHERE estornoDe = :id")
    suspend fun estornosDe(id: Long): List<DespesaPontualEntity>
}
