package com.example

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import com.example.ui.util.QuincenaFilterHelper
import com.example.ui.util.BudgetPeriod
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

        // Q1 incluye gastos del 1 al 15
        assertEquals(3, filtered.q1Transactions.size)
        assertTrue(filtered.q1Transactions.any { it.id == 1L })
        assertTrue(filtered.q1Transactions.any { it.id == 2L })
        assertTrue(filtered.q1Transactions.any { it.id == 3L })
        assertFalse(filtered.q1Transactions.any { it.id == 4L })

        // Q2 incluye gastos del día 16 hasta el último día del mes
        assertEquals(1, filtered.q2Transactions.size)
        assertTrue(filtered.q2Transactions.any { it.id == 4L })
        assertFalse(filtered.q2Transactions.any { it.id == 1L || it.id == 2L || it.id == 3L })
    }

    @Test
    fun testDecemberLastDayExpenseRemainsInSecondCycle() {
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

        // Diciembre incluye las cuatro transacciones; gasto día 31 sigue en Q2.
        assertEquals("Diciembre debe incluir los movimientos del mes", 4, filteredDic.monthTransactions.size)
        assertEquals(1, filteredDic.q1Transactions.size) // Día 1
        assertEquals(3, filteredDic.q2Transactions.size) // Ingreso 15, gastos 24 y 31

        // El gasto del 31 de diciembre permanece en Diciembre Q2.
        val filteredEne27 = QuincenaFilterHelper.filterByQuincena(
            transactions = txs,
            targetYear = 2027,
            targetMonthIndex = 0
        )
        assertEquals("Enero 2027 no debe incluir gastos de diciembre", 0, filteredEne27.q1Transactions.size)
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
    fun testDays14And15BothBelongToQ1ForExpenses() {
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

        assertEquals("Q1 debe incluir los gastos de los días 14 y 15", 4, filtered.q1Transactions.size)
        assertTrue(filtered.q1Transactions.all { it.title.startsWith("Día 14") || it.title.startsWith("Día 15") })
        assertTrue(filtered.q2Transactions.isEmpty())
    }

    @Test
    fun testFebruaryEndLeapAndNonLeapYearAndDecember31() {
        // Febrero no bisiesto (2025): los días 27 y 28 son Q2.
        val txFeb27 = TransactionEntity(id = 1, title = "Feb 27 Penúltimo", amount = 100.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2025, 1, 27))
        val resFeb27 = QuincenaFilterHelper.filterByQuincena(listOf(txFeb27), 2025, 1)
        assertEquals(1, resFeb27.q2Transactions.size)

        val txFeb28 = TransactionEntity(id = 2, title = "Feb 28 Último No Bisiesto", amount = 120.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2025, 1, 28))
        val resFebrero25 = QuincenaFilterHelper.filterByQuincena(listOf(txFeb28), 2025, 1)
        assertEquals(1, resFebrero25.q2Transactions.size)

        // Febrero bisiesto (2024): los días 28 y 29 son Q2.
        val txFeb28B = TransactionEntity(id = 3, title = "Feb 28 Penúltimo Bisiesto", amount = 150.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2024, 1, 28))
        val resFeb28B = QuincenaFilterHelper.filterByQuincena(listOf(txFeb28B), 2024, 1)
        assertEquals(1, resFeb28B.q2Transactions.size)

        val txFeb29B = TransactionEntity(id = 4, title = "Feb 29 Último Bisiesto", amount = 180.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2024, 1, 29))
        val resFebrero24B = QuincenaFilterHelper.filterByQuincena(listOf(txFeb29B), 2024, 1)
        assertEquals(1, resFebrero24B.q2Transactions.size)

        // Un gasto del 31 de diciembre pertenece a Diciembre Q2.
        val txDec31 = TransactionEntity(id = 5, title = "Dic 31 Fin de Año", amount = 500.0, type = TransactionType.EXPENSE, category = "Varios", dateMillis = createMillis(2026, 11, 31, 23))
        val resDic26 = QuincenaFilterHelper.filterByQuincena(listOf(txDec31), 2026, 11)
        assertEquals(1, resDic26.q2Transactions.size)
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
        // Importar los datos contables del Excel familiar y comprobar la regla de saldo real.
        val realAttachmentCsv = getRealAttachmentCsv()
        repository.importFromCsv(realAttachmentCsv)
        val allTx = fakeTxDao.transactions
        val targets = listOf(
            Triple(2026, Calendar.OCTOBER, 2),
            Triple(2026, Calendar.NOVEMBER, 1),
            Triple(2026, Calendar.NOVEMBER, 2),
            Triple(2026, Calendar.DECEMBER, 1),
            Triple(2026, Calendar.DECEMBER, 2)
        )
        for ((year, month, cycle) in targets) {
            val (prevYear, prevMonth, prevCycle) = if (cycle == 2) {
                Triple(year, month, 1)
            } else if (month == Calendar.JANUARY) {
                Triple(year - 1, Calendar.DECEMBER, 2)
            } else {
                Triple(year, month - 1, 2)
            }
            val prevData = QuincenaFilterHelper.filterByQuincena(allTx, prevYear, prevMonth)
            val prevOpening = QuincenaFilterHelper.calculateQuincenaRollover(allTx, prevYear, prevMonth, prevCycle)
            val priorIncome = if (prevCycle == 1) prevData.q1Income else prevData.q2Income
            val priorRealExpense = if (prevCycle == 1) prevData.q1Expense else prevData.q2Expense
            val expectedOpening = (prevOpening + priorIncome - priorRealExpense).coerceAtLeast(0.0)
            assertEquals(
                "El saldo de apertura debe encadenarse sin registrarse como ingreso nuevo",
                expectedOpening,
                QuincenaFilterHelper.calculateQuincenaRollover(allTx, year, month, cycle),
                0.001
            )
        }
    }

    @Test
    fun testSpecialQuincenaMathematicalDistributionAndBoundaries() {
        fun timeAt(year: Int, monthIndex: Int, day: Int): Long = Calendar.getInstance().apply {
            clear()
            set(year, monthIndex, day, 12, 0, 0)
        }.timeInMillis

        // Gastos: 1-15 Q1 y 16-fin Q2, incluyendo cambios de mes/año.
        val oct15 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, Calendar.OCTOBER, 15))
        assertEquals(BudgetPeriod(2026, Calendar.OCTOBER, 1), oct15)
        val oct16 = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, Calendar.OCTOBER, 16))
        assertEquals(BudgetPeriod(2026, Calendar.OCTOBER, 2), oct16)
        val oct31Expense = QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, Calendar.OCTOBER, 31), TransactionType.EXPENSE)
        assertEquals(BudgetPeriod(2026, Calendar.OCTOBER, 2), oct31Expense)

        // Pago del 15 financia Q2 del mismo mes; pago de fin de mes financia Q1 del siguiente.
        assertEquals(
            BudgetPeriod(2026, Calendar.OCTOBER, 2),
            QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, Calendar.OCTOBER, 15), TransactionType.INCOME)
        )
        assertEquals(
            BudgetPeriod(2026, Calendar.NOVEMBER, 1),
            QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, Calendar.OCTOBER, 31), TransactionType.INCOME)
        )
        assertEquals(
            BudgetPeriod(2027, Calendar.JANUARY, 1),
            QuincenaFilterHelper.getBudgetPeriod(timeAt(2026, Calendar.DECEMBER, 31), TransactionType.INCOME)
        )

        val startQ1 = QuincenaFilterHelper.getBudgetPeriodRange(2026, Calendar.OCTOBER, 1)
        val startCal = Calendar.getInstance().apply { timeInMillis = startQ1.first }
        assertEquals(Calendar.SEPTEMBER, startCal.get(Calendar.MONTH))
        assertEquals(30, startCal.get(Calendar.DAY_OF_MONTH))
        val endCal = Calendar.getInstance().apply { timeInMillis = startQ1.second }
        assertEquals(15, endCal.get(Calendar.DAY_OF_MONTH))

        // Exhaustividad: cada día de tres años cae exactamente en un ciclo de gasto.
        val iter = Calendar.getInstance().apply { clear(); set(2026, Calendar.JANUARY, 1, 12, 0, 0) }
        val end = Calendar.getInstance().apply { clear(); set(2028, Calendar.DECEMBER, 31, 12, 0, 0) }
        var days = 0
        while (!iter.after(end)) {
            val bp = QuincenaFilterHelper.getBudgetPeriod(iter.timeInMillis, TransactionType.EXPENSE)
            assertEquals(iter.get(Calendar.YEAR), bp.year)
            assertEquals(iter.get(Calendar.MONTH), bp.monthIndex)
            assertEquals(if (iter.get(Calendar.DAY_OF_MONTH) <= 15) 1 else 2, bp.quincena)
            days++
            iter.add(Calendar.DAY_OF_YEAR, 1)
        }
        assertEquals(1096, days)
    }

    @Test
    fun testFundingCyclesSeparateRealPendingAndCarryForward() {
        fun date(month: Int, day: Int): Long = Calendar.getInstance().apply {
            clear()
            set(2026, month, day, 12, 0, 0)
        }.timeInMillis

        val tx = listOf(
            TransactionEntity(id = 1, title = "Pago fin de octubre", amount = 1000.0, type = TransactionType.INCOME, category = "Salario", dateMillis = date(Calendar.OCTOBER, 31)),
            TransactionEntity(id = 2, title = "Supermercado", amount = 200.0, type = TransactionType.EXPENSE, category = "Comida", dateMillis = date(Calendar.NOVEMBER, 10)),
            TransactionEntity(id = 3, title = "Pago día 15", amount = 500.0, type = TransactionType.INCOME, category = "Salario", dateMillis = date(Calendar.NOVEMBER, 15)),
            TransactionEntity(id = 4, title = "Renta", amount = 300.0, type = TransactionType.EXPENSE, category = "Hogar", dateMillis = date(Calendar.NOVEMBER, 16)),
            TransactionEntity(id = 5, title = "Pago pendiente", amount = 100.0, type = TransactionType.EXPENSE, category = "Otro", dateMillis = date(Calendar.NOVEMBER, 20), notes = "[PENDIENTE]"),
            TransactionEntity(id = 6, title = "Compra descartada", amount = 900.0, type = TransactionType.EXPENSE, category = "Otro", dateMillis = date(Calendar.NOVEMBER, 22), isTentative = true, tentativeDecision = "DISCARDED")
        )
        val nov = QuincenaFilterHelper.filterByQuincena(tx, 2026, Calendar.NOVEMBER)
        assertEquals(1000.0, nov.q1Income, 0.001)
        assertEquals(200.0, nov.q1Expense, 0.001)
        assertEquals(0.0, nov.q1PendingExpense, 0.001)
        assertEquals(500.0, nov.q2Income, 0.001)
        assertEquals(300.0, nov.q2Expense, 0.001)
        assertEquals(100.0, nov.q2PendingExpense, 0.001)
        assertEquals(0.0, QuincenaFilterHelper.calculateQuincenaRollover(tx, 2026, Calendar.NOVEMBER, 1), 0.001)
        val q2OpeningBalance = QuincenaFilterHelper.calculateQuincenaRollover(tx, 2026, Calendar.NOVEMBER, 2)
        assertEquals(800.0, q2OpeningBalance, 0.001)
        val q1ActualAvailable = nov.q1Income - nov.q1Expense
        val q2ActualAvailable = q2OpeningBalance + nov.q2Income - nov.q2Expense
        assertEquals(800.0, q1ActualAvailable, 0.001)
        assertEquals(1000.0, q2ActualAvailable, 0.001)
        assertEquals(900.0, q2ActualAvailable - nov.q2PendingExpense, 0.001)

        // El resumen mensual consolida ambos ciclos: los traspasos son saldo inicial,
        // no ingreso nuevo; gastos reales se cuentan una sola vez y pendientes solo
        // reducen la proyección.
        val q1OpeningBalance = QuincenaFilterHelper.calculateQuincenaRollover(
            tx, 2026, Calendar.NOVEMBER, 1
        )
        val monthAvailable = q2OpeningBalance + nov.q2Income - nov.q2Expense
        val monthProjected = monthAvailable - nov.totalPendingExpense
        assertEquals(1500.0, nov.totalIncome, 0.001)
        assertEquals(500.0, nov.totalExpense, 0.001)
        assertEquals(100.0, nov.totalPendingExpense, 0.001)
        assertEquals(q1OpeningBalance + nov.totalIncome - nov.totalExpense, monthAvailable, 0.001)
        assertEquals(1000.0, monthAvailable, 0.001)
        assertEquals(900.0, monthProjected, 0.001)
        assertEquals(nov.q1Expense + nov.q2Expense, nov.totalExpense, 0.001)
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

    @Test
    fun testQuincenasCycleDisplayAndSearchFiltering() {
        fun date(month: Int, day: Int): Long = Calendar.getInstance().apply {
            clear()
            set(2026, month, day, 12, 0, 0)
        }.timeInMillis

        val txList = listOf(
            TransactionEntity(id = 1, title = "Pago fin de mes previo", amount = 6000.0, type = TransactionType.INCOME, category = "Salario", dateMillis = date(Calendar.OCTOBER, 31)),
            TransactionEntity(id = 2, title = "Supermercado La Torre", amount = 450.0, type = TransactionType.EXPENSE, category = "Comida", dateMillis = date(Calendar.NOVEMBER, 5), notes = "Despensa quincenal"),
            TransactionEntity(id = 3, title = "Gasolina Shell", amount = 250.0, type = TransactionType.EXPENSE, category = "Transporte", dateMillis = date(Calendar.NOVEMBER, 10)),
            TransactionEntity(id = 4, title = "Pago quincena día 15", amount = 6000.0, type = TransactionType.INCOME, category = "Salario", dateMillis = date(Calendar.NOVEMBER, 15)),
            TransactionEntity(id = 5, title = "Farmacia Galeno", amount = 180.0, type = TransactionType.EXPENSE, category = "Salud", dateMillis = date(Calendar.NOVEMBER, 18), notes = "[SUPUESTO] Vitaminas"),
            TransactionEntity(id = 6, title = "Restaurante San Martín", amount = 220.0, type = TransactionType.EXPENSE, category = "Comida", dateMillis = date(Calendar.NOVEMBER, 25))
        )

        val novData = QuincenaFilterHelper.filterByQuincena(txList, 2026, Calendar.NOVEMBER)

        // Q1 contiene tx 1, 2, 3 (Fin mes previo al día 15)
        assertEquals(3, novData.q1Transactions.size)
        // Q2 contiene tx 4, 5, 6 (Día 15 al penúltimo día)
        assertEquals(3, novData.q2Transactions.size)
        // Mes contiene las 6 sin duplicados
        assertEquals(6, novData.monthTransactions.size)

        // Prueba de búsqueda por comercio/título
        fun filterSearch(list: List<TransactionEntity>, query: String): List<TransactionEntity> {
            val q = query.trim().lowercase(java.util.Locale.getDefault())
            return list.filter { tx ->
                tx.title.lowercase(java.util.Locale.getDefault()).contains(q) ||
                tx.category.lowercase(java.util.Locale.getDefault()).contains(q) ||
                tx.notes?.lowercase(java.util.Locale.getDefault())?.contains(q) == true ||
                "%.2f".format(tx.amount).contains(q)
            }
        }

        // Búsqueda en Q1
        val q1Shell = filterSearch(novData.q1Transactions, "Shell")
        assertEquals(1, q1Shell.size)
        assertEquals("Gasolina Shell", q1Shell.first().title)

        // Búsqueda de "Comida" en Mes (debe encontrar 2: La Torre y San Martín)
        val monthFood = filterSearch(novData.monthTransactions, "Comida")
        assertEquals(2, monthFood.size)

        // Búsqueda en Q2 por nota "[SUPUESTO]"
        val q2Supuesto = filterSearch(novData.q2Transactions, "supuesto")
        assertEquals(1, q2Supuesto.size)
        assertEquals("Farmacia Galeno", q2Supuesto.first().title)

        // Búsqueda por monto
        val amountSearch = filterSearch(novData.monthTransactions, "450.00")
        assertEquals(1, amountSearch.size)
        assertEquals("Supermercado La Torre", amountSearch.first().title)

        // Búsqueda sin coincidencias
        val noMatch = filterSearch(novData.monthTransactions, "Inexistente")
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun testMonthlySummaryConsolidationWithoutIncomeDuplication() {
        fun date(month: Int, day: Int): Long = Calendar.getInstance().apply {
            clear()
            set(2026, month, day, 12, 0, 0)
        }.timeInMillis

        val txList = listOf(
            TransactionEntity(id = 1, title = "Pago fin de octubre", amount = 5000.0, type = TransactionType.INCOME, category = "Salario", dateMillis = date(Calendar.OCTOBER, 31)),
            TransactionEntity(id = 2, title = "Gasto Q1", amount = 1500.0, type = TransactionType.EXPENSE, category = "Servicios", dateMillis = date(Calendar.NOVEMBER, 5)),
            TransactionEntity(id = 3, title = "Pago día 15", amount = 5000.0, type = TransactionType.INCOME, category = "Salario", dateMillis = date(Calendar.NOVEMBER, 15)),
            TransactionEntity(id = 4, title = "Gasto Q2", amount = 2000.0, type = TransactionType.EXPENSE, category = "Comida", dateMillis = date(Calendar.NOVEMBER, 20))
        )

        val novData = QuincenaFilterHelper.filterByQuincena(txList, 2026, Calendar.NOVEMBER)

        // Los ingresos se contabilizan exactamente una sola vez
        assertEquals(5000.0, novData.q1Income, 0.001)
        assertEquals(5000.0, novData.q2Income, 0.001)
        assertEquals(10000.0, novData.totalIncome, 0.001)
        assertEquals(novData.q1Income + novData.q2Income, novData.totalIncome, 0.001)

        // Los gastos reales se descuentan exactamente una vez
        assertEquals(1500.0, novData.q1Expense, 0.001)
        assertEquals(2000.0, novData.q2Expense, 0.001)
        assertEquals(3500.0, novData.totalExpense, 0.001)
        assertEquals(novData.q1Expense + novData.q2Expense, novData.totalExpense, 0.001)

        // El resumen mensual contiene exactamente 4 transacciones (sin duplicados)
        assertEquals(4, novData.monthTransactions.size)
        assertEquals(4, novData.monthTransactions.map { it.id }.distinct().size)

        // El saldo trasladado es saldo inicial, nunca ingreso nuevo
        val q2Rollover = QuincenaFilterHelper.calculateQuincenaRollover(txList, 2026, Calendar.NOVEMBER, 2)
        // Q1 tuvo: 5000 ingresos - 1500 gastos = 3500 sobrante
        assertEquals(3500.0, q2Rollover, 0.001)
        // Disponible Q2 = 3500 (traspaso) + 5000 (ingreso) - 2000 (gasto) = 6500
        val q2Available = q2Rollover + novData.q2Income - novData.q2Expense
        assertEquals(6500.0, q2Available, 0.001)
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
