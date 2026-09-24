package org.hoods.pocket.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.model.AuthUser
import org.hoods.pocket.domain.repository.AuthRepository
import org.hoods.pocket.domain.repository.AuthResult

class SupabaseAuthRepository(
    val config: SupabaseConfig,
    clientOverride: SupabaseClient? = null,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AuthRepository {

    val client: SupabaseClient? = when {
        clientOverride != null -> clientOverride
        config.isConfigured -> {
            try {
                createSupabaseClient(
                    supabaseUrl = config.url.orEmpty(),
                    supabaseKey = config.publishableKey.orEmpty()
                ) {
                    install(Auth)
                    install(Postgrest)
                }
            } catch (e: Throwable) {
                null
            }
        }
        else -> null
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        client?.let { supabase ->
            coroutineScope.launch {
                supabase.auth.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val user = status.session.user
                            if (user != null) {
                                _authState.value = AuthState.Authenticated(
                                    AuthUser(
                                        id = user.id,
                                        email = user.email ?: ""
                                    )
                                )
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            _authState.value = AuthState.Unauthenticated
                        }
                        else -> {
                            // Loading or NetworkError retains current state
                        }
                    }
                }
            }
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        if (!config.isConfigured || client == null) {
            return AuthResult.Error("Supabase is not configured")
        }
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val currentSession = client.auth.currentSessionOrNull()
            if (currentSession != null) {
                val user = client.auth.currentUserOrNull() ?: currentSession.user
                val authUser = AuthUser(
                    id = user?.id.orEmpty(),
                    email = user?.email ?: email
                )
                _authState.value = AuthState.Authenticated(authUser)
                AuthResult.Success(authUser)
            } else {
                // Email confirmation is required - truthful state: remain unauthenticated
                AuthResult.ConfirmationRequired(email)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        if (!config.isConfigured || client == null) {
            return AuthResult.Error("Supabase is not configured")
        }
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val currentSession = client.auth.currentSessionOrNull()
            if (currentSession != null) {
                val user = client.auth.currentUserOrNull() ?: currentSession.user
                val authUser = AuthUser(
                    id = user?.id.orEmpty(),
                    email = user?.email ?: email
                )
                _authState.value = AuthState.Authenticated(authUser)
                AuthResult.Success(authUser)
            } else {
                AuthResult.Error("Sign in failed: No active session")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed")
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            client?.auth?.clearSession()
            client?.auth?.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }

    override fun currentUser(): AuthUser? {
        val current = _authState.value
        return if (current is AuthState.Authenticated) current.user else null
    }
}
