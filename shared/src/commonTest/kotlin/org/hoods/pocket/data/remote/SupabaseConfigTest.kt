package org.hoods.pocket.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseConfigTest {

    @Test
    fun resolve_returns_unconfigured_when_environment_is_null_or_empty() {
        val configNull = SupabaseConfig.resolve(null, "http://127.0.0.1:54321", "test-key")
        val configBlank = SupabaseConfig.resolve("   ", "http://127.0.0.1:54321", "test-key")

        assertEquals(SupabaseEnvironment.UNCONFIGURED, configNull.environment)
        assertFalse(configNull.isConfigured)
        assertNull(configNull.url)
        assertNull(configNull.publishableKey)

        assertEquals(SupabaseEnvironment.UNCONFIGURED, configBlank.environment)
        assertFalse(configBlank.isConfigured)
    }

    @Test
    fun resolve_returns_unconfigured_when_environment_is_explicitly_unconfigured() {
        val config = SupabaseConfig.resolve("UNCONFIGURED", "http://127.0.0.1:54321", "test-key")

        assertEquals(SupabaseEnvironment.UNCONFIGURED, config.environment)
        assertFalse(config.isConfigured)
        assertNull(config.url)
        assertNull(config.publishableKey)
    }

    @Test
    fun resolve_returns_unconfigured_when_url_or_key_is_blank() {
        val missingUrl = SupabaseConfig.resolve("LOCAL", "", "test-key")
        val nullUrl = SupabaseConfig.resolve("LOCAL", null, "test-key")
        val missingKey = SupabaseConfig.resolve("LOCAL", "http://127.0.0.1:54321", "")
        val nullKey = SupabaseConfig.resolve("LOCAL", "http://127.0.0.1:54321", null)

        assertEquals(SupabaseEnvironment.UNCONFIGURED, missingUrl.environment)
        assertEquals(SupabaseEnvironment.UNCONFIGURED, nullUrl.environment)
        assertEquals(SupabaseEnvironment.UNCONFIGURED, missingKey.environment)
        assertEquals(SupabaseEnvironment.UNCONFIGURED, nullKey.environment)
        assertFalse(missingUrl.isConfigured)
        assertFalse(missingKey.isConfigured)
    }

    @Test
    fun resolve_returns_local_with_valid_http_or_https_url() {
        val httpConfig = SupabaseConfig.resolve("LOCAL", "http://127.0.0.1:54321", "test-key")
        val httpsConfig = SupabaseConfig.resolve("local", "https://localhost:54321", "test-key")

        assertEquals(SupabaseEnvironment.LOCAL, httpConfig.environment)
        assertEquals("http://127.0.0.1:54321", httpConfig.url)
        assertEquals("test-key", httpConfig.publishableKey)
        assertTrue(httpConfig.isConfigured)

        assertEquals(SupabaseEnvironment.LOCAL, httpsConfig.environment)
        assertEquals("https://localhost:54321", httpsConfig.url)
        assertEquals("test-key", httpsConfig.publishableKey)
        assertTrue(httpsConfig.isConfigured)
    }

    @Test
    fun resolve_returns_unconfigured_when_local_has_invalid_scheme() {
        val ftpConfig = SupabaseConfig.resolve("LOCAL", "ftp://127.0.0.1:54321", "test-key")
        val noScheme = SupabaseConfig.resolve("LOCAL", "127.0.0.1:54321", "test-key")

        assertEquals(SupabaseEnvironment.UNCONFIGURED, ftpConfig.environment)
        assertFalse(ftpConfig.isConfigured)
        assertEquals(SupabaseEnvironment.UNCONFIGURED, noScheme.environment)
        assertFalse(noScheme.isConfigured)
    }

    @Test
    fun resolve_returns_hosted_dev_with_valid_https_url() {
        val config = SupabaseConfig.resolve("HOSTED_DEV", "https://xyz.supabase.co", "test-key")

        assertEquals(SupabaseEnvironment.HOSTED_DEV, config.environment)
        assertEquals("https://xyz.supabase.co", config.url)
        assertEquals("test-key", config.publishableKey)
        assertTrue(config.isConfigured)
    }

    @Test
    fun resolve_returns_unconfigured_when_hosted_dev_uses_non_https_url() {
        val httpConfig = SupabaseConfig.resolve("HOSTED_DEV", "http://xyz.supabase.co", "test-key")

        assertEquals(SupabaseEnvironment.UNCONFIGURED, httpConfig.environment)
        assertFalse(httpConfig.isConfigured)
    }

    @Test
    fun resolve_trims_whitespace_and_handles_case_insensitivity() {
        val config = SupabaseConfig.resolve(
            rawEnvironment = "  hosted_dev  ",
            rawUrl = "  https://xyz.supabase.co  ",
            rawPublishableKey = "  test-publishable-key  "
        )

        assertEquals(SupabaseEnvironment.HOSTED_DEV, config.environment)
        assertEquals("https://xyz.supabase.co", config.url)
        assertEquals("test-publishable-key", config.publishableKey)
        assertTrue(config.isConfigured)
    }

    @Test
    fun unconfigured_instance_is_consistently_falsy() {
        val unconfigured = SupabaseConfig.unconfigured
        assertEquals(SupabaseEnvironment.UNCONFIGURED, unconfigured.environment)
        assertNull(unconfigured.url)
        assertNull(unconfigured.publishableKey)
        assertFalse(unconfigured.isConfigured)
    }
}
