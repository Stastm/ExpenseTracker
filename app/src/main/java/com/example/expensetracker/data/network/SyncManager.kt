package com.example.expensetracker.data.network

import com.example.expensetracker.data.database.TransactionDao
import com.example.expensetracker.data.database.TransactionEntity
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Двусторонняя синхронизация: отправка несинхронизированных и загрузка из облака. */
class SyncManager(
    private val dao: TransactionDao,
    private val remote: FirestoreDataSource
) {
    /** true, если все операции отправлены; false, если были ошибки (нужен повтор). */
    suspend fun syncPending(): Boolean {
        var allOk = true
        for (e in dao.getUnsynced()) {
            try {
                // Без интернета Firestore ждёт ответа бесконечно, поэтому таймаут.
                val done = withTimeoutOrNull(15_000) {
                    remote.upload(e.id, e.type.name, e.amount, e.categoryId, e.note, e.date)
                    true
                }
                if (done == true) dao.markSynced(e.id) else allOk = false
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                allOk = false
            }
        }
        return allOk
    }

    /** Скачивает операции из облака в Room (с пометкой «синхронизировано»). */
    suspend fun pullFromCloud(): Boolean {
        return try {
            val items = withTimeoutOrNull(15_000) { remote.fetchAll() } ?: return false
            for (r in items) {
                try {
                    dao.upsert(
                        TransactionEntity(
                            id = r.id,
                            type = TransactionType.valueOf(r.type),
                            amount = r.amount,
                            categoryId = r.categoryId,
                            note = r.note,
                            date = r.date,
                            isSynced = true
                        )
                    )
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Exception) {
                    // пропускаем одну битую запись, остальные загружаем
                }
            }
            true
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            false
        }
    }
}