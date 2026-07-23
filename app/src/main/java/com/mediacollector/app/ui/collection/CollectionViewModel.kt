package com.mediacollector.app.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediacollector.app.data.remote.dto.MediaCollection
import com.mediacollector.app.data.remote.dto.MediaItem
import com.mediacollector.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionListUiState(
    val collections: List<MediaCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CollectionDetailUiState(
    val collection: MediaCollection? = null,
    val media: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(CollectionListUiState())
    val listState: StateFlow<CollectionListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(CollectionDetailUiState())
    val detailState: StateFlow<CollectionDetailUiState> = _detailState.asStateFlow()

    init { loadCollections() }

    fun loadCollections() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)
            mediaRepository.getCollections().fold(
                onSuccess = { _listState.value = _listState.value.copy(collections = it, isLoading = false) },
                onFailure = { _listState.value = _listState.value.copy(error = it.message, isLoading = false) }
            )
        }
    }

    fun loadCollectionDetail(id: Int) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true, error = null)
            mediaRepository.getCollectionDetail(id).fold(
                onSuccess = { col: MediaCollection ->
                    _detailState.value = _detailState.value.copy(collection = col, isLoading = false)
                },
                onFailure = { e ->
                    _detailState.value = _detailState.value.copy(error = e.message, isLoading = false)
                }
            )
        }
    }
}
