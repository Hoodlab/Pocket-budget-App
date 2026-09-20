package org.hoods.pocket.data.repository

import kotlinx.coroutines.test.runTest
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.domain.model.AuthState
import org.hoods.pocket.domain.repository.AuthResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseAuthRepositoryTest {

    @Test
    fun unconfiguredConfig_startsUnauthenticated_andRejectsSignIn() = runTest {
        val repo = SupabaseAuthRepository(SupabaseConfig.unconfigured)

        assertEquals(AuthState.Unauthenticated, repo.authState.value)
        assertNull(repo.currentUser())

        val result = repo.signIn("user@test.com", "secret123")
        assertTrue(result is AuthResult.Error)
        assertEquals("Supabase is not configured", (result as AuthResult.Error).message)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun unconfiguredConfig_rejectsSignUp() = runTest {
        val repo = SupabaseAuthRepository(SupabaseConfig.unconfigured)

        val result = repo.signUp("user@test.com", "secret123")
        assertTrue(result is AuthResult.Error)
        assertEquals("Supabase is not configured", (result as AuthResult.Error).message)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun unconfiguredConfig_signOutSucceedsSilently() = runTest {
        val repo = SupabaseAuthRepository(SupabaseConfig.unconfigured)

        val result = repo.signOut()
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }
}
