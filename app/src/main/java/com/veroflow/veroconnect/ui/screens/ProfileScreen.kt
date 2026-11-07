package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.MediaType
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.EmptyState
import com.veroflow.veroconnect.ui.components.MediaPlaceholder
import com.veroflow.veroconnect.ui.components.RemoteImage
import com.veroflow.veroconnect.ui.components.ToastEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onMessage: (String) -> Unit
) {
    val isMe = userId == "me"
    var other by remember(userId) { mutableStateOf(MockRepository.userById(userId)) }
    // My own profile is read live, so edits show up as soon as they are saved.
    val user = if (isMe) MockRepository.currentUser else other
    var selectedTab by remember { mutableStateOf(0) }
    val posts = remember(userId) { MockRepository.posts.filter { it.authorId == userId } }
    val snackbarHostState = remember { SnackbarHostState() }
    if (isMe) ToastEffect(snackbarHostState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.fullName) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isMe) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("btn_open_settings")) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // One scrolling grid for the whole page (header + tabs + pictures), so nothing gets squeezed
        // or cut off on small screens.
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(if (selectedTab == 0) "grid_profile_posts" else "grid_profile_media"),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        RemoteImage(
                            url = user.coverUrl,
                            fallbackColor = user.avatarColor.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = 16.dp, y = 43.dp)
                                .size(86.dp)
                                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(3.dp)
                        ) {
                            Avatar(user, 80)
                        }
                    }
                    Spacer(Modifier.height(48.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (user.bio.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(user.bio, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            // For your own profile the numbers are real (from your saved data).
                            ProfileStat("Posts", if (isMe) MockRepository.posts.count { it.authorId == "me" } else user.postsCount)
                            ProfileStat("Followers", user.followers)
                            ProfileStat("Following", if (isMe) MockRepository.users.count { it.isFollowing } else user.following)
                        }
                        Spacer(Modifier.height(16.dp))
                        if (isMe) {
                            Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth().testTag("btn_edit_profile")) {
                                Text("Edit Profile")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        val nowFollowing = !other.isFollowing
                                        other = other.copy(
                                            isFollowing = nowFollowing,
                                            followers = other.followers + if (nowFollowing) 1 else -1
                                        )
                                        MockRepository.users.find { it.id == other.id }?.isFollowing = nowFollowing
                                        Backend.data.setFlag(DataService.FOLLOW, other.id, nowFollowing)
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_follow_toggle")
                                ) {
                                    Text(if (user.isFollowing) "Unfollow" else "Follow")
                                }
                                OutlinedButton(
                                    onClick = { onMessage(user.id) },
                                    modifier = Modifier.weight(1f).testTag("btn_message_user")
                                ) {
                                    Text("Message")
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Posts") },
                        modifier = Modifier.testTag("tab_posts")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Media") },
                        modifier = Modifier.testTag("tab_media")
                    )
                }
            }

            if (selectedTab == 0) {
                if (posts.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(title = "No posts yet", subtitle = "Posts from ${user.firstName} will show up here.")
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        if (post.mediaType == MediaType.NONE) {
                            // Text-only post: show the caption on a coloured tile.
                            Box(
                                modifier = Modifier
                                    .padding(1.dp)
                                    .aspectRatio(1f)
                            ) {
                                MediaPlaceholder(
                                    color = post.mediaColor,
                                    mediaType = MediaType.NONE,
                                    cornerRadius = 0,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    post.text,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.Center).padding(8.dp)
                                )
                            }
                        } else {
                            MediaPlaceholder(
                                color = post.mediaColor,
                                mediaType = post.mediaType,
                                cornerRadius = 0,
                                imageUrl = post.imageUrl,
                                duration = post.duration,
                                modifier = Modifier.padding(1.dp).aspectRatio(1f)
                            )
                        }
                    }
                }
            } else {
                items(MockRepository.mediaGallery, key = { it.id }) { media ->
                    MediaPlaceholder(
                        color = media.color,
                        mediaType = media.type,
                        cornerRadius = 0,
                        imageUrl = media.imageUrl,
                        modifier = Modifier.padding(1.dp).aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
