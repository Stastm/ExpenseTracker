package com.example.expensetracker.di

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.data.database.AppDatabase
import com.example.expensetracker.data.network.FirestoreDataSource
import com.example.expensetracker.data.network.SyncManager
import com.example.expensetracker.data.network.SyncWorker
import com.example.expensetracker.data.repository.AuthRepository
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

/** Контейнер зависимостей. База создаётся лениво, при первом обращении. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    private val remote by lazy { FirestoreDataSource() }

    val syncManager by lazy { SyncManager(database.transactionDao(), remote) }

    val authRepository by lazy {
        AuthRepository(
            auth = FirebaseAuth.getInstance(),
            syncManager = { syncManager },
            clearLocal = { database.transactionDao().clearAll() }
        )
    }

    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy {
        TransactionRepository(database.transactionDao(), remote, ::scheduleSync, appScope)
    }

    /** Ставит задачу синхронизации в очередь. Выполнится, когда появится сеть. */
    fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}