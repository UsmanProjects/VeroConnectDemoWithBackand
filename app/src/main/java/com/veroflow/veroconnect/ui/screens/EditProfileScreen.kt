package com.veroflow.veroconnect.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.ImageUrls
import com.veroflow.veroconnect.data.MockRepository
import com.veroflow.veroconnect.ui.components.RemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val current = MockRepository.currentUser
    var name by remember { mutableStateOf(current.fullName) }
    var bio by remember { mutableStateOf(current.bio) }
    var phone by remember { mutableStateOf(current.phone) }
    var address by remember { mutableStateOf(current.address) }
    var website by remember { mutableStateOf(current.website) }
    var avatarUrl by remember { mutableStateOf(MockRepository.myAvatarUrl.value) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showPhotoChoices by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RemoteImage(
                url = avatarUrl,
                fallbackColor = current.avatarColor,
                modifier = Modifier.size(96.dp).clip(CircleShape)
            )
            TextButton(onClick = { showPhotoSheet = true; showPhotoChoices = false }, modifier = Modifier.testTag("btn_change_photo")) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Change profile picture")
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = { Text("Name") },
                singleLine = true,
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_name")
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth().testTag("field_bio"))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("field_phone"))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth().testTag("field_address"))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("field_website"))

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Name is required"
                        return@Button
                    }
                    val parts = name.trim().split(" ", limit = 2)
                    MockRepository.currentUser = current.copy(
                        firstName = parts[0],
                        lastName = parts.getOrNull(1)?.trim().orEmpty(),
                        bio = bio,
                        phone = phone,
                        address = address,
                        website = website
                    )
                    MockRepository.myAvatarUrl.value = avatarUrl
                    MockRepository.saveProfile()
                    // Shown on the profile screen (this screen closes right away).
                    MockRepository.toast.value = "Profile updated"
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_save_profile")
            ) { Text("Save") }
        }
    }

    if (showPhotoSheet) {
        ModalBottomSheet(onDismissRequest = { showPhotoSheet = false }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 24.dp)) {
                ListItem(
                    headlineContent = { Text("Take a photo") },
                    leadingContent = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                    modifier = Modifier.testTag("btn_photo_camera").clickable {
                        // Simulated camera: pick a random portrait.
                        avatarUrl = ImageUrls.myAvatarOptions.random()
                        showPhotoSheet = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Choose from gallery") },
                    leadingContent = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                    modifier = Modifier.testTag("btn_photo_gallery").clickable {
                        showPhotoChoices = true
                    }
                )
                if (showPhotoChoices) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(ImageUrls.myAvatarOptions) { index, url ->
                            RemoteImage(
                                url = url,
                                fallbackColor = current.avatarColor,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(
                                        if (url == avatarUrl) 3.dp else 0.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                    .clickable {
                                        avatarUrl = url
                                        showPhotoSheet = false
                                    }
                                    .testTag("photo_option_$index")
                            )
                        }
                    }
                }
            }
        }
    }
}
