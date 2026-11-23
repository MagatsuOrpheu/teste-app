package com.jhowww.financeiro.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.data.repositorio.CategoriaRepositorio
import com.jhowww.financeiro.data.repositorio.DespesaRepositorio
import com.jhowww.financeiro.data.repositorio.GastoFixoRepositorio
import com.jhowww.financeiro.data.repositorio.PagamentoRepositorio
import com.jhowww.financeiro.data.repositorio.ParcelamentoRepositorio
import com.jhowww.financeiro.data.repositorio.RendaRepositorio
import com.jhowww.financeiro.data.repositorio.ResultadoExclusao
import com.jhowww.financeiro.data.repositorio.Sessao
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.Categoria
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.Fatura
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.modelo.GastoFixo
import com.jhowww.financeiro.domain.modelo.OcorrenciaFixo
import com.jhowww.financeiro.domain.modelo.PagamentoFatura
import com.jhowww.financeiro.domain.modelo.Parcela
import com.jhowww.financeiro.domain.modelo.RendaPrincipal
import com.jhowww.financeiro.domain.modelo.StatusOcorrencia
import com.jhowww.financeiro.domain.modelo.TipoPagamento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Integridade do banco: chaves estrangeiras, unicidade e transações. Dados apenas de teste, em memória. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BancoTest {
    private lateinit var db: FinanceiroDatabase
    private lateinit var sessao: Sessao
    private val nov = Competencia.de(2026, 11)
    private val dia = LocalDate.of(2026, 11, 10)

    @Before
    fun abrir() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, FinanceiroDatabase::class.java).allowMainThreadQueries().build()
        sessao = Sessao(db)
    }

    @After
    fun fechar() = db.close()

    private suspend fun base(): Triple<Long, Long, Long> {
        val cat = CategoriaRepositorio(db, sessao).salvar(Categoria(nome = "Mercado", cor = 0xFF44CF6E))
        val pix = PagamentoRepositorio(db, sessao).salvarForma(FormaPagamento(nome = "Pix", tipo = TipoPagamento.PIX))
        val credito = PagamentoRepositorio(db, sessao).salvarForma(FormaPagamento(nome = "Crédito", tipo = TipoPagamento.CREDITO))
        return Triple(cat, pix, credito)
    }

    private fun despesa(cat: Long, forma: Long, valor: Long) =
        DespesaPontual(descricao = "Compra", valor = Dinheiro(valor), data = dia, categoriaId = cat, formaPagamentoId = forma)

    @Test
    fun usuarioEConfiguracaoSaoCriadosUmaVezSo() = runBlocking {
        val a = sessao.usuarioId()
        val b = Sessao(db).usuarioId()
        assertEquals(a, b)
        assertEquals(1, db.query("SELECT COUNT(*) FROM usuario", null).use { it.moveToFirst(); it.getInt(0) })
        assertEquals(1, db.query("SELECT COUNT(*) FROM configuracao", null).use { it.moveToFirst(); it.getInt(0) })
        // Nenhum dado financeiro é criado automaticamente.
        assertEquals(0, db.query("SELECT COUNT(*) FROM categoria", null).use { it.moveToFirst(); it.getInt(0) })
    }

    @Test
    fun categoriaEmUsoNaoPodeSerExcluida() = runBlocking {
        val (cat, pix, _) = base()
        DespesaRepositorio(db, sessao).salvar(despesa(cat, pix, 5_000))
        val repo = CategoriaRepositorio(db, sessao)
        assertEquals(ResultadoExclusao.EmUso(1), repo.excluir(cat))
        try {
            db.categoriaDao().excluir(cat)
            fail("A chave estrangeira deveria impedir a exclusão")
        } catch (e: SQLiteConstraintException) {
            // esperado
        }
    }

    @Test
    fun transferenciaMoveRegistrosEExcluiAOrigem() = runBlocking {
        val (cat, pix, _) = base()
        val repo = CategoriaRepositorio(db, sessao)
        val destino = repo.salvar(Categoria(nome = "Alimentação", cor = 0xFFE9973F))
        DespesaRepositorio(db, sessao).salvar(despesa(cat, pix, 5_000))
        repo.transferirEExcluir(cat, destino)
        val categorias = repo.observar().first()
        assertEquals(listOf("Alimentação"), categorias.map { it.nome })
        val despesas = DespesaRepositorio(db, sessao).observarEntre(nov, nov).first()
        assertEquals(destino, despesas.single().categoriaId)
    }

    @Test
    fun ocorrenciaDeGastoFixoEhUnicaPorMes() = runBlocking {
        val (cat, pix, _) = base()
        val repo = GastoFixoRepositorio(db, sessao)
        val id = repo.criar(
            GastoFixo(nome = "Internet", categoriaId = cat, formaPagamentoId = pix, diaVencimento = 10, inicio = nov),
            Dinheiro(10_000),
        )
        repo.salvarOcorrencia(OcorrenciaFixo(gastoFixoId = id, competencia = nov, status = StatusOcorrencia.REGISTRADA))
        repo.salvarOcorrencia(OcorrenciaFixo(gastoFixoId = id, competencia = nov, status = StatusOcorrencia.PAGA, dataPagamento = dia))
        val ocorrencias = repo.observarOcorrencias(nov, nov).first()
        assertEquals(1, ocorrencias.size)
        assertEquals(StatusOcorrencia.PAGA, ocorrencias.single().status)
        assertEquals(listOf(Dinheiro(10_000)), repo.observarVersoes().first().getValue(id).map { it.valor })
    }

    @Test
    fun encerrarGastoFixoMantemSoOcorrenciasPagasDepoisDoFim() = runBlocking {
        val (cat, pix, _) = base()
        val repo = GastoFixoRepositorio(db, sessao)
        val id = repo.criar(
            GastoFixo(nome = "Academia", categoriaId = cat, formaPagamentoId = pix, diaVencimento = 5, inicio = nov),
            Dinheiro(9_000),
        )
        val dez = nov.proxima()
        val jan = dez.proxima()
        repo.salvarOcorrencia(OcorrenciaFixo(gastoFixoId = id, competencia = dez, status = StatusOcorrencia.PAGA))
        repo.salvarOcorrencia(OcorrenciaFixo(gastoFixoId = id, competencia = jan, status = StatusOcorrencia.PULADA))
        val gasto = repo.observarGastos().first().single()
        repo.encerrar(gasto, nov)
        val restantes = repo.observarOcorrencias(nov, jan).first()
        assertEquals(listOf(dez), restantes.map { it.competencia })
        assertEquals(nov, repo.observarGastos().first().single().fim)
    }

    @Test
    fun novoValorNaMesmaCompetenciaSubstituiOAnterior() = runBlocking {
        val repo = RendaRepositorio(db, sessao)
        val id = repo.criarRenda(RendaPrincipal(descricao = "Salário", diaRecebimento = 5), Dinheiro(330_000), nov)
        repo.novoValor(id, Dinheiro(350_000), nov.proxima())
        repo.novoValor(id, Dinheiro(360_000), nov.proxima())
        val versoes = repo.observarVersoes().first().getValue(id)
        assertEquals(listOf(Dinheiro(330_000), Dinheiro(360_000)), versoes.map { it.valor })
    }

    @Test
    fun compraParceladaEntraInteiraEApagaEmCascata() = runBlocking {
        val (cat, _, credito) = base()
        val repo = ParcelamentoRepositorio(db, sessao)
        val compra = CompraParcelada(
            descricao = "Celular", valorTotal = Dinheiro(120_000), numeroParcelas = 3, dataCompra = dia,
            primeiraCompetencia = nov, categoriaId = cat, formaPagamentoId = credito,
        )
        val parcelas = (1..3).map { Parcela(numero = it, competencia = nov.mais(it - 1L), valor = Dinheiro(40_000)) }
        val id = repo.criar(compra, parcelas)
        assertEquals(3, repo.observarParcelasDaCompra(id).first().size)
        assertEquals(1, repo.observarParcelasEntre(nov.proxima(), nov.proxima()).first().size)
        try {
            repo.criar(compra, parcelas.take(2))
            fail("Quantidade de parcelas diferente deveria ser recusada")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
        assertEquals(1, repo.observarCompras().first().size)
        repo.excluir(id)
        assertEquals(0, repo.observarParcelasEntre(nov, nov.mais(2)).first().size)
    }

    @Test
    fun faturaEhUnicaPorCartaoEMesESomaPagamentos() = runBlocking {
        val pagamentos = PagamentoRepositorio(db, sessao)
        val cartao = pagamentos.salvarCartao(Cartao(nome = "Nubank", diaFechamento = 3, diaVencimento = 10))
        val nova = Fatura(cartaoId = cartao, competencia = nov, dataFechamento = LocalDate.of(2026, 11, 3), dataVencimento = LocalDate.of(2026, 11, 10))
        val f1 = pagamentos.obterOuCriarFatura(nova)
        val f2 = pagamentos.obterOuCriarFatura(nova)
        assertEquals(f1.id, f2.id)
        pagamentos.registrarPagamento(PagamentoFatura(faturaId = f1.id, data = dia, valor = Dinheiro(30_000)))
        pagamentos.registrarPagamento(PagamentoFatura(faturaId = f1.id, data = dia, valor = Dinheiro(20_000)))
        assertEquals(50_000L, db.cartaoDao().totalPago(f1.id))
    }

    @Test
    fun estornoExigeValorNegativoEFicaLigadoAOriginal() = runBlocking {
        val (cat, _, credito) = base()
        val repo = DespesaRepositorio(db, sessao)
        val original = repo.salvar(despesa(cat, credito, 8_000))
        try {
            repo.salvar(despesa(cat, credito, 8_000).copy(estornoDe = original))
            fail("Estorno positivo deveria ser recusado")
        } catch (e: IllegalArgumentException) {
            // esperado
        }
        repo.salvar(despesa(cat, credito, -8_000).copy(estornoDe = original))
        assertEquals(1, db.despesaDao().estornosDe(original).size)
        val total = repo.observarEntre(nov, nov).first().sumOf { it.valor.centavos }
        assertEquals(0L, total)
        assertTrue(repo.observarEntre(nov, nov).first().any { it.ehEstorno })
    }
}
