package com.example.ui.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.data.model.TransactionType
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

data class ParsedTransaction(
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val dateStr: String,
    val dateMillis: Long,
    val notes: String = "Importado de Excel",
    val externalId: String? = null
)

private data class SheetMeta(
    val name: String,
    val sheetId: String,
    val rId: String
)

object ExcelXlsxReader {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US),
        SimpleDateFormat("d/M/yyyy HH:mm", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("d/M/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("d-M-yyyy", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd-MMM-yy", Locale.forLanguageTag("es-ES")),
        SimpleDateFormat("dd-MMM-yyyy", Locale.forLanguageTag("es-ES")),
        SimpleDateFormat("dd-MMM-yy", Locale.US),
        SimpleDateFormat("dd-MMM-yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yy", Locale.US),
        SimpleDateFormat("d/M/yy", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("dd.MM.yyyy", Locale.US)
    )

    /**
     * Lee un Uri (que puede ser .xlsx, .xls, .csv, .tsv, etc.) y devuelve el contenido normalizado como CSV/texto.
     */
    fun readUriContentAsCsv(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri) ?: return ""
        return readStreamContentAsCsv(stream)
    }

    /**
     * Si el stream es un ZIP OpenXML (.xlsx), parsea todas las hojas de datos útiles y sharedStrings a formato CSV tabular.
     * De lo contrario, lo lee directamente como texto plano UTF-8 (CSV/TSV).
     */
    fun readStreamContentAsCsv(stream: InputStream): String {
        val bytes = stream.use { it.readBytes() }
        // Detección mágica de encabezado ZIP: PK\u0003\u0004
        if (bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()
        ) {
            try {
                val csv = parseXlsxBytesToCsv(bytes)
                if (csv.isNotBlank()) return csv
            } catch (_: Exception) {
                // Fallback a texto en caso de error
            }
        }
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Parsea un archivo .xlsx desde memoria:
     * 1. Extrae sharedStrings.
     * 2. Detecta pestañas del libro (workbook.xml y workbook.xml.rels).
     * 3. Descarta hojas informativas (p. ej. "INSTRUCCIONES", "Cómo leer y editar el archivo", "Portada").
     * 4. Extrae filas de todas las pestañas de datos (p. ej. "Gastos", "Ingresos", "Transacciones", "Presupuesto").
     * 5. Convierte a formato CSV tabular limpio.
     */
    fun parseXlsxBytesToCsv(bytes: ByteArray): String {
        // 1. Extraer sharedStrings
        val sharedStrings = mutableListOf<String>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                        parseSharedStrings(zip, sharedStrings)
                        break
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (_: Exception) {}

        // 2. Extraer metadatos de las hojas (workbook.xml y workbook.xml.rels)
        val sheetList = mutableListOf<SheetMeta>()
        val relsMap = mutableMapOf<String, String>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name.lowercase(Locale.ROOT)
                    if (entryName == "xl/workbook.xml") {
                        sheetList.addAll(parseWorkbookSheets(zip))
                    } else if (entryName == "xl/_rels/workbook.xml.rels") {
                        relsMap.putAll(parseWorkbookRels(zip))
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (_: Exception) {}

        // Determinar qué archivos de hoja son datos y cuáles son meras instrucciones
        val targetToSheetName = mutableMapOf<String, String>()
        for (sheet in sheetList) {
            val relTarget = relsMap[sheet.rId] ?: "worksheets/sheet${sheet.sheetId}.xml"
            val normFile = relTarget.substringAfterLast("/").lowercase(Locale.ROOT)
            targetToSheetName[normFile] = sheet.name
        }

        val dataSheets = sheetList.filter { !isInstructionalSheet(it.name) }
        val candidateSheets = if (dataSheets.isNotEmpty()) dataSheets else sheetList

        val allowedSheetFileNames = candidateSheets.map { sheet ->
            val relTarget = relsMap[sheet.rId] ?: "worksheets/sheet${sheet.sheetId}.xml"
            relTarget.substringAfterLast("/").lowercase(Locale.ROOT)
        }.toSet()

        // 3. Extraer filas de las hojas de cálculo candidatas (SIN detenerse en la primera hoja)
        val rows = mutableListOf<List<String>>()
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase(Locale.ROOT)
                    if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                        val fileName = name.substringAfterLast("/")
                        val shouldParse = if (allowedSheetFileNames.isNotEmpty()) {
                            allowedSheetFileNames.contains(fileName)
                        } else {
                            true
                        }
                        if (shouldParse) {
                            parseSheet(zip, sharedStrings, rows)
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (_: Exception) {}

        if (rows.isEmpty()) return ""

        // 4. Convertir filas a formato CSV seguro y convertir números seriales de fecha de Excel
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        for (row in rows) {
            if (row.all { it.isBlank() }) continue

            val formattedRow = row.mapIndexed { colIdx, rawVal ->
                var cell = rawVal.trim()
                // Convertir número de fecha serial de Excel si está en la primera o segunda columna
                if ((colIdx == 0 || (colIdx == 1 && row[0].isBlank())) && cell.toDoubleOrNull() != null) {
                    val num = cell.toDouble()
                    if (num in 20000.0..70000.0) {
                        val millis = ((num - 25569.0) * 86400000.0).toLong()
                        cell = sdf.format(Date(millis))
                    }
                }
                // Escapar para CSV
                if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains(";")) {
                    "\"" + cell.replace("\"", "\"\"") + "\""
                } else {
                    cell
                }
            }
            sb.append(formattedRow.joinToString(",")).append("\n")
        }

        return sb.toString()
    }

    private fun parseWorkbookSheets(stream: InputStream): List<SheetMeta> {
        val list = mutableListOf<SheetMeta>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("sheet", ignoreCase = true)) {
                    val name = parser.getAttributeValue(null, "name") ?: ""
                    val sheetId = parser.getAttributeValue(null, "sheetId") ?: ""
                    var rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id") ?: ""
                    if (rId.isBlank()) {
                        rId = parser.getAttributeValue(null, "r:id") ?: ""
                    }
                    if (name.isNotBlank()) {
                        list.add(SheetMeta(name = name, sheetId = sheetId, rId = rId))
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseWorkbookRels(stream: InputStream): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name.equals("Relationship", ignoreCase = true)) {
                    val id = parser.getAttributeValue(null, "Id") ?: ""
                    val target = parser.getAttributeValue(null, "Target") ?: ""
                    if (id.isNotBlank() && target.isNotBlank()) {
                        map[id] = target
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return map
    }

    fun isInstructionalSheet(sheetName: String): Boolean {
        val clean = sheetName.trim().lowercase(Locale.ROOT)
        return clean.contains("instrucc") ||
               clean.contains("instruct") ||
               clean.contains("como leer") ||
               clean.contains("cómo leer") ||
               clean.contains("como usar") ||
               clean.contains("cómo usar") ||
               clean.contains("guia") ||
               clean.contains("guía") ||
               clean.contains("leeme") ||
               clean.contains("readme") ||
               clean.contains("portada") ||
               clean.contains("cover") ||
               clean.contains("ayuda") ||
               clean.contains("help") ||
               clean.contains("info") ||
               clean.contains("acerca") ||
               clean.contains("plantilla")
    }

    private fun parseSharedStrings(stream: InputStream, list: MutableList<String>) {
        val parser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")
        var eventType = parser.eventType
        var inSi = false
        var inT = false
        var currentText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            currentText = StringBuilder()
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) {
                        currentText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> {
                            inSi = false
                            list.add(currentText.toString())
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseSheet(stream: InputStream, sharedStrings: List<String>, rows: MutableList<List<String>>) {
        val parser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")
        var eventType = parser.eventType
        var currentRow: MutableMap<Int, String>? = null
        var currentColIndex = 0
        var currentCellType = ""
        var inV = false
        var inIs = false
        var inT = false
        var cellValue = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableMapOf()
                            currentColIndex = 0
                        }
                        "c" -> {
                            val r = parser.getAttributeValue(null, "r")
                            if (r != null) {
                                currentColIndex = cellRefToColIndex(r)
                            }
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValue = StringBuilder()
                        }
                        "v" -> inV = true
                        "is" -> inIs = true
                        "t" -> if (inIs) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV || (inIs && inT)) {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inV = false
                        "t" -> inT = false
                        "is" -> inIs = false
                        "c" -> {
                            val raw = cellValue.toString().trim()
                            val finalVal = when (currentCellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) {
                                        sharedStrings[idx]
                                    } else raw
                                }
                                "b" -> if (raw == "1") "TRUE" else "FALSE"
                                else -> raw
                            }
                            currentRow?.put(currentColIndex, finalVal)
                            currentColIndex++
                        }
                        "row" -> {
                            if (currentRow != null && currentRow.isNotEmpty()) {
                                val maxCol = currentRow.keys.maxOrNull() ?: 0
                                val rowList = ArrayList<String>(maxCol + 1)
                                for (i in 0..maxCol) {
                                    rowList.add(currentRow[i] ?: "")
                                }
                                rows.add(rowList)
                            }
                            currentRow = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }

    private fun cellRefToColIndex(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return if (col > 0) col - 1 else 0
    }

    /**
     * Tokeniza una línea CSV/TSV respetando comillas y delimitadores estándar.
     */
    fun parseCsvLine(line: String): List<String> {
        val delimiter = when {
            line.contains("\t") -> '\t'
            line.contains(";") -> ';'
            else -> ','
        }
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    tokens.add(current.toString().trim().removeSurrounding("\""))
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        tokens.add(current.toString().trim().removeSurrounding("\""))
        return tokens
    }

    fun isInstructionOrMetaText(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        if (t.isBlank()) return false
        return t.contains("instrucc") ||
               t.contains("instruct") ||
               t.contains("cómo leer") ||
               t.contains("como leer") ||
               t.contains("cómo usar") ||
               t.contains("como usar") ||
               t.contains("leer y editar") ||
               t.contains("plantilla") ||
               t.contains("notas de versión") ||
               t.contains("notas de version") ||
               t.contains("guía rápida") ||
               t.contains("guia rapida") ||
               t.contains("paso a paso") ||
               t.startsWith("nota:") ||
               t.startsWith("importante:") ||
               t.startsWith("atención:") ||
               t.startsWith("atencion:") ||
               t.startsWith("recuerde:") ||
               t.startsWith("aviso:") ||
               t.contains("copyright") ||
               t.contains("todos los derechos")
    }

    fun isSummaryOrTotalText(text: String): Boolean {
        val t = text.trim().lowercase(Locale.ROOT)
        if (t.isBlank()) return false
        return t == "total" ||
               t == "totales" ||
               t.startsWith("total ") ||
               t.startsWith("totales ") ||
               t.startsWith("subtotal") ||
               t == "suma" ||
               t.startsWith("suma ") ||
               t == "saldo" ||
               t.startsWith("saldo ") ||
               t == "balance" ||
               t.startsWith("balance ") ||
               t == "diferencia" ||
               t.startsWith("diferencia ") ||
               t == "resumen" ||
               t.startsWith("resumen ")
    }

    fun isHeaderRow(parts: List<String>): Boolean {
        if (parts.isEmpty()) return false
        val first = parts[0].trim().lowercase(Locale.ROOT)
        val second = parts.getOrNull(1)?.trim()?.lowercase(Locale.ROOT) ?: ""
        val headers = setOf(
            "fecha", "date", "concepto", "título", "titulo", "title",
            "tipo", "type", "categoría", "categoria", "category",
            "monto", "amount", "importe", "valor", "gasto", "ingreso",
            "gastos", "ingresos", "descripción", "descripcion", "description",
            "no.", "no", "id", "#", "referencia", "ref", "folio", "hash", "código", "codigo", "uuid"
        )
        return headers.contains(first) || (first.isBlank() && headers.contains(second))
    }

    private fun cleanAmount(raw: String?): Double? {
        if (raw == null) return null
        val cleaned = raw.replace("Q", "")
            .replace("$", "")
            .replace("€", "")
            .replace(",", "")
            .replace("\"", "")
            .replace(" ", "")
            .trim()
        return cleaned.toDoubleOrNull()
    }

    /**
     * Parsea texto (CSV, TSV o filas extraídas de Excel) produciendo una lista verificada de transacciones.
     * Descarta filas informativas, de resumen y cabeceras, garantizando coherencia absoluta entre
     * la vista previa y la persistencia final en la base de datos Room.
     */
    fun parseTransactionsFromText(raw: String): List<ParsedTransaction> {
        if (raw.isBlank()) return emptyList()
        val lines = raw.lines().filter { it.isNotBlank() }
        val result = mutableListOf<ParsedTransaction>()

        var colDate = -1
        var colType = -1
        var colTitle = -1
        var colCategory = -1
        var colExpense = -1
        var colIncome = -1
        var colAmount = -1
        var colNotes = -1
        var colExternalId = -1
        var hasDetectedHeader = false

        for (line in lines) {
            val parts = parseCsvLine(line)
            if (parts.size < 2) continue

            // 1. Detección y mapeo de fila de encabezados
            if (isHeaderRow(parts)) {
                hasDetectedHeader = true
                colDate = -1
                colType = -1
                colTitle = -1
                colCategory = -1
                colExpense = -1
                colIncome = -1
                colAmount = -1
                colNotes = -1
                colExternalId = -1

                parts.forEachIndexed { idx, colRaw ->
                    val c = colRaw.trim().lowercase(Locale.ROOT)
                    when {
                        c.contains("fecha") || c.contains("date") -> colDate = idx
                        c.contains("tipo") || c.contains("type") -> colType = idx
                        c.contains("concepto") || c.contains("título") || c.contains("titulo") || c.contains("title") || c.contains("nombre") -> colTitle = idx
                        c.contains("categoría") || c.contains("categoria") || c.contains("category") || c.contains("rubro") -> colCategory = idx
                        c.contains("gasto") || c.contains("egreso") || c.contains("débito") || c.contains("debito") -> colExpense = idx
                        c.contains("ingreso") || c.contains("crédito") || c.contains("credito") || c.contains("abono") -> colIncome = idx
                        c.contains("monto") || c.contains("amount") || c.contains("importe") || c.contains("cantidad") || c.contains("valor") -> colAmount = idx
                        c.contains("descrip") || c.contains("nota") || c.contains("detalle") || c.contains("observac") -> colNotes = idx
                        c.contains("id") || c.contains("ref") || c.contains("folio") || c.contains("uuid") || c.contains("hash") || c.contains("código") || c.contains("codigo") || c == "no." || c == "no" || c == "#" -> colExternalId = idx
                    }
                }
                continue
            }

            // 2. Descarte de filas de instrucciones o advertencias informativas
            val firstToken = parts[0].trim()
            val secondToken = parts.getOrNull(1)?.trim() ?: ""
            if (isInstructionOrMetaText(firstToken) || isInstructionOrMetaText(secondToken)) {
                continue
            }

            // Normalización por si la primera columna es un margen vacío o numeración de fila (1, 2, 3...)
            val effectiveParts = if (firstToken.isBlank() && parts.size > 2) {
                parts.drop(1)
            } else {
                parts
            }

            var title = ""
            var amount = 0.0
            var type = TransactionType.EXPENSE
            var category = "Otros Gastos"
            var notes = "Importado de Excel"
            var externalId: String? = null
            var dateStr = effectiveParts[0]

            // 3. Extracción según mapeo de cabecera o heurísticas avanzadas
            if (hasDetectedHeader && (colTitle != -1 || colAmount != -1 || colExpense != -1 || colIncome != -1)) {
                if (colDate != -1 && colDate < parts.size) {
                    dateStr = parts[colDate].trim()
                }
                if (colTitle != -1 && colTitle < parts.size) {
                    title = parts[colTitle].trim()
                }
                if (colCategory != -1 && colCategory < parts.size) {
                    category = parts[colCategory].trim().ifBlank { "General" }
                }
                if (colNotes != -1 && colNotes < parts.size) {
                    val n = parts[colNotes].trim()
                    if (n.isNotBlank()) notes = n
                }
                if (colExternalId != -1 && colExternalId < parts.size) {
                    val ext = parts[colExternalId].trim()
                    if (ext.isNotBlank()) externalId = ext
                }

                val expVal = if (colExpense != -1 && colExpense < parts.size) cleanAmount(parts[colExpense]) ?: 0.0 else 0.0
                val incVal = if (colIncome != -1 && colIncome < parts.size) cleanAmount(parts[colIncome]) ?: 0.0 else 0.0
                val amtVal = if (colAmount != -1 && colAmount < parts.size) cleanAmount(parts[colAmount]) ?: 0.0 else 0.0

                val rawType = if (colType != -1 && colType < parts.size) parts[colType].trim() else ""

                if (incVal > 0.0 || rawType.equals("Ingreso", true) || rawType.equals("INCOME", true)) {
                    type = TransactionType.INCOME
                    amount = if (incVal > 0.0) incVal else (if (amtVal > 0.0) amtVal else expVal)
                } else {
                    type = TransactionType.EXPENSE
                    amount = if (expVal > 0.0) expVal else amtVal
                }
            } else {
                // Heurística de columnas sin cabecera explícita
                // Formato Usuario 7+ columnas: Fecha | Tipo | Concepto | Categoría | Descripción | Monto Gasto | Monto Ingreso
                if (effectiveParts.size >= 7 && (effectiveParts[1].equals("Gasto", true) || effectiveParts[1].equals("Ingreso", true))) {
                    val rawType = effectiveParts[1].trim()
                    title = effectiveParts[2].trim()
                    category = effectiveParts[3].trim().ifBlank { "Otros Gastos" }
                    val desc = effectiveParts.getOrNull(4)?.trim() ?: ""
                    if (desc.isNotBlank()) notes = desc

                    val expVal = cleanAmount(effectiveParts[5]) ?: 0.0
                    val incVal = cleanAmount(effectiveParts[6]) ?: 0.0

                    if (rawType.equals("Ingreso", true) || incVal > 0.0) {
                        type = TransactionType.INCOME
                        amount = if (incVal > 0.0) incVal else expVal
                    } else {
                        type = TransactionType.EXPENSE
                        amount = expVal
                    }
                } else if (effectiveParts.size >= 4 && (effectiveParts[1].equals("Gasto", true) || effectiveParts[1].equals("Ingreso", true))) {
                    // Formato 4-5 columnas con tipo en col 1: Fecha | Tipo | Concepto | Categoría | Monto
                    val rawType = effectiveParts[1].trim()
                    type = if (rawType.equals("Ingreso", true)) TransactionType.INCOME else TransactionType.EXPENSE
                    title = effectiveParts[2].trim()
                    category = effectiveParts.getOrNull(3)?.trim()?.ifBlank { "General" } ?: "General"
                    amount = cleanAmount(effectiveParts.getOrNull(4))
                        ?: cleanAmount(effectiveParts.getOrNull(3))
                        ?: 0.0
                } else {
                    // Formato Quetzi Export (Fecha | Título | Monto | Tipo | Categoría...) o estándar
                    title = effectiveParts.getOrNull(1)?.ifBlank { "Transacción" } ?: "Transacción"
                    val rawAmt1 = cleanAmount(effectiveParts.getOrNull(2)) ?: 0.0
                    if (rawAmt1 > 0.0) {
                        amount = rawAmt1
                        val rawType = effectiveParts.getOrNull(3)?.trim() ?: ""
                        type = if (rawType.equals("Ingreso", true) || rawType.equals("INCOME", true)) {
                            TransactionType.INCOME
                        } else {
                            TransactionType.EXPENSE
                        }
                        category = effectiveParts.getOrNull(4)?.trim()?.ifBlank { "General" } ?: "General"
                    } else {
                        val rawAmt2 = cleanAmount(effectiveParts.getOrNull(3)) ?: 0.0
                        if (rawAmt2 > 0.0) {
                            amount = rawAmt2
                            category = effectiveParts.getOrNull(2)?.trim()?.ifBlank { "General" } ?: "General"
                            val rawType = effectiveParts.getOrNull(4)?.trim() ?: ""
                            type = if (rawType.equals("Ingreso", true) || rawType.equals("INCOME", true)) {
                                TransactionType.INCOME
                            } else {
                                TransactionType.EXPENSE
                            }
                        }
                    }
                }
            }

            // 4. Filtrar filas de subtotales, totales, balance y resúmenes de Excel
            if (isSummaryOrTotalText(title) || isInstructionOrMetaText(title)) {
                continue
            }

            // 5. Validación de transacción obligatoria: monto positivo y concepto válido
            if (amount <= 0.0 || title.isBlank() || title.length <= 1) {
                continue
            }

            // 6. Conversión de fecha a milisegundos de forma robusta
            var parsedDate: Date? = null
            val cleanDate = dateStr.trim()
            val serial = cleanAmount(cleanDate)
            if (serial != null && serial in 30000.0..70000.0 && !cleanDate.contains("/") && !cleanDate.contains("-") && !cleanDate.contains(".")) {
                val days = serial.toLong()
                val cal = java.util.Calendar.getInstance()
                cal.clear()
                cal.set(1899, java.util.Calendar.DECEMBER, 30, 12, 0, 0)
                cal.add(java.util.Calendar.DAY_OF_YEAR, days.toInt())
                parsedDate = cal.time
            } else {
                for (fmt in DATE_FORMATS) {
                    try {
                        parsedDate = fmt.parse(cleanDate)
                        if (parsedDate != null) break
                    } catch (_: Exception) {}
                }
            }

            // Normalización a las 12:00 del mediodía local para blindar contra cualquier desfase horario
            val cal = java.util.Calendar.getInstance()
            if (parsedDate != null) {
                cal.time = parsedDate
            }
            cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val dateMillis = cal.timeInMillis

            val formattedDateStr = if (parsedDate != null) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US).format(cal.time)
            } else {
                dateStr.ifBlank { "Hoy" }
            }

            result.add(
                ParsedTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    dateStr = formattedDateStr,
                    dateMillis = dateMillis,
                    notes = notes,
                    externalId = externalId
                )
            )
        }

        return result
    }
}
