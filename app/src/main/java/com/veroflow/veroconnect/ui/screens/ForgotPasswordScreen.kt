package com.veroflow.veroconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().padding(24.dp)) {
            if (!sent) {
                Text("Enter your email and we'll send you a reset link.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("field_email")
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { if (email.isNotBlank()) sent = true },
                    modifier = Modifier.fillMaxWidth().testTag("btn_send_reset")
                ) {
                    Text("Send Reset Link")
                }
            } else {
                Text("Reset link sent to $email", modifier = Modifier.testTag("text_reset_sent"))
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack, modifier = Modifier.testTag("btn_back_to_login")) {
                    Text("Back to Login")
                }
            }
        }
    }
}
