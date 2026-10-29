package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R

/**
 * Componente seguro para renderizar la ilustración de Quetzi.
 * Comprueba de forma no-composable si el recurso gráfico existe y puede instanciarse.
 * Si no está disponible o falla, renderiza un vector en Canvas puro con 60fps.
 */
@Composable
fun SafeQuetziAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showThumbsUp: Boolean = true
) {
    val context = LocalContext.current
    val hasDrawable = remember(context) {
        try {
            ContextCompat.getDrawable(context, R.drawable.quetzi_mascot) != null
        } catch (_: Throwable) {
            false
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (hasDrawable) {
            Image(
                painter = painterResource(id = R.drawable.quetzi_mascot),
                contentDescription = "Quetzi Mascota",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPureVectorQuetzi(size = this.size)
            }
        }
    }
}

/**
 * Componente seguro para la panorámica de Tikal y Volcanes de Guatemala.
 * Comprueba de forma segura si la imagen existe en drawables; de lo contrario,
 * dibuja la silueta del Gran Jaguar de Tikal y volcanes en Canvas puro.
 */
@Composable
fun SafeTikalVolcanoBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 0.25f
) {
    val context = LocalContext.current
    val drawableResId = remember(context) {
        when {
            try { ContextCompat.getDrawable(context, R.drawable.img_tikal_cinematic) != null } catch (_: Throwable) { false } -> R.drawable.img_tikal_cinematic
            try { ContextCompat.getDrawable(context, R.drawable.img_guatemala_tikal_volcanoes_1792576931245) != null } catch (_: Throwable) { false } -> R.drawable.img_guatemala_tikal_volcanoes_1792576931245
            else -> null
        }
    }

    Box(modifier = modifier) {
        if (drawableResId != null) {
            Image(
                painter = painterResource(id = drawableResId),
                contentDescription = "Tikal Gran Jaguar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPureVectorTikalSilhouettes(alpha = alpha)
            }
        }
    }
}

/**
 * Dibuja la silueta vectorial de la Pirámide de Tikal y volcanes en Canvas puro.
 */
private fun DrawScope.drawPureVectorTikalSilhouettes(alpha: Float) {
    val w = size.width
    val h = size.height

    // Fondo degradado cielo nocturno esmeralda
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF022C22).copy(alpha = alpha),
                Color(0xFF0F172A).copy(alpha = alpha),
                Color(0xFF064E3B).copy(alpha = alpha)
            )
        )
    )

    // Estrellas en el cielo
    val stars = listOf(
        Offset(w * 0.15f, h * 0.20f),
        Offset(w * 0.28f, h * 0.12f),
        Offset(w * 0.50f, h * 0.18f),
        Offset(w * 0.72f, h * 0.10f),
        Offset(w * 0.88f, h * 0.25f),
        Offset(w * 0.38f, h * 0.32f),
        Offset(w * 0.62f, h * 0.28f)
    )
    for (st in stars) {
        drawCircle(
            color = Color(0xFFFDE047).copy(alpha = alpha * 0.85f),
            radius = 2.5f,
            center = st
        )
    }

    // Volcanes al fondo (Volcán de Agua y de Fuego)
    val volcanoPath = Path().apply {
        moveTo(0f, h)
        lineTo(w * 0.20f, h * 0.45f)
        lineTo(w * 0.45f, h)
        close()
    }
    drawPath(
        path = volcanoPath,
        color = Color(0xFF042F2E).copy(alpha = alpha * 0.60f)
    )

    val volcanoPath2 = Path().apply {
        moveTo(w * 0.30f, h)
        lineTo(w * 0.65f, h * 0.38f)
        lineTo(w * 0.95f, h)
        close()
    }
    drawPath(
        path = volcanoPath2,
        color = Color(0xFF064E3B).copy(alpha = alpha * 0.70f)
    )

    // Pirámide de Templo I de Tikal (El Gran Jaguar) en primer plano
    val tikalBaseX = w * 0.45f
    val tikalBaseY = h
    val pyramidWidth = w * 0.40f
    val pyramidHeight = h * 0.55f

    val pyramidPath = Path().apply {
        // Base
        moveTo(tikalBaseX - pyramidWidth / 2f, tikalBaseY)
        // Escalones ascendentes
        val steps = 6
        for (i in 0 until steps) {
            val stepFraction = (i + 1).toFloat() / steps
            val currentW = pyramidWidth * (1f - stepFraction * 0.65f)
            val currentH = tikalBaseY - (pyramidHeight * stepFraction)
            lineTo(tikalBaseX - currentW / 2f, currentH)
            lineTo(tikalBaseX + currentW / 2f, currentH)
        }
        // Cresta superior de Tikal (Peineta maya)
        lineTo(tikalBaseX + pyramidWidth * 0.12f, tikalBaseY - pyramidHeight - 16f)
        lineTo(tikalBaseX - pyramidWidth * 0.12f, tikalBaseY - pyramidHeight - 16f)
        lineTo(tikalBaseX - pyramidWidth * 0.18f, tikalBaseY - pyramidHeight)
        lineTo(tikalBaseX - pyramidWidth / 2f, tikalBaseY)
        close()
    }
    drawPath(
        path = pyramidPath,
        color = Color(0xFF021C16).copy(alpha = alpha * 0.90f)
    )
}

/**
 * Dibuja un Quetzi vectorial estilizado en Canvas con alta resolución y cero retrasos.
 */
private fun DrawScope.drawPureVectorQuetzi(size: Size) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = size.minDimension / 2f * 0.85f

    // 1. Halo brillante
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF34D399).copy(alpha = 0.45f),
                Color(0xFFFDE047).copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius * 1.25f
        ),
        radius = radius * 1.25f,
        center = Offset(cx, cy)
    )

    // 2. Cresta de plumas esmeralda (arriba de la cabeza)
    val crestPath = Path().apply {
        moveTo(cx - radius * 0.3f, cy - radius * 0.6f)
        quadraticBezierTo(cx - radius * 0.1f, cy - radius * 1.15f, cx, cy - radius * 0.7f)
        quadraticBezierTo(cx + radius * 0.2f, cy - radius * 1.25f, cx + radius * 0.35f, cy - radius * 0.5f)
        close()
    }
    drawPath(path = crestPath, color = Color(0xFF10B981))

    // 3. Cabeza y cuerpo verde esmeralda
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF34D399), Color(0xFF059669)),
            startY = cy - radius,
            endY = cy + radius
        ),
        radius = radius * 0.75f,
        center = Offset(cx, cy)
    )

    // 4. Pechera roja carmesí brillante (plumaje del quetzal)
    val chestPath = Path().apply {
        moveTo(cx - radius * 0.45f, cy + radius * 0.1f)
        quadraticBezierTo(cx, cy + radius * 0.85f, cx + radius * 0.45f, cy + radius * 0.1f)
        close()
    }
    drawPath(path = chestPath, color = Color(0xFFEF4444))

    // 5. Ojos grandes cartoon con brillo
    val eyeY = cy - radius * 0.20f
    val leftEyeX = cx - radius * 0.25f
    val rightEyeX = cx + radius * 0.25f

    drawCircle(color = Color.White, radius = radius * 0.22f, center = Offset(leftEyeX, eyeY))
    drawCircle(color = Color.White, radius = radius * 0.22f, center = Offset(rightEyeX, eyeY))

    // Pupilas
    drawCircle(color = Color(0xFF0F172A), radius = radius * 0.12f, center = Offset(leftEyeX + 2f, eyeY))
    drawCircle(color = Color(0xFF0F172A), radius = radius * 0.12f, center = Offset(rightEyeX + 2f, eyeY))

    // Destello de ojo
    drawCircle(color = Color.White, radius = radius * 0.05f, center = Offset(leftEyeX + 4f, eyeY - 3f))
    drawCircle(color = Color.White, radius = radius * 0.05f, center = Offset(rightEyeX + 4f, eyeY - 3f))

    // 6. Pico amarillo dorado
    val beakPath = Path().apply {
        moveTo(cx - radius * 0.14f, cy + radius * 0.02f)
        lineTo(cx + radius * 0.14f, cy + radius * 0.02f)
        lineTo(cx, cy + radius * 0.32f)
        close()
    }
    drawPath(path = beakPath, color = Color(0xFFFBBF24))

    // 7. Lentes modernos (estilo mascota inteligente)
    drawCircle(
        color = Color(0xFF1E293B),
        radius = radius * 0.24f,
        center = Offset(leftEyeX, eyeY),
        style = Stroke(width = 3.5f)
    )
    drawCircle(
        color = Color(0xFF1E293B),
        radius = radius * 0.24f,
        center = Offset(rightEyeX, eyeY),
        style = Stroke(width = 3.5f)
    )
    drawLine(
        color = Color(0xFF1E293B),
        start = Offset(leftEyeX + radius * 0.24f, eyeY),
        end = Offset(rightEyeX - radius * 0.24f, eyeY),
        strokeWidth = 3.5f
    )
}
