package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
                    BottomNavItem("dashboard", "Overview", Icons.Default.Dashboard, "nav_dashboard"),
                    BottomNavItem("expenses", "Expenses", Icons.Default.Receipt, "nav_expenses"),
                    BottomNavItem("yearly_report", "Yearly", Icons.Default.BarChart, "nav_yearly"),
                    BottomNavItem("contacts", "Directory", Icons.Default.People, "nav_contacts"),
                    BottomNavItem("login_config", "Sync Config", Icons.Default.Settings, "nav_config")
                )

                LaunchedEffect(uiState.syncMessage) {
                    uiState.syncMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSyncMessage()
                    }
                }

                if (!uiState.config.isLoggedIn) {
                    GoogleLoginScreen(
                        userEmail = uiState.config.userEmail,
                        webClientId = uiState.config.webClientId,
                        onLogin = { email, clientVal -> viewModel.loginWithGoogle(email, clientVal) }
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
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
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
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
                                    expenses = uiState.expenses,
                                    config = uiState.config,
                                    isLoading = uiState.isLoading,
                                    syncMessage = uiState.syncMessage,
                                    onTriggerSync = { viewModel.triggerSync() },
                                    onLogout = {
                                        viewModel.logout(context)
                                        navController.navigate("login_config") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    },
                                    onNavigateToExpenses = { navController.navigate("expenses") },
                                    onNavigateToYearlyReport = { navController.navigate("yearly_report") },
                                    onNavigateToContacts = { navController.navigate("contacts") },
                                    onNavigateToConfig = { navController.navigate("login_config") }
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
                                    onAddExpense = { particulars, amount, category, vendor, day, remarks ->
                                        viewModel.addExpense(particulars, amount, category, vendor, day, remarks = remarks)
                                    },
                                    onDeleteExpense = { id -> viewModel.deleteExpense(id) },
                                    onNavigateBack = null
                                )
                            }

                            composable("yearly_report") {
                                val col = uiState.collectionRecord
                                val contributions = if (uiState.yearlyContributions.isNotEmpty()) {
                                    uiState.yearlyContributions
                                } else if (col != null) {
                                    listOf(
                                        YearlyContribution("1A", "Flat 1A", col.flat1AAmount),
                                        YearlyContribution("1B", "Flat 1B", col.flat1BAmount),
                                        YearlyContribution("2A", "Flat 2A", col.flat2AAmount),
                                        YearlyContribution("2B", "Flat 2B", col.flat2BAmount),
                                        YearlyContribution("3A", "Flat 3A", col.flat3AAmount),
                                        YearlyContribution("3B", "Flat 3B", col.flat3BAmount)
                                    )
                                } else emptyList()

                                val categories = if (uiState.yearlyCategories.isNotEmpty()) {
                                    uiState.yearlyCategories
                                } else {
                                    uiState.expenses.groupBy { it.category }
                                        .map { (cat, list) -> YearlyExpenseCategory(category = cat, amount2026 = list.sumOf { it.amount }) }
                                }

                                val majorWorks = if (uiState.majorWorks.isNotEmpty()) {
                                    uiState.majorWorks
                                } else {
                                    uiState.expenses.filter {
                                        it.amount >= 2000 ||
                                                it.particulars.contains("sensor", ignoreCase = true) ||
                                                it.particulars.contains("motor", ignoreCase = true) ||
                                                it.category.contains("Alteration", ignoreCase = true)
                                    }.map { MajorWork(description = it.particulars, amount2026 = it.amount) }
                                }

                                YearlyReportScreen(
                                    contributions = contributions,
                                    categories = categories,
                                    majorWorks = majorWorks,
                                    onNavigateBack = null
                                )
                            }

                            composable("contacts") {
                                val defaultOwners = listOf(
                                    OwnerContact("1A", "Flat 1A Resident", "", "Resident"),
                                    OwnerContact("1B", "Flat 1B Resident", "", "Resident"),
                                    OwnerContact("2A", "Flat 2A Resident", "", "Resident"),
                                    OwnerContact("2B", "Flat 2B Resident", "", "Resident"),
                                    OwnerContact("3A", "Flat 3A Resident", "", "Resident"),
                                    OwnerContact("3B", "Flat 3B Resident", "", "Resident")
                                )
                                val defaultServices = listOf(
                                    ServiceContact("Cleaning & Sanitation", "Housekeeping Maid", "", "Common Area Sanitation"),
                                    ServiceContact("Motor & Electrical", "Technician / Electrician", "", "Water Pump Sensor & Wiring"),
                                    ServiceContact("Plumbing", "Plumber", "", "Common Water Line Maintenance")
                                )
                                ContactsScreen(
                                    ownerContacts = if (uiState.ownerContacts.isNotEmpty()) uiState.ownerContacts else defaultOwners,
                                    serviceContacts = if (uiState.serviceContacts.isNotEmpty()) uiState.serviceContacts else defaultServices,
                                    onNavigateBack = null
                                )
                            }

                            composable("login_config") {
                                LoginConfigScreen(
                                    config = uiState.config,
                                    isLoading = uiState.isLoading,
                                    syncMessage = uiState.syncMessage,
                                    onLogin = { email, clientVal -> viewModel.loginWithGoogle(email, clientVal) },
                                    onSaveGcpConfig = { spreadsheetId, apiKey, gcpProject, serviceAccount, userEmail, webClientId ->
                                        viewModel.updateGcpConfig(spreadsheetId, apiKey, gcpProject, serviceAccount, userEmail, webClientId)
                                    },
                                    onTriggerSync = { viewModel.triggerSync() },
                                    onContinueToDashboard = {
                                        navController.navigate("dashboard") {
                                            popUpTo("login_config") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
