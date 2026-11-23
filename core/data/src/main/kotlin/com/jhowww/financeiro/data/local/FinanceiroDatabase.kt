package com.jhowww.financeiro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UsuarioEntity::class],
    version = FinanceiroDatabase.VERSAO,
    exportSchema = true,
)
abstract class FinanceiroDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        const val VERSAO = 1
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
