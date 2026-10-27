package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.CategoryTotal
import com.example.expensetracker.data.database.TransactionDao
import com.example.expensetracker.data.database.TransactionEntity
import com.example.expensetracker.data.database.toDomain
import com.example.expensetracker.data.network.FirestoreDataSource
import com.example.expensetracker.domain.model.Transaction
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId

/** Единственная точка доступа к данным об операциях. Сначала запись в Room, потом синхронизация. */
class TransactionRepository(
    private val dao: TransactionDao,
    private val remote: FirestoreDataSource,
    private val scheduleSync: () -> Unit,
    private val scope: CoroutineScope
) {

    fun observeAll(): Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Сколько операций ещё не отправлено в облако. */
    fun observeUnsyncedCount(): Flow<Int> = dao.observeUnsyncedCount()

    /** Баланс = все доходы минус все расходы. */
    fun observeBalance(): Flow<Double> =
        combine(
            dao.observeTotal(TransactionType.INCOME),
            dao.observeTotal(TransactionType.EXPENSE)
        ) { income, expense -> income - expense }

    fun observeMonthExpense(): Flow<Double> {
        val (from, to) = currentMonthRange()
        return dao.observeTotalBetween(TransactionType.EXPENSE, from, to)
    }

    fun observeMonthIncome(): Flow<Double> {
        val (from, to) = currentMonthRange()
        return dao.observeTotalBetween(TransactionType.INCOME, from, to)
    }

    fun observeMonthExpensesByCategory(): Flow<List<CategoryTotal>> {
        val (from, to) = currentMonthRange()
        return dao.observeExpensesByCategory(from, to)
    }

    suspend fun addTransaction(
        type: TransactionType,
        amount: Double,
        categoryId: Long,
        note: String,
        date: Long = System.currentTimeMillis()
    ): Long {
        val id = dao.insert(
            TransactionEntity(
                type = type,
                amount = amount,
                categoryId = categoryId,
                note = note,
                date = date
            )
        )
        scheduleSync()
        return id
    }

    /** Удаляет локально и просит облако удалить документ (Firestore сам доотправит при появлении сети). */
    suspend fun deleteTransaction(id: Long) {
        dao.deleteById(id)
        scope.launch {
            try {
                remote.delete(id)
            } catch (_: Exception) {
                // нет входа или сети: документ останется в облаке, на работу приложения не влияет
            }
        }
    }

    /** Удаляет всю историю: локально сразу, в облаке в фоне (при отсутствии сети Firestore доудалит сам). */
    suspend fun clearAll() {
        // Собираем id и из локальной базы, и из облака, чтобы ничего не вернулось при синхронизации
        val ids = HashSet<Long>()
        observeAll().first().forEach { ids.add(it.id) }
        try {
            withTimeoutOrNull(10_000) { remote.fetchAll() }?.forEach { ids.add(it.id) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // нет сети: удалим хотя бы то, что знаем локально
        }

        dao.clearAll()

        scope.launch {
            ids.forEach { id ->
                try {
                    remote.delete(id)
                } catch (_: Exception) {
                }
            }
        }
    }

    /** Возвращает удалённую операцию с тем же id (для кнопки «Отменить»). Снова уходит в облако. */
    suspend fun restoreTransaction(t: Transaction): Long {
        val id = dao.insert(
            TransactionEntity(
                id = t.id,
                type = t.type,
                amount = t.amount,
                categoryId = t.category.id,
                note = t.note,
                date = t.date,
                isSynced = false
            )
        )
        scheduleSync()
        return id
    }

    /** Обновляет операцию: после правки она снова считается несинхронизированной. */
    suspend fun updateTransaction(t: Transaction) {
        dao.deleteById(t.id)
        restoreTransaction(t)
    }

    /** Начало текущего месяца (включительно) и начало следующего (исключительно). */
    private fun currentMonthRange(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val first = LocalDate.now().withDayOfMonth(1)
        val from = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = first.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return from to to
    }
}