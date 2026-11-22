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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MoneyMinorUnits
import com.example.data.model.PaymentCommitmentEntity
import com.example.data.model.PaymentFrequency
import com.example.data.model.PaymentStatus
import com.example.data.model.isCancelled
import com.example.data.model.isDueToday
import com.example.data.model.isMonetarySynchronized
import com.example.data.model.isOverdue
import com.example.data.model.isUpcoming
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.JadeGreen
import com.example.ui.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class CommitmentPriorityGroup(val rank: Int) {
    OVERDUE(0),
    DUE_TODAY_OR_UPCOMING(1),
    LATER_PENDING(2),
    PAID(3),
    CANCELLED(4)
}

data class JadeDeckSummary(
    val totalCount: Int,
    val pendingCount: Int,
    val overdueCount: Int,
    val upcomingCount: Int,
    val paidCount: Int,
    val cancelledCount: Int,
    val isEmpty: Boolean,
    val isCaughtUp: Boolean
) {
    val relevantCount: Int get() = totalCount - cancelledCount
}

enum class PostponePreset(val label: String, val subtitle: String) {
    PLUS_1_DAY("+1 día", "Mañana"),
    PLUS_3_DAYS("+3 días", "En 3 días"),
    PLUS_1_WEEK("+1 semana", "En 7 días"),
    NEXT_QUINCENA("Próx. quincena", "Día 15 o fin de mes")
}

object PaymentCommitmentStackLogic {

    fun classifyPriorityGroup(
        commitment: PaymentCommitmentEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): CommitmentPriorityGroup {
        if (commitment.isCancelled) {
            return CommitmentPriorityGroup.CANCELLED
        }
        if (commitment.isPaid || commitment.status == PaymentStatus.PAID) {
            return CommitmentPriorityGroup.PAID
        }
        if (commitment.isOverdue(nowMillis)) {
            return CommitmentPriorityGroup.OVERDUE
        }
        if (commitment.isDueToday(nowMillis) || commitment.isUpcoming(nowMillis, days = 7)) {
            return CommitmentPriorityGroup.DUE_TODAY_OR_UPCOMING
        }
        return CommitmentPriorityGroup.LATER_PENDING
    }

    /**
     * Orden de prioridad para el Mazo de Jade:
     * 1. Vencidos, del más antiguo al más reciente.
     * 2. Próximos (hoy y próximos 7 días), priorizando el vencimiento más cercano.
     * 3. Pendientes posteriores en orden cronológico ascendente.
     * 4. Pagados al final (más recientes primero).
     * 5. Cancelados después de pagados (no cuentan como pagados ni pendientes).
     */
    fun sortCommitmentsForStack(
        commitments: List<PaymentCommitmentEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<PaymentCommitmentEntity> {
        return commitments
            .distinctBy { it.id }
            .sortedWith { a, b ->
                val groupA = classifyPriorityGroup(a, nowMillis)
                val groupB = classifyPriorityGroup(b, nowMillis)
                if (groupA.rank != groupB.rank) {
                    return@sortedWith groupA.rank.compareTo(groupB.rank)
                }
                when (groupA) {
                    CommitmentPriorityGroup.PAID -> {
                        val dateA = a.paidDateMillis ?: a.dueDateMillis
                        val dateB = b.paidDateMillis ?: b.dueDateMillis
                        val cmp = dateB.compareTo(dateA)
                        if (cmp != 0) cmp else a.id.compareTo(b.id)
                    }
                    CommitmentPriorityGroup.CANCELLED -> {
                        val cmp = b.dueDateMillis.compareTo(a.dueDateMillis)
                        if (cmp != 0) cmp else a.id.compareTo(b.id)
                    }
                    else -> {
                        val cmp = a.dueDateMillis.compareTo(b.dueDateMillis)
                        if (cmp != 0) cmp else a.id.compareTo(b.id)
                    }
                }
            }
    }

    fun resolveSelectedCommitmentId(
        commitments: List<PaymentCommitmentEntity>,
        currentSelectedId: Long?
    ): Long? {
        if (commitments.isEmpty()) return null
        if (currentSelectedId != null && commitments.any { it.id == currentSelectedId }) {
            return currentSelectedId
        }
        return commitments.first().id
    }

    fun canPayCommitment(commitment: PaymentCommitmentEntity): Boolean {
        return !commitment.isCancelled &&
            !commitment.isPaid &&
            commitment.status != PaymentStatus.PAID
    }

    fun countUpcomingOrOverdue(
        commitments: List<PaymentCommitmentEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        return commitments.count {
            val group = classifyPriorityGroup(it, nowMillis)
            group == CommitmentPriorityGroup.OVERDUE || group == CommitmentPriorityGroup.DUE_TODAY_OR_UPCOMING
        }
    }

    /**
     * Evalúa explícitamente el estado real del Mazo de Jade sin confundir cancelados o vacío con pagos saldados.
     * Solo está "al día" cuando existen compromisos relevantes no cancelados y todos están efectivamente pagados.
     */
    fun summarizeDeck(
        commitments: List<PaymentCommitmentEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): JadeDeckSummary {
        val distinct = commitments.distinctBy { it.id }
        if (distinct.isEmpty()) {
            return JadeDeckSummary(
                totalCount = 0,
                pendingCount = 0,
                overdueCount = 0,
                upcomingCount = 0,
                paidCount = 0,
                cancelledCount = 0,
                isEmpty = true,
                isCaughtUp = false
            )
        }

        var overdueCount = 0
        var upcomingCount = 0
        var laterPendingCount = 0
        var paidCount = 0
        var cancelledCount = 0

        for (item in distinct) {
            when (classifyPriorityGroup(item, nowMillis)) {
                CommitmentPriorityGroup.OVERDUE -> overdueCount++
                CommitmentPriorityGroup.DUE_TODAY_OR_UPCOMING -> upcomingCount++
                CommitmentPriorityGroup.LATER_PENDING -> laterPendingCount++
                CommitmentPriorityGroup.PAID -> paidCount++
                CommitmentPriorityGroup.CANCELLED -> cancelledCount++
            }
        }

        val pendingCount = overdueCount + upcomingCount + laterPendingCount
        val isCaughtUp = pendingCount == 0 && overdueCount == 0 && paidCount > 0

        return JadeDeckSummary(
            totalCount = distinct.size,
            pendingCount = pendingCount,
            overdueCount = overdueCount,
            upcomingCount = upcomingCount,
            paidCount = paidCount,
            cancelledCount = cancelledCount,
            isEmpty = false,
            isCaughtUp = isCaughtUp
        )
    }

    fun computePostponedDueDate(
        currentDueDateMillis: Long,
        preset: PostponePreset,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val todayStartCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val baseMillis = maxOf(currentDueDateMillis, nowMillis)
        val cal = Calendar.getInstance().apply {
            timeInMillis = if (currentDueDateMillis < todayStartCal.timeInMillis) nowMillis else baseMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (preset) {
            PostponePreset.PLUS_1_DAY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            PostponePreset.PLUS_3_DAYS -> cal.add(Calendar.DAY_OF_YEAR, 3)
            PostponePreset.PLUS_1_WEEK -> cal.add(Calendar.DAY_OF_YEAR, 7)
            PostponePreset.NEXT_QUINCENA -> {
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (day < 15) {
                    cal.set(Calendar.DAY_OF_MONTH, 15)
                } else if (day < lastDay) {
                    cal.set(Calendar.DAY_OF_MONTH, lastDay)
                } else {
                    cal.add(Calendar.MONTH, 1)
                    cal.set(Calendar.DAY_OF_MONTH, 15)
                }
            }
        }
        return cal.timeInMillis
    }

    fun computeSwipeEndState(
        isCurrentlyOpen: Boolean,
        dragOffsetPx: Float,
        trayWidthPx: Float,
        thresholdFraction: Float = 0.32f
    ): Boolean {
        if (trayWidthPx <= 0f) return false
        val thresholdPx = trayWidthPx * thresholdFraction
        return if (!isCurrentlyOpen) {
            dragOffsetPx <= -thresholdPx
        } else {
            dragOffsetPx < -(trayWidthPx - thresholdPx)
        }
    }

    fun shouldCardHandleHorizontalDrag(
        isActionsOpen: Boolean,
        overSlopX: Float
    ): Boolean {
        return isActionsOpen || overSlopX < 0f
    }

    fun resolveStackSwipeDirection(
        dragTotalPx: Float,
        thresholdPx: Float
    ): Int {
        return when {
            dragTotalPx <= -thresholdPx -> 1
            dragTotalPx >= thresholdPx -> -1
            else -> 0
        }
    }

    fun displayAmountForCommitment(commitment: PaymentCommitmentEntity): Double {
        return if (commitment.isMonetarySynchronized) {
            MoneyMinorUnits.toDisplayDouble(commitment.amountMinor)
        } else {
            commitment.amount
        }
    }
}

private data class PaymentQuickAction(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

private data class CommitmentSemanticStyle(
    val statusBadgeText: String,
    val statusBadgeIcon: ImageVector,
    val statusContainerBg: Color,
    val statusContentColor: Color,
    val statusBorderColor: Color,
    val cardBorderColor: Color,
    val cardBg: Color,
    val backPeekBg: Color,
    val jadeBandColor: Color
)

@Composable
private fun rememberCommitmentSemanticStyle(
    commitment: PaymentCommitmentEntity,
    isDarkTheme: Boolean,
    nowMillis: Long
): CommitmentSemanticStyle {
    val isCancelled = commitment.isCancelled
    val isPaid = !isCancelled && (commitment.isPaid || commitment.status == PaymentStatus.PAID)
    val isOverdue = !isCancelled && commitment.isOverdue(nowMillis)
    val isToday = !isCancelled && !isPaid && commitment.isDueToday(nowMillis)
    val isUpcoming = !isCancelled && commitment.isUpcoming(nowMillis, 7)

    val statusContainerBg = when {
        isCancelled -> if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        isPaid -> if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
        isOverdue -> if (isDarkTheme) Color(0xFF5C1D24) else Color(0xFFFEE2E2)
        isToday -> if (isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7)
        isUpcoming -> if (isDarkTheme) Color(0xFF0D3B34) else Color(0xFFCCFBF1)
        else -> if (isDarkTheme) Color(0xFF14362C) else Color(0xFFE6F4EF)
    }
    val statusContentColor = when {
        isCancelled -> if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155)
        isPaid -> if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)
        isOverdue -> if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B)
        isToday -> if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
        isUpcoming -> if (isDarkTheme) Color(0xFF99F6E4) else Color(0xFF115E59)
        else -> if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF064E3B)
    }
    val statusBorderColor = when {
        isCancelled -> if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
        isPaid -> if (isDarkTheme) Color(0xFF34D399) else JadeGreen
        isOverdue -> if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
        isToday -> if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        isUpcoming -> if (isDarkTheme) Color(0xFF2DD4BF) else Color(0xFF0F766E)
        else -> if (isDarkTheme) Color(0xFF10B981) else EmeraldGreen
    }
    val cardBorderColor = when {
        isCancelled -> if (isDarkTheme) Color(0xFF475569) else Color(0xFF94A3B8)
        isOverdue -> if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
        isToday -> if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        isPaid -> if (isDarkTheme) Color(0xFF34D399) else JadeGreen
        isUpcoming -> if (isDarkTheme) Color(0xFF2DD4BF) else Color(0xFF0F766E)
        else -> if (isDarkTheme) Color(0xFF10B981) else EmeraldGreen
    }
    // 100% opaco: paneles Mazo de Jade sólidos para proteger cifras sobre Parque Central
    val cardBg = when {
        isCancelled -> if (isDarkTheme) Color(0xFF111827) else Color(0xFFF8FAFC)
        isPaid -> if (isDarkTheme) Color(0xFF0B241D) else Color(0xFFECFDF5)
        isOverdue -> if (isDarkTheme) Color(0xFF1C181B) else Color(0xFFFFFBF9)
        else -> if (isDarkTheme) Color(0xFF0E2620) else Color(0xFFF7FCFA)
    }
    val backPeekBg = when {
        isCancelled -> if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
        isOverdue -> if (isDarkTheme) Color(0xFF2B1418) else Color(0xFFFEE2E2)
        isToday -> if (isDarkTheme) Color(0xFF2D200B) else Color(0xFFFEF3C7)
        isUpcoming -> if (isDarkTheme) Color(0xFF093029) else Color(0xFFCCFBF1)
        isPaid -> if (isDarkTheme) Color(0xFF062019) else Color(0xFFD1FAE5)
        else -> if (isDarkTheme) Color(0xFF07231C) else Color(0xFFE2F5EE)
    }
    val jadeBandColor = when {
        isCancelled -> if (isDarkTheme) Color(0xFF475569) else Color(0xFF64748B)
        isOverdue -> if (isDarkTheme) Color(0xFF991B1B) else Color(0xFFB91C1C)
        isToday -> if (isDarkTheme) Color(0xFFB45309) else Color(0xFFD97706)
        isUpcoming -> Color(0xFF0F766E)
        else -> EmeraldGreen
    }

    val statusBadgeText = when {
        isCancelled -> "CANCELADO"
        isPaid -> "PAGADO"
        isOverdue -> "VENCIDO"
        isToday -> "VENCE HOY"
        isUpcoming -> "PRÓXIMO"
        else -> "PENDIENTE"
    }
    val statusBadgeIcon = when {
        isCancelled -> Icons.Default.Close
        isPaid -> Icons.Default.CheckCircle
        isOverdue -> Icons.Default.ErrorOutline
        isToday -> Icons.Default.Warning
        isUpcoming -> Icons.Default.HourglassTop
        else -> Icons.Default.Schedule
    }

    return CommitmentSemanticStyle(
        statusBadgeText = statusBadgeText,
        statusBadgeIcon = statusBadgeIcon,
        statusContainerBg = statusContainerBg,
        statusContentColor = statusContentColor,
        statusBorderColor = statusBorderColor,
        cardBorderColor = cardBorderColor,
        cardBg = cardBg,
        backPeekBg = backPeekBg,
        jadeBandColor = jadeBandColor
    )
}

/**
 * Mazo de Jade: stack inteligente de compromisos de pago en quetzales.
 * - Paneles 100% opacos con identidad jade/esmeralda.
 * - Acciones limpias sin duplicación (cara frontal: Posponer / Pagar; swipe o menú: Detalle / Editar).
 * - Estado final claro cuando todos los compromisos visibles ya fueron saldados.
 */
@Composable
fun PaymentCommitmentStack(
    commitments: List<PaymentCommitmentEntity>,
    selectedCommitmentId: Long?,
    accountNameById: Map<Long, String>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    nowMillis: Long,
    resetActionsKey: Any,
    onSelectCommitment: (Long) -> Unit,
    onRequestPay: (PaymentCommitmentEntity) -> Unit,
    onRequestPostpone: (PaymentCommitmentEntity) -> Unit,
    onViewDetail: (PaymentCommitmentEntity) -> Unit,
    onEditCommitment: (PaymentCommitmentEntity) -> Unit,
    onAddNewPayment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (commitments.isEmpty()) return

    val density = LocalDensity.current
    val stackThresholdPx = with(density) { 52.dp.toPx() }

    val currentIndex = remember(commitments, selectedCommitmentId) {
        val idx = commitments.indexOfFirst { it.id == selectedCommitmentId }
        if (idx >= 0) idx else 0
    }
    val frontCommitment = commitments[currentIndex]
    var navigationDirection by remember { mutableIntStateOf(1) }

    val deckSummary = remember(commitments, nowMillis) {
        PaymentCommitmentStackLogic.summarizeDeck(commitments, nowMillis)
    }
    val overdueCount = deckSummary.overdueCount
    val upcomingCount = deckSummary.upcomingCount
    val pendingTotalCount = deckSummary.pendingCount
    val allCaughtUp = deckSummary.isCaughtUp

    val goToPrevious = {
        if (commitments.size > 1) {
            navigationDirection = -1
            val prevIdx = (currentIndex - 1 + commitments.size) % commitments.size
            onSelectCommitment(commitments[prevIdx].id)
        }
    }
    val goToNext = {
        if (commitments.size > 1) {
            navigationDirection = 1
            val nextIdx = (currentIndex + 1) % commitments.size
            onSelectCommitment(commitments[nextIdx].id)
        }
    }

    val barSurface = if (isDarkTheme) Color(0xFF0A241E) else Color(0xFFF0FAF6)
    val barBorder = if (isDarkTheme) Color(0xFF10B981) else EmeraldGreen
    val barText = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF022C22)
    val barSubText = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_commitment_stack"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Estado final del mazo únicamente cuando hay compromisos pagados y 0 pendientes/vencidos
        if (allCaughtUp) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) Color(0xFF063729) else Color(0xFFD1FAE5),
                border = BorderStroke(1.4.dp, if (isDarkTheme) Color(0xFF34D399) else JadeGreen),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("jade_deck_completed_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldGreen,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mazo de Jade al día",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF064E3B)
                        )
                        val caughtUpSubtitle = if (deckSummary.cancelledCount > 0) {
                            "${deckSummary.paidCount} compromiso(s) pagado(s) · ${deckSummary.cancelledCount} cancelado(s)."
                        } else {
                            "Todos los compromisos relevantes (${deckSummary.paidCount}) están pagados."
                        }
                        Text(
                            text = caughtUpSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                    }
                    OutlinedButton(
                        onClick = onAddNewPayment,
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF6EE7B7) else EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "+ Nuevo",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFECFDF5) else EmeraldGreen
                        )
                    }
                }
            }
        } else if (!deckSummary.isEmpty && pendingTotalCount == 0 && deckSummary.cancelledCount > 0) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.2.dp, if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("jade_deck_cancelled_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0xFF475569) else Color(0xFF64748B),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sin compromisos activos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                        )
                        Text(
                            text = "${deckSummary.cancelledCount} compromiso(s) cancelado(s) (no cuentan como pagados).",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                    OutlinedButton(
                        onClick = onAddNewPayment,
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF6EE7B7) else EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "+ Nuevo",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFECFDF5) else EmeraldGreen
                        )
                    }
                }
            }
        }

        // Barra de control del Mazo de Jade (100% opaca)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = barSurface,
            border = BorderStroke(1.2.dp, barBorder),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payment_stack_bar")
                .pointerInput(commitments, currentIndex) {
                    if (commitments.size <= 1) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragTotal = 0f
                        val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            change.consume()
                            dragTotal += overSlop
                        }
                        if (dragStart != null) {
                            val completed = horizontalDrag(dragStart.id) { change ->
                                val deltaX = change.positionChange().x
                                change.consume()
                                dragTotal += deltaX
                            }
                            if (completed) {
                                when (PaymentCommitmentStackLogic.resolveStackSwipeDirection(dragTotal, stackThresholdPx)) {
                                    1 -> goToNext()
                                    -1 -> goToPrevious()
                                }
                            }
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldGreen
                        ) {
                            Text(
                                text = "MAZO DE JADE",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (commitments.size > 1) {
                                "${currentIndex + 1} de ${commitments.size}"
                            } else {
                                "1 de 1"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = barText,
                            modifier = Modifier.testTag("payment_stack_position_label")
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val summaryLabel = buildString {
                        if (overdueCount > 0) append("$overdueCount vencido(s)")
                        if (upcomingCount > 0) {
                            if (isNotEmpty()) append(" · ")
                            append("$upcomingCount próximo(s)")
                        }
                        if (isEmpty() && pendingTotalCount > 0) {
                            append("$pendingTotalCount pendiente(s) · Desliza izquierda para detalle/editar")
                        }
                        if (isEmpty() && pendingTotalCount == 0 && deckSummary.isCaughtUp) {
                            append("${deckSummary.paidCount} pagado(s) · Al día")
                            if (deckSummary.cancelledCount > 0) {
                                append(" · ${deckSummary.cancelledCount} cancelado(s)")
                            }
                        }
                        if (isEmpty() && pendingTotalCount == 0 && !deckSummary.isCaughtUp) {
                            append("${deckSummary.cancelledCount} cancelado(s) · Sin pendientes activos")
                        }
                    }
                    Text(
                        text = summaryLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdueCount > 0) {
                            if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                        } else {
                            barSubText
                        },
                        fontWeight = if (overdueCount > 0) FontWeight.Bold else FontWeight.Medium
                    )
                }

                if (commitments.size > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = goToPrevious,
                            modifier = Modifier.size(48.dp).testTag("payment_stack_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Compromiso anterior",
                                tint = barText
                            )
                        }
                        IconButton(
                            onClick = goToNext,
                            modifier = Modifier.size(48.dp).testTag("payment_stack_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Siguiente compromiso",
                                tint = barText
                            )
                        }
                    }
                }
            }
        }

        // Stack escalonado con tarjetas posteriores 100% opacas (sin alpha fade)
        val peekOffsets = when {
            commitments.size >= 3 -> listOf(2, 1)
            commitments.size == 2 -> listOf(1)
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
                    if (commitments.size > 1) {
                        Modifier.pointerInput(commitments, currentIndex) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var deckDrag = 0f
                                val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                    change.consume()
                                    deckDrag += overSlop
                                }
                                if (dragStart != null) {
                                    val completed = horizontalDrag(dragStart.id) { change ->
                                        val deltaX = change.positionChange().x
                                        change.consume()
                                        deckDrag += deltaX
                                    }
                                    if (completed) {
                                        when (PaymentCommitmentStackLogic.resolveStackSwipeDirection(deckDrag, stackThresholdPx)) {
                                            1 -> goToNext()
                                            -1 -> goToPrevious()
                                        }
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            peekOffsets.forEach { layerDepth ->
                val backCommitment = commitments[(currentIndex + layerDepth) % commitments.size]
                val backStyle = rememberCommitmentSemanticStyle(backCommitment, isDarkTheme, nowMillis)
                val yShift = if (layerDepth == 1) 22.dp else 42.dp
                val scale = if (layerDepth == 1) 0.96f else 0.92f
                val backAmount = PaymentCommitmentStackLogic.displayAmountForCommitment(backCommitment)

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = backStyle.backPeekBg,
                    border = BorderStroke(1.2.dp, backStyle.cardBorderColor),
                    shadowElevation = if (layerDepth == 1) 3.dp else 1.dp,
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = yShift)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            navigationDirection = 1
                            onSelectCommitment(backCommitment.id)
                        }
                        .testTag("payment_stack_back_card_${backCommitment.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${backCommitment.beneficiaryName} · ${backCommitment.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = backStyle.statusContentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${backStyle.statusBadgeText} · ${FormatUtils.formatCurrency(backAmount, currencySymbol, isDiscreetMode)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = backStyle.statusContentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = frontCommitment.id,
                transitionSpec = {
                    val dir = navigationDirection
                    (slideInHorizontally(tween(240)) { width -> dir * (width / 3) } + fadeIn(tween(200)))
                        .togetherWith(
                            slideOutHorizontally(tween(220)) { width -> -dir * (width / 3) } + fadeOut(tween(180))
                        )
                },
                label = "payment_stack_front_card"
            ) { targetId ->
                val activeCommitment = commitments.firstOrNull { it.id == targetId } ?: frontCommitment
                SwipeablePaymentCard(
                    commitment = activeCommitment,
                    currencySymbol = currencySymbol,
                    isDiscreetMode = isDiscreetMode,
                    isDarkTheme = isDarkTheme,
                    nowMillis = nowMillis,
                    resetActionsKey = resetActionsKey,
                    creditCardAccountName = activeCommitment.creditCardAccountId?.let { accountNameById[it] },
                    paymentSourceAccountName = activeCommitment.paymentSourceAccountId?.let { accountNameById[it] },
                    onRequestPay = { onRequestPay(activeCommitment) },
                    onRequestPostpone = { onRequestPostpone(activeCommitment) },
                    onViewDetail = { onViewDetail(activeCommitment) },
                    onEdit = { onEditCommitment(activeCommitment) },
                    cardTestTag = "payment_stack_front_card_${activeCommitment.id}"
                )
            }
        }

        // Indicadores de posición
        if (commitments.size > 1) {
            val maxDots = 7
            if (commitments.size <= maxDots) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    commitments.forEachIndexed { index, item ->
                        val isActive = index == currentIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (isActive) 22.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive) EmeraldGreen
                                    else if (isDarkTheme) Color(0xFF1F4B3E)
                                    else Color(0xFF94D2BD)
                                )
                                .clickable {
                                    navigationDirection = if (index >= currentIndex) 1 else -1
                                    onSelectCommitment(item.id)
                                }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta frontal del Mazo de Jade (100% opaca, sin acciones duplicadas):
 * - Cara frontal: decisiones principales (Posponer / Pagar cuando está pendiente, o Pagado + Detalle si ya está saldado).
 * - Tocar tarjeta: abre Detalle completo.
 * - Deslizar a la izquierda o botón ⋯: revela utilidades secundarias (Detalle / Editar) sin duplicar Pagar/Posponer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwipeablePaymentCard(
    commitment: PaymentCommitmentEntity,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isDarkTheme: Boolean,
    nowMillis: Long,
    resetActionsKey: Any,
    creditCardAccountName: String? = null,
    paymentSourceAccountName: String? = null,
    onRequestPay: () -> Unit,
    onRequestPostpone: () -> Unit,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    cardTestTag: String = "payment_card_${commitment.id}",
    modifier: Modifier = Modifier
) {
    val style = rememberCommitmentSemanticStyle(commitment, isDarkTheme, nowMillis)
    val canPay = PaymentCommitmentStackLogic.canPayCommitment(commitment)
    val isCancelled = commitment.isCancelled
    val isPaid = !isCancelled && (commitment.isPaid || commitment.status == PaymentStatus.PAID)
    val dateFormatter = remember { SimpleDateFormat("dd 'de' MMM, yyyy", Locale("es", "GT")) }

    val primaryText = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentEmeraldText = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)

    // Bandeja contextual de utilidades secundarias (sin duplicar los botones Pagar / Posponer de la tarjeta)
    val quickActions = remember(commitment.id) {
        listOf(
            PaymentQuickAction(
                label = "Detalle",
                icon = Icons.Default.Visibility,
                containerColor = Color(0xFF065F46),
                contentColor = Color.White,
                testTag = "payment_swipe_detail_${commitment.id}",
                onClick = onViewDetail
            ),
            PaymentQuickAction(
                label = "Editar",
                icon = Icons.Default.Edit,
                containerColor = Color(0xFF0F766E),
                contentColor = Color.White,
                testTag = "payment_swipe_edit_${commitment.id}",
                onClick = onEdit
            )
        )
    }

    val density = LocalDensity.current
    val trayWidthDp = (quickActions.size * 68 + 56).dp
    val trayWidthPx = with(density) { trayWidthDp.toPx() }

    var isActionsOpen by remember(commitment.id, resetActionsKey) { mutableStateOf(false) }
    var isDragging by remember(commitment.id, resetActionsKey) { mutableStateOf(false) }
    var dragOffsetPx by remember(commitment.id, resetActionsKey) { mutableFloatStateOf(0f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = when {
            isDragging -> dragOffsetPx
            isActionsOpen -> -trayWidthPx
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
        label = "payment_card_swipe_offset"
    )

    val displayAmount = PaymentCommitmentStackLogic.displayAmountForCommitment(commitment)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(18.dp))
    ) {
        // Bandeja trasera opaca del Mazo de Jade
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDarkTheme) Color(0xFF041C16) else Color(0xFF063729),
            border = BorderStroke(1.2.dp, style.cardBorderColor),
            modifier = Modifier
                .matchParentSize()
                .testTag("payment_swipe_tray_${commitment.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickActions.forEach { action ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = action.containerColor,
                        border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(62.dp)
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
                                fontSize = 11.sp,
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
                    modifier = Modifier.size(48.dp).testTag("payment_swipe_close_${commitment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar opciones",
                        tint = Color(0xFFD1FAE5)
                    )
                }
            }
        }

        // Cara frontal 100% opaca
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = style.cardBg,
            border = BorderStroke(1.5.dp, style.cardBorderColor),
            shadowElevation = if (isPaid) 1.dp else 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(commitment.id, trayWidthPx, resetActionsKey) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var cardAccepted = false
                        val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            if (PaymentCommitmentStackLogic.shouldCardHandleHorizontalDrag(isActionsOpen, overSlop)) {
                                cardAccepted = true
                                change.consume()
                            }
                        }
                        if (cardAccepted && dragStart != null) {
                            isDragging = true
                            dragOffsetPx = if (isActionsOpen) -trayWidthPx else 0f
                            val completed = horizontalDrag(dragStart.id) { change ->
                                val dragAmount = change.positionChange().x
                                change.consume()
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(-trayWidthPx, 0f)
                            }
                            isDragging = false
                            if (completed) {
                                isActionsOpen = PaymentCommitmentStackLogic.computeSwipeEndState(
                                    isCurrentlyOpen = isActionsOpen,
                                    dragOffsetPx = dragOffsetPx,
                                    trayWidthPx = trayWidthPx
                                )
                            }
                        }
                    }
                }
                .clip(RoundedCornerShape(18.dp))
                .clickable {
                    if (isActionsOpen) {
                        isActionsOpen = false
                    } else {
                        onViewDetail()
                    }
                }
                .testTag(cardTestTag)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Franja superior de acento Jade / Semántico
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(style.jadeBandColor)
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    // Beneficiario + Insignia de estado + Menú contextual único (Detalle/Editar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = style.statusContainerBg,
                                border = BorderStroke(1.dp, style.statusBorderColor),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = style.statusBadgeIcon,
                                    contentDescription = null,
                                    tint = style.statusContentColor,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = commitment.beneficiaryName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = primaryText
                                )
                                Text(
                                    text = commitment.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = style.statusContainerBg,
                            border = BorderStroke(1.dp, style.statusBorderColor)
                        ) {
                            Text(
                                text = style.statusBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = style.statusContentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { isActionsOpen = !isActionsOpen },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("payment_card_actions_toggle_${commitment.id}")
                        ) {
                            Icon(
                                imageVector = if (isActionsOpen) Icons.Default.Close else Icons.Default.MoreHoriz,
                                contentDescription = if (isActionsOpen) "Cerrar opciones" else "Ver detalle o editar",
                                tint = secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Concepto
                    Text(
                        text = commitment.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )

                    if (commitment.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = commitment.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!creditCardAccountName.isNullOrBlank() || !paymentSourceAccountName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!creditCardAccountName.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB))
                                ) {
                                    Text(
                                        text = "Tarjeta: $creditCardAccountName",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!paymentSourceAccountName.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen)
                                ) {
                                    Text(
                                        text = "Desde: $paymentSourceAccountName",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF065F46),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vencimiento y Monto en Quetzales
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Vencimiento",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryText
                            )
                            Text(
                                text = dateFormatter.format(Date(commitment.dueDateMillis)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (commitment.isOverdue(nowMillis)) {
                                    if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                } else {
                                    primaryText
                                }
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (commitment.isVariableAmount) {
                                Text(
                                    text = "Monto Variable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = FormatUtils.formatCurrency(displayAmount, currencySymbol, isDiscreetMode),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = if (isPaid) accentEmeraldText else primaryText
                            )
                        }
                    }

                    if (commitment.frequency != PaymentFrequency.ONCE) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = accentEmeraldText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val freqLabel = when (commitment.frequency) {
                                PaymentFrequency.MONTHLY -> "Recurrente Mensual"
                                PaymentFrequency.BIWEEKLY -> "Recurrente Quincenal"
                                PaymentFrequency.WEEKLY -> "Recurrente Semanal"
                                else -> "Recurrente"
                            }
                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentEmeraldText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Acciones primarias únicas (sin duplicar Detalle/Editar que viven al tocar o deslizar)
                    if (canPay) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRequestPostpone,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isDarkTheme) Color(0xFF2B200C) else Color(0xFFFFFBEB),
                                    contentColor = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("stack_postpone_button_${commitment.id}")
                            ) {
                                Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Posponer", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onRequestPay,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("stack_pay_button_${commitment.id}")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pagar", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = style.statusContainerBg,
                            border = BorderStroke(1.dp, style.statusBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCancelled) Icons.Default.Close else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = style.statusContentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCancelled) "Compromiso cancelado" else "Pagado y registrado ✓",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = style.statusContentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
