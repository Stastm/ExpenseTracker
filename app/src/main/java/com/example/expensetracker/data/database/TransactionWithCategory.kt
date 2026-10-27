package com.example.expensetracker.data.database

import androidx.room.Embedded
import androidx.room.Relation

/** Операция вместе со своей категорией (Room сам делает JOIN). */
data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity
)

/** Сумма расходов по одной категории (для круговой диаграммы). */
data class CategoryTotal(
    val categoryId: Long,
    val name: String,
    val colorArgb: Long,
    val total: Double
)