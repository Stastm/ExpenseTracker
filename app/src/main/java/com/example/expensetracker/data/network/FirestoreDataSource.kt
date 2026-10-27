package com.example.expensetracker.data.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Операция, как она хранится в облаке. */
data class RemoteTransaction(
    val id: Long,
    val type: String,
    val amount: Double,
    val categoryId: Long,
    val note: String,
    val date: Long
)

/** Работа с Firebase: вход и запись/чтение операций в Firestore. */
class FirestoreDataSource {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private suspend fun uid(): String {
        auth.currentUser?.let { return it.uid }
        return auth.signInAnonymously().await().user!!.uid
    }

    private suspend fun collection() =
        db.collection("users").document(uid()).collection("transactions")

    suspend fun upload(
        id: Long, type: String, amount: Double,
        categoryId: Long, note: String, date: Long
    ) {
        val data = mapOf(
            "id" to id,
            "type" to type,
            "amount" to amount,
            "categoryId" to categoryId,
            "note" to note,
            "date" to date
        )
        collection().document(id.toString()).set(data).await()
    }

    suspend fun delete(id: Long) {
        collection().document(id.toString()).delete().await()
    }

    /** Все операции текущего пользователя из облака. */
    suspend fun fetchAll(): List<RemoteTransaction> =
        collection().get().await().documents.mapNotNull { d ->
            RemoteTransaction(
                id = d.getLong("id") ?: return@mapNotNull null,
                type = d.getString("type") ?: return@mapNotNull null,
                amount = d.getDouble("amount") ?: return@mapNotNull null,
                categoryId = d.getLong("categoryId") ?: return@mapNotNull null,
                note = d.getString("note") ?: "",
                date = d.getLong("date") ?: return@mapNotNull null
            )
        }
}