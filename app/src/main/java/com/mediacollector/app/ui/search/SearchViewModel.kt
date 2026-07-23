package com.mediacollector.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaItem
import com.mediacollector.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()

        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), hasSearched = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // 防抖
            _state.value = _state.value.copy(isLoading = true, error = null)
            mediaRepository.search(query).fold(
                onSuccess = { result ->
                    _state.value = _state.value.copy(
                        results = result.results,
                        isLoading = false,
                        hasSearched = true
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message,
                        hasSearched = true
                    )
                }
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.value = SearchUiState()
    }
}
