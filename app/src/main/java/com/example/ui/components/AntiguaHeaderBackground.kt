package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.IvoryWarm

/**
 * AntiguaHeaderBackground - Identidad Visual Chapina de Antigua Guatemala
 *
 * Muestra con orgullo la vista panorámica del Arco de Santa Catalina y el Volcán de Agua,
 * con colores vivos y cálidos, protegida por un degradado artístico y rematada con la
 * cenefa tradicional de las alfombras de aserrín de la Semana Santa antigüeña.
 */
@Composable
fun AntiguaHeaderBackground(
    height: Dp = 260.dp,
    alpha: Float = 0.90f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Fotografía panorámica auténtica de Antigua Guatemala
        Image(
            painter = painterResource(id = R.drawable.img_antigua_header),
            contentDescription = "Antigua Guatemala - Arco de Santa Catalina y Volcán de Agua",
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
        )

        // Degradado multicapa diseñado para proteger la legibilidad de controles y títulos
        // sin opacar el arco ni el volcán de agua
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC032E24), // Sombra verde jade en la barra superior para botones y estado
                            Color(0x33032E24), // Zona clara central para lucir el volcán y el arco
                            Color(0x88032E24), // Sombra intermedia para el título
                            IvoryWarm.copy(alpha = 0.85f), // Transición suave hacia el fondo de pantalla
                            IvoryWarm
                        )
                    )
                )
        )

        // Cenefa de alfombra tradicional en el borde inferior
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AlfombraCenefa(height = 6.dp)
        }
    }
}
