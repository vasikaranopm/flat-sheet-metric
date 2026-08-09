package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MajorWork
import com.example.data.model.YearlyContribution
import com.example.data.model.YearlyExpenseCategory
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportScreen(
    contributions: List<YearlyContribution>,
    categories: List<YearlyExpenseCategory>,
    majorWorks: List<MajorWork>,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val totalContributions = contributions.sumOf { it.amount2026 }
    val totalExpenses = categories.sumOf { it.amount2026 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("2026 Yearly Financial Report", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Gomathi Ilam Thendral • Annual Summary", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Annual Overview Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("2026 Financial Overview", fontSize = 14.sp, color = Amber100)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Collected", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("₹${totalContributions.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Expenses", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                            Text("₹${totalExpenses.toInt()}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Rose600)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Contribution Summary Table
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
                        Text("Contribution Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Year 2026", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Amber600)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, shape = RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Resident", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Text("Amount (₹)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    contributions.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Slate900,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.flatNo,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.residentName, fontSize = 13.sp)
                            }
                            Text("₹${item.amount2026.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(color = Slate100, thickness = 0.5.dp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total Collection", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("₹${totalContributions.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Emerald600)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Expense Summary Table
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
                        Text("Expense Summary by Category", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Year 2026", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Amber600)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate100, shape = RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                        Text("Amount Spent (₹)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    categories.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.category, fontSize = 13.sp)
                            Text(
                                text = if (item.amount2026 > 0) "₹${item.amount2026.toInt()}" else "-",
                                fontSize = 13.sp,
                                fontWeight = if (item.amount2026 > 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (item.amount2026 > 0) MaterialTheme.colorScheme.onSurface else Slate800.copy(alpha = 0.4f)
                            )
                        }
                        Divider(color = Slate100, thickness = 0.5.dp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total Expense", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("₹${totalExpenses.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Rose600)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Major Capital Works
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Amber600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Major Capital Works 2026", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    majorWorks.forEach { work ->
                        Surface(
                            color = Amber100.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(work.description, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("₹${work.amount2026.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Amber600)
                            }
                        }
                    }
                }
            }
        }
    }
}
