package com.example.expensetracker

import android.app.Application
import com.example.expensetracker.di.AppContainer

/** Точка входа приложения. Прописана в манифесте как android:name. */
class ExpenseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scheduleSync()
    }

    }
