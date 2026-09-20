package org.hoods.pocket.di

import org.hoods.pocket.data.local.database.AppDatabase
import org.hoods.pocket.data.local.database.getSharedAppDatabase
import org.hoods.pocket.data.remote.SupabaseConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

import android.content.Context

actual val platformModule: Module = module {
    single<AppDatabase> { getSharedAppDatabase(androidContext()) }
    single<SupabaseConfig> { SupabaseConfig.local() }
}

fun initAndroidKoin(context: Context) {
    initKoin {
        androidContext(context)
    }
}
