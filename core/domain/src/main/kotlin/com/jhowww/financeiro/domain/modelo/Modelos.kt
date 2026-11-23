package com.jhowww.financeiro.domain.modelo

import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import java.time.LocalDate

/*
 * Modelos do domínio: o que o motor financeiro e as telas enxergam.
 * Não conhecem Room nem Android; os repositórios convertem de e para as tabelas.
 * id = 0 significa "ainda não gravado".
 */

// ---------------------------------------------------------------- configuração

enum class TipoReserva { FIXO, PERCENTUAL }

/**
 * @param valorReserva centavos quando [TipoReserva.FIXO]; pontos-base (1% = 100) quando PERCENTUAL,
 *   aplicado sobre a renda principal.
 * @param limiteComprometimentoBp acima deste comprometimento (em pontos-base) o app alerta.
 */
data class Configuracao(
    val tipoReserva: TipoReserva = TipoReserva.FIXO,
    val valorReserva: Long = 0,
    val limiteComprometimentoBp: Int = 9_000,
    val alertasAtivos: Boolean = true,
    val backupAutomatico: Boolean = false,
    val biometriaAtiva: Boolean = false,
)

data class Categoria(
    val id: Long = 0,
    val nome: String,
    val cor: Long,
    val icone: String? = null,
    val ativa: Boolean = true,
)

// ---------------------------------------------------------------- pagamento e cartão

enum class TipoPagamento {
    DINHEIRO, PIX, DEBITO, CREDITO;

    /** Crédito é pago pela fatura; os demais saem da conta na hora. */
    val viaFatura: Boolean get() = this == CREDITO
}

data class FormaPagamento(
    val id: Long = 0,
    val nome: String,
    val tipo: TipoPagamento,
    val ativa: Boolean = true,
)

data class Cartao(
    val id: Long = 0,
    val nome: String,
    val diaFechamento: Int,
    val diaVencimento: Int,
    val limite: Dinheiro? = null,
    val ativo: Boolean = true,
)

/** Fatura sem valor próprio: total, pago e restante são calculados a partir dos itens e pagamentos. */
data class Fatura(
    val id: Long = 0,
    val cartaoId: Long,
    /** Mês do vencimento. */
    val competencia: Competencia,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
)

data class PagamentoFatura(
    val id: Long = 0,
    val faturaId: Long,
    val data: LocalDate,
    val valor: Dinheiro,
)

// ---------------------------------------------------------------- renda

data class RendaPrincipal(
    val id: Long = 0,
    val descricao: String,
    val diaRecebimento: Int,
    val ativa: Boolean = true,
)

/** Valor que vale a partir de uma competência (renda principal ou gasto fixo). */
data class VersaoValor(
    val id: Long = 0,
    val vigenteDesde: Competencia,
    val valor: Dinheiro,
)

data class RecebimentoRenda(
    val id: Long = 0,
    val rendaId: Long,
    val competencia: Competencia,
    val valorRecebido: Dinheiro,
    val confirmadoEm: LocalDate,
)

enum class StatusReceita { A_RECEBER, RECEBIDA }

data class ReceitaAdicional(
    val id: Long = 0,
    val descricao: String,
    val valor: Dinheiro,
    /** Data prevista (a receber) ou data do recebimento. */
    val data: LocalDate,
    val status: StatusReceita,
    val recebidaEm: LocalDate? = null,
    val categoriaId: Long? = null,
    val observacao: String? = null,
) {
    /** Só conta no orçamento quando recebida, na competência do recebimento. */
    val competencia: Competencia get() = Competencia.de(recebidaEm ?: data)
}

// ---------------------------------------------------------------- reserva

data class ReservaMes(
    val competencia: Competencia,
    val valorPlanejado: Dinheiro,
)

data class AporteReserva(
    val id: Long = 0,
    val data: LocalDate,
    val valor: Dinheiro,
    val observacao: String? = null,
)

data class RetiradaReserva(
    val id: Long = 0,
    val data: LocalDate,
    val valor: Dinheiro,
    val motivo: String? = null,
)

// ---------------------------------------------------------------- gastos fixos

data class GastoFixo(
    val id: Long = 0,
    val nome: String,
    val categoriaId: Long,
    val formaPagamentoId: Long,
    val cartaoId: Long? = null,
    val diaVencimento: Int,
    /** 1 = mensal, 2 = bimestral, 3 = trimestral, 6 = semestral, 12 = anual, ou outro N. */
    val intervaloMeses: Int = 1,
    val inicio: Competencia,
    val fim: Competencia? = null,
    val debitoAutomatico: Boolean = false,
    val ativo: Boolean = true,
)

enum class StatusOcorrencia { REGISTRADA, PAGA, PULADA, NAO_DEBITADA }

/** Linha gravada só quando há um fato do usuário; o resto das ocorrências é virtual. */
data class OcorrenciaFixo(
    val id: Long = 0,
    val gastoFixoId: Long,
    val competencia: Competencia,
    val status: StatusOcorrencia,
    /** Valor diferente só neste mês; null = valor da versão vigente. */
    val valor: Dinheiro? = null,
    val dataPagamento: LocalDate? = null,
    val faturaId: Long? = null,
    val faturaManual: Boolean = false,
)

// ---------------------------------------------------------------- parcelamentos

data class CompraParcelada(
    val id: Long = 0,
    val descricao: String,
    val valorTotal: Dinheiro,
    val numeroParcelas: Int,
    val dataCompra: LocalDate,
    val primeiraCompetencia: Competencia,
    val categoriaId: Long,
    val formaPagamentoId: Long,
    val cartaoId: Long? = null,
    val observacao: String? = null,
)

data class Parcela(
    val id: Long = 0,
    val compraId: Long = 0,
    val numero: Int,
    val competencia: Competencia,
    val valor: Dinheiro,
    /** Pagamento manual (fora do cartão). No cartão, a parcela fica paga quando a fatura é quitada. */
    val pagaEm: LocalDate? = null,
    val faturaId: Long? = null,
    val faturaManual: Boolean = false,
)

// ---------------------------------------------------------------- despesas pontuais

data class DespesaPontual(
    val id: Long = 0,
    val descricao: String,
    /** Negativo apenas em estornos. */
    val valor: Dinheiro,
    val data: LocalDate,
    val competencia: Competencia = Competencia.de(data),
    val categoriaId: Long,
    val formaPagamentoId: Long,
    val cartaoId: Long? = null,
    val faturaId: Long? = null,
    val faturaManual: Boolean = false,
    val estornoDe: Long? = null,
    val observacao: String? = null,
) {
    val ehEstorno: Boolean get() = estornoDe != null
}
