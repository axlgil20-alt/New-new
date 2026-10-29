package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIconForCategory(category: String): ImageVector {
        val lower = category.lowercase()
        return when {
            lower.contains("salario") || lower.contains("pago") || lower.contains("ingreso") -> Icons.Default.AccountBalanceWallet
            lower.contains("bono") || lower.contains("regalo") || lower.contains("tv") -> Icons.Default.CardGiftcard
            lower.contains("aguinaldo") -> Icons.Default.Star
            lower.contains("saldo anterior") -> Icons.Default.Savings
            lower.contains("extra") -> Icons.Default.TrendingUp
            lower.contains("esposa") || lower.contains("familia") || lower.contains("primo") -> Icons.Default.Favorite
            lower.contains("colegio") || lower.contains("inscripción") || lower.contains("seguro") || lower.contains("escolar") -> Icons.Default.School
            lower.contains("doctor") || lower.contains("consulta") || lower.contains("salud") -> Icons.Default.MedicalServices
            lower.contains("pasajes") || lower.contains("mototaxi") || lower.contains("transporte") -> Icons.Default.DirectionsBus
            lower.contains("electricidad") || lower.contains("luz") || lower.contains("servicio") -> Icons.Default.Bolt
            lower.contains("iglesia") || lower.contains("ofrenda") -> Icons.Default.Church
            lower.contains("chucherías") || lower.contains("hormiga") -> Icons.Default.BugReport
            lower.contains("comida") || lower.contains("restaurante") -> Icons.Default.Fastfood
            lower.contains("teléfono") || lower.contains("celular") -> Icons.Default.PhoneAndroid
            lower.contains("clausura") || lower.contains("fiesta") -> Icons.Default.Celebration
            lower.contains("tienda") || lower.contains("súper") || lower.contains("compra") || lower.contains("pantalón") || lower.contains("zapatos") -> Icons.Default.ShoppingCart
            lower.contains("septiembre") || lower.contains("octubre") || lower.contains("noviembre") || lower.contains("diciembre") -> Icons.Default.CalendarToday
            else -> Icons.Default.MoreHoriz
        }
    }
}
