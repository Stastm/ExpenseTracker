package com.example.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.ExpenseApp
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.model.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Всё, что нужно главному экрану. */
data class HomeUiState(
    val balance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val recent: List<Transaction> = emptyList(),
    val unsynced: Int = 0
)

class HomeViewModel(private val transactions: TransactionRepository) : ViewModel() {

    /**
     * Пять потоков из базы объединяются в одно состояние.
     * При любом изменении в базе экран обновится сам.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        transactions.observeBalance(),
        transactions.observeMonthIncome(),
        transactions.observeMonthExpense(),
        transactions.observeAll().map { it.take(5) },
        transactions.observeUnsyncedCount()
    ) { balance, income, expense, recent, unsynced ->
        HomeUiState(balance, income, expense, recent, unsynced)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Удаляет операцию из Room и из Firestore. */
    fun delete(transaction: Transaction) {
        viewModelScope.launch { transactions.deleteTransaction(transaction.id) }
    }

    /** Возвращает удалённую операцию (кнопка «Отменить»), она снова уйдёт в облако. */
    fun restore(transaction: Transaction) {
        viewModelScope.launch { transactions.restoreTransaction(transaction) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseApp
                HomeViewModel(app.container.transactionRepository)
            }
        }
    }
}