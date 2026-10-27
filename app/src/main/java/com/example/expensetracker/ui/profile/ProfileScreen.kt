package com.example.expensetracker.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ExpenseApp
import com.example.expensetracker.ui.importer.ImportScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.security.SecureRandom

private const val BOT_USERNAME = "rashodydoxodybot"
private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

private fun randomCode(length: Int = 8): String {
    val rnd = SecureRandom()
    return (1..length).map { ALPHABET[rnd.nextInt(ALPHABET.length)] }.joinToString("")
}

private fun createTelegramCode(
    uid: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val code = randomCode()
    FirebaseFirestore.getInstance()
        .collection("users").document(uid)
        .collection("telegram_codes").document(code)
        .set(mapOf("uid" to uid, "createdAt" to System.currentTimeMillis()))
        .addOnSuccessListener { onSuccess(code) }
        .addOnFailureListener { onError(it.message ?: "Ошибка записи") }
}

private fun unlinkTelegram(uid: String, onError: (String) -> Unit) {
    FirebaseFirestore.getInstance()
        .collection("telegram_chats").whereEqualTo("uid", uid).get()
        .addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
        .addOnFailureListener { onError(it.message ?: "Не удалось отвязать") }
}

private fun openUri(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/** Профиль: аккаунт, Telegram, импорт выписки, очистка истории и выход. */
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid
    val email = user?.email ?: "Аккаунт"

    var showImport by remember { mutableStateOf(false) }

    var showClearDialog by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val transactionRepository =
        (context.applicationContext as ExpenseApp).container.transactionRepository

    // null = ещё проверяем, true = привязан, false = не привязан
    var linked by remember { mutableStateOf<Boolean?>(null) }
    var code by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Системная кнопка «Назад» закрывает импорт
    BackHandler(enabled = showImport) { showImport = false }

    // Следим за привязкой: когда бот привяжет аккаунт, экран переключится сам
    DisposableEffect(uid) {
        if (uid == null) {
            linked = false
            onDispose { }
        } else {
            val registration = FirebaseFirestore.getInstance()
                .collection("telegram_chats")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snap, e ->
                    if (e != null) {
                        linked = false
                        error = "Не удалось проверить привязку: ${e.message}"
                        return@addSnapshotListener
                    }
                    linked = snap != null && !snap.isEmpty
                }
            onDispose { registration.remove() }
        }
    }

    LaunchedEffect(linked) {
        if (linked == true) code = null
    }

    if (showImport) {
        ImportScreen(onClose = { showImport = false })
        return
    }

    // Подтверждение очистки истории
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { if (!clearing) showClearDialog = false },
            title = { Text("Очистить всю историю?") },
            text = {
                Text("Все операции будут удалены с телефона и из облака. Это нельзя отменить. Категории останутся.")
            },
            confirmButton = {
                TextButton(
                    enabled = !clearing,
                    onClick = {
                        clearing = true
                        scope.launch {
                            transactionRepository.clearAll()
                            clearing = false
                            showClearDialog = false
                        }
                    }
                ) {
                    Text(
                        text = if (clearing) "Удаляю..." else "Удалить всё",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !clearing,
                    onClick = { showClearDialog = false }
                ) { Text("Отмена") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineMedium)

        // Карточка аккаунта
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = email.first().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ваш аккаунт",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Импорт из выписки
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { showImport = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Импорт из выписки Kaspi", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Добавить операции из PDF-выписки",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Карточка Telegram
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Telegram-бот", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when (linked) {
                                null -> "Проверяем привязку..."
                                true -> "Привязан"
                                false -> "Не привязан"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (linked == true) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                when {
                    linked == null -> {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }

                    linked == true -> {
                        Text(
                            text = "Диаграммы расходов приходят в @$BOT_USERNAME и обновляются сами.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { openUri(context, "https://t.me/$BOT_USERNAME") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) { Text("Открыть бота") }
                        TextButton(
                            onClick = { uid?.let { unlinkTelegram(it) { msg -> error = msg } } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Отвязать Telegram", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    code == null -> {
                        Button(
                            enabled = !loading && uid != null,
                            onClick = {
                                if (uid == null) return@Button
                                loading = true
                                error = null
                                createTelegramCode(
                                    uid = uid,
                                    onSuccess = { code = it; loading = false },
                                    onError = { error = it; loading = false }
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(if (loading) "Создаю код..." else "Привязать Telegram")
                        }
                    }

                    else -> {
                        Text(
                            text = code ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Код действует 15 минут",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { openUri(context, "https://t.me/$BOT_USERNAME?start=$code") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) { Text("Открыть бота") }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Или отправьте боту: /start $code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Очистка истории
        OutlinedButton(
            onClick = { showClearDialog = true },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Очистить всю историю")
        }

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Выйти из аккаунта")
        }
    }
}