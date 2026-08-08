package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OwnerContact
import com.example.data.model.ServiceContact
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    ownerContacts: List<OwnerContact>,
    serviceContacts: List<ServiceContact>,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gomathi Ilam Thendral", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Apartment & Service Directory", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Flat Owners (${ownerContacts.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Service Contacts", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ownerContacts) { owner ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_contact_${owner.flatNo}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Slate900,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = owner.flatNo,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(owner.residentName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text(owner.primaryContactNo, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${owner.primaryContactNo}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.testTag("dial_button_${owner.flatNo}")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Emerald600)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(serviceContacts) { service ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(service.serviceType, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Amber600)
                                    if (service.contactPerson != "--") {
                                        Text("Contact: ${service.contactPerson}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("Phone: ${service.phoneNo}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        if (service.remarks != "--") {
                                            Text("Note: ${service.remarks}", fontSize = 11.sp, color = Teal600)
                                        }
                                    } else {
                                        Text("Contact info not provided yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    }
                                }

                                if (service.phoneNo != "--" && service.phoneNo.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phoneNo}"))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Emerald600)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
