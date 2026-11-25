package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AboutQuetziDialog
import com.example.ui.components.NotificationsSettingsDialog
import com.example.ui.components.PrivacySecurityDialog
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.FinancialAdvisorScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.SmartCartScreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
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
import com.example.ui.util.TermsAcceptanceManager
import com.example.ui.viewmodel.AccountsViewModel
import com.example.ui.viewmodel.BudgetViewModel

internal object PrimaryTabPagerLogic {
    // Orden horizontal requerido:
    // HOY (0) <-> QUINCENAS (2) | PAGOS (5) <-> SMART CART (7) | REGISTROS (1)
    val primaryTabsOrder: List<Int> = listOf(0, 2, 5, 7, 1)

    fun isPrimaryTab(tabId: Int): Boolean = tabId in primaryTabsOrder

    fun pageIndexForTab(tabId: Int): Int? {
        val idx = primaryTabsOrder.indexOf(tabId)
        return if (idx >= 0) idx else null
    }

    fun tabForPageIndex(pageIndex: Int): Int {
        return primaryTabsOrder.getOrElse(pageIndex) { 0 }
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: BudgetViewModel by viewModels()
    private val accountsViewModel: AccountsViewModel by viewModels()

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val accountsState by accountsViewModel.uiState.collectAsState()
            val accountNameById = remember(accountsState.allAccounts) {
                accountsState.allAccounts.associate { it.id to it.name }
            }
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()

            val initialTab = remember {
                val requested = intent?.getIntExtra("open_tab", 0) ?: 0
                if (requested in listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)) requested else 0
            }
            var selectedTab by remember { mutableIntStateOf(initialTab) }
            val pagerState = rememberPagerState(
                initialPage = PrimaryTabPagerLogic.pageIndexForTab(initialTab) ?: 0
            ) { PrimaryTabPagerLogic.primaryTabsOrder.size }
            var showAddTransactionDialog by remember { mutableStateOf(false) }
            var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
            var preselectedAccountIdForNewTx by remember { mutableStateOf<Long?>(null) }
            var showAddAntExpenseDialog by remember { mutableStateOf(false) }
            var showBudgetLimitDialog by remember { mutableStateOf(false) }
            var budgetLimitBeingEdited by remember { mutableStateOf<com.example.data.model.BudgetLimitEntity?>(null) }
            var showRecurringDialog by remember { mutableStateOf(false) }
            var showSavingsGoalDialog by remember { mutableStateOf(false) }
            var showAdvisorDialog by remember { mutableStateOf(false) }
            var showSmartCartDialog by remember { mutableStateOf(false) }
            var showSplashScreen by remember { mutableStateOf(true) }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            var showNotificationsDialog by remember { mutableStateOf(false) }
            var showPrivacySecurityDialog by remember { mutableStateOf(false) }
            var showTermsDialog by remember { mutableStateOf(false) }
            var showAboutDialog by remember { mutableStateOf(false) }

            val deleteTransactionWithUndo: (TransactionEntity) -> Unit = { tx ->
                viewModel.deleteTransactionForUndo(tx) { batchToken, deletedCount ->
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val message = if (deletedCount > 1 && tx.transferGroupId != null) {
                            "Transferencia eliminada ($deletedCount partes): ${tx.title}"
                        } else {
                            "Eliminado: ${tx.title}"
                        }
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = "Deshacer",
                            withDismissAction = true,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoBulkDelete(batchToken)
                        } else {
                            viewModel.clearPendingBulkDeleteIfMatches(batchToken)
                        }
                    }
                }
            }

            val deleteTransactionsWithUndo: (List<Long>) -> Unit = { ids ->
                viewModel.deleteTransactionsForUndo(ids) { batchToken, deletedCount ->
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val message = if (deletedCount == 1) {
                            "1 registro eliminado"
                        } else {
                            "$deletedCount registros eliminados"
                        }
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = "Deshacer",
                            withDismissAction = true,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoBulkDelete(batchToken)
                        } else {
                            viewModel.clearPendingBulkDeleteIfMatches(batchToken)
                        }
                    }
                }
            }

            var pendingTargetPage by remember { mutableStateOf<Int?>(null) }

            LaunchedEffect(selectedTab) {
                val targetPage = PrimaryTabPagerLogic.pageIndexForTab(selectedTab)
                if (targetPage != null) {
                    if (pagerState.currentPage != targetPage) {
                        pendingTargetPage = targetPage
                        try {
                            // La navegación principal no debe arrastrar toda la interfaz
                            // lateralmente. Los desplazamientos horizontales quedan
                            // reservados para controles internos que los necesitan.
                            pagerState.scrollToPage(targetPage)
                        } finally {
                            pendingTargetPage = null
                        }
                    }
                }
            }

            LaunchedEffect(pagerState.settledPage) {
                if (PrimaryTabPagerLogic.isPrimaryTab(selectedTab) && pendingTargetPage == null) {
                    val settledTab = PrimaryTabPagerLogic.tabForPageIndex(pagerState.settledPage)
                    if (selectedTab != settledTab) {
                        selectedTab = settledTab
                    }
                }
            }

            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            BackHandler(enabled = !drawerState.isOpen && selectedTab != 0) {
                selectedTab = if (selectedTab == 3 || selectedTab == 6) 4 else 0
            }

            val prefs = remember { getSharedPreferences(TermsAcceptanceManager.PREFS_NAME, MODE_PRIVATE) }
            var requiresTermsAcceptance by remember {
                mutableStateOf(TermsAcceptanceManager.requiresAcceptance(prefs))
            }

            // Observar mensajes para mostrar Feedback Toast
            LaunchedEffect(uiState.userMessage) {
                uiState.userMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
            LaunchedEffect(accountsState.successMessage, accountsState.errorMessage) {
                val msg = accountsState.errorMessage ?: accountsState.successMessage
                if (!msg.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    accountsViewModel.clearMessages()
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
                } else if (requiresTermsAcceptance) {
                    BackHandler(enabled = true) {
                        // Impide omitir los términos mediante el botón Atrás del sistema
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        TermsAndPrivacyDialog(
                            isMandatory = true,
                            termsVersion = TermsAcceptanceManager.CURRENT_TERMS_VERSION,
                            onAccept = {
                                TermsAcceptanceManager.recordAcceptance(prefs)
                                requiresTermsAcceptance = false
                            },
                            onDeclineAndExit = {
                                finishAffinity()
                            }
                        )
                    }
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = drawerState.isOpen || !PrimaryTabPagerLogic.isPrimaryTab(selectedTab),
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = if (uiState.isDarkTheme) Color(0xFF131D1B) else Color(0xFFF7FAF9),
                                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(0.84f)
                            ) {
                                // Header del Menú Hamburguesa con Identidad Ketzi
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFF022C22),
                                                    Color(0xFF064E3B)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 20.dp, vertical = 24.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldGreen,
                                            border = BorderStroke(1.5.dp, Color(0xFF34D399)),
                                            modifier = Modifier.size(46.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("🇬🇹", fontSize = 24.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "KETZI",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "Tus finanzas sin clavos",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFA7F3D0),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // GRUPO 1: HERRAMIENTAS
                                Text(
                                    text = "HERRAMIENTAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Mis Cuentas", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldGreen) },
                                    selected = selectedTab == 8,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        selectedTab = 8
                                    },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag("drawer_mis_cuentas")
                                )

                                NavigationDrawerItem(
                                    label = { Text("Límites y Metas", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.PieChart, contentDescription = null, tint = EmeraldGreen) },
                                    selected = selectedTab == 6,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        selectedTab = 6
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Asesor Financiero IA", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                    selected = selectedTab == 3,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        selectedTab = 3
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // GRUPO 2: CONFIGURACIÓN
                                Text(
                                    text = "CONFIGURACIÓN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Ajustes", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                    selected = selectedTab == 4,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        selectedTab = 4
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Notificaciones", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showNotificationsDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // GRUPO 3: INFORMACIÓN
                                Text(
                                    text = "INFORMACIÓN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Privacidad y seguridad", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981)) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showPrivacySecurityDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Términos y condiciones", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF64748B)) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showTermsDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Acerca de KETZI", fontWeight = FontWeight.SemiBold) },
                                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldGreen) },
                                    selected = false,
                                    onClick = {
                                        coroutineScope.launch { drawerState.close() }
                                        showAboutDialog = true
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Footer
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = "KETZI v1.0",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "MarvGr · Offline-First",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                    Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    // Barra de Navegación Inferior Simétrica: EXACTAMENTE 5 OPCIONES
                    // REGISTROS | QUINCENAS | HOY (CENTRO DESTACADO) | PAGOS | SMART CART
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            // 1. REGISTROS
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Registros") },
                                label = { Text("Registros", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                            )

                            // 2. QUINCENAS
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Quincenas") },
                                label = { Text("Quincenas", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                            )

                            // 3. HOY (Centrado y Destacado con relieve visual)
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
                                                contentDescription = "HOY",
                                                tint = if (selectedTab == 0) Color.White else EmeraldGreen,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = "HOY",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (selectedTab == 0) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            // 4. PAGOS (Pestaña principal independiente)
                            NavigationBarItem(
                                selected = selectedTab == 5,
                                onClick = { selectedTab = 5 },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.overduePaymentsCount > 0) {
                                                Badge(containerColor = ExpenseRed) {
                                                    Text("${uiState.overduePaymentsCount}", fontSize = 9.sp)
                                                }
                                            } else if (uiState.upcomingPaymentsCount > 0) {
                                                Badge(containerColor = Color(0xFF2563EB)) {
                                                    Text("${uiState.upcomingPaymentsCount}", fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = "Pagos")
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Pagos",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == 5) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            // 5. SMART CART (Pestaña principal independiente: lista, despensa y plantillas)
                            val pendingCartCount = uiState.smartCartItems.count { !it.isChecked }
                            NavigationBarItem(
                                selected = selectedTab == 7,
                                onClick = { selectedTab = 7 },
                                icon = {
                                    if (pendingCartCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = EmeraldGreen) {
                                                    Text("$pendingCartCount", fontSize = 9.sp, color = Color.White)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = "Smart Cart")
                                        }
                                    } else {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = "Smart Cart")
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Smart Cart",
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selectedTab == 7) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == 7) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (PrimaryTabPagerLogic.isPrimaryTab(selectedTab)) {
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 0,
                                userScrollEnabled = false,
                                key = { PrimaryTabPagerLogic.tabForPageIndex(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("main_primary_horizontal_pager")
                            ) { page ->
                                when (PrimaryTabPagerLogic.tabForPageIndex(page)) {
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
                                            preselectedAccountIdForNewTx = null
                                            showAddTransactionDialog = true
                                        },
                                        onAddAntExpenseClick = { showAddAntExpenseDialog = true },
                                        onOpenAdvisorClick = { selectedTab = 3 },
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            preselectedAccountIdForNewTx = null
                                            showAddTransactionDialog = true
                                        },
                                        onDeleteTransaction = deleteTransactionWithUndo,
                                        onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                        onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                        onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) },
                                        onOpenSmartCartClick = { selectedTab = 7 },
                                        onOpenPaymentsClick = { selectedTab = 5 },
                                        onOpenAccountsClick = { selectedTab = 8 },
                                        accountsState = accountsState,
                                        accountNameById = accountNameById,
                                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
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
                                            preselectedAccountIdForNewTx = null
                                            showAddTransactionDialog = true
                                        },
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            preselectedAccountIdForNewTx = null
                                            showAddTransactionDialog = true
                                        },
                                        onDeleteTransaction = deleteTransactionWithUndo,
                                        onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                        onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                        onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) },
                                        onExportCsv = { viewModel.getCsvExport() },
                                        onImportCsv = { text -> viewModel.importCsv(text) },
                                        onFilterPeriodChange = { p -> viewModel.setFilterPeriod(p) },
                                        onFilterSpecificDateChange = { d -> viewModel.setFilterSpecificDate(d) },
                                        onFilterDateRangeChange = { s, e -> viewModel.setFilterDateRange(s, e) },
                                        onFilterQuincenaChange = { q -> viewModel.setFilterQuincena(q) },
                                        onFilterSubCategoryChange = { sc -> viewModel.setFilterSubCategory(sc) },
                                        onFilterBeneficiaryChange = { b -> viewModel.setFilterBeneficiary(b) },
                                        onFilterStatusChange = { st -> viewModel.setFilterStatus(st) },
                                        onFilterSourceChange = { src -> viewModel.setFilterSource(src) },
                                        onFilterDuplicatesModeChange = { m -> viewModel.setFilterDuplicatesMode(m) },
                                        onClearAllFilters = { viewModel.clearAllFilters() },
                                        onDeleteTransactions = deleteTransactionsWithUndo,
                                        onMarkTransactionsAsLegitimate = { ids -> viewModel.markTransactionsAsLegitimate(ids) },
                                        accountNameById = accountNameById,
                                        onSetCompactTransactionsMode = { compact ->
                                            viewModel.setCompactTransactionsMode(compact)
                                        }
                                    )

                                    2 -> PlannedVsRealScreen(
                                        uiState = uiState,
                                        onPreviousMonth = { viewModel.previousMonth() },
                                        onNextMonth = { viewModel.nextMonth() },
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            showAddTransactionDialog = true
                                        },
                                        onDeleteTransaction = deleteTransactionWithUndo,
                                        onHighlightTransaction = { tx, hex -> viewModel.highlightTransaction(tx, hex) },
                                        onToggleVerifyTransaction = { tx -> viewModel.toggleVerifyTransaction(tx) },
                                        onDecideTentative = { tx, decision -> viewModel.decideTentativeTransaction(tx, decision) }
                                    )

                                    5 -> PaymentsScreen(
                                        uiState = uiState,
                                        onAddPayment = { title, ben, desc, amt, due, cat, freq, isVar, rem, notes, cardId, sourceId ->
                                            viewModel.addPaymentCommitment(
                                                title = title,
                                                beneficiaryName = ben,
                                                description = desc,
                                                amount = amt,
                                                dueDateMillis = due,
                                                category = cat,
                                                frequency = freq,
                                                isVariableAmount = isVar,
                                                reminderDaysBefore = rem,
                                                notes = notes,
                                                creditCardAccountId = cardId,
                                                paymentSourceAccountId = sourceId
                                            )
                                        },
                                        onMarkAsPaid = { id -> viewModel.markPaymentAsPaid(id) },
                                        onPostponePayment = { commitment, newDueMillis, presetLabel ->
                                            viewModel.postponePaymentCommitment(commitment, newDueMillis, presetLabel)
                                        },
                                        onUpdatePayment = { p -> viewModel.updatePaymentCommitment(p) },
                                        onDeletePayment = { p -> viewModel.deletePaymentCommitment(p) },
                                        onAddBeneficiary = { name, cat, desc, cont, notes ->
                                            viewModel.addBeneficiary(name, cat, desc, cont, notes)
                                        },
                                        onUpdateBeneficiary = { ben -> viewModel.updateBeneficiary(ben) },
                                        onDeleteBeneficiary = { ben -> viewModel.deleteBeneficiary(ben) },
                                        accountNameById = accountNameById,
                                        accounts = accountsState.activeAccounts
                                    )

                                    7 -> SmartCartScreen(
                                        uiState = uiState,
                                        onAddItem = { name, qty, unit, brand, price, store, notes, url ->
                                            viewModel.addSmartCartItem(
                                                name = name,
                                                quantity = qty,
                                                unit = unit,
                                                brand = brand,
                                                price = price,
                                                store = store,
                                                notes = notes,
                                                url = url,
                                                priceSource = "MANUAL"
                                            )
                                        },
                                        onUpdateItem = { item -> viewModel.updateSmartCartItem(item) },
                                        onDeleteItem = { item -> viewModel.deleteSmartCartItem(item) },
                                        onToggleChecked = { item -> viewModel.toggleSmartCartItemChecked(item) },
                                        onSetBudget = { budget -> viewModel.setSmartCartBudget(budget) },
                                        onCheckoutExpense = { title, amount, store, purchasedNames, updatePantry ->
                                            viewModel.checkoutCartAsExpense(title, amount, store, purchasedNames, updatePantry)
                                        },
                                        onAddPantryItem = { name, qty, unit, status, price, store, notes, brand ->
                                            viewModel.addPantryItem(name, qty, unit, status, price, store, notes, brand)
                                        },
                                        onUpdatePantryItem = { item -> viewModel.updatePantryItem(item) },
                                        onTogglePantryStatus = { item -> viewModel.togglePantryStatus(item) },
                                        onDeletePantryItem = { item -> viewModel.deletePantryItem(item) },
                                        onTogglePantryInCart = { item -> viewModel.togglePantryInCart(item) },
                                        onIncorporateNeedToBuy = { viewModel.incorporateNeedToBuy() },
                                        onClearChecked = { viewModel.clearCheckedCartItems() },
                                        onClearAllCart = { viewModel.clearAllCartItems() },
                                        onSaveTemplate = { name, desc -> viewModel.saveCartAsTemplate(name, desc) },
                                        onApplyTemplate = { id -> viewModel.applyTemplateToCart(id) },
                                        onDuplicateTemplate = { id, name -> viewModel.duplicateTemplate(id, name) },
                                        onDeleteTemplate = { template -> viewModel.deleteTemplate(template) },
                                        onAddScannedItems = { items, dest, avoidDup ->
                                            viewModel.addScannedItems(items, dest, avoidDup)
                                        }
                                    )
                                }
                            }
                        } else {
                            when (selectedTab) {
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
                                    onOpenQuincenas = { selectedTab = 2 },
                                    onOpenLimits = { selectedTab = 6 },
                                    onOpenSmartCart = { selectedTab = 7 },
                                    onOpenAccounts = { selectedTab = 8 },
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

                                6 -> BudgetsScreen(
                                    uiState = uiState,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onToggleAllTime = { viewModel.toggleAllTime() },
                                    onAddBudgetLimitClick = {
                                        budgetLimitBeingEdited = null
                                        showBudgetLimitDialog = true
                                    },
                                    onEditBudgetLimit = { limit ->
                                        budgetLimitBeingEdited = limit
                                        showBudgetLimitDialog = true
                                    },
                                    onDeleteBudgetLimit = { cat -> viewModel.deleteBudgetLimit(cat) },
                                    onAddRecurringExpenseClick = { showRecurringDialog = true },
                                    onDeleteRecurringExpense = { rec -> viewModel.deleteRecurringExpense(rec) },
                                    onToggleRecurringExpense = { rec -> viewModel.toggleRecurringExpenseActive(rec) },
                                    onAddSavingsGoalClick = { showSavingsGoalDialog = true },
                                    onDeleteSavingsGoal = { goal -> viewModel.deleteSavingsGoal(goal) },
                                    onContributeSavingsGoal = { goal, amt -> viewModel.contributeToGoal(goal, amt) }
                                )

                                8 -> AccountsScreen(
                                    accountsState = accountsState,
                                    pendingCommitments = uiState.paymentCommitments.filter { !it.isPaid },
                                    currencySymbol = uiState.currencySymbol,
                                    isDiscreetMode = uiState.isDiscreetMode,
                                    isDarkTheme = uiState.isDarkTheme,
                                    onBack = { selectedTab = 0 },
                                    onSelectAccount = { id -> accountsViewModel.selectAccount(id) },
                                    onCreateAccount = { name, type, inst, last4, openMinor, limitMinor, cutoff, due, policy, autoRem, incLiq, isDef, notes ->
                                        accountsViewModel.createAccount(
                                            name = name,
                                            accountType = type,
                                            institutionName = inst,
                                            lastFourDigits = last4,
                                            openingBalanceMinor = openMinor,
                                            creditLimitMinor = limitMinor,
                                            cutoffDay = cutoff,
                                            paymentDueDay = due,
                                            installmentReservePolicy = policy,
                                            autoCreatePaymentReminder = autoRem,
                                            includeInTotalLiquidity = incLiq,
                                            isDefault = isDef,
                                            notes = notes
                                        )
                                    },
                                    onUpdateAccount = { acc -> accountsViewModel.updateAccount(acc) },
                                    onArchiveAccount = { id -> accountsViewModel.archiveAccount(id) },
                                    onUnarchiveAccount = { id -> accountsViewModel.unarchiveAccount(id) },
                                    onAdjustBalance = { accId, targetMinor, notes ->
                                        accountsViewModel.adjustAccountBalance(accId, targetMinor, notes)
                                    },
                                    onTransfer = { fromId, toId, amtMinor, title, notes ->
                                        accountsViewModel.transferBetweenAccounts(fromId, toId, amtMinor, title, notes)
                                    },
                                    onPayCard = { srcId, cardId, amtMinor, linkedCommId, notes ->
                                        accountsViewModel.payCreditCard(srcId, cardId, amtMinor, notes = notes, linkedCommitmentId = linkedCommId)
                                    },
                                    onSyncCardReminder = { cardId ->
                                        accountsViewModel.syncCreditCardStatementReminder(cardId)
                                    },
                                    onRecordCardPurchase = { cardId, title, amtMinor, cat, member, notes, enforce ->
                                        accountsViewModel.recordCreditCardPurchase(cardId, title, amtMinor, cat, familyMember = member, notes = notes, enforceCreditLimit = enforce)
                                    },
                                    onRecordCardInterest = { cardId, title, amtMinor, cat, notes ->
                                        accountsViewModel.recordCreditCardInterestOrFee(cardId, title, amtMinor, cat, notes = notes)
                                    },
                                    onCreateInstallmentPlan = { cardId, title, merchant, cat, principalMinor, count, rateBps, policyOverride, postFirst, member, notes ->
                                        accountsViewModel.createInstallmentPlan(
                                            cardAccountId = cardId,
                                            title = title,
                                            merchantName = merchant,
                                            category = cat,
                                            principalTotalMinor = principalMinor,
                                            installmentsCount = count,
                                            annualInterestRateBps = rateBps,
                                            policyOverride = policyOverride,
                                            postFirstInstallmentImmediately = postFirst,
                                            familyMember = member,
                                            notes = notes
                                        )
                                    },
                                    onPostInstallmentCharge = { planId, num ->
                                        accountsViewModel.postInstallmentCharge(planId, num)
                                    },
                                    onPostDueInstallments = { accId ->
                                        accountsViewModel.postDueInstallments(accountId = accId)
                                    },
                                    onCloseInstallmentPlan = { planId, paidOff ->
                                        accountsViewModel.cancelOrPayOffInstallmentPlan(planId, paidOff)
                                    },
                                    onAssignTransaction = { txId, accId ->
                                        accountsViewModel.assignAccountToTransaction(txId, accId)
                                    },
                                    onImportCsvToAccount = { csv, targetAccId ->
                                        accountsViewModel.importCsvToAccount(csv, targetAccId)
                                    },
                                    onConfirmReconciliation = { importedId, existingId ->
                                        accountsViewModel.confirmReconciliation(importedId, existingId)
                                    },
                                    onRejectReconciliation = { importedId ->
                                        accountsViewModel.rejectReconciliation(importedId)
                                    },
                                    onExportCsv = { viewModel.getCsvExport() },
                                    onAddStandardMovementForAccount = { acc ->
                                        editingTransaction = null
                                        preselectedAccountIdForNewTx = acc.id
                                        showAddTransactionDialog = true
                                    }
                                )
                            }
                        }

                        // FAB Chapín flotante, arrastrable, acotado y con persistencia local
                        ChapinQuickFab(
                            onSaveExpense = { title, amt, cat, notes ->
                                viewModel.addChapinQuickExpense(title, amt, cat, notes)
                            },
                            onAddFullTransaction = {
                                editingTransaction = null
                                preselectedAccountIdForNewTx = null
                                showAddTransactionDialog = true
                            },
                            fabOffsetXFraction = uiState.fabOffsetXFraction,
                            fabOffsetYFraction = uiState.fabOffsetYFraction,
                            onPositionChanged = { xFrac, yFrac ->
                                viewModel.updateChapinFabPosition(xFrac, yFrac)
                            },
                            onResetPosition = {
                                viewModel.resetChapinFabPosition()
                            }
                        )
                    }

                    // Diálogos modales
                    if (showAddTransactionDialog) {
                        AddEditTransactionDialog(
                            transaction = editingTransaction,
                            currencySymbol = uiState.currencySymbol,
                            accounts = accountsState.activeAccounts,
                            initialAccountId = preselectedAccountIdForNewTx,
                            onDismiss = {
                                showAddTransactionDialog = false
                                editingTransaction = null
                                preselectedAccountIdForNewTx = null
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
                            initialLimit = budgetLimitBeingEdited,
                            currencySymbol = uiState.currencySymbol,
                            onDismiss = {
                                showBudgetLimitDialog = false
                                budgetLimitBeingEdited = null
                            },
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

                    // Modales del Menú Lateral
                    if (showNotificationsDialog) {
                        NotificationsSettingsDialog(onDismiss = { showNotificationsDialog = false })
                    }

                    if (showPrivacySecurityDialog) {
                        PrivacySecurityDialog(onDismiss = { showPrivacySecurityDialog = false })
                    }

                    if (showTermsDialog) {
                        TermsAndPrivacyDialog(
                            onAccept = { showTermsDialog = false },
                            onDismiss = { showTermsDialog = false },
                            isMandatory = false,
                            termsVersion = TermsAcceptanceManager.CURRENT_TERMS_VERSION,
                            acceptanceRecord = TermsAcceptanceManager.getAcceptanceRecord(prefs)
                        )
                    }

                    if (showAboutDialog) {
                        AboutQuetziDialog(onDismiss = { showAboutDialog = false })
                    }
                }
                }
                }
            }
        }
    }
}
