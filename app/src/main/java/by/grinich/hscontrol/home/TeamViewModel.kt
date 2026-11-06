package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.DataRepository
import by.grinich.hscontrol.data.PromoterStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TeamState {
    object Loading : TeamState()
    data class Error(val message: String) : TeamState()
    data class Success(
        val month: String,
        val promoters: List<PromoterStats>
    ) : TeamState()
}

class TeamViewModel : ViewModel() {

    private val _state = MutableStateFlow<TeamState>(TeamState.Loading)
    val state: StateFlow<TeamState> = _state.asStateFlow()

    private var lastMonth: String? = null

    fun load(month: String) {
        lastMonth = month
        viewModelScope.launch {
            _state.value = TeamState.Loading
            try {
                val promoters = DataRepository.getPromotersStats(month)
                _state.value = TeamState.Success(month, promoters)
            } catch (e: Exception) {
                _state.value = TeamState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    fun refresh() {
        lastMonth?.let { load(it) }
    }
}