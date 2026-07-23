package com.mediacollector.app.ui.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.mediacollector.app.ui.common.EmptyState
import com.mediacollector.app.ui.common.ErrorState
import com.mediacollector.app.ui.common.LoadingIndicator

/** 集合列表页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionListScreen(
    onCollectionClick: (Int) -> Unit,
    onBack: () -> Unit = {},
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("集合") },
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
            state.error != null -> ErrorState(state.error!!, onRetry = { viewModel.loadCollections() })
            state.collections.isEmpty() -> EmptyState("还没有集合")
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.collections, key = { it.id }) { collection ->
                        Card(
                            onClick = { onCollectionClick(collection.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                AsyncImage(
                                    model = collection.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        collection.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        "${collection.itemCount} 项",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
