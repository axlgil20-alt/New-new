package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class ScannedGroceryItem(
    var name: String,
    var quantity: Double = 1.0,
    var unit: String = "Unidad",
    var price: Double = 0.0,
    var isSelected: Boolean = true
)

object ListScannerHelper {

    /**
     * Procesa un Bitmap en el dispositivo con Google ML Kit Text Recognition gratuito y offline.
     */
    fun processImageBitmap(
        bitmap: Bitmap,
        onResult: (List<ScannedGroceryItem>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val items = parseOcrTextToItems(visionText.text)
                    onResult(items)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Carga un Bitmap desde un Uri de galería o archivo local de manera segura.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parsea texto sin formato extraído por OCR a una lista estructurada de productos de súper/despensa.
     */
    fun parseOcrTextToItems(rawText: String): List<ScannedGroceryItem> {
        if (rawText.isBlank()) return emptyList()

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 }

        val result = mutableListOf<ScannedGroceryItem>()

        // Expresión regular para detectar unidades comunes en Guatemala/español
        val unitMap = mapOf(
            "lb" to "Libra",
            "lbs" to "Libra",
            "libra" to "Libra",
            "libras" to "Libra",
            "kg" to "Kilo",
            "kilo" to "Kilo",
            "kilos" to "Kilo",
            "l" to "Litro",
            "lt" to "Litro",
            "lts" to "Litro",
            "litro" to "Litro",
            "litros" to "Litro",
            "paquete" to "Paquete",
            "paquetes" to "Paquete",
            "pqt" to "Paquete",
            "pqts" to "Paquete",
            "caja" to "Caja",
            "cajas" to "Caja",
            "bolsa" to "Bolsa",
            "bolsas" to "Bolsa",
            "lata" to "Lata",
            "latas" to "Lata",
            "docena" to "Docena",
            "docenas" to "Docena",
            "unidad" to "Unidad",
            "unidades" to "Unidad",
            "und" to "Unidad",
            "unds" to "Unidad"
        )

        val priceRegex = Regex("""(?:Q|GTQ|\$)?\s*([0-9]+(?:[.,][0-9]{1,2}))\b""", RegexOption.IGNORE_CASE)

        for (rawLine in lines) {
            // Ignorar palabras comunes de cabeceras de tickets o listas
            val lower = rawLine.lowercase()
            if (lower.startsWith("supermercado") || lower.startsWith("total") || lower.startsWith("subtotal") ||
                lower.startsWith("gracias por") || lower.startsWith("cajero") || lower.startsWith("nit") ||
                lower.startsWith("factura") || lower.startsWith("lista de compras") || lower.startsWith("despensa")
            ) {
                continue
            }

            var line = rawLine
                .replace(Regex("""^(?:[•\-\*–—>]|\d+[\.\)\-]|[\[\(][\sXx✓✔]?[\]\)])\s*"""), "")
                .trim()

            if (line.isBlank() || line.length < 2) continue

            // Detectar precio al final si existe (ej: "Leche Trebolac Q18.50" o "Frijol Ducal 12.00")
            var detectedPrice = 0.0
            val priceMatch = priceRegex.findAll(line).lastOrNull()
            if (priceMatch != null) {
                val pStr = priceMatch.groupValues[1].replace(",", ".")
                val pVal = pStr.toDoubleOrNull() ?: 0.0
                if (pVal > 0.0 && pVal < 10000.0) {
                    detectedPrice = pVal
                    // Quitar el precio del nombre
                    line = line.substring(0, priceMatch.range.first).trim()
                }
            }

            // Detectar cantidad y unidad al inicio
            // Ejemplos: "2 lbs de cebollas", "1 litro leche", "3 cajas cereal", "1/2 libra queso", "4 manzanas"
            var quantity = 1.0
            var unit = "Unidad"
            var name = line

            // Caso fracción "1/2"
            if (line.startsWith("1/2")) {
                quantity = 0.5
                line = line.removePrefix("1/2").trim()
            } else if (line.startsWith("1/4")) {
                quantity = 0.25
                line = line.removePrefix("1/4").trim()
            }

            val qtyUnitRegex = Regex("""^(\d+(?:[.,]\d+)?)\s*([a-zA-ZáéíóúÁÉÍÓÚñÑ]+)?(?:\s+de\s+|\s+)?(.*)""", RegexOption.IGNORE_CASE)
            val match = qtyUnitRegex.matchEntire(line)

            if (match != null) {
                val qVal = match.groupValues[1].replace(",", ".").toDoubleOrNull()
                if (qVal != null && qVal > 0.0 && qVal < 500.0) {
                    quantity = qVal
                }
                val rawUnit = match.groupValues[2].lowercase().trim()
                val remainingName = match.groupValues[3].trim()

                if (unitMap.containsKey(rawUnit)) {
                    unit = unitMap[rawUnit] ?: "Unidad"
                    name = remainingName.ifBlank { rawUnit }
                } else if (remainingName.isNotBlank()) {
                    name = "$rawUnit $remainingName".trim()
                } else if (rawUnit.isNotBlank()) {
                    name = rawUnit
                }
            } else {
                val firstWord = line.substringBefore(" ").lowercase().trim()
                if (unitMap.containsKey(firstWord)) {
                    unit = unitMap[firstWord] ?: "Unidad"
                    name = line.substringAfter(" ").trim()
                }
            }

            // Limpieza final del nombre
            name = name.replace(Regex("""^[de\s]+""", RegexOption.IGNORE_CASE), "")
                .trim()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

            if (name.length >= 2 && !name.all { it.isDigit() }) {
                result.add(
                    ScannedGroceryItem(
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        price = detectedPrice,
                        isSelected = true
                    )
                )
            }
        }

        return result
    }
}
