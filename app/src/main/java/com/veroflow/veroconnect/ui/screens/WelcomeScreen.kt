package com.veroflow.veroconnect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.ui.components.AppLogo
import com.veroflow.veroconnect.ui.theme.BrandGradient

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onGuest: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Hero area: takes whatever space is left, so the buttons below are never pushed off screen.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(BrandGradient)
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppLogo(size = 96)
            Spacer(Modifier.height(20.dp))
            Text(
                "Welcome to VeroConnect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect, share and chat with friends — all in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }

        // Buttons: padded for the system navigation bar so "Continue as Guest" is fully visible.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_login")
            ) { Text("Login") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_register")
            ) { Text("Register") }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onGuest, modifier = Modifier.fillMaxWidth().testTag("btn_guest")) {
                Text("Continue as Guest")
            }
        }
    }
}
