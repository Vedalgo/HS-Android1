package by.grinich.hscontrol.auth

import by.grinich.hscontrol.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {

    private val client get() = SupabaseClientProvider.client

    suspend fun signIn(email: String, password: String): UserProfile {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return fetchCurrentProfile()
            ?: throw IllegalStateException("Профиль пользователя не найден в базе")
    }

    suspend fun signUp(email: String, password: String, fullName: String): UserProfile {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
                put("role", "promoter")
            }
        }
        return fetchCurrentProfile()
            ?: throw IllegalStateException("Регистрация прошла, но профиль не создался. Проверьте триггер handle_new_user() в Supabase.")
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun getCurrentProfileOrNull(): UserProfile? {
        try {
            client.auth.awaitInitialization()
        } catch (_: Exception) { }

        val user = client.auth.currentUserOrNull() ?: return null
        return fetchCurrentProfile(user.id)
    }

    private suspend fun fetchCurrentProfile(userId: String? = null): UserProfile? {
        val id = userId ?: client.auth.currentUserOrNull()?.id ?: return null
        return try {
            client.postgrest["profiles"]
                .select {
                    filter { eq("id", id) }
                }
                .decodeSingleOrNull<UserProfile>()
        } catch (e: Exception) {
            null
        }
    }
}