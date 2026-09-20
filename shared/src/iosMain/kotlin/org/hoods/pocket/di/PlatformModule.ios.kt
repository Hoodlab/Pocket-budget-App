package org.hoods.pocket.di

import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.sync.DeviceIdProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID

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

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase() }
    single<SupabaseConfig> { SupabaseConfig.local() }
    single<DeviceIdProvider> { IosDeviceIdProvider() }
}
