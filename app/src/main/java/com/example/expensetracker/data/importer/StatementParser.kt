package com.example.expensetracker.data.importer

import android.content.Context
import android.net.Uri
import com.example.expensetracker.domain.model.TransactionType
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.time.LocalDate
import java.time.ZoneId

/** Одна операция из выписки. */
data class ParsedOperation(
    val date: LocalDate,
    val type: TransactionType,
    val amount: Double,
    val opName: String,
    val details: String,
    /** Перевод между своими счетами (депозит): идёт в категорию «Депозит». */
    val internal: Boolean
) {
    val note: String
        get() = (if (details.isBlank()) opName else "$opName · $details").take(100)

    /** В выписке нет времени, ставим полдень, чтобы дата не «съезжала» из-за часового пояса. */
    fun dateMillis(): Long =
        date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** Строка краткого содержания: название и сумма (плюс для поступлений, минус для списаний). */
data class SummaryLine(val title: String, val amount: Double)

/** Краткое содержание операций по карте, как в начале выписки Kaspi. */
data class StatementSummary(
    val startDate: String?,
    val startBalance: Double?,
    val endDate: String?,
    val endBalance: Double?,
    val lines: List<SummaryLine>
)

/** Разбор PDF-выписки Kaspi Gold. */
object StatementParser {

    // 16.10.26 - 1 050,00 ₸ Покупка ИП "Название"
    private val LINE = Regex(
        """^(\d{2})\.(\d{2})\.(\d{2})\s+([+\-−–])\s*([\d\s\u00A0\u202F]+,\d{2})\s*\S{0,2}\s+(.+)$"""
    )
    private val SPACES = Regex("[\\s\\u00A0\\u202F]")

    // Доступно на 21.10.25 + 4 060,00 ₸
    private val BALANCE = Regex(
        """Доступно на\s+(\d{2}\.\d{2}\.\d{2})\s+([+\-−–])\s*([\d\s\u00A0\u202F]+,\d{2})"""
    )

    // Длинные названия идут раньше коротких
    private val OPS = listOf(
        "Перевод на свой", "Поступление со", "Перевод", "Покупка",
        "Пополнение", "Снятие", "Зачисление", "Возврат"
    )

    /** Расходы с фиксированной суммой: сумма в тенге -> название категории. */
    private val FIXED_AMOUNTS = mapOf(
        650.0 to "Еда",
        700.0 to "Транспорт"
    )

    /** Порядок строк в кратком содержании такой же, как в выписке. */
    private val SUMMARY_TITLES = listOf(
        "Пополнения",
        "Поступления со своих счетов",
        "Зачисления кредитов",
        "Переводы",
        "Переводы на свои счета",
        "Покупки",
        "Снятия",
        "Разное"
    )

    /** Достаёт весь текст из PDF. Вызывать не на главном потоке. */
    fun extractText(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Не удалось открыть файл")
        return stream.use { input ->
            PDDocument.load(input).use { doc ->
                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                stripper.getText(doc)
            }
        }
    }

    /** Разбирает текст выписки в список операций (в том же порядке, что в файле). */
    fun parse(text: String): List<ParsedOperation> {
        val result = mutableListOf<ParsedOperation>()
        for (raw in text.lines()) {
            val m = LINE.matchEntire(raw.trim()) ?: continue
            val (dd, mm, yy, sign, amountText, rest) = m.destructured

            val amount = amountText.replace(SPACES, "").replace(',', '.').toDoubleOrNull()
                ?: continue
            val date = try {
                LocalDate.of(2000 + yy.toInt(), mm.toInt(), dd.toInt())
            } catch (e: Exception) {
                continue
            }

            val op = OPS.firstOrNull { rest.startsWith(it, ignoreCase = true) }
            val details = if (op == null) rest.trim() else rest.substring(op.length).trim()
            val (opName, internal) = when (op) {
                "Перевод на свой" -> "Перевод на свой счет" to true
                "Поступление со" -> "Поступление со своего счета" to true
                null -> "Операция" to false
                else -> op to false
            }

            result += ParsedOperation(
                date = date,
                type = if (sign == "+") TransactionType.INCOME else TransactionType.EXPENSE,
                amount = amount,
                opName = opName,
                details = details,
                internal = internal
            )
        }
        return result
    }

    private fun summaryTitle(op: ParsedOperation): String = when (op.opName) {
        "Пополнение" -> "Пополнения"
        "Поступление со своего счета" -> "Поступления со своих счетов"
        "Зачисление" -> "Зачисления кредитов"
        "Перевод" -> "Переводы"
        "Перевод на свой счет" -> "Переводы на свои счета"
        "Покупка" -> "Покупки"
        "Снятие" -> "Снятия"
        else -> "Разное"
    }

    /** 21.10.25 -> 21.10.2025 */
    private fun fullDate(d: String): String = d.substring(0, 6) + "20" + d.substring(6)

    /** Краткое содержание: суммы по видам операций и остатки на начало и конец периода. */
    fun summarize(text: String, ops: List<ParsedOperation>): StatementSummary {
        val sums = LinkedHashMap<String, Double>()
        SUMMARY_TITLES.forEach { sums[it] = 0.0 }
        for (op in ops) {
            val title = summaryTitle(op)
            val signed = if (op.type == TransactionType.INCOME) op.amount else -op.amount
            sums[title] = (sums[title] ?: 0.0) + signed
        }

        val balances = BALANCE.findAll(text).map { m ->
            val value = m.groupValues[3].replace(SPACES, "").replace(',', '.').toDoubleOrNull() ?: 0.0
            fullDate(m.groupValues[1]) to (if (m.groupValues[2] == "+") value else -value)
        }.toList()
        val first = balances.firstOrNull()
        val last = if (balances.size >= 2) balances.last() else null

        return StatementSummary(
            startDate = first?.first,
            startBalance = first?.second,
            endDate = last?.first,
            endBalance = last?.second,
            lines = sums.map { SummaryLine(it.key, it.value) }
        )
    }

    /** Подбирает название категории по описанию. Дополняйте список под себя. */
    fun guessCategoryName(op: ParsedOperation): String {
        val text = "${op.opName} ${op.details}".lowercase()
        fun has(vararg words: String) = words.any { it in text }

        // Переводы между своими счетами (депозит)
        if (op.internal) return "Депозит"

        if (op.type == TransactionType.INCOME) {
            return if (has("зарплат", "salary")) "Зарплата" else "Другой доход"
        }

        // Сначала правила по точной сумме: 650 ₸ - еда, 700 ₸ - такси
        FIXED_AMOUNTS[op.amount]?.let { return it }

        return when {
            has("tele2", "теле2", "beeline", "билайн", "kcell", "altel", "алтел") -> "Связь"
            has("onay", "yandex", "яндекс", "bolt", "indrive", "такси", "автобус", "метро",
                "азс", "helios", "sinooil") -> "Транспорт"
            has("magnum", "small", "galmart", "korzinka", "кафе", "ресторан", "кофе",
                "coffee", "pizza", "пицца", "burger", "бургер", "kfc", "продукт") -> "Еда"
            has("аптека", "pharm", "клиник", "больниц", "стоматолог") -> "Здоровье"
            has("кино", "cinema", "steam", "playstation") -> "Развлечения"
            has("самообслуживания") -> "Другое"
            op.opName == "Покупка" -> "Покупки"
            else -> "Другое"
        }
    }
}