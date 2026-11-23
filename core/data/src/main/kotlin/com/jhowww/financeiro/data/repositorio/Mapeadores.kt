package com.jhowww.financeiro.data.repositorio

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
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.AporteReserva
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.Categoria
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.Configuracao
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.Fatura
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.modelo.GastoFixo
import com.jhowww.financeiro.domain.modelo.OcorrenciaFixo
import com.jhowww.financeiro.domain.modelo.PagamentoFatura
import com.jhowww.financeiro.domain.modelo.Parcela
import com.jhowww.financeiro.domain.modelo.ReceitaAdicional
import com.jhowww.financeiro.domain.modelo.RecebimentoRenda
import com.jhowww.financeiro.domain.modelo.RendaPrincipal
import com.jhowww.financeiro.domain.modelo.ReservaMes
import com.jhowww.financeiro.domain.modelo.RetiradaReserva
import com.jhowww.financeiro.domain.modelo.StatusOcorrencia
import com.jhowww.financeiro.domain.modelo.StatusReceita
import com.jhowww.financeiro.domain.modelo.TipoPagamento
import com.jhowww.financeiro.domain.modelo.TipoReserva
import com.jhowww.financeiro.domain.modelo.VersaoValor
import java.time.LocalDate

// Conversões entre tabelas (tipos primitivos) e modelos do domínio.

internal fun LocalDate.paraBanco(): Long = toEpochDay()
internal fun Long.paraData(): LocalDate = LocalDate.ofEpochDay(this)
internal fun String.paraCompetencia(): Competencia = Competencia.interpretar(this)

internal fun ConfiguracaoEntity.paraDominio() = Configuracao(
    tipoReserva = TipoReserva.valueOf(tipoReserva),
    valorReserva = valorReserva,
    limiteComprometimentoBp = limiteComprometimentoBp,
    alertasAtivos = alertasAtivos,
    backupAutomatico = backupAutomatico,
    biometriaAtiva = biometriaAtiva,
)

internal fun Configuracao.paraEntidade(usuarioId: Long) = ConfiguracaoEntity(
    usuarioId = usuarioId,
    tipoReserva = tipoReserva.name,
    valorReserva = valorReserva,
    limiteComprometimentoBp = limiteComprometimentoBp,
    alertasAtivos = alertasAtivos,
    backupAutomatico = backupAutomatico,
    biometriaAtiva = biometriaAtiva,
)

internal fun CategoriaEntity.paraDominio() = Categoria(id, nome, cor, icone, ativa)
internal fun Categoria.paraEntidade(usuarioId: Long) =
    CategoriaEntity(id, usuarioId, nome.trim(), cor, icone, ativa)

internal fun FormaPagamentoEntity.paraDominio() = FormaPagamento(id, nome, TipoPagamento.valueOf(tipo), ativa)
internal fun FormaPagamento.paraEntidade(usuarioId: Long) =
    FormaPagamentoEntity(id, usuarioId, nome.trim(), tipo.name, ativa)

internal fun CartaoEntity.paraDominio() =
    Cartao(id, nome, diaFechamento, diaVencimento, limite?.let(::Dinheiro), ativo)
internal fun Cartao.paraEntidade(usuarioId: Long) =
    CartaoEntity(id, usuarioId, nome.trim(), diaFechamento, diaVencimento, limite?.centavos, ativo)

internal fun FaturaEntity.paraDominio() = Fatura(
    id, cartaoId, competencia.paraCompetencia(), dataFechamento.paraData(), dataVencimento.paraData(),
)
internal fun Fatura.paraEntidade() = FaturaEntity(
    id, cartaoId, competencia.chave, dataFechamento.paraBanco(), dataVencimento.paraBanco(),
)

internal fun PagamentoFaturaEntity.paraDominio() = PagamentoFatura(id, faturaId, data.paraData(), Dinheiro(valor))
internal fun PagamentoFatura.paraEntidade() = PagamentoFaturaEntity(id, faturaId, data.paraBanco(), valor.centavos)

internal fun RendaPrincipalEntity.paraDominio() = RendaPrincipal(id, descricao, diaRecebimento, ativa)
internal fun RendaPrincipal.paraEntidade(usuarioId: Long) =
    RendaPrincipalEntity(id, usuarioId, descricao.trim(), diaRecebimento, ativa)

internal fun RendaPrincipalVersaoEntity.paraDominio() =
    VersaoValor(id, vigenteDesde.paraCompetencia(), Dinheiro(valor))

internal fun GastoFixoVersaoEntity.paraDominio() =
    VersaoValor(id, vigenteDesde.paraCompetencia(), Dinheiro(valor))

internal fun RecebimentoRendaEntity.paraDominio() = RecebimentoRenda(
    id, rendaId, competencia.paraCompetencia(), Dinheiro(valorRecebido), confirmadoEm.paraData(),
)
internal fun RecebimentoRenda.paraEntidade() = RecebimentoRendaEntity(
    id, rendaId, competencia.chave, valorRecebido.centavos, confirmadoEm.paraBanco(),
)

internal fun ReceitaAdicionalEntity.paraDominio() = ReceitaAdicional(
    id = id,
    descricao = descricao,
    valor = Dinheiro(valor),
    data = data.paraData(),
    status = StatusReceita.valueOf(status),
    recebidaEm = recebidaEm?.paraData(),
    categoriaId = categoriaId,
    observacao = observacao,
)
internal fun ReceitaAdicional.paraEntidade(usuarioId: Long) = ReceitaAdicionalEntity(
    id = id,
    usuarioId = usuarioId,
    descricao = descricao.trim(),
    valor = valor.centavos,
    data = data.paraBanco(),
    status = status.name,
    recebidaEm = recebidaEm?.paraBanco(),
    competencia = competencia.chave,
    categoriaId = categoriaId,
    observacao = observacao,
)

internal fun ReservaMesEntity.paraDominio() = ReservaMes(competencia.paraCompetencia(), Dinheiro(valorPlanejado))

internal fun AporteReservaEntity.paraDominio() = AporteReserva(id, data.paraData(), Dinheiro(valor), observacao)
internal fun AporteReserva.paraEntidade(usuarioId: Long) = AporteReservaEntity(
    id, usuarioId, data.paraBanco(), Competencia.de(data).chave, valor.centavos, observacao,
)

internal fun RetiradaReservaEntity.paraDominio() = RetiradaReserva(id, data.paraData(), Dinheiro(valor), motivo)
internal fun RetiradaReserva.paraEntidade(usuarioId: Long) = RetiradaReservaEntity(
    id, usuarioId, data.paraBanco(), Competencia.de(data).chave, valor.centavos, motivo,
)

internal fun GastoFixoEntity.paraDominio() = GastoFixo(
    id = id,
    nome = nome,
    categoriaId = categoriaId,
    formaPagamentoId = formaPagamentoId,
    cartaoId = cartaoId,
    diaVencimento = diaVencimento,
    intervaloMeses = intervaloMeses,
    inicio = inicio.paraCompetencia(),
    fim = fim?.paraCompetencia(),
    debitoAutomatico = debitoAutomatico,
    ativo = ativo,
)
internal fun GastoFixo.paraEntidade(usuarioId: Long) = GastoFixoEntity(
    id = id,
    usuarioId = usuarioId,
    nome = nome.trim(),
    categoriaId = categoriaId,
    formaPagamentoId = formaPagamentoId,
    cartaoId = cartaoId,
    diaVencimento = diaVencimento,
    intervaloMeses = intervaloMeses,
    inicio = inicio.chave,
    fim = fim?.chave,
    debitoAutomatico = debitoAutomatico,
    ativo = ativo,
)

internal fun OcorrenciaFixoEntity.paraDominio() = OcorrenciaFixo(
    id = id,
    gastoFixoId = gastoFixoId,
    competencia = competencia.paraCompetencia(),
    status = StatusOcorrencia.valueOf(status),
    valor = valor?.let(::Dinheiro),
    dataPagamento = dataPagamento?.paraData(),
    faturaId = faturaId,
    faturaManual = faturaManual,
)
internal fun OcorrenciaFixo.paraEntidade() = OcorrenciaFixoEntity(
    id = id,
    gastoFixoId = gastoFixoId,
    competencia = competencia.chave,
    status = status.name,
    valor = valor?.centavos,
    dataPagamento = dataPagamento?.paraBanco(),
    faturaId = faturaId,
    faturaManual = faturaManual,
)

internal fun CompraParceladaEntity.paraDominio() = CompraParcelada(
    id = id,
    descricao = descricao,
    valorTotal = Dinheiro(valorTotal),
    numeroParcelas = numeroParcelas,
    dataCompra = dataCompra.paraData(),
    primeiraCompetencia = primeiraCompetencia.paraCompetencia(),
    categoriaId = categoriaId,
    formaPagamentoId = formaPagamentoId,
    cartaoId = cartaoId,
    observacao = observacao,
)
internal fun CompraParcelada.paraEntidade(usuarioId: Long) = CompraParceladaEntity(
    id = id,
    usuarioId = usuarioId,
    descricao = descricao.trim(),
    valorTotal = valorTotal.centavos,
    numeroParcelas = numeroParcelas,
    dataCompra = dataCompra.paraBanco(),
    primeiraCompetencia = primeiraCompetencia.chave,
    categoriaId = categoriaId,
    formaPagamentoId = formaPagamentoId,
    cartaoId = cartaoId,
    observacao = observacao,
)

internal fun ParcelaEntity.paraDominio() = Parcela(
    id, compraId, numero, competencia.paraCompetencia(), Dinheiro(valor), pagaEm?.paraData(), faturaId, faturaManual,
)
internal fun Parcela.paraEntidade() = ParcelaEntity(
    id, compraId, numero, competencia.chave, valor.centavos, pagaEm?.paraBanco(), faturaId, faturaManual,
)

internal fun DespesaPontualEntity.paraDominio() = DespesaPontual(
    id = id,
    descricao = descricao,
    valor = Dinheiro(valor),
    data = data.paraData(),
    competencia = competencia.paraCompetencia(),
    categoriaId = categoriaId,
    formaPagamentoId = formaPagamentoId,
    cartaoId = cartaoId,
    faturaId = faturaId,
    faturaManual = faturaManual,
    estornoDe = estornoDe,
    observacao = observacao,
)
internal fun DespesaPontual.paraEntidade(usuarioId: Long) = DespesaPontualEntity(
    id = id,
    usuarioId = usuarioId,
    descricao = descricao.trim(),
    valor = valor.centavos,
    data = data.paraBanco(),
    competencia = competencia.chave,
    categoriaId = categoriaId,
    formaPagamentoId = formaPagamentoId,
    cartaoId = cartaoId,
    faturaId = faturaId,
    faturaManual = faturaManual,
    estornoDe = estornoDe,
    observacao = observacao,
)
