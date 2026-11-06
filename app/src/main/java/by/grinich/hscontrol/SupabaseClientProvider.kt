package by.grinich.hscontrol

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            // 🔑 ГЛАВНОЕ ИСПРАВЛЕНИЕ:
            // encodeDefaults = true → ВСЕ поля всегда отправляются на сервер,
            // даже если они равны значениям по умолчанию (1, 0, false, null).
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
                explicitNulls = true
            })

            install(Auth) {
                autoLoadFromStorage = true
                alwaysAutoRefresh = true
            }
            install(Postgrest)
        }
    }
}