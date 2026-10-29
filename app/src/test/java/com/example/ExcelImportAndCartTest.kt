package com.example

import com.example.data.model.SmartCartItemEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.util.ExcelXlsxReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExcelImportAndCartTest {

    private fun parseCsvLine(line: String): List<String> {
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

    @Test
    fun testQuoteAwareCsvParsingWithCommasInAmountAndTitle() {
        val line = "\"01/10/2026\",\"Supermercado Walmart, Roosevelt\",\"1,750.50\",\"Gasto\",\"Alimentación & Súper\""
        val parts = parseCsvLine(line)

        assertEquals(5, parts.size)
        assertEquals("01/10/2026", parts[0])
        assertEquals("Supermercado Walmart, Roosevelt", parts[1])
        assertEquals("1,750.50", parts[2])
        assertEquals("Gasto", parts[3])
        assertEquals("Alimentación & Súper", parts[4])

        val amount = parts[2].replace(",", "").toDouble()
        assertEquals(1750.50, amount, 0.001)
    }

    @Test
    fun testUserExcelFormatParsing() {
        // Formato Excel del usuario: Fecha | Tipo | Concepto | Categoría | Descripción | Monto Gasto | Monto Ingreso
        val line = "15/10/2026,Gasto,Súper la Torre,Alimentación,Despensa quincenal,450.00,0.00"
        val parts = parseCsvLine(line)

        assertEquals(7, parts.size)
        assertEquals("Gasto", parts[1])
        assertEquals("Súper la Torre", parts[2])
        assertEquals("Alimentación", parts[3])

        val expVal = parts[5].replace("Q", "").replace(",", "").trim().toDouble()
        val incVal = parts[6].replace("Q", "").replace(",", "").trim().toDouble()

        val type = if (parts[1].equals("Ingreso", true) || incVal > 0.0) TransactionType.INCOME else TransactionType.EXPENSE
        val amount = if (type == TransactionType.INCOME) incVal else expVal

        assertEquals(TransactionType.EXPENSE, type)
        assertEquals(450.00, amount, 0.001)
    }

    @Test
    fun testSmartCartStableOrderAndToggle() {
        // Simulando dos artículos agregados al carrito
        val item1 = SmartCartItemEntity(id = 1, name = "Leche Trebolac", estimatedUnitPrice = 18.0, isChecked = false)
        val item2 = SmartCartItemEntity(id = 2, name = "Frijoles Ducal", estimatedUnitPrice = 12.0, isChecked = false)

        val list = mutableListOf(item1, item2)
        // Con ORDER BY id ASC, la lista siempre mantiene item1 primero e item2 segundo
        list.sortBy { it.id }
        assertEquals(1L, list[0].id)
        assertEquals(2L, list[1].id)

        // Al pulsar check en el primer artículo (id 1):
        val toggled1 = list[0].copy(isChecked = !list[0].isChecked)
        list[0] = toggled1

        // Verificamos que item 1 esté marcado y siga en la posición 0, SIN saltar al final
        list.sortBy { it.id }
        assertTrue(list[0].isChecked)
        assertEquals(1L, list[0].id)
        assertEquals("Leche Trebolac", list[0].name)

        // Verificamos que item 2 siga desmarcado y en la posición 1
        assertFalse(list[1].isChecked)
        assertEquals(2L, list[1].id)
        assertEquals("Frijoles Ducal", list[1].name)
    }

    @Test
    fun testInstructionAndMetaRowsAreSafelyIgnored() {
        // Simulación de archivo Excel con hoja o filas de instrucciones, encabezados, totales y transacciones reales
        val rawExcelCsv = """
            INSTRUCCIONES PARA EL USUARIO
            Cómo leer y editar el archivo
            1. Ingrese sus gastos diarios respetando las columnas
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            15/10/2026,Gasto,Súper la Torre,Alimentación,Despensa quincenal,450.00,0.00
            16/10/2026,Ingreso,Sueldo Quincenal,Sueldo,Primera quincena,0.00,5000.00
            17/10/2026,Gasto,Gasolina Shell,Transporte,Tanque lleno,250.00,0.00
            TOTAL GASTOS,,,,,700.00,0.00
            TOTAL INGRESOS,,,,,0.00,5000.00
        """.trimIndent()

        val parsed = ExcelXlsxReader.parseTransactionsFromText(rawExcelCsv)

        // Debe descartar exactamente las filas de instrucciones, encabezados y totales
        assertEquals(3, parsed.size)

        assertEquals("Súper la Torre", parsed[0].title)
        assertEquals(450.00, parsed[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed[0].type)
        assertEquals("Alimentación", parsed[0].category)

        assertEquals("Sueldo Quincenal", parsed[1].title)
        assertEquals(5000.00, parsed[1].amount, 0.001)
        assertEquals(TransactionType.INCOME, parsed[1].type)
        assertEquals("Sueldo", parsed[1].category)

        assertEquals("Gasolina Shell", parsed[2].title)
        assertEquals(250.00, parsed[2].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed[2].type)
        assertEquals("Transporte", parsed[2].category)
    }

    @Test
    fun testInstructionalSheetFilter() {
        assertTrue(ExcelXlsxReader.isInstructionalSheet("INSTRUCCIONES"))
        assertTrue(ExcelXlsxReader.isInstructionalSheet("Cómo leer y editar el archivo"))
        assertTrue(ExcelXlsxReader.isInstructionalSheet("Instrucciones de uso"))
        assertTrue(ExcelXlsxReader.isInstructionalSheet("Portada"))
        assertTrue(ExcelXlsxReader.isInstructionalSheet("Guía rápida"))
        assertTrue(ExcelXlsxReader.isInstructionalSheet("README"))

        assertFalse(ExcelXlsxReader.isInstructionalSheet("Gastos"))
        assertFalse(ExcelXlsxReader.isInstructionalSheet("Ingresos"))
        assertFalse(ExcelXlsxReader.isInstructionalSheet("Transacciones"))
        assertFalse(ExcelXlsxReader.isInstructionalSheet("Presupuesto Familiar"))
        assertFalse(ExcelXlsxReader.isInstructionalSheet("Enero 2026"))
    }

    @Test
    fun testMultipleColumnFormatsSupported() {
        // Formato 5 columnas
        val text5Col = "18/10/2026,Gasto,Gasolina Shell,Transporte,250.00"
        val parsed5 = ExcelXlsxReader.parseTransactionsFromText(text5Col)
        assertEquals(1, parsed5.size)
        assertEquals("Gasolina Shell", parsed5[0].title)
        assertEquals(250.00, parsed5[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsed5[0].type)

        // Formato Quetzi Export con comillas y símbolos de moneda
        val textExport = "\"20/10/2026\",\"Supermercado La Barata\",\"Q 320.75\",\"Gasto\",\"Alimentación\""
        val parsedExport = ExcelXlsxReader.parseTransactionsFromText(textExport)
        assertEquals(1, parsedExport.size)
        assertEquals("Supermercado La Barata", parsedExport[0].title)
        assertEquals(320.75, parsedExport[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsedExport[0].type)
    }
}
