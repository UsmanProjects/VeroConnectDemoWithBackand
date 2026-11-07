package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.Conversation
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(onOpenChat: (String) -> Unit) {
    // Conversations are Compose-state backed, so unread badges, mute, archive etc. update live.
    val conversations = MockRepository.conversations.filter { !it.isArchived }
    var contextMenuFor by remember { mutableStateOf<Conversation?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chats") }) }
    ) { padding ->
        if (conversations.isEmpty()) {
            EmptyState(
                title = "No conversations",
                subtitle = "Start a chat with a friend to see it here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("list_chats")) {
                items(conversations, key = { it.id }) { convo ->
                    val user = MockRepository.userById(convo.userId)
                    val hasUnread = convo.unreadCount > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    convo.unreadCount = 0
                                    Backend.data.setFlag(DataService.CONV_READ, convo.id, true)
                                    onOpenChat(convo.id)
                                },
                                onLongClick = { contextMenuFor = convo }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("chat_${convo.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(user, 52)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user.fullName,
                                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                convo.lastMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                convo.time,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            if (hasUnread) {
                                Badge { Text("${convo.unreadCount}") }
                            } else if (convo.isMuted) {
                                Icon(
                                    Icons.Filled.VolumeOff,
                                    contentDescription = "Muted",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(start = 80.dp))
                }
            }
        }
    }

    contextMenuFor?.let { convo ->
        val user = MockRepository.userById(convo.userId)
        ModalBottomSheet(onDismissRequest = { contextMenuFor = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    user.fullName,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                ListItem(
                    headlineContent = { Text(if (convo.isMuted) "Unmute conversation" else "Mute conversation") },
                    modifier = Modifier
                        .testTag("btn_mute_chat")
                        .clickable {
                            convo.isMuted = !convo.isMuted
                            Backend.data.setFlag(DataService.CONV_MUTED, convo.id, convo.isMuted)
                            contextMenuFor = null
                        }
                )
                ListItem(
                    headlineContent = { Text("Archive conversation") },
                    modifier = Modifier
                        .testTag("btn_archive_chat")
                        .clickable {
                            convo.isArchived = true
                            Backend.data.setFlag(DataService.CONV_ARCHIVED, convo.id, true)
                            contextMenuFor = null
                        }
                )
                ListItem(
                    headlineContent = { Text("Delete conversation", color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier
                        .testTag("btn_delete_chat")
                        .clickable {
                            MockRepository.conversations.remove(convo)
                            Backend.data.deleteConversation(convo.id)
                            contextMenuFor = null
                        }
                )
            }
        }
    }
}
