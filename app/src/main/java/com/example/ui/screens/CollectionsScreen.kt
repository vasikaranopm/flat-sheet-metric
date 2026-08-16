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
    onAddCollection: (String, String, String, String, Double, Double, Double, Double, Double, Double) -> Unit,
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

    // Helper map of flat numbers to resident names
    val residentNames = remember(ownerContacts) {
        ownerContacts.associate { it.flatNo.uppercase() to it.residentName }
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
                            text = "Month-by-month flat maintenance breakdown",
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
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Record Collection")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Month", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                text = "Total Collections",
                                fontSize = 12.sp,
                                color = Amber100
                            )
                            Text(
                                text = "₹${totalCollected.toInt()}",
                                fontSize = 24.sp,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Individual flat amounts can vary per month (e.g. ₹1000, ₹1200 or custom charges).",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
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
                            label = { Text(month, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                            text = "No maintenance records found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate800
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Record Month' to log monthly maintenance for flats 1A to 3B.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(filteredRecords, key = { it.id }) { record ->
                        MonthCollectionCard(
                            record = record,
                            residentNames = residentNames,
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
        MonthlyCollectionDialog(
            initialRecord = recordToEdit,
            residentNames = residentNames,
            onDismiss = {
                showAddDialog = false
                recordToEdit = null
            },
            onSave = { year, month, particulars, remarks, f1a, f1b, f2a, f2b, f3a, f3b ->
                if (recordToEdit != null) {
                    val updated = recordToEdit!!.copy(
                        year = year,
                        month = month,
                        particulars = particulars,
                        remarks = remarks,
                        flat1AAmount = f1a,
                        flat1BAmount = f1b,
                        flat2AAmount = f2a,
                        flat2BAmount = f2b,
                        flat3AAmount = f3a,
                        flat3BAmount = f3b,
                        totalAmount = f1a + f1b + f2a + f2b + f3a + f3b
                    )
                    onUpdateCollection(updated)
                } else {
                    onAddCollection(year, month, particulars, remarks, f1a, f1b, f2a, f2b, f3a, f3b)
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
            title = { Text("Delete Collection Record?") },
            text = {
                Text("Are you sure you want to delete the maintenance collection record for ${recordToDelete!!.month} ${recordToDelete!!.year} (Total: ₹${recordToDelete!!.totalAmount.toInt()})?")
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
fun MonthCollectionCard(
    record: CollectionRecord,
    residentNames: Map<String, String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Amber100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Amber600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${record.month} ${record.year}".trim(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (record.particulars.isNotBlank()) {
                            Text(
                                text = record.particulars,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "₹${record.totalAmount.toInt()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald700,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // 6 Flats Grid (2 columns x 3 rows)
            val flats = listOf(
                Triple("1A", record.flat1AAmount, residentNames["1A"] ?: "M.Madhan Raj"),
                Triple("1B", record.flat1BAmount, residentNames["1B"] ?: "S.Vasikaran"),
                Triple("2A", record.flat2AAmount, residentNames["2A"] ?: "S. Hariprasad"),
                Triple("2B", record.flat2BAmount, residentNames["2B"] ?: "P.Seenivasan"),
                Triple("3A", record.flat3AAmount, residentNames["3A"] ?: "A. Venkatesh Kumar"),
                Triple("3B", record.flat3BAmount, residentNames["3B"] ?: "M.Mohan")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                flats.chunked(2).forEach { rowFlats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowFlats.forEach { (flatNo, amount, resident) ->
                            FlatMonthlyItem(
                                flatNo = flatNo,
                                amount = amount,
                                resident = resident,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (record.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Amber600,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.remarks,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun FlatMonthlyItem(
    flatNo: String,
    amount: Double,
    resident: String,
    modifier: Modifier = Modifier
) {
    val isPaid = amount > 0
    Surface(
        modifier = modifier,
        color = if (isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Rose50,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (isPaid) Slate900 else Rose600,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = flatNo,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = resident,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (isPaid) "₹${amount.toInt()}" else "Pending",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPaid) Emerald700 else Rose600
            )
        }
    }
}

@Composable
fun MonthlyCollectionDialog(
    initialRecord: CollectionRecord?,
    residentNames: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double, Double, Double, Double, Double, Double) -> Unit
) {
    var yearInput by remember { mutableStateOf(initialRecord?.year?.ifBlank { "2026" } ?: "2026") }
    var monthInput by remember { mutableStateOf(initialRecord?.month?.ifBlank { "September" } ?: "September") }
    var particularsInput by remember { mutableStateOf(initialRecord?.particulars?.ifBlank { "Monthly Maintenance" } ?: "Monthly Maintenance") }
    var remarksInput by remember { mutableStateOf(initialRecord?.remarks ?: "") }

    var f1aInput by remember { mutableStateOf(initialRecord?.flat1AAmount?.toInt()?.toString() ?: "1000") }
    var f1bInput by remember { mutableStateOf(initialRecord?.flat1BAmount?.toInt()?.toString() ?: "1000") }
    var f2aInput by remember { mutableStateOf(initialRecord?.flat2AAmount?.toInt()?.toString() ?: "1000") }
    var f2bInput by remember { mutableStateOf(initialRecord?.flat2BAmount?.toInt()?.toString() ?: "1000") }
    var f3aInput by remember { mutableStateOf(initialRecord?.flat3AAmount?.toInt()?.toString() ?: "1000") }
    var f3bInput by remember { mutableStateOf(initialRecord?.flat3BAmount?.toInt()?.toString() ?: "1000") }

    var presetAmountInput by remember { mutableStateOf("1000") }

    val liveTotal = remember(f1aInput, f1bInput, f2aInput, f2bInput, f3aInput, f3bInput) {
        (f1aInput.toDoubleOrNull() ?: 0.0) +
                (f1bInput.toDoubleOrNull() ?: 0.0) +
                (f2aInput.toDoubleOrNull() ?: 0.0) +
                (f2bInput.toDoubleOrNull() ?: 0.0) +
                (f3aInput.toDoubleOrNull() ?: 0.0) +
                (f3bInput.toDoubleOrNull() ?: 0.0)
    }

    val monthsList = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = Amber600)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialRecord != null) "Edit Monthly Collection" else "Record Month Maintenance",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Specify maintenance amounts for each flat. Amounts can differ per flat (e.g. ₹1000, ₹1200 or extra arrears):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                item {
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
                }

                // Preset button to set all flats at once
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = presetAmountInput,
                                onValueChange = { presetAmountInput = it },
                                label = { Text("Base Amount", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(110.dp)
                            )
                            Button(
                                onClick = {
                                    f1aInput = presetAmountInput
                                    f1bInput = presetAmountInput
                                    f2aInput = presetAmountInput
                                    f2bInput = presetAmountInput
                                    f3aInput = presetAmountInput
                                    f3bInput = presetAmountInput
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate900)
                            ) {
                                Text("Apply All", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Individual Flat Amount inputs
                item {
                    Text("Flat Maintenance Amounts (₹):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatAmountField(flatNo = "1A", resident = residentNames["1A"] ?: "M.Madhan Raj", value = f1aInput, onValueChange = { f1aInput = it }, modifier = Modifier.weight(1f))
                        FlatAmountField(flatNo = "1B", resident = residentNames["1B"] ?: "S.Vasikaran", value = f1bInput, onValueChange = { f1bInput = it }, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatAmountField(flatNo = "2A", resident = residentNames["2A"] ?: "S. Hariprasad", value = f2aInput, onValueChange = { f2aInput = it }, modifier = Modifier.weight(1f))
                        FlatAmountField(flatNo = "2B", resident = residentNames["2B"] ?: "P.Seenivasan", value = f2bInput, onValueChange = { f2bInput = it }, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlatAmountField(flatNo = "3A", resident = residentNames["3A"] ?: "A. Venkatesh Kumar", value = f3aInput, onValueChange = { f3aInput = it }, modifier = Modifier.weight(1f))
                        FlatAmountField(flatNo = "3B", resident = residentNames["3B"] ?: "M.Mohan", value = f3bInput, onValueChange = { f3bInput = it }, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    // Total Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Emerald50),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Collection:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Emerald900)
                            Text("₹${liveTotal.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = particularsInput,
                        onValueChange = { particularsInput = it },
                        label = { Text("Particulars / Purpose") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = remarksInput,
                        onValueChange = { remarksInput = it },
                        label = { Text("Remarks / Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val f1a = f1aInput.toDoubleOrNull() ?: 0.0
                    val f1b = f1bInput.toDoubleOrNull() ?: 0.0
                    val f2a = f2aInput.toDoubleOrNull() ?: 0.0
                    val f2b = f2bInput.toDoubleOrNull() ?: 0.0
                    val f3a = f3aInput.toDoubleOrNull() ?: 0.0
                    val f3b = f3bInput.toDoubleOrNull() ?: 0.0
                    onSave(
                        yearInput.trim(),
                        monthInput.trim(),
                        particularsInput.trim().ifBlank { "Monthly Maintenance" },
                        remarksInput.trim(),
                        f1a, f1b, f2a, f2b, f3a, f3b
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate900)
            ) {
                Text(if (initialRecord != null) "Update" else "Save Collection", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FlatAmountField(
    flatNo: String,
    resident: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$flatNo (₹)", fontSize = 11.sp) },
        placeholder = { Text("0") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
