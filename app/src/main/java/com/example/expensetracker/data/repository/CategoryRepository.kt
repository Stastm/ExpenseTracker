package com.example.expensetracker.data.repository

import com.example.expensetracker.data.database.CategoryDao
import com.example.expensetracker.data.database.CategoryEntity
import com.example.expensetracker.data.database.toDomain
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Доступ к категориям. UI работает только с доменными моделями. */
class CategoryRepository(private val dao: CategoryDao) {

    fun observeByType(type: TransactionType): Flow<List<Category>> =
        dao.observeByType(type).map { list -> list.map { it.toDomain() } }

    suspend fun getByType(type: TransactionType): List<Category> =
        dao.getByType(type).map { it.toDomain() }

    /** Создаёт категорию «Депозит» (для расходов и для доходов), если её ещё нет. */
    suspend fun ensureDepositCategories() {
        for (type in listOf(TransactionType.EXPENSE, TransactionType.INCOME)) {
            if (dao.countByNameAndType(DEPOSIT, type) == 0) {
                dao.insert(
                    CategoryEntity(
                        name = DEPOSIT,
                        iconKey = "other",
                        colorArgb = 0xFF26A69A,
                        type = type
                    )
                )
            }
        }
    }

    companion object {
        const val DEPOSIT = "Депозит"
    }
}