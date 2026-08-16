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

@Composable
fun DashboardScreen(
    collectionRecord: CollectionRecord?,
    collectionRecords: List<CollectionRecord> = emptyList(),
    expenses: List<ExpenseRecord> = emptyList(),
    contributions: List<YearlyContribution> = emptyList(),
    config: GoogleSheetConfig,
    isLoading: Boolean = false,
    syncMessage: String? = null,
    onTriggerSync: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToYearlyReport: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Dynamic Financial calculations
    val totalCollected = remember(collectionRecords, contributions, collectionRecord) {
        if (collectionRecords.isNotEmpty()) {
            collectionRecords.sumOf { it.totalAmount }
        } else if (contributions.isNotEmpty()) {
            contributions.sumOf { it.amount2026 }
        } else {
            collectionRecord?.totalAmount ?: 0.0
        }
    }

    val totalSpent = remember(expenses) { expenses.sumOf { it.amount } }
    val remainingBalance = totalCollected - totalSpent
    val spentPercentage = if (totalCollected > 0) ((totalSpent / totalCollected) * 100).coerceIn(0.0, 100.0).toFloat() else 0f

    val latestCollection = remember(collectionRecords, collectionRecord) {
        collectionRecords.firstOrNull() ?: collectionRecord
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Minimalist Clean Top Bar: Apartment Name + Sync Button + Settings Icon ONLY
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Apartment Name & Subtitle
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
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = "Apartment",
                            tint = Slate900,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = config.spreadsheetTitle.ifBlank { "Gomathi Ilam Thendral" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Maintenance Ledger",
                            fontSize = 11.sp,
                            color = Amber100
                        )
                    }
                }

                // Sync Button and Settings Gear Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onTriggerSync,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber500,
                            contentColor = Slate900
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("dashboard_sync_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Slate900,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLoading) "Syncing" else "Sync",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onNavigateToConfig,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Financial Overview Cards (Row 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Collected",
                amount = "₹${totalCollected.toInt()}",
                subtitle = "${collectionRecords.size} Months Logged",
                icon = Icons.Default.AccountBalanceWallet,
                accentColor = Emerald600,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Total Expenses",
                amount = "₹${totalSpent.toInt()}",
                subtitle = "${expenses.size} Transactions",
                icon = Icons.Default.ReceiptLong,
                accentColor = Rose600,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Net Balance Card (Row 2)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
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
                            text = "Net Cash Balance",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "₹${remainingBalance.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingBalance >= 0) Emerald700 else Rose600
                        )
                    }

                    Surface(
                        color = if (remainingBalance >= 0) Emerald50 else Rose50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (remainingBalance >= 0) "Surplus" else "Deficit",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingBalance >= 0) Emerald700 else Rose600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Budget Utilization",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${spentPercentage.toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Amber600
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { spentPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (spentPercentage > 90) Rose600 else Amber500,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NavButtonChip(
                label = "Collections",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = onNavigateToCollections,
                modifier = Modifier.weight(1f)
            )
            NavButtonChip(
                label = "Expenses",
                icon = Icons.Default.ReceiptLong,
                onClick = onNavigateToExpenses,
                modifier = Modifier.weight(1f)
            )
            NavButtonChip(
                label = "Yearly",
                icon = Icons.Default.BarChart,
                onClick = onNavigateToYearlyReport,
                modifier = Modifier.weight(1f)
            )
            NavButtonChip(
                label = "Directory",
                icon = Icons.Default.PeopleAlt,
                onClick = onNavigateToContacts,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Maintenance Breakdown Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
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
                            text = if (latestCollection != null && latestCollection.month.isNotBlank())
                                "${latestCollection.month} ${latestCollection.year} Maintenance"
                            else "Monthly Maintenance",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Individual flat amounts may differ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    TextButton(onClick = onNavigateToCollections) {
                        Text("View All", fontSize = 12.sp, color = Amber600, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (latestCollection != null) {
                    val flats = listOf(
                        Triple("1A", "M.Madhan Raj", latestCollection.flat1AAmount),
                        Triple("1B", "S.Vasikaran", latestCollection.flat1BAmount),
                        Triple("2A", "S. Hariprasad", latestCollection.flat2AAmount),
                        Triple("2B", "P.Seenivasan", latestCollection.flat2BAmount),
                        Triple("3A", "A. Venkatesh Kumar", latestCollection.flat3AAmount),
                        Triple("3B", "M.Mohan", latestCollection.flat3BAmount)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        flats.chunked(2).forEach { rowFlats ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowFlats.forEach { (flatNo, name, amt) ->
                                    DynamicFlatCard(
                                        flatNo = flatNo,
                                        residentName = name,
                                        amount = amt,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No collection records logged yet. Tap 'Collections' to record maintenance.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Expenses Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
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
                        Text("View All", fontSize = 12.sp, color = Amber600, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (expenses.isEmpty()) {
                    Text(
                        text = "No expenses recorded yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
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
        shape = RoundedCornerShape(14.dp),
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
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
    val isPaid = amount > 0
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = if (isPaid) Slate900 else Rose600,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = flatNo,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = residentName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPaid) "₹${amount.toInt()}" else "Pending",
                        fontSize = 10.sp,
                        color = if (isPaid) Emerald600 else Rose600,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                if (isPaid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isPaid) Emerald600 else Rose600,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
