package com.jhowww.financeiro.data.local

import androidx.room.migration.Migration

/**
 * Migrações escritas à mão, em ordem. Mudanças simples (tabelas ou colunas novas) usam
 * AutoMigration no @Database; as que precisam transformar dados entram aqui.
 *
 * Histórico:
 * - 1 → 2: AutoMigration (cria as tabelas do modelo financeiro).
 */
object Migracoes {
    val TODAS: Array<Migration> = arrayOf()
}
