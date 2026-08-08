package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GoogleSheetConfig
import com.example.ui.theme.*

@Composable
fun LoginConfigScreen(
    config: GoogleSheetConfig,
    onLogin: (String, String) -> Unit,
    onSaveGcpConfig: (String, String, String, String, String, String) -> Unit,
    onContinueToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var emailInput by remember(config.userEmail) { mutableStateOf(config.userEmail) }
    var webClientIdInput by remember(config.webClientId) { mutableStateOf(config.webClientId) }
    var spreadsheetIdInput by remember(config.spreadsheetId) { mutableStateOf(config.spreadsheetId) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var serviceAccountInput by remember(config.serviceAccountEmail) { mutableStateOf(config.serviceAccountEmail) }
    var gcpProjectInput by remember(config.gcpProjectId) { mutableStateOf(config.gcpProjectId) }

    var showGcpSetupModal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Header Branding
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Amber500),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Apartment,
                contentDescription = "Apartment Icon",
                tint = Slate900,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Gomathi Ilam Thendral",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Maintenance Collection & Expense Dashboard",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (!config.isLoggedIn) {
            GoogleLoginScreen(
                userEmail = config.userEmail,
                webClientId = config.webClientId,
                onLogin = { email, clientVal ->
                    emailInput = email
                    webClientIdInput = clientVal
                    onLogin(email, clientVal)
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Already signed in card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Emerald100,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Signed In",
                                    tint = Emerald600,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Signed In Account",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = config.userEmail,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Account Authenticated",
                            fontSize = 12.sp,
                            color = Emerald600,
                            fontWeight = FontWeight.Medium
                        )
                        OutlinedButton(
                            onClick = { onLogin("", webClientIdInput) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sign Out", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sheet & GCP Service Account Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = "Sheet",
                        tint = Amber600,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linked Google Sheet",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "GIT - Record Book",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AssistChip(
                        onClick = { },
                        label = { Text("Read-Only", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                OutlinedButton(
                    onClick = { showGcpSetupModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_gcp_config_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Google Sheet Parameters")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinueToDashboard,
            colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate900),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("view_dashboard_button")
        ) {
            Text("Open Maintenance Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showGcpSetupModal) {
        AlertDialog(
            onDismissRequest = { showGcpSetupModal = false },
            title = { Text("Google Sheet & GCP Config") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Enter your Google Sheet parameters:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = spreadsheetIdInput,
                        onValueChange = { spreadsheetIdInput = it },
                        label = { Text("Spreadsheet ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gcpProjectInput,
                        onValueChange = { gcpProjectInput = it },
                        label = { Text("GCP Project ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serviceAccountInput,
                        onValueChange = { serviceAccountInput = it },
                        label = { Text("Service Account Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Optional Google API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveGcpConfig(
                            spreadsheetIdInput,
                            apiKeyInput,
                            gcpProjectInput,
                            serviceAccountInput,
                            emailInput,
                            webClientIdInput
                        )
                        showGcpSetupModal = false
                    }
                ) {
                    Text("Save Config")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGcpSetupModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
