package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.BudgetDao
import com.example.data.local.RecurringExpenseDao
import com.example.data.local.SavingsGoalDao
import com.example.data.local.SmartCartDao
import com.example.data.local.TransactionDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val smartCartDao: SmartCartDao,
    private val database: AppDatabase? = null
) {
    private var lastCheckoutTime: Long = 0L
    private var lastCheckoutSignature: String = ""

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetLimitEntity>> = budgetDao.getAllBudgets()
    val allGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    val allRecurringExpenses: Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAll()
    val allCartItems: Flow<List<SmartCartItemEntity>> = smartCartDao.getAllItems()

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startMillis, endMillis)

    suspend fun insertTransaction(tx: TransactionEntity): Long = transactionDao.insertTransaction(tx)
    suspend fun updateTransaction(tx: TransactionEntity) = transactionDao.updateTransaction(tx)
    suspend fun deleteTransaction(tx: TransactionEntity) = transactionDao.deleteTransaction(tx)
    suspend fun deleteTransactionById(id: Long) = transactionDao.deleteById(id)

    suspend fun setBudget(budget: BudgetLimitEntity) = budgetDao.setBudget(budget)
    suspend fun deleteBudget(category: String) = budgetDao.deleteBudget(category)

    suspend fun insertGoal(goal: SavingsGoalEntity): Long = savingsGoalDao.insertGoal(goal)
    suspend fun updateGoal(goal: SavingsGoalEntity) = savingsGoalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: SavingsGoalEntity) = savingsGoalDao.deleteGoal(goal)
    suspend fun contributeToGoal(goal: SavingsGoalEntity, addAmount: Double) {
        val updated = goal.copy(currentAmount = (goal.currentAmount + addAmount).coerceAtLeast(0.0))
        savingsGoalDao.updateGoal(updated)
    }

    suspend fun insertRecurringExpense(expense: RecurringExpenseEntity): Long = recurringExpenseDao.insert(expense)
    suspend fun updateRecurringExpense(expense: RecurringExpenseEntity) = recurringExpenseDao.update(expense)
    suspend fun deleteRecurringExpense(expense: RecurringExpenseEntity) = recurringExpenseDao.delete(expense)

    suspend fun insertCartItem(item: SmartCartItemEntity): Long = smartCartDao.insertItem(item)
    suspend fun updateCartItem(item: SmartCartItemEntity) = smartCartDao.updateItem(item)
    suspend fun toggleCartItemChecked(id: Long) = smartCartDao.toggleItemChecked(id)
    suspend fun deleteCartItem(item: SmartCartItemEntity) = smartCartDao.deleteItem(item)

    suspend fun clearCheckedCartItems() = runInTransaction {
        val all = smartCartDao.getAllItemsList()
        for (item in all) {
            if (item.isChecked) {
                if (item.isPantryProduct()) {
                    val baseStatus = item.getPantryStatus()
                    smartCartDao.updateItem(item.copy(priceSource = baseStatus, isChecked = false))
                } else {
                    smartCartDao.deleteItem(item)
                }
            }
        }
    }

    suspend fun addPantryItemToCart(item: SmartCartItemEntity) {
        val newSource = "${item.getPantryStatus()}+CART"
        smartCartDao.updateItem(item.copy(priceSource = newSource, isChecked = false))
    }

    suspend fun removeCartItemFromCart(item: SmartCartItemEntity) {
        if (item.isPantryProduct()) {
            val baseStatus = item.getPantryStatus()
            smartCartDao.updateItem(item.copy(priceSource = baseStatus, isChecked = false))
        } else {
            smartCartDao.deleteItem(item)
        }
    }

    suspend fun togglePantryInCart(item: SmartCartItemEntity) {
        if (item.isInShoppingList()) {
            removeCartItemFromCart(item)
        } else {
            addPantryItemToCart(item)
        }
    }

    suspend fun clearCartItems() = smartCartDao.clearCartItems()
    suspend fun clearAllCartItems() = smartCartDao.clearCartItems()
    suspend fun clearPantryItems() = smartCartDao.clearPantryItems()
    suspend fun updatePantryStatus(id: Long, status: String) = smartCartDao.updatePantryStatus(id, status)
    suspend fun updatePantryStatusToGreenByName(name: String) = smartCartDao.updatePantryStatusToGreenByName(name)

    suspend fun addOrUpdateCartItem(
        name: String,
        quantity: Double = 1.0,
        unit: String = "Unidad",
        brand: String = "",
        price: Double = 0.0,
        store: String = "General",
        notes: String = "",
        url: String = "",
        priceSource: String = "MANUAL",
        existingItems: List<SmartCartItemEntity> = emptyList()
    ) = runInTransaction {
        val cleanName = name.trim()
        val cleanBrand = brand.trim()
        val currentList = if (existingItems.isNotEmpty()) existingItems else smartCartDao.getAllItemsList()
        val existing = currentList.find { it.matchesProduct(cleanName, cleanBrand) }
        if (existing != null) {
            val newPriceSource = if (existing.isPantryProduct()) "${existing.getPantryStatus()}+CART" else priceSource
            val updated = existing.copy(
                quantity = if (quantity > 0.0) quantity else existing.quantity,
                unit = if (unit.isNotBlank()) unit else existing.unit,
                brand = if (cleanBrand.isNotBlank()) cleanBrand else existing.brand,
                estimatedUnitPrice = if (price > 0.0) price else existing.estimatedUnitPrice,
                storeName = if (store.isNotBlank() && store != "General") store else existing.storeName,
                priceSource = newPriceSource,
                notes = if (notes.isNotBlank()) notes else existing.notes,
                productUrl = if (url.isNotBlank()) url else existing.productUrl,
                isChecked = false
            )
            smartCartDao.updateItem(updated)
        } else {
            val item = SmartCartItemEntity(
                name = cleanName,
                quantity = if (quantity <= 0.0) 1.0 else quantity,
                unit = if (unit.isBlank()) "Unidad" else unit,
                brand = cleanBrand,
                estimatedUnitPrice = price.coerceAtLeast(0.0),
                storeName = if (store.isBlank()) "General" else store,
                priceSource = priceSource,
                notes = notes.trim(),
                productUrl = url.trim()
            )
            smartCartDao.insertItem(item)
        }
    }

    suspend fun addOrUpdatePantryItem(
        name: String,
        quantity: Double = 1.0,
        unit: String = "Unidad",
        status: String = "PANTRY_VERDE",
        price: Double = 0.0,
        store: String = "General",
        notes: String = "",
        brand: String = "",
        existingPantry: List<SmartCartItemEntity> = emptyList()
    ) = runInTransaction {
        val cleanName = name.trim()
        val cleanBrand = brand.trim()
        val currentList = if (existingPantry.isNotEmpty()) existingPantry else smartCartDao.getAllItemsList()
        val existing = currentList.find { it.matchesProduct(cleanName, cleanBrand) }
        if (existing != null) {
            val inCart = existing.isInShoppingList()
            val newStatus = if (inCart) "$status+CART" else status
            val updated = existing.copy(
                quantity = if (quantity > 0.0) quantity else existing.quantity,
                unit = if (unit.isNotBlank()) unit else existing.unit,
                brand = if (cleanBrand.isNotBlank()) cleanBrand else existing.brand,
                priceSource = newStatus,
                estimatedUnitPrice = if (price > 0.0) price else existing.estimatedUnitPrice,
                storeName = if (store.isNotBlank() && store != "General") store else existing.storeName,
                notes = if (notes.isNotBlank()) notes else existing.notes
            )
            smartCartDao.updateItem(updated)
        } else {
            val item = SmartCartItemEntity(
                name = cleanName,
                quantity = if (quantity <= 0.0) 1.0 else quantity,
                unit = if (unit.isBlank()) "Unidad" else unit,
                brand = cleanBrand,
                estimatedUnitPrice = price.coerceAtLeast(0.0),
                storeName = if (store.isBlank()) "General" else store,
                priceSource = status,
                notes = notes.trim()
            )
            smartCartDao.insertItem(item)
        }
    }

    suspend fun generateReplenishmentList(
        pantryItems: List<SmartCartItemEntity> = emptyList(),
        existingCartItems: List<SmartCartItemEntity> = emptyList()
    ): Int = runInTransaction {
        val all = if (pantryItems.isEmpty() && existingCartItems.isEmpty()) {
            smartCartDao.getAllItemsList()
        } else {
            (pantryItems + existingCartItems).distinctBy {
                if (it.id != 0L) it.id else "${it.name.trim().lowercase()}_${it.brand.trim().lowercase()}_${it.priceSource}"
            }
        }
        val currentPantry = all.filter { it.isPantryProduct() }
        val currentCart = all.filter { it.isInShoppingList() }

        var addedCount = 0
        for (pantryItem in currentPantry) {
            val status = pantryItem.getPantryStatus()
            val needsReplenishment = status == "PANTRY_AMARILLO" || status == "PANTRY_ROJO"
            if (!needsReplenishment) continue

            // 1. Si el producto de despensa ya está en la lista de compras activa, no duplicar
            if (pantryItem.isInShoppingList()) {
                continue
            }

            // 2. Si el usuario ya agregó manualmente un producto equivalente al carrito:
            val manualMatch = currentCart.find { !it.isPantryProduct() && it.matchesProduct(pantryItem.name, pantryItem.brand) }
            if (manualMatch != null) {
                // Fusionar: Vincular al producto de despensa para que al comprar actualice el inventario,
                // conservando el precio manual, la cantidad requerida, unidad y notas del usuario
                val mergedStatus = "${status}+CART"
                val merged = pantryItem.copy(
                    quantity = if (manualMatch.quantity > pantryItem.quantity) manualMatch.quantity else pantryItem.quantity,
                    unit = if (manualMatch.unit.isNotBlank() && manualMatch.unit != "Unidad") manualMatch.unit else pantryItem.unit,
                    estimatedUnitPrice = if (manualMatch.estimatedUnitPrice > 0.0) manualMatch.estimatedUnitPrice else pantryItem.estimatedUnitPrice,
                    storeName = if (manualMatch.storeName.isNotBlank() && manualMatch.storeName != "General") manualMatch.storeName else pantryItem.storeName,
                    priceSource = mergedStatus,
                    notes = if (manualMatch.notes.isNotBlank()) manualMatch.notes else pantryItem.notes,
                    isChecked = manualMatch.isChecked
                )
                smartCartDao.updateItem(merged)
                smartCartDao.deleteItem(manualMatch)
                addedCount++
            } else {
                // Agregar producto de despensa a la compra sin duplicar
                val newSource = "${status}+CART"
                smartCartDao.updateItem(pantryItem.copy(priceSource = newSource, isChecked = false))
                addedCount++
            }
        }
        addedCount
    }

    suspend fun addOrUpdateUnifiedItem(
        name: String,
        quantity: Double = 1.0,
        unit: String = "Unidad",
        brand: String = "",
        estimatedUnitPrice: Double = 0.0,
        storeName: String = "General",
        status: String = "PANTRY_AMARILLO",
        isChecked: Boolean = false,
        notes: String = "",
        productUrl: String = ""
    ) = runInTransaction {
        val cleanName = name.trim()
        val cleanBrand = brand.trim()
        val all = smartCartDao.getAllItemsList()
        val existing = all.find { it.matchesProduct(cleanName, cleanBrand) }
        if (existing != null) {
            val inCart = isChecked || existing.isInShoppingList()
            val newStatus = if (status.startsWith("PANTRY_")) {
                if (inCart) "$status+CART" else status
            } else status

            val updated = existing.copy(
                quantity = if (quantity > 0.0) quantity else existing.quantity,
                unit = if (unit.isNotBlank()) unit else existing.unit,
                brand = if (cleanBrand.isNotBlank()) cleanBrand else existing.brand,
                estimatedUnitPrice = if (estimatedUnitPrice > 0.0) estimatedUnitPrice else existing.estimatedUnitPrice,
                storeName = if (storeName.isNotBlank() && storeName != "General") storeName else existing.storeName,
                priceSource = newStatus,
                isChecked = if (isChecked) true else existing.isChecked,
                notes = if (notes.isNotBlank()) notes else existing.notes,
                productUrl = if (productUrl.isNotBlank()) productUrl else existing.productUrl
            )
            smartCartDao.updateItem(updated)
        } else {
            val item = SmartCartItemEntity(
                name = cleanName,
                quantity = if (quantity <= 0.0) 1.0 else quantity,
                unit = if (unit.isBlank()) "Unidad" else unit,
                brand = cleanBrand,
                estimatedUnitPrice = estimatedUnitPrice.coerceAtLeast(0.0),
                storeName = if (storeName.isBlank()) "General" else storeName,
                priceSource = status,
                isChecked = isChecked,
                notes = notes.trim(),
                productUrl = productUrl.trim()
            )
            smartCartDao.insertItem(item)
        }
    }

    suspend fun incorporateNeedToBuyIntoCart(): Int {
        val all = smartCartDao.getAllItemsList()
        val pantry = all.filter { it.isPantryProduct() }
        val cart = all.filter { it.isInShoppingList() }
        return generateReplenishmentList(pantry, cart)
    }

    suspend fun uncheckAllCartItems() {
        smartCartDao.uncheckAll()
    }

    suspend fun deduplicateItemsIfAny() {
        val all = smartCartDao.getAllItemsList()
        val seen = mutableMapOf<String, SmartCartItemEntity>()
        val toDelete = mutableListOf<SmartCartItemEntity>()
        for (item in all) {
            val key = "${item.name.trim().lowercase()}__${item.brand.trim().lowercase()}"
            if (key.isBlank()) continue
            if (seen.containsKey(key)) {
                val existing = seen[key]!!
                val mergedStatus = when {
                    existing.priceSource.contains("ROJO") || item.priceSource.contains("ROJO") -> "PANTRY_ROJO"
                    existing.priceSource.contains("AMARILLO") || item.priceSource.contains("AMARILLO") || existing.priceSource == "REPOSICION" || item.priceSource == "REPOSICION" -> "PANTRY_AMARILLO"
                    else -> "PANTRY_VERDE"
                }
                val inCart = existing.isInShoppingList() || item.isInShoppingList()
                val finalStatus = if (inCart) "$mergedStatus+CART" else mergedStatus

                val merged = existing.copy(
                    quantity = maxOf(existing.quantity, item.quantity),
                    estimatedUnitPrice = if (existing.estimatedUnitPrice > 0) existing.estimatedUnitPrice else item.estimatedUnitPrice,
                    isChecked = existing.isChecked || item.isChecked,
                    priceSource = finalStatus,
                    storeName = if (existing.storeName != "General") existing.storeName else item.storeName
                )
                seen[key] = merged
                smartCartDao.updateItem(merged)
                toDelete.add(item)
            } else {
                seen[key] = item
            }
        }
        for (del in toDelete) {
            smartCartDao.deleteItem(del)
        }
    }

    suspend fun confirmPurchaseAndUpdatePantry(
        title: String,
        amount: Double,
        storeName: String,
        purchasedItemNames: List<String>
    ): Boolean {
        if (amount <= 0.0 && purchasedItemNames.isEmpty()) {
            return false
        }

        // Deduplicación contra doble clic o taps repetidos rápidos (ventana de 2.5 segundos)
        val now = System.currentTimeMillis()
        val signature = "$storeName-$amount-${purchasedItemNames.sorted().joinToString(",")}"
        if (now - lastCheckoutTime < 2500L && signature == lastCheckoutSignature) {
            return false
        }
        lastCheckoutTime = now
        lastCheckoutSignature = signature

        return runInTransaction {
            val tx = TransactionEntity(
                title = if (title.isBlank()) "Súper $storeName (Smart Cart)" else title.trim(),
                amount = amount,
                type = TransactionType.EXPENSE,
                category = "Alimentación & Súper",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = "Efectivo",
                notes = "Compra completada con Smart Cart Quetzi en $storeName"
            )
            transactionDao.insertTransaction(tx)

            val all = smartCartDao.getAllItemsList()
            val purchasedSet = purchasedItemNames.map { it.trim().lowercase() }.toSet()

            for (item in all) {
                val wasPurchasedByName = purchasedSet.contains(item.name.trim().lowercase())
                val wasChecked = item.isChecked
                if (wasPurchasedByName || wasChecked) {
                    if (item.isPantryProduct()) {
                        smartCartDao.updateItem(item.copy(priceSource = "PANTRY_VERDE", isChecked = false))
                    } else {
                        // Artículos de súper no recurrentes se eliminan tras ser comprados
                        smartCartDao.deleteItem(item)
                    }
                }
            }
            true
        }
    }

    suspend fun clearAllData() = runInTransaction {
        transactionDao.clearAll()
        budgetDao.clearAll()
        savingsGoalDao.clearAll()
        recurringExpenseDao.clearAll()
        smartCartDao.clearAll()
    }

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

    fun matchesExistingTransaction(
        existing: TransactionEntity,
        incomingTitle: String,
        incomingAmount: Double,
        incomingType: String,
        incomingCategory: String,
        incomingDateMillis: Long,
        incomingNotes: String
    ): Boolean {
        // 1. Tipo de transacción (INCOME vs EXPENSE)
        if (!existing.type.equals(incomingType, ignoreCase = true)) return false

        // 2. Monto con tolerancia de coma flotante
        if (kotlin.math.abs(existing.amount - incomingAmount) > 0.005) return false

        // 3. Mismo día calendario local
        if (!com.example.ui.util.QuincenaFilterHelper.isSameCalendarDay(existing.dateMillis, incomingDateMillis)) {
            return false
        }

        // 4. Mismo concepto/título
        if (!existing.title.trim().equals(incomingTitle.trim(), ignoreCase = true)) return false

        // 5. Categoría
        val exCat = existing.category.trim().lowercase()
        val inCat = incomingCategory.trim().lowercase()
        val catMatches = exCat == inCat ||
                exCat.isEmpty() || inCat.isEmpty() ||
                exCat == "general" || inCat == "general" ||
                exCat == "otros gastos" || inCat == "otros gastos"
        if (!catMatches) return false

        // 6. Si ambas transacciones tienen notas personalizadas distintas a la marca por defecto de Excel, deben coincidir
        val exNotes = existing.notes.trim()
        val inNotes = incomingNotes.trim()
        val exIsDefault = exNotes.isEmpty() || exNotes.equals("Importado de Excel", ignoreCase = true)
        val inIsDefault = inNotes.isEmpty() || inNotes.equals("Importado de Excel", ignoreCase = true)
        if (!exIsDefault && !inIsDefault) {
            if (!exNotes.equals(inNotes, ignoreCase = true)) return false
        }

        return true
    }

    suspend fun importFromCsv(csvText: String): ImportSummary {
        val parsedList = com.example.ui.util.ExcelXlsxReader.parseTransactionsFromText(csvText)
        if (parsedList.isEmpty()) return ImportSummary(0, 0, 0, 0, 0)

        return runInTransaction {
            val existing = transactionDao.getAllTransactionsList().toMutableList()
            val toInsert = mutableListOf<TransactionEntity>()
            var dupCount = 0
            var suspectedDupCount = 0
            var errCount = 0

            // Rastreador de ocurrencias dentro del mismo archivo para calcular hash estable
            val signatureCounts = mutableMapOf<String, Int>()

            for (parsed in parsedList) {
                if (parsed.amount <= 0.0 || parsed.title.isBlank()) {
                    errCount++
                    continue
                }

                val cal = Calendar.getInstance().apply { timeInMillis = parsed.dateMillis }
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val baseSig = "${parsed.type.uppercase()}|${"%.2f".format(Locale.US, parsed.amount)}|$y-$m-$d|${parsed.title.trim().lowercase(Locale.ROOT)}"
                val occIndex = (signatureCounts[baseSig] ?: 0) + 1
                signatureCounts[baseSig] = occIndex
                val generatedHash = "$baseSig|#$occIndex"

                // 1. Detección por ID externo estable si está presente en el archivo
                val matchByIdIdx = if (!parsed.externalId.isNullOrBlank()) {
                    existing.indexOfFirst {
                        !it.externalId.isNullOrBlank() && it.externalId.equals(parsed.externalId.trim(), ignoreCase = true)
                    }
                } else -1

                // 2. Detección por hash de importación previo
                val matchByHashIdx = if (matchByIdIdx == -1) {
                    existing.indexOfFirst {
                        !it.importHash.isNullOrBlank() && it.importHash == generatedHash
                    }
                } else -1

                // 3. Detección heurística por campos equivalentes (reserva consumible de existentes)
                val matchByFieldsIdx = if (matchByIdIdx == -1 && matchByHashIdx == -1) {
                    existing.indexOfFirst {
                        matchesExistingTransaction(
                            existing = it,
                            incomingTitle = parsed.title,
                            incomingAmount = parsed.amount,
                            incomingType = parsed.type,
                            incomingCategory = parsed.category,
                            incomingDateMillis = parsed.dateMillis,
                            incomingNotes = parsed.notes
                        )
                    }
                } else -1

                val matchedIdx = when {
                    matchByIdIdx != -1 -> matchByIdIdx
                    matchByHashIdx != -1 -> matchByHashIdx
                    else -> matchByFieldsIdx
                }

                if (matchedIdx != -1) {
                    // La transacción ya existe en la base de datos: se omite para evitar duplicados
                    existing.removeAt(matchedIdx)
                    dupCount++
                } else {
                    // Verificar si existe una coincidencia probable en BD (mismo día y monto) para marcarla como posible duplicado
                    val hasProbableMatch = existing.any {
                        it.type.equals(parsed.type, ignoreCase = true) &&
                        kotlin.math.abs(it.amount - parsed.amount) < 0.005 &&
                        com.example.ui.util.QuincenaFilterHelper.isSameCalendarDay(it.dateMillis, parsed.dateMillis)
                    }

                    if (hasProbableMatch) {
                        suspectedDupCount++
                    }

                    toInsert.add(
                        TransactionEntity(
                            title = parsed.title.trim(),
                            amount = parsed.amount,
                            type = parsed.type,
                            category = parsed.category.trim(),
                            dateMillis = parsed.dateMillis,
                            notes = parsed.notes.trim(),
                            externalId = parsed.externalId?.trim()?.ifBlank { null },
                            importHash = generatedHash,
                            isSuspectedDuplicate = hasProbableMatch
                        )
                    )
                }
            }

            if (toInsert.isNotEmpty()) {
                transactionDao.insertAll(toInsert)
            }

            ImportSummary(
                newCount = toInsert.size,
                duplicateCount = dupCount,
                errorCount = errCount,
                totalProcessed = parsedList.size,
                suspectedDuplicateCount = suspectedDupCount
            )
        }
    }

    fun exportToCsv(transactions: List<TransactionEntity>): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Fecha,Título,Monto,Tipo,Categoría,Miembro,Verificado,Supuesto,Notas\n")
        for (tx in transactions) {
            val date = sdf.format(Date(tx.dateMillis))
            val verified = if (tx.isVerified) "SÍ" else "NO"
            val tentative = if (tx.isTentative) "SUPUESTO" else "CONFIRMADO"
            sb.append("\"$date\",\"${tx.title}\",${tx.amount},\"${tx.type}\",\"${tx.category}\",\"${tx.familyMember}\",\"$verified\",\"$tentative\",\"${tx.notes}\"\n")
        }
        return sb.toString()
    }
}
