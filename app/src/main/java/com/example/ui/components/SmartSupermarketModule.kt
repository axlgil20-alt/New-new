package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import com.example.ui.util.ListScannerHelper
import com.example.ui.util.ScannedGroceryItem
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SmartCartItemEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.FormatUtils

data class SupermarketChainInfo(
    val name: String,
    val homeUrl: String,
    val searchUrlPrefix: String,
    val badgeColor: Color
)

val GUATEMALA_SUPERMARKETS = listOf(
    SupermarketChainInfo("Walmart", "https://www.walmart.com.gt/", "https://www.walmart.com.gt/search?q=", Color(0xFF0071DC)),
    SupermarketChainInfo("Paiz", "https://www.paiz.com.gt/", "https://www.paiz.com.gt/search?q=", Color(0xFFE11932)),
    SupermarketChainInfo("La Torre", "https://www.supermercadolatorre.com/", "https://www.supermercadolatorre.com/?s=", Color(0xFFD32F2F)),
    SupermarketChainInfo("Maxi Despensa", "https://www.maxidespensa.com.gt/", "https://www.maxidespensa.com.gt/search?q=", Color(0xFFE65100)),
    SupermarketChainInfo("Súper del Barrio", "https://superdelbarrio.com.gt/", "https://superdelbarrio.com.gt/?s=", Color(0xFFD32F2F)),
    SupermarketChainInfo("General", "", "", Color(0xFF4B5563))
)

val UNIT_OPTIONS = listOf("Unidad", "Libra", "Kilo", "Paquete", "Litro", "Caja", "Docena", "Bolsa", "Lata")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSupermarketModule(
    cartItems: List<SmartCartItemEntity>,
    pantryItems: List<SmartCartItemEntity> = emptyList(),
    budgetAmount: Double,
    selectedStoreName: String,
    historicalTransactions: List<TransactionEntity>,
    currencySymbol: String = "Q",
    isDiscreetMode: Boolean = false,
    onDismiss: () -> Unit,
    onAddItem: (name: String, quantity: Double, unit: String, brand: String, price: Double, store: String, notes: String, url: String, source: String) -> Unit,
    onUpdateItem: (SmartCartItemEntity) -> Unit,
    onDeleteItem: (SmartCartItemEntity) -> Unit,
    onToggleChecked: (SmartCartItemEntity) -> Unit,
    onSetBudget: (Double) -> Unit,
    onSelectStore: (String) -> Unit,
    onClearChecked: () -> Unit,
    onCheckoutExpense: (title: String, amount: Double, store: String, purchasedNames: List<String>) -> Unit,
    onAddPantryItem: (name: String, quantity: Double, unit: String, status: String, price: Double, store: String, notes: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdatePantryItem: (SmartCartItemEntity) -> Unit = {},
    onTogglePantryStatus: (SmartCartItemEntity) -> Unit = {},
    onDeletePantryItem: (SmartCartItemEntity) -> Unit = {},
    onGenerateReplenishment: () -> Unit = {},
    onAddScannedItems: (items: List<ScannedGroceryItem>, destination: String, avoidDuplicates: Boolean) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    // Mapa de precios históricos de Room para autocompletado y cotización
    val historicalPriceMap = remember(historicalTransactions) {
        val map = mutableMapOf<String, Double>()
        historicalTransactions
            .filter { it.category.contains("Súper", ignoreCase = true) || it.category.contains("Alimentación", ignoreCase = true) }
            .sortedBy { it.dateMillis }
            .forEach { tx ->
                val lower = tx.title.trim().lowercase()
                if (tx.amount > 0) {
                    map[lower] = tx.amount
                }
            }
        map
    }

    // Pestaña activa: 0 = Lista de Compras, 1 = Mi Despensa
    var activeTab by remember { mutableIntStateOf(0) }
    var storeFilter by remember { mutableStateOf<String?> (null) }
    var pantryFilter by remember { mutableStateOf<String?> (null) }

    // Estados para escáner OCR y despensa
    var showScannerChoiceDialog by remember { mutableStateOf(false) }
    var isScanningOcr by remember { mutableStateOf(false) }
    var showScannedReviewDialog by remember { mutableStateOf(false) }
    val scannedItemsToReview = remember { mutableStateListOf<ScannedGroceryItem>() }

    var showAddEditPantryDialog by remember { mutableStateOf(false) }
    var pantryItemToEdit by remember { mutableStateOf<SmartCartItemEntity?>(null) }
    var showDeletePantryConfirm by remember { mutableStateOf<SmartCartItemEntity?>(null) }

    // Lanzador de Cámara nativa
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

    // Lanzador de Galería nativa (Photo Picker zero-permission)
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
            } else {
                Toast.makeText(context, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Estados para diálogos
    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<SmartCartItemEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmItem by remember { mutableStateOf<SmartCartItemEntity?>(null) }

    // Filtrar artículos si el usuario selecciona una tienda específica
    val displayedItems = remember(cartItems, storeFilter) {
        if (storeFilter == null || storeFilter == "Todas") {
            cartItems
        } else {
            cartItems.filter { it.storeName.equals(storeFilter, ignoreCase = true) }
        }
    }

    // Totales calculados en tiempo real
    val totalEstimated = remember(cartItems) {
        cartItems.sumOf { it.quantity * it.estimatedUnitPrice }
    }
    val inCartCheckedTotal = remember(cartItems) {
        cartItems.filter { it.isChecked }.sumOf { it.quantity * it.estimatedUnitPrice }
    }
    val missingPriceCount = remember(cartItems) {
        cartItems.count { it.estimatedUnitPrice <= 0.0 }
    }
    val checkedCount = remember(cartItems) {
        cartItems.count { it.isChecked }
    }
    val totalCount = cartItems.size

    val progress = if (totalEstimated > 0) (inCartCheckedTotal / totalEstimated).toFloat().coerceIn(0f, 1f) else 0f
    val budgetDifference = budgetAmount - totalEstimated

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.2.dp, EmeraldGreen.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("smart_cart_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. CABECERA: TÍTULO, IDENTIDAD & CERRAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (activeTab == 0) Icons.Default.ShoppingCart else Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart Cart Quetzi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (activeTab == 0) "Lista de Súper & Cotizador Chapín" else "Despensa Inteligente del Hogar",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón de Escanear Lista con IA/OCR
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showScannerChoiceDialog = true }
                                .testTag("open_scanner_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Escanear",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Escanear",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SELECTOR DE PESTAÑAS: LISTA DE COMPRAS vs MI DESPENSA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (activeTab == 0) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { activeTab = 0 }
                            .testTag("tab_shopping_cart")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lista Súper (${cartItems.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (activeTab == 0) FontWeight.Black else FontWeight.SemiBold,
                                color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (activeTab == 1) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { activeTab = 1 }
                            .testTag("tab_pantry")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Kitchen,
                                contentDescription = null,
                                tint = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mi Despensa (${pantryItems.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (activeTab == 1) FontWeight.Black else FontWeight.SemiBold,
                                color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeTab == 0) {

                // =============================================================
                // 2. SELECCIÓN DE SUPERMERCADOS DE GUATEMALA
                // =============================================================
                Text(
                    text = "Supermercado Seleccionado:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GUATEMALA_SUPERMARKETS.forEach { store ->
                        val isSelected = selectedStoreName.equals(store.name, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) store.badgeColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.2.dp, if (isSelected) store.badgeColor else Color.Gray.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 110.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onSelectStore(store.name)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    softWrap = true,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (store.homeUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(store.homeUrl))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Abrir ${store.name}",
                                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // =============================================================
                // 3. TARJETA DE PRESUPUESTO & CONTROL FINANCIERO
                // =============================================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Presupuesto Asignado",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (budgetAmount > 0) FormatUtils.formatMoney(currencySymbol, budgetAmount, isDiscreetMode) else "Sin límite fijado",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldGreen
                                )
                            }

                            OutlinedButton(
                                onClick = { showBudgetDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    text = if (budgetAmount > 0) "Modificar" else "Fijar Presupuesto",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progreso de compra en tienda
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Comprados: $checkedCount de $totalCount artículos",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "En Carrito: ${FormatUtils.formatMoney(currencySymbol, inCartCheckedTotal, isDiscreetMode)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = IncomeGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Estado de comparación Presupuesto vs Estimado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Estimado Lista:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = FormatUtils.formatMoney(currencySymbol, totalEstimated, isDiscreetMode),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (budgetAmount > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (budgetDifference >= 0) "Saldo Disponible:" else "Supera Presupuesto:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (budgetDifference >= 0) IncomeGreen else ExpenseRed
                                    )
                                    Text(
                                        text = FormatUtils.formatMoney(currencySymbol, kotlin.math.abs(budgetDifference), isDiscreetMode),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = if (budgetDifference >= 0) IncomeGreen else ExpenseRed
                                    )
                                }
                            }
                        }

                        if (missingPriceCount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$missingPriceCount producto(s) sin precio asignado (Total parcial)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // =============================================================
                // 4. ACCIONES PRINCIPALES: AÑADIR PRODUCTO & REGISTRAR GASTO
                // =============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            itemToEdit = null
                            showAddEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Añadir Producto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }

                    if (checkedCount > 0) {
                        Button(
                            onClick = { showCheckoutDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Registrar Gasto", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (checkedCount > 0) {
                        IconButton(
                            onClick = onClearChecked,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar comprados",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // =============================================================
                // 5. LISTA DE ARTÍCULOS EN ROOM
                // =============================================================
                if (displayedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tu lista de compras está vacía",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toca '+ Añadir Producto' para cotizar y planificar tu súper.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedItems, key = { it.id }) { item ->
                            SmartCartItemRow(
                                item = item,
                                currencySymbol = currencySymbol,
                                isDiscreetMode = isDiscreetMode,
                                onToggleChecked = { onToggleChecked(item) },
                                onEdit = {
                                    itemToEdit = item
                                    showAddEditDialog = true
                                },
                                onDelete = { showDeleteConfirmItem = item }
                            )
                        }
                    }
                }
                } else {
                    // =============================================================
                    // 6. VISTA: MI DESPENSA REUTILIZABLE (SEMÁFORO Y REPOSICIÓN)
                    // =============================================================
                    val greenCount = remember(pantryItems) { pantryItems.count { it.priceSource == "PANTRY_VERDE" } }
                    val yellowCount = remember(pantryItems) { pantryItems.count { it.priceSource == "PANTRY_AMARILLO" } }
                    val redCount = remember(pantryItems) { pantryItems.count { it.priceSource == "PANTRY_ROJO" } }
                    val replenishTotal = yellowCount + redCount

                    // Barra de Resumen Semáforo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Verde
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { pantryFilter = if (pantryFilter == "PANTRY_VERDE") null else "PANTRY_VERDE" }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "$greenCount", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = Color(0xFF10B981))
                                Text(text = "Suficiente", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
                            }
                        }

                        // Amarillo
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { pantryFilter = if (pantryFilter == "PANTRY_AMARILLO") null else "PANTRY_AMARILLO" }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "$yellowCount", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = Color(0xFFF59E0B))
                                Text(text = "Queda poco", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B))
                            }
                        }

                        // Rojo
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { pantryFilter = if (pantryFilter == "PANTRY_ROJO") null else "PANTRY_ROJO" }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "$redCount", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = Color(0xFFEF4444))
                                Text(text = "Agotado", style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Acciones de Despensa: Reposición y Añadir
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                pantryItemToEdit = null
                                showAddEditPantryDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("add_pantry_item_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Añadir a Despensa", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = onGenerateReplenishment,
                            enabled = replenishTotal > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (replenishTotal > 0) Color(0xFFD97706) else Color.LightGray,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("replenish_pantry_button")
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (replenishTotal > 0) "Reponer ($replenishTotal)" else "Despensa al Día",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lista de Despensa
                    val displayedPantry = remember(pantryItems, pantryFilter) {
                        if (pantryFilter == null) pantryItems else pantryItems.filter { it.priceSource == pantryFilter }
                    }

                    if (displayedPantry.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Kitchen, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (pantryFilter != null) "No hay productos con este estado" else "Tu despensa está vacía",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Agrega tus alimentos básicos para controlar qué falta.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedPantry, key = { it.id }) { pItem ->
                                PantryItemRow(
                                    item = pItem,
                                    currencySymbol = currencySymbol,
                                    onToggleStatus = { onTogglePantryStatus(pItem) },
                                    onEdit = {
                                        pantryItemToEdit = pItem
                                        showAddEditPantryDialog = true
                                    },
                                    onDelete = { showDeletePantryConfirm = pItem }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // DIÁLOGOS DE SOPORTE: AÑADIR/EDITAR, PRESUPUESTO, CHECKOUT, ELIMINAR
    // =========================================================================

    if (showAddEditDialog) {
        AddEditSmartCartItemDialog(
            item = itemToEdit,
            currencySymbol = currencySymbol,
            defaultStore = selectedStoreName,
            historicalPriceMap = historicalPriceMap,
            onDismiss = { showAddEditDialog = false },
            onSave = { name, qty, unit, brand, price, store, notes, url, src ->
                if (itemToEdit == null) {
                    onAddItem(name, qty, unit, brand, price, store, notes, url, src)
                } else {
                    onUpdateItem(
                        itemToEdit!!.copy(
                            name = name,
                            quantity = qty,
                            unit = unit,
                            brand = brand,
                            estimatedUnitPrice = price,
                            storeName = store,
                            notes = notes,
                            productUrl = url,
                            priceSource = src
                        )
                    )
                }
                showAddEditDialog = false
            }
        )
    }

    if (showBudgetDialog) {
        SetBudgetDialog(
            currentBudget = budgetAmount,
            currencySymbol = currencySymbol,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { newBudget ->
                onSetBudget(newBudget)
                showBudgetDialog = false
            }
        )
    }

    if (showCheckoutDialog) {
        CheckoutConfirmationDialog(
            totalAmount = inCartCheckedTotal,
            currencySymbol = currencySymbol,
            storeName = selectedStoreName,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { expenseTitle ->
                val purchasedNames = cartItems.filter { it.isChecked }.map { it.name }
                onCheckoutExpense(expenseTitle, inCartCheckedTotal, selectedStoreName, purchasedNames)
                showCheckoutDialog = false
                onDismiss()
            }
        )
    }

    if (showAddEditPantryDialog) {
        AddEditPantryItemDialog(
            item = pantryItemToEdit,
            currencySymbol = currencySymbol,
            onDismiss = { showAddEditPantryDialog = false },
            onSave = { name, qty, unit, status, price, store, notes ->
                if (pantryItemToEdit == null) {
                    onAddPantryItem(name, qty, unit, status, price, store, notes)
                } else {
                    onUpdatePantryItem(
                        pantryItemToEdit!!.copy(
                            name = name,
                            quantity = qty,
                            unit = unit,
                            priceSource = status,
                            estimatedUnitPrice = price,
                            storeName = store,
                            notes = notes
                        )
                    )
                }
                showAddEditPantryDialog = false
            }
        )
    }

    if (showScannerChoiceDialog) {
        ScannerSelectionDialog(
            onDismiss = { showScannerChoiceDialog = false },
            onTakePhoto = { cameraLauncher.launch(null) },
            onPickGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        )
    }

    if (showScannedReviewDialog) {
        ReviewScannedListDialog(
            initialItems = scannedItemsToReview,
            currencySymbol = currencySymbol,
            onDismiss = { showScannedReviewDialog = false },
            onConfirm = { items, dest, avoidDup ->
                onAddScannedItems(items, dest, avoidDup)
                showScannedReviewDialog = false
            }
        )
    }

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
                    Text(
                        text = "Quetzi está reconociendo productos...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeletePantryConfirm != null) {
        BasicAlertDialog(onDismissRequest = { showDeletePantryConfirm = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¿Retirar de la despensa?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¿Deseas eliminar '${showDeletePantryConfirm!!.name}' de tu despensa permanente?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { showDeletePantryConfirm = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onDeletePantryItem(showDeletePantryConfirm!!)
                                showDeletePantryConfirm = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("Eliminar", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmItem != null) {
        BasicAlertDialog(onDismissRequest = { showDeleteConfirmItem = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "¿Eliminar producto?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Se eliminará \"${showDeleteConfirmItem?.name}\" de tu lista.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showDeleteConfirmItem = null },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showDeleteConfirmItem?.let { onDeleteItem(it) }
                                showDeleteConfirmItem = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Eliminar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fila interactiva para cada producto del carrito con diseño limpio y accesible (mín 48dp).
 */
@Composable
fun SmartCartItemRow(
    item: SmartCartItemEntity,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onToggleChecked: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val itemSubtotal = item.quantity * item.estimatedUnitPrice
    val store = GUATEMALA_SUPERMARKETS.find { it.name.equals(item.storeName, ignoreCase = true) }
        ?: GUATEMALA_SUPERMARKETS.last()

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (item.isChecked) Color.Transparent else EmeraldGreen.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox grande táctil (48x48)
            IconButton(
                onClick = onToggleChecked,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = if (item.isChecked) "Comprado" else "Pendiente",
                    tint = if (item.isChecked) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Datos del producto
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleChecked() }
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.brand.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = item.brand,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = store.badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.storeName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = store.badgeColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.priceSource.isNotBlank()) {
                        Text(
                            text = " • ${item.priceSource.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Subtotal y precios
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (item.estimatedUnitPrice > 0) {
                    Text(
                        text = FormatUtils.formatMoney(currencySymbol, itemSubtotal, isDiscreetMode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (item.isChecked) IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@ ${FormatUtils.formatMoney(currencySymbol, item.estimatedUnitPrice, isDiscreetMode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Sin precio",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            // Enlace web para cotizar en tienda
            val searchUrl = when {
                item.productUrl.isNotBlank() -> item.productUrl
                store.searchUrlPrefix.isNotBlank() -> store.searchUrlPrefix + Uri.encode(item.name)
                else -> ""
            }

            if (searchUrl.isNotBlank()) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Cotizar en tienda",
                        tint = store.badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Editar
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Eliminar
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = ExpenseRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Diálogo para Añadir o Editar un producto en el Smart Cart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSmartCartItemDialog(
    item: SmartCartItemEntity?,
    currencySymbol: String,
    defaultStore: String,
    historicalPriceMap: Map<String, Double>,
    onDismiss: () -> Unit,
    onSave: (name: String, quantity: Double, unit: String, brand: String, price: Double, store: String, notes: String, url: String, source: String) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var quantityText by remember { mutableStateOf(item?.quantity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "1") }
    var unit by remember { mutableStateOf(item?.unit ?: "Unidad") }
    var brand by remember { mutableStateOf(item?.brand ?: "") }
    var priceText by remember { mutableStateOf(item?.estimatedUnitPrice?.let { if (it > 0) "%.2f".format(it) else "" } ?: "") }
    var selectedStore by remember { mutableStateOf(item?.storeName ?: defaultStore) }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var productUrl by remember { mutableStateOf(item?.productUrl ?: "") }
    var priceSource by remember { mutableStateOf(item?.priceSource ?: "MANUAL") }

    var unitMenuExpanded by remember { mutableStateOf(false) }

    // Autocompletado reactivo de precios históricos de Room
    fun onNameChange(newName: String) {
        name = newName
        val lower = newName.trim().lowercase()
        if (priceText.isBlank() && lower.isNotBlank()) {
            val matched = historicalPriceMap[lower]
                ?: historicalPriceMap.entries.find { lower.contains(it.key) || it.key.contains(lower) }?.value
            if (matched != null) {
                priceText = "%.2f".format(matched)
                priceSource = "HISTORIAL"
            }
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (item == null) "Añadir a Smart Cart" else "Editar Producto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )

                // Nombre
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it) },
                    label = { Text("Nombre del Producto *") },
                    placeholder = { Text("Ej: Frijoles Negros, Leche, Huevos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Cantidad y Unidad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitMenuExpanded,
                        onExpandedChange = { unitMenuExpanded = !unitMenuExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Presentación") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false }
                        ) {
                            UNIT_OPTIONS.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Precio manual y Marca
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = {
                            priceText = it
                            priceSource = "MANUAL"
                        },
                        label = { Text("Precio Unitario ($currencySymbol)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca (opcional)") },
                        placeholder = { Text("Ej: Ina, Lala") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Selección de tienda para el producto
                Text(
                    text = "Supermercado sugerido:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GUATEMALA_SUPERMARKETS.forEach { s ->
                        FilterChip(
                            selected = selectedStore.equals(s.name, ignoreCase = true),
                            onClick = { selectedStore = s.name },
                            label = {
                                Text(
                                    text = s.name,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = s.badgeColor,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        )
                    }
                }

                // Notas y Enlace opcional
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (ej: pasillo 4, preferir bajo en grasa)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Botones Cancelar y Guardar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val qty = quantityText.toDoubleOrNull() ?: 1.0
                                val prc = priceText.toDoubleOrNull() ?: 0.0
                                onSave(name, qty, unit, brand, prc, selectedStore, notes, productUrl, priceSource)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (item == null) "Añadir a Lista" else "Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Diálogo para fijar presupuesto Pre-Compra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetDialog(
    currentBudget: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var textValue by remember {
        mutableStateOf(if (currentBudget > 0) "%.2f".format(currentBudget) else "")
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Fijar Presupuesto de Compra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
                Text(
                    text = "Define cuánto planeas gastar en el supermercado para que Quetzi controle tu saldo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Presupuesto en $currencySymbol") },
                    placeholder = { Text("Ej: 350.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = textValue.toDoubleOrNull() ?: 0.0
                            onConfirm(amount)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Fijar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Diálogo para Confirmar Compra y Registrar Gasto en Room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutConfirmationDialog(
    totalAmount: Double,
    currencySymbol: String,
    storeName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("Súper $storeName (Smart Cart)") }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Registrar Compra en Presupuesto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
                Text(
                    text = "Se registrará un gasto de ${currencySymbol}${"%,.2f".format(totalAmount)} en la categoría 'Alimentación & Súper'.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descripción del Gasto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancelar")
                    }
                    var isSubmitting by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (!isSubmitting) {
                                isSubmitting = true
                                onConfirm(title)
                            }
                        },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Confirmar y Registrar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
