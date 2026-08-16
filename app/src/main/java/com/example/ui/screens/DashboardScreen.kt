package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectionRecord
import com.example.data.model.ExpenseRecord
import com.example.data.model.GoogleSheetConfig
import com.example.data.model.YearlyContribution
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    collectionRecord: CollectionRecord?,
    expenses: List<ExpenseRecord> = emptyList(),
    contributions: List<YearlyContribution> = emptyList(),
    config: GoogleSheetConfig,
    isLoading: Boolean = false,
    syncMessage: String? = null,
    onTriggerSync: () -> Unit,
    onLogout: () -> Unit,
    onUpdateSheetUrl: (String) -> Unit = {},
    onNavigateToExpenses: () -> Unit,
    onNavigateToYearlyReport: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showEditSheetDialog by remember { mutableStateOf(false) }
    var editSheetUrlInput by remember { mutableStateOf("") }

    val lastSyncedText = remember(config.lastSyncTime) {
        if (config.lastSyncTime > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            "Synced: ${sdf.format(Date(config.lastSyncTime))}"
        } else {
            "Not synced yet"
        }
    }

    // Dynamic Financial aggregates from live synced data
    val totalCollected = if (contributions.isNotEmpty()) {
        contributions.sumOf { it.amount2026 }
    } else {
        collectionRecord?.totalAmount ?: 0.0
    }
    val totalSpent = expenses.sumOf { it.amount }
    val remainingBalance = totalCollected - totalSpent
    val spentPercentage = if (totalCollected > 0) ((totalSpent / totalCollected) * 100).coerceIn(0.0, 100.0).toFloat() else 0f

    val latestMonth = remember(expenses) {
        expenses.firstOrNull { it.month.isNotBlank() }?.month ?: ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Syncing Banner Indicator
        if (isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Amber100),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Slate900,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Syncing Google Sheet Live...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Fetching real-time records from Google Sheets",
                            fontSize = 11.sp,
                            color = Slate800
                        )
                    }
                }
            }
        }

        // Diagnostic Banner Indicator / Sync Message
        if (!isLoading && syncMessage != null && syncMessage.isNotBlank()) {
            val isError = syncMessage.contains("Reason", ignoreCase = true) ||
                    syncMessage.contains("Error", ignoreCase = true) ||
                    syncMessage.contains("Restricted", ignoreCase = true) ||
                    syncMessage.contains("Denied", ignoreCase = true) ||
                    syncMessage.contains("Failed", ignoreCase = true) ||
                    syncMessage.contains("404", ignoreCase = true) ||
                    syncMessage.contains("403", ignoreCase = true) ||
                    syncMessage.contains("401", ignoreCase = true) ||
                    syncMessage.contains("Unable", ignoreCase = true) ||
                    syncMessage.contains("Invalid", ignoreCase = true)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) Rose50 else Emerald50
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) Rose600 else Emerald600,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isError) "Google Sheet Sync Diagnostic" else "Sync Status",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isError) Rose900 else Emerald900
                            )
                        }
                        if (isError) {
                            TextButton(
                                onClick = {
                                    editSheetUrlInput = if (config.spreadsheetId.isNotBlank() && !config.spreadsheetId.startsWith("http")) {
                                        "https://docs.google.com/spreadsheets/d/${config.spreadsheetId}/edit"
                                    } else config.spreadsheetId
                                    showEditSheetDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Change URL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Rose700)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = syncMessage,
                        fontSize = 12.sp,
                        color = if (isError) Rose800 else Emerald800,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Top Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Amber500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apartment, contentDescription = null, tint = Slate900)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = config.spreadsheetTitle.ifBlank { "Apartment Maintenance Ledger" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (latestMonth.isNotBlank()) "Live Sheet Ledger • $latestMonth" else "Live Maintenance Ledger",
                                fontSize = 12.sp,
                                color = Amber100
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = Color.White)
                        }
                        IconButton(
                            onClick = onNavigateToConfig,
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }

                if (config.userEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Signed in: ${config.userEmail}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            Text(
                                text = "Sign Out",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Amber400,
                                modifier = Modifier.clickable { onLogout() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Connection badge row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                editSheetUrlInput = if (config.spreadsheetId.isNotBlank() && !config.spreadsheetId.startsWith("http")) {
                                    "https://docs.google.com/spreadsheets/d/${config.spreadsheetId}/edit"
                                } else config.spreadsheetId
                                showEditSheetDialog = true
                            }
                    ) {
                        Icon(
                            imageVector = if (config.spreadsheetId.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (config.spreadsheetId.isNotBlank()) Emerald600 else Amber500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (config.spreadsheetId.isNotBlank()) "Google Sheet Linked" else "No Sheet Configured",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Sheet URL",
                                    tint = Amber400,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = lastSyncedText,
                                fontSize = 10.sp,
                                color = Amber100
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                editSheetUrlInput = if (config.spreadsheetId.isNotBlank() && !config.spreadsheetId.startsWith("http")) {
                                    "https://docs.google.com/spreadsheets/d/${config.spreadsheetId}/edit"
                                } else config.spreadsheetId
                                showEditSheetDialog = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Sheet URL",
                                tint = Amber400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = onTriggerSync,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate900),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("dashboard_sync_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = Slate900,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Live", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (showEditSheetDialog) {
            AlertDialog(
                onDismissRequest = { showEditSheetDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Amber600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Google Sheet URL", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Enter or paste the full Google Sheet link or Sheet ID for your apartment maintenance ledger:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editSheetUrlInput,
                            onValueChange = { editSheetUrlInput = it },
                            placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                            singleLine = true,
                            trailingIcon = {
                                if (editSheetUrlInput.isNotEmpty()) {
                                    IconButton(onClick = { editSheetUrlInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_sheet_url_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ensure the sheet is shared as 'Anyone with link can view'.",
                            fontSize = 11.sp,
                            color = Amber600
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showEditSheetDialog = false
                            onUpdateSheetUrl(editSheetUrlInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate900, contentColor = Color.White)
                    ) {
                        Text("Update & Sync", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditSheetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // If no records exist at all, show Connect Sheet / Diagnostic action card
        if (expenses.isEmpty() && contributions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (syncMessage != null && (syncMessage.contains("Reason", true) || syncMessage.contains("Error", true) || syncMessage.contains("Restricted", true))) Icons.Default.CloudOff else Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (syncMessage != null && (syncMessage.contains("Reason", true) || syncMessage.contains("Error", true) || syncMessage.contains("Restricted", true))) Rose600 else Amber600,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (config.spreadsheetId.isNotBlank()) "Google Sheet Data Not Loaded" else "Ready to Connect Google Sheet",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (syncMessage != null && syncMessage.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Diagnostic Check Result:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber600
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = syncMessage,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Connect your Google Sheet maintenance ledger to load live records, expenses, and flat collections.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                editSheetUrlInput = if (config.spreadsheetId.isNotBlank() && !config.spreadsheetId.startsWith("http")) {
                                    "https://docs.google.com/spreadsheets/d/${config.spreadsheetId}/edit"
                                } else config.spreadsheetId
                                showEditSheetDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("connect_sheet_button")
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update Sheet Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onTriggerSync,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Live Summary Metric Cards (Row 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Collected",
                    amount = "₹${totalCollected.toInt()}",
                    subtitle = if (contributions.isNotEmpty()) "${contributions.size} Flats Paid" else "From Ledger",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = Emerald600,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Total Spent",
                    amount = "₹${totalSpent.toInt()}",
                    subtitle = "${expenses.size} live expenses",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = Rose600,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remaining Balance & Budget Utilization Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Net Cash Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                "₹${remainingBalance.toInt()}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remainingBalance >= 0) Teal600 else Rose600
                            )
                        }
                        Surface(
                            color = if (remainingBalance >= 0) Teal50 else Rose100,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (remainingBalance >= 0) "Surplus: ₹${remainingBalance.toInt()}" else "Deficit: ₹${remainingBalance.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBalance >= 0) Teal600 else Rose600,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (totalCollected > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Budget Utilized", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Text("${spentPercentage.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { spentPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (spentPercentage > 90) Rose600 else Amber500,
                            trackColor = Slate100
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Navigation Buttons
            Text(
                text = "Ledger Views",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NavButtonChip(
                    label = "Expenses",
                    icon = Icons.Default.Payments,
                    onClick = onNavigateToExpenses,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_expenses_button")
                )
                NavButtonChip(
                    label = "Yearly Report",
                    icon = Icons.Default.BarChart,
                    onClick = onNavigateToYearlyReport,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_yearly_report_button")
                )
                NavButtonChip(
                    label = "Directory",
                    icon = Icons.Default.ContactPhone,
                    onClick = onNavigateToContacts,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_contacts_button")
                )
            }

            // Dynamic Flat Contributions Section (If detected in the sheet)
            if (contributions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Flat Collections",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${contributions.size} Flats Synced from Sheet",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Surface(
                                color = Amber100,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "₹${totalCollected.toInt()}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber600,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            contributions.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { contrib ->
                                        DynamicFlatCard(
                                            flatNo = contrib.flatNo,
                                            residentName = contrib.residentName.ifBlank { "Flat ${contrib.flatNo}" },
                                            amount = contrib.amount2026,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Live Expenses List
            if (expenses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions (${expenses.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToExpenses) {
                                Text("View All", fontSize = 12.sp, color = Amber600)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            expenses.take(5).forEach { exp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { onNavigateToExpenses() }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exp.particulars,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            if (exp.dateDay.isNotBlank()) {
                                                Text(
                                                    text = exp.dateDay,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(" • ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                            }
                                            Text(
                                                text = exp.category.ifBlank { "General" },
                                                fontSize = 11.sp,
                                                color = Amber600,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Text(
                                        text = "₹${exp.amount.toInt()}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Rose600
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(amount, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Text(subtitle, fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun NavButtonChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DynamicFlatCard(
    flatNo: String,
    residentName: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = flatNo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = residentName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "₹${amount.toInt()}",
                        fontSize = 11.sp,
                        color = Emerald600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(Icons.Default.CheckCircle, contentDescription = "Paid", tint = Emerald600, modifier = Modifier.size(16.dp))
        }
    }
}
