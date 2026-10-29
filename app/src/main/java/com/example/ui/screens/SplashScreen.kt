package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.SafeTikalVolcanoBackground
import com.example.ui.theme.EmeraldGreen
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Modelo de partícula de polvo de estrellas mágicas.
 */
private class Stardust(
    val id: Long,
    val spawnX: Float,
    val spawnY: Float,
    val vx: Float,
    val vy: Float,
    val baseRadius: Float,
    val color: Color,
    val spawnTimeMs: Long,
    val lifetimeMs: Long,
    val isStarSparkle: Boolean,
    val initialRotation: Float,
    val rotationSpeed: Float
)

/**
 * Punto histórico en la trayectoria de vuelo de Quetzi para dibujar la estela continua.
 */
private class WakePoint(
    val x: Float,
    val y: Float,
    val timeMs: Long,
    val thickness: Float
)

/**
 * Intro Cinematográfico de Quetzi - Clase Mundial
 *
 * Dirección Artística & Características:
 * 1. Background de Tikal Gran Jaguar:
 *    - Respeta fielmente la composición y perspectiva vertical de la referencia visual.
 *    - Fotografía de alta resolución con pirámide del Gran Jaguar, selva de Petén y cielo con nubes.
 *    - Tratamiento atmosférico: Bruma matutina, iluminación ámbar/dorada en la cresta,
 *      y viñeteado cinematográfico en verde esmeralda profundo (#022C22 / #064E3B).
 * 2. Quetzi - Protagonista:
 *    - Mascota original con billete, lentes y tenis, con fondo transparente limpio.
 *    - Vuelo orgánico continuo sin saltos: curva suave que nace sobre Tikal, desciende
 *      hacia el frente y asciende al centro heroico con banking (inclinación natural).
 *    - Flotación orgánica suave (hovering) al llegar a su posición final.
 * 3. Polvo de Estrellas y Estela de Cometa (Efecto Disney/Fantasía):
 *    - Emisor en tiempo real vinculado a las coordenadas vivas de la mascota.
 *    - Partículas persistentes que flotan en el aire y se desvanecen gradualmente con brillo alfa.
 *    - Chispas doradas (oro maya), verde esmeralda, menta neón y blanco estrella.
 *    - Estrellas resplandecientes de 4 puntas y orbes luminosos con halo radial.
 *    - Cinta luminosa (wake ribbon) continua que conecta la trayectoria reciente de vuelo.
 * 4. Identidad Verbal Exacta:
 *    - Nombre: Quetzi
 *    - Eslogan: «De chapines para chapines, tus finanzas sin clavos.»
 *    - Firma: MarvGr
 */
@Composable
fun SplashScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasNavigated by remember { mutableStateOf(false) }

    fun safeFinish() {
        if (!hasNavigated) {
            hasNavigated = true
            onContinue()
        }
    }

    // Controladores de transición globales
    val screenFadeAlpha = remember { Animatable(1f) }
    val bgZoom = remember { Animatable(1.05f) }
    val bgAtmosphereAlpha = remember { Animatable(0f) }

    // Controlador de revelación de marca
    val brandAlpha = remember { Animatable(0f) }
    val brandSlideY = remember { Animatable(24f) }
    val sloganAlpha = remember { Animatable(0f) }
    val signatureAlpha = remember { Animatable(0f) }
    val thumbsUpScale = remember { Animatable(0f) }

    // Estado reactivo de animación de trayectoria y partículas (60 FPS)
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var currentX by remember { mutableFloatStateOf(0f) }
    var currentY by remember { mutableFloatStateOf(0f) }
    var currentScale by remember { mutableFloatStateOf(0.35f) }
    var currentRotation by remember { mutableFloatStateOf(0f) }
    var currentAlpha by remember { mutableFloatStateOf(0f) }

    val particles = remember { mutableStateListOf<Stardust>() }
    val wakeTrail = remember { mutableStateListOf<WakePoint>() }

    // Cronograma maestro de animaciones de Compose
    LaunchedEffect(Unit) {
        // Revelación atmosférica inicial
        bgAtmosphereAlpha.animateTo(1f, tween(1000, easing = LinearEasing))
        bgZoom.animateTo(1.0f, tween(6000, easing = LinearEasing))
    }

    LaunchedEffect(Unit) {
        // Revelación coordinada de textos después del aterrizaje de Quetzi (3.4s)
        kotlinx.coroutines.delay(3400)
        thumbsUpScale.animateTo(1.0f, tween(350, easing = FastOutSlowInEasing))

        // Revelación de Quetzi (Marca)
        brandAlpha.animateTo(1.0f, tween(500, easing = FastOutSlowInEasing))
        brandSlideY.animateTo(0f, tween(500, easing = FastOutSlowInEasing))

        // Revelación del Eslogan
        kotlinx.coroutines.delay(200)
        sloganAlpha.animateTo(1.0f, tween(500, easing = FastOutSlowInEasing))

        // Revelación de la Firma
        kotlinx.coroutines.delay(250)
        signatureAlpha.animateTo(1.0f, tween(500, easing = FastOutSlowInEasing))

        // Tiempo para disfrutar el intro cinematográfico y transición final suave
        kotlinx.coroutines.delay(2000)
        screenFadeAlpha.animateTo(0f, tween(400, easing = LinearEasing))
        safeFinish()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(screenFadeAlpha.value)
            .background(Color(0xFF021B16))
            .testTag("splash_screen")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Toque directo para entrar de inmediato
                safeFinish()
            }
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }

        // Bucle de física cinemática y generación continua de polvo de estrellas
        LaunchedEffect(widthPx, heightPx) {
            var startNanos = 0L
            var particleIdCounter = 0L
            val random = Random(42)

            while (!hasNavigated) {
                withFrameNanos { frameNanos ->
                    if (startNanos == 0L) startNanos = frameNanos
                    val nowMs = (frameNanos - startNanos) / 1_000_000L
                    elapsedMs = nowMs

                    // 1. Evaluación de la trayectoria continua de Quetzi
                    // Fases temporales:
                    // 0ms - 700ms: Tikal y atmósfera despiertan, Quetzi se aproxima en el horizonte
                    // 700ms - 3200ms: Vuelo cinemático curvilíneo en S (2.5s)
                    // 3200ms+: Posado en centro heroico con flotación continua (hovering)

                    val flightDurationMs = 2500f
                    val flightStartMs = 700f

                    if (nowMs < flightStartMs) {
                        // Antes de entrar: posición en horizonte sobre Tikal
                        val introT = (nowMs / flightStartMs).coerceIn(0f, 1f)
                        currentX = widthPx * 0.72f
                        currentY = heightPx * 0.20f
                        currentScale = 0.30f
                        currentRotation = -4f
                        currentAlpha = introT
                    } else if (nowMs < flightStartMs + flightDurationMs) {
                        // Vuelo curvilíneo cinematográfico suave
                        val rawT = ((nowMs - flightStartMs) / flightDurationMs).coerceIn(0f, 1f)
                        val t = FastOutSlowInEasing.transform(rawT)

                        // Puntos de control de la curva Bézier cúbica de vuelo:
                        // P0: Sobre la cresta de Tikal (fondo derecho)
                        val p0x = widthPx * 0.72f
                        val p0y = heightPx * 0.20f
                        // P1: Descenso swooping a la izquierda hacia la copa de los árboles
                        val p1x = widthPx * 0.05f
                        val p1y = heightPx * 0.48f
                        // P2: Ascenso curvo hacia el frente derecho
                        val p2x = widthPx * 0.88f
                        val p2y = heightPx * 0.38f
                        // P3: Llegada al centro heroico
                        val p3x = widthPx * 0.50f
                        val p3y = heightPx * 0.32f

                        val u = 1f - t
                        val tt = t * t
                        val uu = u * u
                        val uuu = uu * u
                        val ttt = tt * t

                        val targetX = uuu * p0x + 3f * uu * t * p1x + 3f * u * tt * p2x + ttt * p3x
                        val targetY = uuu * p0y + 3f * uu * t * p1y + 3f * u * tt * p2y + ttt * p3y

                        // Tangente para cálculo dinámico del ángulo de inclinación (banking natural)
                        val dx = 3f * uu * (p1x - p0x) + 6f * u * t * (p2x - p1x) + 3f * tt * (p3x - p2x)
                        val dy = 3f * uu * (p1y - p0y) + 6f * u * t * (p2y - p1y) + 3f * tt * (p3y - p2y)
                        val motionAngleRad = atan2(dy, dx)
                        val rawTiltDeg = (motionAngleRad * 180f / PI.toFloat()).coerceIn(-18f, 16f)

                        currentX = targetX
                        currentY = targetY
                        // Escala: crece de 0.32x hasta 1.12x en el primer plano y se asienta en 1.0x
                        currentScale = (0.32f * (1f - t) + 1.10f * t).coerceAtMost(1.10f)
                        currentRotation = rawTiltDeg * 0.65f
                        currentAlpha = 1f
                    } else {
                        // Posado heroico: física de flotación orgánica (hovering continuo)
                        val hoverTime = (nowMs - (flightStartMs + flightDurationMs)) * 0.003f
                        val hoverOffsetX = sin(hoverTime) * 6f
                        val hoverOffsetY = sin(hoverTime * 2f) * 4.5f
                        val hoverTilt = cos(hoverTime) * 2.8f

                        currentX = widthPx * 0.50f + hoverOffsetX
                        currentY = heightPx * 0.32f + hoverOffsetY
                        currentScale = 1.0f
                        currentRotation = hoverTilt
                        currentAlpha = 1f
                    }

                    // 2. Punto emisor de polvo de estrellas (en los tenis y wake de Quetzi)
                    val quetziSizePx = 130f * currentScale
                    val emitX = currentX - (quetziSizePx * 0.15f)
                    val emitY = currentY + (quetziSizePx * 0.30f)

                    // Registrar punto en la estela continua
                    wakeTrail.add(
                        WakePoint(
                            x = emitX,
                            y = emitY,
                            timeMs = nowMs,
                            thickness = (quetziSizePx * 0.35f).coerceIn(12f, 44f)
                        )
                    )
                    // Purgar puntos de estela más antiguos de 450ms
                    wakeTrail.removeAll { nowMs - it.timeMs > 450L }

                    // 3. Emisión de partículas de polvo de estrellas y chispas
                    val isActivelyFlying = nowMs in flightStartMs.toLong()..(flightStartMs + flightDurationMs).toLong()
                    val spawnCount = if (isActivelyFlying) 3 else if (nowMs % 3L == 0L) 1 else 0

                    for (i in 0 until spawnCount) {
                        val angle = random.nextFloat() * 2f * PI.toFloat()
                        val speed = random.nextFloat() * (if (isActivelyFlying) 60f else 25f) + 15f
                        val vx = cos(angle) * speed - (if (isActivelyFlying) 40f else 0f)
                        val vy = sin(angle) * speed + 20f // Ligera caída por gravedad mágica

                        val palette = listOf(
                            Color(0xFFFDE047), // Oro maya brillante
                            Color(0xFFF59E0B), // Ámbar dorado
                            Color(0xFF34D399), // Menta esmeralda resplandeciente
                            Color(0xFF10B981), // Verde jade luminoso
                            Color(0xFFFFFFFF), // Destello blanco estrella
                            Color(0xFFA7F3D0)  // Jade claro
                        )
                        val color = palette[random.nextInt(palette.size)]
                        val isStar = random.nextFloat() < 0.38f // 38% estrellas de 4 puntas

                        particles.add(
                            Stardust(
                                id = particleIdCounter++,
                                spawnX = emitX + (random.nextFloat() - 0.5f) * 16f,
                                spawnY = emitY + (random.nextFloat() - 0.5f) * 16f,
                                vx = vx,
                                vy = vy,
                                baseRadius = random.nextFloat() * 4.5f + 2.5f,
                                color = color,
                                spawnTimeMs = nowMs,
                                lifetimeMs = random.nextLong(650L, 1350L),
                                isStarSparkle = isStar,
                                initialRotation = random.nextFloat() * 360f,
                                rotationSpeed = (random.nextFloat() - 0.5f) * 180f
                            )
                        )
                    }

                    // Purgar partículas extintas
                    particles.removeAll { nowMs - it.spawnTimeMs >= it.lifetimeMs }
                }
            }
        }

        // ====================================================================
        // CAPA 1: BACKGROUND CINEMATOGRÁFICO DE TIKAL (TEMPLO DEL GRAN JAGUAR)
        // ====================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(bgZoom.value)
        ) {
            SafeTikalVolcanoBackground(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.95f
            )

            // Iluminación ambiental y viñeteado cinematográfico en verde esmeralda profundo
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD9022C22), // Esmeralda bosque nocturno superior
                                Color(0x33022C22), // Apertura clara para el Templo del Gran Jaguar
                                Color(0x66022C22), // Sombra sobre la copa de la selva
                                Color(0xF2021A14)  // Base oscura rica para textos legibles
                            )
                        )
                    )
            )

            // Bruma matutina sutil sobre la selva maya
            Canvas(modifier = Modifier.fillMaxSize()) {
                val mistY = size.height * 0.44f
                val mistWidth = size.width
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF34D399).copy(alpha = 0.12f * bgAtmosphereAlpha.value),
                            Color(0xFFCBD5E1).copy(alpha = 0.10f * bgAtmosphereAlpha.value),
                            Color.Transparent
                        ),
                        startY = mistY - 60f,
                        endY = mistY + 120f
                    ),
                    topLeft = Offset(0f, mistY - 60f),
                    size = androidx.compose.ui.geometry.Size(mistWidth, 180f)
                )
            }
        }

        // ====================================================================
        // CAPA 2: POLVO DE ESTRELLAS Y ESTELA DE COMETA (RENDERIZADO CONTINUO)
        // ====================================================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nowMs = elapsedMs

            // 1. Cinta luminosa fluida de la estela reciente (Wake Ribbon)
            if (wakeTrail.size >= 2) {
                val path = Path()
                val first = wakeTrail.first()
                path.moveTo(first.x, first.y)
                for (i in 1 until wakeTrail.size) {
                    val pt = wakeTrail[i]
                    path.lineTo(pt.x, pt.y)
                }

                // Resplandor exterior suave
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = 0.0f),
                            Color(0xFF10B981).copy(alpha = 0.35f),
                            Color(0xFFFDE047).copy(alpha = 0.65f)
                        )
                    ),
                    style = Stroke(
                        width = 18f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Núcleo brillante interior
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF).copy(alpha = 0.0f),
                            Color(0xFFFDE047).copy(alpha = 0.60f),
                            Color(0xFFFFFFFF).copy(alpha = 0.90f)
                        )
                    ),
                    style = Stroke(
                        width = 5.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 2. Partículas individuales flotantes con decaimiento orgánico
            particles.forEach { p ->
                val ageMs = nowMs - p.spawnTimeMs
                val ageFraction = (ageMs.toFloat() / p.lifetimeMs).coerceIn(0f, 1f)

                // Curva de brillo tipo campana: rápida aparición, brillo máximo y suave desvanecimiento
                val alpha = (sin(ageFraction * PI.toFloat()) * 0.95f).coerceIn(0f, 1f)
                val dtSec = ageMs / 1000f
                val curX = p.spawnX + p.vx * dtSec
                val curY = p.spawnY + p.vy * dtSec

                if (p.isStarSparkle) {
                    // Estrella resplandeciente de 4 puntas de hada/Disney
                    val starSize = p.baseRadius * (1.2f + sin(ageFraction * PI.toFloat()) * 1.5f)
                    val starRot = p.initialRotation + p.rotationSpeed * dtSec
                    drawSparkleStar(
                        center = Offset(curX, curY),
                        radius = starSize,
                        color = p.color.copy(alpha = alpha),
                        rotationDegrees = starRot
                    )
                } else {
                    // Orbe de polvo mágico con halo radial suave
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                p.color.copy(alpha = alpha),
                                p.color.copy(alpha = alpha * 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(curX, curY),
                            radius = p.baseRadius * 2.2f
                        ),
                        radius = p.baseRadius * 2.2f,
                        center = Offset(curX, curY)
                    )
                    // Núcleo blanco brillante
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.85f),
                        radius = p.baseRadius * 0.55f,
                        center = Offset(curX, curY)
                    )
                }
            }
        }

        // ====================================================================
        // CAPA 3: QUETZI - EL PROTAGONISTA EN VUELO CONTINUO
        // ====================================================================
        val quetziBoxSizeDp = 130.dp
        val halfBoxPx = with(LocalDensity.current) { (quetziBoxSizeDp / 2).toPx() }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (currentX - halfBoxPx).roundToInt(),
                        y = (currentY - halfBoxPx).roundToInt()
                    )
                }
                .size(quetziBoxSizeDp)
                .scale(currentScale)
                .rotate(currentRotation)
                .alpha(currentAlpha),
            contentAlignment = Alignment.Center
        ) {
            // Mascota oficial Quetzi con recorte transparente perfecto
            Image(
                painter = painterResource(id = R.drawable.quetzi_mascot),
                contentDescription = "Quetzi",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Pulgar arriba oficial (👍) con resplandor dorado al posarse
            if (thumbsUpScale.value > 0.05f) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFDE047),
                    shadowElevation = 8.dp,
                    border = BorderStroke(2.dp, EmeraldGreen),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(38.dp)
                        .scale(thumbsUpScale.value)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "👍",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // ====================================================================
        // CAPA 4: IDENTIDAD VERBAL CINEMATOGRÁFICA
        // ====================================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TÍTULO OFICIAL: QUETZI
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(0, brandSlideY.value.roundToInt()) }
                    .alpha(brandAlpha.value)
                    .testTag("splash_title_quetzi")
            ) {
                // Aura dorada suave de fondo
                Text(
                    text = "QUETZI",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = Color(0xFFFDE047).copy(alpha = 0.25f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(1.02f)
                )

                // Texto principal en blanco puro de alto contraste
                Text(
                    text = "QUETZI",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. MENSAJE COMPLETO DE MARCA
            Text(
                text = "De guatemaltecos para guatemaltecos.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFDE047), // Dorado maya
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .alpha(sloganAlpha.value)
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Tus finanzas sin clavos.",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA7F3D0), // Menta esmeralda claro
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(sloganAlpha.value)
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cada quetzal cuenta. Cada familia merece herramientas para organizarse, ahorrar y vivir con menos preocupación.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier
                    .alpha(sloganAlpha.value)
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. FIRMA OFICIAL EXACTA: MarvGr
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.45f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .alpha(signatureAlpha.value)
                    .testTag("brand_marvgr_studios")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(text = "🇬🇹", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MarvGr",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFFFDE047) // Oro maya
                    )
                }
            }
        }
    }
}

/**
 * Dibuja un destello brillante tipo estrella de 4 puntas de hada/Disney.
 */
private fun DrawScope.drawSparkleStar(
    center: Offset,
    radius: Float,
    color: Color,
    rotationDegrees: Float
) {
    val rad = (rotationDegrees * PI.toFloat() / 180f)
    val cosR = cos(rad)
    val sinR = sin(rad)

    fun rotatePoint(x: Float, y: Float): Offset {
        return Offset(
            center.x + (x * cosR - y * sinR),
            center.y + (x * sinR + y * cosR)
        )
    }

    val path = Path().apply {
        val thin = radius * 0.22f
        moveTo(rotatePoint(0f, -radius).x, rotatePoint(0f, -radius).y)
        quadraticBezierTo(
            rotatePoint(0f, 0f).x, rotatePoint(0f, 0f).y,
            rotatePoint(radius, 0f).x, rotatePoint(radius, 0f).y
        )
        quadraticBezierTo(
            rotatePoint(0f, 0f).x, rotatePoint(0f, 0f).y,
            rotatePoint(0f, radius).x, rotatePoint(0f, radius).y
        )
        quadraticBezierTo(
            rotatePoint(0f, 0f).x, rotatePoint(0f, 0f).y,
            rotatePoint(-radius, 0f).x, rotatePoint(-radius, 0f).y
        )
        quadraticBezierTo(
            rotatePoint(0f, 0f).x, rotatePoint(0f, 0f).y,
            rotatePoint(0f, -radius).x, rotatePoint(0f, -radius).y
        )
        close()
    }

    // Resplandor exterior suave
    drawCircle(
        color = color.copy(alpha = color.alpha * 0.35f),
        radius = radius * 1.5f,
        center = center
    )

    // Estrella llena
    drawPath(path = path, color = color)

    // Centro blanco hiperbrillante
    drawCircle(
        color = Color.White.copy(alpha = color.alpha * 0.95f),
        radius = radius * 0.25f,
        center = center
    )
}
