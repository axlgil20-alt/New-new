package com.example.data.service

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FinancialDiagnosticReport(
    val periodTitle: String,
    val antExpensesAmount: Double,
    val discrepancyAmount: Double,
    val summaryHeadline: String,
    val antExpenseImpact: String,
    val inflationAndWorkTips: String,
    val actionableRules: List<String>,
    val aiPersonalizedAdvice: String
)

object FinancialDiagnosticService {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun generateLocalDiagnostic(
        periodName: String,
        income: Double,
        expense: Double,
        tentativeExpenses: Double,
        antExpenseSum: Double,
        budgetLimitsSum: Double,
        budgetOverruns: List<String>,
        currency: String
    ): FinancialDiagnosticReport {
        val net = income - expense
        val simulatedNet = net - tentativeExpenses

        val headline = when {
            net < 0 -> "Atención: El presupuesto de este periodo está en déficit por $currency${"%.2f".format(kotlin.math.abs(net))}."
            simulatedNet < 0 -> "Precaución: El saldo actual es positivo ($currency${"%.2f".format(net)}), pero si realizas las compras supuestas ($currency${"%.2f".format(tentativeExpenses)}) quedarás en déficit ($currency${"%.2f".format(simulatedNet)})."
            net > 0 && tentativeExpenses > 0 -> "Buen control: Saldo positivo asegurado ($currency${"%.2f".format(net)}). Las compras supuestas ($currency${"%.2f".format(tentativeExpenses)}) están dentro de tu margen."
            else -> "Finanzas Saludables: Tienes un margen favorable de $currency${"%.2f".format(net)}."
        }

        val antImpact = if (antExpenseSum > 0) {
            "Los gastos hormiga suman $currency${"%.2f".format(antExpenseSum)} (${"%.1f".format(if (income > 0) (antExpenseSum / income) * 100 else 0.0)}% del ingreso). Reducirlos a la mitad te liberaría $currency${"%.2f".format(antExpenseSum / 2)} para ahorro o metas familiares."
        } else {
            "No se han registrado gastos hormiga en este ciclo. ¡Excelente disciplina!"
        }

        val rules = mutableListOf(
            "Regla de la Quincena: Asigna primero los compromisos fijos del día 15 y reserva el margen de transporte y comida.",
            "Antes de realizar un gasto supuesto: Espera 48 horas o decide 'Descartar' en la app para sumar ese monto a tu fondo de ahorro.",
            "Fondo de Imprevistos: Destina al menos el 10% del excedente neto a tu meta de Emergencia Familiar."
        )

        if (budgetOverruns.isNotEmpty()) {
            rules.add("Categorías que superan el límite fijado: ${budgetOverruns.joinToString(", ")}.")
        }

        return FinancialDiagnosticReport(
            periodTitle = periodName,
            antExpensesAmount = antExpenseSum,
            discrepancyAmount = if (budgetLimitsSum > 0) (expense - budgetLimitsSum).coerceAtLeast(0.0) else 0.0,
            summaryHeadline = headline,
            antExpenseImpact = antImpact,
            inflationAndWorkTips = "Monitorea los gastos de transporte y alimentación diaria; son los más sensibles a variaciones mensuales.",
            actionableRules = rules,
            aiPersonalizedAdvice = "Tu presupuesto familiar muestra una estructura quincenal bien definida. Mantén el registro constante para evitar fugas financieras."
        )
    }

    suspend fun getGeminiAdvice(promptText: String): String {
        return try {
            // Intentar llamada ligera o devolver recomendación basada en reglas
            val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
            if (apiKey.isBlank()) {
                return "Consejo Financiero Familiar: Prioriza siempre pagar las cuotas escolares y compromisos fijos en la primera quincena. Si tienes compras supuestas pendientes, revísalas al final del mes antes de efectuarlas."
            }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
            val jsonPayload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        })
                    })
                })
            }
            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return parts.getJSONObject(0).optString("text")
                    }
                }
            }
            "Recomendación: Monitorea tus compras supuestas en el panel de simulaciones para asegurar que tus compromisos quincenales queden cubiertos."
        } catch (_: Exception) {
            "Recomendación Asesor: Para maximizar tu ahorro, mantén actualizado el balance de gastos fijos y utiliza la opción 'Descartar' en gastos supuestos innecesarios."
        }
    }
}
