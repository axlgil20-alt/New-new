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
    private val paymentCommitmentDao: com.example.data.local.PaymentCommitmentDao? = null,
    private val beneficiaryDao: com.example.data.local.BeneficiaryDao? = null,
    private val shoppingTemplateDao: com.example.data.local.ShoppingTemplateDao? = null,
    private val database: AppDatabase? = null,
    private val financialAccountDao: com.example.data.local.FinancialAccountDao? = null,
    private val installmentPlanDao: com.example.data.local.InstallmentPlanDao? = null,
    private val installmentScheduleDao: com.example.data.local.InstallmentScheduleDao? = null,
    private val accountLedgerDao: com.example.data.local.AccountLedgerDao? = null
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

    /**
     * Convierte `amountMinor` en la fuente primaria de verdad en toda escritura.
     * - Si el registro ya existía y un llamador heredado modificó solo `amount` (ej. `.copy(amount = nuevoValor)`)
     *   sin actualizar `amountMinor`, sincroniza `amountMinor` desde ese nuevo valor y deriva `amount`.
     * - En cualquier otro caso, conserva `amountMinor` como verdad primaria y deriva `amount` con `toDisplayDouble`.
     */
    internal suspend fun normalizeTransactionForWrite(tx: TransactionEntity): TransactionEntity {
        if (tx.id > 0L) {
            val existing = accountLedgerDao?.getTransactionById(tx.id)
                ?: transactionDao.getAllTransactionsList().find { it.id == tx.id }
            if (existing != null && tx.amountMinor == existing.amountMinor) {
                val legacyMinor = MoneyMinorUnits.fromDouble(tx.amount)
                if (legacyMinor != existing.amountMinor) {
                    return tx.withExactAmountMinor(legacyMinor)
                }
            }
        }
        return tx.withSyncedLegacyAmountFromMinor()
    }

    internal suspend fun normalizeCommitmentForWrite(commitment: PaymentCommitmentEntity): PaymentCommitmentEntity {
        if (commitment.id > 0L && paymentCommitmentDao != null) {
            val existing = paymentCommitmentDao.getCommitmentById(commitment.id)
            if (existing != null && commitment.amountMinor == existing.amountMinor) {
                val legacyMinor = MoneyMinorUnits.fromDouble(commitment.amount)
                if (legacyMinor != existing.amountMinor) {
                    return commitment.withExactAmountMinor(legacyMinor)
                }
            }
        }
        return commitment.withSyncedLegacyAmountFromMinor()
    }

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetLimitEntity>> = budgetDao.getAllBudgets()
    val allGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    val allRecurringExpenses: Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAll()
    val allCartItems: Flow<List<SmartCartItemEntity>> = smartCartDao.getAllItems()
    val allPaymentCommitments: Flow<List<PaymentCommitmentEntity>> =
        paymentCommitmentDao?.getAllCommitments() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allBeneficiaries: Flow<List<BeneficiaryEntity>> =
        beneficiaryDao?.getAllBeneficiaries() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allShoppingTemplates: Flow<List<ShoppingTemplateEntity>> =
        shoppingTemplateDao?.getAllTemplates() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startMillis, endMillis)

    suspend fun insertTransaction(tx: TransactionEntity): Long =
        transactionDao.insertTransaction(normalizeTransactionForWrite(tx))

    suspend fun updateTransaction(tx: TransactionEntity) =
        transactionDao.updateTransaction(normalizeTransactionForWrite(tx))

    suspend fun deleteTransaction(tx: TransactionEntity) = runInTransaction {
        val groupId = tx.transferGroupId
        if (groupId != null && accountLedgerDao != null) {
            accountLedgerDao.deleteByTransferGroup(groupId)
        } else {
            transactionDao.deleteTransaction(tx)
        }
    }

    suspend fun deleteTransactionById(id: Long) = runInTransaction {
        val existing = accountLedgerDao?.getTransactionById(id)
            ?: transactionDao.getAllTransactionsList().find { it.id == id }
        val groupId = existing?.transferGroupId
        if (groupId != null && accountLedgerDao != null) {
            accountLedgerDao.deleteByTransferGroup(groupId)
        } else {
            transactionDao.deleteById(id)
        }
    }

    suspend fun deleteTransactionsByIds(ids: List<Long>) = runInTransaction {
        if (ids.isEmpty()) return@runInTransaction
        if (accountLedgerDao != null) {
            val all = transactionDao.getAllTransactionsList()
            val groupIds = all.asSequence()
                .filter { it.id in ids && it.transferGroupId != null }
                .mapNotNull { it.transferGroupId }
                .distinct()
                .toList()
            for (groupId in groupIds) {
                accountLedgerDao.deleteByTransferGroup(groupId)
            }
        }
        transactionDao.deleteByIds(ids)
    }

    suspend fun markTransactionsAsLegitimate(ids: List<Long>) = runInTransaction {
        val all = transactionDao.getAllTransactionsList()
        for (tx in all) {
            if (tx.id in ids && tx.isSuspectedDuplicate) {
                transactionDao.updateTransaction(
                    normalizeTransactionForWrite(tx).copy(isSuspectedDuplicate = false)
                )
            }
        }
    }

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
        // AISLAMIENTO ESTRICTO: Buscar únicamente dentro de la lista de compras activa.
        // NUNCA alterar productos de Mi Despensa ni agregarlo automáticamente a la despensa.
        val existingCart = currentList.find { !it.isPantryProduct() && it.matchesProduct(cleanName, cleanBrand) }
        if (existingCart != null) {
            val updated = existingCart.copy(
                quantity = if (quantity > 0.0) quantity else existingCart.quantity,
                unit = if (unit.isNotBlank()) unit else existingCart.unit,
                brand = if (cleanBrand.isNotBlank()) cleanBrand else existingCart.brand,
                estimatedUnitPrice = if (price > 0.0) price else existingCart.estimatedUnitPrice,
                storeName = if (store.isNotBlank() && store != "General") store else existingCart.storeName,
                notes = if (notes.isNotBlank()) notes else existingCart.notes,
                productUrl = if (url.isNotBlank()) url else existingCart.productUrl,
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
                priceSource = if (priceSource.startsWith("PANTRY_")) "MANUAL" else priceSource,
                notes = notes.trim(),
                productUrl = url.trim(),
                isChecked = false
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
        val existing = currentList.find { it.isPantryProduct() && it.matchesProduct(cleanName, cleanBrand) }
        val finalStatus = if (status.startsWith("PANTRY_")) status else "PANTRY_VERDE"
        if (existing != null) {
            val inCart = existing.priceSource.contains("+CART")
            val newStatus = if (inCart) "$finalStatus+CART" else finalStatus
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
                priceSource = finalStatus,
                notes = notes.trim(),
                isChecked = false
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
        purchasedItemNames: List<String>,
        updatePantryOnCheckout: Boolean = true,
        accountId: Long? = null
    ): Boolean {
        val amountMinor = MoneyMinorUnits.fromDouble(amount)
        if (amountMinor <= 0L && purchasedItemNames.isEmpty()) {
            return false
        }

        // Deduplicación contra doble clic o taps repetidos rápidos (ventana de 2.5 segundos)
        val now = System.currentTimeMillis()
        val signature = "$storeName-$amountMinor-${purchasedItemNames.sorted().joinToString(",")}-$updatePantryOnCheckout-${accountId ?: -1L}"
        if (now - lastCheckoutTime < 2500L && signature == lastCheckoutSignature) {
            return false
        }
        lastCheckoutTime = now
        lastCheckoutSignature = signature

        return runInTransaction {
            val account = if (accountId != null) financialAccountDao?.getAccountById(accountId) else null
            val displayAmount = MoneyMinorUnits.toDisplayDouble(amountMinor)
            val tx = TransactionEntity(
                title = if (title.isBlank()) "Súper $storeName (Smart Cart)" else title.trim(),
                amount = displayAmount,
                type = TransactionType.EXPENSE,
                category = "Alimentación & Súper",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = account?.name ?: "Efectivo",
                notes = "Compra completada con Smart Cart Quetzi en $storeName",
                amountMinor = amountMinor,
                accountId = account?.id ?: accountId,
                entryKind = TransactionEntryKind.STANDARD
            )
            transactionDao.insertTransaction(tx)

            val all = smartCartDao.getAllItemsList()
            val purchasedSet = purchasedItemNames.map { it.trim().lowercase() }.toSet()
            val pantryList = all.filter { it.isPantryProduct() }.toMutableList()

            for (item in all) {
                val wasPurchasedByName = purchasedSet.contains(item.name.trim().lowercase())
                val wasChecked = item.isChecked
                if (wasPurchasedByName || wasChecked) {
                    if (item.isPantryProduct()) {
                        // Es un producto de despensa que estaba en el carrito para reposición
                        if (updatePantryOnCheckout) {
                            smartCartDao.updateItem(item.copy(priceSource = "PANTRY_VERDE", isChecked = false))
                        } else {
                            // Remover la marca de compra, conservando el estado actual de despensa
                            smartCartDao.updateItem(item.copy(priceSource = item.getPantryStatus(), isChecked = false))
                        }
                    } else {
                        // Es un producto de la lista del súper
                        if (updatePantryOnCheckout) {
                            val existingInPantry = pantryList.find { it.matchesProduct(item.name, item.brand) }
                            if (existingInPantry != null) {
                                smartCartDao.updateItem(existingInPantry.copy(priceSource = "PANTRY_VERDE", isChecked = false))
                            } else {
                                val newPantryItem = SmartCartItemEntity(
                                    name = item.name.trim(),
                                    quantity = item.quantity,
                                    unit = item.unit,
                                    brand = item.brand,
                                    estimatedUnitPrice = item.estimatedUnitPrice,
                                    storeName = item.storeName,
                                    priceSource = "PANTRY_VERDE",
                                    isChecked = false,
                                    notes = item.notes
                                )
                                smartCartDao.insertItem(newPantryItem)
                                pantryList.add(newPantryItem)
                            }
                        }
                        // Quitar el producto de la lista activa tras comprarse
                        smartCartDao.deleteItem(item)
                    }
                }
            }
            true
        }
    }

    // =========================================================================
    // LISTAS DE COMPRAS REUTILIZABLES (PLANTILLAS)
    // =========================================================================

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun serializeTemplateItems(items: List<TemplateItemData>): String {
        val sb = StringBuilder()
        sb.append("[")
        for (i in items.indices) {
            val it = items[i]
            sb.append("{")
            sb.append("\"name\":\"").append(escapeJson(it.name)).append("\",")
            sb.append("\"quantity\":").append(it.quantity).append(",")
            sb.append("\"unit\":\"").append(escapeJson(it.unit)).append("\",")
            sb.append("\"brand\":\"").append(escapeJson(it.brand)).append("\",")
            sb.append("\"estimatedUnitPrice\":").append(it.estimatedUnitPrice).append(",")
            sb.append("\"storeName\":\"").append(escapeJson(it.storeName)).append("\",")
            sb.append("\"notes\":\"").append(escapeJson(it.notes)).append("\"")
            sb.append("}")
            if (i < items.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    suspend fun saveCartAsTemplate(name: String, description: String = ""): Long = runInTransaction {
        if (shoppingTemplateDao == null) return@runInTransaction 0L
        val all = smartCartDao.getAllItemsList()
        val cartItems = all.filter { it.isInShoppingList() }
        val templateItems = cartItems.map { item ->
            TemplateItemData(
                name = item.name,
                quantity = item.quantity,
                unit = item.unit,
                brand = item.brand,
                estimatedUnitPrice = item.estimatedUnitPrice,
                storeName = item.storeName,
                notes = item.notes
            )
        }
        val total = cartItems.sumOf { it.quantity * it.estimatedUnitPrice }
        val template = ShoppingTemplateEntity(
            name = name.trim().ifBlank { "Lista de Compras" },
            description = description.trim(),
            itemsJson = serializeTemplateItems(templateItems),
            itemCount = cartItems.size,
            estimatedTotal = total
        )
        shoppingTemplateDao.insertTemplate(template)
    }

    suspend fun applyTemplateToCart(templateId: Long): Int = runInTransaction {
        if (shoppingTemplateDao == null) return@runInTransaction 0
        val template = shoppingTemplateDao.getTemplateById(templateId) ?: return@runInTransaction 0
        val items = parseTemplateItemsJson(template.itemsJson)
        val all = smartCartDao.getAllItemsList()
        val currentCart = all.filter { it.isInShoppingList() }
        var count = 0

        for (item in items) {
            val existing = currentCart.find { !it.isPantryProduct() && it.matchesProduct(item.name, item.brand) }
            if (existing != null) {
                val updated = existing.copy(
                    quantity = existing.quantity + item.quantity,
                    estimatedUnitPrice = if (item.estimatedUnitPrice > 0.0) item.estimatedUnitPrice else existing.estimatedUnitPrice,
                    isChecked = false
                )
                smartCartDao.updateItem(updated)
                count++
            } else {
                val newItem = SmartCartItemEntity(
                    name = item.name.trim(),
                    quantity = item.quantity,
                    unit = item.unit.ifBlank { "Unidad" },
                    brand = item.brand.trim(),
                    estimatedUnitPrice = item.estimatedUnitPrice.coerceAtLeast(0.0),
                    storeName = if (item.storeName.isBlank()) "General" else item.storeName,
                    priceSource = "MANUAL",
                    isChecked = false,
                    notes = item.notes.trim()
                )
                smartCartDao.insertItem(newItem)
                count++
            }
        }
        count
    }

    suspend fun duplicateTemplate(templateId: Long, newName: String = ""): Long = runInTransaction {
        if (shoppingTemplateDao == null) return@runInTransaction 0L
        val original = shoppingTemplateDao.getTemplateById(templateId) ?: return@runInTransaction 0L
        val finalName = if (newName.isNotBlank()) newName.trim() else "${original.name} (Copia)"
        val copy = original.copy(
            id = 0L,
            name = finalName,
            createdAtMillis = System.currentTimeMillis()
        )
        shoppingTemplateDao.insertTemplate(copy)
    }

    suspend fun deleteTemplate(template: ShoppingTemplateEntity) {
        shoppingTemplateDao?.deleteTemplate(template)
    }

    suspend fun updateTemplate(template: ShoppingTemplateEntity) {
        shoppingTemplateDao?.updateTemplate(template)
    }

    fun parseTemplateItemsJson(jsonStr: String): List<TemplateItemData> {
        val list = mutableListOf<TemplateItemData>()
        if (jsonStr.isBlank()) return list
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TemplateItemData(
                        name = obj.optString("name", ""),
                        quantity = obj.optDouble("quantity", 1.0),
                        unit = obj.optString("unit", "Unidad"),
                        brand = obj.optString("brand", ""),
                        estimatedUnitPrice = obj.optDouble("estimatedUnitPrice", 0.0),
                        storeName = obj.optString("storeName", "General"),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            if (list.isNotEmpty()) return list
        } catch (_: Throwable) {}

        // Fallback robust regex parser para tests unitarios JVM donde org.json no está disponible
        val objectPattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}")
        val matcher = objectPattern.matcher(jsonStr)
        while (matcher.find()) {
            val body = matcher.group(1) ?: continue
            fun extractString(key: String): String {
                val p = java.util.regex.Pattern.compile("\"$key\"\\s*:\\s*\"([^\"]*)\"")
                val m = p.matcher(body)
                return if (m.find()) (m.group(1) ?: "").replace("\\\"", "\"").replace("\\\\", "\\") else ""
            }
            fun extractDouble(key: String, default: Double): Double {
                val p = java.util.regex.Pattern.compile("\"$key\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
                val m = p.matcher(body)
                return if (m.find()) m.group(1)?.toDoubleOrNull() ?: default else default
            }
            val name = extractString("name")
            if (name.isNotBlank()) {
                list.add(
                    TemplateItemData(
                        name = name,
                        quantity = extractDouble("quantity", 1.0),
                        unit = extractString("unit").ifBlank { "Unidad" },
                        brand = extractString("brand"),
                        estimatedUnitPrice = extractDouble("estimatedUnitPrice", 0.0),
                        storeName = extractString("storeName").ifBlank { "General" },
                        notes = extractString("notes")
                    )
                )
            }
        }
        return list
    }

    suspend fun ensureDefaultShoppingTemplates() = runInTransaction {
        if (shoppingTemplateDao == null) return@runInTransaction
        val existing = shoppingTemplateDao.getAllTemplatesList()
        if (existing.isNotEmpty()) return@runInTransaction

        val default1Items = org.json.JSONArray().apply {
            put(org.json.JSONObject().apply { put("name", "Huevos de granja"); put("quantity", 1.0); put("unit", "Docena"); put("estimatedUnitPrice", 22.0); put("storeName", "Maxi Despensa") })
            put(org.json.JSONObject().apply { put("name", "Frijoles negros Ducal"); put("quantity", 2.0); put("unit", "Lata"); put("estimatedUnitPrice", 14.50); put("storeName", "La Torre") })
            put(org.json.JSONObject().apply { put("name", "Leche entera Trebolac"); put("quantity", 2.0); put("unit", "Litro"); put("estimatedUnitPrice", 16.0); put("storeName", "Walmart") })
            put(org.json.JSONObject().apply { put("name", "Arroz blanco Gallo Dorado"); put("quantity", 2.0); put("unit", "Libra"); put("estimatedUnitPrice", 6.50); put("storeName", "General") })
            put(org.json.JSONObject().apply { put("name", "Aceite vegetal Ideal"); put("quantity", 1.0); put("unit", "Litro"); put("estimatedUnitPrice", 24.0); put("storeName", "Maxi Despensa") })
            put(org.json.JSONObject().apply { put("name", "Pan sandwich Europa"); put("quantity", 1.0); put("unit", "Bolsa"); put("estimatedUnitPrice", 18.0); put("storeName", "Paiz") })
            put(org.json.JSONObject().apply { put("name", "Pollo fresco en piezas"); put("quantity", 3.0); put("unit", "Libra"); put("estimatedUnitPrice", 16.0); put("storeName", "La Barata") })
        }

        val default2Items = org.json.JSONArray().apply {
            put(org.json.JSONObject().apply { put("name", "Detergente en polvo"); put("quantity", 1.0); put("unit", "Bolsa"); put("estimatedUnitPrice", 38.0); put("storeName", "Maxi Despensa") })
            put(org.json.JSONObject().apply { put("name", "Cloro desinfectante"); put("quantity", 1.0); put("unit", "Litro"); put("estimatedUnitPrice", 12.0); put("storeName", "La Torre") })
            put(org.json.JSONObject().apply { put("name", "Jabón para trastos"); put("quantity", 1.0); put("unit", "Bote"); put("estimatedUnitPrice", 19.50); put("storeName", "Walmart") })
            put(org.json.JSONObject().apply { put("name", "Papel higiénico 4 rollos"); put("quantity", 2.0); put("unit", "Paquete"); put("estimatedUnitPrice", 21.0); put("storeName", "Súper del Barrio") })
            put(org.json.JSONObject().apply { put("name", "Bolsas para basura"); put("quantity", 1.0); put("unit", "Paquete"); put("estimatedUnitPrice", 15.0); put("storeName", "General") })
        }

        shoppingTemplateDao.insertTemplate(
            ShoppingTemplateEntity(
                name = "Compra de quincena básica",
                description = "Alimentos esenciales para el hogar",
                itemsJson = default1Items.toString(),
                itemCount = default1Items.length(),
                estimatedTotal = 159.0
            )
        )

        shoppingTemplateDao.insertTemplate(
            ShoppingTemplateEntity(
                name = "Limpieza y hogar",
                description = "Higiene y mantenimiento de la casa",
                itemsJson = default2Items.toString(),
                itemCount = default2Items.length(),
                estimatedTotal = 126.50
            )
        )
    }

    // =========================================================================
    // MÓDULO PAGOS (ADMINISTRADOR DE COMPROMISOS FAMILIARES & BENEFICIARIOS)
    // =========================================================================

    suspend fun createPaymentCommitment(commitment: PaymentCommitmentEntity): Long = runInTransaction {
        if (paymentCommitmentDao == null) return@runInTransaction 0L

        val normalizedCommitment = normalizeCommitmentForWrite(commitment)
        val exactMinor = normalizedCommitment.amountMinor
        val displayAmount = MoneyMinorUnits.toDisplayDouble(exactMinor)

        // 1. Crear gasto supuesto vinculado en Room: refleja la obligación en proyecciones y quincenas sin descontar saldo real
        val txTitle = if (normalizedCommitment.beneficiaryName.isNotBlank() &&
            !normalizedCommitment.title.contains(normalizedCommitment.beneficiaryName, ignoreCase = true)
        ) {
            "${normalizedCommitment.beneficiaryName} - ${normalizedCommitment.title}".trim()
        } else {
            normalizedCommitment.title.ifBlank { normalizedCommitment.beneficiaryName }
        }

        val isCardStatement = normalizedCommitment.commitmentKind == CommitmentKind.CREDIT_CARD_STATEMENT
        val tentativeTx = TransactionEntity(
            title = txTitle,
            amount = displayAmount,
            type = TransactionType.EXPENSE,
            category = normalizedCommitment.category,
            dateMillis = normalizedCommitment.dueDateMillis,
            isTentative = true,
            tentativeDecision = "PENDING",
            notes = if (normalizedCommitment.description.isNotBlank()) "[PENDIENTE] ${normalizedCommitment.description}" else "[PENDIENTE] Pago programado",
            amountMinor = exactMinor,
            accountId = normalizedCommitment.paymentSourceAccountId,
            entryKind = if (isCardStatement) TransactionEntryKind.TRANSFER_OUT else TransactionEntryKind.STANDARD,
            counterpartAccountId = if (isCardStatement) normalizedCommitment.creditCardAccountId else null
        )
        val linkedId = transactionDao.insertTransaction(tentativeTx)

        // 2. Guardar el compromiso con el identificador estable de la transacción
        val toSave = normalizedCommitment.copy(linkedTransactionId = linkedId)
        val savedId = paymentCommitmentDao.insertCommitment(toSave)

        // 3. Si el beneficiario no existe en el directorio, guardarlo automáticamente
        if (beneficiaryDao != null && normalizedCommitment.beneficiaryName.isNotBlank()) {
            val existing = beneficiaryDao.getAllBeneficiariesList()
            if (existing.none { it.name.trim().equals(normalizedCommitment.beneficiaryName.trim(), ignoreCase = true) }) {
                beneficiaryDao.insertBeneficiary(
                    BeneficiaryEntity(
                        name = normalizedCommitment.beneficiaryName.trim(),
                        category = normalizedCommitment.category,
                        defaultDescription = normalizedCommitment.description.trim()
                    )
                )
            }
        }

        savedId
    }

    suspend fun markPaymentAsPaid(commitmentId: Long): Boolean = runInTransaction {
        if (paymentCommitmentDao == null) return@runInTransaction false
        val rawCommitment = paymentCommitmentDao.getCommitmentById(commitmentId) ?: return@runInTransaction false
        val commitment = normalizeCommitmentForWrite(rawCommitment)

        // PROTECCIÓN CONTRA DUPLICADOS: Si ya fue confirmado, retornar false de inmediato sin descontar doble
        if (commitment.isPaid || commitment.status == PaymentStatus.PAID) {
            return@runInTransaction false
        }

        val now = System.currentTimeMillis()
        val exactMinor = commitment.amountMinor
        val displayAmount = MoneyMinorUnits.toDisplayDouble(exactMinor)
        val isCardStatementSettlement =
            commitment.commitmentKind == CommitmentKind.CREDIT_CARD_STATEMENT &&
                commitment.creditCardAccountId != null &&
                commitment.paymentSourceAccountId != null

        var resultingLinkedTxId = commitment.linkedTransactionId

        if (isCardStatementSettlement) {
            // Liquidación atómica de tarjeta: dos patas enlazadas que ajustan saldos sin duplicar gastos en presupuesto
            val linkedId = commitment.linkedTransactionId
            if (linkedId != null) {
                transactionDao.deleteById(linkedId)
            }
            val transferGroupId = "ccstmt_${commitment.id}_${java.util.UUID.randomUUID()}"
            val sourceAccount = commitment.paymentSourceAccountId?.let { financialAccountDao?.getAccountById(it) }
            val cardAccount = commitment.creditCardAccountId?.let { financialAccountDao?.getAccountById(it) }
            val outTx = TransactionEntity(
                title = "Pago tarjeta ${cardAccount?.name ?: commitment.title}".trim(),
                amount = displayAmount,
                type = TransactionType.EXPENSE,
                category = commitment.category,
                dateMillis = now,
                paymentMethod = sourceAccount?.name ?: "Cuenta",
                notes = "Liquidación de estado de cuenta (${commitment.beneficiaryName})",
                isVerified = true,
                isTentative = false,
                tentativeDecision = "CONFIRMED",
                amountMinor = exactMinor,
                accountId = commitment.paymentSourceAccountId,
                entryKind = TransactionEntryKind.TRANSFER_OUT,
                transferGroupId = transferGroupId,
                counterpartAccountId = commitment.creditCardAccountId
            )
            val cardPaymentTx = TransactionEntity(
                title = "Abono ${cardAccount?.name ?: commitment.title}".trim(),
                amount = displayAmount,
                type = TransactionType.INCOME,
                category = commitment.category,
                dateMillis = now,
                paymentMethod = sourceAccount?.name ?: "Transferencia",
                notes = "Pago de estado de cuenta desde ${sourceAccount?.name ?: "cuenta propia"}",
                isVerified = true,
                isTentative = false,
                tentativeDecision = "CONFIRMED",
                amountMinor = exactMinor,
                accountId = commitment.creditCardAccountId,
                entryKind = TransactionEntryKind.CARD_PAYMENT,
                transferGroupId = transferGroupId,
                counterpartAccountId = commitment.paymentSourceAccountId
            )
            transactionDao.insertTransaction(outTx)
            resultingLinkedTxId = transactionDao.insertTransaction(cardPaymentTx)
        } else {
            // 1. Actualizar el movimiento vinculado en REGISTROS a GASTO REAL CONFIRMADO
            val linkedId = commitment.linkedTransactionId
            if (linkedId != null) {
                val allTxs = transactionDao.getAllTransactionsList()
                val existingTx = allTxs.find { it.id == linkedId }
                if (existingTx != null) {
                    val cleanNotes = existingTx.notes.replace("[PENDIENTE]", "")
                        .replace("Pendiente", "", ignoreCase = true)
                        .replace("[SUPUESTO]", "")
                        .replace("Supuesto", "", ignoreCase = true)
                        .trim()
                    val confirmedTx = existingTx.withExactAmountMinor(exactMinor).copy(
                        isTentative = false,
                        tentativeDecision = "CONFIRMED",
                        isVerified = true,
                        accountId = existingTx.accountId ?: commitment.paymentSourceAccountId,
                        notes = if (cleanNotes.isNotBlank()) "$cleanNotes (Pagado)" else "Pago realizado"
                    )
                    transactionDao.updateTransaction(confirmedTx)
                } else {
                    val newRealTx = TransactionEntity(
                        title = commitment.title.ifBlank { commitment.beneficiaryName },
                        amount = displayAmount,
                        type = TransactionType.EXPENSE,
                        category = commitment.category,
                        dateMillis = now,
                        isTentative = false,
                        tentativeDecision = "CONFIRMED",
                        isVerified = true,
                        notes = "Pago realizado a ${commitment.beneficiaryName}",
                        amountMinor = exactMinor,
                        accountId = commitment.paymentSourceAccountId,
                        entryKind = TransactionEntryKind.STANDARD
                    )
                    resultingLinkedTxId = transactionDao.insertTransaction(newRealTx)
                }
            }
        }

        // 2. Marcar el compromiso como PAGADO con fecha de confirmación
        val updatedCommitment = commitment.copy(
            isPaid = true,
            status = PaymentStatus.PAID,
            paidDateMillis = now,
            linkedTransactionId = resultingLinkedTxId
        )
        paymentCommitmentDao.updateCommitment(updatedCommitment)

        // 3. Si es un pago RECURRENTE: generar el siguiente vencimiento automáticamente sin duplicar ni borrar el historial
        if (commitment.frequency != PaymentFrequency.ONCE) {
            val nextDueDate = calculateNextDueDate(commitment.dueDateMillis, commitment.frequency)
            val nextTitle = if (commitment.beneficiaryName.isNotBlank() && !commitment.title.contains(commitment.beneficiaryName, ignoreCase = true)) {
                "${commitment.beneficiaryName} - ${commitment.title}".trim()
            } else {
                commitment.title.ifBlank { commitment.beneficiaryName }
            }
            val isCardStmt = commitment.commitmentKind == CommitmentKind.CREDIT_CARD_STATEMENT
            val nextTentativeTx = TransactionEntity(
                title = nextTitle,
                amount = displayAmount,
                type = TransactionType.EXPENSE,
                category = commitment.category,
                dateMillis = nextDueDate,
                isTentative = true,
                tentativeDecision = "PENDING",
                notes = if (commitment.description.isNotBlank()) "[PENDIENTE] ${commitment.description}" else "[PENDIENTE] Pago programado",
                amountMinor = exactMinor,
                accountId = commitment.paymentSourceAccountId,
                entryKind = if (isCardStmt) TransactionEntryKind.TRANSFER_OUT else TransactionEntryKind.STANDARD,
                counterpartAccountId = if (isCardStmt) commitment.creditCardAccountId else null
            )
            val nextLinkedTxId = transactionDao.insertTransaction(nextTentativeTx)
            val nextCommitment = commitment.copy(
                id = 0L,
                dueDateMillis = nextDueDate,
                isPaid = false,
                status = PaymentStatus.PENDING,
                paidDateMillis = null,
                linkedTransactionId = nextLinkedTxId,
                createdAtMillis = now
            )
            paymentCommitmentDao.insertCommitment(nextCommitment)
        }

        true
    }

    suspend fun updatePaymentCommitment(commitment: PaymentCommitmentEntity) = runInTransaction {
        if (paymentCommitmentDao == null) return@runInTransaction
        val normalized = normalizeCommitmentForWrite(commitment)
        // Si el compromiso aún está pendiente, actualizar la transacción vinculada manteniendo paridad monetaria exacta
        if (!normalized.isPaid && normalized.linkedTransactionId != null) {
            val allTxs = transactionDao.getAllTransactionsList()
            val existingTx = allTxs.find { it.id == normalized.linkedTransactionId }
            if (existingTx != null) {
                val updatedTx = existingTx.withExactAmountMinor(normalized.amountMinor).copy(
                    title = if (normalized.beneficiaryName.isNotBlank() &&
                        !normalized.title.contains(normalized.beneficiaryName, ignoreCase = true)
                    ) {
                        "${normalized.beneficiaryName} - ${normalized.title}".trim()
                    } else normalized.title,
                    category = normalized.category,
                    dateMillis = normalized.dueDateMillis,
                    accountId = normalized.paymentSourceAccountId ?: existingTx.accountId
                )
                transactionDao.updateTransaction(updatedTx)
            }
        }
        paymentCommitmentDao.updateCommitment(normalized)
    }

    suspend fun deletePaymentCommitment(commitment: PaymentCommitmentEntity) = runInTransaction {
        if (paymentCommitmentDao == null) return@runInTransaction
        // PROTECCIÓN DE HISTORIAL: Si el compromiso ya fue pagado, NO borrar el gasto real confirmado
        if (!commitment.isPaid && commitment.linkedTransactionId != null) {
            transactionDao.deleteById(commitment.linkedTransactionId)
        }
        paymentCommitmentDao.deleteCommitment(commitment)
    }

    fun calculateNextDueDate(currentDueMillis: Long, frequency: String): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDueMillis }
        when (frequency) {
            PaymentFrequency.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
            PaymentFrequency.BIWEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 14)
            PaymentFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    suspend fun addBeneficiary(beneficiary: BeneficiaryEntity): Long {
        return beneficiaryDao?.insertBeneficiary(beneficiary) ?: 0L
    }

    suspend fun updateBeneficiary(beneficiary: BeneficiaryEntity) {
        beneficiaryDao?.updateBeneficiary(beneficiary)
    }

    suspend fun deleteBeneficiary(beneficiary: BeneficiaryEntity) {
        beneficiaryDao?.deleteBeneficiary(beneficiary)
    }

    suspend fun ensureDefaultBeneficiaries() = runInTransaction {
        if (beneficiaryDao == null) return@runInTransaction
        val existing = beneficiaryDao.getAllBeneficiariesList()
        if (existing.isEmpty()) {
            val defaults = listOf(
                BeneficiaryEntity(name = "EEGSA", category = "Vivienda & Servicios", defaultDescription = "Servicio de Electricidad"),
                BeneficiaryEntity(name = "Tigo", category = "Telefonía & Internet", defaultDescription = "Internet Residencial / Móvil"),
                BeneficiaryEntity(name = "Claro", category = "Telefonía & Internet", defaultDescription = "Línea y Claro Hogar"),
                BeneficiaryEntity(name = "Alquiler", category = "Vivienda & Servicios", defaultDescription = "Renta mensual de vivienda"),
                BeneficiaryEntity(name = "Colegio", category = "Educación", defaultDescription = "Mensualidad / Colegiatura"),
                BeneficiaryEntity(name = "Préstamo / Banco", category = "Otros Gastos", defaultDescription = "Cuota mensual crédito"),
                BeneficiaryEntity(name = "Agua Potable", category = "Vivienda & Servicios", defaultDescription = "Servicio de agua / Municipal"),
                BeneficiaryEntity(name = "Seguro Médico", category = "Salud & Medicina", defaultDescription = "Cuota seguro de salud")
            )
            beneficiaryDao.insertAllBeneficiaries(defaults)
        }
    }

    suspend fun clearAllData() = runInTransaction {
        transactionDao.clearAll()
        budgetDao.clearAll()
        savingsGoalDao.clearAll()
        recurringExpenseDao.clearAll()
        smartCartDao.clearAll()
        paymentCommitmentDao?.clearAll()
        beneficiaryDao?.clearAll()
        installmentScheduleDao?.clearAll()
        installmentPlanDao?.clearAll()
        financialAccountDao?.clearAll()
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

        // 2. Monto exacto en unidades menores
        val incomingMinor = MoneyMinorUnits.fromDouble(incomingAmount)
        if (existing.effectiveAmountMinor != incomingMinor) return false

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

    suspend fun importFromCsv(
        csvText: String,
        targetAccountId: Long? = null
    ): ImportSummary {
        val parsedList = com.example.ui.util.ExcelXlsxReader.parseTransactionsFromText(csvText)
        if (parsedList.isEmpty()) return ImportSummary(0, 0, 0, 0, 0)

        return runInTransaction {
            val targetAccount = if (targetAccountId != null) {
                financialAccountDao?.getAccountById(targetAccountId)
            } else null

            val existing = transactionDao.getAllTransactionsList().toMutableList()
            val toInsert = mutableListOf<TransactionEntity>()
            var dupCount = 0
            var suspectedDupCount = 0
            var errCount = 0

            // Rastreador de ocurrencias dentro del mismo archivo para calcular hash estable
            val signatureCounts = mutableMapOf<String, Int>()

            for (parsed in parsedList) {
                val parsedMinor = MoneyMinorUnits.fromDouble(parsed.amount)
                if (parsedMinor <= 0L || parsed.title.isBlank()) {
                    errCount++
                    continue
                }
                val displayAmount = MoneyMinorUnits.toDisplayDouble(parsedMinor)
                val formattedAmountStr = MoneyMinorUnits.toBigDecimal(parsedMinor).toPlainString()

                val cal = Calendar.getInstance().apply { timeInMillis = parsed.dateMillis }
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val accountPrefix = if (targetAccountId != null) "ACC$targetAccountId|" else ""
                val baseSig = "$accountPrefix${parsed.type.uppercase()}|$formattedAmountStr|$y-$m-$d|${parsed.title.trim().lowercase(Locale.ROOT)}"
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
                        (targetAccountId == null || it.accountId == null || it.accountId == targetAccountId) &&
                            matchesExistingTransaction(
                                existing = it,
                                incomingTitle = parsed.title,
                                incomingAmount = displayAmount,
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
                    // Verificar si existe una coincidencia probable en BD (mismo día y monto exacto) para marcarla como posible duplicado
                    val hasProbableMatch = existing.any {
                        it.type.equals(parsed.type, ignoreCase = true) &&
                            (targetAccountId == null || it.accountId == null || it.accountId == targetAccountId) &&
                            it.effectiveAmountMinor == parsedMinor &&
                            com.example.ui.util.QuincenaFilterHelper.isSameCalendarDay(it.dateMillis, parsed.dateMillis)
                    }

                    if (hasProbableMatch) {
                        suspectedDupCount++
                    }

                    toInsert.add(
                        TransactionEntity(
                            title = parsed.title.trim(),
                            amount = displayAmount,
                            type = parsed.type,
                            category = parsed.category.trim(),
                            dateMillis = parsed.dateMillis,
                            paymentMethod = targetAccount?.name ?: "Efectivo",
                            notes = parsed.notes.trim(),
                            externalId = parsed.externalId?.trim()?.ifBlank { null },
                            importHash = generatedHash,
                            isSuspectedDuplicate = hasProbableMatch,
                            amountMinor = parsedMinor,
                            accountId = targetAccountId,
                            entryKind = TransactionEntryKind.STANDARD
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
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val sb = StringBuilder()
        sb.append("Fecha,Descripción,Monto,Tipo,Categoría,Subcategoría,Beneficiario,Notas,Quincena,Estado,Origen,ID\n")
        for (tx in transactions) {
            val date = sdf.format(Date(tx.dateMillis))
            val typeLabel = if (tx.type == TransactionType.INCOME) "Ingreso" else "Gasto"
            val statusLabel = when (tx.status) {
                TransactionStatus.PROJECTED -> "Proyectado"
                TransactionStatus.PENDING -> "Pendiente"
                TransactionStatus.DISCARDED -> "Descartado"
                else -> "Real"
            }
            val sourceLabel = if (tx.source == TransactionSource.IMPORTED) "Importado Excel" else "Manual"
            val quincena = tx.quincenaLabel
            val subCat = tx.familyMember
            val extId = tx.externalId ?: ""
            sb.append("\"$date\",")
            sb.append("\"${tx.title.replace("\"", "\"\"")}\",")
            sb.append(String.format(Locale.US, "%.2f", tx.amount)).append(",")
            sb.append("\"$typeLabel\",")
            sb.append("\"${tx.category.replace("\"", "\"\"")}\",")
            sb.append("\"${subCat.replace("\"", "\"\"")}\",")
            sb.append("\"${tx.title.replace("\"", "\"\"")}\",")
            sb.append("\"${tx.notes.replace("\"", "\"\"")}\",")
            sb.append("\"$quincena\",")
            sb.append("\"$statusLabel\",")
            sb.append("\"$sourceLabel\",")
            sb.append("\"$extId\"\n")
        }
        return sb.toString()
    }
}
