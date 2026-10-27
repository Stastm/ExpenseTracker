package com.example.expensetracker.domain.model

/**
 * Финансовая операция для UI и бизнес-логики (не знает про Room).
 * amount всегда положительный, знак определяется по type.
 * date: время в миллисекундах (Unix time).
 */
data class Transaction(
    val id: Long,
    val type: TransactionType,
    val amount: Double,
    val category: Category,
    val note: String,
    val date: Long,
    val isSynced: Boolean
)