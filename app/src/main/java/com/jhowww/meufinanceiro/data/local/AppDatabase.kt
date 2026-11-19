package com.jhowww.meufinanceiro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        RecurringEntity::class,
        RecurringPaidEntity::class,
        TransactionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringDao(): RecurringDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        /** Categorias criadas na primeira abertura (nome, cor ARGB — paleta do Obsidian). */
        private val DEFAULT_CATEGORIES = listOf(
            "Moradia" to 0xFFA882FF,
            "Alimentação" to 0xFFE9973F,
            "Mercado" to 0xFF44CF6E,
            "Transporte" to 0xFF027AFF,
            "Contas" to 0xFF53DFDD,
            "Saúde" to 0xFFFB464C,
            "Educação" to 0xFFE0DE71,
            "Lazer" to 0xFFFA99CD,
            "Assinaturas" to 0xFF8B6CEF,
            "Compras" to 0xFFE8A87C,
            "Outros" to 0xFF999999,
        )

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "meu_financeiro.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DEFAULT_CATEGORIES.forEach { (name, color) ->
                            db.execSQL(
                                "INSERT INTO categories (name, color, monthlyLimitCents) VALUES (?, ?, 0)",
                                arrayOf<Any>(name, color),
                            )
                        }
                    }
                })
                .build()
    }
}
