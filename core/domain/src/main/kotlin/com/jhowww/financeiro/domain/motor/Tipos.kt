package com.jhowww.financeiro.domain.motor

import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import java.time.LocalDate

/** Identifica uma fatura mesmo antes de ela existir no banco: cartão + mês do vencimento. */
data class FaturaChave(val cartaoId: Long, val competencia: Competencia)

/** Prevista: ainda não aconteceu. Registrada: aconteceu, dinheiro ainda não saiu. Paga: saiu da conta. */
enum class EstadoDespesa { PREVISTA, REGISTRADA, PAGA }

enum class OrigemDespesa { FIXO, PARCELA, PONTUAL }

/**
 * Uma despesa da competência, venha de onde vier. É a única forma como as despesas
 * chegam aos indicadores, à fatura, às listas e aos gráficos: cada uma conta uma vez só.
 *
 * @param referenciaId id do gasto fixo, da parcela ou da despesa pontual, conforme [origem].
 * @param data vencimento (fixo), data da compra (parcela) ou data do lançamento (pontual).
 */
data class ItemDespesa(
    val origem: OrigemDespesa,
    val referenciaId: Long,
    val descricao: String,
    val valor: Dinheiro,
    val competencia: Competencia,
    val data: LocalDate,
    val categoriaId: Long,
    val estado: EstadoDespesa,
    val fatura: FaturaChave? = null,
    /** A fatura do item recebeu pagamento, mas ainda tem restante. */
    val faturaPagaParcialmente: Boolean = false,
    val ehEstorno: Boolean = false,
    val debitoAutomatico: Boolean = false,
    /** Gasto fixo com linha gravada (pago, valor do mês, não debitado, fatura corrigida). */
    val ocorrenciaGravada: Boolean = false,
    val compraId: Long? = null,
    val numeroParcela: Int? = null,
    val totalParcelas: Int? = null,
) {
    val viaCartao: Boolean get() = fatura != null
}

enum class StatusFatura { ABERTA, FECHADA, PAGA_PARCIALMENTE, PAGA, VENCIDA }

data class ResumoFatura(
    val chave: FaturaChave,
    /** null enquanto a fatura só existe no cálculo (ainda não foi gravada). */
    val id: Long?,
    val dataFechamento: LocalDate,
    val dataVencimento: LocalDate,
    val itens: List<ItemDespesa>,
    val total: Dinheiro,
    val pago: Dinheiro,
    val status: StatusFatura,
) {
    val restante: Dinheiro get() = total - pago
    val quitada: Boolean get() = status == StatusFatura.PAGA
}

enum class QualidadeEstimativa {
    /** Mediana de meses completos anteriores. */
    HISTORICO,
    /** Ritmo do próprio mês: pouco histórico, estimativa fraca. */
    RITMO_DO_MES,
    /** Sem base nenhuma (mês futuro sem histórico); valor zero. */
    SEM_DADOS,
    /** Mês já encerrado: não falta nada a estimar. */
    MES_ENCERRADO,
}

data class Estimativa(val valor: Dinheiro, val qualidade: QualidadeEstimativa)

/** Todos os indicadores de uma competência. Nada aqui é gravado: é sempre recalculado. */
data class ResumoMes(
    val competencia: Competencia,
    /** Renda principal do mês: confirmada quando houver recebimento, senão a prevista. */
    val rendaPrincipal: Dinheiro,
    val rendaPrincipalConfirmada: Dinheiro,
    val salarioAConfirmar: Boolean,
    val receitasRecebidas: Dinheiro,
    /** Só informativo: nunca entra no orçamento. */
    val receitasAReceber: Dinheiro,
    val rendaProjetada: Dinheiro,
    val rendaConfirmada: Dinheiro,
    val reservaPlanejada: Dinheiro,
    val orcamentoProjetado: Dinheiro,
    val orcamentoConfirmado: Dinheiro,
    val despesasFixas: Dinheiro,
    val despesasParceladas: Dinheiro,
    val despesasVariaveis: Dinheiro,
    /** D: total comprometido no mês, contado uma vez. */
    val despesas: Dinheiro,
    val despesasPagas: Dinheiro,
    /** D ÷ orçamento projetado, em pontos-base (60% = 6000); null se o orçamento for zero ou negativo. */
    val comprometimentoBp: Int?,
    val saldoDisponivel: Dinheiro,
    val saldoConfirmado: Dinheiro,
    val estimativaVariaveis: Estimativa,
    val saldoProjetado: Dinheiro,
    val aportesReserva: Dinheiro,
    val retiradasReserva: Dinheiro,
    val reservaRealizada: Dinheiro,
    val sobra: Dinheiro,
    /** Mês fora da base dos alertas (início de uso, sem lançamentos, salário não confirmado, atual ou futuro). */
    val incompleto: Boolean,
)

/** Um ponto da linha do saldo restante ao longo do mês. */
data class PontoSaldo(val dia: Int, val saldo: Dinheiro, val projetado: Boolean)

enum class TipoAlerta {
    SALARIO_A_CONFIRMAR,
    DEBITO_A_CONFIRMAR,
    FATURA_VENCIDA,
    RISCO_DE_ESTOURO,
    COMPROMETIMENTO_ALTO,
    RITMO_ACIMA,
    RITMO_SAUDAVEL,
    DADOS_INSUFICIENTES,
}

enum class Severidade { INFO, ATENCAO, CRITICO }

/** Todo alerta traz o motivo com números. [referenciaId] aponta o registro envolvido, se houver. */
data class Alerta(
    val tipo: TipoAlerta,
    val severidade: Severidade,
    val mensagem: String,
    val referenciaId: Long? = null,
)

sealed interface ResultadoValidacao {
    data object Valido : ResultadoValidacao
    data class Recusado(val motivo: String) : ResultadoValidacao
}
