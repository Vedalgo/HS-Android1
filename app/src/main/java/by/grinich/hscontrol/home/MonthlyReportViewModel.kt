package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.DataRepository
import by.grinich.hscontrol.data.MonthlyReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportState {
    object Loading : ReportState()
    data class Error(val message: String) : ReportState()
    data class Success(val month: String, val report: MonthlyReport) : ReportState()
}

class MonthlyReportViewModel : ViewModel() {

    private val _state = MutableStateFlow<ReportState>(ReportState.Loading)
    val state: StateFlow<ReportState> = _state.asStateFlow()

    fun load(userId: String, month: String) {
        viewModelScope.launch {
            _state.value = ReportState.Loading
            try {
                val report = DataRepository.getMonthlyReport(userId, month)
                _state.value = ReportState.Success(month, report)
            } catch (e: Exception) {
                _state.value = ReportState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }
}