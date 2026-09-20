package org.hoods.pocket.di

import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

import android.content.Context
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

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase(androidContext()) }
    single<SupabaseConfig> { SupabaseConfig.local() }
    single<DeviceIdProvider> { AndroidDeviceIdProvider(androidContext()) }
}

fun initAndroidKoin(context: Context) {
    initKoin {
        androidContext(context)
    }
}
