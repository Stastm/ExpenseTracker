package com.example.expensetracker.data.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.ExpenseApp

/** Фоновая задача: отправляет несинхронизированные операции. Если были ошибки, WorkManager повторит. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ExpenseApp).container
        return if (container.syncManager.syncPending()) Result.success() else Result.retry()
    }
}