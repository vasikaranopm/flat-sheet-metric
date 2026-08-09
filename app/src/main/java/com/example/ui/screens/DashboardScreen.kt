package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectionRecord
import com.example.data.model.GoogleSheetConfig
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    collectionRecord: CollectionRecord?,
    config: GoogleSheetConfig,
    onTriggerSync: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToYearlyReport: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Calculated financial aggregates
    val totalCollected = collectionRecord?.totalAmount ?: 0.0
    val totalSpent = 0.0
    val remainingBalance = totalCollected - totalSpent
    val spentPercentage = if (totalCollected > 0) ((totalSpent / totalCollected) * 100).toFloat() else 0f

    val flats = listOf(
        FlatInfo("1A", "Flat 1A", collectionRecord?.flat1AAmount ?: 0.0),
        FlatInfo("1B", "Flat 1B", collectionRecord?.flat1BAmount ?: 0.0),
        FlatInfo("2A", "Flat 2A", collectionRecord?.flat2AAmount ?: 0.0),
        FlatInfo("2B", "Flat 2B", collectionRecord?.flat2BAmount ?: 0.0),
        FlatInfo("3A", "Flat 3A", collectionRecord?.flat3AAmount ?: 0.0),
        FlatInfo("3B", "Flat 3B", collectionRecord?.flat3BAmount ?: 0.0)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Amber500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apartment, contentDescription = null, tint = Slate900)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gomathi Ilam Thendral",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Maintenance Record Book",
                                fontSize = 12.sp,
                                color = Amber100
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToConfig,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Connection badge row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (config.isLoggedIn) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (config.isLoggedIn) Emerald600 else Amber500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (config.isLoggedIn) {
                                if (config.userEmail.isNotEmpty()) "Live Sheet Synced (${config.userEmail})" else "Live Sheet Synced"
                            } else "Initial Mode (Local Offline Record)",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (config.isLoggedIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onTriggerSync,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", tint = Amber500, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", fontSize = 12.sp, color = Amber500)
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            TextButton(
                                onClick = onLogout,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("dashboard_logout_button")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = Rose600, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sign Out", fontSize = 12.sp, color = Rose600, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        TextButton(
                            onClick = onNavigateToConfig,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Login", tint = Amber500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Login", fontSize = 12.sp, color = Amber500, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Metric Cards (Row 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Collected",
                amount = "₹${totalCollected.toInt()}",
                subtitle = "6 of 6 Flats Paid",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = Emerald600,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Total Spent",
                amount = "₹${totalSpent.toInt()}",
                subtitle = "July 2026",
                icon = Icons.Default.ReceiptLong,
                accentColor = Rose600,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Remaining Balance & Budget Bar
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
                        Text("Cash Balance Available (Surplus)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("₹${remainingBalance.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Teal600)
                    }
                    Surface(
                        color = Teal50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Net Surplus: ₹${remainingBalance.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

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
                    color = Amber500,
                    trackColor = Slate100
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Navigation Section
        Text(
            text = "Record Book Sections",
            fontSize = 16.sp,
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
                label = "Contacts",
                icon = Icons.Default.ContactPhone,
                onClick = onNavigateToContacts,
                modifier = Modifier
                    .weight(1f)
                    .testTag("nav_contacts_button")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sheet 1: Maintenance Collection Record (July 2026)
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
                            text = "Collection Record",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "July 2026 • Monthly Maintenance",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Surface(
                        color = Amber100,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Total ₹12,000",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Amber600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown Remarks Note
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Particulars: ₹1,000 Regular Maintenance + ₹1,000 Motor Sensor Contribution = ₹2,000 per flat",
                            fontSize = 11.sp,
                            color = Slate800
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Resident Contributions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Grid of 6 Flats
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    flats.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { flat ->
                                FlatCard(
                                    flat = flat,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class FlatInfo(
    val flatNo: String,
    val residentName: String,
    val amount: Double
)

@Composable
fun MetricCard(
    title: String,
    amount: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
fun FlatCard(
    flat: FlatInfo,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = flat.flatNo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = flat.residentName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Paid ₹${flat.amount.toInt()}",
                        fontSize = 11.sp,
                        color = Emerald600,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(Icons.Default.CheckCircle, contentDescription = "Paid", tint = Emerald600, modifier = Modifier.size(18.dp))
        }
    }
}
