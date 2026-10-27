package com.example.expensetracker.data.database

import androidx.room.TypeConverter
import com.example.expensetracker.domain.model.TransactionType

/** Room не умеет хранить enum сам, поэтому сохраняем его как текст. */
class Converters {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}