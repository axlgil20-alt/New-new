package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.FinancialAdvisorScreen
import com.example.ui.theme.EmeraldGreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.TransactionEntity
import com.example.ui.components.AddAntExpenseDialog
import com.example.ui.components.AddEditRecurringExpenseDialog
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.BudgetLimitDialog
import com.example.ui.components.ChapinQuickFab
import com.example.ui.components.FinancialAdvisorDialog
import com.example.ui.components.HouseholdFinancialAdvisorDialog
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.components.SmartSupermarketModule
import com.example.ui.components.TermsAndPrivacyDialog
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GoalsAndToolsScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.PlannedVsRealScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PresupuestoTheme
import com.example.ui.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            var selectedTab by remember { mutableIntStateOf(0) }
            var showAddTransactionDialog by remember { mutableStateOf(false) }
            var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
            var showAddAntExpenseDialog by remember { mutableStateOf(false) }
            var showBudgetLimitDialog by remember { mutableStateOf(false) }
            var showRecurringDialog by remember { mutableStateOf(false) }
            var showSavingsGoalDialog by remember { mutableStateOf(false) }
            var showAdvisorDialog by remember { mutableStateOf(false) }
            var showSmartCartDialog by remember { mutableStateOf(false) }
            var showSplashScreen by remember { mutableStateOf(true) }

            val prefs = remember { getSharedPreferences("quetzi_prefs", MODE_PRIVATE) }
            var showFirstLaunchTerms by remember {
                mutableStateOf(!prefs.getBoolean("has_accepted_terms_v1", false))
            }

            // Observar mensajes para mostrar Feedback Toast
            LaunchedEffect(uiState.userMessage) {
                uiState.userMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }

            PresupuestoTheme(
                themeIndex = uiState.selectedThemeIndex,
                isDarkTheme = uiState.isDarkTheme
            ) {
                if (showSplashScreen) {
                    SplashScreen(
                        onContinue = { showSplashScreen = false }
                    )
                } else {
                    Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    // 6. FAB Chapín: Conexión real con guardado directo en Room Database y registro completo
                    floatingActionButton = {
                        ChapinQuickFab(
                            onSaveExpense = { title, amt, cat, notes ->
                                viewModel.addChapinQuickExpense(title, amt, cat, notes)
                            },
                            onAddFullTransaction = {
                                editingTransaction = null
                                showAddTransactionDialog = true
                            }
                        )
                    },
                    // 4. Barra de Navegación Inferior Simétrica: Inicio centrado y destacado, y Asesor IA dedicado
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            // 1. Registros
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Registros") },
                                label = { Text("Registros") }
                            )

                            // 2. Límites
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.PieChart, contentDescription = "Límites") },
                                label = { Text("Límites") }
                            )

                            // 3. Inicio (Centrado y Destacado con relieve visual)
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selectedTab == 0) EmeraldGreen else EmeraldGreen.copy(alpha = 0.15f),
                                        shadowElevation = if (selectedTab == 0) 6.dp else 0.dp,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Inicio",
                                                tint = if (selectedTab == 0) Color.White else EmeraldGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Inicio",
                                        fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (selectedTab == 0) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            // 4. Asesor (Dedicado con ícono Robot/AI)
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.SmartToy, contentDescription = "Asesor IA") },
                                label = { Text("Asesor") }
                            )

                            // 5. Ajustes
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = { Icon(Icons.Default.Build, contentDescription = "Ajustes") },
                                label = { Text("Ajustes") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTab) {
                            0 -> InicioScreen(
                                uiState = uiState,
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onToggleAllTime = { viewModel.toggleAllTime() },
                                onToggleIncludeTentative = { viewModel.toggleIncludeTentative() },
                                onToggleDiscreetMode = { viewModel.toggleDiscreetMode() },
                                onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                                onAddTransactionClick = {
                                    editingTransaction = null
                                    showAddTransactionDialog = true
                                },
                                onAddAntExpenseClick = { showAddAntExpenseDialog = true },
                                onOpenAdvisorClick = { selectedTab = 3 },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    showAddTransactionDialog = true
                                },
                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) },
                                onOpenSmartCartClick = { showSmartCartDialog = true }
                            )

                            1 -> TransactionsScreen(
                                uiState = uiState,
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onToggleAllTime = { viewModel.toggleAllTime() },
                                onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                                onFilterTypeChange = { t -> viewModel.setFilterType(t) },
                                onFilterCategoryChange = { c -> viewModel.setCategoryFilter(c) },
                                onFilterMemberChange = { m -> viewModel.setMemberFilter(m) },
                                onToggleOnlyHighlighted = { h -> viewModel.setOnlyHighlighted(h) },
                                onAddTransactionClick = {
                                    editingTransaction = null
                                    showAddTransactionDialog = true
                                },
                                onEditTransaction = { tx ->
                                    editingTransaction = tx
                                    showAddTransactionDialog = true
                                },
                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) },
                                onExportCsv = { viewModel.getCsvExport() },
                                onImportCsv = { text -> viewModel.importCsv(text) }
                            )

                            2 -> BudgetsScreen(
                                uiState = uiState,
                                onPreviousMonth = { viewModel.previousMonth() },
                                onNextMonth = { viewModel.nextMonth() },
                                onToggleAllTime = { viewModel.toggleAllTime() },
                                onAddBudgetLimitClick = { showBudgetLimitDialog = true },
                                onDeleteBudgetLimit = { cat -> viewModel.deleteBudgetLimit(cat) },
                                onAddRecurringExpenseClick = { showRecurringDialog = true },
                                onDeleteRecurringExpense = { rec -> viewModel.deleteRecurringExpense(rec) },
                                onToggleRecurringExpense = { rec -> viewModel.toggleRecurringExpenseActive(rec) },
                                onAddSavingsGoalClick = { showSavingsGoalDialog = true },
                                onDeleteSavingsGoal = { goal -> viewModel.deleteSavingsGoal(goal) },
                                onContributeSavingsGoal = { goal, amt -> viewModel.contributeToGoal(goal, amt) }
                            )

                            3 -> FinancialAdvisorScreen(
                                uiState = uiState
                            )

                            4 -> GoalsAndToolsScreen(
                                uiState = uiState,
                                onOpenAdvisor = { selectedTab = 3 },
                                onSetTheme = { idx -> viewModel.setAppTheme(idx) },
                                onSetCurrency = { sym -> viewModel.setCurrency(sym) },
                                onImportCsv = { text -> viewModel.importCsv(text) },
                                onExportCsv = { viewModel.getCsvExport() },
                                onClearAllData = { viewModel.clearAllData() },
                                onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                                onOpenQuincenas = { selectedTab = 5 },
                                onOpenSmartCart = { showSmartCartDialog = true },
                                onSaveIncomeToBudget = { title, amountGtq, note ->
                                    viewModel.addTransaction(
                                        com.example.data.model.TransactionEntity(
                                            title = title,
                                            amount = amountGtq,
                                            type = com.example.data.model.TransactionType.INCOME,
                                            category = "Ingreso Extra / Remesa",
                                            dateMillis = System.currentTimeMillis(),
                                            notes = note
                                        )
                                    )
                                },
                                onSaveExpenseToBudget = { title, amountGtq, category, note ->
                                    viewModel.addTransaction(
                                        com.example.data.model.TransactionEntity(
                                            title = title,
                                            amount = amountGtq,
                                            type = com.example.data.model.TransactionType.EXPENSE,
                                            category = category,
                                            dateMillis = System.currentTimeMillis(),
                                            notes = note
                                        )
                                    )
                                }
                            )

                            5 -> {
                                BackHandler {
                                    selectedTab = 4
                                }
                                PlannedVsRealScreen(
                                    uiState = uiState,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onToggleAllTime = { viewModel.toggleAllTime() },
                                    onEditTransaction = { tx ->
                                        editingTransaction = tx
                                        showAddTransactionDialog = true
                                    },
                                    onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                    onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                    onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                    onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) }
                                )
                            }
                        }
                    }

                    // Diálogos modales
                    if (showAddTransactionDialog) {
                        AddEditTransactionDialog(
                            transaction = editingTransaction,
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = {
                                showAddTransactionDialog = false
                                editingTransaction = null
                            },
                            onSave = { tx ->
                                if (editingTransaction == null) {
                                    viewModel.addTransaction(tx)
                                } else {
                                    viewModel.updateTransaction(tx)
                                }
                            }
                        )
                    }

                    if (showAddAntExpenseDialog) {
                        AddAntExpenseDialog(
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = { showAddAntExpenseDialog = false },
                            onSave = { tx -> viewModel.addAntExpense(tx) }
                        )
                    }

                    if (showBudgetLimitDialog) {
                        BudgetLimitDialog(
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = { showBudgetLimitDialog = false },
                            onSave = { cat, limit, reason, tip ->
                                viewModel.setBudgetLimit(cat, limit, reason, tip)
                            }
                        )
                    }

                    if (showRecurringDialog) {
                        AddEditRecurringExpenseDialog(
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = { showRecurringDialog = false },
                            onSave = { title, amt, cat, freq, member, pay, notes ->
                                viewModel.addRecurringExpense(title, amt, cat, freq, member, pay, notes)
                            }
                        )
                    }

                    if (showSavingsGoalDialog) {
                        SavingsGoalDialog(
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = { showSavingsGoalDialog = false },
                            onSave = { name, target, initAmt, notes ->
                                viewModel.addSavingsGoal(name, target, initAmt, notes)
                            }
                        )
                    }

                    if (showAdvisorDialog) {
                        FinancialAdvisorDialog(
                            uiState = uiState,
                            onDismiss = { showAdvisorDialog = false }
                        )
                    }

                    if (showSmartCartDialog) {
                        SmartSupermarketModule(
                            cartItems = uiState.smartCartItems,
                            pantryItems = uiState.smartPantryItems,
                            budgetAmount = uiState.smartCartBudget,
                            selectedStoreName = uiState.selectedCartStore,
                            historicalTransactions = uiState.filteredTransactions,
                            currencySymbol = uiState.currencySymbol,
                            isDiscreetMode = uiState.isDiscreetMode,
                            onDismiss = { showSmartCartDialog = false },
                            onAddItem = { name, qty, unit, brand, price, store, notes, url, source ->
                                viewModel.addSmartCartItem(
                                    name = name,
                                    quantity = qty,
                                    unit = unit,
                                    brand = brand,
                                    price = price,
                                    store = store,
                                    notes = notes,
                                    url = url,
                                    priceSource = source
                                )
                            },
                            onUpdateItem = { item -> viewModel.updateSmartCartItem(item) },
                            onDeleteItem = { item -> viewModel.deleteSmartCartItem(item) },
                            onToggleChecked = { item -> viewModel.toggleSmartCartItemChecked(item) },
                            onSetBudget = { budget -> viewModel.setSmartCartBudget(budget) },
                            onSelectStore = { store -> viewModel.setSelectedCartStore(store) },
                            onClearChecked = { viewModel.clearCheckedCartItems() },
                            onCheckoutExpense = { title, amount, store, purchasedNames ->
                                viewModel.checkoutCartAsExpense(title, amount, store, purchasedNames)
                            },
                            onAddPantryItem = { name, qty, unit, status, price, store, notes ->
                                viewModel.addPantryItem(name, qty, unit, status, price, store, notes)
                            },
                            onUpdatePantryItem = { item -> viewModel.updatePantryItem(item) },
                            onTogglePantryStatus = { item -> viewModel.togglePantryStatus(item) },
                            onDeletePantryItem = { item -> viewModel.deletePantryItem(item) },
                            onGenerateReplenishment = { viewModel.generateReplenishmentList() },
                            onAddScannedItems = { items, destination, avoidDuplicates ->
                                viewModel.addScannedItems(items, destination, avoidDuplicates)
                            }
                        )
                    }

                    // Modal de Términos y Privacidad (Aparición Única en Primer Inicio)
                    if (showFirstLaunchTerms) {
                        TermsAndPrivacyDialog(
                            onAccept = {
                                prefs.edit().putBoolean("has_accepted_terms_v1", true).apply()
                                showFirstLaunchTerms = false
                            }
                        )
                    }
                }
                }
            }
        }
    }
}
