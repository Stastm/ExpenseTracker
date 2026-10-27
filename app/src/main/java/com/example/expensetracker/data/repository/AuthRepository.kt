package com.example.expensetracker.data.repository

import com.example.expensetracker.data.network.SyncManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/** Результат операции авторизации. */
sealed interface AuthResult {
    data object Success : AuthResult
    data class Error(val message: String) : AuthResult
}

/**
 * Репозиторий авторизации на Firebase Auth. Анонимный пользователь не считается вошедшим:
 * при регистрации к нему привязывается email, и uid (а с ним данные в облаке) сохраняется.
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val syncManager: () -> SyncManager,
    private val clearLocal: suspend () -> Unit
) {
    private val _isLoggedIn = MutableStateFlow(auth.currentUser?.isAnonymous == false)
    val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        auth.addAuthStateListener { _isLoggedIn.value = it.currentUser?.isAnonymous == false }
    }

    suspend fun register(email: String, password: String): AuthResult = try {
        val mail = email.trim()
        val user = auth.currentUser
        if (user != null && user.isAnonymous) {
            user.linkWithCredential(EmailAuthProvider.getCredential(mail, password)).await()
        } else {
            auth.createUserWithEmailAndPassword(mail, password).await()
        }
        _isLoggedIn.value = true
        AuthResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuthResult.Error(mapError(e))
    }

    suspend fun login(email: String, password: String): AuthResult = try {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        // Подтягиваем данные из облака до открытия главного экрана (если сети нет, догрузится позже).
        syncManager().pullFromCloud()
        _isLoggedIn.value = true
        AuthResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuthResult.Error(mapError(e))
    }

    /** Выход: сначала досылаем неотправленное, чтобы ничего не потерять. */
    suspend fun logout(): AuthResult {
        if (!syncManager().syncPending()) {
            return AuthResult.Error("Есть неотправленные операции. Подключитесь к интернету и повторите")
        }
        clearLocal()
        auth.signOut()
        _isLoggedIn.value = false
        return AuthResult.Success
    }

    private fun mapError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Пароль слишком простой (минимум 6 символов)"
        is FirebaseAuthUserCollisionException -> "Эта почта уже зарегистрирована. Войдите"
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> "Неверная почта или пароль"
        is FirebaseNetworkException -> "Нет подключения к интернету"
        else -> "Ошибка: ${e.localizedMessage}"
    }
}