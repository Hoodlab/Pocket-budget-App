package org.hoods.pocket.di

import org.hoods.pocket.data.remote.SupabaseEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmSupabaseConfigTest {

    @Test
    fun resolveJvmSupabaseConfig_reads_system_properties_with_precedence_over_environment_variables() {
        val sysProps = mapOf(
            "supabase.environment" to "LOCAL",
            "supabase.url" to "http://127.0.0.1:54321",
            "supabase.key" to "sys-prop-key"
        )
        val envVars = mapOf(
            "SUPABASE_ENVIRONMENT" to "HOSTED_DEV",
            "SUPABASE_URL" to "https://env.supabase.co",
            "SUPABASE_PUBLISHABLE_KEY" to "env-key"
        )

        val config = resolveJvmSupabaseConfig(
            systemPropertyLookup = { sysProps[it] },
            environmentLookup = { envVars[it] }
        )

        assertEquals(SupabaseEnvironment.LOCAL, config.environment)
        assertEquals("http://127.0.0.1:54321", config.url)
        assertEquals("sys-prop-key", config.publishableKey)
        assertTrue(config.isConfigured)
    }

    @Test
    fun resolveJvmSupabaseConfig_falls_back_to_environment_variables_when_system_properties_absent() {
        val envVars = mapOf(
            "SUPABASE_ENVIRONMENT" to "HOSTED_DEV",
            "SUPABASE_URL" to "https://project.supabase.co",
            "SUPABASE_PUBLISHABLE_KEY" to "env-key"
        )

        val config = resolveJvmSupabaseConfig(
            systemPropertyLookup = { null },
            environmentLookup = { envVars[it] }
        )

        assertEquals(SupabaseEnvironment.HOSTED_DEV, config.environment)
        assertEquals("https://project.supabase.co", config.url)
        assertEquals("env-key", config.publishableKey)
        assertTrue(config.isConfigured)
    }

    @Test
    fun resolveJvmSupabaseConfig_returns_unconfigured_when_no_properties_or_env_vars_set() {
        val config = resolveJvmSupabaseConfig(
            systemPropertyLookup = { null },
            environmentLookup = { null }
        )

        assertEquals(SupabaseEnvironment.UNCONFIGURED, config.environment)
        assertNull(config.url)
        assertNull(config.publishableKey)
        assertFalse(config.isConfigured)
    }

    @Test
    fun resolveJvmSupabaseConfig_returns_unconfigured_when_url_is_invalid() {
        val sysProps = mapOf(
            "pocket.supabase.environment" to "HOSTED_DEV",
            "pocket.supabase.url" to "http://insecure.supabase.co", // must be https for HOSTED_DEV
            "pocket.supabase.key" to "key"
        )

        val config = resolveJvmSupabaseConfig(
            systemPropertyLookup = { sysProps[it] },
            environmentLookup = { null }
        )

        assertEquals(SupabaseEnvironment.UNCONFIGURED, config.environment)
        assertFalse(config.isConfigured)
    }
}
