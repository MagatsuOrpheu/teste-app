package com.jhowww.financeiro.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.data.repositorio.CategoriaRepositorio
import com.jhowww.financeiro.data.repositorio.DespesaRepositorio
import com.jhowww.financeiro.data.repositorio.Operacoes
import com.jhowww.financeiro.data.repositorio.PagamentoRepositorio
import com.jhowww.financeiro.data.repositorio.ParcelamentoRepositorio
import com.jhowww.financeiro.data.repositorio.RetratoRepositorio
import com.jhowww.financeiro.data.repositorio.Sessao
import com.jhowww.financeiro.domain.Competencia
import com.jhowww.financeiro.domain.Dinheiro
import com.jhowww.financeiro.domain.modelo.Cartao
import com.jhowww.financeiro.domain.modelo.Categoria
import com.jhowww.financeiro.domain.modelo.CompraParcelada
import com.jhowww.financeiro.domain.modelo.DespesaPontual
import com.jhowww.financeiro.domain.modelo.FormaPagamento
import com.jhowww.financeiro.domain.modelo.TipoPagamento
import com.jhowww.financeiro.domain.motor.FaturaChave
import com.jhowww.financeiro.domain.motor.MotorFinanceiro
import com.jhowww.financeiro.domain.motor.StatusFatura
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Gravações com regra de negócio e o retrato lido pelo motor. Banco em memória, dados só de teste. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OperacoesTest {
    private lateinit var db: FinanceiroDatabase
    private lateinit var sessao: Sessao
    private lateinit var pagamentos: PagamentoRepositorio
    private lateinit var despesas: DespesaRepositorio
    private lateinit var operacoes: Operacoes
    private val nov = Competencia.de(2026, 11)
    private val dez = Competencia.de(2026, 12)

    @Before
    fun abrir() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, FinanceiroDatabase::class.java).allowMainThreadQueries().build()
        sessao = Sessao(db)
        pagamentos = PagamentoRepositorio(db, sessao)
        despesas = DespesaRepositorio(db, sessao)
        operacoes = Operacoes(db, sessao, pagamentos, despesas, ParcelamentoRepositorio(db, sessao))
    }

    @After
    fun fechar() = db.close()

    private class Base(val categoria: Long, val credito: Long, val cartao: Long)

    private suspend fun base(): Base = Base(
        categoria = CategoriaRepositorio(db, sessao).salvar(Categoria(nome = "Mercado", cor = 0xFF44CF6E)),
        credito = pagamentos.salvarForma(FormaPagamento(nome = "Crédito", tipo = TipoPagamento.CREDITO)),
        cartao = pagamentos.salvarCartao(Cartao(nome = "Nubank", diaFechamento = 3, diaVencimento = 10)),
    )

    private fun compraNoCartao(b: Base, valor: Long, data: LocalDate) = DespesaPontual(
        descricao = "Compra", valor = Dinheiro(valor), data = data,
        categoriaId = b.categoria, formaPagamentoId = b.credito, cartaoId = b.cartao,
    )

    @Test
    fun retratoTrazTodasAsTabelas() = runBlocking {
        val b = base()
        operacoes.salvarDespesa(compraNoCartao(b, 5_000, LocalDate.of(2026, 11, 10)))
        val retrato = RetratoRepositorio(db, sessao).observar().first()
        assertEquals(listOf("Mercado"), retrato.categorias.map { it.nome })
        assertEquals(1, retrato.formas.size)
        assertEquals(1, retrato.dados.cartoes.size)
        assertEquals(1, retrato.dados.despesas.size)
        assertEquals(1, retrato.dados.faturas.size)
    }

    @Test
    fun compraNoCartaoFicaNaFaturaDoLancamento() = runBlocking {
        val b = base()
        operacoes.salvarDespesa(compraNoCartao(b, 5_000, LocalDate.of(2026, 11, 10)))
        val fatura = pagamentos.fatura(b.cartao, dez)!!
        assertEquals(fatura.id, despesas.observarEntre(nov, nov).first().single().faturaId)

        // Mudar os dias do cartão depois não move a compra já lançada.
        pagamentos.salvarCartao(Cartao(id = b.cartao, nome = "Nubank", diaFechamento = 20, diaVencimento = 28))
        val dados = RetratoRepositorio(db, sessao).observar().first().dados
        val motor = MotorFinanceiro(dados, LocalDate.of(2026, 11, 20))
        assertEquals(FaturaChave(b.cartao, dez), motor.itensDoMes(nov).single().fatura)
    }

    @Test
    fun estornoFicaNegativoELigadoAOriginal() = runBlocking {
        val b = base()
        val id = operacoes.salvarDespesa(compraNoCartao(b, 8_000, LocalDate.of(2026, 11, 10)))
        val original = despesas.observarEntre(nov, nov).first().single { it.id == id }
        operacoes.estornar(original, Dinheiro(3_000), LocalDate.of(2026, 12, 5))
        val estorno = despesas.observarEntre(dez, dez).first().single()
        assertEquals(Dinheiro(-3_000), estorno.valor)
        assertEquals(id, estorno.estornoDe)
        assertEquals(pagamentos.fatura(b.cartao, Competencia.de(2027, 1))!!.id, estorno.faturaId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun estornoMaiorQueACompraEhRecusado() = runBlocking {
        val b = base()
        val id = operacoes.salvarDespesa(compraNoCartao(b, 8_000, LocalDate.of(2026, 11, 10)))
        val original = despesas.observarEntre(nov, nov).first().single { it.id == id }
        operacoes.estornar(original, Dinheiro(9_000), LocalDate.of(2026, 12, 5))
        Unit
    }

    @Test
    fun parceladaNoCartaoCriaUmaFaturaPorParcela() = runBlocking {
        val b = base()
        val compra = CompraParcelada(
            descricao = "Celular", valorTotal = Dinheiro(90_000), numeroParcelas = 3, dataCompra = LocalDate.of(2026, 11, 10),
            primeiraCompetencia = nov, categoriaId = b.categoria, formaPagamentoId = b.credito, cartaoId = b.cartao,
        )
        operacoes.lancarCompraParcelada(compra)
        val dados = RetratoRepositorio(db, sessao).observar().first().dados
        assertEquals(listOf(dez, Competencia.de(2027, 1), Competencia.de(2027, 2)), dados.faturas.map { it.competencia }.sorted())
        assertTrue(dados.parcelas.all { it.faturaId != null })
        assertEquals(listOf(30_000L, 30_000L, 30_000L), dados.parcelas.map { it.valor.centavos })
    }

    @Test
    fun mudarPrimeiraParcelaDeslocaTodasERefazAsFaturas() = runBlocking {
        val b = base()
        val compra = CompraParcelada(
            descricao = "Fone", valorTotal = Dinheiro(30_000), numeroParcelas = 3, dataCompra = LocalDate.of(2026, 11, 10),
            primeiraCompetencia = nov, categoriaId = b.categoria, formaPagamentoId = b.credito, cartaoId = b.cartao,
        )
        val id = operacoes.lancarCompraParcelada(compra)
        val antes = RetratoRepositorio(db, sessao).observar().first().dados
        operacoes.mudarPrimeiraParcela(antes.compras.single(), antes.parcelas, dez)
        val depois = RetratoRepositorio(db, sessao).observar().first().dados
        assertEquals(dez, depois.compras.single { it.id == id }.primeiraCompetencia)
        assertEquals(listOf(dez, Competencia.de(2027, 1), Competencia.de(2027, 2)), depois.parcelas.sortedBy { it.numero }.map { it.competencia })
        val faturas = depois.faturas.associateBy { it.id }
        assertEquals(
            listOf(Competencia.de(2027, 1), Competencia.de(2027, 2), Competencia.de(2027, 3)),
            depois.parcelas.sortedBy { it.numero }.map { faturas.getValue(it.faturaId!!).competencia },
        )
    }

    @Test
    fun pagamentoParcialCriaAFaturaENaoMudaDespesas() = runBlocking {
        val b = base()
        operacoes.salvarDespesa(compraNoCartao(b, 50_000, LocalDate.of(2026, 10, 20)))
        operacoes.pagarFatura(FaturaChave(b.cartao, nov), Dinheiro(30_000), LocalDate.of(2026, 11, 5))
        val dados = RetratoRepositorio(db, sessao).observar().first().dados
        val motor = MotorFinanceiro(dados, LocalDate.of(2026, 11, 6))
        val fatura = motor.fatura(FaturaChave(b.cartao, nov))
        assertEquals(Dinheiro(20_000), fatura.restante)
        assertEquals(StatusFatura.PAGA_PARCIALMENTE, fatura.status)
        assertEquals(Dinheiro(50_000), motor.resumoMes(Competencia.de(2026, 10)).despesas)
    }

    @Test
    fun moverDespesaDeFaturaEVoltarAoAutomatico() = runBlocking {
        val b = base()
        val id = operacoes.salvarDespesa(compraNoCartao(b, 8_000, LocalDate.of(2026, 11, 10)))
        val original = despesas.observarEntre(nov, nov).first().single { it.id == id }
        operacoes.moverDespesa(original, FaturaChave(b.cartao, Competencia.de(2027, 1)))
        val movida = despesas.observarEntre(nov, nov).first().single()
        assertTrue(movida.faturaManual)
        assertEquals(pagamentos.fatura(b.cartao, Competencia.de(2027, 1))!!.id, movida.faturaId)
        assertEquals(nov, movida.competencia)

        operacoes.moverDespesa(movida, null)
        val devolvida = despesas.observarEntre(nov, nov).first().single()
        assertTrue(!devolvida.faturaManual)
        assertEquals(pagamentos.fatura(b.cartao, dez)!!.id, devolvida.faturaId)
    }
}
