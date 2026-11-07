package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object TransactionType {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
}

object RecurringFrequency {
    const val BIWEEKLY = "BIWEEKLY"
    const val MONTHLY_15TH = "MONTHLY_15TH"
    const val MONTHLY_END = "MONTHLY_END"
}

data class ImportSummary(
    val newCount: Int,
    val duplicateCount: Int,
    val errorCount: Int,
    val totalProcessed: Int,
    val suspectedDuplicateCount: Int = 0
)

data class HighlightColorOption(
    val name: String,
    val colorHex: Long,
    val label: String
)

data class AppThemeOption(
    val name: String,
    val primaryHex: Long,
    val containerHex: Long
)

data class CategoryInfo(
    val name: String,
    val iconKey: String,
    val type: String,
    val colorHex: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val dateMillis: Long,
    val paymentMethod: String = "Efectivo",
    val familyMember: String = "Personal",
    val notes: String = "",
    val isAntExpense: Boolean = false,
    val isUnplanned: Boolean = false,
    val originalPlannedAmount: Double? = null,
    val highlightColorHex: Long = 0L,
    val isVerified: Boolean = false,
    val isTentative: Boolean = false,
    val tentativeDecision: String = "", // "PENDING", "CONFIRMED", "DISCARDED"
    val externalId: String? = null,
    val importHash: String? = null,
    val isSuspectedDuplicate: Boolean = false
)

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey
    val category: String,
    val monthlyLimit: Double,
    val reason: String = "",
    val tipForControl: String = ""
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDateMillis: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String,
    val familyMember: String = "Personal",
    val paymentMethod: String = "Efectivo",
    val notes: String = "",
    val isActive: Boolean = true
)

object PaymentFrequency {
    const val ONCE = "ONCE"
    const val WEEKLY = "WEEKLY"
    const val BIWEEKLY = "BIWEEKLY"
    const val MONTHLY = "MONTHLY"
    const val CUSTOM = "CUSTOM"
}

object PaymentStatus {
    const val PENDING = "PENDING"
    const val PAID = "PAID"
    const val CANCELLED = "CANCELLED"
}

@Entity(tableName = "payment_commitments")
data class PaymentCommitmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val beneficiaryName: String,
    val description: String = "",
    val amount: Double,
    val dueDateMillis: Long,
    val category: String = "Vivienda & Servicios",
    val frequency: String = PaymentFrequency.ONCE,
    val isVariableAmount: Boolean = false,
    val reminderDaysBefore: Int = 1, // 0 = Mismo día, 1 = 1 día antes, 2 = 2 días antes, etc.
    val status: String = PaymentStatus.PENDING,
    val isPaid: Boolean = false,
    val paidDateMillis: Long? = null,
    val linkedTransactionId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "payment_beneficiaries")
data class BeneficiaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val category: String = "Vivienda & Servicios",
    val defaultDescription: String = "",
    val phoneOrContact: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "smart_cart_items")
data class SmartCartItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "Unidad",
    val brand: String = "",
    val estimatedUnitPrice: Double = 0.0,
    val isChecked: Boolean = false,
    val storeName: String = "General",
    val priceSource: String = "MANUAL", // "MANUAL", "HISTORIAL", "VERIFICADO", "PANTRY_VERDE", "PANTRY_AMARILLO", "PANTRY_ROJO" (+CART)
    val notes: String = "",
    val productUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shopping_templates")
data class ShoppingTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val itemsJson: String = "[]",
    val itemCount: Int = 0,
    val estimatedTotal: Double = 0.0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class TemplateItemData(
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "Unidad",
    val brand: String = "",
    val estimatedUnitPrice: Double = 0.0,
    val storeName: String = "General",
    val notes: String = ""
)

fun SmartCartItemEntity.isPantryProduct(): Boolean =
    priceSource.startsWith("PANTRY_")

fun SmartCartItemEntity.getPantryStatus(): String = when {
    priceSource.contains("PANTRY_VERDE") -> "PANTRY_VERDE"
    priceSource.contains("PANTRY_AMARILLO") -> "PANTRY_AMARILLO"
    priceSource.contains("PANTRY_ROJO") -> "PANTRY_ROJO"
    else -> "PANTRY_AMARILLO"
}

fun SmartCartItemEntity.getPantryStatusDisplay(): String = when (getPantryStatus()) {
    "PANTRY_VERDE" -> "Verde (Suficiente)"
    "PANTRY_AMARILLO" -> "Amarillo (Por agotarse)"
    "PANTRY_ROJO" -> "Rojo (Agotado)"
    else -> "Amarillo (Por agotarse)"
}

fun SmartCartItemEntity.isInShoppingList(): Boolean =
    priceSource.contains("+CART") ||
    priceSource.contains("|CART") ||
    priceSource == "REPOSICION" ||
    priceSource == "OCR" ||
    !priceSource.startsWith("PANTRY_")

fun SmartCartItemEntity.matchesProduct(otherName: String, otherBrand: String = ""): Boolean {
    val sameName = this.name.trim().equals(otherName.trim(), ignoreCase = true)
    if (!sameName) return false
    val thisBrand = this.brand.trim()
    val thatBrand = otherBrand.trim()
    if (thisBrand.isEmpty() || thatBrand.isEmpty()) return true
    return thisBrand.equals(thatBrand, ignoreCase = true)
}

data class RecurringExpenseWarning(
    val title: String,
    val amount: Double,
    val dueDateMessage: String,
    val severity: String = "MEDIUM"
)

object DefaultData {
    val highlightColors = listOf(
        HighlightColorOption("Ninguno", 0L, "Sin resaltar"),
        HighlightColorOption("Amarillo", 0xFFFEF08AL, "Importante"),
        HighlightColorOption("Verde", 0xFFBBF7D0L, "Aprobado / Pagado"),
        HighlightColorOption("Rosa", 0xFFFECDD3L, "Urgente / Atención"),
        HighlightColorOption("Celeste", 0xFFBAE6FDL, "Por Conciliar"),
        HighlightColorOption("Púrpura", 0xFFDDD6FEL, "Gasto Personal / Extra")
    )

    val appThemes = listOf(
        AppThemeOption("Verde Quetzal & Lujo", 0xFF0F5132L, 0xFFD1E7DDL),
        AppThemeOption("Azul Zafiro Ejecutivo", 0xFF0D6EFDL, 0xFFCFE2FFL),
        AppThemeOption("Dorado Presidencial", 0xFFB45309L, 0xFFFEF3C7L),
        AppThemeOption("Grafito Minimalista", 0xFF334155L, 0xFFE2E8F0L)
    )

    val expenseCategories = listOf(
        CategoryInfo("Alimentación & Súper", "shopping_cart", TransactionType.EXPENSE, 0xFF10B981L),
        CategoryInfo("Transporte & Combustible", "directions_bus", TransactionType.EXPENSE, 0xFFF59E0BL),
        CategoryInfo("Vivienda & Servicios", "bolt", TransactionType.EXPENSE, 0xFF14B8A6L),
        CategoryInfo("Educación", "school", TransactionType.EXPENSE, 0xFF6366F1L),
        CategoryInfo("Salud & Medicina", "medical_services", TransactionType.EXPENSE, 0xFFEF4444L),
        CategoryInfo("Ropa & Calzado", "shopping_bag", TransactionType.EXPENSE, 0xFF8B5CF6L),
        CategoryInfo("Telefonía & Internet", "phone_android", TransactionType.EXPENSE, 0xFF06B6D4L),
        CategoryInfo("Antojitos & Gastos Hormiga", "bug_report", TransactionType.EXPENSE, 0xFFE11D48L),
        CategoryInfo("Ahorro & Metas", "savings", TransactionType.EXPENSE, 0xFF059669L),
        CategoryInfo("Otros Gastos", "more_horiz", TransactionType.EXPENSE, 0xFF64748BL)
    )

    val incomeCategories = listOf(
        CategoryInfo("Salario / Sueldo", "account_balance_wallet", TransactionType.INCOME, 0xFF10B981L),
        CategoryInfo("Bono / Extra", "trending_up", TransactionType.INCOME, 0xFF059669L),
        CategoryInfo("Aguinaldo", "star", TransactionType.INCOME, 0xFFD97706L),
        CategoryInfo("Bono 14", "celebration", TransactionType.INCOME, 0xFF16A34AL),
        CategoryInfo("Otros Ingresos", "savings", TransactionType.INCOME, 0xFF0D9488L)
    )

    val familyMembers = listOf("Todos", "Personal", "Hogar", "Familia")
}

object TransactionStatus {
    const val REAL = "REAL"
    const val PENDING = "PENDING"
    const val PROJECTED = "PROJECTED"
    const val DISCARDED = "DISCARDED"
}

object TransactionSource {
    const val ALL = "ALL"
    const val MANUAL = "MANUAL"
    const val IMPORTED = "IMPORTED"
}

object DuplicateFilterMode {
    const val ALL = "ALL"
    const val ONLY_DUPLICATES = "ONLY_DUPLICATES"
    const val EXCLUDE_DUPLICATES = "EXCLUDE_DUPLICATES"
}

object FilterPeriod {
    const val ALL = "ALL"
    const val DAY = "DAY"
    const val WEEK = "WEEK"
    const val MONTH = "MONTH"
    const val YEAR = "YEAR"
}

val TransactionEntity.beneficiary: String
    get() = title

val TransactionEntity.isGastoSupuesto: Boolean
    get() = (type == TransactionType.EXPENSE) &&
        (tentativeDecision != "CONFIRMED") &&
        (isTentative ||
         tentativeDecision == "PENDING" ||
         notes.contains("[SUPUESTO]", ignoreCase = true) ||
         notes.contains("Supuesto", ignoreCase = true) ||
         notes.contains("[PENDIENTE]", ignoreCase = true) ||
         notes.contains("Pendiente", ignoreCase = true))

val TransactionEntity.isGastoReal: Boolean
    get() = (type == TransactionType.EXPENSE) &&
        (tentativeDecision != "DISCARDED") &&
        !isGastoSupuesto

val TransactionEntity.isIngresoReal: Boolean
    get() = (type == TransactionType.INCOME)

val TransactionEntity.status: String
    get() = when {
        isTentative && tentativeDecision == "DISCARDED" -> TransactionStatus.DISCARDED
        isGastoSupuesto -> TransactionStatus.PROJECTED
        else -> TransactionStatus.REAL
    }

val TransactionEntity.source: String
    get() = if (!importHash.isNullOrBlank() || !externalId.isNullOrBlank() || notes.contains("Importado de Excel", ignoreCase = true)) {
        TransactionSource.IMPORTED
    } else {
        TransactionSource.MANUAL
    }

val TransactionEntity.quincenaNumber: Int
    get() = com.example.ui.util.QuincenaFilterHelper.getBudgetPeriod(dateMillis, type).quincena

val TransactionEntity.quincenaLabel: String
    get() = if (quincenaNumber == 1) "1ra Quincena (1 - 15)" else "2da Quincena (16 - fin de mes)"

fun PaymentCommitmentEntity.isOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (isPaid || status == PaymentStatus.PAID) return false
    val (todayStart, _) = com.example.ui.util.QuincenaFilterHelper.getDayRange(nowMillis)
    return dueDateMillis < todayStart
}

fun PaymentCommitmentEntity.isDueToday(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val (todayStart, todayEnd) = com.example.ui.util.QuincenaFilterHelper.getDayRange(nowMillis)
    return dueDateMillis in todayStart..todayEnd
}

fun PaymentCommitmentEntity.isUpcoming(nowMillis: Long = System.currentTimeMillis(), days: Int = 7): Boolean {
    if (isPaid || status == PaymentStatus.PAID) return false
    val (todayStart, _) = com.example.ui.util.QuincenaFilterHelper.getDayRange(nowMillis)
    val horizonMillis = todayStart + days * 24L * 60L * 60L * 1000L + (24L * 60L * 60L * 1000L - 1)
    return dueDateMillis in todayStart..horizonMillis
}
