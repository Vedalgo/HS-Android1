package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.DataRepository
import by.grinich.hscontrol.data.HonorModelDto
import by.grinich.hscontrol.data.Sale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailySale(
    val date: String,
    val models: List<Pair<String, Int>>,
    val totalForDay: Int
)

data class ModelSaleStat(
    val model: String,
    val quantity: Int,
    val percent: Double
)

sealed class HistoryState {
    object Loading : HistoryState()
    data class Error(val message: String) : HistoryState()
    data class Success(
        val fromDate: String,
        val toDate: String,
        val totalHonor: Int,
        val totalCompetitors: Int,
        val totalAll: Int,
        val daysCount: Int,
        val avgPerDay: Double,
        val daily: List<DailySale>,
        val modelStats: List<ModelSaleStat>
    ) : HistoryState()
}

class SalesHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private val _models = MutableStateFlow<List<HonorModelDto>>(emptyList())
    val models: StateFlow<List<HonorModelDto>> = _models.asStateFlow()

    fun loadModels() {
        if (_models.value.isNotEmpty()) return
        viewModelScope.launch {
            try {
                _models.value = DataRepository.getHonorModels()
            } catch (_: Exception) {}
        }
    }

    /**
     * Период: today / month / custom / specific.
     * modelFilter: "all" или конкретная модель
     */
    fun load(
        userId: String,
        period: String,
        fromDate: String,
        toDate: String,
        specificDate: String,
        modelFilter: String
    ) {
        viewModelScope.launch {
            _state.value = HistoryState.Loading
            try {
                val (start, end) = calculateRange(period, fromDate, toDate, specificDate)

                val allSales = DataRepository.getSalesInRange(userId, start, end)

                val honorSales = allSales.filter {
                    it.brand == "Honor" && (modelFilter == "all" || it.model == modelFilter)
                }
                val competitorsSales = allSales.filter { it.brand != "Honor" }

                val totalHonor = honorSales.sumOf { it.quantity }
                val totalCompetitors = competitorsSales.sumOf { it.quantity }
                val totalAll = totalHonor + totalCompetitors

                // По дням
                val salesByDate = mutableMapOf<String, MutableMap<String, Int>>()
                honorSales.forEach { sale ->
                    val day = sale.saleDate
                    val model = sale.model ?: sale.brand
                    salesByDate.getOrPut(day) { mutableMapOf() }
                    salesByDate[day]!![model] = (salesByDate[day]!![model] ?: 0) + sale.quantity
                }
                val daily = salesByDate.entries
                    .sortedByDescending { it.key }
                    .map { (date, modelsMap) ->
                        val list = modelsMap.entries
                            .map { it.key to it.value }
                            .sortedByDescending { it.second }
                        DailySale(date, list, list.sumOf { it.second })
                    }

                val daysCount = daily.size
                val avgPerDay = if (daysCount > 0) totalHonor.toDouble() / daysCount else 0.0

                // По моделям
                val modelsMap = mutableMapOf<String, Int>()
                honorSales.forEach { sale ->
                    val model = sale.model ?: sale.brand
                    modelsMap[model] = (modelsMap[model] ?: 0) + sale.quantity
                }
                val modelStats = modelsMap.entries
                    .map { (model, qty) ->
                        ModelSaleStat(
                            model = model,
                            quantity = qty,
                            percent = if (totalHonor > 0) qty * 100.0 / totalHonor else 0.0
                        )
                    }
                    .sortedByDescending { it.quantity }

                _state.value = HistoryState.Success(
                    fromDate = start,
                    toDate = end,
                    totalHonor = totalHonor,
                    totalCompetitors = totalCompetitors,
                    totalAll = totalAll,
                    daysCount = daysCount,
                    avgPerDay = avgPerDay,
                    daily = daily,
                    modelStats = modelStats
                )
            } catch (e: Exception) {
                _state.value = HistoryState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    private fun calculateRange(
        period: String,
        fromDate: String,
        toDate: String,
        specificDate: String
    ): Pair<String, String> {
        val today = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        return when (period) {
            "today" -> {
                val t = sdf.format(today.time)
                t to t
            }
            "month" -> {
                today.set(Calendar.DAY_OF_MONTH, 1)
                val start = sdf.format(today.time)
                today.set(Calendar.DAY_OF_MONTH, today.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = sdf.format(today.time)
                start to end
            }
            "custom" -> fromDate to toDate
            "specific" -> specificDate to specificDate
            else -> {
                val t = sdf.format(today.time)
                t to t
            }
        }
    }
}