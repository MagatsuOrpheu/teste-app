package com.jhowww.financeiro.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jhowww.financeiro.data.local.dao.CartaoDao
import com.jhowww.financeiro.data.local.dao.CategoriaDao
import com.jhowww.financeiro.data.local.dao.ConfiguracaoDao
import com.jhowww.financeiro.data.local.dao.DespesaDao
import com.jhowww.financeiro.data.local.dao.FormaPagamentoDao
import com.jhowww.financeiro.data.local.dao.GastoFixoDao
import com.jhowww.financeiro.data.local.dao.ParcelamentoDao
import com.jhowww.financeiro.data.local.dao.RendaDao
import com.jhowww.financeiro.data.local.dao.ReservaDao
import com.jhowww.financeiro.data.local.dao.RetratoDao
import com.jhowww.financeiro.data.local.dao.UsuarioDao
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

@Database(
    entities = [
        UsuarioEntity::class,
        ConfiguracaoEntity::class,
        CategoriaEntity::class,
        FormaPagamentoEntity::class,
        CartaoEntity::class,
        FaturaEntity::class,
        PagamentoFaturaEntity::class,
        RendaPrincipalEntity::class,
        RendaPrincipalVersaoEntity::class,
        RecebimentoRendaEntity::class,
        ReceitaAdicionalEntity::class,
        ReservaMesEntity::class,
        AporteReservaEntity::class,
        RetiradaReservaEntity::class,
        GastoFixoEntity::class,
        GastoFixoVersaoEntity::class,
        OcorrenciaFixoEntity::class,
        CompraParceladaEntity::class,
        ParcelaEntity::class,
        DespesaPontualEntity::class,
    ],
    version = FinanceiroDatabase.VERSAO,
    exportSchema = true,
    // 1 → 2: só cria tabelas novas; o Room gera o SQL a partir dos esquemas exportados.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class FinanceiroDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun configuracaoDao(): ConfiguracaoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun formaPagamentoDao(): FormaPagamentoDao
    abstract fun cartaoDao(): CartaoDao
    abstract fun rendaDao(): RendaDao
    abstract fun reservaDao(): ReservaDao
    abstract fun gastoFixoDao(): GastoFixoDao
    abstract fun parcelamentoDao(): ParcelamentoDao
    abstract fun despesaDao(): DespesaDao
    abstract fun retratoDao(): RetratoDao

    companion object {
        const val VERSAO = 2
        const val NOME_ARQUIVO = "financeiro.db"

        /**
         * Sem fallbackToDestructiveMigration: se faltar uma migração, o app falha de forma
         * visível em vez de apagar os dados do usuário.
         */
        fun criar(context: Context): FinanceiroDatabase =
            Room.databaseBuilder(context, FinanceiroDatabase::class.java, NOME_ARQUIVO)
                .addMigrations(*Migracoes.TODAS)
                .build()
    }
}
