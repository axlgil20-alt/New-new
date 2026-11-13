package com.example.ui.util

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TermsAcceptanceManagerTest {

    private lateinit var prefs: InMemorySharedPreferences

    @Before
    fun setUp() {
        prefs = InMemorySharedPreferences()
    }

    @Test
    fun freshInstall_requiresAcceptance() {
        assertTrue(TermsAcceptanceManager.requiresAcceptance(prefs))
    }

    @Test
    fun legacyBooleanWithoutVersionAndTimestamp_requiresExplicitAcceptance() {
        prefs.edit().putBoolean(TermsAcceptanceManager.KEY_LEGACY_ACCEPTED_V1, true).apply()
        assertTrue(TermsAcceptanceManager.requiresAcceptance(prefs))
    }

    @Test
    fun recordAcceptance_persistsVersionAndTimestampAndClearsRequirement() {
        val timestamp = 1762972800000L
        val record = TermsAcceptanceManager.recordAcceptance(
            prefs = prefs,
            version = TermsAcceptanceManager.CURRENT_TERMS_VERSION,
            acceptedAtMillis = timestamp
        )

        assertEquals(TermsAcceptanceManager.CURRENT_TERMS_VERSION, record.version)
        assertEquals(timestamp, record.acceptedAtMillis)
        assertNotNull(record.acceptedAtIso)
        assertFalse(record.acceptedAtIso.isNullOrBlank())

        assertFalse(TermsAcceptanceManager.requiresAcceptance(prefs))
        assertEquals(
            TermsAcceptanceManager.CURRENT_TERMS_VERSION,
            prefs.getString(TermsAcceptanceManager.KEY_ACCEPTED_VERSION, null)
        )
        assertEquals(
            timestamp,
            prefs.getLong(TermsAcceptanceManager.KEY_ACCEPTED_AT_MILLIS, 0L)
        )
        assertTrue(prefs.getBoolean(TermsAcceptanceManager.KEY_LEGACY_ACCEPTED_V1, false))
    }

    @Test
    fun versionUpgrade_requiresReAcceptanceAndUpdatesTimestamp() {
        val t1 = 1762972800000L
        val t2 = 1765564800000L

        TermsAcceptanceManager.recordAcceptance(prefs, version = "1.0", acceptedAtMillis = t1)
        assertFalse(TermsAcceptanceManager.requiresAcceptance(prefs, currentVersion = "1.0"))

        // Al cambiar la versión de términos, debe volver a exigir aceptación
        assertTrue(TermsAcceptanceManager.requiresAcceptance(prefs, currentVersion = "2.0"))

        val updated = TermsAcceptanceManager.recordAcceptance(prefs, version = "2.0", acceptedAtMillis = t2)
        assertEquals("2.0", updated.version)
        assertEquals(t2, updated.acceptedAtMillis)
        assertFalse(TermsAcceptanceManager.requiresAcceptance(prefs, currentVersion = "2.0"))

        val stored = TermsAcceptanceManager.getAcceptanceRecord(prefs)
        assertEquals("2.0", stored.version)
        assertEquals(t2, stored.acceptedAtMillis)
    }

    @Test
    fun zeroOrNegativeTimestamp_requiresAcceptance() {
        prefs.edit()
            .putString(TermsAcceptanceManager.KEY_ACCEPTED_VERSION, TermsAcceptanceManager.CURRENT_TERMS_VERSION)
            .putLong(TermsAcceptanceManager.KEY_ACCEPTED_AT_MILLIS, 0L)
            .apply()

        assertTrue(TermsAcceptanceManager.requiresAcceptance(prefs))
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl(data)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class EditorImpl(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = values
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = value
            }
            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) pending[key] = null
            }
            override fun clear(): SharedPreferences.Editor = apply {
                clearRequested = true
            }
            override fun commit(): Boolean {
                if (clearRequested) target.clear()
                pending.forEach { (k, v) ->
                    if (v == null) target.remove(k) else target[k] = v
                }
                return true
            }
            override fun apply() {
                commit()
            }
        }
    }
}
