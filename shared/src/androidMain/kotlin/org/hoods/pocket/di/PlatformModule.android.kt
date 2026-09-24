package org.hoods.pocket.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.hoods.pocket.domain.service.ConnectivityObserver
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.UUID

private class AndroidDeviceIdProvider(private val context: Context) : DeviceIdProvider {
    private val prefs by lazy { context.getSharedPreferences("pocket_device", Context.MODE_PRIVATE) }
    override fun getDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }
}

private class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _isOnline = MutableStateFlow(checkCurrent())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val cm = connectivityManager
        if (cm != null) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }
                override fun onLost(network: Network) {
                    _isOnline.value = checkCurrent()
                }
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
            }
            try {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                cm.registerNetworkCallback(request, callback)
            } catch (e: Exception) {
                // Fallback to initial check if restrictions apply
            }
        }
    }

    private fun checkCurrent(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val active = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(active) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}

private var androidSupabaseConfig: SupabaseConfig = SupabaseConfig.unconfigured

fun initAndroidKoin(
    context: Context,
    supabaseConfig: SupabaseConfig = SupabaseConfig.unconfigured
) {
    androidSupabaseConfig = supabaseConfig
    initKoin {
        androidContext(context)
    }
}

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase(androidContext()) }
    single<SupabaseConfig> { androidSupabaseConfig }
    single<DeviceIdProvider> { AndroidDeviceIdProvider(androidContext()) }
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
}
