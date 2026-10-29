package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class PreviewTransaction(
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String
)

/**
 * Importador y Exportador Excel / CSV Oficial (ExcelCsvImporter.kt)
 *
 * Características Críticas:
 * - Lector y selector de archivos nativo de Android (.xlsx, .xls, .csv).
 * - Carga de archivo directa desde el teléfono con indicación del nombre.
 * - Opción alternativa para pegar celdas copiadas directamente de Excel/Sheets.
 * - VISTA PREVIA AUTOMÁTICA EN TABLA con desglose de filas detectadas (Concepto, Monto, Tipo, Categoría) antes de guardar.
 * - Exportador de reportes con copia al portapapeles y compartir por WhatsApp/Correo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelCsvImporter(
    initialExportMode: Boolean = false,
    onDismiss: () -> Unit,
    onImportText: (String) -> Unit,
    onExportRequest: suspend () -> String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var isExportMode by remember { mutableStateOf(initialExportMode) }
    var textToImport by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }
    var showManualPasteArea by remember { mutableStateOf(false) }
    var exportPreview by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    // Selector de archivo nativo del teléfono
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                selectedFileName = getFileNameFromUri(context, it)
                val content = com.example.ui.util.ExcelXlsxReader.readUriContentAsCsv(context, it)
                if (content.isNotBlank()) {
                    textToImport = content
                    Toast.makeText(context, "Archivo '$selectedFileName' cargado con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se encontraron filas con datos en '$selectedFileName'", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al leer archivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(isExportMode) {
        if (isExportMode && exportPreview.isBlank()) {
            isGenerating = true
            exportPreview = onExportRequest()
            isGenerating = false
        }
    }

    // Análisis automático y vista previa de transacciones detectadas
    val previewRows = remember(textToImport) {
        parsePreviewTransactions(textToImport)
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("excel_csv_importer_dialog"),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cabecera Oficial
                Surface(
                    color = EmeraldGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isExportMode) "Exportar a Excel / CSV" else "Importar Excel / CSV",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Selector de Pestañas: Importar vs Exportar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { isExportMode = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isExportMode) EmeraldGreen else Color(0xFFF1F5F9),
                                contentColor = if (!isExportMode) Color.White else Color.DarkGray
                            )
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importar", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isExportMode = true
                                coroutineScope.launch {
                                    isGenerating = true
                                    exportPreview = onExportRequest()
                                    isGenerating = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isExportMode) EmeraldGreen else Color(0xFFF1F5F9),
                                contentColor = if (isExportMode) Color.White else Color.DarkGray
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isExportMode) {
                        // 1. CARGA NATIVA DESDE EL DISPOSITIVO
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            border = BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileOpen,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Cargar archivo desde mi teléfono",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Archivos .xlsx, .xls, .csv o texto de Excel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("select_native_excel_csv_file_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Examinar / Seleccionar Archivo", fontWeight = FontWeight.Bold)
                                }

                                if (selectedFileName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldGreen.copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Archivo: $selectedFileName",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldGreen,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. OPCIÓN ALTERNATIVA: PEGAR TEXTO DE CELDAS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "O pegar filas copiadas de Excel",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text ?: ""
                                    if (clip.isNotBlank()) {
                                        textToImport = clip
                                        selectedFileName = "Celdas pegadas del portapapeles"
                                        Toast.makeText(context, "Texto pegado desde el portapapeles", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "El portapapeles está vacío", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pegar", fontSize = 12.sp)
                            }
                        }

                        // Editor de texto colapsable / editable si el usuario desea ajustar
                        OutlinedTextField(
                            value = textToImport,
                            onValueChange = { textToImport = it },
                            placeholder = {
                                Text(
                                    "Formato detectado:\n" +
                                            "Fecha | Concepto | Monto | Tipo | Categoría\n" +
                                            "Ejemplo: Supermercado\t150.00\tAlimentación",
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )

                        // 3. VISTA PREVIA AUTOMÁTICA DE FILAS DETECTADAS
                        if (previewRows.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Vista Previa (${previewRows.size} detectadas)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Total: Q ${"%.2f".format(previewRows.sumOf { it.amount })}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Encabezado de la tabla de vista previa
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("Concepto", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                                        Text("Cat.", modifier = Modifier.weight(1.3f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                                        Text("Monto", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray, textAlign = TextAlign.End)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Muestra las primeras 5 filas detectadas
                                    previewRows.take(5).forEach { row ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2f)) {
                                                Text(row.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                                                Text(row.date, fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (row.type == "Ingreso") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                                modifier = Modifier.weight(1.3f)
                                            ) {
                                                Text(
                                                    text = row.category,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (row.type == "Ingreso") Color(0xFF16A34A) else Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = "Q ${"%.2f".format(row.amount)}",
                                                modifier = Modifier.weight(1.2f),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (row.type == "Ingreso") Color(0xFF16A34A) else Color(0xFFDC2626),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                    }

                                    if (previewRows.size > 5) {
                                        Text(
                                            text = "+ ${previewRows.size - 5} transacciones adicionales detectadas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Botón de Confirmación
                        Button(
                            onClick = {
                                if (textToImport.isNotBlank()) {
                                    onImportText(textToImport)
                                    onDismiss()
                                }
                            },
                            enabled = previewRows.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_import_button")
                        ) {
                            Text(
                                text = if (previewRows.isNotEmpty()) "Guardar ${previewRows.size} Registros en mi Presupuesto" else "Selecciona o pega filas para importar",
                                fontWeight = FontWeight.Bold
                            )
                        }

                    } else {
                        // MODO EXPORTACIÓN
                        Text(
                            text = "Reporte generado listo para abrir en Microsoft Excel, Google Sheets o compartir por correo y WhatsApp:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        if (isGenerating) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = EmeraldGreen)
                            }
                        } else {
                            OutlinedTextField(
                                value = exportPreview,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp),
                                shape = RoundedCornerShape(14.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (exportPreview.isNotBlank()) {
                                            clipboardManager.setText(AnnotatedString(exportPreview))
                                            Toast.makeText(context, "¡Reporte CSV copiado al portapapeles!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = exportPreview.isNotBlank()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copiar", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (exportPreview.isNotBlank()) {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, exportPreview)
                                                putExtra(Intent.EXTRA_TITLE, "Reporte_Quetzi_${System.currentTimeMillis()}.csv")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Exportar Reporte Quetzi"))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    enabled = exportPreview.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compartir", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

/**
 * Extrae el nombre del archivo seleccionado desde el Uri de Android
 */
private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "archivo_seleccionado"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (_: Exception) {}
    return name
}

private fun splitCsvLine(line: String): List<String> {
    return com.example.ui.util.ExcelXlsxReader.parseCsvLine(line)
}

/**
 * Parsea el texto sin procesar para generar la lista de vista previa de transacciones.
 * Utiliza exactamente las mismas reglas de validación y filtrado que la persistencia en Room.
 */
private fun parsePreviewTransactions(raw: String): List<PreviewTransaction> {
    if (raw.isBlank()) return emptyList()
    val parsed = com.example.ui.util.ExcelXlsxReader.parseTransactionsFromText(raw)
    return parsed.map {
        PreviewTransaction(
            title = it.title,
            amount = it.amount,
            type = if (it.type == com.example.data.model.TransactionType.INCOME || it.type.equals("Ingreso", true)) "Ingreso" else "Gasto",
            category = it.category,
            date = it.dateStr
        )
    }
}
