package com.example.expensetracker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.expensetracker.domain.model.TransactionType

/** Таблица categories. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val colorArgb: Long,
    val type: TransactionType
)