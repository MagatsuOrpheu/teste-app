package com.jhowww.financeiro.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.data.local.entidade.CategoriaEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Simula um aparelho com o banco da versão 1 (Etapa 1) e abre com a versão atual:
 * a migração precisa preservar os dados e deixar todas as tabelas novas utilizáveis.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigracaoTest {

    @Test
    fun versao1ParaAtualPreservaDados() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val arquivo = ctx.getDatabasePath(FinanceiroDatabase.NOME_ARQUIVO)
        arquivo.parentFile?.mkdirs()
        arquivo.delete()

        SQLiteDatabase.openOrCreateDatabase(arquivo, null).use { v1 ->
            v1.execSQL("CREATE TABLE IF NOT EXISTS `usuario` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nome` TEXT NOT NULL)")
            v1.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            v1.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'versao-1')")
            v1.execSQL("INSERT INTO usuario (nome) VALUES ('Existente')")
            v1.version = 1
        }

        val db = FinanceiroDatabase.criar(ctx)
        try {
            val usuario = db.usuarioDao().primeiro()
            assertEquals("Existente", usuario?.nome)
            val id = db.categoriaDao().inserir(
                CategoriaEntity(usuarioId = usuario!!.id, nome = "Teste", cor = 0, icone = null, ativa = true),
            )
            assertEquals(0, db.categoriaDao().contarUsos(id))
            assertEquals(FinanceiroDatabase.VERSAO, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
            arquivo.delete()
        }
    }
}
