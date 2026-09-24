package org.hoods.pocket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.hoods.pocket.data.remote.SupabaseConfig
import org.hoods.pocket.di.initAndroidKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val supabaseConfig = SupabaseConfig.resolve(
            rawEnvironment = BuildConfig.SUPABASE_ENVIRONMENT,
            rawUrl = BuildConfig.SUPABASE_URL,
            rawPublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        )

        initAndroidKoin(this, supabaseConfig)

        setContent {
            App()
        }
    }
}