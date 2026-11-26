package com.cayboard.engine.compat

import android.content.Context
import com.cayboard.settings.SettingsPreferences
import java.io.File

internal object LegacyNameMigration {
    @Synchronized
    fun migrate(context: Context, force: Boolean = false) {
        val prefs = SettingsPreferences.getPrefsPublic(context)
        val changed = LegacyNames.migrateFiles(context.filesDir, force)
        val complete = prefs.getBoolean("cayboard_names_v2", false)
        if (!force && complete && !changed) return
        val directory = File(context.applicationInfo.dataDir, "shared_prefs")
        val oldSettings = context.getSharedPreferences(LegacyNames.OLD_SETTINGS_PREFS, Context.MODE_PRIVATE)
        val migratedSettings = LegacyNames.mergeLegacyPreferences(prefs.all, oldSettings.all)
        val prefsEditor = prefs.edit()
        for ((key, value) in migratedSettings) {
            if (prefs.all[key] == value) continue
            when (value) {
                is Boolean -> prefsEditor.putBoolean(key, value)
                is Int -> prefsEditor.putInt(key, value)
                is Long -> prefsEditor.putLong(key, value)
                is Float -> prefsEditor.putFloat(key, value)
                is String -> prefsEditor.putString(key, value)
                is Set<*> -> prefsEditor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        if (changed) prefsEditor.putBoolean("deployment_done", false).remove("deployment_hash")
        check(prefsEditor.commit()) { "Cannot finish input-name migration" }

        // Migrate legacy identifiers in other stores without clearing unrelated settings.
        directory.listFiles().orEmpty().filter { it.extension == "xml" }
            .map { it.nameWithoutExtension }
            .filter { it != SettingsPreferences.PREFS_NAME && it != LegacyNames.OLD_SETTINGS_PREFS }
            .distinct()
            .forEach { name ->
                val store = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                val editor = store.edit()
                for ((key, value) in store.all) {
                    val newKey = LegacyNames.canonical(key)
                    val newValue = LegacyNames.preferenceValue(key, value)
                    if (newKey == key && value == newValue) continue
                    if (newKey != key && store.contains(newKey)) { editor.remove(key); continue }
                    if (newKey != key) editor.remove(key)
                    when (newValue) {
                        is Boolean -> editor.putBoolean(newKey, newValue)
                        is Int -> editor.putInt(newKey, newValue)
                        is Long -> editor.putLong(newKey, newValue)
                        is Float -> editor.putFloat(newKey, newValue)
                        is String -> editor.putString(newKey, newValue)
                        is Set<*> -> editor.putStringSet(newKey, newValue.filterIsInstance<String>().toSet())
                    }
                }
                check(editor.commit()) { "Cannot migrate input preferences: $name" }
            }

        // Remove the old settings file only after the destination has been durably written.
        if (oldSettings.all.isNotEmpty()) {
            val editor = oldSettings.edit()
            oldSettings.all.keys.forEach(editor::remove)
            check(editor.commit()) { "Cannot clear legacy input preferences" }
        }
        File(directory, "${LegacyNames.OLD_SETTINGS_PREFS}.xml").delete()
        check(prefs.edit().putBoolean("cayboard_names_v2", true).commit()) {
            "Cannot finish input-name migration"
        }
    }
}
