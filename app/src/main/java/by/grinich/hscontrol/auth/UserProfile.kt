package by.grinich.hscontrol.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val role: String = "promoter",
    @SerialName("supervisor_id") val supervisorId: String? = null,
    val active: Boolean = true
) {
    val roleDisplay: String
        get() = when (role) {
            "developer" -> "Разработчик"
            "supervisor" -> "Супервайзер"
            "promoter" -> "Промоутер"
            else -> role
        }
}