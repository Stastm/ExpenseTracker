package com.example.expensetracker.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Единственный экземпляр DataStore на всё приложение.
private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/** Сохранённые данные учётной записи. */
data class StoredCredentials(
    val email: String,
    val passwordHash: String,
    val salt: String
)

/** Хранилище авторизации на Jetpack DataStore. На этапе 7 здесь будет токен сервера. */
class AuthPreferences(private val context: Context) {

    private object Keys {
        val EMAIL = stringPreferencesKey("email")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val SALT = stringPreferencesKey("salt")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
    }

    /** Поток состояния сессии: true, если пользователь вошёл. */
    val isLoggedIn: Flow<Boolean> =
        context.authDataStore.data.map { it[Keys.LOGGED_IN] ?: false }

    suspend fun saveCredentials(credentials: StoredCredentials) {
        context.authDataStore.edit {
            it[Keys.EMAIL] = credentials.email
            it[Keys.PASSWORD_HASH] = credentials.passwordHash
            it[Keys.SALT] = credentials.salt
        }
    }

    suspend fun getCredentials(): StoredCredentials? {
        val prefs = context.authDataStore.data.first()
        val email = prefs[Keys.EMAIL] ?: return null
        val hash = prefs[Keys.PASSWORD_HASH] ?: return null
        val salt = prefs[Keys.SALT] ?: return null
        return StoredCredentials(email, hash, salt)
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.authDataStore.edit { it[Keys.LOGGED_IN] = value }
    }
}