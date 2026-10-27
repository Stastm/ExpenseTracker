package com.example.expensetracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.ExpenseApp
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.model.Transaction
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val query: String = "",
    val typeFilter: TransactionType? = null, // null = все
    val items: List<Transaction> = emptyList(),
    val isEmptyDatabase: Boolean = true
)

class HistoryViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeAll(),
        query,
        typeFilter
    ) { all, q, type ->
        val text = q.trim()
        val filtered = all.filter { t ->
            (type == null || t.type == type) &&
                    (text.isEmpty() ||
                            t.note.contains(text, ignoreCase = true) ||
                            t.category.name.contains(text, ignoreCase = true))
        }
        HistoryUiState(
            query = q,
            typeFilter = type,
            items = filtered,
            isEmptyDatabase = all.isEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onQueryChange(value: String) { query.value = value }

    fun onTypeFilterChange(type: TransactionType?) { typeFilter.value = type }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction.id) }
    }

    fun restore(transaction: Transaction) {
        viewModelScope.launch { repository.restoreTransaction(transaction) }
    }

    fun update(old: Transaction, amount: Double, note: String) {
        viewModelScope.launch {
            repository.updateTransaction(old.copy(amount = amount, note = note.trim()))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseApp
                HistoryViewModel(app.container.transactionRepository)
            }
        }
    }
}