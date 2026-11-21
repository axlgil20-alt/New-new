package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import kotlin.math.roundToInt

data class ChapinQuickOption(
    val title: String,
    val iconEmoji: String,
    val vectorIcon: ImageVector,
    val defaultAmount: Double,
    val category: String,
    val color: Color
)

object ChapinFabLayoutLogic {
    fun clampFractions(xFraction: Float, yFraction: Float): Pair<Float, Float> {
        return xFraction.coerceIn(0f, 1f) to yFraction.coerceIn(0f, 1f)
    }

    fun computeClampedOffsetPx(
        xFraction: Float,
        yFraction: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        fabSizePx: Float,
        marginPx: Float,
        topSafePx: Float = 0f,
        bottomSafePx: Float = 0f
    ): Pair<Float, Float> {
        val minX = marginPx
        val maxX = (containerWidthPx - fabSizePx - marginPx).coerceAtLeast(minX)
        val minY = marginPx + topSafePx
        val maxY = (containerHeightPx - fabSizePx - marginPx - bottomSafePx).coerceAtLeast(minY)
        val (fx, fy) = clampFractions(xFraction, yFraction)
        val x = minX + (maxX - minX) * fx
        val y = minY + (maxY - minY) * fy
        return x.coerceIn(minX, maxX) to y.coerceIn(minY, maxY)
    }

    fun offsetPxToFractions(
        offsetXPx: Float,
        offsetYPx: Float,
        containerWidthPx: Float,
        containerHeightPx: Float,
        fabSizePx: Float,
        marginPx: Float,
        topSafePx: Float = 0f,
        bottomSafePx: Float = 0f
    ): Pair<Float, Float> {
        val minX = marginPx
        val maxX = (containerWidthPx - fabSizePx - marginPx).coerceAtLeast(minX)
        val minY = marginPx + topSafePx
        val maxY = (containerHeightPx - fabSizePx - marginPx - bottomSafePx).coerceAtLeast(minY)
        val xSpan = (maxX - minX).coerceAtLeast(1f)
        val ySpan = (maxY - minY).coerceAtLeast(1f)
        val fx = ((offsetXPx.coerceIn(minX, maxX) - minX) / xSpan).coerceIn(0f, 1f)
        val fy = ((offsetYPx.coerceIn(minY, maxY) - minY) / ySpan).coerceIn(0f, 1f)
        return fx to fy
    }

    fun shouldOpenMenuUpward(yFraction: Float): Boolean = yFraction >= 0.48f

    fun shouldAlignMenuEnd(xFraction: Float): Boolean = xFraction >= 0.50f
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapinQuickFab(
    onSaveExpense: (title: String, amount: Double, category: String, notes: String) -> Unit,
    onAddFullTransaction: () -> Unit = {},
    fabOffsetXFraction: Float = 1f,
    fabOffsetYFraction: Float = 1f,
    onPositionChanged: (Float, Float) -> Unit = { _, _ -> },
    onResetPosition: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionForCustomAmount by remember { mutableStateOf<ChapinQuickOption?>(null) }
    var customAmountText by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val menuBg = if (isDark) Color(0xFF12201C) else Color(0xFFFFFFFF)
    val menuBorder = if (isDark) Color(0xFF10B981) else Color(0xFFA7F3D0)
    val rowBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val rowText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val accentEmerald = if (isDark) Color(0xFF6EE7B7) else EmeraldGreen

    val options = remember {
        listOf(
            ChapinQuickOption("Tortillas", "🫓", Icons.Default.Restaurant, 5.0, "Alimentación & Súper", Color(0xFFF59E0B)),
            ChapinQuickOption("Pasaje / Bus", "🚌", Icons.Default.DirectionsBus, 5.0, "Transporte & Combustible", Color(0xFF3B82F6)),
            ChapinQuickOption("Tuc-Tuc", "🛺", Icons.Default.ElectricRickshaw, 10.0, "Transporte & Combustible", Color(0xFF10B981)),
            ChapinQuickOption("Recargas", "📱", Icons.Default.PhoneAndroid, 10.0, "Telefonía & Internet", Color(0xFF8B5CF6)),
            ChapinQuickOption("Agua / Antojitos", "🥤", Icons.Default.Fastfood, 10.0, "Antojitos & Gastos Hormiga", Color(0xFFEF4444))
        )
    }

    val density = LocalDensity.current
    val fabSizeDp = 56.dp
    val marginDp = 14.dp
    val topSafeDp = 64.dp
    val bottomSafeDp = 12.dp
    val menuWidthDp = 248.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("chapin_fab_draggable_container")
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val fabSizePx = with(density) { fabSizeDp.toPx() }
        val marginPx = with(density) { marginDp.toPx() }
        val topSafePx = with(density) { topSafeDp.toPx() }
        val bottomSafePx = with(density) { bottomSafeDp.toPx() }
        val menuWidthPx = with(density) { menuWidthDp.toPx() }

        var liveXFraction by remember { mutableFloatStateOf(fabOffsetXFraction.coerceIn(0f, 1f)) }
        var liveYFraction by remember { mutableFloatStateOf(fabOffsetYFraction.coerceIn(0f, 1f)) }

        LaunchedEffect(fabOffsetXFraction, fabOffsetYFraction) {
            liveXFraction = fabOffsetXFraction.coerceIn(0f, 1f)
            liveYFraction = fabOffsetYFraction.coerceIn(0f, 1f)
        }

        val (fabOffsetXPx, fabOffsetYPx) = ChapinFabLayoutLogic.computeClampedOffsetPx(
            xFraction = liveXFraction,
            yFraction = liveYFraction,
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
            fabSizePx = fabSizePx,
            marginPx = marginPx,
            topSafePx = topSafePx,
            bottomSafePx = bottomSafePx
        )

        val openUpward = ChapinFabLayoutLogic.shouldOpenMenuUpward(liveYFraction)
        val alignEnd = ChapinFabLayoutLogic.shouldAlignMenuEnd(liveXFraction)
        val isCustomPos = kotlin.math.abs(liveXFraction - 1f) > 0.04f || kotlin.math.abs(liveYFraction - 1f) > 0.04f

        // Menú flotante anclado dinámicamente para nunca salirse de la pantalla ni tapar el FAB
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = if (openUpward) Alignment.Bottom else Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = if (openUpward) Alignment.Bottom else Alignment.Top),
            modifier = Modifier.offset {
                val rawMenuX = if (alignEnd) {
                    fabOffsetXPx + fabSizePx - menuWidthPx
                } else {
                    fabOffsetXPx
                }
                val clampedMenuX = rawMenuX.coerceIn(marginPx, (containerWidthPx - menuWidthPx - marginPx).coerceAtLeast(marginPx))
                val gapPx = 10.dp.toPx()
                val menuAnchorY = if (openUpward) {
                    (fabOffsetYPx - gapPx - 360.dp.toPx()).coerceAtLeast(marginPx)
                } else {
                    (fabOffsetYPx + fabSizePx + gapPx).coerceAtMost((containerHeightPx - 360.dp.toPx() - marginPx).coerceAtLeast(marginPx))
                }
                IntOffset(clampedMenuX.roundToInt(), menuAnchorY.roundToInt())
            }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = menuBg,
                shadowElevation = 10.dp,
                border = BorderStroke(1.2.dp, menuBorder),
                modifier = Modifier
                    .width(menuWidthDp)
                    .testTag("chapin_fab_menu")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acciones Rápidas",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentEmerald
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isCustomPos) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            liveXFraction = 1f
                                            liveYFraction = 1f
                                            onResetPosition()
                                        }
                                        .testTag("chapin_fab_reset_pos_button")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RestartAlt,
                                            contentDescription = "Restablecer posición del botón rápido",
                                            tint = accentEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Centrar",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentEmerald
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = "Q",
                                    fontWeight = FontWeight.Bold,
                                    color = accentEmerald,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Botón para Nuevo Registro Completo (>= 48dp touch target)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                expanded = false
                                onAddFullTransaction()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nuevo Registro Completo",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "Gastos Frecuentes Chapines",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    options.forEach { opt ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = rowBg,
                            border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    customAmountText = opt.defaultAmount.toInt().toString()
                                    selectedOptionForCustomAmount = opt
                                    expanded = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(opt.color.copy(alpha = 0.20f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = opt.vectorIcon,
                                            contentDescription = opt.title,
                                            tint = opt.color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${opt.iconEmoji} ${opt.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = rowText
                                    )
                                }
                                Text(
                                    text = "Q${opt.defaultAmount.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = opt.color
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Arrastra el botón + para reubicarlo",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Botón FAB Principal Arrastrable (mantiene target >= 56dp y persiste posición al soltar)
        Box(
            modifier = Modifier
                .offset { IntOffset(fabOffsetXPx.roundToInt(), fabOffsetYPx.roundToInt()) }
                .pointerInput(containerWidthPx, containerHeightPx) {
                    var currentX = fabOffsetXPx
                    var currentY = fabOffsetYPx
                    detectDragGestures(
                        onDragStart = {
                            currentX = fabOffsetXPx
                            currentY = fabOffsetYPx
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentX += dragAmount.x
                            currentY += dragAmount.y
                            val (fx, fy) = ChapinFabLayoutLogic.offsetPxToFractions(
                                offsetXPx = currentX,
                                offsetYPx = currentY,
                                containerWidthPx = containerWidthPx,
                                containerHeightPx = containerHeightPx,
                                fabSizePx = fabSizePx,
                                marginPx = marginPx,
                                topSafePx = topSafePx,
                                bottomSafePx = bottomSafePx
                            )
                            liveXFraction = fx
                            liveYFraction = fy
                        },
                        onDragEnd = {
                            onPositionChanged(liveXFraction, liveYFraction)
                        },
                        onDragCancel = {
                            onPositionChanged(liveXFraction, liveYFraction)
                        }
                    )
                }
        ) {
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = if (expanded) Color(0xFF334155) else EmeraldGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(fabSizeDp)
                    .semantics {
                        contentDescription = "Botón de acciones rápidas Chapín. Toca para abrir o arrastra para mover."
                    }
                    .testTag("chapin_fab_button")
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (expanded) "Cerrar acciones rápidas" else "Abrir acciones rápidas",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // Diálogo interactivo para guardar el gasto real en la base de datos
    selectedOptionForCustomAmount?.let { opt ->
        BasicAlertDialog(onDismissRequest = { selectedOptionForCustomAmount = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(opt.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = opt.vectorIcon,
                                contentDescription = null,
                                tint = opt.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gasto Rápido: ${opt.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Categoría: ${opt.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it },
                        label = { Text("Monto en Quetzales") },
                        prefix = { Text("Q ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3.0, 5.0, 10.0, 15.0, 20.0).forEach { preset ->
                            OutlinedButton(
                                onClick = { customAmountText = preset.toInt().toString() },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text("Q${preset.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { selectedOptionForCustomAmount = null },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = customAmountText.toDoubleOrNull() ?: opt.defaultAmount
                                if (amt > 0) {
                                    onSaveExpense(
                                        opt.title,
                                        amt,
                                        opt.category,
                                        "Gasto rápido Chapín (${opt.title})"
                                    )
                                    selectedOptionForCustomAmount = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Guardar Gasto", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
