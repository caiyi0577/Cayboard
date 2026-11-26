package com.cayboard.settings

import com.cayboard.engine.compat.LegacyNames
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Versioned app-owned assets only; personal dictionaries and user databases are never enumerated. */
internal object BundledInputSync {
    private fun hash(input: InputStream): String = input.use {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        while (true) { val n = it.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
        digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    /** Unknown large dictionaries may contain user-added words; large model files are reproducible. */
    private const val UNKNOWN_BACKUP_LIMIT = 1L shl 20
    private const val LEGACY_MANIFEST = LegacyNames.OLD_BUNDLED_MANIFEST
    private const val LEGACY_REVISION = LegacyNames.OLD_BUNDLED_REVISION
    private val REVISION_DIR = Regex("[0-9a-f]{64}")
    private val RETIRED_JAPANESE = Regex("""jaroomaji(\.[a-z0-9_]+)?\.dict\.yaml|cayboard_japanese_particles\.dict\.yaml|lua/cayboard_japanese_completion\.lua""")

    private fun parse(manifest: String) =
        manifest.lineSequence().filter { it.isNotBlank() }.map { it.split('\t') }.toList()

    fun install(target: File, backups: File, manifest: String, open: (String) -> InputStream): Boolean {
        val revision = hash(manifest.byteInputStream())
        val stamp = File(target, ".cayboard-bundled-revision")
        val installedManifest = File(target, ".cayboard-bundled-manifest")
        val entries = parse(manifest)
        // 上一次装进来的每个文件的哈希和大小：和它一致说明用户没改过，替换前不用备份。
        val previous = runCatching {
            val source = installedManifest.takeIf { it.isFile } ?: File(target, LEGACY_MANIFEST)
            parse(source.readText())
        }.getOrDefault(emptyList())
            .filter { it.size == 4 }
            .associate { entry -> entry[3] to (entry[0] to entry[1].toLongOrNull()) }
        // A bundle may encounter either the original name or one already migrated on startup.
        val previousNames = previous.mapKeys { (name, _) -> LegacyNames.canonical(name) } + previous
        fun destination(relative: String): File {
            val result = File(target, relative).canonicalFile
            require(result.path.startsWith(target.canonicalPath + File.separator))
            require(!relative.endsWith(".custom.yaml") && !relative.contains("userdb"))
            return result
        }
        // No large dictionary hashing on ordinary starts. A new bundle revision always checks content.
        if (stamp.isFile && stamp.readText() == revision && entries.all {
                val file = destination(it[3]); file.isFile && file.length() == it[1].toLong()
            }) return false
        var changed = false
        for (entry in entries) {
            require(entry.size == 4)
            val (expected, size, asset, relative) = entry
            val file = destination(relative)
            if (file.isFile && file.length() == size.toLong() && hash(file.inputStream()) == expected) continue
            file.parentFile?.mkdirs()
            val pending = File(file.parentFile, "${file.name}.cayboard-installing")
            try {
                open(asset).use { input -> pending.outputStream().use { input.copyTo(it) } }
                check(pending.length() == size.toLong() && hash(pending.inputStream()) == expected) { "Bundled asset checksum mismatch: $asset" }
                val installed = previousNames[relative]
                if (file.exists() && userModified(file, installed)) {
                    val backup = File(backups, "$revision/$relative")
                    require(backup.canonicalPath.startsWith(backups.canonicalPath + File.separator))
                    if (!backup.exists()) { backup.parentFile?.mkdirs(); file.copyTo(backup) }
                }
                // Invalidate before replacement: a missing stamp makes an interrupted upgrade run again and changes the
                // deployment hash. The compiled cache in build/ stays. Table and prism artifacts keep their source
                // checksums (DictCompiler::Compile), so only artifacts whose sources changed are rebuilt;
                // compiled configs are rebuilt when a dependency is newer. Wiping build/ here made every upgrade that
                // touched any bundled file recompile every dictionary on the first keyboard open.
                stamp.delete()
                Files.move(pending.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                changed = true
            } finally { pending.delete() }
        }
        // CyLM files (cylm_*) change with the tier a package carries: one the new package no longer has is removed
        // when unmodified, so switching to a smaller tier does not leave hundreds of MB behind. The jaroomaji
        // dictionaries that Japanese used before azooKey go the same way. Other files a package dropped stay,
        // as before (a lightweight edition keeps the enhancement models already installed).
        val carried = entries.map { it[3] }.toSet()
        for ((relative, installed) in previousNames) {
            if (!(relative.startsWith("cylm_") || RETIRED_JAPANESE.matches(relative) ||
                    InputResourcePolicy.isRetiredAsset(relative)) || relative in carried) continue
            val file = destination(relative)
            if (file.isFile && !userModified(file, installed) && file.delete()) {
                stamp.delete()
                changed = true
            }
        }
        target.mkdirs()
        val pendingManifest = File(target, ".cayboard-bundled-manifest.tmp")
        pendingManifest.writeText(manifest)
        Files.move(pendingManifest.toPath(), installedManifest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        val pendingStamp = File(target, ".cayboard-bundled-revision.tmp")
        pendingStamp.writeText(revision)
        Files.move(pendingStamp.toPath(), stamp.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File(target, LEGACY_MANIFEST).delete()
        File(target, LEGACY_REVISION).delete()
        pruneBackups(backups)
        return changed
    }

    private fun userModified(file: File, installed: Pair<String, Long?>?): Boolean {
        if (installed == null) return file.length() < UNKNOWN_BACKUP_LIMIT || file.name.endsWith(".dict.yaml")
        val (expected, size) = installed
        return file.length() != size || hash(file.inputStream()) != expected
    }

    /** 每次升级的备份放在以清单哈希命名的目录里；只留最近一次，其它功能的备份目录不动。 */
    internal fun pruneBackups(backups: File) {
        val revisions = backups.listFiles { file -> file.isDirectory && REVISION_DIR.matches(file.name) }.orEmpty()
        revisions.sortedByDescending { it.lastModified() }.drop(1).forEach { it.deleteRecursively() }
    }
}
