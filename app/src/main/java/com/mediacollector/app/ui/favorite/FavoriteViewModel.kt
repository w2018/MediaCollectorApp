package com.mediacollector.app.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaItem
import com.mediacollector.app.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteUiState(
    val items: List<MediaItem> = emptyList(),
    val filteredItems: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterType: String = "all" // all / photo / video
)

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FavoriteUiState())
    val state: StateFlow<FavoriteUiState> = _state.asStateFlow()

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            favoriteRepository.syncFromServer()
            val result = favoriteRepository.getRemoteFavorites()
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

    private fun filterByType(items: List<MediaItem>, type: String): List<MediaItem> {
        return when (type) {
            "photo" -> items.filter { it.type == "photo" }
            "video" -> items.filter { it.type == "video" }
            else -> items
        }
    }

    fun removeFavorite(mediaId: Int) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(mediaId)
            val newItems = _state.value.items.filter { it.id != mediaId }
            _state.value = _state.value.copy(
                items = newItems,
                filteredItems = filterByType(newItems, _state.value.filterType)
            )
        }
    }
}
