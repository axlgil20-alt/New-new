package com.example.ui.util

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TermsAcceptanceRecord(
    val version: String?,
    val acceptedAtMillis: Long,
    val acceptedAtIso: String?
)

/**
 * Gestiona el registro local estricto de aceptación de Términos y Condiciones
 * (versión aceptada + fecha/hora) y la verificación ante cambios de versión.
 */
object TermsAcceptanceManager {
    const val PREFS_NAME = "quetzi_prefs"
    const val CURRENT_TERMS_VERSION = "1.0"

    const val KEY_ACCEPTED_VERSION = "terms_accepted_version"
    const val KEY_ACCEPTED_AT_MILLIS = "terms_accepted_at_millis"
    const val KEY_ACCEPTED_AT_ISO = "terms_accepted_at_iso"
    const val KEY_LEGACY_ACCEPTED_V1 = "has_accepted_terms_v1"

    fun requiresAcceptance(
        prefs: SharedPreferences,
        currentVersion: String = CURRENT_TERMS_VERSION
    ): Boolean {
        val acceptedVersion = prefs.getString(KEY_ACCEPTED_VERSION, null)
        val acceptedAtMillis = prefs.getLong(KEY_ACCEPTED_AT_MILLIS, 0L)
        return acceptedVersion != currentVersion || acceptedAtMillis <= 0L
    }

    fun recordAcceptance(
        prefs: SharedPreferences,
        version: String = CURRENT_TERMS_VERSION,
        acceptedAtMillis: Long = System.currentTimeMillis()
    ): TermsAcceptanceRecord {
        val isoTimestamp = formatIsoTimestamp(acceptedAtMillis)
        prefs.edit()
            .putString(KEY_ACCEPTED_VERSION, version)
            .putLong(KEY_ACCEPTED_AT_MILLIS, acceptedAtMillis)
            .putString(KEY_ACCEPTED_AT_ISO, isoTimestamp)
            .putBoolean(KEY_LEGACY_ACCEPTED_V1, true)
            .apply()
        return TermsAcceptanceRecord(
            version = version,
            acceptedAtMillis = acceptedAtMillis,
            acceptedAtIso = isoTimestamp
        )
    }

    fun getAcceptanceRecord(prefs: SharedPreferences): TermsAcceptanceRecord {
        return TermsAcceptanceRecord(
            version = prefs.getString(KEY_ACCEPTED_VERSION, null),
            acceptedAtMillis = prefs.getLong(KEY_ACCEPTED_AT_MILLIS, 0L),
            acceptedAtIso = prefs.getString(KEY_ACCEPTED_AT_ISO, null)
        )
    }

    fun formatIsoTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(millis))
    }

    fun formatDisplayTimestamp(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "GT"))
        return sdf.format(Date(millis))
    }
}
