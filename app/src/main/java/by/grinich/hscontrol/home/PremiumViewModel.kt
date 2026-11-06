package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.*
import by.grinich.hscontrol.utils.Calculations
import by.grinich.hscontrol.utils.MonthUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PremiumState {
    object Loading : PremiumState()
    data class Error(val message: String) : PremiumState()
    data class Success(
        val version: Int,
        val month: String,
        val premiumItems: List<PremiumItem>,
        val focusItems: List<FocusItem>,
        val premiumSettings: PremiumSettings?,
        val premiumResult: PremiumResult?,
        val totalHonor: Int,
        val totalAll: Int,
        val channel: String,
        val competitors: Int,
        val honorModels: List<HonorModelDto>
    ) : PremiumState()
}

class PremiumViewModel : ViewModel() {

    private val _state = MutableStateFlow<PremiumState>(PremiumState.Loading)
    val state: StateFlow<PremiumState> = _state.asStateFlow()

    private var lastUserId: String? = null
    private var lastMonth: String? = null

    fun load(userId: String, month: String) {
        lastUserId = userId
        lastMonth = month
        viewModelScope.launch {
            val current = _state.value
            if (current !is PremiumState.Success || current.month != month) {
                _state.value = PremiumState.Loading
            }

            try {
                val version = MonthUtils.motivationVersion(month)
                val sales = DataRepository.getSales(userId, month)
                val totalHonor = sales.filter { it.brand == "Honor" }.sumOf { it.quantity }
                val totalAll = sales.sumOf { it.quantity }

                val settings = DataRepository.getMonthlySettings(userId, month)
                val channel = settings?.channel ?: "5 Element"
                val competitors = settings?.competitorsCount ?: 0

                val premiumItems = if (version == 3) DataRepository.getPremiumItems(userId, month) else emptyList()
                val focusItems = if (version <= 2) DataRepository.getFocusItems(userId, month) else emptyList()
                val premiumSettings = if (version == 3) DataRepository.getPremiumSettings(userId, month) else null

                val premiumResult = if (version == 3) {
                    Calculations.calculatePremium(premiumItems, totalHonor, totalAll, channel, competitors, premiumSettings)
                } else null

                val models = DataRepository.getHonorModels()

                _state.value = PremiumState.Success(
                    version = version, month = month,
                    premiumItems = premiumItems.sortedByDescending { it.rate },
                    focusItems = focusItems,
                    premiumSettings = premiumSettings,
                    premiumResult = premiumResult,
                    totalHonor = totalHonor, totalAll = totalAll,
                    channel = channel, competitors = competitors,
                    honorModels = models
                )
            } catch (e: Exception) {
                _state.value = PremiumState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun refresh() {
        lastUserId?.let { u -> lastMonth?.let { m -> load(u, m) } }
    }

    fun addPremiumItem(userId: String, month: String, name: String, rate: Double, fixedBonus: Double, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                DataRepository.addPremiumItem(NewPremiumItem(userId = userId, month = month, name = name, rate = rate, fixedBonus = fixedBonus, sold = 0))
                refresh()
                onDone()
            } catch (e: Exception) { onError(e.message ?: "Ошибка сохранения") }
        }
    }

    fun updatePremiumSold(itemId: String, delta: Int) {
        val current = _state.value as? PremiumState.Success ?: return
        val idx = current.premiumItems.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val oldItem = current.premiumItems[idx]
        val newSold = (oldItem.sold + delta).coerceAtLeast(0)
        if (newSold == oldItem.sold) return

        val newItem = oldItem.copy(sold = newSold)
        val updatedList = current.premiumItems.toMutableList().apply { this[idx] = newItem }
        val newResult = current.premiumResult?.let {
            Calculations.calculatePremium(updatedList, current.totalHonor, current.totalAll, current.channel, current.competitors, current.premiumSettings)
        }
        _state.value = current.copy(premiumItems = updatedList, premiumResult = newResult)
        viewModelScope.launch { try { DataRepository.updatePremiumSold(itemId, newSold) } catch (_: Exception) {} }
    }

    fun updatePremiumRate(itemId: String, newRate: Double) {
        val current = _state.value as? PremiumState.Success ?: return
        val idx = current.premiumItems.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val oldItem = current.premiumItems[idx]
        if (newRate == oldItem.rate) return
        val newItem = oldItem.copy(rate = newRate)
        val updatedList = current.premiumItems.toMutableList().apply { this[idx] = newItem }
        val newResult = current.premiumResult?.let {
            Calculations.calculatePremium(updatedList, current.totalHonor, current.totalAll, current.channel, current.competitors, current.premiumSettings)
        }
        _state.value = current.copy(premiumItems = updatedList, premiumResult = newResult)
        viewModelScope.launch { try { DataRepository.updatePremiumRate(itemId, newRate) } catch (_: Exception) {} }
    }

    fun updatePremiumFixed(itemId: String, newFixed: Double) {
        val current = _state.value as? PremiumState.Success ?: return
        val idx = current.premiumItems.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val oldItem = current.premiumItems[idx]
        if (newFixed == oldItem.fixedBonus) return
        val newItem = oldItem.copy(fixedBonus = newFixed)
        val updatedList = current.premiumItems.toMutableList().apply { this[idx] = newItem }
        val newResult = current.premiumResult?.let {
            Calculations.calculatePremium(updatedList, current.totalHonor, current.totalAll, current.channel, current.competitors, current.premiumSettings)
        }
        _state.value = current.copy(premiumItems = updatedList, premiumResult = newResult)
        viewModelScope.launch { try { DataRepository.updatePremiumFixed(itemId, newFixed) } catch (_: Exception) {} }
    }

    fun deletePremiumItem(id: String) {
        val current = _state.value as? PremiumState.Success ?: return
        val updatedList = current.premiumItems.filter { it.id != id }
        val newResult = current.premiumResult?.let {
            Calculations.calculatePremium(updatedList, current.totalHonor, current.totalAll, current.channel, current.competitors, current.premiumSettings)
        }
        _state.value = current.copy(premiumItems = updatedList, premiumResult = newResult)
        viewModelScope.launch { try { DataRepository.deletePremiumItem(id) } catch (_: Exception) {} }
    }

    fun savePremiumSettings(userId: String, month: String, bonus1: Double, bonus2: Double, target1: Double, target2: Double, extraPerPercent: Double, onDone: () -> Unit) {
        val current = _state.value as? PremiumState.Success ?: return
        val newSettings = current.premiumSettings?.copy(bonus1 = bonus1, bonus2 = bonus2, target1 = target1, target2 = target2, extraPerPercent = extraPerPercent)
            ?: PremiumSettings(userId = userId, month = month, bonus1 = bonus1, bonus2 = bonus2, target1 = target1, target2 = target2, extraPerPercent = extraPerPercent)
        val newResult = current.premiumResult?.let {
            Calculations.calculatePremium(current.premiumItems, current.totalHonor, current.totalAll, current.channel, current.competitors, newSettings)
        }
        _state.value = current.copy(premiumSettings = newSettings, premiumResult = newResult)
        viewModelScope.launch {
            try {
                DataRepository.upsertPremiumSettings(newSettings)
                onDone()
            } catch (_: Exception) {}
        }
    }

    fun addFocusItem(userId: String, month: String, category: String, name: String, bonus: Double, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                DataRepository.addFocusItem(NewFocusItem(userId = userId, month = month, category = category, name = name, bonus = bonus, sold = 0))
                refresh()
                onDone()
            } catch (e: Exception) { onError(e.message ?: "Ошибка сохранения") }
        }
    }

    fun updateFocusSold(itemId: String, delta: Int) {
        val current = _state.value as? PremiumState.Success ?: return
        val idx = current.focusItems.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val oldItem = current.focusItems[idx]
        val newSold = (oldItem.sold + delta).coerceAtLeast(0)
        if (newSold == oldItem.sold) return
        val newItem = oldItem.copy(sold = newSold)
        val updatedList = current.focusItems.toMutableList().apply { this[idx] = newItem }
        _state.value = current.copy(focusItems = updatedList)
        viewModelScope.launch { try { DataRepository.updateFocusSold(itemId, newSold) } catch (_: Exception) {} }
    }

    fun updateFocusBonus(itemId: String, newBonus: Double) {
        val current = _state.value as? PremiumState.Success ?: return
        val idx = current.focusItems.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val oldItem = current.focusItems[idx]
        if (newBonus == oldItem.bonus) return
        val newItem = oldItem.copy(bonus = newBonus)
        val updatedList = current.focusItems.toMutableList().apply { this[idx] = newItem }
        _state.value = current.copy(focusItems = updatedList)
        viewModelScope.launch { try { DataRepository.updateFocusBonus(itemId, newBonus) } catch (_: Exception) {} }
    }

    fun deleteFocusItem(id: String) {
        val current = _state.value as? PremiumState.Success ?: return
        val updatedList = current.focusItems.filter { it.id != id }
        _state.value = current.copy(focusItems = updatedList)
        viewModelScope.launch { try { DataRepository.deleteFocusItem(id) } catch (_: Exception) {} }
    }
}