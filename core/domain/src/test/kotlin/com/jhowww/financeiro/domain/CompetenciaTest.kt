package com.jhowww.financeiro.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CompetenciaTest {

    @Test
    fun vencimentoNoDia31SeAjustaAoMes() {
        assertEquals(LocalDate.of(2026, 2, 28), Competencia.de(2026, 2).diaEfetivo(31))
        assertEquals(LocalDate.of(2028, 2, 29), Competencia.de(2028, 2).diaEfetivo(31))
        assertEquals(LocalDate.of(2026, 4, 30), Competencia.de(2026, 4).diaEfetivo(31))
        assertEquals(LocalDate.of(2026, 1, 31), Competencia.de(2026, 1).diaEfetivo(31))
    }

    @Test
    fun navegacaoEntreMeses() {
        val nov = Competencia.de(2026, 11)
        assertEquals(Competencia.de(2027, 1), nov.mais(2))
        assertEquals(Competencia.de(2026, 10), nov.anterior())
        assertEquals(14L, Competencia.de(2028, 1).mesesDesde(nov))
        assertEquals("2026-11", nov.chave)
        assertEquals(nov, Competencia.interpretar("2026-11"))
        assertEquals(nov, Competencia.de(LocalDate.of(2026, 11, 23)))
    }
}
