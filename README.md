# ExpenseTracker

Android-приложение для учёта расходов и доходов.

## Возможности
- История операций, категории, статистика по месяцам с диаграммой
- Краткое содержание операций по карте (как в выписке Kaspi)
- Импорт операций из PDF-выписки Kaspi Gold с автоподбором категорий
- Синхронизация с облаком (Firebase) и Telegram-бот с диаграммами расходов
- Работа без интернета: данные хранятся локально

## Технологии
Kotlin, Jetpack Compose, Room, Coroutines/Flow, WorkManager, Firebase (Auth, Firestore), PdfBox

## Запуск
Для сборки нужен собственный файл `app/google-services.json` из проекта Firebase.
