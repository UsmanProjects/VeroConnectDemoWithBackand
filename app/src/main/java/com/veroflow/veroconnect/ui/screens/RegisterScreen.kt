package com.veroflow.veroconnect.ui.screens

import com.veroflow.veroconnect.backend.Backend
import com.veroflow.veroconnect.backend.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.veroflow.veroconnect.data.MockRepository
import kotlinx.coroutines.launch

private fun isValidEmail(email: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onBack: () -> Unit, onRegistered: () -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }
    var subscribeNewsletter by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun validate(): Boolean {
        val e = mutableMapOf<String, String>()
        if (firstName.isBlank()) e["firstName"] = "First name is required"
        if (lastName.isBlank()) e["lastName"] = "Last name is required"
        if (username.isBlank()) e["username"] = "Username is required"
        if (email.isBlank()) e["email"] = "Email is required"
        else if (!isValidEmail(email)) e["email"] = "Enter a valid email address"
        if (phone.isBlank()) e["phone"] = "Phone number is required"
        if (password.isBlank()) e["password"] = "Password is required"
        else if (password.length < 8) e["password"] = "Password must be at least 8 characters"
        if (confirmPassword != password) e["confirmPassword"] = "Passwords do not match"
        if (!acceptTerms) e["terms"] = "You must accept the Terms to continue"
        errors = e
        return e.isEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = firstName, onValueChange = { firstName = it },
                label = { Text("First Name") }, singleLine = true,
                isError = errors.containsKey("firstName"),
                supportingText = { errors["firstName"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_first_name")
            )
            OutlinedTextField(
                value = lastName, onValueChange = { lastName = it },
                label = { Text("Last Name") }, singleLine = true,
                isError = errors.containsKey("lastName"),
                supportingText = { errors["lastName"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_last_name")
            )
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("Username") }, singleLine = true,
                isError = errors.containsKey("username"),
                supportingText = { errors["username"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_username")
            )
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errors.containsKey("email"),
                supportingText = { errors["email"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_email")
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone Number") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = errors.containsKey("phone"),
                supportingText = { errors["phone"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_phone")
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.testTag("btn_toggle_password")) {
                        Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle password visibility")
                    }
                },
                isError = errors.containsKey("password"),
                supportingText = { errors["password"]?.let { Text(it) } ?: Text("At least 8 characters") },
                modifier = Modifier.fillMaxWidth().testTag("field_password")
            )
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = errors.containsKey("confirmPassword"),
                supportingText = { errors["confirmPassword"]?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth().testTag("field_confirm_password")
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("checkbox_terms_row")) {
                Checkbox(checked = acceptTerms, onCheckedChange = { acceptTerms = it }, modifier = Modifier.testTag("checkbox_terms"))
                Text("I accept the Terms & Conditions")
            }
            errors["terms"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("checkbox_newsletter_row")) {
                Checkbox(checked = subscribeNewsletter, onCheckedChange = { subscribeNewsletter = it }, modifier = Modifier.testTag("checkbox_newsletter"))
                Text("Subscribe to Newsletter")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (isLoading) return@Button
                    if (validate()) {
                        isLoading = true
                        scope.launch {
                            delay(400)
                            // The account is saved in the database (password hashed) off the main thread.
                            val result = withContext(Dispatchers.IO) {
                                Backend.auth.register(firstName, lastName, username, email, phone, password)
                            }
                            isLoading = false
                            when (result) {
                                is AuthResult.Success -> {
                                    MockRepository.toast.value = "Account created successfully. Please log in."
                                    onRegistered()
                                }
                                is AuthResult.Failure -> {
                                    val field = result.field
                                    if (field != null) errors = errors + (field to result.message)
                                    snackbarHostState.showSnackbar(result.message)
                                }
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fix the errors above")
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().testTag("btn_submit_register")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Register")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
