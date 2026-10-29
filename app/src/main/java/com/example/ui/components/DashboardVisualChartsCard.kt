package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TentativePurple
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BiweeklyDataPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

@Composable
fun DashboardVisualChartsCard(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    isDiscreetMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) return

    // Agrupar en Quincena 1 (días 1-15) y Quincena 2 (días 16-fin)
    val cal = Calendar.getInstance()
    var q1Income = 0.0
    var q1Expense = 0.0
    var q2Income = 0.0
    var q2Expense = 0.0

    for (tx in transactions) {
        cal.timeInMillis = tx.dateMillis
        val day = cal.get(Calendar.DAY_OF_MONTH)
        if (day <= 15) {
            if (tx.type == TransactionType.INCOME) q1Income += tx.amount else q1Expense += tx.amount
        } else {
            if (tx.type == TransactionType.INCOME) q2Income += tx.amount else q2Expense += tx.amount
        }
    }

    val maxVal = maxOf(q1Income, q1Expense, q2Income, q2Expense, 100.0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Flujo de Efectivo Quincenal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Comparativa visual de Ingresos vs Gastos por cada quincena",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Leyenda de colores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(IncomeGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ingresos", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ExpenseRed))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gastos", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gráfico de Barras con Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    val w = size.width
                    val h = size.height - 25.dp.toPx()
                    val barWidth = 32.dp.toPx()

                    // Líneas guía de fondo
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFF1F5F9),
                        start = Offset(0f, h / 2),
                        end = Offset(w, h / 2),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Grupo 1: 1a Quincena (Día 15)
                    val q1CenterX = w * 0.28f
                    val q1IncHeight = ((q1Income / maxVal) * h).toFloat().coerceAtLeast(4f)
                    val q1ExpHeight = ((q1Expense / maxVal) * h).toFloat().coerceAtLeast(4f)

                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(q1CenterX - barWidth - 2.dp.toPx(), h - q1IncHeight),
                        size = Size(barWidth, q1IncHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(q1CenterX + 2.dp.toPx(), h - q1ExpHeight),
                        size = Size(barWidth, q1ExpHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Grupo 2: 2a Quincena (Fin de Mes)
                    val q2CenterX = w * 0.72f
                    val q2IncHeight = ((q2Income / maxVal) * h).toFloat().coerceAtLeast(4f)
                    val q2ExpHeight = ((q2Expense / maxVal) * h).toFloat().coerceAtLeast(4f)

                    drawRoundRect(
                        color = IncomeGreen,
                        topLeft = Offset(q2CenterX - barWidth - 2.dp.toPx(), h - q2IncHeight),
                        size = Size(barWidth, q2IncHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawRoundRect(
                        color = ExpenseRed,
                        topLeft = Offset(q2CenterX + 2.dp.toPx(), h - q2ExpHeight),
                        size = Size(barWidth, q2ExpHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                // Etiquetas de las quincenas debajo de las barras
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1ª Quincena (Día 15)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(if (isDiscreetMode) "Q ****.**" else "$currencySymbol${"%,.0f".format(q1Income - q1Expense)} neto", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("2ª Quincena (Fin de Mes)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(if (isDiscreetMode) "Q ****.**" else "$currencySymbol${"%,.0f".format(q2Income - q2Expense)} neto", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
