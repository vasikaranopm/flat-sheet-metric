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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginConfigScreen(
    config: GoogleSheetConfig,
    isLoading: Boolean = false,
    syncMessage: String? = null,
    onLogin: (String, String) -> Unit,
    onSaveGcpConfig: (String, String, String, String, String, String) -> Unit,
    onTriggerSync: () -> Unit,
    onContinueToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var emailInput by remember(config.userEmail) { mutableStateOf(config.userEmail) }
    var webClientIdInput by remember(config.webClientId) { mutableStateOf(config.webClientId) }
    var spreadsheetIdInput by remember(config.spreadsheetId) { mutableStateOf(config.spreadsheetId) }
    var sheetLinkInput by remember(config.spreadsheetId) {
        mutableStateOf(
            if (config.spreadsheetId.isNotBlank() && !config.spreadsheetId.startsWith("http")) {
                "https://docs.google.com/spreadsheets/d/${config.spreadsheetId}/edit"
            } else if (config.spreadsheetId.isNotBlank()) {
                config.spreadsheetId
            } else {
                getDefaultSheetLinkEnv()
            }
        )
    }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var serviceAccountInput by remember(config.serviceAccountEmail) { mutableStateOf(config.serviceAccountEmail) }
    var gcpProjectInput by remember(config.gcpProjectId) { mutableStateOf(config.gcpProjectId) }

    var showGcpSetupModal by remember { mutableStateOf(false) }

    val lastSyncedText = remember(config.lastSyncTime) {
        if (config.lastSyncTime > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            "Last synced: ${sdf.format(Date(config.lastSyncTime))}"
        } else {
            "Not synced yet"
        }
    }

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
            text = config.spreadsheetTitle.ifBlank { "Apartment Maintenance" },
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

        // Google Sheet Status & Validation Card
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
                            text = "Google Sheet Configuration",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (config.spreadsheetId.isNotBlank()) "Sheet ID: ${config.spreadsheetId.take(12)}..." else "No Sheet Link Set",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    AssistChip(
                        onClick = { showGcpSetupModal = true },
                        label = { Text("Link Config", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = lastSyncedText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Amber100)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Slate900,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Validating Google Sheet access & loading data...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate900
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showGcpSetupModal = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Link", fontSize = 13.sp)
                        }

                        Button(
                            onClick = onTriggerSync,
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_data_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Data", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!syncMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isError = syncMessage.contains("Error", ignoreCase = true) || syncMessage.contains("Failed", ignoreCase = true) || syncMessage.contains("Denied", ignoreCase = true)
                    Surface(
                        color = if (isError) Rose100 else Emerald100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) Rose600 else Emerald600,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncMessage,
                                fontSize = 12.sp,
                                color = if (isError) Rose600 else Emerald600,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
            onDismissRequest = { if (!isLoading) showGcpSetupModal = false },
            title = { Text("Configure Google Sheet Link") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Paste your Google Sheet view/edit link below to test access & load data:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = sheetLinkInput,
                        onValueChange = { sheetLinkInput = it },
                        label = { Text("Google Sheet Link / URL") },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sheet_url_input_field")
                    )

                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing sheet access & loading records...", fontSize = 12.sp, color = Amber600)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val extractedId = extractSpreadsheetId(sheetLinkInput)
                        spreadsheetIdInput = extractedId
                        onSaveGcpConfig(
                            extractedId,
                            apiKeyInput,
                            gcpProjectInput,
                            serviceAccountInput,
                            emailInput,
                            webClientIdInput
                        )
                        showGcpSetupModal = false
                    },
                    enabled = !isLoading && sheetLinkInput.isNotBlank()
                ) {
                    Text("Validate & Save Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGcpSetupModal = false },
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun extractSpreadsheetId(input: String): String {
    val trimmed = input.trim()
    val pattern = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
    val match = pattern.find(trimmed)
    return match?.groupValues?.get(1) ?: trimmed
}
