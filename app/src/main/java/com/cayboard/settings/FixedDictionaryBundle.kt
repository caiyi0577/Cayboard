package com.cayboard.settings

import android.content.Context

/** Dictionaries are immutable APK resources. Preserve learned words and previously imported local lists. */
object FixedDictionaryBundle {
    private const val MIGRATED = "fixed_dictionary_bundle_v1"
    val tables = listOf("cayboard_corpus", "fixed_ice", "fixed_zhwiki", "fixed_baidu")
    private val retiredIds = CellDictionaryCatalog.builtins.map { it.id }.toSet() +
        CellDictionaryCatalog.offers.map { it.id }

    internal fun retainLocal(ids: Set<String>): Set<String> = ids - retiredIds

    fun migrateLegacy(context: Context) {
        val prefs = SettingsPreferences.getPrefsPublic(context)
        if (prefs.getBoolean(MIGRATED, false)) return
        val store = CellDictionaryManager.store(context)
        val previous = store.read()
        (previous.selected intersect retiredIds).forEach { store.select(it, false) }
        store.markApplied(retainLocal(previous.applied))
        prefs.edit().remove("cell_word_auto_update").remove("cell_word_auto_update_checked_at")
            .putBoolean(MIGRATED, true).commit()
    }
}
