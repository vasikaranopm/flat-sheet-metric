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
    onLogout: () -> Unit = {},
    onUpdateSheetUrl: (String) -> Unit = {},
    onSaveGcpConfig: (String, String, String, String, String, String) -> Unit,
    onTriggerSync: () -> Unit,
    onContinueToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sheetLinkInput by remember(config.spreadsheetId) {
        mutableStateOf(
            if (config.spreadsheetId.isNotBlank() && !config.spreadsheetId.startsWith("http")) {
                "https://docs.google.com/spreadsheets/d/${config.spreadsheetId}/edit"
            } else if (config.spreadsheetId.isNotBlank()) {
                config.spreadsheetId
            } else {
                val def = getDefaultSheetLinkEnv()
                if (def.isNotBlank()) "https://docs.google.com/spreadsheets/d/$def/edit" else ""
            }
        )
    }

    val lastSyncedText = remember(config.lastSyncTime) {
        if (config.lastSyncTime > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            "Last Synced: ${sdf.format(Date(config.lastSyncTime))}"
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
        Spacer(modifier = Modifier.height(12.dp))

        // Branding Header
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Amber500),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = "Sync Icon",
                tint = Slate900,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Google Sheet Live Sync",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Connect your Google Sheet maintenance ledger to sync live financial records.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (config.userEmail.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Authenticated User",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = config.userEmail,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign Out", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Sheet Link Configuration Card
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
                        contentDescription = null,
                        tint = Amber600,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google Sheet Link / URL",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sheetLinkInput,
                    onValueChange = { sheetLinkInput = it },
                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (sheetLinkInput.isNotEmpty()) {
                            IconButton(onClick = { sheetLinkInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sheet_url_input_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sharing Instructions Callout
                Surface(
                    color = Amber50,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Amber600,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "To enable live syncing, in Google Sheets tap Share -> General Access -> set to 'Anyone with the link can view'.",
                            fontSize = 11.sp,
                            color = Slate800,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = lastSyncedText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sync / Action Buttons
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
                            text = "Connecting to Google Sheet & fetching live data...",
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
                            onClick = {
                                onUpdateSheetUrl(sheetLinkInput)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Link", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onUpdateSheetUrl(sheetLinkInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_data_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Live Data", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!syncMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val isError = syncMessage.contains("Reason", ignoreCase = true) ||
                            syncMessage.contains("Error", ignoreCase = true) ||
                            syncMessage.contains("Failed", ignoreCase = true) ||
                            syncMessage.contains("Denied", ignoreCase = true) ||
                            syncMessage.contains("Restricted", ignoreCase = true) ||
                            syncMessage.contains("404", ignoreCase = true) ||
                            syncMessage.contains("403", ignoreCase = true) ||
                            syncMessage.contains("401", ignoreCase = true) ||
                            syncMessage.contains("Unable", ignoreCase = true) ||
                            syncMessage.contains("Invalid", ignoreCase = true)
                    Surface(
                        color = if (isError) Rose100 else Emerald100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) Rose600 else Emerald600,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncMessage,
                                fontSize = 12.sp,
                                color = if (isError) Rose800 else Emerald800,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onContinueToDashboard,
            colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate900),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("view_dashboard_button")
        ) {
            Text("Open Maintenance Dashboard", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
