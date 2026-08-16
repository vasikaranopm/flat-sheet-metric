package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectionRecord
import com.example.data.model.OwnerContact
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    collectionRecords: List<CollectionRecord>,
    ownerContacts: List<OwnerContact> = emptyList(),
    onAddCollection: (String, String, Double, String, String) -> Unit,
    onUpdateCollection: (CollectionRecord) -> Unit,
    onDeleteCollection: (Int) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedMonthFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<CollectionRecord?>(null) }
    var recordToDelete by remember { mutableStateOf<CollectionRecord?>(null) }

    val availableMonths = remember(collectionRecords) {
        listOf("All") + collectionRecords.map { it.month }.filter { it.isNotBlank() }.distinct()
    }

    val filteredRecords = remember(collectionRecords, selectedMonthFilter) {
        if (selectedMonthFilter == "All") {
            collectionRecords
        } else {
            collectionRecords.filter { it.month.equals(selectedMonthFilter, ignoreCase = true) }
        }
    }

    val totalCollected = remember(filteredRecords) {
        filteredRecords.sumOf { it.totalAmount }
    }

    val averageMonthly = remember(filteredRecords) {
        if (filteredRecords.isNotEmpty()) totalCollected / filteredRecords.size else 0.0
    }

    val averagePerFlat = remember(averageMonthly) {
        averageMonthly / 6.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Maintenance Collections",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "6 Flats (1A, 1B, 2A, 2B, 3A, 3B)",
                            fontSize = 11.sp,
                            color = Slate800.copy(alpha = 0.7f)
                        )
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
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_collection_header_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Collection", tint = Amber600)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Amber500,
                contentColor = Slate900,
                modifier = Modifier.testTag("add_collection_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record Month")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Maintenance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Building Collection",
                                fontSize = 12.sp,
                                color = Amber100
                            )
                            Text(
                                text = "₹${totalCollected.toInt()}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            color = Amber500,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Slate900,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${filteredRecords.size} Months",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Avg/Flat: ₹${averagePerFlat.toInt()}/mo",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Monthly Total: ₹${averageMonthly.toInt()}",
                            fontSize = 11.sp,
                            color = Amber100.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Month Filter Chips
            if (availableMonths.size > 2) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableMonths) { month ->
                        val isSelected = selectedMonthFilter == month
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedMonthFilter = month },
                            label = {
                                Text(
                                    month,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Amber500,
                                selectedLabelColor = Slate900
                            )
                        )
                    }
                }
            }

            // Collection Records List
            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Slate800.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No maintenance records yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Record Maintenance' to add monthly maintenance value.",
                            fontSize = 12.sp,
                            color = Slate800.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        MonthlyTotalCard(
                            record = record,
                            onEdit = { recordToEdit = record },
                            onDelete = { recordToDelete = record }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || recordToEdit != null) {
        MaintenanceTotalDialog(
            initialRecord = recordToEdit,
            onDismiss = {
                showAddDialog = false
                recordToEdit = null
            },
            onSave = { year, month, totalAmount, particulars, remarks ->
                if (recordToEdit != null) {
                    val perFlat = totalAmount / 6.0
                    val updated = recordToEdit!!.copy(
                        year = year,
                        month = month,
                        particulars = particulars,
                        remarks = remarks,
                        flat1AAmount = perFlat,
                        flat1BAmount = perFlat,
                        flat2AAmount = perFlat,
                        flat2BAmount = perFlat,
                        flat3AAmount = perFlat,
                        flat3BAmount = perFlat,
                        totalAmount = totalAmount
                    )
                    onUpdateCollection(updated)
                } else {
                    onAddCollection(year, month, totalAmount, particulars, remarks)
                }
                showAddDialog = false
                recordToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Delete Maintenance Record?") },
            text = {
                Text("Are you sure you want to delete the maintenance record for ${recordToDelete!!.month} ${recordToDelete!!.year} (Total: ₹${recordToDelete!!.totalAmount.toInt()}, ₹${(recordToDelete!!.totalAmount / 6.0).toInt()}/flat)?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCollection(recordToDelete!!.id)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MonthlyTotalCard(
    record: CollectionRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val f1a = if (record.flat1AAmount > 0) record.flat1AAmount else record.totalAmount / 6.0
    val f1b = if (record.flat1BAmount > 0) record.flat1BAmount else record.totalAmount / 6.0
    val f2a = if (record.flat2AAmount > 0) record.flat2AAmount else record.totalAmount / 6.0
    val f2b = if (record.flat2BAmount > 0) record.flat2BAmount else record.totalAmount / 6.0
    val f3a = if (record.flat3AAmount > 0) record.flat3AAmount else record.totalAmount / 6.0
    val f3b = if (record.flat3BAmount > 0) record.flat3BAmount else record.totalAmount / 6.0

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Amber100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Amber600,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${record.month} ${record.year}".trim(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "6 Flats • Monthly Maintenance",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "₹${record.totalAmount.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Emerald700
                            )
                            Text(
                                text = "Month Total",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald700.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate800, modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Rose600, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6 Flats individual collection chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val flatItems = listOf(
                    "1A" to f1a,
                    "1B" to f1b,
                    "2A" to f2a,
                    "2B" to f2b,
                    "3A" to f3a,
                    "3B" to f3b
                )
                flatItems.forEach { (flat, amt) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = flat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "₹${amt.toInt()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Amber600
                            )
                        }
                    }
                }
            }

            if (record.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${record.remarks}",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MaintenanceTotalDialog(
    initialRecord: CollectionRecord?,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, String) -> Unit
) {
    var yearInput by remember { mutableStateOf(initialRecord?.year?.ifBlank { "2026" } ?: "2026") }
    var monthInput by remember { mutableStateOf(initialRecord?.month?.ifBlank { "September" } ?: "September") }

    // Rate per Flat vs Total
    var perFlatInput by remember {
        mutableStateOf(
            if (initialRecord != null && initialRecord.totalAmount > 0) {
                (initialRecord.totalAmount / 6.0).toInt().toString()
            } else {
                "1000"
            }
        )
    }

    var totalAmountInput by remember {
        mutableStateOf(
            if (initialRecord != null && initialRecord.totalAmount > 0) {
                initialRecord.totalAmount.toInt().toString()
            } else {
                "6000"
            }
        )
    }

    var particularsInput by remember { mutableStateOf(initialRecord?.particulars?.ifBlank { "Monthly Maintenance" } ?: "Monthly Maintenance") }
    var remarksInput by remember { mutableStateOf(initialRecord?.remarks ?: "") }

    val presetPerFlatRates = listOf("1000", "1200", "1250", "1500", "2000")
    val monthsList = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = Amber600)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialRecord != null) "Edit Monthly Maintenance" else "Record Monthly Maintenance",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Specify maintenance value per flat or total for the 6 flats:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Month & Year Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = monthInput,
                        onValueChange = { monthInput = it },
                        label = { Text("Month", fontSize = 12.sp) },
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = yearInput,
                        onValueChange = { yearInput = it },
                        label = { Text("Year", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // Month Quick Select
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(monthsList) { m ->
                        val isSelected = monthInput.equals(m, ignoreCase = true)
                        SuggestionChip(
                            onClick = { monthInput = m },
                            label = { Text(m.take(3), fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) Amber500 else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                // Maintenance Value Per Flat Input
                OutlinedTextField(
                    value = perFlatInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }
                        perFlatInput = clean
                        val rate = clean.toDoubleOrNull() ?: 0.0
                        totalAmountInput = (rate * 6).toInt().toString()
                    },
                    label = { Text("Maintenance Value per Flat (₹)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g. 1000 or 1200") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Amber600) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Presets Per Flat
                Column {
                    Text(
                        text = "Quick Presets per Flat (6 flats):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presetPerFlatRates) { rate ->
                            val isSelected = perFlatInput == rate
                            SuggestionChip(
                                onClick = {
                                    perFlatInput = rate
                                    val r = rate.toDoubleOrNull() ?: 0.0
                                    totalAmountInput = (r * 6).toInt().toString()
                                },
                                label = { Text("₹$rate / flat", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) Amber500 else MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }

                // Total Calculation Display / Direct edit
                Surface(
                    color = Emerald50,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Building Collection",
                                fontSize = 11.sp,
                                color = Emerald700.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "₹$totalAmountInput (${perFlatInput.ifBlank { "0" }} × 6 flats)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700
                            )
                        }

                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = particularsInput,
                    onValueChange = { particularsInput = it },
                    label = { Text("Particulars / Description", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("Remarks / Notes (Optional)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val total = totalAmountInput.toDoubleOrNull() ?: 0.0
                    if (monthInput.isNotBlank() && total > 0) {
                        onSave(yearInput.trim(), monthInput.trim(), total, particularsInput.trim(), remarksInput.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate900),
                modifier = Modifier.testTag("save_collection_button")
            ) {
                Text("Save Record", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
