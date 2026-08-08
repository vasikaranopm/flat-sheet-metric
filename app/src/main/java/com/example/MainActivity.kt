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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                                    config = uiState.config,
                                    onTriggerSync = { viewModel.triggerSync() },
                                    onLogout = { viewModel.logout() },
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
                                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                    onCategorySelected = { viewModel.onCategoryFilterSelected(it) },
                                    onAddExpense = { particulars, amount, category, vendor, day, remarks ->
                                        viewModel.addExpense(particulars, amount, category, vendor, day, remarks = remarks)
                                    },
                                    onDeleteExpense = { id -> viewModel.deleteExpense(id) },
                                    onNavigateBack = null
                                )
                            }

                            composable("yearly_report") {
                                YearlyReportScreen(
                                    contributions = uiState.yearlyContributions,
                                    categories = uiState.yearlyCategories,
                                    majorWorks = uiState.majorWorks,
                                    onNavigateBack = null
                                )
                            }

                            composable("contacts") {
                                ContactsScreen(
                                    ownerContacts = uiState.ownerContacts,
                                    serviceContacts = uiState.serviceContacts,
                                    onNavigateBack = null
                                )
                            }

                            composable("login_config") {
                                LoginConfigScreen(
                                    config = uiState.config,
                                    onLogin = { email, clientVal -> viewModel.loginWithGoogle(email, clientVal) },
                                    onSaveGcpConfig = { spreadsheetId, apiKey, gcpProject, serviceAccount, userEmail, webClientId ->
                                        viewModel.updateGcpConfig(spreadsheetId, apiKey, gcpProject, serviceAccount, userEmail, webClientId)
                                    },
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
