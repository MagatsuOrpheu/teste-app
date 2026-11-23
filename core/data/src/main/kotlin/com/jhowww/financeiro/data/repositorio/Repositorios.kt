package com.jhowww.financeiro.data.repositorio

import androidx.room.withTransaction
import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.data.local.entidade.GastoFixoVersaoEntity
import com.jhowww.financeiro.data.local.entidade.RendaPrincipalVersaoEntity
import com.jhowww.financeiro.data.local.entidade.ReservaMesEntity
import com.jhowww.financeiro.data.local.entidade.UsuarioEntity
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
import com.jhowww.financeiro.domain.modelo.VersaoValor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Dono dos dados. Na primeira vez cria o usuário e a configuração padrão (dados técnicos,
 * não registros financeiros). O banco financeiro continua vazio.
 */
class Sessao(private val db: FinanceiroDatabase) {
    private val trava = Mutex()
    @Volatile private var id: Long? = null

    suspend fun usuarioId(): Long = id ?: trava.withLock {
        id ?: db.withTransaction {
            db.usuarioDao().primeiro()?.id ?: run {
                val novo = db.usuarioDao().inserir(UsuarioEntity(nome = "Usuário"))
                db.configuracaoDao().salvar(Configuracao().paraEntidade(novo))
                novo
            }
        }.also { id = it }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun <T> doUsuario(consulta: (Long) -> Flow<T>): Flow<T> =
        flow { emit(usuarioId()) }.flatMapLatest(consulta)
}

class ConfiguracaoRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    fun observar(): Flow<Configuracao> =
        sessao.doUsuario { db.configuracaoDao().observar(it) }.map { it?.paraDominio() ?: Configuracao() }

    suspend fun salvar(configuracao: Configuracao) =
        db.configuracaoDao().salvar(configuracao.paraEntidade(sessao.usuarioId()))
}

sealed interface ResultadoExclusao {
    data object Excluida : ResultadoExclusao
    /** A categoria tem registros; é preciso escolher outra para recebê-los. */
    data class EmUso(val registros: Int) : ResultadoExclusao
}

class CategoriaRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    private val dao = db.categoriaDao()

    fun observar(): Flow<List<Categoria>> =
        sessao.doUsuario { dao.observarTodas(it) }.map { lista -> lista.map { it.paraDominio() } }

    suspend fun salvar(categoria: Categoria): Long {
        val e = categoria.paraEntidade(sessao.usuarioId())
        return if (categoria.id == 0L) dao.inserir(e) else e.id.also { dao.atualizar(e) }
    }

    /** Só exclui se ninguém usa a categoria. */
    suspend fun excluir(id: Long): ResultadoExclusao = db.withTransaction {
        val usos = dao.contarUsos(id)
        if (usos > 0) ResultadoExclusao.EmUso(usos) else ResultadoExclusao.Excluida.also { dao.excluir(id) }
    }

    suspend fun transferirEExcluir(origem: Long, destino: Long) = dao.transferirEExcluir(origem, destino)
}

class PagamentoRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    private val formas = db.formaPagamentoDao()
    private val cartoes = db.cartaoDao()

    fun observarFormas(): Flow<List<FormaPagamento>> =
        sessao.doUsuario { formas.observarTodas(it) }.map { l -> l.map { it.paraDominio() } }

    suspend fun salvarForma(forma: FormaPagamento): Long {
        val e = forma.paraEntidade(sessao.usuarioId())
        return if (forma.id == 0L) formas.inserir(e) else e.id.also { formas.atualizar(e) }
    }

    fun observarCartoes(): Flow<List<Cartao>> =
        sessao.doUsuario { cartoes.observarCartoes(it) }.map { l -> l.map { it.paraDominio() } }

    suspend fun salvarCartao(cartao: Cartao): Long {
        val e = cartao.paraEntidade(sessao.usuarioId())
        return if (cartao.id == 0L) cartoes.inserirCartao(e) else e.id.also { cartoes.atualizarCartao(e) }
    }

    suspend fun fatura(cartaoId: Long, competencia: Competencia): Fatura? =
        cartoes.fatura(cartaoId, competencia.chave)?.paraDominio()

    /** Devolve a fatura existente ou cria com as datas informadas (copiadas do cartão pelo motor). */
    suspend fun obterOuCriarFatura(nova: Fatura): Fatura = db.withTransaction {
        cartoes.fatura(nova.cartaoId, nova.competencia.chave)?.paraDominio()
            ?: nova.copy(id = cartoes.inserirFatura(nova.paraEntidade()))
    }

    fun observarFaturas(cartaoId: Long): Flow<List<Fatura>> =
        cartoes.observarFaturas(cartaoId).map { l -> l.map { it.paraDominio() } }

    fun observarPagamentos(faturaId: Long): Flow<List<PagamentoFatura>> =
        cartoes.observarPagamentos(faturaId).map { l -> l.map { it.paraDominio() } }

    fun observarPagamentosEntre(de: Competencia, ate: Competencia): Flow<List<PagamentoFatura>> =
        cartoes.observarPagamentosEntre(de.chave, ate.chave).map { l -> l.map { it.paraDominio() } }

    suspend fun registrarPagamento(pagamento: PagamentoFatura): Long {
        require(pagamento.valor > Dinheiro.ZERO) { "O pagamento precisa ser maior que zero" }
        return cartoes.inserirPagamento(pagamento.paraEntidade())
    }

    suspend fun excluirPagamento(id: Long) = cartoes.excluirPagamento(id)
}

class RendaRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    private val dao = db.rendaDao()

    fun observarRendas(): Flow<List<RendaPrincipal>> =
        sessao.doUsuario { dao.observarRendas(it) }.map { l -> l.map { it.paraDominio() } }

    /** Cria a renda principal já com o primeiro valor. */
    suspend fun criarRenda(renda: RendaPrincipal, valor: Dinheiro, desde: Competencia): Long {
        require(valor > Dinheiro.ZERO) { "O valor precisa ser maior que zero" }
        val usuario = sessao.usuarioId()
        return db.withTransaction { criarRendaEmTransacao(renda, valor, desde, usuario) }
    }

    private suspend fun criarRendaEmTransacao(renda: RendaPrincipal, valor: Dinheiro, desde: Competencia, usuario: Long): Long {
        val id = dao.inserirRenda(renda.paraEntidade(usuario))
        dao.salvarVersao(RendaPrincipalVersaoEntity(rendaId = id, vigenteDesde = desde.chave, valor = valor.centavos))
        return id
    }

    suspend fun atualizarRenda(renda: RendaPrincipal) = dao.atualizarRenda(renda.paraEntidade(sessao.usuarioId()))

    /** Alteração salarial: vale de [desde] em diante; meses anteriores mantêm o valor antigo. */
    suspend fun novoValor(rendaId: Long, valor: Dinheiro, desde: Competencia) {
        dao.salvarVersao(RendaPrincipalVersaoEntity(rendaId = rendaId, vigenteDesde = desde.chave, valor = valor.centavos))
    }

    fun observarVersoes(): Flow<Map<Long, List<VersaoValor>>> =
        dao.observarVersoes().map { l -> l.groupBy({ it.rendaId }, { it.paraDominio() }) }

    fun observarRecebimentos(de: Competencia, ate: Competencia): Flow<List<RecebimentoRenda>> =
        dao.observarRecebimentos(de.chave, ate.chave).map { l -> l.map { it.paraDominio() } }

    suspend fun confirmarRecebimento(recebimento: RecebimentoRenda): Long {
        require(recebimento.valorRecebido > Dinheiro.ZERO) { "O valor recebido precisa ser maior que zero" }
        return dao.confirmarRecebimento(recebimento.paraEntidade())
    }

    suspend fun desfazerRecebimento(rendaId: Long, competencia: Competencia) =
        dao.desfazerRecebimento(rendaId, competencia.chave)

    fun observarReceitas(de: Competencia, ate: Competencia): Flow<List<ReceitaAdicional>> =
        sessao.doUsuario { dao.observarReceitas(it, de.chave, ate.chave) }.map { l -> l.map { it.paraDominio() } }

    suspend fun salvarReceita(receita: ReceitaAdicional): Long {
        require(receita.valor > Dinheiro.ZERO) { "O valor precisa ser maior que zero" }
        val e = receita.paraEntidade(sessao.usuarioId())
        return if (receita.id == 0L) dao.inserirReceita(e) else e.id.also { dao.atualizarReceita(e) }
    }

    suspend fun excluirReceita(id: Long) = dao.excluirReceita(id)
}

class ReservaRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    private val dao = db.reservaDao()

    fun observarMetas(de: Competencia, ate: Competencia): Flow<List<ReservaMes>> =
        sessao.doUsuario { dao.observarMetas(it, de.chave, ate.chave) }.map { l -> l.map { it.paraDominio() } }

    /** Ajuste da meta só para um mês; os demais seguem o padrão da configuração. */
    suspend fun ajustarMeta(meta: ReservaMes) = dao.salvarMeta(
        ReservaMesEntity(usuarioId = sessao.usuarioId(), competencia = meta.competencia.chave, valorPlanejado = meta.valorPlanejado.centavos),
    )

    suspend fun voltarAoPadrao(competencia: Competencia) = dao.removerMeta(sessao.usuarioId(), competencia.chave)

    fun observarAportes(de: Competencia, ate: Competencia): Flow<List<AporteReserva>> =
        sessao.doUsuario { dao.observarAportes(it, de.chave, ate.chave) }.map { l -> l.map { it.paraDominio() } }

    suspend fun registrarAporte(aporte: AporteReserva): Long {
        require(aporte.valor > Dinheiro.ZERO) { "O aporte precisa ser maior que zero" }
        return dao.inserirAporte(aporte.paraEntidade(sessao.usuarioId()))
    }

    suspend fun excluirAporte(id: Long) = dao.excluirAporte(id)

    fun observarRetiradas(de: Competencia, ate: Competencia): Flow<List<RetiradaReserva>> =
        sessao.doUsuario { dao.observarRetiradas(it, de.chave, ate.chave) }.map { l -> l.map { it.paraDominio() } }

    suspend fun registrarRetirada(retirada: RetiradaReserva): Long {
        require(retirada.valor > Dinheiro.ZERO) { "A retirada precisa ser maior que zero" }
        return dao.inserirRetirada(retirada.paraEntidade(sessao.usuarioId()))
    }

    suspend fun excluirRetirada(id: Long) = dao.excluirRetirada(id)
}

class GastoFixoRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    private val dao = db.gastoFixoDao()

    fun observarGastos(): Flow<List<GastoFixo>> =
        sessao.doUsuario { dao.observarGastos(it) }.map { l -> l.map { it.paraDominio() } }

    suspend fun criar(gasto: GastoFixo, valor: Dinheiro): Long {
        require(valor > Dinheiro.ZERO) { "O valor precisa ser maior que zero" }
        require(gasto.intervaloMeses >= 1) { "Periodicidade inválida" }
        return dao.inserirComValor(gasto.paraEntidade(sessao.usuarioId()), valor.centavos)
    }

    suspend fun atualizar(gasto: GastoFixo) = dao.atualizarGasto(gasto.paraEntidade(sessao.usuarioId()))

    /** Novo valor a partir de [desde]; ocorrências e meses anteriores não mudam. */
    suspend fun novoValor(gastoFixoId: Long, valor: Dinheiro, desde: Competencia) {
        require(valor > Dinheiro.ZERO) { "O valor precisa ser maior que zero" }
        dao.salvarVersao(GastoFixoVersaoEntity(gastoFixoId = gastoFixoId, vigenteDesde = desde.chave, valor = valor.centavos))
    }

    suspend fun encerrar(gasto: GastoFixo, fim: Competencia) =
        dao.encerrar(gasto.paraEntidade(sessao.usuarioId()), fim.chave)

    suspend fun excluir(id: Long) = dao.excluirGasto(id)

    fun observarVersoes(): Flow<Map<Long, List<VersaoValor>>> =
        dao.observarVersoes().map { l -> l.groupBy({ it.gastoFixoId }, { it.paraDominio() }) }

    fun observarOcorrencias(de: Competencia, ate: Competencia): Flow<List<OcorrenciaFixo>> =
        dao.observarOcorrencias(de.chave, ate.chave).map { l -> l.map { it.paraDominio() } }

    suspend fun salvarOcorrencia(ocorrencia: OcorrenciaFixo): Long = dao.salvarOcorrencia(ocorrencia.paraEntidade())

    /** Volta a ocorrência a ser apenas virtual (desfaz pagamento, pulo ou exceção). */
    suspend fun descartarOcorrencia(gastoFixoId: Long, competencia: Competencia) =
        dao.excluirOcorrencia(gastoFixoId, competencia.chave)
}

class ParcelamentoRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    private val dao = db.parcelamentoDao()

    fun observarCompras(): Flow<List<CompraParcelada>> =
        sessao.doUsuario { dao.observarCompras(it) }.map { l -> l.map { it.paraDominio() } }

    /** As parcelas chegam prontas do motor financeiro (rateio e competências). */
    suspend fun criar(compra: CompraParcelada, parcelas: List<Parcela>): Long =
        dao.inserirCompraComParcelas(compra.paraEntidade(sessao.usuarioId()), parcelas.map { it.paraEntidade() })

    suspend fun substituir(compra: CompraParcelada, parcelas: List<Parcela>) =
        dao.substituirCompra(compra.paraEntidade(sessao.usuarioId()), parcelas.map { it.paraEntidade() })

    suspend fun excluir(id: Long) = dao.excluirCompra(id)

    fun observarParcelasEntre(de: Competencia, ate: Competencia): Flow<List<Parcela>> =
        dao.observarParcelasEntre(de.chave, ate.chave).map { l -> l.map { it.paraDominio() } }

    fun observarParcelasDaCompra(compraId: Long): Flow<List<Parcela>> =
        dao.observarParcelasDaCompra(compraId).map { l -> l.map { it.paraDominio() } }

    suspend fun atualizarParcela(parcela: Parcela) = dao.atualizarParcela(parcela.paraEntidade())
}

class DespesaRepositorio(private val db: FinanceiroDatabase, private val sessao: Sessao) {
    private val dao = db.despesaDao()

    fun observarEntre(de: Competencia, ate: Competencia): Flow<List<DespesaPontual>> =
        sessao.doUsuario { dao.observarEntre(it, de.chave, ate.chave) }.map { l -> l.map { it.paraDominio() } }

    fun observarDaFatura(faturaId: Long): Flow<List<DespesaPontual>> =
        dao.observarDaFatura(faturaId).map { l -> l.map { it.paraDominio() } }

    suspend fun salvar(despesa: DespesaPontual): Long {
        if (despesa.ehEstorno) {
            require(despesa.valor < Dinheiro.ZERO) { "Estorno precisa ter valor negativo" }
        } else {
            require(despesa.valor > Dinheiro.ZERO) { "O valor precisa ser maior que zero" }
        }
        val e = despesa.paraEntidade(sessao.usuarioId())
        return if (despesa.id == 0L) dao.inserir(e) else e.id.also { dao.atualizar(e) }
    }

    suspend fun excluir(despesa: DespesaPontual) = dao.excluir(despesa.paraEntidade(sessao.usuarioId()))
}
