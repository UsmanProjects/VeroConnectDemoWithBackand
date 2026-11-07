package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.veroflow.veroconnect.data.ImageUrls
import com.veroflow.veroconnect.data.MediaType
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.data.Post
import com.veroflow.veroconnect.data.Story
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.EmptyState
import com.veroflow.veroconnect.ui.components.MediaPlaceholder
import com.veroflow.veroconnect.ui.components.RemoteImage
import com.veroflow.veroconnect.ui.components.ToastEffect
import com.veroflow.veroconnect.ui.theme.BrandGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    onOpenComments: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onCreatePost: () -> Unit,
    onOpenMenu: () -> Unit = {}
) {
    // Posts are Compose-state backed, so likes / saves / new comments show up immediately.
    val posts = MockRepository.posts
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var reportedPostId by remember { mutableStateOf<String?>(null) }
    var openStory by remember { mutableStateOf<Story?>(null) }
    val pullState = rememberPullToRefreshState()
    ToastEffect(snackbarHostState)

    if (pullState.isRefreshing) {
        LaunchedEffect(Unit) {
            delay(900)
            pullState.endRefresh()
            snackbarHostState.showSnackbar("Feed refreshed")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VeroConnect", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu, modifier = Modifier.testTag("btn_open_menu")) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (!pullState.isRefreshing) pullState.startRefresh() },
                        modifier = Modifier.testTag("btn_pull_to_refresh")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh feed")
                    }
                    IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("btn_open_search")) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("btn_open_notifications")) {
                        BadgedBox(badge = {
                            val count = MockRepository.unreadNotificationCount()
                            if (count > 0) Badge { Text("$count") }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePost, modifier = Modifier.testTag("fab_create_post")) {
                Icon(Icons.Filled.Add, contentDescription = "Create post")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .nestedScroll(pullState.nestedScrollConnection)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().testTag("list_feed")) {
                item {
                    if (pullState.isRefreshing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("progress_refresh"))
                    }
                }
                item { StoriesRow(onOpenStory = { openStory = it }, onAddStory = onCreatePost) }
                if (posts.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No posts yet",
                            subtitle = "Follow people or create your first post to see content here."
                        )
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onLikeToggle = {
                                post.isLiked = !post.isLiked
                                post.likeCount += if (post.isLiked) 1 else -1
                                Backend.data.setFlag(DataService.LIKE, post.id, post.isLiked)
                            },
                            onSaveToggle = {
                                post.isSaved = !post.isSaved
                                Backend.data.setFlag(DataService.SAVE, post.id, post.isSaved)
                                scope.launch {
                                    snackbarHostState.showSnackbar(if (post.isSaved) "Post saved" else "Removed from saved")
                                }
                            },
                            onShare = {
                                post.shareCount += 1
                                scope.launch { snackbarHostState.showSnackbar("Post shared") }
                            },
                            onComment = { onOpenComments(post.id) },
                            onOpenProfile = { onOpenProfile(post.authorId) },
                            onReport = { reportedPostId = post.id }
                        )
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
            PullToRefreshContainer(
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    reportedPostId?.let {
        AlertDialog(
            onDismissRequest = { reportedPostId = null },
            title = { Text("Report Post") },
            text = { Text("Are you sure you want to report this post? Our team will review it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        reportedPostId = null
                        scope.launch { snackbarHostState.showSnackbar("Post reported") }
                    },
                    modifier = Modifier.testTag("btn_confirm_report")
                ) { Text("Report") }
            },
            dismissButton = {
                TextButton(onClick = { reportedPostId = null }, modifier = Modifier.testTag("btn_cancel_report")) {
                    Text("Cancel")
                }
            }
        )
    }

    openStory?.let { story ->
        StoryViewer(story = story, onClose = { openStory = null }, onOpenProfile = {
            openStory = null
            onOpenProfile(story.userId)
        })
    }
}

@Composable
private fun StoriesRow(onOpenStory: (Story) -> Unit, onAddStory: () -> Unit) {
    val ringViewed = SolidColor(MaterialTheme.colorScheme.outlineVariant)
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("list_stories"),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "Your story" bubble
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onAddStory() }
                    .testTag("story_add")
            ) {
                Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                    Avatar(MockRepository.currentUser, 60)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Your story", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
        items(MockRepository.stories, key = { it.id }) { story ->
            val user = MockRepository.userById(story.userId)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(72.dp)
                    .clickable { onOpenStory(story) }
                    .testTag("story_${story.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .border(3.dp, if (story.viewed) ringViewed else BrandGradient, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(user, 58)
                }
                Spacer(Modifier.height(4.dp))
                Text(user.firstName, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
    Divider(modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun StoryViewer(story: Story, onClose: () -> Unit, onOpenProfile: () -> Unit) {
    val user = MockRepository.userById(story.userId)
    val progress = remember(story.id) { Animatable(0f) }
    LaunchedEffect(story.id) {
        story.viewed = true
        progress.animateTo(1f, tween(5000, easing = LinearEasing))
        onClose()
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).testTag("story_viewer")) {
            RemoteImage(
                url = ImageUrls.story(story.id),
                fallbackColor = story.color,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                    .padding(start = 12.dp, end = 4.dp, top = 28.dp, bottom = 16.dp)
            ) {
                LinearProgressIndicator(
                    progress = progress.value,
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.35f)
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).clickable { onOpenProfile() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(user, 36)
                        Spacer(Modifier.width(8.dp))
                        Text(user.fullName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("btn_close_story")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    onLikeToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit,
    onOpenProfile: () -> Unit,
    onReport: () -> Unit
) {
    val author = MockRepository.userById(post.authorId)
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("post_${post.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clickable { onOpenProfile() }) {
                    Avatar(author, 40)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f).clickable { onOpenProfile() }) {
                    Text(author.fullName, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (post.timeAgo == "now") "Just now" else "${post.timeAgo} ago",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("btn_post_menu_${post.id}")) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Report post") },
                            onClick = { menuExpanded = false; onReport() },
                            modifier = Modifier.testTag("menu_report_${post.id}")
                        )
                    }
                }
            }
            if (post.text.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(post.text, style = MaterialTheme.typography.bodyMedium)
            }

            if (post.mediaType != MediaType.NONE) {
                Spacer(Modifier.height(8.dp))
                MediaPlaceholder(
                    color = post.mediaColor,
                    mediaType = post.mediaType,
                    imageUrl = post.imageUrl,
                    duration = post.duration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (post.mediaType == MediaType.VIDEO) 16f / 9f else 4f / 3f)
                        .pointerInput(post.id) {
                            // Double-tap the picture to like, like in most social apps.
                            detectTapGestures(onDoubleTap = { if (!post.isLiked) onLikeToggle() })
                        }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "${post.likeCount} likes · ${post.comments.size} comments · ${post.shareCount} shares",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.padding(vertical = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ActionButton(
                    icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = "Like",
                    tint = if (post.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    onClick = onLikeToggle,
                    testTag = "btn_like_${post.id}"
                )
                ActionButton(
                    icon = Icons.Filled.ChatBubbleOutline,
                    label = "Comment",
                    onClick = onComment,
                    testTag = "btn_comment_${post.id}"
                )
                ActionButton(
                    icon = Icons.Filled.Share,
                    label = "Share",
                    onClick = onShare,
                    testTag = "btn_share_${post.id}"
                )
                ActionButton(
                    icon = if (post.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    label = "Save",
                    onClick = onSaveToggle,
                    testTag = "btn_save_${post.id}"
                )
            }
        }
    }
}

@Composable
private fun RowScope.ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    TextButton(onClick = onClick, modifier = Modifier.weight(1f).testTag(testTag)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
