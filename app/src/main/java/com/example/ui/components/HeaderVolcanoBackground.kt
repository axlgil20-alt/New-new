package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * HeaderVolcanoBackground - Identidad Visual Chapina Unificada (Branding de Volcanes y Tikal)
 *
 * Utiliza SafeTikalVolcanoBackground con capa de sombreado transparente en verde esmeralda
 * (#022C22 / #064E3B) que se funde con el fondo para garantizar alto contraste y estabilidad.
 */
@Composable
fun HeaderVolcanoBackground(
    height: Dp = 190.dp,
    alpha: Float = 0.25f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Fondo base de volcanes y Tikal
        SafeTikalVolcanoBackground(
            modifier = Modifier.fillMaxSize(),
            alpha = alpha
        )

        // Capa de sombreado transparente en verde esmeralda (#022C22 / #064E3B) y fundido con el fondo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF022C22).copy(alpha = 0.35f),
                            Color(0xFF064E3B).copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
    }
}
