package com.jhowww.meufinanceiro

import android.app.Application
import android.content.Context
import com.jhowww.meufinanceiro.data.FinanceRepository
import com.jhowww.meufinanceiro.data.local.AppDatabase

/** Injeção de dependência manual (simples, sem Hilt). */
class MeuFinanceiroApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val prefs: AppPrefs by lazy { AppPrefs(this) }
}

class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("meu_financeiro_prefs", Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboarding_done", false)
        set(value) {
            sp.edit().putBoolean("onboarding_done", value).apply()
        }
}
