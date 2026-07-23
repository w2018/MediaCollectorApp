package com.mediacollector.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.local.entity.LocalHistory
import com.mediacollector.app.data.remote.dto.HistoryRecord
import com.mediacollector.app.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val items: List<HistoryRecord> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init { loadHistory() }

    fun loadHistory() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            historyRepository.syncFromServer()
            val result = historyRepository.getRemoteHistory()
            result.fold(
                onSuccess = { data ->
                    _state.value = _state.value.copy(
                        items = data.items,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
                    )
                }
            )
        }
    }

    fun removeHistory(mediaId: Int) {
        viewModelScope.launch {
            historyRepository.removeHistory(mediaId)
            _state.value = _state.value.copy(
                items = _state.value.items.filter { it.mediaId != mediaId }
            )
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
            _state.value = _state.value.copy(items = emptyList())
        }
    }
}
