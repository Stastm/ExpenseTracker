package com.example.expensetracker.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    /** Flow сам присылает новый список при любом изменении таблицы. */
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    fun observeByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    suspend fun getByType(type: TransactionType): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    /** Сколько категорий с таким именем и типом уже есть. */
    @Query("SELECT COUNT(*) FROM categories WHERE name = :name AND type = :type")
    suspend fun countByNameAndType(name: String, type: TransactionType): Int
}