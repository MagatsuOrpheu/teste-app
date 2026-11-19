package com.jhowww.meufinanceiro.data

import androidx.room.withTransaction
import com.jhowww.meufinanceiro.data.local.AppDatabase
import com.jhowww.meufinanceiro.data.local.CategoryEntity
import com.jhowww.meufinanceiro.data.local.RecurringEntity
import com.jhowww.meufinanceiro.data.local.RecurringPaidEntity
import com.jhowww.meufinanceiro.data.local.TransactionEntity
import com.jhowww.meufinanceiro.domain.Dates
import com.jhowww.meufinanceiro.domain.model.Category
import com.jhowww.meufinanceiro.domain.model.EntryType
import com.jhowww.meufinanceiro.domain.model.RecurringItem
import com.jhowww.meufinanceiro.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class FinanceRepository(private val db: AppDatabase) {
    private val categories = db.categoryDao()
    private val recurring = db.recurringDao()
    private val transactions = db.transactionDao()

    // ---------- Categorias ----------
    fun observeCategories(): Flow<List<Category>> =
        categories.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun saveCategory(c: Category) {
        val e = CategoryEntity(c.id, c.name.trim(), c.color, c.monthlyLimitCents)
        if (c.id == 0L) categories.insert(e) else categories.update(e)
    }

    suspend fun deleteCategory(id: Long) = db.withTransaction {
        transactions.clearCategory(id)
        recurring.clearCategory(id)
        categories.delete(id)
    }

    // ---------- Fixos (recorrentes) ----------
    fun observeRecurring(): Flow<List<RecurringItem>> =
        recurring.observeAll().map { list -> list.map { it.toDomain() } }

    fun observePaid(month: YearMonth): Flow<Set<Long>> =
        recurring.observePaid(Dates.monthKey(month)).map { it.toSet() }

    suspend fun saveRecurring(r: RecurringItem) {
        val e = r.toEntity()
        if (r.id == 0L) recurring.insert(e) else recurring.update(e)
    }

    /**
     * Edita um fixo a partir de [month]. Se o valor mudou e o item já existia em meses anteriores,
     * o item antigo é encerrado no mês anterior e um novo começa agora — assim o histórico não muda.
     */
    suspend fun updateRecurringFrom(old: RecurringItem, new: RecurringItem, month: YearMonth) = db.withTransaction {
        val valueChanged = old.amountCents != new.amountCents || old.type != new.type
        if (valueChanged && old.startMonth.isBefore(month)) {
            recurring.setEndMonth(old.id, Dates.monthKey(month.minusMonths(1)))
            val newId = recurring.insert(new.copy(id = 0, startMonth = month, endMonth = old.endMonth).toEntity())
            // Mantém a marcação de "pago" deste mês e dos seguintes no item novo
            val paidNow = recurring.paidMonthsFor(old.id).filter { !Dates.parseMonthKey(it).isBefore(month) }
            paidNow.forEach {
                recurring.unmarkPaid(RecurringPaidEntity(old.id, it))
                recurring.markPaid(RecurringPaidEntity(newId, it))
            }
        } else {
            recurring.update(new.copy(id = old.id, startMonth = old.startMonth, endMonth = old.endMonth).toEntity())
        }
    }

    /**
     * Remove o item a partir do mês informado, preservando o histórico dos meses anteriores.
     * Se ele começou nesse mês (ou depois), é apagado de vez.
     */
    suspend fun removeRecurringFrom(item: RecurringItem, month: YearMonth) = db.withTransaction {
        if (!item.startMonth.isBefore(month)) {
            recurring.deletePaidFor(item.id)
            recurring.delete(item.id)
        } else {
            recurring.setEndMonth(item.id, Dates.monthKey(month.minusMonths(1)))
        }
    }

    suspend fun setPaid(recurringId: Long, month: YearMonth, paid: Boolean) {
        val e = RecurringPaidEntity(recurringId, Dates.monthKey(month))
        if (paid) recurring.markPaid(e) else recurring.unmarkPaid(e)
    }

    // ---------- Lançamentos ----------
    fun observeTransactions(month: YearMonth): Flow<List<Transaction>> =
        transactions.observeByMonth(Dates.monthKey(month)).map { list -> list.map { it.toDomain() } }

    fun observeTransactionsBetween(from: YearMonth, to: YearMonth): Flow<List<Transaction>> =
        transactions.observeBetween(Dates.monthKey(from), Dates.monthKey(to))
            .map { list -> list.map { it.toDomain() } }

    suspend fun saveTransaction(t: Transaction) {
        val e = TransactionEntity(
            id = t.id,
            description = t.description.trim(),
            amountCents = t.amountCents,
            type = t.type.name,
            categoryId = t.categoryId,
            epochDay = t.date.toEpochDay(),
            month = Dates.monthKey(YearMonth.from(t.date)),
        )
        if (t.id == 0L) transactions.insert(e) else transactions.update(e)
    }

    suspend fun deleteTransaction(id: Long) = transactions.delete(id)
}

// ---------- Mapeamentos ----------
private fun CategoryEntity.toDomain() = Category(id, name, color, monthlyLimitCents)

private fun RecurringEntity.toDomain() = RecurringItem(
    id = id,
    name = name,
    amountCents = amountCents,
    type = EntryType.valueOf(type),
    categoryId = categoryId,
    dayOfMonth = dayOfMonth,
    startMonth = Dates.parseMonthKey(startMonth),
    endMonth = endMonth?.let { Dates.parseMonthKey(it) },
)

private fun RecurringItem.toEntity() = RecurringEntity(
    id = id,
    name = name.trim(),
    amountCents = amountCents,
    type = type.name,
    categoryId = categoryId,
    dayOfMonth = dayOfMonth,
    startMonth = Dates.monthKey(startMonth),
    endMonth = endMonth?.let { Dates.monthKey(it) },
)

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    description = description,
    amountCents = amountCents,
    type = EntryType.valueOf(type),
    categoryId = categoryId,
    date = LocalDate.ofEpochDay(epochDay),
)
