package com.example.expensetracker.ui.history

import com.example.expensetracker.domain.model.Transaction
import com.example.expensetracker.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ruLocale = Locale.forLanguageTag("ru")

/** CSV для Excel: разделитель «;», UTF-8 с BOM, сумма с запятой. */
fun List<Transaction>.toCsv(): String {
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val sb = StringBuilder("\uFEFF")
    sb.append("Дата;Тип;Категория;Сумма;Заметка\r\n")
    forEach { t ->
        val date = Instant.ofEpochMilli(t.date)
            .atZone(ZoneId.systemDefault())
            .format(dateFormat)
        val type = if (t.type == TransactionType.EXPENSE) "Расход" else "Доход"
        sb.append(date).append(';')
            .append(type).append(';')
            .append(t.category.name.csvEscape()).append(';')
            .append("%.2f".format(ruLocale, t.amount)).append(';')
            .append(t.note.csvEscape())
            .append("\r\n")
    }
    return sb.toString()
}

private fun String.csvEscape(): String =
    if (any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else this