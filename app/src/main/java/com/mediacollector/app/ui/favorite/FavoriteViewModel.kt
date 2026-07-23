package com.mediacollector.app.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.local.entity.LocalFavorite
import com.mediacollector.app.data.remote.dto.MediaItem
import com.mediacollector.app.data.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteUiState(
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FavoriteUiState())
    val state: StateFlow<FavoriteUiState> = _state.asStateFlow()

    init { loadFavorites() }

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            favoriteRepository.syncFromServer()
            val result = favoriteRepository.getRemoteFavorites()
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

    fun removeFavorite(mediaId: Int) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(mediaId)
            _state.value = _state.value.copy(
                items = _state.value.items.filter { it.id != mediaId }
            )
        }
    }
}
