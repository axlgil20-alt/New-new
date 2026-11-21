package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.data.model.FinancialAccountEntity
import com.example.data.model.MoneyMinorUnits
import com.example.data.service.AccountBalanceSnapshot
import com.example.data.service.CreditCardStatementSummary
import com.example.ui.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class AccountCardVisualStyle(
    val gradient: List<Color>,
    val borderColor: Color,
    val badgeBg: Color,
    val badgeFg: Color,
    val accentColor: Color,
    val typeTag: String
)

private fun styleForAccountType(accountType: String): AccountCardVisualStyle = when (accountType) {
    AccountType.CASH -> AccountCardVisualStyle(
        gradient = listOf(Color(0xFF064E3B), Color(0xFF047857)),
        borderColor = Color(0xFF34D399),
        badgeBg = Color(0xFFD1FAE5),
        badgeFg = Color(0xFF064E3B),
        accentColor = Color(0xFFA7F3D0),
        typeTag = "EFECTIVO"
    )
    AccountType.BANK_CHECKING -> AccountCardVisualStyle(
        gradient = listOf(Color(0xFF172554), Color(0xFF1E40AF)),
        borderColor = Color(0xFF60A5FA),
        badgeBg = Color(0xFFDBEAFE),
        badgeFg = Color(0xFF1E3A8A),
        accentColor = Color(0xFFBFDBFE),
        typeTag = "BANCO · MONETARIA"
    )
    AccountType.BANK_SAVINGS -> AccountCardVisualStyle(
        gradient = listOf(Color(0xFF0F3D4C), Color(0xFF0E7490)),
        borderColor = Color(0xFF22D3EE),
        badgeBg = Color(0xFFCFFAFE),
        badgeFg = Color(0xFF164E63),
        accentColor = Color(0xFFA5F3FC),
        typeTag = "BANCO · AHORRO"
    )
    AccountType.COOPERATIVE -> AccountCardVisualStyle(
        gradient = listOf(Color(0xFF5C2D0C), Color(0xFF92400E)),
        borderColor = Color(0xFFFBBF24),
        badgeBg = Color(0xFFFEF3C7),
        badgeFg = Color(0xFF78350F),
        accentColor = Color(0xFFFDE68A),
        typeTag = "COOPERATIVA"
    )
    AccountType.CREDIT_CARD -> AccountCardVisualStyle(
        gradient = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0B2E26)),
        borderColor = Color(0xFFF59E0B),
        badgeBg = Color(0xFFF59E0B),
        badgeFg = Color(0xFF0F172A),
        accentColor = Color(0xFFFCD34D),
        typeTag = "TARJETA DE CRÉDITO"
    )
    else -> AccountCardVisualStyle(
        gradient = listOf(Color(0xFF1E293B), Color(0xFF334155)),
        borderColor = Color(0xFF94A3B8),
        badgeBg = Color(0xFFE2E8F0),
        badgeFg = Color(0xFF0F172A),
        accentColor = Color(0xFFCBD5E1),
        typeTag = "CUENTA"
    )
}

private data class QuickCardAction(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * Pila interactiva de tarjetas de cuentas (Card Stack) con navegación independiente por gesto
 * sobre el área del stack y acciones rápidas contextuales por swipe sobre la tarjeta individual.
 */
@Composable
fun AccountCardStack(
    accounts: List<FinancialAccountEntity>,
    selectedAccountId: Long?,
    snapshotsById: Map<Long, AccountBalanceSnapshot>,
    statementsById: Map<Long, CreditCardStatementSummary>,
    canTransfer: Boolean,
    canPayCard: Boolean,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    onSelectAccount: (Long) -> Unit,
    onViewDetail: (FinancialAccountEntity) -> Unit,
    onEditAccount: (FinancialAccountEntity) -> Unit,
    onRequestTransfer: (FinancialAccountEntity) -> Unit,
    onRequestPayCard: (FinancialAccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) return

    val density = LocalDensity.current
    val stackThresholdPx = with(density) { 52.dp.toPx() }

    val currentIndex = remember(accounts, selectedAccountId) {
        val idx = accounts.indexOfFirst { it.id == selectedAccountId }
        if (idx >= 0) idx else 0
    }
    val frontAccount = accounts[currentIndex]
    var navigationDirection by remember { mutableIntStateOf(1) }

    val goToPrevious = {
        if (accounts.size > 1) {
            navigationDirection = -1
            val prevIdx = (currentIndex - 1 + accounts.size) % accounts.size
            onSelectAccount(accounts[prevIdx].id)
        }
    }
    val goToNext = {
        if (accounts.size > 1) {
            navigationDirection = 1
            val nextIdx = (currentIndex + 1) % accounts.size
            onSelectAccount(accounts[nextIdx].id)
        }
    }

    val barSurface = if (isDarkTheme) Color(0xFF0F172A) else Color.White
    val barBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val barText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val barSubText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_card_stack"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Zona A: Barra dedicada al control y swipe del stack (separada del swipe individual de la tarjeta)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = barSurface,
            border = BorderStroke(1.dp, barBorder),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(accounts, currentIndex) {
                    if (accounts.size <= 1) return@pointerInput
                    var dragTotal = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragCancel = { dragTotal = 0f },
                        onDragEnd = {
                            if (dragTotal <= -stackThresholdPx) {
                                goToNext()
                            } else if (dragTotal >= stackThresholdPx) {
                                goToPrevious()
                            }
                            dragTotal = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    Text(
                        text = if (accounts.size > 1) {
                            "Desliza aquí o toca una tarjeta inferior (${currentIndex + 1} de ${accounts.size})"
                        } else {
                            "Cuenta activa (1 de 1)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = barText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Desliza la tarjeta hacia la izquierda para acciones rápidas",
                        style = MaterialTheme.typography.labelSmall,
                        color = barSubText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (accounts.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = goToPrevious,
                            modifier = Modifier.size(48.dp).testTag("stack_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Cuenta anterior",
                                tint = barText
                            )
                        }
                        IconButton(
                            onClick = goToNext,
                            modifier = Modifier.size(48.dp).testTag("stack_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Siguiente cuenta",
                                tint = barText
                            )
                        }
                    }
                }
            }
        }

        // Pila escalonada de tarjetas: tarjeta frontal + hasta 2 tarjetas posteriores asomándose abajo
        val peekOffsets = when {
            accounts.size >= 3 -> listOf(2, 1)
            accounts.size == 2 -> listOf(1)
            else -> emptyList()
        }
        val extraBottomSpace = when (peekOffsets.size) {
            2 -> 48.dp
            1 -> 26.dp
            else -> 0.dp
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = extraBottomSpace)
                .then(
                    if (accounts.size > 1) {
                        Modifier.pointerInput(accounts, currentIndex) {
                            var deckDrag = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { deckDrag = 0f },
                                onDragCancel = { deckDrag = 0f },
                                onDragEnd = {
                                    if (deckDrag <= -stackThresholdPx) {
                                        goToNext()
                                    } else if (deckDrag >= stackThresholdPx) {
                                        goToPrevious()
                                    }
                                    deckDrag = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    deckDrag += dragAmount
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            // Tarjetas posteriores asomándose con profundidad y click para traer al frente
            peekOffsets.forEach { layerDepth ->
                val backAccount = accounts[(currentIndex + layerDepth) % accounts.size]
                val backStyle = styleForAccountType(backAccount.accountType)
                val yShift = if (layerDepth == 1) 24.dp else 44.dp
                val scale = if (layerDepth == 1) 0.95f else 0.90f
                val alphaLayer = if (layerDepth == 1) 0.90f else 0.76f

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, backStyle.borderColor.copy(alpha = 0.75f)),
                    shadowElevation = if (layerDepth == 1) 3.dp else 1.dp,
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .offset(y = yShift)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = alphaLayer
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            navigationDirection = 1
                            onSelectAccount(backAccount.id)
                        }
                        .testTag("stack_back_card_${backAccount.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(backStyle.gradient))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${backAccount.name} · ${backStyle.typeTag}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Tocar para abrir",
                                style = MaterialTheme.typography.labelSmall,
                                color = backStyle.accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Tarjeta frontal animada con swipe individual para revelar acciones rápidas
            AnimatedContent(
                targetState = frontAccount.id,
                transitionSpec = {
                    val dir = navigationDirection
                    (slideInHorizontally(tween(240)) { width -> dir * (width / 3) } + fadeIn(tween(200)))
                        .togetherWith(
                            slideOutHorizontally(tween(220)) { width -> -dir * (width / 3) } + fadeOut(tween(180))
                        )
                },
                label = "account_stack_front_card"
            ) { targetAccountId ->
                val activeAcc = accounts.firstOrNull { it.id == targetAccountId } ?: frontAccount
                SwipeableAccountCard(
                    account = activeAcc,
                    snapshot = snapshotsById[activeAcc.id],
                    statement = statementsById[activeAcc.id],
                    isSelected = activeAcc.id == selectedAccountId,
                    canTransfer = canTransfer && !activeAcc.isArchived && AccountType.isLiquidAsset(activeAcc.accountType),
                    canPayCard = canPayCard && !activeAcc.isArchived && AccountType.isCreditCard(activeAcc.accountType),
                    currencySymbol = currencySymbol,
                    isDiscreetMode = isDiscreetMode,
                    onCardTap = { onSelectAccount(activeAcc.id) },
                    onViewDetail = { onViewDetail(activeAcc) },
                    onEditAccount = { onEditAccount(activeAcc) },
                    onRequestTransfer = { onRequestTransfer(activeAcc) },
                    onRequestPayCard = { onRequestPayCard(activeAcc) },
                    cardTestTag = "stack_front_card_${activeAcc.id}"
                )
            }
        }

        // Indicadores de posición del stack
        if (accounts.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                accounts.forEachIndexed { index, acc ->
                    val isActive = index == currentIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (isActive) 22.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFF10B981)
                                else if (isDarkTheme) Color(0xFF475569)
                                else Color(0xFF94A3B8)
                            )
                            .clickable {
                                navigationDirection = if (index >= currentIndex) 1 else -1
                                onSelectAccount(acc.id)
                            }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta financiera individual diferenciada por tipo de cuenta con gesto horizontal propio
 * (swipe izquierda) y botón accesible para revelar acciones rápidas contextuales sin ejecutar
 * operaciones monetarias automáticas.
 */
@Composable
fun SwipeableAccountCard(
    account: FinancialAccountEntity,
    snapshot: AccountBalanceSnapshot?,
    statement: CreditCardStatementSummary?,
    isSelected: Boolean,
    canTransfer: Boolean,
    canPayCard: Boolean,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onCardTap: () -> Unit,
    onViewDetail: () -> Unit,
    onEditAccount: () -> Unit,
    onRequestTransfer: () -> Unit,
    onRequestPayCard: () -> Unit,
    cardTestTag: String = "account_card_${account.id}",
    modifier: Modifier = Modifier
) {
    val style = remember(account.accountType) { styleForAccountType(account.accountType) }
    val isCard = AccountType.isCreditCard(account.accountType)
    val sdf = remember { SimpleDateFormat("dd MMM", Locale("es", "GT")) }

    val quickActions = remember(account.id, account.accountType, account.isArchived, canTransfer, canPayCard) {
        buildList {
            add(
                QuickCardAction(
                    label = "Detalle",
                    icon = Icons.Default.Visibility,
                    containerColor = Color(0xFF065F46),
                    contentColor = Color.White,
                    testTag = "swipe_action_detail_${account.id}",
                    onClick = onViewDetail
                )
            )
            add(
                QuickCardAction(
                    label = "Editar",
                    icon = Icons.Default.Edit,
                    containerColor = Color(0xFF1E3A8A),
                    contentColor = Color.White,
                    testTag = "swipe_action_edit_${account.id}",
                    onClick = onEditAccount
                )
            )
            if (!account.isArchived && AccountType.isLiquidAsset(account.accountType) && canTransfer) {
                add(
                    QuickCardAction(
                        label = "Transferir",
                        icon = Icons.Default.SwapHoriz,
                        containerColor = Color(0xFF0F766E),
                        contentColor = Color.White,
                        testTag = "swipe_action_transfer_${account.id}",
                        onClick = onRequestTransfer
                    )
                )
            }
            if (!account.isArchived && isCard && canPayCard) {
                add(
                    QuickCardAction(
                        label = "Pagar",
                        icon = Icons.Default.Payments,
                        containerColor = Color(0xFFB45309),
                        contentColor = Color.White,
                        testTag = "swipe_action_pay_${account.id}",
                        onClick = onRequestPayCard
                    )
                )
            }
        }
    }

    val density = LocalDensity.current
    val trayWidthDp = (quickActions.size * 64 + 44).dp
    val trayWidthPx = with(density) { trayWidthDp.toPx() }
    val snapOpenThresholdPx = trayWidthPx * 0.32f

    var isActionsOpen by remember(account.id) { mutableStateOf(false) }
    var isDragging by remember(account.id) { mutableStateOf(false) }
    var dragOffsetPx by remember(account.id) { mutableFloatStateOf(0f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = when {
            isDragging -> dragOffsetPx
            isActionsOpen -> -trayWidthPx
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
        label = "card_swipe_reveal_offset"
    )

    val headlineMinor = if (isCard) {
        snapshot?.totalDebtMinor ?: account.openingBalanceMinor
    } else {
        snapshot?.currentBalanceMinor ?: account.openingBalanceMinor
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(186.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Bandeja de acciones rápidas revelable (detrás de la cara frontal de la tarjeta)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0B1320).copy(alpha = 0.94f),
            border = BorderStroke(1.dp, style.borderColor.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickActions.forEach { action ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = action.containerColor,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(58.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                isActionsOpen = false
                                action.onClick()
                            }
                            .testTag(action.testTag)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = action.contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = action.contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { isActionsOpen = false },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar acciones",
                        tint = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        // Cara frontal de la tarjeta con gesto horizontal consumido exclusivamente por ella
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = if (isSelected) 2.dp else 1.2.dp,
                color = if (isSelected) style.borderColor else style.borderColor.copy(alpha = 0.7f)
            ),
            shadowElevation = 6.dp,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(account.id, trayWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragOffsetPx = if (isActionsOpen) -trayWidthPx else 0f
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDragEnd = {
                            isDragging = false
                            isActionsOpen = if (!isActionsOpen) {
                                dragOffsetPx <= -snapOpenThresholdPx
                            } else {
                                dragOffsetPx < -(trayWidthPx - snapOpenThresholdPx)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(-trayWidthPx, 0f)
                        }
                    )
                }
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    if (isActionsOpen) {
                        isActionsOpen = false
                    } else {
                        onCardTap()
                    }
                }
                .testTag(cardTestTag)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(style.gradient))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Fila superior: insignia de tipo + banco/4 dígitos + botón accesible de acciones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = style.badgeBg
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = AccountUiMeta.iconForType(account.accountType),
                                        contentDescription = null,
                                        tint = style.badgeFg,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = style.typeTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = style.badgeFg,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (account.lastFourDigits.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•••• ${account.lastFourDigits}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = style.accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            } else if (isCard) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = style.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { isActionsOpen = !isActionsOpen },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("card_actions_toggle_${account.id}")
                        ) {
                            Icon(
                                imageVector = if (isActionsOpen) Icons.Default.Close else Icons.Default.MoreHoriz,
                                contentDescription = if (isActionsOpen) "Cerrar acciones" else "Acciones de cuenta",
                                tint = Color.White
                            )
                        }
                    }

                    // Nombre e institución
                    Column {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = listOfNotNull(
                            account.institutionName.takeIf { it.isNotBlank() },
                            if (account.isDefault) "Principal" else null,
                            if (account.isArchived) "Archivada" else null
                        ).joinToString(" · ")
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Fila inferior: saldo/deuda + información secundaria contextual
                    if (isCard) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "Deuda total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(headlineMinor), currencySymbol, isDiscreetMode),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (headlineMinor > 0L) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Disponible",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(
                                            MoneyMinorUnits.toDisplayDouble(snapshot?.availableCreditMinor ?: account.creditLimitMinor),
                                            currencySymbol,
                                            isDiscreetMode
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = style.accentColor
                                    )
                                }
                            }
                            val utilRatio = ((snapshot?.utilizationPercent ?: 0.0).toFloat() / 100f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { utilRatio },
                                color = if (utilRatio >= 0.85f) Color(0xFFF87171) else style.borderColor,
                                trackColor = Color.White.copy(alpha = 0.20f),
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                            )
                            val cycleText = buildString {
                                account.cutoffDay?.let { append("Corte día $it") }
                                account.paymentDueDay?.let {
                                    if (isNotEmpty()) append(" · ")
                                    append("Pago día $it")
                                }
                                statement?.let {
                                    if (isNotEmpty()) append(" · ")
                                    append("Vence ${sdf.format(Date(it.cycleWindow.paymentDueDateMillis))}")
                                }
                            }.ifBlank { "Tarjeta de crédito activa" }
                            Text(
                                text = cycleText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF1F5F9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Saldo disponible",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(MoneyMinorUnits.toDisplayDouble(headlineMinor), currencySymbol, isDiscreetMode),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (!account.includeInTotalLiquidity) "Excluida de liquidez" else "Incluida en liquidez",
                                style = MaterialTheme.typography.labelSmall,
                                color = style.accentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
