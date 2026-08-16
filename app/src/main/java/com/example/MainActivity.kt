package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsState()
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val bottomNavItems = listOf(
                    BottomNavItem("dashboard", "Overview", Icons.Default.Home, "nav_dashboard"),
                    BottomNavItem("collections", "Collection", Icons.Default.AccountBalanceWallet, "nav_collections"),
                    BottomNavItem("expenses", "Expenses", Icons.Default.ReceiptLong, "nav_expenses"),
                    BottomNavItem("contacts", "Directory", Icons.Default.PeopleAlt, "nav_contacts"),
                    BottomNavItem("settings", "Settings", Icons.Default.Settings, "nav_settings")
                )

                LaunchedEffect(uiState.syncMessage) {
                    uiState.syncMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSyncMessage()
                    }
                }

                if (!uiState.config.isLoggedIn || uiState.config.userEmail.isBlank()) {
                    GoogleLoginScreen(
                        config = uiState.config,
                        isLoading = uiState.isLoading,
                        onLoginSuccess = { email ->
                            viewModel.loginWithGoogle(email)
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentRoute == item.route
                                    NavigationBarItem(
                                        modifier = Modifier.testTag(item.testTag),
                                        selected = selected,
                                        onClick = {
                                            if (currentRoute != item.route) {
                                                navController.navigate(item.route) {
                                                    popUpTo("dashboard") {
                                                        saveState = false
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = {
                                            Text(
                                                text = item.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Amber600,
                                            selectedTextColor = Amber600,
                                            indicatorColor = Amber100
                                        )
                                    )
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    collectionRecord = uiState.collectionRecord,
                                    collectionRecords = uiState.collectionRecords,
                                    expenses = uiState.expenses,
                                    contributions = uiState.yearlyContributions,
                                    config = uiState.config,
                                    isLoading = uiState.isLoading,
                                    syncMessage = uiState.syncMessage,
                                    onTriggerSync = { viewModel.triggerSync() },
                                    onNavigateToCollections = { navController.navigate("collections") },
                                    onNavigateToExpenses = { navController.navigate("expenses") },
                                    onNavigateToYearlyReport = { navController.navigate("yearly_report") },
                                    onNavigateToContacts = { navController.navigate("contacts") },
                                    onNavigateToConfig = { navController.navigate("settings") }
                                )
                            }

                            composable("collections") {
                                CollectionsScreen(
                                    collectionRecords = uiState.collectionRecords,
                                    ownerContacts = uiState.ownerContacts,
                                    onNavigateBack = null
                                )
                            }

                            composable("expenses") {
                                ExpenseScreen(
                                    expenses = uiState.filteredExpenses,
                                    searchQuery = uiState.searchQuery,
                                    selectedCategory = uiState.selectedCategoryFilter,
                                    selectedMonth = uiState.selectedMonthFilter,
                                    allExpensesForMonths = uiState.expenses,
                                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                    onCategorySelected = { viewModel.onCategoryFilterSelected(it) },
                                    onMonthSelected = { viewModel.onMonthFilterSelected(it) },
                                    onNavigateBack = null
                                )
                            }

                            composable("yearly_report") {
                                val contributions = uiState.yearlyContributions
                                val categories = if (uiState.yearlyCategories.isNotEmpty()) {
                                    uiState.yearlyCategories
                                } else {
                                    uiState.expenses.groupBy { it.category.ifBlank { "General" } }
                                        .map { (cat, list) -> YearlyExpenseCategory(category = cat, amount2026 = list.sumOf { it.amount }) }
                                }
                                val majorWorks = if (uiState.majorWorks.isNotEmpty()) {
                                    uiState.majorWorks
                                } else {
                                    uiState.expenses.filter {
                                        it.amount >= 1000 ||
                                                it.particulars.contains("sensor", ignoreCase = true) ||
                                                it.particulars.contains("motor", ignoreCase = true)
                                    }.map { MajorWork(description = it.particulars, amount2026 = it.amount) }
                                }

                                YearlyReportScreen(
                                    contributions = contributions,
                                    collectionRecords = uiState.collectionRecords,
                                    categories = categories,
                                    majorWorks = majorWorks,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable("contacts") {
                                ContactsScreen(
                                    ownerContacts = uiState.ownerContacts,
                                    serviceContacts = uiState.serviceContacts,
                                    onNavigateBack = null
                                )
                            }

                            composable("settings") {
                                SettingsScreen(
                                    config = uiState.config,
                                    isLoading = uiState.isLoading,
                                    syncMessage = uiState.syncMessage,
                                    onLogout = {
                                        viewModel.logout(context)
                                    },
                                    onUpdateSheetUrl = { url ->
                                        viewModel.updateSheetUrl(url)
                                    },
                                    onSaveGcpConfig = { spreadsheetId, apiKey, gcpProject, serviceAccount, userEmail, webClientId ->
                                        viewModel.updateGcpConfig(spreadsheetId, apiKey, gcpProject, serviceAccount, userEmail, webClientId)
                                    },
                                    onTriggerSync = { viewModel.triggerSync() },
                                    onNavigateToYearlyReport = { navController.navigate("yearly_report") },
                                    onNavigateBack = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
