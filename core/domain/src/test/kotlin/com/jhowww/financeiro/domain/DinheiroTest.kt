package com.jhowww.financeiro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DinheiroTest {

    @Test
    fun somaESubtracaoEmCentavos() {
        assertEquals(Dinheiro(30), Dinheiro(10) + Dinheiro(20))
        assertEquals(Dinheiro(-5), Dinheiro(10) - Dinheiro(15))
        assertEquals(Dinheiro(60_000), Dinheiro(20_000) * 3)
        assertEquals(Dinheiro(350), listOf(Dinheiro(100), Dinheiro(250)).soma())
        assertTrue(Dinheiro(-1).ehNegativo)
    }

    @Test
    fun formatacaoEmReais() {
        assertEquals("R$ 1.234,56", Dinheiro(123_456).formatar())
        assertEquals("-R$ 10,00", Dinheiro(-1_000).formatar())
        assertEquals("R$ 0,00", Dinheiro.ZERO.formatar())
    }

    @Test
    fun interpretacaoDoQueOUsuarioDigita() {
        assertEquals(Dinheiro(150_000), Dinheiro.interpretar("1500"))
        assertEquals(Dinheiro(150_000), Dinheiro.interpretar("1.500"))
        assertEquals(Dinheiro(150_090), Dinheiro.interpretar("1.500,90"))
        assertEquals(Dinheiro(150_090), Dinheiro.interpretar("1500,9"))
        assertEquals(Dinheiro(150_090), Dinheiro.interpretar("1500.90"))
        assertEquals(Dinheiro(2_000), Dinheiro.interpretar("R$ 20"))
        assertNull(Dinheiro.interpretar("abc"))
        assertNull(Dinheiro.interpretar(""))
        assertNull(Dinheiro.interpretar("-5"))
    }

    @Test(expected = ArithmeticException::class)
    fun estouroNaoPassaDespercebido() {
        Dinheiro(Long.MAX_VALUE) + Dinheiro(1)
    }
}
