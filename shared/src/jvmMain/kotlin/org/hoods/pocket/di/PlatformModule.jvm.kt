package org.hoods.pocket.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.hoods.pocket.domain.service.ConnectivityObserver
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.util.UUID

private class JvmDeviceIdProvider : DeviceIdProvider {
    private val id: String by lazy {
        val userHome = System.getProperty("user.home") ?: "."
        val appDir = File(userHome, ".pocketbudget")
        appDir.mkdirs()
        val file = File(appDir, "device_id")
        if (file.exists()) {
            val content = file.readText().trim()
            if (content.isNotEmpty()) content else {
                val newId = UUID.randomUUID().toString()
                file.writeText(newId)
                newId
            }
        } else {
            val newId = UUID.randomUUID().toString()
            file.writeText(newId)
            newId
        }
    }
    override fun getDeviceId(): String = id
}

private class JvmConnectivityObserver(
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : ConnectivityObserver {
    private val _isOnline = MutableStateFlow(checkInterfaces())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        coroutineScope.launch {
            while (isActive) {
                _isOnline.value = checkInterfaces()
                delay(3000)
            }
        }
    }

    private fun checkInterfaces(): Boolean {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return false
            interfaces.asSequence().any { it.isUp && !it.isLoopback && !it.isPointToPoint }
        } catch (e: Exception) {
            false
        }
    }
}

    fun resolveJvmSupabaseConfig(
        systemPropertyLookup: (String) -> String? = System::getProperty,
        environmentLookup: (String) -> String? = System::getenv
    ): SupabaseConfig {
        fun find(sysKeys: List<String>, envKeys: List<String>): String? {
            for (k in sysKeys) {
                val v = systemPropertyLookup(k)
                if (!v.isNullOrBlank()) return v
            }
            for (k in envKeys) {
                val v = environmentLookup(k)
                if (!v.isNullOrBlank()) return v
            }
            return null
        }

        val env = find(
            listOf("pocket.supabase.environment", "supabase.environment", "SUPABASE_ENVIRONMENT"),
            listOf("POCKET_SUPABASE_ENVIRONMENT", "SUPABASE_ENVIRONMENT")
        )
        val url = find(
            listOf("pocket.supabase.url", "supabase.url", "SUPABASE_URL"),
            listOf("POCKET_SUPABASE_URL", "SUPABASE_URL")
        )
        val key = find(
            listOf("pocket.supabase.key", "supabase.key", "SUPABASE_PUBLISHABLE_KEY", "supabase.publishableKey", "SUPABASE_KEY"),
            listOf("POCKET_SUPABASE_KEY", "SUPABASE_PUBLISHABLE_KEY", "SUPABASE_KEY")
        )

        return SupabaseConfig.resolve(env, url, key)
    }

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase() }
    single<SupabaseConfig> { resolveJvmSupabaseConfig() }
    single<DeviceIdProvider> { JvmDeviceIdProvider() }
    single<ConnectivityObserver> { JvmConnectivityObserver() }
}
