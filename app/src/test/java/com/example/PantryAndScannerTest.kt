package com.example

import com.example.data.model.SmartCartItemEntity
import com.example.ui.components.PantryItemStatus
import com.example.ui.util.ListScannerHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryAndScannerTest {

    @Test
    fun testPantryStatusCycleTransitions() {
        val initialStatus = PantryItemStatus.fromCode("PANTRY_VERDE")
        assertEquals(PantryItemStatus.GREEN, initialStatus)

        // Ciclo: VERDE -> AMARILLO -> ROJO -> VERDE
        fun getNextStatus(currentCode: String): String = when (currentCode) {
            "PANTRY_VERDE" -> "PANTRY_AMARILLO"
            "PANTRY_AMARILLO" -> "PANTRY_ROJO"
            else -> "PANTRY_VERDE"
        }

        val step1 = getNextStatus("PANTRY_VERDE")
        assertEquals("PANTRY_AMARILLO", step1)

        val step2 = getNextStatus(step1)
        assertEquals("PANTRY_ROJO", step2)

        val step3 = getNextStatus(step2)
        assertEquals("PANTRY_VERDE", step3)
    }

    @Test
    fun testDuplicatePreventionInPantry() {
        val pantry = mutableListOf(
            SmartCartItemEntity(id = 1, name = "Huevos de Granja", quantity = 1.0, unit = "Docena", priceSource = "PANTRY_VERDE"),
            SmartCartItemEntity(id = 2, name = "Frijoles Ducal", quantity = 2.0, unit = "Bolsa", priceSource = "PANTRY_AMARILLO")
        )

        // Intento de agregar "huevos de granja" (mismo nombre, diferente mayúscula/espacios)
        val incomingName = "  Huevos De Granja "
        val cleanName = incomingName.trim()
        val existingIndex = pantry.indexOfFirst { it.name.trim().equals(cleanName, ignoreCase = true) }

        assertTrue("Debe detectar el artículo existente", existingIndex != -1)

        // En lugar de duplicar, se actualiza o incrementa la cantidad
        val updated = pantry[existingIndex].copy(quantity = pantry[existingIndex].quantity + 1.0)
        pantry[existingIndex] = updated

        assertEquals(2, pantry.size) // No creció a 3 filas
        assertEquals(2.0, pantry[existingIndex].quantity, 0.001)
    }

    @Test
    fun testReplenishmentListGeneration() {
        val pantry = listOf(
            SmartCartItemEntity(id = 1, name = "Leche Trebolac", priceSource = "PANTRY_VERDE"),     // Suficiente -> NO se repone
            SmartCartItemEntity(id = 2, name = "Frijoles Ducal", priceSource = "PANTRY_AMARILLO"), // Poco -> SÍ se repone
            SmartCartItemEntity(id = 3, name = "Huevos", priceSource = "PANTRY_ROJO"),              // Agotado -> SÍ se repone
            SmartCartItemEntity(id = 4, name = "Arroz Gallo", priceSource = "PANTRY_AMARILLO")       // Ya está en carrito -> NO duplicar
        )

        val existingCart = listOf(
            SmartCartItemEntity(id = 10, name = "Arroz Gallo", isChecked = false)
        )

        // Filtrar productos a reponer
        val toReplenish = pantry.filter {
            it.priceSource == "PANTRY_AMARILLO" || it.priceSource == "PANTRY_ROJO"
        }
        assertEquals(3, toReplenish.size)

        val newCartItems = mutableListOf<SmartCartItemEntity>()
        for (item in toReplenish) {
            val alreadyInCart = existingCart.any { it.name.trim().equals(item.name.trim(), ignoreCase = true) }
            if (!alreadyInCart) {
                newCartItems.add(
                    item.copy(id = 0L, priceSource = "REPOSICION", isChecked = false)
                )
            }
        }

        // Solo Frijoles Ducal y Huevos deben agregarse, porque Arroz Gallo ya estaba en el carrito
        assertEquals(2, newCartItems.size)
        assertTrue(newCartItems.any { it.name == "Frijoles Ducal" })
        assertTrue(newCartItems.any { it.name == "Huevos" })
        assertFalse(newCartItems.any { it.name == "Arroz Gallo" })
    }

    @Test
    fun testPurchaseConfirmationUpdatesPantryToGreen() {
        val pantry = mutableListOf(
            SmartCartItemEntity(id = 1, name = "Leche Trebolac", priceSource = "PANTRY_ROJO"),
            SmartCartItemEntity(id = 2, name = "Huevos", priceSource = "PANTRY_AMARILLO"),
            SmartCartItemEntity(id = 3, name = "Jabón en Polvo", priceSource = "PANTRY_ROJO")
        )

        val purchasedNames = listOf("leche trebolac", "huevos")

        // Al confirmar compra, los productos comprados pasan a PANTRY_VERDE
        for (name in purchasedNames) {
            val idx = pantry.indexOfFirst { it.name.trim().equals(name.trim(), ignoreCase = true) }
            if (idx != -1) {
                pantry[idx] = pantry[idx].copy(priceSource = "PANTRY_VERDE")
            }
        }

        // Leche y Huevos ahora están verdes
        assertEquals("PANTRY_VERDE", pantry[0].priceSource)
        assertEquals("PANTRY_VERDE", pantry[1].priceSource)
        // Jabón en polvo NO se compró, permanece rojo
        assertEquals("PANTRY_ROJO", pantry[2].priceSource)
    }

    @Test
    fun testOcrListParsingWithGuatemalanFormats() {
        val rawOcrText = """
            Lista de compras
            1. 2 Litros de leche trebolac Q18.50
            • 3 lbs frijol negro
            - 1/2 libra queso fresco Q15.00
            * 1 caja cereal zucaritas
            4 aguacates
            Total aproximado
        """.trimIndent()

        val parsed = ListScannerHelper.parseOcrTextToItems(rawOcrText)

        assertEquals(5, parsed.size)

        // Item 1: Leche trebolac
        val item1 = parsed[0]
        assertEquals("Leche trebolac", item1.name)
        assertEquals(2.0, item1.quantity, 0.001)
        assertEquals("Litro", item1.unit)
        assertEquals(18.50, item1.price, 0.001)

        // Item 2: Frijol negro
        val item2 = parsed[1]
        assertEquals("Frijol negro", item2.name)
        assertEquals(3.0, item2.quantity, 0.001)
        assertEquals("Libra", item2.unit)

        // Item 3: Queso fresco
        val item3 = parsed[2]
        assertEquals("Queso fresco", item3.name)
        assertEquals(0.5, item3.quantity, 0.001)
        assertEquals("Libra", item3.unit)
        assertEquals(15.00, item3.price, 0.001)

        // Item 4: Cereal zucaritas
        val item4 = parsed[3]
        assertEquals("Cereal zucaritas", item4.name)
        assertEquals(1.0, item4.quantity, 0.001)
        assertEquals("Caja", item4.unit)

        // Item 5: Aguacates
        val item5 = parsed[4]
        assertEquals("Aguacates", item5.name)
        assertEquals(4.0, item5.quantity, 0.001)
        assertEquals("Unidad", item5.unit)
    }
}
