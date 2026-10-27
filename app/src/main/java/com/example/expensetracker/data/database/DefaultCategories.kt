package com.example.expensetracker.data.database

import com.example.expensetracker.domain.model.TransactionType

/** Категории, которые создаются при первом запуске приложения. */
object DefaultCategories {

    val all: List<CategoryEntity> = listOf(
        // Расходы
        CategoryEntity(name = "Еда", iconKey = "food", colorArgb = 0xFFFF8A65, type = TransactionType.EXPENSE),
        CategoryEntity(name = "Транспорт", iconKey = "transport", colorArgb = 0xFF64B5F6, type = TransactionType.EXPENSE),
        CategoryEntity(name = "Развлечения", iconKey = "fun", colorArgb = 0xFFBA68C8, type = TransactionType.EXPENSE),
        CategoryEntity(name = "Дом", iconKey = "home", colorArgb = 0xFF4DB6AC, type = TransactionType.EXPENSE),
        CategoryEntity(name = "Покупки", iconKey = "shopping", colorArgb = 0xFFFFD54F, type = TransactionType.EXPENSE),
        CategoryEntity(name = "Здоровье", iconKey = "health", colorArgb = 0xFFE57373, type = TransactionType.EXPENSE),
        CategoryEntity(name = "Связь", iconKey = "phone", colorArgb = 0xFF7986CB, type = TransactionType.EXPENSE),
        CategoryEntity(name = "Другое", iconKey = "other", colorArgb = 0xFF90A4AE, type = TransactionType.EXPENSE),
        // Доходы
        CategoryEntity(name = "Зарплата", iconKey = "salary", colorArgb = 0xFF66DD99, type = TransactionType.INCOME),
        CategoryEntity(name = "Подарок", iconKey = "gift", colorArgb = 0xFFF06292, type = TransactionType.INCOME),
        CategoryEntity(name = "Подработка", iconKey = "work", colorArgb = 0xFF81C784, type = TransactionType.INCOME),
        CategoryEntity(name = "Другой доход", iconKey = "other", colorArgb = 0xFFA5D6A7, type = TransactionType.INCOME)
    )
}