package com.jhowww.financeiro.data.local.entidade

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Tabelas do banco (versão 2). Convenções:
 * - valores em centavos (Long); datas em dias desde 1970 (LocalDate.toEpochDay);
 *   competências em texto "AAAA-MM"; enums pelo nome.
 * - Nenhum total é gravado: tudo é calculado pelo motor a partir destas tabelas.
 * - RESTRICT impede apagar um registro que ainda tem dependentes (ex.: categoria em uso);
 *   CASCADE apaga filhos que não existem sem o pai (versões, ocorrências, parcelas).
 */

@Entity(tableName = "usuario")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
)

@Entity(
    tableName = "configuracao",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
)
data class ConfiguracaoEntity(
    @PrimaryKey val usuarioId: Long,
    val tipoReserva: String,
    val valorReserva: Long,
    val limiteComprometimentoBp: Int,
    val alertasAtivos: Boolean,
    val backupAutomatico: Boolean,
    val biometriaAtiva: Boolean,
)

@Entity(
    tableName = "categoria",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["usuarioId", "nome"], unique = true)],
)
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val nome: String,
    val cor: Long,
    val icone: String?,
    val ativa: Boolean,
)

@Entity(
    tableName = "forma_pagamento",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("usuarioId")],
)
data class FormaPagamentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val nome: String,
    val tipo: String,
    val ativa: Boolean,
)

@Entity(
    tableName = "cartao",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("usuarioId")],
)
data class CartaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val nome: String,
    val diaFechamento: Int,
    val diaVencimento: Int,
    val limite: Long?,
    val ativo: Boolean,
)

@Entity(
    tableName = "fatura",
    foreignKeys = [ForeignKey(CartaoEntity::class, ["id"], ["cartaoId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["cartaoId", "competencia"], unique = true)],
)
data class FaturaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cartaoId: Long,
    val competencia: String,
    val dataFechamento: Long,
    val dataVencimento: Long,
)

@Entity(
    tableName = "pagamento_fatura",
    foreignKeys = [ForeignKey(FaturaEntity::class, ["id"], ["faturaId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("faturaId")],
)
data class PagamentoFaturaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val faturaId: Long,
    val data: Long,
    val valor: Long,
)

@Entity(
    tableName = "renda_principal",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("usuarioId")],
)
data class RendaPrincipalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val descricao: String,
    val diaRecebimento: Int,
    val ativa: Boolean,
)

@Entity(
    tableName = "renda_principal_versao",
    foreignKeys = [ForeignKey(RendaPrincipalEntity::class, ["id"], ["rendaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["rendaId", "vigenteDesde"], unique = true)],
)
data class RendaPrincipalVersaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rendaId: Long,
    val vigenteDesde: String,
    val valor: Long,
)

@Entity(
    tableName = "recebimento_renda",
    foreignKeys = [ForeignKey(RendaPrincipalEntity::class, ["id"], ["rendaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["rendaId", "competencia"], unique = true)],
)
data class RecebimentoRendaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rendaId: Long,
    val competencia: String,
    val valorRecebido: Long,
    val confirmadoEm: Long,
)

@Entity(
    tableName = "receita_adicional",
    foreignKeys = [
        ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoriaEntity::class, ["id"], ["categoriaId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("usuarioId"), Index("categoriaId"), Index("competencia")],
)
data class ReceitaAdicionalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val descricao: String,
    val valor: Long,
    val data: Long,
    val status: String,
    val recebidaEm: Long?,
    /** Gravada para consulta por mês: mês do recebimento, ou da data prevista. */
    val competencia: String,
    val categoriaId: Long?,
    val observacao: String?,
)

@Entity(
    tableName = "reserva_mes",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["usuarioId", "competencia"], unique = true)],
)
data class ReservaMesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val competencia: String,
    val valorPlanejado: Long,
)

@Entity(
    tableName = "aporte_reserva",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("usuarioId"), Index("competencia")],
)
data class AporteReservaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val data: Long,
    val competencia: String,
    val valor: Long,
    val observacao: String?,
)

@Entity(
    tableName = "retirada_reserva",
    foreignKeys = [ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("usuarioId"), Index("competencia")],
)
data class RetiradaReservaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val data: Long,
    val competencia: String,
    val valor: Long,
    val motivo: String?,
)

@Entity(
    tableName = "gasto_fixo",
    foreignKeys = [
        ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoriaEntity::class, ["id"], ["categoriaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(FormaPagamentoEntity::class, ["id"], ["formaPagamentoId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CartaoEntity::class, ["id"], ["cartaoId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("usuarioId"), Index("categoriaId"), Index("formaPagamentoId"), Index("cartaoId")],
)
data class GastoFixoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val nome: String,
    val categoriaId: Long,
    val formaPagamentoId: Long,
    val cartaoId: Long?,
    val diaVencimento: Int,
    val intervaloMeses: Int,
    val inicio: String,
    val fim: String?,
    val debitoAutomatico: Boolean,
    val ativo: Boolean,
)

@Entity(
    tableName = "gasto_fixo_versao",
    foreignKeys = [ForeignKey(GastoFixoEntity::class, ["id"], ["gastoFixoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["gastoFixoId", "vigenteDesde"], unique = true)],
)
data class GastoFixoVersaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gastoFixoId: Long,
    val vigenteDesde: String,
    val valor: Long,
)

@Entity(
    tableName = "ocorrencia_fixo",
    foreignKeys = [
        ForeignKey(GastoFixoEntity::class, ["id"], ["gastoFixoId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FaturaEntity::class, ["id"], ["faturaId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["gastoFixoId", "competencia"], unique = true), Index("faturaId")],
)
data class OcorrenciaFixoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gastoFixoId: Long,
    val competencia: String,
    val status: String,
    val valor: Long?,
    val dataPagamento: Long?,
    val faturaId: Long?,
    val faturaManual: Boolean,
)

@Entity(
    tableName = "compra_parcelada",
    foreignKeys = [
        ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoriaEntity::class, ["id"], ["categoriaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(FormaPagamentoEntity::class, ["id"], ["formaPagamentoId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CartaoEntity::class, ["id"], ["cartaoId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("usuarioId"), Index("categoriaId"), Index("formaPagamentoId"), Index("cartaoId")],
)
data class CompraParceladaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val descricao: String,
    val valorTotal: Long,
    val numeroParcelas: Int,
    val dataCompra: Long,
    val primeiraCompetencia: String,
    val categoriaId: Long,
    val formaPagamentoId: Long,
    val cartaoId: Long?,
    val observacao: String?,
)

@Entity(
    tableName = "parcela",
    foreignKeys = [
        ForeignKey(CompraParceladaEntity::class, ["id"], ["compraId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FaturaEntity::class, ["id"], ["faturaId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index(value = ["compraId", "numero"], unique = true), Index("competencia"), Index("faturaId")],
)
data class ParcelaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val compraId: Long,
    val numero: Int,
    val competencia: String,
    val valor: Long,
    val pagaEm: Long?,
    val faturaId: Long?,
    val faturaManual: Boolean,
)

@Entity(
    tableName = "despesa_pontual",
    foreignKeys = [
        ForeignKey(UsuarioEntity::class, ["id"], ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoriaEntity::class, ["id"], ["categoriaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(FormaPagamentoEntity::class, ["id"], ["formaPagamentoId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CartaoEntity::class, ["id"], ["cartaoId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(FaturaEntity::class, ["id"], ["faturaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(DespesaPontualEntity::class, ["id"], ["estornoDe"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("usuarioId"), Index("categoriaId"), Index("formaPagamentoId"), Index("cartaoId"),
        Index("faturaId"), Index("estornoDe"), Index("competencia"),
    ],
)
data class DespesaPontualEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    val descricao: String,
    val valor: Long,
    val data: Long,
    val competencia: String,
    val categoriaId: Long,
    val formaPagamentoId: Long,
    val cartaoId: Long?,
    val faturaId: Long?,
    val faturaManual: Boolean,
    val estornoDe: Long?,
    val observacao: String?,
)
