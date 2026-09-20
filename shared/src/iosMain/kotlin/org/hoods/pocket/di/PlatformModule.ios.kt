package org.hoods.pocket.di

import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase() }
    single<SupabaseConfig> { SupabaseConfig.local() }
}
