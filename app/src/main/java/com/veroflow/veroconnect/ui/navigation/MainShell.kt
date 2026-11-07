package com.veroflow.veroconnect.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.Avatar
import com.veroflow.veroconnect.ui.screens.*
import kotlinx.coroutines.launch

private enum class MainTab(val label: String, val icon: ImageVector, val testTag: String) {
    HOME("Home", Icons.Filled.Home, "tab_home"),
    CHATS("Chats", Icons.Filled.ChatBubble, "tab_chats"),
    NOTIFICATIONS("Alerts", Icons.Filled.Notifications, "tab_notifications"),
    PROFILE("Profile", Icons.Filled.Person, "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(
    onOpenComments: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCreatePost: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenFriends: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenMediaGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onLoggedOut: () -> Unit
) {
    // rememberSaveable: the selected tab survives opening a post, chat or profile and coming back.
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Back from Chats / Alerts / Profile goes to Home first (like most apps).
    BackHandler(enabled = selectedTab != MainTab.HOME) { selectedTab = MainTab.HOME }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.testTag("navigation_drawer")) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Avatar(MockRepository.currentUser, 56)
                    Spacer(Modifier.height(8.dp))
                    Text(MockRepository.currentUser.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("@${MockRepository.currentUser.username}", style = MaterialTheme.typography.labelSmall)
                }
                Divider()
                DrawerItem(Icons.Filled.People, "Friends", "drawer_friends") {
                    scope.launch { drawerState.close() }; onOpenFriends()
                }
                DrawerItem(Icons.Filled.Group, "Groups", "drawer_groups") {
                    scope.launch { drawerState.close() }; onOpenGroups()
                }
                DrawerItem(Icons.Filled.PhotoLibrary, "Media Gallery", "drawer_media_gallery") {
                    scope.launch { drawerState.close() }; onOpenMediaGallery()
                }
                Divider()
                DrawerItem(Icons.Filled.Settings, "Settings", "drawer_settings") {
                    scope.launch { drawerState.close() }; onOpenSettings()
                }
                DrawerItem(Icons.Filled.Info, "About", "drawer_about") {
                    scope.launch { drawerState.close() }; onOpenAbout()
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(modifier = Modifier.testTag("bottom_navigation")) {
                    MainTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (tab == MainTab.NOTIFICATIONS) {
                                    val count = MockRepository.unreadNotificationCount()
                                    BadgedBox(badge = { if (count > 0) Badge { Text("$count") } }) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        ) { padding ->
            // consumeWindowInsets: inner screens have their own Scaffold and must not add the
            // status/navigation bar padding a second time.
            Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
                when (selectedTab) {
                    MainTab.HOME -> HomeFeedScreen(
                        onOpenComments = onOpenComments,
                        onOpenProfile = onOpenProfile,
                        onOpenSearch = onOpenSearch,
                        onOpenNotifications = { selectedTab = MainTab.NOTIFICATIONS },
                        onCreatePost = onCreatePost,
                        onOpenMenu = { scope.launch { drawerState.open() } }
                    )
                    MainTab.CHATS -> ChatListScreen(onOpenChat = onOpenChat)
                    MainTab.NOTIFICATIONS -> NotificationsScreen(onOpenProfile = onOpenProfile)
                    MainTab.PROFILE -> ProfileScreen(
                        userId = "me",
                        onBack = { selectedTab = MainTab.HOME },
                        onEditProfile = onOpenEditProfile,
                        onOpenSettings = onOpenSettings,
                        onMessage = { }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, testTag: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).testTag(testTag)
    )
}
