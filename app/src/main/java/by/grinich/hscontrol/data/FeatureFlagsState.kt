package by.grinich.hscontrol.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Глобальное состояние фича-флагов.
 * Загружается один раз при старте, обновляется при изменении.
 *
 * По умолчанию (если флаг не найден в БД) — считаем функцию ВКЛЮЧЁННОЙ,
 * чтобы отсутствие флага не ломало приложение.
 */
object FeatureFlagsState {

    private val _flags = MutableStateFlow<Map<String, FeatureFlag>>(emptyMap())
    val flags: StateFlow<Map<String, FeatureFlag>> = _flags.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun load() {
        _isLoading.value = true
        try {
            val list = DataRepository.getFeatureFlags()
            _flags.value = list.associateBy { it.key }
        } catch (_: Exception) {
            // тихо игнорируем — оставляем старые значения
        }
        _isLoading.value = false
    }

    suspend fun setFlag(key: String, enabled: Boolean) {
        try {
            DataRepository.setFeatureFlag(key, enabled)
            load()
        } catch (_: Exception) {}
    }

    /** Возвращает true, если флаг включён или отсутствует */
    fun isEnabled(key: String): Boolean {
        return _flags.value[key]?.enabled ?: true
    }

    /** Возвращает сам объект флага или null */
    fun get(key: String): FeatureFlag? = _flags.value[key]
}