package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen

data class ChapinQuickOption(
    val title: String,
    val iconEmoji: String,
    val vectorIcon: ImageVector,
    val defaultAmount: Double,
    val category: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapinQuickFab(
    onSaveExpense: (title: String, amount: Double, category: String, notes: String) -> Unit,
    onAddFullTransaction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionForCustomAmount by remember { mutableStateOf<ChapinQuickOption?>(null) }
    var customAmountText by remember { mutableStateOf("") }

    val options = listOf(
        ChapinQuickOption("Tortillas", "🫓", Icons.Default.Restaurant, 5.0, "Alimentación & Súper", Color(0xFFF59E0B)),
        ChapinQuickOption("Pasaje / Bus", "🚌", Icons.Default.DirectionsBus, 5.0, "Transporte & Combustible", Color(0xFF3B82F6)),
        ChapinQuickOption("Tuc-Tuc", "🛺", Icons.Default.ElectricRickshaw, 10.0, "Transporte & Combustible", Color(0xFF10B981)),
        ChapinQuickOption("Recargas", "📱", Icons.Default.PhoneAndroid, 10.0, "Telefonía & Internet", Color(0xFF8B5CF6)),
        ChapinQuickOption("Agua / Antojitos", "🥤", Icons.Default.Fastfood, 10.0, "Antojitos & Gastos Hormiga", Color(0xFFEF4444))
    )

    Column(
        modifier = modifier.padding(bottom = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Menú flotante expandido
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1E7DD)),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .width(230.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = EmeraldGreen
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD1E7DD)
                        ) {
                            Text(
                                text = "Q",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Botón para Nuevo Registro Completo
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expanded = false
                                onAddFullTransaction()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
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
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Abrir diálogo para confirmar o ajustar monto
                                    customAmountText = opt.defaultAmount.toInt().toString()
                                    selectedOptionForCustomAmount = opt
                                    expanded = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(opt.color.copy(alpha = 0.15f)),
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
                                        fontWeight = FontWeight.SemiBold
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
                }
            }
        }

        // Botón FAB Principal
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = if (expanded) Color(0xFF334155) else EmeraldGreen,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.testTag("chapin_fab_button")
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "FAB Chapín",
                modifier = Modifier.size(26.dp)
            )
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
                                color = Color.Gray
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

                    // Atajos de incremento rápido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3.0, 5.0, 10.0, 15.0, 20.0).forEach { preset ->
                            OutlinedButton(
                                onClick = { customAmountText = preset.toInt().toString() },
                                modifier = Modifier.weight(1f),
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
                        OutlinedButton(onClick = { selectedOptionForCustomAmount = null }) {
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
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("Guardar Gasto")
                        }
                    }
                }
            }
        }
    }
}
