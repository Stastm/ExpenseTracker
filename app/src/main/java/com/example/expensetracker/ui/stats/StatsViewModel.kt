package com.example.expensetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.database.CategoryTotal
import com.example.expensetracker.data.importer.SummaryLine
import com.example.expensetracker.data.repository.TransactionRepository
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class StatsUiState(
    val monthExpense: Double = 0.0,
    val monthIncome: Double = 0.0,
    val byCategory: List<CategoryTotal> = emptyList(),
    /** Краткое содержание по карте за текущий месяц. */
    val cardSummaryMonth: List<SummaryLine> = emptyList(),
    /** Краткое содержание по карте за всё время. */
    val cardSummaryAll: List<SummaryLine> = emptyList()
)

class StatsViewModel(
    repository: TransactionRepository
) : ViewModel() {

    val state: StateFlow<StatsUiState> = combine(
        repository.observeMonthExpense(),
        repository.observeMonthIncome(),
        repository.observeMonthExpensesByCategory(),
        repository.observeAll()
    ) { expense, income, categories, all ->
        val zone = ZoneId.systemDefault()
        val first = LocalDate.now().withDayOfMonth(1)
        val from = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = first.plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val monthOps = all.filter { it.date >= from && it.date < to }

        StatsUiState(
            monthExpense = expense,
            monthIncome = income,
            byCategory = categories,
            cardSummaryMonth = buildSummary(monthOps.map { Triple(it.type, it.amount, it.note) }),
            cardSummaryAll = buildSummary(all.map { Triple(it.type, it.amount, it.note) })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    companion object {
        private val TITLES = listOf(
            "Пополнения",
            "Поступления со своих счетов",
            "Зачисления кредитов",
            "Переводы",
            "Переводы на свои счета",
            "Покупки",
            "Снятия",
            "Разное"
        )
        private const val MANUAL = "Добавлено вручную"

        /** Вид операции определяем по началу заметки: при импорте она начинается с названия операции. */
        private fun titleOf(note: String): String = when {
            note.startsWith("Поступление со своего счета") -> "Поступления со своих счетов"
            note.startsWith("Перевод на свой счет") -> "Переводы на свои счета"
            note.startsWith("Пополнение") -> "Пополнения"
            note.startsWith("Зачисление") -> "Зачисления кредитов"
            note.startsWith("Перевод") -> "Переводы"
            note.startsWith("Покупка") -> "Покупки"
            note.startsWith("Снятие") -> "Снятия"
            note.startsWith("Возврат") || note.startsWith("Операция") -> "Разное"
            else -> MANUAL
        }

        private fun buildSummary(ops: List<Triple<TransactionType, Double, String>>): List<SummaryLine> {
            val sums = LinkedHashMap<String, Double>()
            TITLES.forEach { sums[it] = 0.0 }
            var manualCount = 0
            for ((type, amount, note) in ops) {
                val title = titleOf(note)
                if (title == MANUAL) manualCount++
                val signed = if (type == TransactionType.INCOME) amount else -amount
                sums[title] = (sums[title] ?: 0.0) + signed
            }
            if (manualCount == 0) sums.remove(MANUAL)
            return sums.map { SummaryLine(it.key, it.value) }
        }

        fun factory(repository: TransactionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StatsViewModel(repository) as T
        }
    }
}