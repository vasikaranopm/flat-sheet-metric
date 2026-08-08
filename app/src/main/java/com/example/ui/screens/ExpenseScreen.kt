package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseRecord
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    expenses: List<ExpenseRecord>,
    searchQuery: String,
    selectedCategory: String,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAddExpense: ((particulars: String, amount: Double, category: String, vendor: String, day: String, remarks: String) -> Unit)? = null,
    onDeleteExpense: ((Int) -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Cleaning", "Alteration/Additional work", "Common Purchases")

    val cleaningSum = expenses.filter { it.category.contains("Cleaning", ignoreCase = true) }.sumOf { it.amount }
    val alterationSum = expenses.filter { it.category.contains("Alteration", ignoreCase = true) }.sumOf { it.amount }
    val purchasesSum = expenses.filter { it.category.contains("Purchases", ignoreCase = true) }.sumOf { it.amount }
    val totalExpenseSum = cleaningSum + alterationSum + purchasesSum

    var selectedExpenseItem by remember { mutableStateOf<ExpenseRecord?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    var newParticulars by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var newVendor by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Cleaning") }
    var newRemarks by remember { mutableStateOf("") }
    var newDay by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            if (onAddExpense != null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Slate900,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Common Expense Record", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Gomathi Ilam Thendral • July 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    Surface(
                        color = Rose100,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Spent ₹${totalExpenseSum.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
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
        ) {
            // Search Bar & Filter Chips Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search by vendor, item, or remarks...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_search_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { onCategorySelected(cat) },
                            label = { Text(cat, fontSize = 12.sp) },
                            leadingIcon = if (selectedCategory == cat) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Visual Breakdown Bar Chart Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Category Budget Allocation",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Horizontal Donut/Bar Chart
                            if (totalExpenseSum > 0) {
                                Column {
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                    ) {
                                        val totalWidth = size.width
                                        val cWidth = (cleaningSum / totalExpenseSum).toFloat() * totalWidth
                                        val aWidth = (alterationSum / totalExpenseSum).toFloat() * totalWidth
                                        val pWidth = (purchasesSum / totalExpenseSum).toFloat() * totalWidth

                                        // Cleaning Segment (Amber)
                                        drawRect(
                                            color = Color(0xFFF59E0B),
                                            topLeft = Offset(0f, 0f),
                                            size = Size(cWidth, size.height)
                                        )

                                        // Alteration Segment (Teal)
                                        drawRect(
                                            color = Color(0xFF0D9488),
                                            topLeft = Offset(cWidth, 0f),
                                            size = Size(aWidth, size.height)
                                        )

                                        // Common Purchases Segment (Rose)
                                        drawRect(
                                            color = Color(0xFFE11D48),
                                            topLeft = Offset(cWidth + aWidth, 0f),
                                            size = Size(pWidth, size.height)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Category Legend Items
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        LegendBadge("Cleaning", "₹${cleaningSum.toInt()}", Color(0xFFF59E0B))
                                        LegendBadge("Alteration", "₹${alterationSum.toInt()}", Color(0xFF0D9488))
                                        LegendBadge("Purchases", "₹${purchasesSum.toInt()}", Color(0xFFE11D48))
                                    }
                                }
                            }
                        }
                    }
                }

                // Opening Balance Row
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Slate900,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Amber500)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Opening Balance", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("01-Jul-2026", fontSize = 11.sp, color = Amber100)
                                }
                            }

                            Text("₹12,000", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Amber500)
                        }
                    }
                }

                item {
                    Text(
                        text = "Expense Activity Timeline",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                // Expense Item Cards or Empty State
                if (expenses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Expense Records",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No expense entries found. Tap the + button to add a real expense or configure Google Sheets sync.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(expenses) { expense ->
                        ExpenseCard(
                            expense = expense,
                            onClick = { selectedExpenseItem = expense }
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog
    if (showAddDialog && onAddExpense != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Real Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newParticulars,
                        onValueChange = { newParticulars = it },
                        label = { Text("Expense Particulars") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAmount,
                        onValueChange = { newAmount = it },
                        label = { Text("Amount (₹)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVendor,
                        onValueChange = { newVendor = it },
                        label = { Text("Vendor / Payee") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category (e.g. Cleaning, EB, AMC)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDay,
                        onValueChange = { newDay = it },
                        label = { Text("Date (e.g. 15)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRemarks,
                        onValueChange = { newRemarks = it },
                        label = { Text("Remarks (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = newAmount.toDoubleOrNull() ?: 0.0
                        if (newParticulars.isNotBlank() && amt > 0) {
                            onAddExpense(newParticulars, amt, newCategory, newVendor, newDay, newRemarks)
                            newParticulars = ""
                            newAmount = ""
                            newVendor = ""
                            newRemarks = ""
                            newDay = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detail Dialog for Bill & Picture link reference
    selectedExpenseItem?.let { expense ->
        val context = LocalContext.current
        val hasBill = expense.billAvailable.contains("Available", ignoreCase = true) || expense.billAvailable.startsWith("http")
        val hasPhoto = expense.picture.contains("Available", ignoreCase = true) || expense.picture.startsWith("http")

        AlertDialog(
            onDismissRequest = { selectedExpenseItem = null },
            title = {
                Text(expense.particulars, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Date: ${expense.dateDay} July ${expense.year}", fontSize = 13.sp)
                    Text("Category: ${expense.category}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Amount: ₹${expense.amount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Rose600)
                    Text("Vendor/Payee: ${expense.vendorPayee}", fontSize = 13.sp)
                    Text("Remarks: ${expense.remarks}", fontSize = 13.sp)
                    Text("Running Balance: ₹${expense.balance.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Teal600)

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bill Document:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Surface(
                            color = if (hasBill) Emerald100 else Slate100,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = expense.billAvailable,
                                fontSize = 12.sp,
                                color = if (hasBill) Emerald600 else Slate800,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Photo Verification:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Surface(
                            color = if (hasPhoto) Emerald100 else Slate100,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = expense.picture,
                                fontSize = 12.sp,
                                color = if (hasPhoto) Emerald600 else Slate800,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (hasBill || hasPhoto) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val url = if (expense.billAvailable.startsWith("http")) expense.billAvailable 
                                          else if (expense.picture.startsWith("http")) expense.picture 
                                          else "https://drive.google.com"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Linked Bill / Photo", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedExpenseItem = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun LegendBadge(
    label: String,
    amount: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("$label: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExpenseCard(
    expense: ExpenseRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Slate900,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(expense.dateDay, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("JUL", fontSize = 9.sp, color = Amber500, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = expense.particulars,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vendor: ${expense.vendorPayee}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "- ₹${expense.amount.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Rose600
                    )
                    Surface(
                        color = Teal50,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Bal ₹${expense.balance.toInt()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Teal600,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = expense.remarks,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = expense.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (expense.billAvailable != "N/A") {
                        AssistChip(
                            onClick = onClick,
                            label = { Text("Bill", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    if (expense.picture != "N/A") {
                        AssistChip(
                            onClick = onClick,
                            label = { Text("Photo", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
        }
    }
}
