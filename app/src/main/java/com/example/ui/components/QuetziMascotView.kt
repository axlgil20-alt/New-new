package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldGreen
import com.example.ui.viewmodel.BudgetUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * QuetziMascotView - Animación de Vuelo Profundo 3D y Saludo Real Chapín
 *
 * Mejoras Clave:
 * 1. Vuelo Real y Dinámico (Sin saltos verticales):
 *    - Inicia volando desde el fondo (cerca de Tikal y los volcanes en escala 0.45x).
 *    - Realiza un barrido curvilíneo fluido acercándose hacia la pantalla con efecto profundidad (1.28x).
 *    - Regresa y aterriza suavemente en su posición flotante natural.
 * 2. Animación de Manos Activa (Saludo Real "¡Hola!"):
 *    - Mueve activamente sus manos saludando de lado a lado en un saludo alegre antes de congelarse en su pulgar arriba (👍).
 * 3. Física de Flotación Continua (Lissajous / Ocho Suave):
 *    - Movimiento aerodinámico sutil y orgánico de caricatura con inclinación (banking).
 */
@Composable
fun QuetziMascotView(
    isDiscreetMode: Boolean,
    celebrationTrigger: Long = 0L,
    uiState: BudgetUiState? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val quetziAdviceList = remember(uiState, isDiscreetMode) {
        val tips = mutableListOf<String>()
        if (uiState != null) {
            val net = uiState.netBalance
            val inc = uiState.totalIncome
            val exp = uiState.totalExpense
            val sym = uiState.currencySymbol

            if (exp > inc && inc > 0) {
                tips.add("⚠️ Ojo al presupuesto: tus gastos superan tus ingresos este mes. Ajusta tus salidas variables para no desbalancearte.")
            } else if (net > 0 && inc > 0) {
                val formattedNet = if (isDiscreetMode) "Q ****.**" else "$sym${"%,.2f".format(net)}"
                tips.add("💡 Tu saldo disponible va en positivo con $formattedNet. ¡Buen momento para destinar un porcentaje a tus metas!")
            }

            if (uiState.savingsRate >= 10.0) {
                tips.add("📈 ¡Excelente manejo! Tu tasa de ahorro estimada es del ${"%.0f".format(uiState.savingsRate)}% este periodo.")
            }

            if (uiState.smartCartItems.isNotEmpty()) {
                val unchecked = uiState.smartCartItems.count { !it.isChecked }
                if (unchecked > 0) {
                    tips.add("🛒 Smart Cart: Tienes $unchecked artículos pendientes en tu lista de súper. Cotiza antes de ir al súper.")
                }
            }

            if (uiState.upcomingDeficitWarnings.isNotEmpty()) {
                tips.add("📅 Tienes gastos fijos o pagos recurrentes próximos. Ten reservado el efectivo correspondiente.")
            }
        }

        // Consejos prácticos de alto valor para las finanzas familiares
        tips.add("🛡️ Privacidad: Si estás en transporte o en la calle, activa el Ojito (👁️) para ocultar tus montos en quetzales.")
        tips.add("💡 Ahorro previo: Destina primero una porción de tus ingresos al ahorro antes de gastar lo demás.")
        tips.add("🛒 Planifica en frío: Preparar tu lista en Smart Cart desde casa te ayuda a no comprar por impulso en la tienda.")
        tips.add("⚖️ Simulación: Si dudas de un gasto, márcalo como 'Supuesto' para ver cómo afectaría tu saldo sin registrarlo aún.")
        tips
    }

    var tipIndex by remember { mutableIntStateOf(0) }
    var isDeepFlying by remember { mutableStateOf(false) }
    var isWavingHands by remember { mutableStateOf(false) }
    var showThumbsUpBadge by remember { mutableStateOf(true) }
    var isCelebrating by remember { mutableStateOf(false) }

    // Animadores de control de trayectoria de vuelo libre
    val flightProgress = remember { Animatable(1f) } // 0f = inicio en el fondo (volcanes), 1f = posado
    val waveHandAngle = remember { Animatable(0f) }
    val spinRotation = remember { Animatable(0f) }

    // Función suspendida para ejecutar el vuelo libre con profundidad y el saludo real
    suspend fun performDeepFlightAndGreeting() {
        isDeepFlying = true
        showThumbsUpBadge = false
        isWavingHands = false

        // 1. Despegar desde el fondo de Tikal/volcanes (progreso 0.0) hacia el primer plano
        flightProgress.snapTo(0f)
        flightProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
        isDeepFlying = false

        // 2. Al momento de posarse: Saludo real activo moviendo las manos ("¡Hola chapín!")
        isWavingHands = true
        repeat(4) {
            waveHandAngle.animateTo(26f, tween(120, easing = LinearEasing))
            waveHandAngle.animateTo(-26f, tween(120, easing = LinearEasing))
        }
        waveHandAngle.animateTo(0f, tween(100))
        isWavingHands = false

        // 3. Congelarse y sellar con su gesto amistoso de pulgar arriba (thumbs up 👍)
        showThumbsUpBadge = true
    }

    // Ejecutar vuelo de bienvenida al iniciar la vista
    LaunchedEffect(Unit) {
        delay(350)
        performDeepFlightAndGreeting()
    }

    // Reaccionar cuando se guarda una transacción (Giro festivo 360° + saludo y pulgar arriba)
    LaunchedEffect(celebrationTrigger) {
        if (celebrationTrigger > 0L) {
            isCelebrating = true
            showThumbsUpBadge = false

            // Giro festivo 360°
            spinRotation.snapTo(0f)
            spinRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )

            // Saludo de felicitación activo
            isWavingHands = true
            repeat(3) {
                waveHandAngle.animateTo(24f, tween(110))
                waveHandAngle.animateTo(-24f, tween(110))
            }
            waveHandAngle.animateTo(0f, tween(90))
            isWavingHands = false

            // Pulgar arriba triunfal
            showThumbsUpBadge = true
            delay(1200)
            isCelebrating = false
        }
    }

    // Física de flotación aerodinámica orgánica (Lissajous continua cuando está posado)
    val infiniteTransition = rememberInfiniteTransition(label = "quetzi_organic_flight")

    val idleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_time"
    )

    // Curvas suaves de flotación continua (no salto vertical, sino suave figura en ocho)
    val idleOffsetX = sin(idleTime) * 8f
    val idleOffsetY = sin(idleTime * 2f) * 4.5f
    val idleTilt = cos(idleTime) * 4.2f

    // Parámetros calculados según el progreso del VUELO PROFUNDO (0f a 1f)
    val p = flightProgress.value
    // Trayectoria Bezier/curvilínea:
    // Al inicio (p=0): Viene del fondo (y = -85dp, x = -70dp, escala 0.48x)
    // En el clímax (p=0.55): Se acerca al frente con efecto profundidad (y = 10dp, x = 35dp, escala 1.30x)
    // Al final (p=1.0): Se posa suavemente (x = 0dp, y = 0dp, escala 1.0x)
    val flightX = if (isDeepFlying) {
        val t = p
        (-70f * (1f - t) * (1f - t) + 110f * 2f * (1f - t) * t + 0f * t * t)
    } else 0f

    val flightY = if (isDeepFlying) {
        val t = p
        (-85f * (1f - t) * (1f - t) + 25f * 2f * (1f - t) * t + 0f * t * t)
    } else 0f

    val flightScale = if (isDeepFlying) {
        val t = p
        (0.48f * (1f - t) * (1f - t) + 1.35f * 2f * (1f - t) * t + 1.0f * t * t)
    } else 1.0f

    val flightTilt = if (isDeepFlying) {
        val t = p
        sin(t * PI.toFloat() * 2f) * 18f
    } else 0f

    val combinedX = flightX + (if (!isDeepFlying) idleOffsetX else 0f)
    val combinedY = flightY + (if (!isDeepFlying) idleOffsetY else 0f)
    val combinedRotation = flightTilt + idleTilt + spinRotation.value + (waveHandAngle.value * 0.25f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("quetzi_mascot_view")
            .clickable {
                coroutineScope.launch {
                    tipIndex = (tipIndex + 1) % quetziAdviceList.size
                    performDeepFlightAndGreeting()
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCelebrating) Color(0xFFF0FDF4) else Color(0xFFFBFDFB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDeepFlying) 8.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isCelebrating) Color(0xFF86EFAC) else Color(0xFFD1E7DD)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor de Quetzi con área suficiente para el vuelo libre y la estela
            Box(
                modifier = Modifier
                    .size(115.dp),
                contentAlignment = Alignment.Center
            ) {
                // 1. Estela luminosa y chispas de polvo de estrellas por DETRÁS de Quetzi
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                ) {
                    drawFlightSparkleTrail(
                        posX = combinedX,
                        posY = combinedY,
                        isDeepFlying = isDeepFlying,
                        isCelebrating = isCelebrating,
                        progress = p
                    )
                }

                // 2. Sticker de Quetzi con transformación tridimensional (escala y trayectoria libre)
                Box(
                    modifier = Modifier
                        .offset(x = combinedX.dp, y = combinedY.dp)
                        .scale(flightScale)
                        .rotate(combinedRotation),
                    contentAlignment = Alignment.Center
                ) {
                    // Imagen principal sin fondo blanco
                    Image(
                        painter = painterResource(id = R.drawable.quetzi_mascot),
                        contentDescription = "Quetzi Mascota Chapina",
                        modifier = Modifier
                            .size(92.dp),
                        contentScale = ContentScale.Fit
                    )

                    // 3. Animación de Manos Activa (Saludo Real "¡Hola!"):
                    // Se superpone un brazo/mano animado con movimiento oscilante alegre
                    if (isWavingHands) {
                        Canvas(
                            modifier = Modifier
                                .size(92.dp)
                        ) {
                            drawWavingHandOverlay(waveAngle = waveHandAngle.value)
                        }
                    }

                    // 4. Insignia sellada con Pulgar Arriba (Thumbs Up 👍) al terminar el saludo
                    if (showThumbsUpBadge && !isDeepFlying) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFDE047),
                            shadowElevation = 5.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 6.dp)
                                .size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "👍",
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Burbuja de diálogo de Quetzi con personalidad guatemalteca
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Quetzi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD1E7DD)
                        ) {
                            Text(
                                text = when {
                                    isDeepFlying -> "¡Volando sobre Tikal! 🦅"
                                    isWavingHands -> "¡Pilas con tu dinero! 👍"
                                    isCelebrating -> "¡Todo Nítido! 👍"
                                    else -> "Consejo Financiero"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Botón para hacer volar a Quetzi a voluntad
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                coroutineScope.launch {
                                    performDeepFlightAndGreeting()
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Flight,
                                contentDescription = "Volar Quetzi",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        isDeepFlying -> "¡Surcando los volcanes y templos de Tikal! De chapín a chapín, ¡cuidemos cada quetzal!"
                        isWavingHands -> "¡Organízate hoy! Recuerda revisar tus gastos antes de salir de compras."
                        isCelebrating -> "¡Excelente! Transacción guardada con éxito en tu base de datos."
                        else -> quetziAdviceList[tipIndex % quetziAdviceList.size]
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

/**
 * Dibuja la mano que saluda activamente ("¡Hola!") con guante cartoon
 * oscilando de lado a lado con articulación fluida.
 */
private fun DrawScope.drawWavingHandOverlay(waveAngle: Float) {
    // Posición del hombro/mano derecha de Quetzi
    val shoulderX = size.width * 0.72f
    val shoulderY = size.height * 0.48f

    val armLength = 20f
    val rad = (waveAngle - 45f) * (PI.toFloat() / 180f)
    val handX = shoulderX + cos(rad) * armLength
    val handY = shoulderY + sin(rad) * armLength

    // Brazo verde estilo billete
    drawLine(
        color = Color(0xFF059669),
        start = Offset(shoulderX, shoulderY),
        end = Offset(handX, handY),
        strokeWidth = 6.5f,
        cap = StrokeCap.Round
    )

    // Guante blanco de caricatura
    drawCircle(
        color = Color.White,
        radius = 8f,
        center = Offset(handX, handY)
    )
    drawCircle(
        color = Color(0xFF047857),
        radius = 8f,
        center = Offset(handX, handY),
        style = Stroke(width = 1.5f)
    )

    // Dedos del saludo
    val fingerOffset = Offset(cos(rad + 0.3f) * 4f, sin(rad + 0.3f) * 4f)
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = Offset(handX + fingerOffset.x, handY + fingerOffset.y)
    )
}

/**
 * Estela luminosa dinámica y ráfaga de estrellas que sigue a Quetzi por detrás
 */
private fun DrawScope.drawFlightSparkleTrail(
    posX: Float,
    posY: Float,
    isDeepFlying: Boolean,
    isCelebrating: Boolean,
    progress: Float
) {
    val originX = size.width / 2f + posX - 14f
    val originY = size.height / 2f + posY + 16f

    val trailLength = if (isDeepFlying) 70f else if (isCelebrating) 55f else 32f
    val alphaFactor = if (isDeepFlying) 0.85f else if (isCelebrating) 0.75f else 0.45f

    // Pincel degradado de la estela cometa
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF34D399).copy(alpha = alphaFactor),
            Color(0xFFFDE047).copy(alpha = alphaFactor * 0.7f),
            Color.Transparent
        ),
        start = Offset(originX, originY),
        end = Offset(originX - trailLength, originY + trailLength * 0.45f)
    )

    val path = Path().apply {
        moveTo(originX, originY - 12f)
        lineTo(originX - trailLength, originY + trailLength * 0.45f)
        lineTo(originX + 6f, originY + 14f)
        close()
    }
    drawPath(path = path, brush = brush)

    // Estrellas y chispas doradas flotantes
    val particleCount = if (isDeepFlying) 10 else if (isCelebrating) 8 else 4
    for (i in 0 until particleCount) {
        val dist = 14f + i * 8f
        val px = originX - dist - (sin(i * 1.3f) * 8f)
        val py = originY + (i * 5f) + (cos(i * 1.5f) * 6f)
        val pRadius = (2f + (i % 3) * 1.4f)
        val pAlpha = (0.75f - i * 0.07f).coerceIn(0.15f, 0.85f)

        drawCircle(
            color = if (i % 2 == 0) Color(0xFF10B981).copy(alpha = pAlpha) else Color(0xFFF59E0B).copy(alpha = pAlpha),
            radius = pRadius,
            center = Offset(px, py)
        )
    }
}
