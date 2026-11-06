package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.DataRepository
import by.grinich.hscontrol.data.MonthlySettings
import by.grinich.hscontrol.utils.Calculations
import by.grinich.hscontrol.utils.MonthUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IotCategory(
    val id: String,
    val label: String,
    val percent: Double,
    val amount: Double
) {
    val bonus: Double get() = amount * percent / 100.0
}

sealed class IotState {
    object Loading : IotState()
    object NotAvailable : IotState()
    data class Error(val message: String) : IotState()
    data class Success(
        val version: Int,
        val month: String,
        val channel: String,
        val channelType: String,
        val categories: List<IotCategory>,
        val monthlySettings: MonthlySettings
    ) : IotState() {
        val totalBonus: Double get() = categories.sumOf { it.bonus }
    }
}

class IotViewModel : ViewModel() {

    private val _state = MutableStateFlow<IotState>(IotState.Loading)
    val state: StateFlow<IotState> = _state.asStateFlow()

    private var lastUserId: String? = null
    private var lastMonth: String? = null
    private var saveJob: Job? = null

    fun load(userId: String, month: String) {
        lastUserId = userId
        lastMonth = month
        viewModelScope.launch {
            _state.value = IotState.Loading
            try {
                val version = MonthUtils.motivationVersion(month)
                if (version == 1) {
                    _state.value = IotState.NotAvailable
                    return@launch
                }

                val settings = DataRepository.getMonthlySettings(userId, month)
                    ?: MonthlySettings(userId = userId, month = month)

                _state.value = buildSuccess(version, month, settings)
            } catch (e: Exception) {
                _state.value = IotState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    private fun buildSuccess(
        version: Int,
        month: String,
        settings: MonthlySettings
    ): IotState.Success {
        val channel = settings.channel
        val channelType = if (channel == "5 Element" || channel == "Electrosila") "OM" else "OP"

        val percentages = if (channelType == "OM")
            mapOf("Laptop" to 1.0, "Pad" to 1.0, "Watch" to 2.0, "Earbuds" to 2.0, "Other" to 2.0)
        else
            mapOf("Laptop" to 1.5, "Pad" to 1.5, "Watch" to 3.0, "Earbuds" to 3.0, "Other" to 3.0)

        val categories = listOf(
            IotCategory("Laptop", "💻 Ноутбуки", percentages["Laptop"]!!, settings.iotLaptop),
            IotCategory("Pad", "📱 Планшеты", percentages["Pad"]!!, settings.iotPad),
            IotCategory("Watch", "⌚ Часы", percentages["Watch"]!!, settings.iotWatch),
            IotCategory("Earbuds", "🎧 Наушники", percentages["Earbuds"]!!, settings.iotEarbuds),
            IotCategory("Other", "📦 Другое", percentages["Other"]!!, settings.iotOther)
        )

        return IotState.Success(
            version = version,
            month = month,
            channel = channel,
            channelType = channelType,
            categories = categories,
            monthlySettings = settings
        )
    }

    fun refresh() {
        lastUserId?.let { u -> lastMonth?.let { m -> load(u, m) } }
    }

    fun saveIot(categoryId: String, newAmount: Double) {
        val current = _state.value as? IotState.Success ?: return

        val newSettings = when (categoryId) {
            "Laptop" -> current.monthlySettings.copy(iotLaptop = newAmount)
            "Pad" -> current.monthlySettings.copy(iotPad = newAmount)
            "Watch" -> current.monthlySettings.copy(iotWatch = newAmount)
            "Earbuds" -> current.monthlySettings.copy(iotEarbuds = newAmount)
            "Other" -> current.monthlySettings.copy(iotOther = newAmount)
            else -> current.monthlySettings
        }

        // Локально пересобираем — итог обновится мгновенно
        _state.value = buildSuccess(current.version, current.month, newSettings)

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            try {
                delay(400)
                DataRepository.upsertMonthlySettings(newSettings)
            } catch (_: Exception) {}
        }
    }
}