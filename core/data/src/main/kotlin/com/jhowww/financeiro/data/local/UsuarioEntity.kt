package com.jhowww.financeiro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Dono dos dados. O app tem um usuário só, mas a tabela já existe para expansões futuras. */
@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
)
