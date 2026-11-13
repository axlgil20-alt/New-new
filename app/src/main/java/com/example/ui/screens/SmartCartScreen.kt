@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ShoppingTemplateEntity
import com.example.data.model.SmartCartItemEntity
import com.example.data.model.getPantryStatus
import com.example.data.model.isPantryProduct
import com.example.ui.components.GUATEMALA_SUPERMARKETS
import com.example.ui.components.PantryItemStatus
import com.example.ui.components.UNIT_OPTIONS
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.FormatUtils
import com.example.ui.util.ListScannerHelper
import com.example.ui.util.ScannedGroceryItem
import com.example.ui.viewmodel.BudgetUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCartScreen(
    uiState: BudgetUiState,
    onAddItem: (name: String, quantity: Double, unit: String, brand: String, price: Double, store: String, notes: String, url: String) -> Unit,
    onUpdateItem: (SmartCartItemEntity) -> Unit,
    onDeleteItem: (SmartCartItemEntity) -> Unit,
    onToggleChecked: (SmartCartItemEntity) -> Unit,
    onSetBudget: (Double) -> Unit,
    onCheckoutExpense: (title: String, amount: Double, store: String, purchasedNames: List<String>, updatePantry: Boolean) -> Unit,
    onAddPantryItem: (name: String, quantity: Double, unit: String, status: String, price: Double, store: String, notes: String, brand: String) -> Unit,
    onUpdatePantryItem: (SmartCartItemEntity) -> Unit,
    onTogglePantryStatus: (SmartCartItemEntity) -> Unit,
    onDeletePantryItem: (SmartCartItemEntity) -> Unit,
    onTogglePantryInCart: (SmartCartItemEntity) -> Unit,
    onIncorporateNeedToBuy: () -> Unit,
    onClearChecked: () -> Unit,
    onClearAllCart: () -> Unit,
    onSaveTemplate: (name: String, description: String) -> Unit,
    onApplyTemplate: (Long) -> Unit,
    onDuplicateTemplate: (Long, String) -> Unit,
    onDeleteTemplate: (ShoppingTemplateEntity) -> Unit,
    onAddScannedItems: (items: List<ScannedGroceryItem>, destination: String, avoidDuplicates: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Lista del Súper, 1: Mi Despensa, 2: Plantillas Reutilizables

    // Filtros
    var cartStoreFilter by remember { mutableStateOf<String?>("Todas") }
    var cartSearchQuery by remember { mutableStateOf("") }
    var pantryStatusFilter by remember { mutableStateOf<String?>("Todos") }
    var pantrySearchQuery by remember { mutableStateOf("") }

    // Diálogos Modales
    var showAddEditCartDialog by remember { mutableStateOf(false) }
    var cartItemToEdit by remember { mutableStateOf<SmartCartItemEntity?>(null) }
    var showAddEditPantryDialog by remember { mutableStateOf(false) }
    var pantryItemToEdit by remember { mutableStateOf<SmartCartItemEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmItem by remember { mutableStateOf<SmartCartItemEntity?>(null) }
    var showDeleteConfirmPantry by remember { mutableStateOf<SmartCartItemEntity?>(null) }
    var showDeleteConfirmTemplate by remember { mutableStateOf<ShoppingTemplateEntity?>(null) }

    // Escáner OCR
    var showScannerChoiceDialog by remember { mutableStateOf(false) }
    var isScanningOcr by remember { mutableStateOf(false) }
    var showScannedReviewDialog by remember { mutableStateOf(false) }
    val scannedItemsToReview = remember { mutableStateListOf<ScannedGroceryItem>() }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            isScanningOcr = true
            ListScannerHelper.processImageBitmap(
                bitmap = bitmap,
                onResult = { items ->
                    isScanningOcr = false
                    scannedItemsToReview.clear()
                    scannedItemsToReview.addAll(items)
                    showScannedReviewDialog = true
                },
                onError = { e ->
                    isScanningOcr = false
                    Toast.makeText(context, "Error en escáner: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = ListScannerHelper.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                isScanningOcr = true
                ListScannerHelper.processImageBitmap(
                    bitmap = bitmap,
                    onResult = { items ->
                        isScanningOcr = false
                        scannedItemsToReview.clear()
                        scannedItemsToReview.addAll(items)
                        showScannedReviewDialog = true
                    },
                    onError = { e ->
                        isScanningOcr = false
                        Toast.makeText(context, "Error en escáner: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Cálculos de Lista del Súper
    val filteredCartItems = remember(uiState.smartCartItems, cartStoreFilter, cartSearchQuery) {
        uiState.smartCartItems.filter { item ->
            val matchStore = cartStoreFilter == null || cartStoreFilter == "Todas" || item.storeName.equals(cartStoreFilter, ignoreCase = true)
            val matchQuery = cartSearchQuery.isBlank() || item.name.contains(cartSearchQuery, ignoreCase = true) || item.brand.contains(cartSearchQuery, ignoreCase = true)
            matchStore && matchQuery
        }
    }

    val totalEstimatedCart = remember(uiState.smartCartItems) {
        uiState.smartCartItems.sumOf { it.quantity * it.estimatedUnitPrice }
    }
    val checkedCartTotal = remember(uiState.smartCartItems) {
        uiState.smartCartItems.filter { it.isChecked }.sumOf { it.quantity * it.estimatedUnitPrice }
    }
    val pendingCartCount = remember(uiState.smartCartItems) {
        uiState.smartCartItems.count { !it.isChecked }
    }

    // Cálculos de Despensa
    val filteredPantryItems = remember(uiState.smartPantryItems, pantryStatusFilter, pantrySearchQuery) {
        uiState.smartPantryItems.filter { item ->
            val status = item.getPantryStatus()
            val matchStatus = when (pantryStatusFilter) {
                "Verde" -> status == "PANTRY_VERDE"
                "Amarillo" -> status == "PANTRY_AMARILLO"
                "Rojo" -> status == "PANTRY_ROJO"
                else -> true
            }
            val matchQuery = pantrySearchQuery.isBlank() || item.name.contains(pantrySearchQuery, ignoreCase = true) || item.brand.contains(pantrySearchQuery, ignoreCase = true)
            matchStatus && matchQuery
        }
    }

    val redPantryCount = remember(uiState.smartPantryItems) {
        uiState.smartPantryItems.count { it.getPantryStatus() == "PANTRY_ROJO" }
    }

    val isDark = uiState.isDarkTheme
    val pageBackground = if (isDark) Color(0xFF14110E) else Color(0xFFFAF5ED)
    val backgroundScrim = if (isDark) Color(0xD914110E) else Color(0xD1FAF5ED)
    val heroTextShadow = Shadow(color = Color(0xCC000000), offset = Offset(0f, 2f), blurRadius = 6f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {
        // Fotografía de fondo para el cuerpo inferior: Mercado de Chichicastenango con velo protector
        Image(
            painter = painterResource(id = R.drawable.img_chichicastenango_market),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isDark) 0.14f else 0.18f)
        )
        // Velo opaco protector para preservar lectura nítida sobre la textura del mercado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundScrim)
        )

        // FOTOGRAFÍA HEROICA SUPERIOR DOMINANTE: Mercado tradicional de Chichicastenango, Guatemala
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_chichicastenango_market),
                contentDescription = "Mercado tradicional de Chichicastenango, Guatemala",
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            )
            // Gradiente vertical de protección: sólido arriba para lectura nítida,
            // ventana clara en el centro para apreciar el mercado, y fusión limpia al fondo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color(0xEB140D08),
                            0.28f to Color(0xA6140D08),
                            0.72f to Color(0x33000000),
                            1.0f to pageBackground
                        )
                    )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. CABECERA PRINCIPAL INTEGRADA SOBRE EL MERCADO DE CHICHICASTENANGO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen,
                            border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Smart Cart",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SMART CART",
                                style = MaterialTheme.typography.titleLarge.copy(shadow = heroTextShadow),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Planificá tus compras y ahorrá",
                                style = MaterialTheme.typography.bodySmall.copy(shadow = heroTextShadow),
                                color = Color(0xFFFEF3C7),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Acción Rápida de Cabecera (Escáner OCR) con fondo sólido institucional
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF0E2921) else Color(0xFF063729),
                        border = BorderStroke(1.2.dp, Color(0xFF6EE7B7)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showScannerChoiceDialog = true }
                            .testTag("smart_cart_scan_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Escanear lista",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Escanear",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // PESTAÑAS PRINCIPALES CON CONTENEDOR SÓLIDO OPACO DE ALTO CONTRASTE
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF0F221C) else Color(0xFF063226),
                    border = BorderStroke(1.2.dp, Color(0xFF34D399)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = {},
                        divider = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 0) EmeraldGreen else Color.Transparent)
                                .testTag("tab_supermarket_list"),
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Lista del Súper",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (selectedTab == 0) Color.White else Color(0xFFD1FAE5)
                                    )
                                    if (pendingCartCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF047857),
                                            border = BorderStroke(0.8.dp, Color(0xFFA7F3D0)),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$pendingCartCount",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 1) EmeraldGreen else Color.Transparent)
                                .testTag("tab_my_pantry"),
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Mi Despensa",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (selectedTab == 1) Color.White else Color(0xFFD1FAE5)
                                    )
                                    if (redPantryCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFB91C1C),
                                            border = BorderStroke(0.8.dp, Color(0xFFFECACA)),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$redPantryCount",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )

                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == 2) EmeraldGreen else Color.Transparent)
                                .testTag("tab_shopping_templates"),
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Plantillas",
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (selectedTab == 2) Color.White else Color(0xFFD1FAE5)
                                    )
                                    if (uiState.shoppingTemplates.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF4338CA),
                                            border = BorderStroke(0.8.dp, Color(0xFFC7D2FE)),
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${uiState.shoppingTemplates.size}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

        // 2. CONTENIDO SEGÚN LA PESTAÑA ACTIVA
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> ShoppingListContent(
                    cartItems = filteredCartItems,
                    totalEstimated = totalEstimatedCart,
                    checkedTotal = checkedCartTotal,
                    budgetAmount = uiState.smartCartBudget,
                    currencySymbol = uiState.currencySymbol,
                    isDiscreetMode = uiState.isDiscreetMode,
                    isDarkTheme = isDark,
                    selectedStoreFilter = cartStoreFilter,
                    searchQuery = cartSearchQuery,
                    onSearchQueryChange = { cartSearchQuery = it },
                    onSelectStoreFilter = { cartStoreFilter = it },
                    onAddItemClick = {
                        cartItemToEdit = null
                        showAddEditCartDialog = true
                    },
                    onEditItemClick = { item ->
                        cartItemToEdit = item
                        showAddEditCartDialog = true
                    },
                    onDeleteItemClick = { item -> showDeleteConfirmItem = item },
                    onToggleChecked = onToggleChecked,
                    onOpenBudgetDialog = { showBudgetDialog = true },
                    onOpenCheckoutDialog = { showCheckoutDialog = true },
                    onClearChecked = onClearChecked,
                    onClearAll = onClearAllCart,
                    onOpenTemplatesTab = { selectedTab = 2 },
                    onSaveAsTemplateClick = { showSaveTemplateDialog = true }
                )

                1 -> PantryContent(
                    pantryItems = filteredPantryItems,
                    rawPantryCount = uiState.smartPantryItems.size,
                    redCount = redPantryCount,
                    isDarkTheme = isDark,
                    selectedStatusFilter = pantryStatusFilter,
                    searchQuery = pantrySearchQuery,
                    onSearchQueryChange = { pantrySearchQuery = it },
                    onSelectStatusFilter = { pantryStatusFilter = it },
                    onAddPantryItemClick = {
                        pantryItemToEdit = null
                        showAddEditPantryDialog = true
                    },
                    onEditPantryItemClick = { item ->
                        pantryItemToEdit = item
                        showAddEditPantryDialog = true
                    },
                    onDeletePantryItemClick = { item -> showDeleteConfirmPantry = item },
                    onToggleStatus = onTogglePantryStatus,
                    onTogglePantryInCart = onTogglePantryInCart,
                    onIncorporateNeedToBuy = onIncorporateNeedToBuy
                )

                2 -> TemplatesContent(
                    templates = uiState.shoppingTemplates,
                    currencySymbol = uiState.currencySymbol,
                    isDarkTheme = isDark,
                    onApplyTemplate = { id ->
                        onApplyTemplate(id)
                        selectedTab = 0 // Regresar a la lista de compras para ver los productos cargados
                    },
                    onDuplicateTemplate = onDuplicateTemplate,
                    onDeleteTemplate = { template -> showDeleteConfirmTemplate = template },
                    onSaveCurrentCartClick = { showSaveTemplateDialog = true }
                )
            }
        }
    }
}

    // =========================================================================
    // DIÁLOGOS MODALES
    // =========================================================================

    // Diálogo: Agregar / Editar Producto en Lista del Súper
    if (showAddEditCartDialog) {
        AddEditCartItemDialog(
            item = cartItemToEdit,
            currencySymbol = uiState.currencySymbol,
            onDismiss = {
                showAddEditCartDialog = false
                cartItemToEdit = null
            },
            onSave = { name, qty, unit, brand, price, store, notes, url ->
                if (cartItemToEdit == null) {
                    onAddItem(name, qty, unit, brand, price, store, notes, url)
                } else {
                    onUpdateItem(
                        cartItemToEdit!!.copy(
                            name = name,
                            quantity = qty,
                            unit = unit,
                            brand = brand,
                            estimatedUnitPrice = price,
                            storeName = store,
                            notes = notes,
                            productUrl = url
                        )
                    )
                }
                showAddEditCartDialog = false
                cartItemToEdit = null
            }
        )
    }

    // Diálogo: Registrar / Editar Artículo en Mi Despensa
    if (showAddEditPantryDialog) {
        AddEditPantryItemDialog(
            item = pantryItemToEdit,
            currencySymbol = uiState.currencySymbol,
            onDismiss = {
                showAddEditPantryDialog = false
                pantryItemToEdit = null
            },
            onSave = { name, qty, unit, status, price, store, notes, brand ->
                if (pantryItemToEdit == null) {
                    onAddPantryItem(name, qty, unit, status, price, store, notes, brand)
                } else {
                    onUpdatePantryItem(
                        pantryItemToEdit!!.copy(
                            name = name,
                            quantity = qty,
                            unit = unit,
                            priceSource = status,
                            estimatedUnitPrice = price,
                            storeName = store,
                            notes = notes,
                            brand = brand
                        )
                    )
                }
                showAddEditPantryDialog = false
                pantryItemToEdit = null
            }
        )
    }

    // Diálogo: Configurar Presupuesto de Súper
    if (showBudgetDialog) {
        SetBudgetDialog(
            currentBudget = uiState.smartCartBudget,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showBudgetDialog = false },
            onSave = { newBudget ->
                onSetBudget(newBudget)
                showBudgetDialog = false
            }
        )
    }

    // Diálogo: Confirmar Compra & Registrar Gasto
    if (showCheckoutDialog) {
        CheckoutConfirmationDialog(
            cartItems = uiState.smartCartItems,
            totalAmount = if (checkedCartTotal > 0.0) checkedCartTotal else totalEstimatedCart,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { storeName, amount, updatePantry ->
                val purchasedNames = uiState.smartCartItems
                    .filter { if (checkedCartTotal > 0.0) it.isChecked else true }
                    .map { it.name }
                onCheckoutExpense(
                    "Súper $storeName",
                    amount,
                    storeName,
                    purchasedNames,
                    updatePantry
                )
                showCheckoutDialog = false
            }
        )
    }

    // Diálogo: Guardar Lista Activa como Plantilla
    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            itemCount = uiState.smartCartItems.size,
            onDismiss = { showSaveTemplateDialog = false },
            onSave = { name, desc ->
                onSaveTemplate(name, desc)
                showSaveTemplateDialog = false
            }
        )
    }

    // Diálogos de Confirmación de Eliminación
    if (showDeleteConfirmItem != null) {
        BasicAlertDialog(
            onDismissRequest = { showDeleteConfirmItem = null }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Eliminar de la lista",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Deseas eliminar «${showDeleteConfirmItem!!.name}» de tu lista de compras?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteConfirmItem = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onDeleteItem(showDeleteConfirmItem!!)
                                showDeleteConfirmItem = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White)
                        ) {
                            Text("Eliminar", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmPantry != null) {
        BasicAlertDialog(
            onDismissRequest = { showDeleteConfirmPantry = null }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Eliminar de la despensa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Deseas retirar «${showDeleteConfirmPantry!!.name}» de Mi Despensa?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteConfirmPantry = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onDeletePantryItem(showDeleteConfirmPantry!!)
                                showDeleteConfirmPantry = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White)
                        ) {
                            Text("Eliminar", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmTemplate != null) {
        BasicAlertDialog(
            onDismissRequest = { showDeleteConfirmTemplate = null }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Eliminar plantilla",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Deseas eliminar permanentemente la plantilla «${showDeleteConfirmTemplate!!.name}»?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteConfirmTemplate = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onDeleteTemplate(showDeleteConfirmTemplate!!)
                                showDeleteConfirmTemplate = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White)
                        ) {
                            Text("Eliminar", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Escáner OCR: Selección de Cámara o Galería
    if (showScannerChoiceDialog) {
        BasicAlertDialog(
            onDismissRequest = { showScannerChoiceDialog = false }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Escanear lista de compras",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Toma una foto de tu lista manuscrita o ticket, o selecciónala de tu galería.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showScannerChoiceDialog = false
                                cameraLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cámara")
                        }

                        Button(
                            onClick = {
                                showScannerChoiceDialog = false
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galería")
                        }
                    }
                }
            }
        }
    }

    // Estado de carga del escáner OCR
    if (isScanningOcr) {
        BasicAlertDialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = EmeraldGreen, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Digitalizando lista...", fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // Diálogo de revisión de artículos escaneados
    if (showScannedReviewDialog) {
        BasicAlertDialog(
            onDismissRequest = { showScannedReviewDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Artículos Digitalizados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Revisa los productos detectados e incorpóralos a tu lista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 350.dp)
                    ) {
                        items(scannedItemsToReview) { item ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { chk ->
                                            val idx = scannedItemsToReview.indexOf(item)
                                            if (idx != -1) {
                                                scannedItemsToReview[idx] = item.copy(isSelected = chk)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "Cant: ${item.quantity} ${item.unit} • Est: Q ${item.price}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showScannedReviewDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onAddScannedItems(scannedItemsToReview.toList(), "CART", true)
                                showScannedReviewDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Agregar a Lista", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTE: CONTENIDO DE LA LISTA DEL SÚPER
// =============================================================================

private fun accessibleStoreSolidColor(baseColor: Color): Color {
    // Asegura contraste >= 4.5:1 con texto blanco en insignias y chips seleccionados
    return when (baseColor.value) {
        Color(0xFFE65100).value -> Color(0xFFBF360C) // Maxi Despensa naranja oscuro accesible
        Color(0xFFE53935).value -> Color(0xFFC62828) // La Barata rojo oscuro accesible
        Color(0xFF0071DC).value -> Color(0xFF005BB5) // Walmart azul oscuro accesible
        else -> baseColor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListContent(
    cartItems: List<SmartCartItemEntity>,
    totalEstimated: Double,
    checkedTotal: Double,
    budgetAmount: Double,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    selectedStoreFilter: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectStoreFilter: (String) -> Unit,
    onAddItemClick: () -> Unit,
    onEditItemClick: (SmartCartItemEntity) -> Unit,
    onDeleteItemClick: (SmartCartItemEntity) -> Unit,
    onToggleChecked: (SmartCartItemEntity) -> Unit,
    onOpenBudgetDialog: () -> Unit,
    onOpenCheckoutDialog: () -> Unit,
    onClearChecked: () -> Unit,
    onClearAll: () -> Unit,
    onOpenTemplatesTab: () -> Unit,
    onSaveAsTemplateClick: () -> Unit
) {
    val progress = if (totalEstimated > 0) (checkedTotal / totalEstimated).toFloat().coerceIn(0f, 1f) else 0f
    val difference = budgetAmount - totalEstimated

    val cardSurface = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFD6E4DF)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
    val accentEmeraldTint = if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen
    val incomeText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)
    val dangerText = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // RESUMEN FINANCIERO DEL SÚPER (Tarjeta ejecutiva sólida opaca)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.2.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_cart_budget_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL ESTIMADO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryText
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(totalEstimated, currencySymbol, isDiscreetMode),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = accentEmeraldText
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenBudgetDialog() }
                                    .testTag("smart_cart_budget_button")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Presupuesto asignado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondaryText
                                    )
                                    Text(
                                        text = if (budgetAmount > 0) FormatUtils.formatCurrency(budgetAmount, currencySymbol, isDiscreetMode) else "Sin límite (Fijar)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (budgetAmount > 0) primaryText else accentEmeraldText
                                    )
                                }
                            }
                        }

                        if (budgetAmount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (difference >= 0) "Dentro del presupuesto" else "Exceso proyectado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (difference >= 0) incomeText else dangerText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (difference >= 0) "Restante: " else "Exceso: "}${FormatUtils.formatCurrency(kotlin.math.abs(difference), currencySymbol, isDiscreetMode)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (difference >= 0) incomeText else dangerText,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        // Barra de progreso de productos marcados
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progreso en carrito",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryText
                                )
                                Text(
                                    text = "${FormatUtils.formatCurrency(checkedTotal, currencySymbol, isDiscreetMode)} (${(progress * 100).toInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentEmeraldTint,
                                trackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FILTRO DE SUPERMERCADOS DE GUATEMALA (Contenedor sólido opaco + chips opacos)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = accentEmeraldTint,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filtrar por tienda:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val storeList = remember {
                            listOf("Todas") + GUATEMALA_SUPERMARKETS.map { it.name }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(storeList) { storeName ->
                                val isSelected = (selectedStoreFilter == null && storeName == "Todas") || selectedStoreFilter == storeName
                                val supermarketInfo = GUATEMALA_SUPERMARKETS.find { it.name == storeName }
                                val solidSelectedColor = accessibleStoreSolidColor(supermarketInfo?.badgeColor ?: EmeraldGreen)

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectStoreFilter(storeName) },
                                    label = {
                                        Text(
                                            text = storeName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                        labelColor = primaryText,
                                        selectedContainerColor = solidSelectedColor,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                        selectedBorderColor = solidSelectedColor
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 42.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // BARRA DE BÚSQUEDA Y BOTÓN AGREGAR (Superficies sólidas opacas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "Buscar producto...",
                                fontSize = 13.sp,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = accentEmeraldTint,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = secondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardSurface,
                            unfocusedContainerColor = cardSurface,
                            focusedTextColor = primaryText,
                            unfocusedTextColor = primaryText,
                            focusedBorderColor = accentEmeraldTint,
                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    )

                    Button(
                        onClick = onAddItemClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag("smart_cart_add_item_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ACCIONES SECUNDARIAS SOBRE BARRA SÓLIDA OPACA (Guardar plantilla, Desmarcar, Vaciar)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardSurface,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onSaveAsTemplateClick,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("btn_save_as_template")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = accentEmeraldText
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar plantilla", fontSize = 12.sp, color = accentEmeraldText, fontWeight = FontWeight.Bold)
                        }

                        Row {
                            if (cartItems.any { it.isChecked }) {
                                TextButton(
                                    onClick = onClearChecked,
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Desmarcar", fontSize = 12.sp, color = secondaryText, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (cartItems.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearAll,
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Vaciar", fontSize = 12.sp, color = dangerText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // LISTA DE PRODUCTOS ACTIVOS
            if (cartItems.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = accentEmeraldText,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tu lista de compras está vacía",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Agrega productos manualmente, cárgalos desde una plantilla o utiliza el escáner OCR.",
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = onOpenTemplatesTab,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, accentEmeraldTint),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF0FDF4),
                                    contentColor = accentEmeraldText
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = accentEmeraldText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ver Plantillas Reutilizables", fontWeight = FontWeight.Bold, color = accentEmeraldText)
                            }
                        }
                    }
                }
            } else {
                items(cartItems, key = { it.id }) { item ->
                    CartItemCard(
                        item = item,
                        currencySymbol = currencySymbol,
                        isDarkTheme = isDarkTheme,
                        onToggleChecked = { onToggleChecked(item) },
                        onEditClick = { onEditItemClick(item) },
                        onDeleteClick = { onDeleteItemClick(item) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // BARRA FIJA INFERIOR SÓLIDA PARA CONFIRMAR COMPRA
        if (cartItems.isNotEmpty()) {
            Surface(
                color = cardSurface,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val checkedCount = cartItems.count { it.isChecked }
                        Text(
                            text = if (checkedCount > 0) "$checkedCount en carrito" else "${cartItems.size} productos",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText
                        )
                        Text(
                            text = FormatUtils.formatCurrency(if (checkedTotal > 0.0) checkedTotal else totalEstimated, currencySymbol, isDiscreetMode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = accentEmeraldText
                        )
                    }

                    Button(
                        onClick = onOpenCheckoutDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("smart_cart_confirm_checkout_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirmar Compra", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTE: TARJETA INDIVIDUAL DE PRODUCTO EN LISTA DE SÚPER
// =============================================================================

@Composable
private fun CartItemCard(
    item: SmartCartItemEntity,
    currencySymbol: String,
    isDarkTheme: Boolean,
    onToggleChecked: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val subtotal = item.quantity * item.estimatedUnitPrice
    val supermarketInfo = GUATEMALA_SUPERMARKETS.find { it.name.equals(item.storeName, ignoreCase = true) }
    val storeBadgeColor = accessibleStoreSolidColor(supermarketInfo?.badgeColor ?: Color(0xFF4B5563))

    val cardBg = if (item.isChecked) {
        if (isDarkTheme) Color(0xFF14241D) else Color(0xFFF0FDF4)
    } else {
        if (isDarkTheme) Color(0xFF1E293B) else Color.White
    }
    val cardBorder = if (item.isChecked) {
        if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen
    } else {
        if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    }
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.2.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isChecked) 1.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleChecked,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("cart_item_checkbox_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (item.isChecked) "Comprado" else "Pendiente",
                    tint = if (item.isChecked) accentEmeraldText else secondaryText
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isChecked) secondaryText else primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.brand.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${item.brand})",
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryText,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${item.quantity} ${item.unit} x ${FormatUtils.formatCurrency(item.estimatedUnitPrice, currencySymbol, false)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText
                    )

                    Text(
                        text = "Subtotal: ${FormatUtils.formatCurrency(subtotal, currencySymbol, false)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isChecked) secondaryText else accentEmeraldText
                    )
                }

                if (item.storeName.isNotBlank() && item.storeName != "General") {
                    Spacer(modifier = Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = storeBadgeColor,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = item.storeName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = accentEmeraldText,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTE: CONTENIDO DE MI DESPENSA (CONTROL DE EXISTENCIAS)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantryContent(
    pantryItems: List<SmartCartItemEntity>,
    rawPantryCount: Int,
    redCount: Int,
    isDarkTheme: Boolean,
    selectedStatusFilter: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectStatusFilter: (String) -> Unit,
    onAddPantryItemClick: () -> Unit,
    onEditPantryItemClick: (SmartCartItemEntity) -> Unit,
    onDeletePantryItemClick: (SmartCartItemEntity) -> Unit,
    onToggleStatus: (SmartCartItemEntity) -> Unit,
    onTogglePantryInCart: (SmartCartItemEntity) -> Unit,
    onIncorporateNeedToBuy: () -> Unit
) {
    val cardSurface = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFD6E4DF)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
    val accentEmeraldTint = if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // BANNER DE CONTROL DE EXISTENCIAS (Tarjeta sólida opaca)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            border = BorderStroke(1.2.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Kitchen,
                        contentDescription = null,
                        tint = accentEmeraldTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Control de existencias del hogar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Monitorea lo que tienes en casa durante la quincena. Cambia el estado a Rojo cuando se agote y repón a la lista del súper con un toque.",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )

                if (redCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onIncorporateNeedToBuy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB91C1C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("pantry_replenish_all_button")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Agregar todo lo agotado ($redCount) a la Lista del Súper",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // FILTRO DE ESTADOS: TODOS, VERDE, AMARILLO, ROJO (Contenedor sólido opaco + chips opacos)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardSurface,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            val filterStates = listOf("Todos", "Verde", "Amarillo", "Rojo")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterStates.forEach { filter ->
                    val isSelected = (selectedStatusFilter == null && filter == "Todos") || selectedStatusFilter == filter
                    val selectedBg = when (filter) {
                        "Verde" -> Color(0xFF047857)
                        "Amarillo" -> Color(0xFFB45309)
                        "Rojo" -> Color(0xFFB91C1C)
                        else -> EmeraldGreen
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStatusFilter(filter) },
                        label = {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            labelColor = primaryText,
                            selectedContainerColor = selectedBg,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                            selectedBorderColor = selectedBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 42.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // BÚSQUEDA Y BOTÓN REGISTRAR (Superficies sólidas opacas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Buscar en despensa...",
                        fontSize = 13.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = accentEmeraldTint,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = secondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardSurface,
                    unfocusedContainerColor = cardSurface,
                    focusedTextColor = primaryText,
                    unfocusedTextColor = primaryText,
                    focusedBorderColor = accentEmeraldTint,
                    unfocusedBorderColor = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
            )

            Button(
                onClick = onAddPantryItemClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .testTag("pantry_add_item_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Registrar", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LISTA DE PRODUCTOS EN DESPENSA
        if (pantryItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = accentEmeraldText,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (rawPantryCount == 0) "Tu despensa está vacía" else "No hay productos con este filtro",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Registra los productos que tienes en casa para controlar existencias.",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(pantryItems, key = { it.id }) { item ->
                    PantryItemCard(
                        item = item,
                        isDarkTheme = isDarkTheme,
                        onToggleStatus = { onToggleStatus(item) },
                        onToggleInCart = { onTogglePantryInCart(item) },
                        onEditClick = { onEditPantryItemClick(item) },
                        onDeleteClick = { onDeletePantryItemClick(item) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTE: TARJETA INDIVIDUAL DE MI DESPENSA (VERDE / AMARILLO / ROJO)
// =============================================================================

@Composable
private fun PantryItemCard(
    item: SmartCartItemEntity,
    isDarkTheme: Boolean,
    onToggleStatus: () -> Unit,
    onToggleInCart: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val status = item.getPantryStatus()
    val isInCart = item.priceSource.contains("+CART")

    val (statusLabel, statusDotColor, statusFg, statusBg, statusBorder) = when (status) {
        "PANTRY_VERDE" -> listOf(
            "Suficiente",
            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669),
            if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
            if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
        )
        "PANTRY_AMARILLO" -> listOf(
            "Por agotarse",
            if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706),
            if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E),
            if (isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7),
            if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        )
        "PANTRY_ROJO" -> listOf(
            "Agotado",
            if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626),
            if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B),
            if (isDarkTheme) Color(0xFF5C1D24) else Color(0xFFFEE2E2),
            if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
        )
        else -> listOf(
            "Disponible",
            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669),
            if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
            if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
        )
    }

    val label = statusLabel as String
    val dotColor = statusDotColor as Color
    val fgColor = statusFg as Color
    val bgColor = statusBg as Color
    val borderColor = statusBorder as Color

    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White),
        border = BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pantry_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = dotColor,
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.brand.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${item.brand})",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = accentEmeraldText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.quantity} ${item.unit}${if (item.storeName.isNotBlank() && item.storeName != "General") " • ${item.storeName}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )

                // CHIP INTERACTIVO SÓLIDO PARA CAMBIAR ESTADO (Verde -> Amarillo -> Rojo)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleStatus() }
                        .testTag("pantry_status_toggle_${item.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = fgColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Cambiar estado",
                            tint = fgColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // ACCIÓN DE REPOSICIÓN: AGREGAR A LA LISTA DEL SÚPER CON UN TOQUE
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status == "PANTRY_ROJO" || status == "PANTRY_AMARILLO") {
                    if (isInCart) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleInCart() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "En lista de compras ✓ (Retirar)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onToggleInCart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status == "PANTRY_ROJO") Color(0xFFB91C1C) else Color(0xFFB45309),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("btn_add_pantry_to_cart_${item.id}")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Agregar a la lista del súper",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Estado Verde
                    Text(
                        text = "Existencia suficiente en casa",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)
                    )
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENTE: CONTENIDO DE PLANTILLAS REUTILIZABLES
// =============================================================================

@Composable
private fun TemplatesContent(
    templates: List<ShoppingTemplateEntity>,
    currencySymbol: String,
    isDarkTheme: Boolean,
    onApplyTemplate: (Long) -> Unit,
    onDuplicateTemplate: (Long, String) -> Unit,
    onDeleteTemplate: (ShoppingTemplateEntity) -> Unit,
    onSaveCurrentCartClick: () -> Unit
) {
    val cardSurface = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFD6E4DF)
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
    val accentEmeraldTint = if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // BANNER DE PLANTILLAS (Tarjeta sólida opaca)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardSurface),
            border = BorderStroke(1.2.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = accentEmeraldTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Listas de Compras Reutilizables",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aprovecha tus compras habituales. Reutiliza listas de quincena o mes con un solo toque sin crear productos en Mi Despensa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onSaveCurrentCartClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("templates_save_cart_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar lista activa como plantilla", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    border = BorderStroke(1.dp, cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = accentEmeraldText,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No hay plantillas guardadas",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Arma una lista en el súper y presiona «Guardar plantilla».",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(templates, key = { it.id }) { template ->
                    TemplateItemCard(
                        template = template,
                        currencySymbol = currencySymbol,
                        isDarkTheme = isDarkTheme,
                        onApply = { onApplyTemplate(template.id) },
                        onDuplicate = { onDuplicateTemplate(template.id, "${template.name} (Copia)") },
                        onDelete = { onDeleteTemplate(template) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun TemplateItemCard(
    template: ShoppingTemplateEntity,
    currencySymbol: String,
    isDarkTheme: Boolean,
    onApply: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White),
        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("template_card_${template.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    if (template.description.isNotBlank()) {
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicar",
                            tint = accentEmeraldText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${template.itemCount} productos • Total est: ${FormatUtils.formatCurrency(template.estimatedTotal, currencySymbol, false)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentEmeraldText,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("btn_load_template_${template.id}")
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cargar a mi Lista", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =============================================================================
// DIÁLOGOS DE ENTRADA Y ACCIÓN
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCartItemDialog(
    item: SmartCartItemEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: Double, unit: String, brand: String, price: Double, store: String, notes: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var quantity by remember { mutableDoubleStateOf(item?.quantity ?: 1.0) }
    var unit by remember { mutableStateOf(item?.unit ?: "Unidad") }
    var brand by remember { mutableStateOf(item?.brand ?: "") }
    var priceStr by remember { mutableStateOf(if (item != null && item.estimatedUnitPrice > 0) item.estimatedUnitPrice.toString() else "") }
    var storeName by remember { mutableStateOf(item?.storeName ?: "General") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }

    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var storeDropdownExpanded by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (item == null) "Agregar Producto a la Lista" else "Editar Producto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del producto *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString(),
                        onValueChange = { quantity = it.toDoubleOrNull() ?: 1.0 },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            UNIT_OPTIONS.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        unit = opt
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca (opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Precio est. ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = storeDropdownExpanded,
                    onExpandedChange = { storeDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supermercado preferido") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storeDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = storeDropdownExpanded,
                        onDismissRequest = { storeDropdownExpanded = false }
                    ) {
                        GUATEMALA_SUPERMARKETS.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    storeName = s.name
                                    storeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (ej. 2x1, maduro, etc.)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    name.trim(),
                                    quantity.coerceAtLeast(0.1),
                                    unit,
                                    brand.trim(),
                                    priceStr.toDoubleOrNull() ?: 0.0,
                                    storeName,
                                    notes.trim(),
                                    ""
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPantryItemDialog(
    item: SmartCartItemEntity?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: Double, unit: String, status: String, price: Double, store: String, notes: String, brand: String) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var quantity by remember { mutableDoubleStateOf(item?.quantity ?: 1.0) }
    var unit by remember { mutableStateOf(item?.unit ?: "Unidad") }
    var brand by remember { mutableStateOf(item?.brand ?: "") }
    var status by remember { mutableStateOf(item?.getPantryStatus() ?: "PANTRY_VERDE") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }

    var unitDropdownExpanded by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (item == null) "Registrar en Mi Despensa" else "Editar en Despensa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del producto *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString(),
                        onValueChange = { quantity = it.toDoubleOrNull() ?: 1.0 },
                        label = { Text("Cantidad actual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            UNIT_OPTIONS.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        unit = opt
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Estado de existencia:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isDialogDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val statusOptions = listOf(
                        Triple(
                            "PANTRY_VERDE",
                            "Verde (Suficiente)",
                            if (isDialogDark) Pair(Color(0xFF064E3B), Color(0xFFA7F3D0)) else Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
                        ),
                        Triple(
                            "PANTRY_AMARILLO",
                            "Amarillo (Poco)",
                            if (isDialogDark) Pair(Color(0xFF451A03), Color(0xFFFDE68A)) else Pair(Color(0xFFFEF3C7), Color(0xFF92400E))
                        ),
                        Triple(
                            "PANTRY_ROJO",
                            "Rojo (Agotado)",
                            if (isDialogDark) Pair(Color(0xFF5C1D24), Color(0xFFFECACA)) else Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
                        )
                    )

                    statusOptions.forEach { (code, label, palette) ->
                        val isSel = status == code
                        val (selBg, selFg) = palette
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) selBg else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) selFg else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { status = code }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) selFg else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marca (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas de almacenamiento (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    name.trim(),
                                    quantity.coerceAtLeast(0.1),
                                    unit,
                                    status,
                                    0.0,
                                    "General",
                                    notes.trim(),
                                    brand.trim()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Guardar en Despensa", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetBudgetDialog(
    currentBudget: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetStr by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toString() else "") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Presupuesto para el Supermercado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Define cuánto dinero tienes previsto gastar en esta compra para alertarte de excesos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Presupuesto ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newB = budgetStr.toDoubleOrNull() ?: 0.0
                            onSave(newB)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutConfirmationDialog(
    cartItems: List<SmartCartItemEntity>,
    totalAmount: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (storeName: String, amount: Double, updatePantry: Boolean) -> Unit
) {
    var storeName by remember { mutableStateOf("Walmart") }
    var amountStr by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", totalAmount)) }
    var updatePantry by remember { mutableStateOf(true) }
    var storeDropdownExpanded by remember { mutableStateOf(false) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Confirmar Compra Real",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Esta acción registrará el gasto real en «Alimentación & Súper» y actualizará las existencias de Mi Despensa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(
                    expanded = storeDropdownExpanded,
                    onExpandedChange = { storeDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tienda donde se compró") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storeDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = storeDropdownExpanded,
                        onDismissRequest = { storeDropdownExpanded = false }
                    ) {
                        GUATEMALA_SUPERMARKETS.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    storeName = s.name
                                    storeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Total pagado en caja ($currencySymbol) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CONTROL DE ACTUALIZACIÓN DE DESPENSA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { updatePantry = !updatePantry }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = updatePantry,
                        onCheckedChange = { updatePantry = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Actualizar productos en Mi Despensa",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ingresarán como disponibles (VERDE) sin duplicar existencias.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalAmt = amountStr.toDoubleOrNull() ?: totalAmount
                            if (finalAmt > 0) {
                                onConfirm(storeName, finalAmt, updatePantry)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Confirmar y Registrar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveTemplateDialog(
    itemCount: Int,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Guardar Lista como Plantilla",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Se guardarán los $itemCount productos de tu lista de compras para reutilizarlos cuando desees.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la plantilla * (ej. Compra quincenal)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o notas (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), description.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                }
            }
        }
    }
}
