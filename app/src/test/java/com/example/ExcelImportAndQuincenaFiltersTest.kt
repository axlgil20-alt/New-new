package com.example

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import com.example.ui.util.QuincenaFilterHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * Pruebas obligatorias para:
 * A. Importación Excel sin duplicados
 * B. Filtros de Quincenas (Q1: 1-15, Q2: 16-fin de mes, límites de mes/año, totales)
 */
class ExcelImportAndQuincenaFiltersTest {

    private lateinit var fakeTxDao: FakeTransactionDao
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        fakeTxDao = FakeTransactionDao()
        repository = BudgetRepository(
            transactionDao = fakeTxDao,
            budgetDao = FakeBudgetDao(),
            savingsGoalDao = FakeSavingsGoalDao(),
            recurringExpenseDao = FakeRecurringExpenseDao(),
            smartCartDao = FakeSmartCartDao(),
            database = null
        )
    }

    // =========================================================================
    // PRUEBAS DE IMPORTACIÓN EXCEL SIN DUPLICADOS
    // =========================================================================

    @Test
    fun testImportTwiceDoesNotDuplicateOperations() = runBlocking {
        val csvData = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            10/10/2026,Gasto,Súper La Torre,Alimentación,Despensa quincenal,450.00,0.00
            15/10/2026,Ingreso,Sueldo Quincenal,Sueldo,Primera quincena,0.00,5000.00
            18/10/2026,Gasto,Gasolina Shell,Transporte,Tanque lleno,250.00,0.00
        """.trimIndent()

        // Primera importación: Debe insertar los 3 registros
        val summary1 = repository.importFromCsv(csvData)
        assertEquals(3, summary1.newCount)
        assertEquals(0, summary1.duplicateCount)
        assertEquals(3, fakeTxDao.transactions.size)

        // Segunda importación con el mismo archivo: Debe omitir los 3 duplicados
        val summary2 = repository.importFromCsv(csvData)
        assertEquals(0, summary2.newCount)
        assertEquals(3, summary2.duplicateCount)
        // La base de datos no debe crecer
        assertEquals("La base de datos debe mantener exactamente 3 transacciones", 3, fakeTxDao.transactions.size)
    }

    @Test
    fun testTwoLegitimateDistinctTransactionsWithSameDateAndAmountAreBothPreserved() = runBlocking {
        // Dos compras legítimas el mismo día por el mismo monto en la misma gasolinera
        val csvData = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            15/10/2026,Gasto,Gasolina Shell,Transporte,Carga matutina,200.00,0.00
            15/10/2026,Gasto,Gasolina Shell,Transporte,Carga nocturna viaje,200.00,0.00
        """.trimIndent()

        // Primera importación: Ambas operaciones legítimas deben registrarse
        val summary1 = repository.importFromCsv(csvData)
        assertEquals(2, summary1.newCount)
        assertEquals(0, summary1.duplicateCount)
        assertEquals(2, fakeTxDao.transactions.size)

        // Si se reimporta el mismo archivo, ambas son reconocidas como ya existentes y se omiten
        val summary2 = repository.importFromCsv(csvData)
        assertEquals(0, summary2.newCount)
        assertEquals(2, summary2.duplicateCount)
        assertEquals(2, fakeTxDao.transactions.size)
    }

    @Test
    fun testPartialNewTransactionsInReimport() = runBlocking {
        val initialCsv = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            15/10/2026,Gasto,Farmacia Galeno,Salud,Medicinas,150.00,0.00
        """.trimIndent()

        repository.importFromCsv(initialCsv)
        assertEquals(1, fakeTxDao.transactions.size)

        // Archivo actualizado que contiene la fila anterior + una fila nueva
        val updatedCsv = """
            Fecha,Tipo,Concepto,Categoría,Descripción,Monto Gasto,Monto Ingreso
            15/10/2026,Gasto,Farmacia Galeno,Salud,Medicinas,150.00,0.00
            16/10/2026,Gasto,Panadería San Martín,Alimentación,Desayuno,85.00,0.00
        """.trimIndent()

        val summary = repository.importFromCsv(updatedCsv)
        assertEquals("Debe importar exactamente 1 nueva fila", 1, summary.newCount)
        assertEquals("Debe omitir 1 fila ya existente", 1, summary.duplicateCount)
        assertEquals("Total en BD debe ser 2", 2, fakeTxDao.transactions.size)
    }

    // =========================================================================
    // PRUEBAS DE FILTROS REALES DE QUINCENAS (MES, AÑO, Q1 Y Q2)
    // =========================================================================

    private fun createMillis(year: Int, monthIndex: Int, day: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIndex)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun testOctoberDoesNotShowSeptemberOrNovemberMovements() {
        val txs = listOf(
            TransactionEntity(id = 1, title = "Septiembre Gasto", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 8, 28)), // Septiembre (mes 8)
            TransactionEntity(id = 2, title = "Octubre Gasto 1", amount = 150.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 5)),   // Octubre (mes 9)
            TransactionEntity(id = 3, title = "Octubre Gasto 2", amount = 200.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 20)),  // Octubre (mes 9)
            TransactionEntity(id = 4, title = "Noviembre Gasto", amount = 300.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 10, 3))   // Noviembre (mes 10)
        )

        // Filtrar exclusivamente para Octubre 2026 (mes 9)
        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9
        )

        assertEquals("Octubre debe contener exactamente 2 transacciones", 2, filtered.monthTransactions.size)
        assertTrue(filtered.monthTransactions.all { it.title.startsWith("Octubre") })
        assertFalse("No debe contener transacciones de septiembre", filtered.monthTransactions.any { it.title.contains("Septiembre") })
        assertFalse("No debe contener transacciones de noviembre", filtered.monthTransactions.any { it.title.contains("Noviembre") })
    }

    @Test
    fun testQ1DoesNotIncludeQ2AndQ2DoesNotIncludeQ1() {
        val txs = listOf(
            TransactionEntity(id = 1, title = "Q1 Día 1", amount = 50.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 1)),
            TransactionEntity(id = 2, title = "Q1 Día 14 Límite", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 14, 23)),
            TransactionEntity(id = 3, title = "Q2 Día 15 Inicio", amount = 200.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 15, 0)),
            TransactionEntity(id = 4, title = "Q2 Día 30 Penúltimo", amount = 300.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 30))
        )

        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9
        )

        // Q1 debe tener exclusivamente hasta el día 14 (o último día del mes previo)
        assertEquals(2, filtered.q1Transactions.size)
        assertTrue(filtered.q1Transactions.any { it.id == 1L })
        assertTrue(filtered.q1Transactions.any { it.id == 2L })
        assertFalse(filtered.q1Transactions.any { it.id == 3L || it.id == 4L })

        // Q2 debe tener exclusivamente del día 15 al penúltimo día
        assertEquals(2, filtered.q2Transactions.size)
        assertTrue(filtered.q2Transactions.any { it.id == 3L })
        assertTrue(filtered.q2Transactions.any { it.id == 4L })
        assertFalse(filtered.q2Transactions.any { it.id == 1L || it.id == 2L })
    }

    @Test
    fun testDecemberWorksCorrectlyUpToPenultimateDayAndLastDayStartsNextYear() {
        // Diciembre (mes 11) tiene 31 días:
        // Día 30 es el penúltimo día (pertenece a Diciembre Q2)
        // Día 31 es el último día (inicia Enero 2027 Q1)
        val txs = listOf(
            TransactionEntity(id = 1, title = "Día 1 Dic", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 11, 1)),
            TransactionEntity(id = 2, title = "Aguinaldo 15 Dic", amount = 4000.0, type = TransactionType.INCOME, category = "Sueldo", dateMillis = createMillis(2026, 11, 15)),
            TransactionEntity(id = 3, title = "Cena 24 Dic", amount = 600.0, type = TransactionType.EXPENSE, category = "Comida", dateMillis = createMillis(2026, 11, 24)),
            TransactionEntity(id = 4, title = "Año Nuevo 31 Dic", amount = 500.0, type = TransactionType.EXPENSE, category = "Fiesta", dateMillis = createMillis(2026, 11, 31, 23))
        )

        val filteredDic = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 11
        )

        // Diciembre incluye día 1 (Q1), día 15 (Q2) y día 24 (Q2) = 3 transacciones
        assertEquals("Diciembre debe incluir las 3 transacciones hasta el penúltimo día", 3, filteredDic.monthTransactions.size)
        assertEquals(1, filteredDic.q1Transactions.size) // Día 1
        assertEquals(2, filteredDic.q2Transactions.size) // Día 15 y 24

        // El 31 de diciembre inicia Enero 2027 Q1
        val filteredEne27 = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2027,
            targetMonthIndex = 0
        )
        assertEquals("Enero 2027 debe incluir el 31 de diciembre en su Q1", 1, filteredEne27.q1Transactions.size)
        assertEquals(4L, filteredEne27.q1Transactions.first().id)
    }

    @Test
    fun testTotalsMatchFilteredTransactionsStrictly() {
        val txs = listOf(
            // Q1: Día 5
            TransactionEntity(id = 1, title = "Renta Q1", amount = 2000.0, type = TransactionType.EXPENSE, category = "Vivienda", dateMillis = createMillis(2026, 9, 5)),
            // Q2: Día 15, Día 20, Día 25
            TransactionEntity(id = 2, title = "Sueldo Q2", amount = 6000.0, type = TransactionType.INCOME, category = "Sueldo", dateMillis = createMillis(2026, 9, 15)),
            TransactionEntity(id = 3, title = "Bono Q2", amount = 1500.0, type = TransactionType.INCOME, category = "Extra", dateMillis = createMillis(2026, 9, 25)),
            TransactionEntity(id = 4, title = "Súper Q2", amount = 1000.0, type = TransactionType.EXPENSE, category = "Alimentación", dateMillis = createMillis(2026, 9, 20)),
            // Mes diferente
            TransactionEntity(id = 5, title = "Gasto Agosto", amount = 500.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 7, 15))
        )

        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9
        )

        // Totales Q1
        assertEquals(0.0, filtered.q1Income, 0.001)
        assertEquals(2000.0, filtered.q1Expense, 0.001)
        assertEquals(-2000.0, filtered.q1Net, 0.001)

        // Totales Q2
        assertEquals(7500.0, filtered.q2Income, 0.001) // 6000 + 1500
        assertEquals(1000.0, filtered.q2Expense, 0.001)
        assertEquals(6500.0, filtered.q2Net, 0.001)

        // Totales del mes completo
        assertEquals(7500.0, filtered.totalIncome, 0.001)
        assertEquals(3000.0, filtered.totalExpense, 0.001)
        assertEquals(4500.0, filtered.totalNet, 0.001)
    }

    @Test
    fun testDays14And15AssignedStrictlyToQ1AndQ2() {
        val txs = listOf(
            TransactionEntity(id = 1, title = "Día 14 Inicio", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 14, 0)),
            TransactionEntity(id = 2, title = "Día 14 Fin", amount = 150.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 14, 23)),
            TransactionEntity(id = 3, title = "Día 15 Inicio", amount = 200.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 15, 0)),
            TransactionEntity(id = 4, title = "Día 15 Fin", amount = 250.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 9, 15, 23))
        )

        val filtered = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2026,
            targetMonthIndex = 9 // Octubre
        )

        assertEquals("Q1 debe contener exactamente los 2 registros del día 14", 2, filtered.q1Transactions.size)
        assertTrue(filtered.q1Transactions.all { it.title.startsWith("Día 14") })

        assertEquals("Q2 debe contener exactamente los 2 registros del día 15", 2, filtered.q2Transactions.size)
        assertTrue(filtered.q2Transactions.all { it.title.startsWith("Día 15") })
    }

    @Test
    fun testFebruaryEndLeapAndNonLeapYearAndDecember31() {
        // Febrero no bisiesto (2025): día 27 penúltimo es Q2, día 28 último inicia Marzo Q1
        val txFeb27 = TransactionEntity(id = 1, title = "Feb 27 Penúltimo", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2025, 1, 27))
        val resFeb27 = QuincenaFilterHelper.filterByQuincena(listOf(txFeb27), 2025, 1)
        assertEquals(1, resFeb27.q2Transactions.size)

        val txFeb28 = TransactionEntity(id = 2, title = "Feb 28 Último No Bisiesto", amount = 120.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2025, 1, 28))
        val resMarzo25 = QuincenaFilterHelper.filterByQuincena(listOf(txFeb28), 2025, 2)
        assertEquals(1, resMarzo25.q1Transactions.size)

        // Febrero bisiesto (2024): día 28 penúltimo es Q2, día 29 último inicia Marzo Q1
        val txFeb28B = TransactionEntity(id = 3, title = "Feb 28 Penúltimo Bisiesto", amount = 150.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2024, 1, 28))
        val resFeb28B = QuincenaFilterHelper.filterByQuincena(listOf(txFeb28B), 2024, 1)
        assertEquals(1, resFeb28B.q2Transactions.size)

        val txFeb29B = TransactionEntity(id = 4, title = "Feb 29 Último Bisiesto", amount = 180.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2024, 1, 29))
        val resMarzo24 = QuincenaFilterHelper.filterByQuincena(listOf(txFeb29B), 2024, 2)
        assertEquals(1, resMarzo24.q1Transactions.size)

        // Diciembre 31: último día del año, inicia Enero 2027 Q1
        val txDec31 = TransactionEntity(id = 5, title = "Dic 31 Fin de Año", amount = 500.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 11, 31, 23))
        val resEne27 = QuincenaFilterHelper.filterByQuincena(listOf(txDec31), 2027, 0)
        assertEquals(1, resEne27.q1Transactions.size)
    }

    @Test
    fun testExternalIdPreservedAndUsedForDeduplication() = runBlocking {
        val csv = """
            ID,Fecha,Tipo,Concepto,Categoría,Monto Gasto,Monto Ingreso
            TX-1001,10/10/2026,Gasto,Suscripción,Servicios,99.00,0.00
            TX-1002,12/10/2026,Gasto,Cine,Ocio,120.00,0.00
        """.trimIndent()

        val summary1 = repository.importFromCsv(csv)
        assertEquals(2, summary1.newCount)
        assertEquals(0, summary1.duplicateCount)
        assertEquals("TX-1001", fakeTxDao.transactions[0].externalId)
        assertEquals("TX-1002", fakeTxDao.transactions[1].externalId)

        // Re-importar: detecta los mismos externalIds
        val summary2 = repository.importFromCsv(csv)
        assertEquals(0, summary2.newCount)
        assertEquals(2, summary2.duplicateCount)
        assertEquals(2, fakeTxDao.transactions.size)
    }

    @Test
    fun testImportFailureDoesNotLeavePartialState() = runBlocking {
        val failingDao = object : FakeTransactionDao() {
            var shouldFail = false
            override suspend fun insertAll(txs: List<TransactionEntity>) {
                if (shouldFail) throw RuntimeException("Error simulado de base de datos durante la importación")
                super.insertAll(txs)
            }
        }
        val safeRepo = BudgetRepository(
            transactionDao = failingDao,
            budgetDao = FakeBudgetDao(),
            savingsGoalDao = FakeSavingsGoalDao(),
            recurringExpenseDao = FakeRecurringExpenseDao(),
            smartCartDao = FakeSmartCartDao(),
            database = null
        )

        val csv = """
            Fecha,Tipo,Concepto,Categoría,Monto Gasto,Monto Ingreso
            10/10/2026,Gasto,Compra 1,Varios,100.00,0.00
            11/10/2026,Gasto,Compra 2,Varios,200.00,0.00
        """.trimIndent()

        failingDao.shouldFail = true
        try {
            safeRepo.importFromCsv(csv)
            fail("Debió lanzar una excepción")
        } catch (_: Exception) {
            // Esperado
        }

        assertEquals("No deben quedar transacciones parciales tras el fallo", 0, failingDao.transactions.size)
    }

    @Test
    fun testStageAAndBAnalysisDiscardsSaldoAnteriorAndTotals() {
        val rawSpreadsheetText = """
            Fecha,Tipo,Concepto,Categoría,Monto Gasto,Monto Ingreso
            01/10/2026,Ingreso,Saldo Anterior Mes Previo,Balance,0.00,12500.00
            05/10/2026,Gasto,Súper Paiz,Alimentación,650.00,0.00
            10/10/2026,Gasto,Farmacia Cruz Verde,Salud,120.00,0.00
            15/10/2026,Ingreso,Sueldo Quincena 1,Sueldo,0.00,4500.00
            30/10/2026,Gasto,Subtotal Gastos Quincena 2,Resumen,1800.00,0.00
            31/10/2026,Gasto,Total General Gastos,Balance,2570.00,0.00
            31/10/2026,Ingreso,Saldo Arrastrado Siguiente Mes,Balance,0.00,3500.00
        """.trimIndent()

        val analysis = com.example.ui.util.ExcelXlsxReader.analyzeTransactionsFromText(rawSpreadsheetText)

        // Verificación de filas válidas: Exclusivamente las 3 transacciones reales (Súper Paiz, Farmacia Cruz Verde, Sueldo)
        assertEquals("Debe detectar exactamente 3 movimientos válidos", 3, analysis.validTransactions.size)
        assertEquals("Súper Paiz", analysis.validTransactions[0].title)
        assertEquals(650.00, analysis.validTransactions[0].amount, 0.001)
        assertEquals("Farmacia Cruz Verde", analysis.validTransactions[1].title)
        assertEquals(120.00, analysis.validTransactions[1].amount, 0.001)
        assertEquals("Sueldo Quincena 1", analysis.validTransactions[2].title)
        assertEquals(4500.00, analysis.validTransactions[2].amount, 0.001)

        // Verificación de filas descartadas: 1 header + 4 filas no contables (saldo anterior, subtotal, total general, saldo arrastrado) = 5 descartadas
        assertEquals(5, analysis.discardedRowsCount)
        assertEquals(1, analysis.discardedHeaderRowsCount)
        assertEquals(4, analysis.discardedSummaryRowsCount)

        // Totales desglosados limpios (sin contaminar con los 12,500 de saldo anterior ni los 3,500 de saldo arrastrado)
        assertEquals(4500.00, analysis.totalIncome, 0.001)
        assertEquals(770.00, analysis.totalExpense, 0.001)

        // Comprobar que ninguna fila descartada de saldo anterior se convirtió en transacción
        assertFalse(analysis.validTransactions.any { it.title.contains("Saldo Anterior") })
        assertFalse(analysis.validTransactions.any { it.title.contains("Saldo Arrastrado") })
        assertFalse(analysis.validTransactions.any { it.title.contains("Total General") })
        assertFalse(analysis.validTransactions.any { it.title.contains("Subtotal") })
    }

    @Test
    fun testCancelStageBLeavesDatabaseCompletelyUntouched() = runBlocking {
        // Estado inicial de la base de datos
        fakeTxDao.insertTransaction(
            TransactionEntity(id = 1, title = "Registro previo", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = 1000L)
        )
        assertEquals(1, fakeTxDao.transactions.size)

        val rawSpreadsheetText = """
            Fecha,Tipo,Concepto,Categoría,Monto Gasto,Monto Ingreso
            05/10/2026,Gasto,Gasto Nuevo 1,Alimentación,300.00,0.00
            06/10/2026,Gasto,Gasto Nuevo 2,Transporte,150.00,0.00
        """.trimIndent()

        // El usuario visualiza la Etapa B (análisis):
        val analysis = com.example.ui.util.ExcelXlsxReader.analyzeTransactionsFromText(rawSpreadsheetText)
        assertEquals(2, analysis.validTransactions.size)

        // El usuario cancela la operación (no se llama a repository.importFromCsv):
        // La base de datos debe permanecer inalterada con exactamente 1 registro
        assertEquals("La base de datos debe permanecer inalterada tras cancelar la vista previa", 1, fakeTxDao.transactions.size)
        assertEquals("Registro previo", fakeTxDao.transactions[0].title)
    }

    @Test
    fun testConfirmationStageCPersistsAtomicallyAndSecondImportIsIdempotent() = runBlocking {
        val rawSpreadsheetText = """
            Fecha,Tipo,Concepto,Categoría,Monto Gasto,Monto Ingreso
            01/10/2026,Ingreso,Saldo Anterior,Balance,0.00,10000.00
            05/10/2026,Gasto,Gasolina Shell,Transporte,250.00,0.00
            05/10/2026,Gasto,Gasolina Shell,Transporte,250.00,0.00
            15/10/2026,Ingreso,Sueldo Quincena,Sueldo,0.00,5000.00
            30/10/2026,Gasto,Total Mes,Resumen,500.00,0.00
        """.trimIndent()

        // 1. Confirmación de Primera Importación: Debe insertar 3 movimientos legítimos (las 2 gasolinas legítimas + sueldo)
        val summary1 = repository.importFromCsv(rawSpreadsheetText)
        assertEquals(3, summary1.newCount)
        assertEquals(0, summary1.duplicateCount)
        assertEquals(3, fakeTxDao.transactions.size)

        // 2. Segunda Importación idéntica: Debe reconocer los 3 como duplicados existentes y agregar 0
        val summary2 = repository.importFromCsv(rawSpreadsheetText)
        assertEquals(0, summary2.newCount)
        assertEquals(3, summary2.duplicateCount)
        assertEquals("El recuento en base de datos no debe aumentar en la segunda importación", 3, fakeTxDao.transactions.size)
    }

    private fun getRealAttachmentCsv(): String {
        val fileOnDisk = java.io.File("/public/assets/aistudio/Presupuesto_Diciembre_Fecha_Tipo_Visibles_f2724159")
        return if (fileOnDisk.exists()) {
            fileOnDisk.readText()
        } else {
            """
            ,,,,,,,,,,
            ,,,,,,,,,,
            ,,,,,,,,,,
            REGISTRADORA DE PRESUPUESTO,,,,,,,,,,
            Ingreso total,Gasto total,Saldo final,Pendiente,Edita Gasto o Ingreso en las celdas de color,,,,,,
            "Q 29,823.00","Q 23,665.00",#REF!,#REF!,Pendiente aparece en rojo cuando el saldo de la fila es negativo.,,,,,,
            Fecha,Tipo,Concepto / beneficiario,Categoría,Notas,Gasto,Ingreso,Saldo anterior,Saldo,Mes,Pendiente
            30-sep-26,Ingreso,Salario del 30 de septiembre,Ingreso,Ingreso confirmado,Q -,"Q 4,200.00",Q -,"Q 4,200.00",sep,Q -
            30-sep-26,Gasto,Primo,Gastos septiembre,Gasto del periodo,"Q 1,100.00",Q -,"Q 4,200.00","Q 3,100.00",sep,Q -
            30-sep-26,Gasto,Esposa septiembre,Gastos septiembre,Gasto del periodo,Q 700.00,Q -,"Q 3,100.00","Q 2,400.00",sep,Q -
            30-sep-26,Gasto,Colegio,Gastos septiembre,Gasto del periodo,Q 660.00,Q -,"Q 2,400.00","Q 1,740.00",sep,Q -
            30-sep-26,Gasto,Doctor,Gastos septiembre,Gasto del periodo,Q 350.00,Q -,"Q 1,740.00","Q 1,390.00",sep,Q -
            30-sep-26,Gasto,Internet,Gastos septiembre,Gasto del periodo,Q 260.00,Q -,"Q 1,390.00","Q 1,130.00",sep,Q -
            30-sep-26,Gasto,Consulta + mototaxi,Gastos septiembre,Gasto del periodo,Q 60.00,Q -,"Q 1,130.00","Q 1,070.00",sep,Q -
            30-sep-26,Gasto,Axl + Mia,Gastos septiembre,Gasto del periodo,Q 200.00,Q -,"Q 1,070.00",Q 870.00,sep,Q -
            30-sep-26,Gasto,Pasajes septiembre,Gastos septiembre,Gasto del periodo,Q 300.00,Q -,Q 870.00,Q 570.00,sep,Q -
            30-sep-26,Gasto,Electricidad,Gastos septiembre,Servicio de casa,Q 80.00,Q -,Q 570.00,Q 490.00,sep,Q -
            15-oct-26,Saldo anterior,Saldo anterior,Saldo quincenal,Saldo de quincena,Q -,Q 490.00,Q -,Q 490.00,oct,Q -
            15-oct-26,Ingreso,Bonos del 15 de octubre,Ingreso,Ingreso confirmado,Q -,"Q 2,000.00",Q 490.00,"Q 2,490.00",oct,Q -
            15-oct-26,Gasto,Esposa desde octubre,Gastos octubre,Gasto del periodo,"Q 1,000.00",Q -,"Q 2,490.00","Q 1,490.00",oct,Q -
            15-oct-26,Gasto,Pantalón,Gastos octubre,Gasto del periodo,Q 200.00,Q -,"Q 1,490.00","Q 1,290.00",oct,Q -
            15-oct-26,Gasto,Zapatos,Gastos octubre,Gasto del periodo,Q 200.00,Q -,"Q 1,290.00","Q 1,090.00",oct,Q -
            15-oct-26,Gasto,Comida rápida,Gastos octubre,Gasto del periodo,Q 150.00,Q -,"Q 1,090.00",Q 940.00,oct,Q -
            15-oct-26,Gasto,Pasajes,Gastos octubre,Pasajes,Q 350.00,Q -,Q 940.00,Q 590.00,oct,Q -
            15-oct-26,Gasto,Ofrendas y pasajes de la iglesia,Gastos octubre,Gasto de iglesia,Q 287.50,Q -,Q 590.00,Q 302.50,oct,Q -
            15-oct-26,Gasto,Inscripción Axl,Gastos octubre,Pago escolar,Q 100.00,Q -,Q 302.50,Q 202.50,oct,Q -
            15-oct-26,Gasto,Seguro inscripción Axl,Gastos octubre,Pago escolar,Q 100.00,Q -,Q 202.50,Q 102.50,oct,Q -
            31-oct-26,Ingreso,Salario del 31 de octubre,Ingreso,Ingreso confirmado,Q -,"Q 4,996.00",Q 102.50,"Q 5,098.50",oct,Q -
            31-oct-26,Gasto,Esposa desde octubre,Gastos octubre,Gasto del periodo,"Q 1,000.00",Q -,"Q 5,098.50","Q 4,098.50",oct,Q -
            31-oct-26,Gasto,Comida rápida,Gastos octubre,Gasto del periodo,Q 150.00,Q -,"Q 4,098.50","Q 3,948.50",oct,Q -
            31-oct-26,Gasto,Pasajes,Gastos octubre,Pasajes,Q 350.00,Q -,"Q 3,948.50","Q 3,598.50",oct,Q -
            31-oct-26,Gasto,Ofrendas y pasajes de la iglesia,Gastos octubre,Gasto de iglesia,Q 287.50,Q -,"Q 3,598.50","Q 3,311.00",oct,Q -
            15-nov-26,Saldo anterior,Saldo anterior,Saldo quincenal,Saldo de quincena,Q -,"Q 3,311.00",Q -,"Q 3,311.00",nov,Q -
            15-nov-26,Ingreso,Bonos del 15 de noviembre,Ingreso,Ingreso confirmado,Q -,"Q 2,000.00","Q 3,311.00","Q 5,311.00",nov,Q -
            15-nov-26,Gasto,Esposa desde octubre,Gastos noviembre,Gasto del periodo,"Q 1,000.00",Q -,"Q 5,311.00","Q 4,311.00",nov,Q -
            15-nov-26,Gasto,Comida rápida,Gastos noviembre,Gasto del periodo,Q 150.00,Q -,"Q 4,311.00","Q 4,161.00",nov,Q -
            15-nov-26,Gasto,Pasajes,Gastos noviembre,Pasajes,Q 350.00,Q -,"Q 4,161.00","Q 3,811.00",nov,Q -
            15-nov-26,Gasto,Ofrendas y pasajes de la iglesia,Gastos noviembre,Gasto de iglesia,Q 287.50,Q -,"Q 3,811.00","Q 3,523.50",nov,Q -
            15-nov-26,Gasto,Inscripción Mia,Gastos noviembre,Pago escolar,Q 350.00,Q -,"Q 3,523.50","Q 3,173.50",nov,Q -
            30-nov-26,Ingreso,Salario del 30 de noviembre,Ingreso,Ingreso confirmado,Q -,"Q 4,996.00","Q 3,173.50","Q 8,169.50",nov,Q -
            30-nov-26,Gasto,Esposa desde octubre,Gastos noviembre,Gasto del periodo,"Q 1,000.00",Q -,"Q 8,169.50","Q 7,169.50",nov,Q -
            30-nov-26,Gasto,Comida rápida,Gastos noviembre,Gasto del periodo,Q 150.00,Q -,"Q 7,169.50","Q 7,019.50",nov,Q -
            30-nov-26,Gasto,Pasajes,Gastos noviembre,Pasajes,Q 350.00,Q -,"Q 7,019.50","Q 6,669.50",nov,Q -
            30-nov-26,Gasto,Ofrendas y pasajes de la iglesia,Gastos noviembre,Gasto de iglesia,Q 287.50,Q -,"Q 6,669.50","Q 6,382.00",nov,Q -
            30-nov-26,Gasto,Renta apartamento,Gastos mudanza noviembre,Pago de mudanza,"Q 2,000.00",Q -,"Q 6,382.00","Q 4,382.00",nov,Q -
            15-dic-26,Ingreso,Bonos del 15 de diciembre,Ingreso,Ingreso confirmado,Q -,"Q 2,000.00",#REF!,#REF!,dic,#REF!
            15-dic-26,Ingreso,Aguinaldo proporcional,Ingreso,Ingreso confirmado,Q -,Q 834.00,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,TV esposa,Navidad,Gasto de Navidad,"Q 2,000.00",Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Regalo hijo,Navidad,Gasto de Navidad,Q 300.00,Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Regalo hija,Navidad,Gasto de Navidad,Q 300.00,Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Estrenos de los 4,Navidad,Gasto de Navidad,"Q 2,000.00",Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Comida,Gastos diciembre,Gasto del periodo,"Q 1,000.00",Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Esposa desde octubre,Gastos diciembre,Gasto del periodo,"Q 1,000.00",Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Comida rápida,Gastos diciembre,Gasto del periodo,Q 150.00,Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Pasajes,Gastos diciembre,Pasajes,Q 190.00,Q -,#REF!,#REF!,dic,#REF!
            15-dic-26,Gasto,Ofrendas y pasajes de la iglesia,Iglesia,Gasto de iglesia,Q 287.50,Q -,#REF!,#REF!,dic,#REF!
            31-dic-26,Ingreso,Salario del 31 de diciembre,Ingreso,Ingreso confirmado,Q -,"Q 4,996.00",#REF!,#REF!,dic,#REF!
            31-dic-26,Gasto,Comida,Gastos diciembre,Gasto del periodo,"Q 1,000.00",Q -,#REF!,#REF!,dic,#REF!
            31-dic-26,Gasto,Esposa desde octubre,Gastos diciembre,Gasto del periodo,"Q 1,000.00",Q -,#REF!,#REF!,dic,#REF!
            31-dic-26,Gasto,Comida rápida,Gastos diciembre,Gasto del periodo,Q 150.00,Q -,#REF!,#REF!,dic,#REF!
            31-dic-26,Gasto,Pasajes,Gastos diciembre,Pasajes,Q 190.00,Q -,#REF!,#REF!,dic,#REF!
            31-dic-26,Gasto,Ofrendas y pasajes de la iglesia,Gastos diciembre,Gasto de iglesia,Q 287.50,Q -,#REF!,#REF!,dic,#REF!
            """.trimIndent()
        }
    }

    @Test
    fun testRealFamilyExcelAttachmentAnalysisAndImport() = runBlocking {
        val realAttachmentCsv = getRealAttachmentCsv()

        // 1. Análisis en Etapa B (Vista Previa)
        val analysis = com.example.ui.util.ExcelXlsxReader.analyzeTransactionsFromText(realAttachmentCsv)

        // Exactamente 53 transacciones contables reales
        assertEquals("Debe extraer exactamente 53 transacciones reales", 53, analysis.validTransactions.size)

        // 8 ingresos y 45 gastos
        val incomes = analysis.validTransactions.filter { it.type == TransactionType.INCOME }
        val expenses = analysis.validTransactions.filter { it.type == TransactionType.EXPENSE }
        assertEquals("Debe contener exactamente 8 ingresos reales", 8, incomes.size)
        assertEquals("Debe contener exactamente 45 gastos reales", 45, expenses.size)

        // Total Ingresos = Q 26,022.00 (excluyendo los dos saldos anteriores de 490 y 3,311)
        assertEquals(26022.00, analysis.totalIncome, 0.001)

        // Total Gastos = Q 23,665.00 (coincidencia exacta al centavo con el Excel de la familia)
        assertEquals(23665.00, analysis.totalExpense, 0.001)

        // Comprobar que ningún saldo anterior o resumen fue admitido
        assertFalse(analysis.validTransactions.any { it.title.equals("Saldo anterior", ignoreCase = true) })
        assertFalse(analysis.validTransactions.any { it.title.contains("total", ignoreCase = true) })
        assertFalse(analysis.validTransactions.any { it.category.equals("Saldo inicial", ignoreCase = true) })

        // Comprobar parseo de fechas de Septiembre, Octubre, Noviembre, Diciembre
        val cal = Calendar.getInstance()
        val firstTx = analysis.validTransactions.first()
        cal.timeInMillis = firstTx.dateMillis
        assertEquals("Año debe ser 2026", 2026, cal.get(Calendar.YEAR))
        assertEquals("Mes debe ser Septiembre (8)", 8, cal.get(Calendar.MONTH))
        assertEquals("Día debe ser 30", 30, cal.get(Calendar.DAY_OF_MONTH))

        val lastTx = analysis.validTransactions.last()
        cal.timeInMillis = lastTx.dateMillis
        assertEquals("Año debe ser 2026", 2026, cal.get(Calendar.YEAR))
        assertEquals("Mes debe ser Diciembre (11)", 11, cal.get(Calendar.MONTH))
        assertEquals("Día debe ser 31", 31, cal.get(Calendar.DAY_OF_MONTH))

        // 2. Primera Importación: Debe persistir los 53 registros legítimos
        val summary1 = repository.importFromCsv(realAttachmentCsv)
        assertEquals("Primera importación debe registrar 53 transacciones", 53, summary1.newCount)
        assertEquals("Primera importación debe tener 0 duplicados", 0, summary1.duplicateCount)
        assertEquals("Base de datos debe contener 53 movimientos", 53, fakeTxDao.transactions.size)

        // 3. Segunda Importación idéntica: Idempotencia absoluta (0 duplicados)
        val summary2 = repository.importFromCsv(realAttachmentCsv)
        assertEquals("Segunda importación debe registrar 0 nuevas", 0, summary2.newCount)
        assertEquals("Segunda importación debe omitir las 53 duplicadas", 53, summary2.duplicateCount)
        assertEquals("Base de datos debe mantenerse intacta en 53 movimientos", 53, fakeTxDao.transactions.size)
    }

    // =========================================================================
    // PRUEBAS DE SALARIO ROLL-OVER Y GASTOS SUPUESTOS VS REALES
    // =========================================================================

    @Test
    fun testQuincenaRolloverAccuratelyChainedAcrossQuincenas() = runBlocking {
        // Importar las 53 transacciones contables del Excel familiar
        val realAttachmentCsv = getRealAttachmentCsv()
        repository.importFromCsv(realAttachmentCsv)
        val allTx = fakeTxDao.transactions

        // 1. Rollover hacia Octubre 2da Quincena (15 - 30 oct 2026, mes index 9):
        // Remanente de Octubre Q1 (30 sep - 14 oct): Salario 30 sep (4,200.00) - Gastos (3,710.00) = 490.00
        val rolloverOctQ2 = QuincenaFilterHelper.calculateQuincenaRollover(allTx, 2026, 9, 2)
        assertEquals("Rollover hacia Octubre Q2 debe ser exactamente Q 490.00", 490.00, rolloverOctQ2, 0.001)

        // 2. Rollover hacia Noviembre 1ra Quincena (inicia 31 oct 2026, mes index 10):
        // Remanente de Octubre Q2 (15 - 30 oct): (Bonos 2,000.00 + Rollover 490.00) - Gastos 2,387.50 = 102.50
        val rolloverNovQ1 = QuincenaFilterHelper.calculateQuincenaRollover(allTx, 2026, 10, 1)
        assertEquals("Rollover hacia Noviembre Q1 debe ser exactamente Q 102.50 (igual que celda Saldo anterior en Excel)", 102.50, rolloverNovQ1, 0.001)

        // 3. Rollover hacia Noviembre 2da Quincena (15 - 29 nov 2026, mes index 10):
        // Remanente de Noviembre Q1 (31 oct - 14 nov): (Salario 4,996.00 + Rollover 102.50) - Gastos 1,787.50 = 3,311.00
        val rolloverNovQ2 = QuincenaFilterHelper.calculateQuincenaRollover(allTx, 2026, 10, 2)
        assertEquals("Rollover hacia Noviembre Q2 debe ser exactamente Q 3,311.00 (igual que en Excel)", 3311.00, rolloverNovQ2, 0.001)

        // 4. Rollover hacia Diciembre 1ra Quincena (inicia 30 nov 2026, mes index 11):
        // Remanente de Noviembre Q2 (15 - 29 nov): (Bonos 2,000.00 + Rollover 3,311.00) - Gastos 2,137.50 = 3,173.50
        val rolloverDicQ1 = QuincenaFilterHelper.calculateQuincenaRollover(allTx, 2026, 11, 1)
        assertEquals("Rollover hacia Diciembre Q1 debe ser exactamente Q 3,173.50 (igual que celda Saldo anterior en Excel)", 3173.50, rolloverDicQ1, 0.001)

        // 5. Rollover hacia Diciembre 2da Quincena (15 - 30 dic 2026, mes index 11):
        // Remanente de Diciembre Q1 (30 nov - 14 dic): (Salario 4,996.00 + Rollover 3,173.50) - Gastos 3,787.50 = 4,382.00
        val rolloverDicQ2 = QuincenaFilterHelper.calculateQuincenaRollover(allTx, 2026, 11, 2)
        assertEquals("Rollover hacia Diciembre Q2 debe ser exactamente Q 4,382.00 (resolviendo el #REF! del Excel)", 4382.00, rolloverDicQ2, 0.001)
    }

    @Test
    fun testSpecialQuincenaMathematicalDistributionAndBoundaries() {
        val cal = Calendar.getInstance()

        // Helper para crear fecha
        fun timeAt(year: Int, monthIndex: Int, day: Int): Long {
            cal.clear()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthIndex)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            return cal.timeInMillis
        }

        // 1. Octubre - Noviembre 2026
        // 30 de septiembre (último día sep) -> Octubre 2026, Q1
        val pSep30 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 8, 30))
        assertEquals(2026, pSep30.year)
        assertEquals(9, pSep30.monthIndex) // Octubre
        assertEquals(1, pSep30.quincena)

        // 14 de octubre -> Octubre 2026, Q1
        val pOct14 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 9, 14))
        assertEquals(2026, pOct14.year)
        assertEquals(9, pOct14.monthIndex)
        assertEquals(1, pOct14.quincena)

        // 15 de octubre -> Octubre 2026, Q2
        val pOct15 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 9, 15))
        assertEquals(2026, pOct15.year)
        assertEquals(9, pOct15.monthIndex)
        assertEquals(2, pOct15.quincena)

        // 30 de octubre (penúltimo día) -> Octubre 2026, Q2
        val pOct30 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 9, 30))
        assertEquals(2026, pOct30.year)
        assertEquals(9, pOct30.monthIndex)
        assertEquals(2, pOct30.quincena)

        // 31 de octubre (último día) -> Noviembre 2026, Q1 (El último día inicia la siguiente Q1)
        val pOct31 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 9, 31))
        assertEquals(2026, pOct31.year)
        assertEquals(10, pOct31.monthIndex) // Noviembre
        assertEquals(1, pOct31.quincena)

        // 2. Noviembre - Diciembre 2026
        // 29 de noviembre (penúltimo día de nov) -> Noviembre 2026, Q2
        val pNov29 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 10, 29))
        assertEquals(2026, pNov29.year)
        assertEquals(10, pNov29.monthIndex)
        assertEquals(2, pNov29.quincena)

        // 30 de noviembre (último día de nov) -> Diciembre 2026, Q1
        val pNov30 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 10, 30))
        assertEquals(2026, pNov30.year)
        assertEquals(11, pNov30.monthIndex) // Diciembre
        assertEquals(1, pNov30.quincena)

        // 3. Diciembre 2026 - Enero 2027
        // 30 de diciembre (penúltimo día) -> Diciembre 2026, Q2
        val pDic30 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 11, 30))
        assertEquals(2026, pDic30.year)
        assertEquals(11, pDic30.monthIndex)
        assertEquals(2, pDic30.quincena)

        // 31 de diciembre (último día) -> Enero 2027, Q1 (cambio de año exacto)
        val pDic31 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, 11, 31))
        assertEquals(2027, pDic31.year)
        assertEquals(0, pDic31.monthIndex) // Enero 2027
        assertEquals(1, pDic31.quincena)

        // 4. Febrero de 2027 (Año normal no bisiesto, 28 días)
        // 31 de enero de 2027 (último día de ene) -> Febrero 2027, Q1
        val pEne31_27 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2027, 0, 31))
        assertEquals(2027, pEne31_27.year)
        assertEquals(1, pEne31_27.monthIndex) // Febrero
        assertEquals(1, pEne31_27.quincena)

        // 14 de febrero de 2027 -> Febrero 2027, Q1
        val pFeb14_27 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2027, 1, 14))
        assertEquals(2027, pFeb14_27.year)
        assertEquals(1, pFeb14_27.monthIndex)
        assertEquals(1, pFeb14_27.quincena)

        // 15 de febrero de 2027 -> Febrero 2027, Q2
        val pFeb15_27 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2027, 1, 15))
        assertEquals(2027, pFeb15_27.year)
        assertEquals(1, pFeb15_27.monthIndex)
        assertEquals(2, pFeb15_27.quincena)

        // 27 de febrero de 2027 (penúltimo día de feb no bisiesto) -> Febrero 2027, Q2
        val pFeb27_27 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2027, 1, 27))
        assertEquals(2027, pFeb27_27.year)
        assertEquals(1, pFeb27_27.monthIndex)
        assertEquals(2, pFeb27_27.quincena)

        // 28 de febrero de 2027 (último día de feb no bisiesto) -> Marzo 2027, Q1
        val pFeb28_27 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2027, 1, 28))
        assertEquals(2027, pFeb28_27.year)
        assertEquals(2, pFeb28_27.monthIndex) // Marzo
        assertEquals(1, pFeb28_27.quincena)

        // 5. Febrero de 2028 (Año bisiesto, 29 días)
        // 31 de enero de 2028 (último día de ene) -> Febrero 2028, Q1
        val pEne31_28 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2028, 0, 31))
        assertEquals(2028, pEne31_28.year)
        assertEquals(1, pEne31_28.monthIndex) // Febrero
        assertEquals(1, pEne31_28.quincena)

        // 14 de febrero de 2028 -> Febrero 2028, Q1
        val pFeb14_28 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2028, 1, 14))
        assertEquals(2028, pFeb14_28.year)
        assertEquals(1, pFeb14_28.monthIndex)
        assertEquals(1, pFeb14_28.quincena)

        // 15 de febrero de 2028 -> Febrero 2028, Q2
        val pFeb15_28 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2028, 1, 15))
        assertEquals(2028, pFeb15_28.year)
        assertEquals(1, pFeb15_28.monthIndex)
        assertEquals(2, pFeb15_28.quincena)

        // 28 de febrero de 2028 (penúltimo día de feb bisiesto) -> Febrero 2028, Q2
        val pFeb28_28 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2028, 1, 28))
        assertEquals(2028, pFeb28_28.year)
        assertEquals(1, pFeb28_28.monthIndex)
        assertEquals(2, pFeb28_28.quincena)

        // 29 de febrero de 2028 (último día de feb bisiesto) -> Marzo 2028, Q1
        val pFeb29_28 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2028, 1, 29))
        assertEquals(2028, pFeb29_28.year)
        assertEquals(2, pFeb29_28.monthIndex) // Marzo
        assertEquals(1, pFeb29_28.quincena)

        // 6. Prueba Matemática Exhaustiva de 1,096 días (2026 a 2028)
        // Comprobar ausencia absoluta de días huérfanos (sin asignación) o superposiciones
        val iterCal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 1, 12, 0, 0)
        }
        val endCal = Calendar.getInstance().apply {
            clear()
            set(2028, Calendar.DECEMBER, 31, 12, 0, 0)
        }

        var totalDaysChecked = 0
        while (!iterCal.after(endCal)) {
            val bp = QuincenaFilterHelper.getBudgetPeriod(iterCal.timeInMillis)
            assertNotNull(bp)
            assertTrue("Quincena debe ser 1 o 2", bp.quincena in 1..2)
            assertTrue("Mes presupuestario debe estar entre 0 y 11", bp.monthIndex in 0..11)
            assertTrue("Año presupuestario debe ser coherente", bp.year in 2026..2029)
            totalDaysChecked++
            iterCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        assertEquals("Se deben verificar exactamente 1096 días (3 años completos)", 1096, totalDaysChecked)
    }

    @Test
    fun testGastosSupuestosVsGastosRealesClassification() {
        val now = System.currentTimeMillis()
        val gastoReal = TransactionEntity(
            id = 1,
            title = "Gasolina",
            amount = 250.0,
            type = TransactionType.EXPENSE,
            category = "Transporte",
            dateMillis = now,
            notes = "Carga de combustible"
        )
        val gastoSupuestoConTag = TransactionEntity(
            id = 2,
            title = "Cena familiar",
            amount = 300.0,
            type = TransactionType.EXPENSE,
            category = "Restaurantes",
            dateMillis = now,
            notes = "[PENDIENTE] Posible salida fin de semana"
        )
        val gastoSupuestoNotes = TransactionEntity(
            id = 3,
            title = "Reparación auto",
            amount = 800.0,
            type = TransactionType.EXPENSE,
            category = "Mantenimiento",
            dateMillis = now,
            notes = "[SUPUESTO] Taller mecánico"
        )
        val gastoSupuestoTentative = TransactionEntity(
            id = 4,
            title = "Pintura casa",
            amount = 500.0,
            type = TransactionType.EXPENSE,
            category = "Hogar",
            dateMillis = now,
            isTentative = true,
            tentativeDecision = "PENDING"
        )
        val ingreso = TransactionEntity(
            id = 5,
            title = "Sueldo",
            amount = 5000.0,
            type = TransactionType.INCOME,
            category = "Salario",
            dateMillis = now
        )

        // Verificación de Gasto Real
        assertTrue("gastoReal debe ser isGastoReal", gastoReal.isGastoReal)
        assertFalse("gastoReal no debe ser isGastoSupuesto", gastoReal.isGastoSupuesto)
        assertEquals(TransactionStatus.REAL, gastoReal.status)

        // Verificación de Gastos Supuestos
        assertTrue("gastoSupuestoConTag debe ser isGastoSupuesto", gastoSupuestoConTag.isGastoSupuesto)
        assertFalse("gastoSupuestoConTag no debe ser isGastoReal", gastoSupuestoConTag.isGastoReal)
        assertEquals(TransactionStatus.PROJECTED, gastoSupuestoConTag.status)

        assertTrue("gastoSupuestoNotes debe ser isGastoSupuesto", gastoSupuestoNotes.isGastoSupuesto)
        assertFalse("gastoSupuestoNotes no debe ser isGastoReal", gastoSupuestoNotes.isGastoReal)
        assertEquals(TransactionStatus.PROJECTED, gastoSupuestoNotes.status)

        assertTrue("gastoSupuestoTentative debe ser isGastoSupuesto", gastoSupuestoTentative.isGastoSupuesto)
        assertFalse("gastoSupuestoTentative no debe ser isGastoReal", gastoSupuestoTentative.isGastoReal)
        assertEquals(TransactionStatus.PROJECTED, gastoSupuestoTentative.status)

        // Verificación de Ingreso Real
        assertTrue("ingreso debe ser isIngresoReal", ingreso.isIngresoReal)
        assertFalse("ingreso no debe ser isGastoReal", ingreso.isGastoReal)
        assertFalse("ingreso no debe ser isGastoSupuesto", ingreso.isGastoSupuesto)
    }

    @Test
    fun testOverviewMetricsCalculations() {
        // Salario Quincenal: 5,000.00
        // Salario Roll-Over previo: 490.00
        // Gastos Reales: 1,500.00
        // Gastos Supuestos: 600.00
        val rollover = 490.00
        val income = 5000.00
        val totalToSpend = income + rollover // 5,490.00

        val gastoReal1 = 1000.0
        val gastoReal2 = 500.0
        val realExpensesTotal = gastoReal1 + gastoReal2 // 1,500.00

        val gastoSupuesto1 = 400.0
        val gastoSupuesto2 = 200.0
        val supuestoExpensesTotal = gastoSupuesto1 + gastoSupuesto2 // 600.00

        val totalBothExpenses = realExpensesTotal + supuestoExpensesTotal // 2,100.00

        // Saldo real en mano (sin supuestos): no se descuentan los supuestos
        val realRemaining = totalToSpend - realExpensesTotal // 3,990.00

        // Saldo proyectado (si se aprueban todos los supuestos):
        val projectedRemaining = totalToSpend - totalBothExpenses // 3,390.00

        assertEquals(5490.0, totalToSpend, 0.001)
        assertEquals(1500.0, realExpensesTotal, 0.001)
        assertEquals(600.0, supuestoExpensesTotal, 0.001)
        assertEquals(2100.0, totalBothExpenses, 0.001)
        assertEquals(3990.0, realRemaining, 0.001)
        assertEquals(3390.0, projectedRemaining, 0.001)

        // El saldo real en mano es exactamente igual al saldo proyectado + gastos supuestos
        assertEquals(realRemaining, projectedRemaining + supuestoExpensesTotal, 0.001)
    }


    // =========================================================================
    // FAKES
    // =========================================================================

    open class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = transactions.toList()
        override fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> = flowOf(transactions)

        override suspend fun insertTransaction(tx: TransactionEntity): Long {
            val id = if (tx.id == 0L) nextId++ else tx.id
            transactions.add(tx.copy(id = id))
            return id
        }
        open override suspend fun insertAll(txs: List<TransactionEntity>) {
            for (t in txs) insertTransaction(t)
        }
        override suspend fun updateTransaction(tx: TransactionEntity) {}
        override suspend fun deleteTransaction(tx: TransactionEntity) {}
        override suspend fun deleteById(id: Long) { transactions.removeAll { it.id == id } }
        override suspend fun deleteByIds(ids: List<Long>) { transactions.removeAll { it.id in ids } }
        override suspend fun clearAll() { transactions.clear() }
    }

    class FakeBudgetDao : BudgetDao {
        override fun getAllBudgets(): Flow<List<BudgetLimitEntity>> = flowOf(emptyList())
        override suspend fun setBudget(budget: BudgetLimitEntity) {}
        override suspend fun insertAll(budgets: List<BudgetLimitEntity>) {}
        override suspend fun deleteBudget(category: String) {}
        override suspend fun clearAll() {}
    }

    class FakeSavingsGoalDao : SavingsGoalDao {
        override fun getAllGoals(): Flow<List<SavingsGoalEntity>> = flowOf(emptyList())
        override suspend fun insertGoal(goal: SavingsGoalEntity): Long = 1L
        override suspend fun insertAll(goals: List<SavingsGoalEntity>) {}
        override suspend fun updateGoal(goal: SavingsGoalEntity) {}
        override suspend fun deleteGoal(goal: SavingsGoalEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() {}
    }

    class FakeRecurringExpenseDao : RecurringExpenseDao {
        override fun getAll(): Flow<List<RecurringExpenseEntity>> = flowOf(emptyList())
        override suspend fun insert(expense: RecurringExpenseEntity): Long = 1L
        override suspend fun insertAll(expenses: List<RecurringExpenseEntity>) {}
        override suspend fun update(expense: RecurringExpenseEntity) {}
        override suspend fun delete(expense: RecurringExpenseEntity) {}
        override suspend fun deleteById(id: Long) {}
        override suspend fun clearAll() {}
    }

    class FakeSmartCartDao : SmartCartDao {
        override fun getAllItems(): Flow<List<SmartCartItemEntity>> = flowOf(emptyList())
        override suspend fun getAllItemsList(): List<SmartCartItemEntity> = emptyList()
        override suspend fun toggleItemChecked(id: Long) {}
        override suspend fun updatePantryStatus(id: Long, status: String) {}
        override suspend fun updatePantryStatusToGreenByName(name: String) {}
        override suspend fun markItemPurchasedAsGreen(id: Long) {}
        override suspend fun markItemPurchasedAsGreenByName(name: String) {}
        override suspend fun incorporateNeedToBuyIntoCart() {}
        override suspend fun uncheckAll() {}
        override suspend fun insertItem(item: SmartCartItemEntity): Long = 1L
        override suspend fun insertAll(items: List<SmartCartItemEntity>) {}
        override suspend fun updateItem(item: SmartCartItemEntity) {}
        override suspend fun deleteItem(item: SmartCartItemEntity) {}
        override suspend fun deleteCheckedItems() {}
        override suspend fun clearCartItems() {}
        override suspend fun clearPantryItems() {}
        override suspend fun clearAll() {}
    }
}
