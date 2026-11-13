package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.EmeraldGreen
import com.example.ui.util.TermsAcceptanceManager
import com.example.ui.util.TermsAcceptanceRecord

/**
 * Modal de Términos y Privacidad de Quetzi con Cláusula de Anonimización Estricta
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndPrivacyDialog(
    onAccept: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    onDeclineAndExit: (() -> Unit)? = null,
    isMandatory: Boolean = onDismiss == null,
    termsVersion: String = TermsAcceptanceManager.CURRENT_TERMS_VERSION,
    acceptanceRecord: TermsAcceptanceRecord? = null
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accentEmerald = if (isDarkTheme) Color(0xFF34D399) else EmeraldGreen
    val declineColor = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)

    BasicAlertDialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismiss?.invoke()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.2.dp, accentEmerald.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("terms_privacy_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera Oficial
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldGreen,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Términos y Privacidad",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Quetzi • MarvGr®",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!isMandatory && onDismiss != null) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cláusula 1: Almacenamiento Offline-First
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentEmerald.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = accentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "1. Privacidad y Soberanía Offline",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = accentEmerald
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tus registros financieros y presupuestos residen exclusivamente en la base de datos local (Room) de tu dispositivo. No subimos tus saldos ni tus gastos a servidores externos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cláusula 2: Anonimización Estricta para Bancos (REQUERIMIENTO OBLIGATORIO)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentEmerald.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = accentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "2. Anonimización Estricta para Bancos",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = accentEmerald
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "En caso de integrarse o comunicarse con ecosistemas financieros o bancarios autorizados en Guatemala (SIB/SAT), toda la telemetría operará bajo estricta anonimización agregada. Ninguna entidad bancaria o tercero podrá identificar a qué usuario individual corresponden los registros financieros, ni acceder a información sensible sin consentimiento expreso.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cláusula 3: Cero Venta de Datos y Control Total
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = "3. Control y Respaldo Chapín",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Puedes exportar tu información en formato CSV estándar de Excel o limpiar todos los registros en cualquier momento desde los ajustes del sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metadatos de versión y estado de aceptación local
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "Versión v$termsVersion",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (acceptanceRecord != null &&
                            acceptanceRecord.acceptedAtMillis > 0L &&
                            !acceptanceRecord.version.isNullOrBlank()
                        ) {
                            Text(
                                text = "Aceptado (v${acceptanceRecord.version}) el ${TermsAcceptanceManager.formatDisplayTimestamp(acceptanceRecord.acceptedAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isMandatory) {
                            Text(
                                text = "Debes aceptar estos términos expresamente para ingresar a QUETZI.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de aceptación expresa o cierre informativo
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("accept_terms_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isMandatory) "Aceptar y Comenzar" else "Entendido",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (isMandatory && onDeclineAndExit != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onDeclineAndExit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("decline_terms_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, declineColor.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = declineColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No acepto, salir",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
