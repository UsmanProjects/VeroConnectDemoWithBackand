package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.FriendRequest
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.data.User
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.EmptyState
import com.veroflow.veroconnect.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(onBack: () -> Unit, onOpenProfile: (String) -> Unit) {
    // Friend lists are Compose-state backed, so accept / reject / remove update the screen live.
    val requests = MockRepository.friendRequests
    val friends = MockRepository.friends
    val suggestions = remember {
        MockRepository.users
            .filter { u -> !u.isFriend && MockRepository.friendRequests.none { r -> r.userId == u.id } }
            .take(10)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("list_friends")) {
            item { SectionHeader("Friend Requests (${requests.size})") }
            if (requests.isEmpty()) {
                item {
                    EmptyState(title = "No pending requests", subtitle = "You're all caught up.")
                }
            } else {
                items(requests, key = { it.id }) { request ->
                    val user = MockRepository.userById(request.userId)
                    FriendRequestRow(
                        user = user,
                        onAccept = {
                            MockRepository.friendRequests.remove(request)
                            user.isFriend = true
                            if (MockRepository.friends.none { it.id == user.id }) MockRepository.friends.add(user)
                            Backend.data.setFlag(DataService.FRIEND, user.id, true)
                            Backend.data.setFlag(DataService.REQUEST_DONE, user.id, true)
                        },
                        onReject = {
                            MockRepository.friendRequests.remove(request)
                            Backend.data.setFlag(DataService.REQUEST_DONE, user.id, true)
                        },
                        onOpenProfile = { onOpenProfile(user.id) }
                    )
                }
            }

            item { SectionHeader("Your Friends (${friends.size})") }
            if (friends.isEmpty()) {
                item { EmptyState(title = "No friends yet", subtitle = "Accept requests or add new friends to get started.") }
            } else {
                items(friends, key = { it.id }) { friend ->
                    FriendRow(
                        user = friend,
                        onRemove = {
                            friend.isFriend = false
                            MockRepository.friends.remove(friend)
                            Backend.data.setFlag(DataService.FRIEND, friend.id, false)
                        },
                        onOpenProfile = { onOpenProfile(friend.id) }
                    )
                }
            }

            item { SectionHeader("Suggested Friends") }
            items(suggestions, key = { it.id }) { suggestion ->
                SuggestionRow(user = suggestion, onOpenProfile = { onOpenProfile(suggestion.id) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FriendRequestRow(user: User, onAccept: () -> Unit, onReject: () -> Unit, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenProfile() }.padding(horizontal = 16.dp, vertical = 8.dp).testTag("request_${user.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(user)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, fontWeight = FontWeight.SemiBold)
            Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onAccept, modifier = Modifier.testTag("btn_accept_${user.id}")) { Text("Accept") }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = onReject, modifier = Modifier.testTag("btn_reject_${user.id}")) { Text("Reject") }
    }
}

@Composable
private fun FriendRow(user: User, onRemove: () -> Unit, onOpenProfile: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenProfile() }.padding(horizontal = 16.dp, vertical = 8.dp).testTag("friend_${user.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(user)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, fontWeight = FontWeight.SemiBold)
            Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onRemove, modifier = Modifier.testTag("btn_remove_friend_${user.id}")) { Text("Remove") }
    }
}

@Composable
private fun SuggestionRow(user: User, onOpenProfile: () -> Unit) {
    var added by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenProfile() }.padding(horizontal = 16.dp, vertical = 8.dp).testTag("suggestion_${user.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(user)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.fullName, fontWeight = FontWeight.SemiBold)
            Text("${user.followers} followers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = { added = true },
            enabled = !added,
            modifier = Modifier.testTag("btn_add_friend_${user.id}")
        ) { Text(if (added) "Requested" else "Add Friend") }
    }
}
