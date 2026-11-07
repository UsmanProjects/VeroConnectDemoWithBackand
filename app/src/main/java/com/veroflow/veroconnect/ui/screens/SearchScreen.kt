package com.veroflow.veroconnect.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.veroflow.veroconnect.data.MediaType
import com.veroflow.veroconnect.ui.components.MediaPlaceholder
import com.veroflow.veroconnect.ui.components.RemoteImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.components.EmptySearchState
import com.veroflow.veroconnect.ui.components.SectionHeader

private enum class SearchFilter { ALL, USERS, POSTS, GROUPS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenComments: (String) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    var recentSearches by remember { mutableStateOf(MockRepository.recentSearches.toList()) }

    val matchedUsers = remember(query) {
        if (query.isBlank()) emptyList() else MockRepository.users.filter {
            it.fullName.contains(query, true) || it.username.contains(query, true)
        }
    }
    val matchedGroups = remember(query) {
        if (query.isBlank()) emptyList() else MockRepository.groups.filter { it.name.contains(query, true) }
    }
    val matchedPosts = remember(query) {
        if (query.isBlank()) emptyList() else MockRepository.posts.filter { it.text.contains(query, true) }
    }

    fun commitSearch(q: String) {
        if (q.isNotBlank() && !MockRepository.recentSearches.contains(q)) {
            MockRepository.recentSearches.add(0, q)
            recentSearches = MockRepository.recentSearches.toList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search users, posts, groups...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }, modifier = Modifier.testTag("btn_clear_search")) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("field_search"),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { commitSearch(query) })
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (query.isBlank()) {
                SectionHeader("Recent Searches")
                if (recentSearches.isEmpty()) {
                    Text("No recent searches", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(modifier = Modifier.testTag("list_recent_searches")) {
                        items(recentSearches) { recent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { query = recent }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("recent_$recent"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(recent, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        MockRepository.recentSearches.remove(recent)
                                        recentSearches = MockRepository.recentSearches.toList()
                                    },
                                    modifier = Modifier.testTag("btn_remove_recent_$recent")
                                ) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        SearchFilter.ALL to "All", SearchFilter.USERS to "Users",
                        SearchFilter.POSTS to "Posts", SearchFilter.GROUPS to "Groups"
                    ).forEach { (f, label) ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f; commitSearch(query) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("filter_${f.name}")
                        )
                    }
                }

                val hasUsers = filter == SearchFilter.ALL || filter == SearchFilter.USERS
                val hasPosts = filter == SearchFilter.ALL || filter == SearchFilter.POSTS
                val hasGroups = filter == SearchFilter.ALL || filter == SearchFilter.GROUPS
                val noResults = (!hasUsers || matchedUsers.isEmpty()) &&
                    (!hasPosts || matchedPosts.isEmpty()) &&
                    (!hasGroups || matchedGroups.isEmpty())

                if (noResults) {
                    EmptySearchState(modifier = Modifier.testTag("empty_search_results"))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().testTag("list_search_results")) {
                        if (hasUsers && matchedUsers.isNotEmpty()) {
                            item { SectionHeader("Users") }
                            items(matchedUsers, key = { "u_${it.id}" }) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenProfile(user.id) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("result_user_${user.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(user)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(user.fullName, fontWeight = FontWeight.SemiBold)
                                        Text("@${user.username}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        if (hasGroups && matchedGroups.isNotEmpty()) {
                            item { SectionHeader("Groups") }
                            items(matchedGroups, key = { "g_${it.id}" }) { group ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("result_group_${group.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RemoteImage(
                                        url = group.imageUrl,
                                        fallbackColor = group.color,
                                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(group.name, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${group.memberCount} members",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        if (hasPosts && matchedPosts.isNotEmpty()) {
                            item { SectionHeader("Posts") }
                            items(matchedPosts, key = { "p_${it.id}" }) { post ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenComments(post.id) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("result_post_${post.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (post.mediaType != MediaType.NONE) {
                                        MediaPlaceholder(
                                            color = post.mediaColor,
                                            mediaType = post.mediaType,
                                            imageUrl = post.imageUrl,
                                            cornerRadius = 8,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Text(post.text, maxLines = 2, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
