package com.veroflow.veroconnect.ui.screens

import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.ui.components.Avatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.MockRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onLoggedOut: () -> Unit
) {
    var darkMode by remember { mutableStateOf(MockRepository.darkMode.value) }
    var notificationsEnabled by remember { mutableStateOf(MockRepository.notificationsEnabled.value) }
    var locationEnabled by remember { mutableStateOf(MockRepository.locationEnabled.value) }
    var autoPlay by remember { mutableStateOf(MockRepository.autoPlayVideos.value) }
    var language by remember { mutableStateOf(MockRepository.language.value) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordMessage by remember { mutableStateOf<String?>(null) }
    var passwordMessageIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val me = MockRepository.currentUser

    val languages = listOf("English", "Urdu", "Spanish", "French", "Arabic")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).testTag("list_settings")) {
            ListItem(
                headlineContent = { Text(me.fullName) },
                supportingContent = {
                    Text(if (Backend.accountId != null) me.email else "Guest - activity is not saved")
                },
                leadingContent = { Avatar(me, 44) },
                modifier = Modifier.testTag("row_account")
            )
            Divider()
            SettingsSwitchRow(
                title = "Dark Mode",
                icon = Icons.Filled.DarkMode,
                checked = darkMode,
                testTag = "switch_dark_mode"
            ) {
                darkMode = it
                MockRepository.darkMode.value = it
                MockRepository.saveSettings()
            }
            SettingsSwitchRow(
                title = "Notifications",
                icon = Icons.Filled.Notifications,
                checked = notificationsEnabled,
                testTag = "switch_notifications"
            ) {
                notificationsEnabled = it
                MockRepository.notificationsEnabled.value = it
                MockRepository.saveSettings()
            }
            SettingsSwitchRow(
                title = "Location",
                icon = Icons.Filled.LocationOn,
                checked = locationEnabled,
                testTag = "switch_location"
            ) {
                locationEnabled = it
                MockRepository.locationEnabled.value = it
                MockRepository.saveSettings()
            }
            SettingsSwitchRow(
                title = "Auto Play Videos",
                icon = Icons.Filled.PlayCircle,
                checked = autoPlay,
                testTag = "switch_autoplay"
            ) {
                autoPlay = it
                MockRepository.autoPlayVideos.value = it
                MockRepository.saveSettings()
            }

            Box {
                ListItem(
                    headlineContent = { Text("Language") },
                    supportingContent = { Text(language) },
                    leadingContent = { Icon(Icons.Filled.Language, contentDescription = null) },
                    modifier = Modifier
                        .testTag("row_language")
                        .clickable { languageMenuExpanded = true }
                )
                DropdownMenu(expanded = languageMenuExpanded, onDismissRequest = { languageMenuExpanded = false }) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                language = lang
                                MockRepository.language.value = lang
                                MockRepository.saveSettings()
                                languageMenuExpanded = false
                            },
                            modifier = Modifier.testTag("lang_option_$lang")
                        )
                    }
                }
            }

            ListItem(
                headlineContent = { Text("Privacy") },
                leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) },
                modifier = Modifier
                    .testTag("row_privacy")
                    .clickable { showPrivacySheet = true }
            )
            ListItem(
                headlineContent = { Text("Security") },
                leadingContent = { Icon(Icons.Filled.Security, contentDescription = null) },
                modifier = Modifier
                    .testTag("row_security")
                    .clickable { showSecuritySheet = true }
            )
            ListItem(
                headlineContent = { Text("About") },
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                modifier = Modifier
                    .testTag("row_about")
                    .clickable { onOpenAbout() }
            )
            ListItem(
                headlineContent = { Text("Clear all data") },
                supportingContent = { Text("Delete every account and saved activity on this device") },
                leadingContent = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
                modifier = Modifier
                    .testTag("row_clear_data")
                    .clickable { showClearDataDialog = true }
            )
            ListItem(
                headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .testTag("row_logout")
                    .clickable { showLogoutDialog = true }
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        MockRepository.endSession()
                        onLoggedOut()
                    },
                    modifier = Modifier.testTag("btn_logout_yes")
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }, modifier = Modifier.testTag("btn_logout_no")) {
                    Text("No")
                }
            }
        )
    }

    if (showPrivacySheet) {
        ModalBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 24.dp)) {
                Text("Privacy Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Control who can see your profile, posts, and activity. This is a demo screen for testing purposes.")
            }
        }
    }
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear all data?") },
            text = { Text("This permanently deletes every account and all saved posts, comments and messages on this device, then signs you out.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        MockRepository.clearAllData()
                        onLoggedOut()
                    },
                    modifier = Modifier.testTag("btn_clear_data_yes")
                ) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }, modifier = Modifier.testTag("btn_clear_data_no")) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSecuritySheet) {
        ModalBottomSheet(onDismissRequest = { showSecuritySheet = false }) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 24.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Security Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val accountId = Backend.accountId
                if (accountId == null) {
                    Text("Sign in with a registered account to change your password.")
                } else {
                    Text("Change password", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; passwordMessage = null },
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("field_current_password")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordMessage = null },
                        label = { Text("New password (min 8 characters)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("field_new_password")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it; passwordMessage = null },
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("field_confirm_new_password")
                    )
                    passwordMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            color = if (passwordMessageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("text_password_message")
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            when {
                                oldPassword.isBlank() || newPassword.isBlank() -> {
                                    passwordMessageIsError = true
                                    passwordMessage = "Please fill in all fields"
                                }
                                newPassword.length < 8 -> {
                                    passwordMessageIsError = true
                                    passwordMessage = "New password must be at least 8 characters"
                                }
                                newPassword != confirmNewPassword -> {
                                    passwordMessageIsError = true
                                    passwordMessage = "New passwords do not match"
                                }
                                else -> scope.launch {
                                    val error = withContext(Dispatchers.IO) {
                                        Backend.auth.changePassword(accountId, oldPassword, newPassword)
                                    }
                                    if (error == null) {
                                        passwordMessageIsError = false
                                        passwordMessage = "Password changed successfully"
                                        oldPassword = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                    } else {
                                        passwordMessageIsError = true
                                        passwordMessage = error
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_change_password")
                    ) { Text("Change password") }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.testTag(testTag))
        }
    )
}
