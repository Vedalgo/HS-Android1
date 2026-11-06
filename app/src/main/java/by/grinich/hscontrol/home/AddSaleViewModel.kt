package by.grinich.hscontrol.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.grinich.hscontrol.data.DataRepository
import by.grinich.hscontrol.data.HonorModelDto
import by.grinich.hscontrol.data.NewSale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddSaleViewModel : ViewModel() {

    private val _models = MutableStateFlow<List<HonorModelDto>>(emptyList())
    val models: StateFlow<List<HonorModelDto>> = _models.asStateFlow()

    private val _quantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val quantities: StateFlow<Map<String, Int>> = _quantities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadModels() {
        if (_models.value.isNotEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = DataRepository.getHonorModels()
                _models.value = list
                _quantities.value = list.associate { it.modelName to 0 }
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка загрузки моделей"
            }
            _isLoading.value = false
        }
    }

    fun changeQuantity(model: String, delta: Int) {
        val current = _quantities.value[model] ?: 0
        _quantities.value = _quantities.value.toMutableMap().apply {
            this[model] = (current + delta).coerceAtLeast(0)
        }
    }

    fun getTotalHonor(): Int = _quantities.value.values.sum()

    fun clearError() { _error.value = null }

    fun saveHonorSales(
        userId: String,
        date: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val entries = _quantities.value.filter { it.value > 0 }
        if (entries.isEmpty()) {
            onError("Выберите хотя бы одну модель")
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val sales = entries.map { (model, qty) ->
                    NewSale(
                        userId = userId,
                        saleDate = date,
                        brand = "Honor",
                        model = model,
                        quantity = qty
                    )
                }
                DataRepository.addSales(sales)
                _isSaving.value = false
                reset()
                onSuccess()
            } catch (e: Exception) {
                _isSaving.value = false
                onError(e.message ?: "Ошибка сохранения")
            }
        }
    }

    fun saveOtherBrand(
        userId: String,
        date: String,
        brand: String,
        quantity: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (quantity < 1) {
            onError("Введите количество")
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                DataRepository.addSale(
                    NewSale(
                        userId = userId,
                        saleDate = date,
                        brand = brand,
                        model = brand,
                        quantity = quantity
                    )
                )
                _isSaving.value = false
                reset()
                onSuccess()
            } catch (e: Exception) {
                _isSaving.value = false
                onError(e.message ?: "Ошибка сохранения")
            }
        }
    }

    private fun reset() {
        _quantities.value = _quantities.value.mapValues { 0 }
    }
}