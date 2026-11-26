package com.cayboard.engine.compat

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The only product-name boundary for files and settings created by predecessor versions. */
internal object LegacyNames {
    const val DATA_DIRECTORY = "cayboard_input"
    const val ASSET_BASE = "cayboard_base"
    const val OLD_SETTINGS_PREFS = "kime_settings"
    const val OLD_CONFIG = "xime.yaml"
    const val OLD_CUSTOM_CONFIG = "xime.custom.yaml"
    const val OLD_BUNDLED_MANIFEST = ".cyime-bundled-manifest"
    const val OLD_BUNDLED_REVISION = ".cyime-bundled-revision"
    const val OLD_BUNDLED_CORPUS = "fixed_cy.dict.yaml"
    const val OLD_BACKUP_PREFIX = "_xime_backup/"
    const val OLD_CELL_DICTIONARY_FORMAT = "cyime.dictionary.v1"
    const val OLD_HANDWRITING_FORMAT = "xime-handwriting-capture"
    const val OLD_TRANSLATE_ACTION = "com.kingzcheung.xime.plugin.translate:open"
    const val OLD_APP_LOG_PREFIX = "kime_"
    const val OLD_ENGINE_LOG_TOKEN = "rime.kime"
    const val OLD_ASSET_VERSION_KEY = "rime_assets_version"
    const val OLD_ASSET_MARKER = ".cyime-assets-installing"
    const val OLD_CHINESE_DEFAULTS = "cyime_chinese_defaults_v1"
    const val OLD_DOUBLE_PINYIN_DEFAULTS = "cyime_double_pinyin_defaults_v1"
    const val OLD_SOFT_BLUE = "cyime_soft_blue_v1"
    private const val STAMP = ".cayboard-names-v2"

    private val identifierMigrations = listOf(
        "rime_deployment_" to "cayboard_deployment_",
        "rime_assets_" to "cayboard_assets_",
        OLD_ASSET_VERSION_KEY to "cayboard_assets_version",
        "rime_chinese" to "cayboard_chinese",
        "rime_japanese" to "cayboard_japanese",
        "rime_letters" to "cayboard_letters",
        "rime_ice" to "cayboard_pinyin",
        OLD_CUSTOM_CONFIG to "cayboard.custom.yaml",
        OLD_CONFIG to "cayboard.yaml",
        "__xime_english" to "__cayboard_english",
        "fixed_cy" to "cayboard_corpus",
        "CyimeSurnames.txt" to "CayboardSurnames.txt",
        "CyimeTWPhrases.ocd2" to "CayboardTWPhrases.ocd2",
        "CyimeUserKeep.txt" to "CayboardUserKeep.txt",
        "CYIME_" to "CAYBOARD_",
        "Cyime_" to "Cayboard_",
        "cyime_" to "cayboard_",
    )

    /** Rewrite known legacy identifiers without changing prose or user-authored values. */
    fun canonical(value: String): String = identifierMigrations.fold(value) { result, (old, new) ->
        val isPrefix = old.endsWith('_')
        val after = if (isPrefix) "(?=[A-Za-z0-9])" else "(?=$|[^A-Za-z0-9])"
        result.replace(Regex("(?<![A-Za-z0-9])${Regex.escape(old)}$after"), new)
    }

    /** Text chosen by the user is never treated as an identifier. */
    fun preferenceValue(key: String, value: Any?): Any? = when (value) {
        is String -> if (key.contains("schema") || key.contains("layout") ||
            key.contains("input_mode") || key.contains("input_profile") ||
            key.contains("last_input_mode") || key.contains("language_mode")) canonical(value) else value
        is Set<*> -> if (key.contains("schema") || key.contains("layout"))
            value.mapTo(linkedSetOf()) { if (it is String) canonical(it) else it } else value
        else -> value
    }

    /** Canonical keys win when a restored or migrated store contains both generations. */
    fun migratePreferences(values: Map<String, *>): Map<String, Any?> {
        val migrated = linkedMapOf<String, Any?>()
        values.entries.sortedBy { (key, _) -> if (canonical(key) == key) 0 else 1 }
            .forEach { (key, value) ->
                val canonicalKey = canonical(key)
                if (canonicalKey !in migrated) migrated[canonicalKey] = preferenceValue(key, value)
            }
        return migrated
    }

    /** Add legacy settings to the new store while preserving values already saved by Cayboard. */
    fun mergeLegacyPreferences(current: Map<String, *>, legacy: Map<String, *>): Map<String, Any?> {
        val merged = migratePreferences(legacy).toMutableMap()
        merged.putAll(migratePreferences(current))
        return merged
    }

    /** Runs before any native session opens a database. Renames do not decode or rewrite LevelDB. */
    @Synchronized
    fun migrateFiles(filesDir: File, force: Boolean = false): Boolean {
        val root = File(filesDir, DATA_DIRECTORY)
        val old = File(filesDir, "rime")
        val previousStamp = File(root, ".cayboard-names-v1")
        if (!force && File(root, STAMP).isFile && !old.exists()) return false
        var changed = old.exists() || previousStamp.exists()
        if (old.exists()) movePreserving(old, root, File(filesDir, "cayboard-name-conflicts"))
        if (!root.isDirectory) return changed
        // Older installations split engine files and user databases into separate trees.
        listOf("user", "shared").forEach { name ->
            val split = File(root, name)
            if (split.isDirectory) {
                split.listFiles().orEmpty().forEach { movePreserving(it, File(root, it.name),
                    File(filesDir, "cayboard-name-conflicts/$name")) }
                check(split.delete()) { "Cannot finish input directory migration" }
                changed = true
            }
        }
        val files = root.walkBottomUp().filter { it != root && !it.relativeTo(root).invariantSeparatorsPath.startsWith("build/") }
            .toList()
        val previousManifest = listOf(".cayboard-bundled-manifest", OLD_BUNDLED_MANIFEST)
            .map { File(root, it) }.firstOrNull { it.isFile }
        val bundledCorpus = previousManifest?.useLines { lines ->
            lines.any { it.substringAfterLast('\t') == OLD_BUNDLED_CORPUS }
        } == true
        for (source in files) {
            // Preserve the old bundled table's hash until the replacement bundle checks for user edits.
            val retiredDictionary = bundledCorpus && source.name == OLD_BUNDLED_CORPUS
            if (source.isFile && !source.path.contains(".userdb") && !retiredDictionary) {
                if (source.name.endsWith(".dict.yaml")) {
                    // Only the dictionary header contains identifiers; a word list can be hundreds of MB.
                    val header = source.bufferedReader().use { reader ->
                        buildString { repeat(200) {
                            val line = reader.readLine() ?: return@buildString
                            appendLine(line)
                            if (line.trim() == "...") return@buildString
                        } }
                    }
                    val updated = if (header.trimEnd().endsWith("\n...")) canonical(header) else header
                    if (updated != header) {
                        val pending = File(source.parentFile, source.name + ".cayboard-migrating")
                        try {
                            source.bufferedReader().use { input -> pending.bufferedWriter().use { output ->
                                repeat(header.count { it == '\n' }) { input.readLine() }
                                output.write(updated); input.copyTo(output)
                            } }
                            Files.move(pending.toPath(), source.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        } finally { pending.delete() }
                        changed = true
                    }
                } else if (source.extension in setOf("yaml", "yml", "lua", "json") && source.length() <= 4L * 1024 * 1024) {
                    val before = source.readText()
                    val after = canonical(before)
                    if (before != after) { atomicText(source, after); changed = true }
                }
            }
            val renamed = canonical(source.name)
            if (renamed != source.name) {
                movePreserving(source, File(source.parentFile, renamed), File(filesDir, "cayboard-name-conflicts"))
                changed = true
            }
        }
        // Old compiled tables have source checksums and IDs embedded in them. Keep their bytes intact;
        // the renamed schema/dictionary gets a newly compiled cache under its new identity.
        atomicText(File(root, STAMP), "1\n")
        if (previousStamp.exists()) check(previousStamp.delete()) { "Cannot remove legacy migration marker" }
        return changed
    }

    private fun atomicText(file: File, value: String) {
        val pending = File(file.parentFile, file.name + ".cayboard-migrating")
        try {
            pending.writeText(value)
            Files.move(pending.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally { pending.delete() }
    }

    private fun movePreserving(source: File, target: File, conflicts: File) {
        if (!target.exists()) {
            Files.createDirectories(target.toPath().parent)
            Files.move(source.toPath(), target.toPath())
        } else if (source.isDirectory && target.isDirectory && !source.name.endsWith(".userdb")) {
            source.listFiles().orEmpty().forEach { movePreserving(it, File(target, it.name), conflicts) }
            check(source.delete()) { "Cannot finish migration: ${source.name}" }
        } else {
            // Keep each conflicting user database intact, including its manifest and journal files.
            Files.createDirectories(conflicts.toPath())
            var saved = File(conflicts, source.name)
            var suffix = 1
            while (saved.exists()) saved = File(conflicts, source.name + "." + suffix++)
            Files.move(source.toPath(), saved.toPath())
        }
    }
}
