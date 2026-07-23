package com.mediacollector.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.HistoryRecord
import com.mediacollector.app.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<HistoryRecord> = emptyList(),
    val filteredItems: List<HistoryRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterType: String = "all" // all / photo / video
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    fun loadHistory() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            historyRepository.syncFromServer()
            val result = historyRepository.getRemoteHistory()
            result.fold(
                onSuccess = { data ->
                    _state.value = _state.value.copy(
                        items = data.items,
                        filteredItems = filterByType(data.items, _state.value.filterType),
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        items = emptyList(),
                        filteredItems = emptyList(),
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            )
        }
    }

    fun setFilter(type: String) {
        _state.value = _state.value.copy(
            filterType = type,
            filteredItems = filterByType(_state.value.items, type)
        )
    }

    fun removeHistory(mediaId: Int) {
        viewModelScope.launch {
            historyRepository.removeHistory(mediaId)
            val newItems = _state.value.items.filter { it.mediaId != mediaId }
            _state.value = _state.value.copy(
                items = newItems,
                filteredItems = filterByType(newItems, _state.value.filterType)
            )
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
            _state.value = _state.value.copy(items = emptyList(), filteredItems = emptyList())
        }
    }

    private fun filterByType(items: List<HistoryRecord>, type: String): List<HistoryRecord> {
        return when (type) {
            "photo" -> items.filter { it.mediaType == "photo" }
            "video" -> items.filter { it.mediaType == "video" }
            else -> items
        }
    }
}
