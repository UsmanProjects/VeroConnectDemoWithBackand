package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.Comment
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(postId: String, onBack: () -> Unit) {
    val post = remember { MockRepository.posts.find { it.id == postId } }
    var commentText by remember { mutableStateOf("") }
    var editingCommentId by remember { mutableStateOf<String?>(null) }
    var menuForComment by remember { mutableStateOf<String?>(null) }

    fun send() {
        if (commentText.isBlank() || post == null) return
        val editing = editingCommentId
        if (editing != null) {
            post.comments.find { it.id == editing }?.text = commentText
            Backend.data.updateComment(editing, commentText)
            editingCommentId = null
        } else {
            val newComment = Comment(
                id = "c_new_${System.currentTimeMillis()}",
                authorId = "me",
                text = commentText,
                timeAgo = "now"
            )
            post.comments.add(0, newComment)
            Backend.data.saveComment(post.id, newComment)
        }
        commentText = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // The input bar sits above both the system navigation bar and the keyboard.
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(MockRepository.currentUser, 36)
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text(if (editingCommentId != null) "Edit your comment..." else "Write a comment...") },
                        modifier = Modifier.weight(1f).testTag("field_comment"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() })
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { send() },
                        modifier = Modifier.testTag("btn_send_comment")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send comment", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        if (post == null || post.comments.isEmpty()) {
            EmptyState(
                title = "No comments yet",
                subtitle = "Be the first to share your thoughts.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("list_comments")) {
                items(post.comments, key = { it.id }) { comment ->
                    val author = MockRepository.userById(comment.authorId)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp).testTag("comment_${comment.id}"),
                        verticalAlignment = Alignment.Top
                    ) {
                        Avatar(author, 36)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(author.fullName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (comment.timeAgo == "now") "Just now" else "${comment.timeAgo} ago",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                TextButton(
                                    onClick = {
                                        comment.isLiked = !comment.isLiked
                                        comment.likes += if (comment.isLiked) 1 else -1
                                        Backend.data.setFlag(DataService.COMMENT_LIKE, comment.id, comment.isLiked)
                                    },
                                    modifier = Modifier.testTag("btn_like_comment_${comment.id}")
                                ) {
                                    Icon(
                                        if (comment.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like comment",
                                        modifier = Modifier.size(14.dp),
                                        tint = if (comment.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("${comment.likes}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (comment.authorId == "me") {
                            Box {
                                IconButton(onClick = { menuForComment = comment.id }, modifier = Modifier.testTag("btn_comment_menu_${comment.id}")) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Comment options")
                                }
                                DropdownMenu(expanded = menuForComment == comment.id, onDismissRequest = { menuForComment = null }) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            menuForComment = null
                                            editingCommentId = comment.id
                                            commentText = comment.text
                                        },
                                        modifier = Modifier.testTag("menu_edit_${comment.id}")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            menuForComment = null
                                            post.comments.remove(comment)
                                            Backend.data.deleteComment(comment.id)
                                        },
                                        modifier = Modifier.testTag("menu_delete_${comment.id}")
                                    )
                                }
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
