package com.example.expensetracker.ui.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.importer.StatementSummary
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.ui.common.categoryIcon
import com.example.expensetracker.ui.common.formatMoney
import com.example.expensetracker.ui.theme.ExpenseRed
import com.example.expensetracker.ui.theme.IncomeGreen
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Импорт операций из PDF-выписки Kaspi Gold. */
@Composable
fun ImportScreen(
    onClose: () -> Unit,
    viewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pickingFor by remember { mutableStateOf<ImportItem?>(null) }
    var showSummary by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFileSelected(uri)
    }

    // При выходе с экрана очищаем состояние
    DisposableEffect(Unit) { onDispose { viewModel.reset() } }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Импорт выписки", style = MaterialTheme.typography.titleLarge)
        }

        when {
            state.loading || state.saving -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.importedCount != null -> DonePane(count = state.importedCount ?: 0, onClose = onClose)

            state.items.isNotEmpty() -> ItemsPane(
                state = state,
                onToggle = viewModel::toggle,
                onPickCategory = { pickingFor = it },
                onShowSummary = { showSummary = true },
                onImport = viewModel::importSelected
            )

            else -> StartPane(
                error = state.error,
                debugText = state.debugText,
                onPick = { picker.launch(arrayOf("application/pdf")) }
            )
        }
    }

    // Выбор категории для строки
    pickingFor?.let { item ->
        val list = if (item.op.type == TransactionType.EXPENSE) state.expenseCategories
        else state.incomeCategories
        AlertDialog(
            onDismissRequest = { pickingFor = null },
            title = { Text("Категория") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    list.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCategory(item.id, category.id)
                                    pickingFor = null
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = categoryIcon(category.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(category.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickingFor = null }) { Text("Отмена") } }
        )
    }

    // Краткое содержание операций по карте
    if (showSummary) {
        state.summary?.let { summary ->
            SummaryDialog(summary = summary, onDismiss = { showSummary = false })
        }
    }
}

@Composable
private fun SummaryDialog(summary: StatementSummary, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Краткое содержание") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (summary.startDate != null && summary.startBalance != null) {
                    SummaryRow(
                        title = "Доступно на ${summary.startDate}",
                        amount = summary.startBalance,
                        bold = true,
                        colored = false
                    )
                }
                summary.lines.forEach { line ->
                    SummaryRow(title = line.title, amount = line.amount)
                }
                if (summary.endDate != null && summary.endBalance != null) {
                    SummaryRow(
                        title = "Доступно на ${summary.endDate}",
                        amount = summary.endBalance,
                        bold = true,
                        colored = false
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Суммы посчитаны по всем операциям из файла, а не только по отмеченным.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun SummaryRow(
    title: String,
    amount: Double,
    bold: Boolean = false,
    colored: Boolean = true
) {
    val negative = amount < -0.004
    val positive = amount > 0.004
    val sign = if (negative) "−" else "+"
    val color = when {
        !colored -> Color.Unspecified
        negative -> ExpenseRed
        positive -> IncomeGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val weight = if (bold) FontWeight.Bold else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$sign ${formatMoney(abs(amount))}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight,
            color = color
        )
    }
}

@Composable
private fun StartPane(error: String?, debugText: String?, onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    imageVector = Icons.Filled.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("Выписка Kaspi Gold", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "В приложении Kaspi.kz: Мой Банк, Kaspi Gold, «Выписка». " +
                            "Сформируйте её в PDF, сохраните на телефон и выберите здесь файл. " +
                            "Перед добавлением вы увидите список и сможете убрать лишнее.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onPick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) { Text("Выбрать PDF-файл") }
            }
        }

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        if (debugText != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Что прочитано из файла:", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(debugText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ItemsPane(
    state: ImportUiState,
    onToggle: (Int) -> Unit,
    onPickCategory: (ImportItem) -> Unit,
    onShowSummary: () -> Unit,
    onImport: () -> Unit
) {
    val selectedCount = state.items.count { it.selected }

    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.summary != null) {
            item {
                OutlinedButton(
                    onClick = onShowSummary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Краткое содержание по карте") }
            }
        }
        item {
            Text(
                text = "Найдено операций: ${state.items.size}. Выбрано: $selectedCount. " +
                        "Переводы между своими счетами идут в категорию «Депозит».",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(state.items, key = { it.id }) { item ->
            val list = if (item.op.type == TransactionType.EXPENSE) state.expenseCategories
            else state.incomeCategories
            val categoryName = list.firstOrNull { it.id == item.categoryId }?.name ?: "Выбрать"
            ImportRow(
                item = item,
                categoryName = categoryName,
                onToggle = { onToggle(item.id) },
                onPickCategory = { onPickCategory(item) }
            )
        }
    }

    Button(
        onClick = onImport,
        enabled = selectedCount > 0,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(56.dp)
    ) {
        Text("Импортировать ($selectedCount)", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ImportRow(
    item: ImportItem,
    categoryName: String,
    onToggle: () -> Unit,
    onPickCategory: () -> Unit
) {
    val op = item.op
    val isIncome = op.type == TransactionType.INCOME

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.selected, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (op.details.isBlank()) op.opName else op.details,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${op.date.format(dateFormat)} · ${op.opName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                when {
                    item.duplicate -> Text(
                        text = "Уже есть в приложении",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    op.internal -> Text(
                        text = "Депозит (между своими счетами)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Категория: $categoryName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onPickCategory)
                        .padding(top = 4.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = (if (isIncome) "+" else "−") + formatMoney(op.amount),
                style = MaterialTheme.typography.titleSmall,
                color = if (isIncome) IncomeGreen else ExpenseRed
            )
        }
    }
}

@Composable
private fun DonePane(count: Int, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Добавлено операций: $count", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Они уже в истории и отправляются в облако, бот подхватит их сам.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClose,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) { Text("Готово") }
    }
}