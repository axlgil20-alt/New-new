package com.example.data.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Utilidades monetarias exactas para QUETZI.
 *
 * Convención contable:
 * - Persistencia en Room: `Long` en unidades monetarias menores (centavos para GTQ/USD, escala = 2).
 * - Aritmética de dominio: `BigDecimal` con escala 2 y [RoundingMode.HALF_UP] para resultados monetarios,
 *   y [DOMAIN_MATH_CONTEXT] (precisión 16, `HALF_UP`) para divisiones y tasas intermedias.
 */
object MoneyMinorUnits {
    const val CURRENCY_SCALE = 2
    val ROUNDING_MODE: RoundingMode = RoundingMode.HALF_UP
    val DOMAIN_MATH_CONTEXT: MathContext = MathContext(16, RoundingMode.HALF_UP)

    private val ONE_HUNDRED = BigDecimal(100)

    /** Convierte unidades menores exactas (`Long`, ej. 17550L) a `BigDecimal` ("175.50"). */
    fun toBigDecimal(minorUnits: Long): BigDecimal =
        BigDecimal.valueOf(minorUnits, CURRENCY_SCALE)

    /**
     * Convierte un `BigDecimal` a unidades menores exactas (`Long`), redondeando a 2 decimales
     * con `RoundingMode.HALF_UP`.
     */
    fun fromBigDecimal(amount: BigDecimal): Long =
        amount.setScale(CURRENCY_SCALE, ROUNDING_MODE)
            .movePointRight(CURRENCY_SCALE)
            .longValueExact()

    /**
     * Adaptador exclusivo de frontera heredada (`Double` -> `Long` centavos).
     * Nunca debe usarse para aritmética financiera interna.
     */
    fun fromDouble(amount: Double): Long {
        if (!amount.isFinite()) return 0L
        return BigDecimal.valueOf(amount)
            .setScale(CURRENCY_SCALE, ROUNDING_MODE)
            .movePointRight(CURRENCY_SCALE)
            .toLong()
    }

    /**
     * Adaptador exclusivo de visualización heredada (`Long` centavos -> `Double`).
     */
    fun toDisplayDouble(minorUnits: Long): Double =
        toBigDecimal(minorUnits).toDouble()

    /** Suma exacta de montos en unidades menores usando `BigDecimal` (sin desbordamiento silencioso). */
    fun sumMinorToBigDecimal(minorAmounts: Iterable<Long>): BigDecimal {
        var acc = BigDecimal.ZERO.setScale(CURRENCY_SCALE, ROUNDING_MODE)
        for (value in minorAmounts) {
            acc = acc.add(toBigDecimal(value))
        }
        return acc.setScale(CURRENCY_SCALE, ROUNDING_MODE)
    }

    /**
     * Distribuye exactamente [totalMinor] entre [parts] cuotas (>= 1) sin perder ni inventar centavos.
     *
     * Regla explícita de residuos de centavos:
     * - Para `totalMinor >= 0`: `base = totalMinor / parts` y `remainder = totalMinor % parts`.
     * - Las primeras `parts - remainder` cuotas reciben `base` centavos, y las últimas `remainder`
     *   cuotas absorben `+1` centavo cada una.
     * - Propiedades garantizadas: todas las cuotas son `>= 0`, la diferencia máxima entre cualquier
     *   par de cuotas es de `Q0.01`, y la suma total equivale exactamente a `totalMinor`.
     */
    fun distributeEvenly(totalMinor: Long, parts: Int): List<Long> {
        require(parts > 0) { "La cantidad de cuotas debe ser mayor a 0 (recibido: $parts)." }
        require(totalMinor >= 0L) { "El monto a distribuir no puede ser negativo (recibido: $totalMinor)." }

        val base = totalMinor / parts.toLong()
        val remainder = (totalMinor % parts.toLong()).toInt()
        val firstWithExtra = parts - remainder

        return List(parts) { index ->
            if (index >= firstWithExtra) base + 1L else base
        }
    }

    /** Convierte basis points (ej. 2400 bps = 24.00%) a tasa mensual decimal (`0.02`). */
    fun annualBpsToMonthlyRate(annualBps: Int): BigDecimal {
        if (annualBps <= 0) return BigDecimal.ZERO
        val annualDecimal = BigDecimal.valueOf(annualBps.toLong())
            .divide(BigDecimal.valueOf(10_000L), DOMAIN_MATH_CONTEXT)
        return annualDecimal.divide(BigDecimal.valueOf(12L), DOMAIN_MATH_CONTEXT)
    }

    fun percentOf(partMinor: Long, wholeMinor: Long): BigDecimal {
        if (wholeMinor <= 0L || partMinor <= 0L) {
            return BigDecimal.ZERO.setScale(CURRENCY_SCALE, ROUNDING_MODE)
        }
        return toBigDecimal(partMinor)
            .multiply(ONE_HUNDRED)
            .divide(toBigDecimal(wholeMinor), CURRENCY_SCALE, ROUNDING_MODE)
    }
}
