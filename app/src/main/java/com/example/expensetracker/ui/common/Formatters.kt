package com.example.expensetracker.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Символ валюты. Поменяйте здесь, если нужна другая. */
const val CURRENCY = "₸"

private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))

/** 1234.5 -> «1 234,50 ₸» (разделители зависят от языка телефона). */
fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "%,.2f", value) + " " + CURRENCY

/** Время в миллисекундах -> «9 октября». */
fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(dateFormatter)