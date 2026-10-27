package com.example.expensetracker.domain.model

/**
 * Категория операции (Еда, Транспорт и т. д.).
 * iconKey: ключ иконки, по нему UI выберет картинку.
 * colorArgb: цвет категории для диаграммы.
 */
data class Category(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val type: TransactionType
)