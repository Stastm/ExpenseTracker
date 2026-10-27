package com.example.expensetracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.ExpenseApp
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние формы добавления операции. */
data class AddUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val error: String? = null,
    val isSaved: Boolean = false
)

class AddTransactionViewModel(
    private val transactions: TransactionRepository,
    categories: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    // Тип операции отдельным потоком: при его смене подгружаются нужные категории
    private val typeFlow = MutableStateFlow(TransactionType.EXPENSE)
    private var isSaving = false

    init {
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            typeFlow
                .flatMapLatest { categories.observeByType(it) }
                .collect { list ->
                    _uiState.update { state ->
                        // Оставляем выбор, если категория есть в новом списке, иначе берём первую
                        val keep = list.any { it.id == state.selectedCategoryId }
                        state.copy(
                            categories = list,
                            selectedCategoryId = if (keep) state.selectedCategoryId else list.firstOrNull()?.id
                        )
                    }
                }
        }
    }

    fun onTypeChange(type: TransactionType) {
        typeFlow.value = type
        _uiState.update { it.copy(type = type, error = null) }
    }

    /** Пропускаем только цифры и один разделитель (точку или запятую). */
    fun onAmountChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '.' || it == ',' }
        val separators = cleaned.count { it == '.' || it == ',' }
        if (separators > 1 || cleaned.length > 12) return
        _uiState.update { it.copy(amount = cleaned, error = null) }
    }

    fun onNoteChange(value: String) =
        _uiState.update { it.copy(note = value.take(100), error = null) }

    fun onCategorySelect(id: Long) =
        _uiState.update { it.copy(selectedCategoryId = id, error = null) }

    fun onDateChange(millis: Long) =
        _uiState.update { it.copy(dateMillis = millis) }

    fun save() {
        val state = _uiState.value
        if (isSaving) return

        val amount = state.amount.replace(',', '.').toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(error = "Введите сумму больше нуля") }
            return
        }
        val category = state.categories.firstOrNull {
            it.id == state.selectedCategoryId && it.type == state.type
        }
        if (category == null) {
            _uiState.update { it.copy(error = "Выберите категорию") }
            return
        }

        isSaving = true // защита от двойного нажатия
        viewModelScope.launch {
            transactions.addTransaction(
                type = state.type,
                amount = amount,
                categoryId = category.id,
                note = state.note.trim(),
                date = state.dateMillis
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseApp
                AddTransactionViewModel(
                    app.container.transactionRepository,
                    app.container.categoryRepository
                )
            }
        }
    }
}