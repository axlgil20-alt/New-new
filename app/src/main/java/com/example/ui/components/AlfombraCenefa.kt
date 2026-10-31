package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralRed
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.JadeGreen
import com.example.ui.theme.NazarenoPurple
import com.example.ui.theme.TurquoiseAccent

/**
 * AlfombraCenefa - Cenefa geométrica inspirada en las tradicionales alfombras de aserrín
 * de la Semana Santa de Antigua Guatemala.
 *
 * Dibuja un patrón rítmico de rombos y grecas escalonadas con la paleta ceremonial:
 * Verde Jade, Amarillo Dorado, Morado Nazareno, Coral y Turquesa.
 */
@Composable
fun AlfombraCenefa(
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val segmentWidth = 24.dp.toPx()
        val numSegments = (w / segmentWidth).toInt() + 2

        val colors = listOf(
            JadeGreen,
            GoldenYellow,
            NazarenoPurple,
            CoralRed,
            TurquoiseAccent
        )

        for (i in 0 until numSegments) {
            val startX = i * segmentWidth
            val color = colors[i % colors.size]
            val nextColor = colors[(i + 1) % colors.size]

            // Rombo central
            val diamondPath = Path().apply {
                moveTo(startX + segmentWidth / 2f, 0f)
                lineTo(startX + segmentWidth, h / 2f)
                lineTo(startX + segmentWidth / 2f, h)
                lineTo(startX, h / 2f)
                close()
            }
            drawPath(path = diamondPath, color = color)

            // Triángulo superior
            val topTriPath = Path().apply {
                moveTo(startX, 0f)
                lineTo(startX + segmentWidth / 2f, 0f)
                lineTo(startX + segmentWidth / 4f, h / 2f)
                close()
            }
            drawPath(path = topTriPath, color = nextColor)

            // Triángulo inferior
            val bottomTriPath = Path().apply {
                moveTo(startX + segmentWidth / 2f, h)
                lineTo(startX + segmentWidth, h)
                lineTo(startX + 3f * segmentWidth / 4f, h / 2f)
                close()
            }
            drawPath(path = bottomTriPath, color = nextColor)
        }
    }
}
