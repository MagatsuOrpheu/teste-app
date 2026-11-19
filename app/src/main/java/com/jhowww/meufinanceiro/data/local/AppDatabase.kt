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
        /** Categorias criadas na primeira abertura (nome, cor ARGB). */
        private val DEFAULT_CATEGORIES = listOf(
            "Moradia" to 0xFF5C6BC0,
            "Alimentação" to 0xFFEF6C00,
            "Mercado" to 0xFF43A047,
            "Transporte" to 0xFF1E88E5,
            "Contas" to 0xFF8E24AA,
            "Saúde" to 0xFFE53935,
            "Educação" to 0xFF00897B,
            "Lazer" to 0xFFFDD835,
            "Assinaturas" to 0xFF6D4C41,
            "Compras" to 0xFFD81B60,
            "Outros" to 0xFF757575,
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
