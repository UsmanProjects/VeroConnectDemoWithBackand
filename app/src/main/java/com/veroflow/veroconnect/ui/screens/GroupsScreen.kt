package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.DataService
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.Group
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.RemoteImage
import com.veroflow.veroconnect.ui.components.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(onBack: () -> Unit = {}) {
    // Groups are Compose-state backed, so Join / Leave updates the lists immediately.
    val groups = MockRepository.groups
    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val joined = groups.filter { it.isJoined }
    val suggested = groups.filter { !it.isJoined }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }, modifier = Modifier.testTag("fab_create_group")) {
                Icon(Icons.Filled.Add, contentDescription = "Create group")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).testTag("list_groups")) {
            item { SectionHeader("Your Groups (${joined.size})") }
            items(joined, key = { it.id }) { group ->
                GroupRow(group) {
                    group.isJoined = false
                    Backend.data.setFlag(DataService.GROUP, group.id, false)
                    scope.launch { snackbarHostState.showSnackbar("Left ${group.name}") }
                }
            }
            item { SectionHeader("Suggested Groups") }
            items(suggested, key = { it.id }) { group ->
                GroupRow(group) {
                    group.isJoined = true
                    Backend.data.setFlag(DataService.GROUP, group.id, true)
                    scope.launch { snackbarHostState.showSnackbar("Joined ${group.name}") }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    modifier = Modifier.testTag("field_group_name")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            val newGroup = Group(
                                id = "g_new_${System.currentTimeMillis()}",
                                name = newGroupName.trim(),
                                memberCount = 1,
                                color = Color(0xFF6750A4),
                                isJoined = true
                            )
                            MockRepository.groups.add(0, newGroup)
                            Backend.data.saveGroup(newGroup)
                            newGroupName = ""
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_create_group")
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }, modifier = Modifier.testTag("btn_cancel_create_group")) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GroupRow(group: Group, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("group_${group.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteImage(
            url = group.imageUrl,
            fallbackColor = group.color,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(group.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${group.memberCount} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (group.isJoined) {
            OutlinedButton(onClick = onToggle, modifier = Modifier.testTag("btn_leave_${group.id}")) { Text("Leave") }
        } else {
            Button(onClick = onToggle, modifier = Modifier.testTag("btn_join_${group.id}")) { Text("Join") }
        }
    }
}
