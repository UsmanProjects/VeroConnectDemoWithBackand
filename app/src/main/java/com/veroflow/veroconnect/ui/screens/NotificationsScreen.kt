package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.data.NotificationType
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onOpenProfile: (String) -> Unit) {
    // Notifications are Compose-state backed: read / clear update the screen live.
    val notifications = MockRepository.notifications

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    TextButton(
                        onClick = {
                            Backend.data.setFlags(DataService.NOTIF_CLEARED, MockRepository.notifications.map { it.id }, true)
                            MockRepository.notifications.clear()
                        },
                        modifier = Modifier.testTag("btn_clear_all")
                    ) { Text("Clear all") }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyState(
                title = "No notifications",
                subtitle = "You're all caught up for now.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("list_notifications")) {
                items(notifications, key = { it.id }) { notif ->
                    val user = MockRepository.userById(notif.userId)
                    val icon = when (notif.type) {
                        NotificationType.LIKE -> Icons.Filled.Favorite
                        NotificationType.COMMENT -> Icons.Filled.ChatBubble
                        NotificationType.FRIEND_REQUEST -> Icons.Filled.PersonAdd
                        NotificationType.MENTION -> Icons.Filled.AlternateEmail
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable {
                                notif.isRead = true
                                Backend.data.setFlag(DataService.NOTIF_READ, notif.id, true)
                                onOpenProfile(user.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("notification_${notif.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            Avatar(user, 44)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${user.fullName} ${notif.text}")
                            Text(notif.timeAgo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!notif.isRead) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        }
                    }
                    Divider()
                }
            }
        }
    }
}
