package org.hoods.pocket.di

import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.data.sync.DeviceIdProvider
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

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase() }
    single<SupabaseConfig> { SupabaseConfig.local() }
    single<DeviceIdProvider> { JvmDeviceIdProvider() }
}
