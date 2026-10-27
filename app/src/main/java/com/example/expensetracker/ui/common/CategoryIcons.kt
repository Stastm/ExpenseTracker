package com.example.expensetracker.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * В базе хранится только текстовый ключ иконки (iconKey),
 * а сама картинка подбирается здесь, в слое UI.
 */
fun categoryIcon(key: String): ImageVector = when (key) {
    "food" -> Icons.Filled.Restaurant
    "transport" -> Icons.Filled.DirectionsCar
    "fun" -> Icons.Filled.SportsEsports
    "home" -> Icons.Filled.Home
    "shopping" -> Icons.Filled.ShoppingCart
    "health" -> Icons.Filled.LocalHospital
    "phone" -> Icons.Filled.PhoneAndroid
    "salary" -> Icons.Filled.Payments
    "gift" -> Icons.Filled.CardGiftcard
    "work" -> Icons.Filled.Work
    else -> Icons.Filled.MoreHoriz
}