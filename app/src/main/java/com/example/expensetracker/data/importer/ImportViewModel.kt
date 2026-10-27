package com.example.expensetracker.ui.importer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.ExpenseApp
import com.example.expensetracker.data.importer.ParsedOperation
import com.example.expensetracker.data.importer.StatementParser
import com.example.expensetracker.data.importer.StatementSummary
import com.example.expensetracker.data.repository.CategoryRepository
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Строка предпросмотра импорта. */
data class ImportItem(
    val id: Int,
    val op: ParsedOperation,
    val categoryId: Long?,
    val selected: Boolean,
    val duplicate: Boolean
)

data class ImportUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val items: List<ImportItem> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val summary: StatementSummary? = null,
    val error: String? = null,
    /** Фрагмент прочитанного текста, если операции не нашлись (чтобы можно было разобраться). */
    val debugText: String? = null,
    val importedCount: Int? = null
)

class ImportViewModel(
    private val app: ExpenseApp,
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun reset() {
        _state.value = ImportUiState()
    }

    fun onFileSelected(uri: Uri) {
        _state.value = ImportUiState(loading = true)
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { StatementParser.extractText(app, uri) }
                val ops = StatementParser.parse(text)
                if (ops.isEmpty()) {
                    _state.value = ImportUiState(
                        error = "Не удалось найти операции в файле. Возможно, это не выписка Kaspi Gold.",
                        debugText = text.take(700)
                    )
                    return@launch
                }
                val summary = StatementParser.summarize(text, ops)

                // Категория «Депозит» создаётся при первом импорте
                categories.ensureDepositCategories()

                val expense = categories.getByType(TransactionType.EXPENSE)
                val income = categories.getByType(TransactionType.INCOME)

                // Что уже есть в приложении: считаем по ключу, чтобы не плодить дубли
                val counts = HashMap<String, Int>()
                transactions.observeAll().first().forEach { t ->
                    val day = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).toLocalDate()
                    counts.merge(keyOf(day, t.type, t.amount, t.note), 1, Int::plus)
                }

                val items = ops.mapIndexed { index, op ->
                    val key = keyOf(op.date, op.type, op.amount, op.note)
                    val left = counts[key] ?: 0
                    val duplicate = left > 0
                    if (duplicate) counts[key] = left - 1

                    val list = if (op.type == TransactionType.EXPENSE) expense else income
                    val wanted = StatementParser.guessCategoryName(op)
                    val category = list.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
                        ?: list.firstOrNull()

                    ImportItem(
                        id = index,
                        op = op,
                        categoryId = category?.id,
                        selected = !duplicate,
                        duplicate = duplicate
                    )
                }
                _state.value = ImportUiState(
                    items = items,
                    expenseCategories = expense,
                    incomeCategories = income,
                    summary = summary
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ImportUiState(error = "Не удалось прочитать файл: ${e.localizedMessage}")
            }
        }
    }

    fun toggle(id: Int) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == id) it.copy(selected = !it.selected) else it })
    }

    fun setCategory(id: Int, categoryId: Long) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == id) it.copy(categoryId = categoryId) else it })
    }

    fun importSelected() {
        val chosen = _state.value.items.filter { it.selected && it.categoryId != null }
        if (chosen.isEmpty()) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            // В выписке новые сверху: добавляем с конца, чтобы порядок в истории совпал
            chosen.asReversed().forEach { item ->
                transactions.addTransaction(
                    type = item.op.type,
                    amount = item.op.amount,
                    categoryId = item.categoryId!!,
                    note = item.op.note,
                    date = item.op.dateMillis()
                )
            }
            _state.update { it.copy(saving = false, importedCount = chosen.size) }
        }
    }

    private fun keyOf(day: LocalDate, type: TransactionType, amount: Double, note: String) =
        "$day|$type|${String.format(Locale.US, "%.2f", amount)}|$note"

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseApp
                ImportViewModel(
                    app,
                    app.container.transactionRepository,
                    app.container.categoryRepository
                )
            }
        }
    }
}