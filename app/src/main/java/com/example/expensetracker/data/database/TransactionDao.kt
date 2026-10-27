package com.example.expensetracker.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import androidx.room.OnConflictStrategy

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Вся история, новые сверху. */
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<TransactionWithCategory>>

    /** Операции за период [from, to). */
    @Transaction
    @Query(
        "SELECT * FROM transactions WHERE date >= :from AND date < :to " +
                "ORDER BY date DESC, id DESC"
    )
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionWithCategory>>

    /** Общая сумма по типу за всё время (для баланса). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = :type")
    fun observeTotal(type: TransactionType): Flow<Double>

    /** Сумма по типу за период. */
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
                "WHERE type = :type AND date >= :from AND date < :to"
    )
    fun observeTotalBetween(type: TransactionType, from: Long, to: Long): Flow<Double>

    /** Расходы, сгруппированные по категориям. */
    @Query(
        "SELECT c.id AS categoryId, c.name AS name, c.colorArgb AS colorArgb, " +
                "SUM(t.amount) AS total " +
                "FROM transactions t INNER JOIN categories c ON c.id = t.categoryId " +
                "WHERE t.type = 'EXPENSE' AND t.date >= :from AND t.date < :to " +
                "GROUP BY c.id ORDER BY total DESC"
    )
    fun observeExpensesByCategory(from: Long, to: Long): Flow<List<CategoryTotal>>
    /** Операции, которые ещё не отправлены в облако. */
    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    /** Для индикатора в интерфейсе: сколько операций ждут отправки. */
    @Query("SELECT COUNT(*) FROM transactions WHERE isSynced = 0")
    fun observeUnsyncedCount(): Flow<Int>
    /** Вставка с заменой: используется при загрузке из облака. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}