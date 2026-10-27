package com.example.expensetracker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.Transaction
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen

/** Одна строка списка операций. Используется на главной и в истории. */
@Composable
fun TransactionItem(transaction: Transaction, modifier: Modifier = Modifier) {
    val categoryColor = Color(transaction.category.colorArgb)
    val isIncome = transaction.type == TransactionType.INCOME

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Круглая иконка категории на полупрозрачном фоне её цвета
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(transaction.category.iconKey),
                contentDescription = null,
                tint = categoryColor
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.category.name,
                style = MaterialTheme.typography.titleMedium
            )
            val subtitle = if (transaction.note.isBlank()) {
                formatDate(transaction.date)
            } else {
                "${transaction.note} · ${formatDate(transaction.date)}"
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))

        Text(
            text = (if (isIncome) "+" else "−") + formatMoney(transaction.amount),
            style = MaterialTheme.typography.titleMedium,
            color = if (isIncome) IncomeGreen else ExpenseRed
        )
    }
}