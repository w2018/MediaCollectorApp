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
    val filteredResults: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val filterType: String = "all" // all / photo / video
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
            _state.value = _state.value.copy(results = emptyList(), filteredResults = emptyList(), hasSearched = false)
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // 防抖
            _state.value = _state.value.copy(isLoading = true, error = null)
            mediaRepository.search(query, page = 1, pageSize = 50).fold(
                onSuccess = { result ->
                    _state.value = _state.value.copy(
                        results = result.results,
                        filteredResults = filterByType(result.results, _state.value.filterType),
                        isLoading = false,
                        hasSearched = true,
                        currentPage = result.pagination.page,
                        totalPages = result.pagination.totalPages
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

    fun loadMore() {
        val current = _state.value
        if (current.isLoadingMore || current.currentPage >= current.totalPages) return

        viewModelScope.launch {
            _state.value = current.copy(isLoadingMore = true)
            mediaRepository.search(current.query, page = current.currentPage + 1, pageSize = 50).fold(
                onSuccess = { result ->
                    _state.value = _state.value.copy(
                        results = _state.value.results + result.results,
                        filteredResults = filterByType(
                            _state.value.results + result.results,
                            _state.value.filterType
                        ),
                        isLoadingMore = false,
                        currentPage = result.pagination.page,
                        totalPages = result.pagination.totalPages
                    )
                },
                onFailure = { _state.value = _state.value.copy(isLoadingMore = false) }
            )
        }
    }

    fun setFilter(type: String) {
        _state.value = _state.value.copy(
            filterType = type,
            filteredResults = filterByType(_state.value.results, type)
        )
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.value = SearchUiState()
    }

    private fun filterByType(items: List<MediaItem>, type: String): List<MediaItem> {
        return when (type) {
            "photo" -> items.filter { it.type == "photo" }
            "video" -> items.filter { it.type == "video" }
            else -> items
        }
    }
}
