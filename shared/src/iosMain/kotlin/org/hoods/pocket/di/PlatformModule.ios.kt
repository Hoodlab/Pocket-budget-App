package org.hoods.pocket.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.hoods.pocket.domain.service.ConnectivityObserver
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

private class IosDeviceIdProvider : DeviceIdProvider {
    override fun getDeviceId(): String {
        val defaults = NSUserDefaults.standardUserDefaults
        var id = defaults.stringForKey("pocket_device_id")
        if (id.isNullOrBlank()) {
            id = NSUUID().UUIDString
            defaults.setObject(id, "pocket_device_id")
        }
        return id
    }
}

private class IosConnectivityObserver : ConnectivityObserver {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            _isOnline.value = (status == nw_path_status_satisfied)
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }
}

fun resolveIosSupabaseConfig(): SupabaseConfig {
    val bundle = platform.Foundation.NSBundle.mainBundle
    val env = bundle.objectForInfoDictionaryKey("SUPABASE_ENVIRONMENT") as? String
    val url = bundle.objectForInfoDictionaryKey("SUPABASE_URL") as? String
    val key = bundle.objectForInfoDictionaryKey("SUPABASE_PUBLISHABLE_KEY") as? String

    fun clean(value: String?): String? {
        if (value == null || value.startsWith("$(")) return null
        return value.trim().takeIf { it.isNotEmpty() }
    }

    return SupabaseConfig.resolve(
        rawEnvironment = clean(env),
        rawUrl = clean(url),
        rawPublishableKey = clean(key)
    )
}

private var iosSupabaseConfigOverride: SupabaseConfig? = null

fun initIosKoin(configOverride: SupabaseConfig? = null) {
    iosSupabaseConfigOverride = configOverride
    initKoin()
}

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase() }
    single<SupabaseConfig> { iosSupabaseConfigOverride ?: resolveIosSupabaseConfig() }
    single<DeviceIdProvider> { IosDeviceIdProvider() }
    single<ConnectivityObserver> { IosConnectivityObserver() }
}
