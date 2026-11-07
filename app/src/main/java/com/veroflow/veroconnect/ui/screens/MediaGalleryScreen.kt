package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.MediaType
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.MediaPlaceholder

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaGalleryScreen(onBack: () -> Unit) {
    var items by remember { mutableStateOf(MockRepository.mediaGallery.toList()) }
    var openedIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            com.veroflow.veroconnect.ui.components.EmptyState(
                title = "No media",
                subtitle = "Photos and videos you share will appear here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding).testTag("grid_media"),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(items, key = { it.id }) { media ->
                    val index = items.indexOf(media)
                    MediaPlaceholder(
                        color = media.color,
                        mediaType = media.type,
                        cornerRadius = 4,
                        imageUrl = media.imageUrl,
                        modifier = Modifier
                            .padding(2.dp)
                            .aspectRatio(1f)
                            .pointerInput(media.id) {
                                detectTapGestures(onTap = { openedIndex = index })
                            }
                            .testTag("media_${media.id}")
                    )
                }
            }
        }
    }

    openedIndex?.let { startIndex ->
        val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
        Dialog(onDismissRequest = { openedIndex = null }) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize().testTag("pager_media_viewer")) { page ->
                    MediaPlaceholder(
                        color = items[page].color,
                        mediaType = items[page].type,
                        cornerRadius = 0,
                        imageUrl = items[page].imageUrl,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Row(
                    modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(16.dp)
                ) {
                    IconButton(
                        onClick = {
                            val toRemove = items[pagerState.currentPage]
                            items = items.filterNot { it.id == toRemove.id }
                            MockRepository.mediaGallery.removeAll { it.id == toRemove.id }
                            Backend.data.setFlag(DataService.MEDIA_DELETED, toRemove.id, true)
                            openedIndex = null
                        },
                        modifier = Modifier.testTag("btn_delete_media")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = androidx.compose.ui.graphics.Color.White)
                    }
                    IconButton(onClick = { openedIndex = null }, modifier = Modifier.testTag("btn_close_viewer")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun Dialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        content()
    }
}
