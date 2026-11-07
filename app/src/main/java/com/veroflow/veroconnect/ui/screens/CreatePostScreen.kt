package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.Comment
import com.veroflow.veroconnect.data.ImageUrls
import com.veroflow.veroconnect.data.MediaType
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.data.Post
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.MediaPlaceholder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PermissionState { UNKNOWN, GRANTED, DENIED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(onCancel: () -> Unit, onPublished: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var mediaAttached by remember { mutableStateOf(MediaType.NONE) }
    var mediaUrl by remember { mutableStateOf<String?>(null) }
    var fileAttached by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }
    var cameraPermission by remember { mutableStateOf(PermissionState.UNKNOWN) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showUploadFailure by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun attachImage() {
        mediaAttached = MediaType.IMAGE
        mediaUrl = ImageUrls.draft()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("btn_cancel_post")) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        enabled = !isPublishing,
                        onClick = {
                            if (text.isBlank() && mediaAttached == MediaType.NONE && fileAttached == null) {
                                scope.launch { snackbarHostState.showSnackbar("Add text or media before publishing") }
                                return@TextButton
                            }
                            isPublishing = true
                            scope.launch {
                                delay(1000)
                                val newPost =
                                    Post(
                                        id = "post_new_${System.currentTimeMillis()}",
                                        authorId = "me",
                                        text = text,
                                        mediaType = mediaAttached,
                                        mediaColor = Color(0xFF6750A4),
                                        likeCount = 0,
                                        comments = mutableListOf<Comment>(),
                                        shareCount = 0,
                                        timeAgo = "now",
                                        mediaUrl = if (mediaAttached == MediaType.NONE) null else mediaUrl
                                    )
                                MockRepository.posts.add(0, newPost)
                                Backend.data.savePost(newPost)
                                isPublishing = false
                                // Shown on the feed (this screen closes right away).
                                MockRepository.toast.value = "Post published"
                                onPublished()
                            }
                        },
                        modifier = Modifier.testTag("btn_publish_post")
                    ) { Text("Publish") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(MockRepository.currentUser, 40)
                Spacer(Modifier.width(10.dp))
                Text(MockRepository.currentUser.fullName, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth().height(140.dp).testTag("field_post_text")
            )
            Spacer(Modifier.height(16.dp))

            if (mediaAttached != MediaType.NONE) {
                Box {
                    MediaPlaceholder(
                        color = Color(0xFF6750A4),
                        mediaType = mediaAttached,
                        imageUrl = mediaUrl,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    IconButton(
                        onClick = { mediaAttached = MediaType.NONE; mediaUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd).testTag("btn_remove_media")
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Remove media", tint = Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            fileAttached?.let { file ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("attached_file_row")) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(file, modifier = Modifier.weight(1f))
                    IconButton(onClick = { fileAttached = null }, modifier = Modifier.testTag("btn_remove_file")) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove file")
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text("Add to your post", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (cameraPermission == PermissionState.UNKNOWN) {
                            showPermissionDialog = true
                        } else if (cameraPermission == PermissionState.GRANTED) {
                            attachImage()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Camera permission denied") }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.testTag("btn_camera")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Camera")
                }
                OutlinedButton(
                    onClick = { attachImage() },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.testTag("btn_gallery")
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery")
                }
                OutlinedButton(
                    onClick = {
                        // Simulate an occasional upload failure for testing error states.
                        if ((0..4).random() == 0) {
                            showUploadFailure = true
                        } else {
                            fileAttached = "document_${(100..999).random()}.pdf"
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.testTag("btn_attach_file")
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("File")
                }
            }

            if (isPublishing) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("progress_publishing"))
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Allow Camera Access") },
            text = { Text("VeroConnect needs camera access to take photos for your posts.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cameraPermission = PermissionState.GRANTED
                        showPermissionDialog = false
                        attachImage()
                    },
                    modifier = Modifier.testTag("btn_permission_allow")
                ) { Text("Allow") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        cameraPermission = PermissionState.DENIED
                        showPermissionDialog = false
                    },
                    modifier = Modifier.testTag("btn_permission_deny")
                ) { Text("Deny") }
            }
        )
    }

    if (showUploadFailure) {
        AlertDialog(
            onDismissRequest = { showUploadFailure = false },
            title = { Text("Upload Failed") },
            text = { Text("We couldn't upload your file. Please check your connection and try again.") },
            confirmButton = {
                TextButton(onClick = { showUploadFailure = false }, modifier = Modifier.testTag("btn_dismiss_upload_failure")) {
                    Text("OK")
                }
            }
        )
    }
}
