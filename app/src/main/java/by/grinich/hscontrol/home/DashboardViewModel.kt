package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.*
import by.grinich.hscontrol.utils.MonthUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    object Loading : DashboardState()
    data class Error(val message: String) : DashboardState()
    data class Success(
        val version: Int,
        val month: String,
        val sales: List<Sale>,
        val salary: SalaryResult?,
        val share: ShareResult?,
        val premium: PremiumResult?,
        val brandStats: List<BrandStat>,
        val totalHonor: Int,
        val totalAll: Int
    ) : DashboardState()
}

data class BrandStat(
    val brand: String,
    val quantity: Int,
    val percent: Double
)

class DashboardViewModel : ViewModel() {

    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var lastUserId: String? = null
    private var lastMonth: String? = null

    fun load(userId: String, month: String) {
        lastUserId = userId
        lastMonth = month
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                val version = MonthUtils.motivationVersion(month)
                val sales = DataRepository.getSales(userId, month)
                val salary = DataRepository.calculateSalary(userId, month)
                val share = DataRepository.calculateShare(userId, month)
                val premium = if (version == 3) {
                    DataRepository.calculatePremium(userId, month)
                } else null

                val totalHonor = sales.filter { it.brand == "Honor" }.sumOf { it.quantity }
                val totalAll = sales.sumOf { it.quantity }

                val brandMap = mutableMapOf<String, Int>()
                sales.forEach { s ->
                    val brand = if (s.brand == "Xiaomi" || s.brand == "POCO") "Xiaomi+POCO" else s.brand
                    brandMap[brand] = (brandMap[brand] ?: 0) + s.quantity
                }
                val brandStats = brandMap.entries
                    .map {
                        BrandStat(
                            brand = it.key,
                            quantity = it.value,
                            percent = if (totalAll > 0) it.value * 100.0 / totalAll else 0.0
                        )
                    }
                    .sortedByDescending { it.quantity }

                _state.value = DashboardState.Success(
                    version = version,
                    month = month,
                    sales = sales,
                    salary = salary,
                    share = share,
                    premium = premium,
                    brandStats = brandStats,
                    totalHonor = totalHonor,
                    totalAll = totalAll
                )
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun refresh() {
        lastUserId?.let { u -> lastMonth?.let { m -> load(u, m) } }
    }
}