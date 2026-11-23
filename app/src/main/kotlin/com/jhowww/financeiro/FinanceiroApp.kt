package com.jhowww.financeiro

import android.app.Application
import android.content.Context
import com.jhowww.financeiro.data.local.FinanceiroDatabase
import com.jhowww.financeiro.data.repositorio.CategoriaRepositorio
import com.jhowww.financeiro.data.repositorio.ConfiguracaoRepositorio
import com.jhowww.financeiro.data.repositorio.DespesaRepositorio
import com.jhowww.financeiro.data.repositorio.GastoFixoRepositorio
import com.jhowww.financeiro.data.repositorio.PagamentoRepositorio
import com.jhowww.financeiro.data.repositorio.ParcelamentoRepositorio
import com.jhowww.financeiro.data.repositorio.RendaRepositorio
import com.jhowww.financeiro.data.repositorio.ReservaRepositorio
import com.jhowww.financeiro.data.repositorio.Sessao

class FinanceiroApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Injeção de dependência manual: um único lugar cria e guarda os objetos compartilhados. */
class AppContainer(context: Context) {
    val database: FinanceiroDatabase by lazy { FinanceiroDatabase.criar(context) }
    val sessao: Sessao by lazy { Sessao(database) }

    val configuracao by lazy { ConfiguracaoRepositorio(database, sessao) }
    val categorias by lazy { CategoriaRepositorio(database, sessao) }
    val pagamentos by lazy { PagamentoRepositorio(database, sessao) }
    val rendas by lazy { RendaRepositorio(database, sessao) }
    val reserva by lazy { ReservaRepositorio(database, sessao) }
    val gastosFixos by lazy { GastoFixoRepositorio(database, sessao) }
    val parcelamentos by lazy { ParcelamentoRepositorio(database, sessao) }
    val despesas by lazy { DespesaRepositorio(database, sessao) }
}
