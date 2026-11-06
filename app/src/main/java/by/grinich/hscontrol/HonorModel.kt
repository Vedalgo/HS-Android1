package by.grinich.hscontrol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HonorModel(
    val id: String,
    @SerialName("model_name") val modelName: String,
    @SerialName("sort_order") val sortOrder: Int = 0
)