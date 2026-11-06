package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.*
import by.grinich.hscontrol.utils.MonthUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SettingsState {
    object Loading : SettingsState()
    data class Error(val message: String) : SettingsState()
    data class Success(
        val version: Int,
        val month: String,
        val userId: String,
        val role: String,
        val settings: MonthlySettings,
        val honorModels: List<HonorModelDto>,
        val sales: List<Sale>
    ) : SettingsState()
}

class SettingsViewModel : ViewModel() {

    private val _state = MutableStateFlow<SettingsState>(SettingsState.Loading)
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var lastUserId: String? = null
    private var lastMonth: String? = null
    private var lastRole: String = "promoter"
    private var saveJob: Job? = null

    fun load(userId: String, month: String, role: String) {
        lastUserId = userId
        lastMonth = month
        lastRole = role
        viewModelScope.launch {
            _state.value = SettingsState.Loading
            try {
                val version = MonthUtils.motivationVersion(month)
                val settings = DataRepository.getMonthlySettings(userId, month)
                    ?: MonthlySettings(userId = userId, month = month)
                val models = DataRepository.getHonorModels()
                val sales = DataRepository.getSales(userId, month)

                _state.value = SettingsState.Success(
                    version = version,
                    month = month,
                    userId = userId,
                    role = role,
                    settings = settings,
                    honorModels = models,
                    sales = sales
                )
            } catch (e: Exception) {
                _state.value = SettingsState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun refresh() {
        lastUserId?.let { u -> lastMonth?.let { m -> load(u, m, lastRole) } }
    }

    /**
     * Автосохранение настроек с debounce 600мс.
     * Локально обновляем сразу (UI мгновенно реагирует), на сервер — с задержкой.
     */
    fun autoSave(
        channel: String,
        competitors: Int,
        baseSalary: Double,
        mobilePayment: Double,
        targetBonus: Double?,
        overBonus: Double?,
        testBonus: Double,
        onError: (String) -> Unit = {}
    ) {
        val s = _state.value as? SettingsState.Success ?: return
        val newSettings = s.settings.copy(
            channel = channel,
            competitorsCount = competitors,
            baseSalary = baseSalary,
            mobilePayment = mobilePayment,
            targetBonus = targetBonus,
            overBonus = overBonus,
            testBonus = testBonus
        )

        // Локально сразу
        _state.value = s.copy(settings = newSettings)

        // На сервер с debounce
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            try {
                delay(600)
                DataRepository.upsertMonthlySettings(newSettings)
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка сохранения")
            }
        }
    }

    fun deleteHonorModel(id: String) {
        viewModelScope.launch {
            try {
                DataRepository.deleteHonorModel(id)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun addHonorModel(name: String, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val s = _state.value as? SettingsState.Success ?: return@launch
                val maxOrder = s.honorModels.maxOfOrNull { it.sortOrder } ?: 0
                DataRepository.addHonorModel(name, maxOrder + 1)
                refresh()
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка добавления")
            }
        }
    }

    fun moveModelUp(id: String) {
        val s = _state.value as? SettingsState.Success ?: return
        val list = s.honorModels.toList()
        val index = list.indexOfFirst { it.id == id }
        if (index <= 0) return
        val a = list[index]
        val b = list[index - 1]
        viewModelScope.launch {
            try {
                DataRepository.swapHonorModels(a.id, a.sortOrder, b.id, b.sortOrder)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun moveModelDown(id: String) {
        val s = _state.value as? SettingsState.Success ?: return
        val list = s.honorModels.toList()
        val index = list.indexOfFirst { it.id == id }
        if (index < 0 || index >= list.size - 1) return
        val a = list[index]
        val b = list[index + 1]
        viewModelScope.launch {
            try {
                DataRepository.swapHonorModels(a.id, a.sortOrder, b.id, b.sortOrder)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun deleteSale(saleId: String) {
        viewModelScope.launch {
            try {
                DataRepository.deleteSale(saleId)
                refresh()
            } catch (_: Exception) {}
        }
    }

    fun deleteAllSales(onDone: () -> Unit) {
        val s = _state.value as? SettingsState.Success ?: return
        viewModelScope.launch {
            try {
                s.sales.forEach { sale ->
                    sale.id?.let { DataRepository.deleteSale(it) }
                }
                refresh()
                onDone()
            } catch (_: Exception) {}
        }
    }
}