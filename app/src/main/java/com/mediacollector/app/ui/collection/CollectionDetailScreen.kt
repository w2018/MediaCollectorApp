package com.mediacollector.app.ui.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediacollector.app.ui.common.LoadingIndicator
import com.mediacollector.app.ui.media.components.MediaCard
import com.mediacollector.app.ui.photo.BrowsingContext

/** 集合详情页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Int,
    onMediaClick: (mediaId: Int, type: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    LaunchedEffect(collectionId) { viewModel.loadCollectionDetail(collectionId) }
    val state by viewModel.detailState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.collection?.name ?: "集合详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingIndicator()
            state.error != null -> {
                Box(modifier = Modifier.padding(padding)) {
                    com.mediacollector.app.ui.common.ErrorState(
                        state.error!!,
                        onRetry = { viewModel.loadCollectionDetail(collectionId) }
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.media, key = { it.id }) { media ->
                        MediaCard(
                            media = media,
                            onClick = {
                                BrowsingContext.mediaIds = state.media.map { it.id }
                                onMediaClick(media.id, media.type)
                            }
                        )
                    }
                }
            }
        }
    }
}
