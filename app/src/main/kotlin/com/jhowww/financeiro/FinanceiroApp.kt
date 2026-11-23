package com.jhowww.financeiro

import android.app.Application
import android.content.Context
import com.jhowww.financeiro.data.local.FinanceiroDatabase

class FinanceiroApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Injeção de dependência manual: um único lugar cria e guarda os objetos compartilhados. */
class AppContainer(context: Context) {
    val database: FinanceiroDatabase by lazy { FinanceiroDatabase.criar(context) }
}
