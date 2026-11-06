package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.DataRepository
import by.grinich.hscontrol.data.MonthlySettings
import by.grinich.hscontrol.data.SalaryResult
import by.grinich.hscontrol.data.Sale
import by.grinich.hscontrol.utils.Calculations
import by.grinich.hscontrol.utils.MonthUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrandCount(val brand: String, val quantity: Int)

sealed class SalaryState {
    object Loading : SalaryState()
    data class Error(val message: String) : SalaryState()
    data class Success(
        val version: Int,
        val month: String,
        val salary: SalaryResult,
        val settings: MonthlySettings,
        val sales: List<Sale>,
        val totalHonor: Int,
        val totalAll: Int,
        val currentShare: Int,
        val premiumPoints: Double,
        val premiumPercent: Double,
        val brandStats: List<BrandCount>
    ) : SalaryState() {
        val advanceAmount: Double get() = settings.advanceAmount
        val adjustedSalary: Double get() = settings.adjustedSalary
        val testPassed: Boolean get() = settings.testPassed
        val payment10th: Double get() = salary.finalPayment
        val paymentAfterAdjustment: Double
            get() = if (adjustedSalary > 0) maxOf(0.0, adjustedSalary - advanceAmount) else payment10th
    }
}

class SalaryViewModel : ViewModel() {

    private val _state = MutableStateFlow<SalaryState>(SalaryState.Loading)
    val state: StateFlow<SalaryState> = _state.asStateFlow()

    private var lastUserId: String? = null
    private var lastMonth: String? = null
    private var saveJob: Job? = null

    fun load(userId: String, month: String) {
        lastUserId = userId
        lastMonth = month
        viewModelScope.launch {
            val current = _state.value
            if (current !is SalaryState.Success || current.month != month) {
                _state.value = SalaryState.Loading
            }
            try {
                val version = MonthUtils.motivationVersion(month)
                val settings = DataRepository.getMonthlySettings(userId, month) ?: MonthlySettings(userId = userId, month = month)
                val sales = DataRepository.getSales(userId, month)
                _state.value = buildSuccess(version, month, settings, sales)
            } catch (e: Exception) {
                _state.value = SalaryState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    private suspend fun buildSuccess(version: Int, month: String, settings: MonthlySettings, sales: List<Sale>): SalaryState.Success {
        val totalHonor = sales.filter { it.brand == "Honor" }.sumOf { it.quantity }
        val totalAll = sales.sumOf { it.quantity }
        val currentShare = if (totalAll > 0) (totalHonor * 100.0 / totalAll).toInt() else 0

        val share = Calculations.calculateShare(
            totalHonor = totalHonor, totalAll = totalAll,
            channel = settings.channel, competitors = settings.competitorsCount,
            targetBonusOverride = settings.targetBonus, overBonusOverride = settings.overBonus
        )

        var premiumPayout = 0.0
        var premiumPoints = 0.0
        var premiumPercent = 0.0
        var focusBonus = 0.0
        var iotBonus = 0.0
        var sharePayment = share.sharePayment

        when (version) {
            3 -> {
                val premiumItems = DataRepository.getPremiumItems(settings.userId, settings.month)
                val premiumSettings = DataRepository.getPremiumSettings(settings.userId, settings.month)
                val premium = Calculations.calculatePremium(
                    premiumItems, totalHonor, totalAll, settings.channel, settings.competitorsCount, premiumSettings
                )
                premiumPayout = premium.payout
                premiumPoints = premium.points
                premiumPercent = premium.percent
                iotBonus = Calculations.calculateIot(settings)
            }
            2 -> {
                val focusItems = DataRepository.getFocusItems(settings.userId, settings.month).filter { it.category == "Смартфоны" }
                focusBonus = Calculations.calculateFocus(focusItems)
                iotBonus = Calculations.calculateIot(settings)
            }
            1 -> {
                val plan = settings.planQuantity
                val basePlan = settings.basePlanPayment
                val overRate = settings.overplanBonus
                sharePayment = if (totalHonor >= plan) basePlan + (totalHonor - plan) * overRate else basePlan * totalHonor / plan
                val focusItems = DataRepository.getFocusItems(settings.userId, settings.month)
                focusBonus = Calculations.calculateFocus(focusItems)
            }
        }

        val salary = Calculations.calculateSalary(
            version = version, settings = settings, sharePayment = sharePayment,
            premiumPayout = premiumPayout, iotBonus = iotBonus, focusBonus = focusBonus
        )

        val brandMap = mutableMapOf<String, Int>()
        sales.forEach { s -> brandMap[s.brand] = (brandMap[s.brand] ?: 0) + s.quantity }
        val brandStats = brandMap.entries.map { BrandCount(it.key, it.value) }.sortedByDescending { it.quantity }

        return SalaryState.Success(
            version = version, month = month, salary = salary, settings = settings, sales = sales,
            totalHonor = totalHonor, totalAll = totalAll, currentShare = currentShare,
            premiumPoints = premiumPoints, premiumPercent = premiumPercent, brandStats = brandStats
        )
    }

    fun refresh() {
        lastUserId?.let { u -> lastMonth?.let { m -> load(u, m) } }
    }

    private fun updateSettings(transform: (MonthlySettings) -> MonthlySettings) {
        val s = _state.value as? SalaryState.Success ?: return
        val newSettings = transform(s.settings)
        viewModelScope.launch {
            try {
                val newState = buildSuccess(s.version, s.month, newSettings, s.sales)
                _state.value = newState
            } catch (_: Exception) {
                _state.value = s.copy(settings = newSettings)
            }
        }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            try {
                delay(400)
                DataRepository.upsertMonthlySettings(newSettings)
            } catch (_: Exception) {}
        }
    }

    fun setTestPassed(passed: Boolean) = updateSettings { it.copy(testPassed = passed) }
    fun setAdvanceAmount(amount: Double) = updateSettings { it.copy(advanceAmount = amount.coerceAtLeast(0.0)) }
    fun setAdjustedSalary(amount: Double) = updateSettings { it.copy(adjustedSalary = amount.coerceAtLeast(0.0)) }
}