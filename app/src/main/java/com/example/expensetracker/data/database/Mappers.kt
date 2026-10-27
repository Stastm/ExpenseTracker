package com.example.expensetracker.data.database

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Transaction

/** Преобразование моделей базы данных в доменные модели. */
fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    type = type
)

fun TransactionWithCategory.toDomain() = Transaction(
    id = transaction.id,
    type = transaction.type,
    amount = transaction.amount,
    category = category.toDomain(),
    note = transaction.note,
    date = transaction.date,
    isSynced = transaction.isSynced
)