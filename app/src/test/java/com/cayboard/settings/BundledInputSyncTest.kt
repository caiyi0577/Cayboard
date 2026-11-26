package com.cayboard.settings

import com.cayboard.engine.compat.LegacyNames
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BundledInputSyncTest {
    @get:Rule val folder = TemporaryFolder()
    private fun manifest(text: String, name: String = "core.dict.yaml"): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        return "$hash\t${text.toByteArray().size}\tbundled/$name\t$name\n"
    }
    @Test fun removesUneditedRetiredLanguageResourcesAndKeepsPersonalData() {
        val target = folder.newFolder("retired"); val backups = folder.newFolder("retired-backup")
        val old = manifest("chinese", "cayboard_pinyin.schema.yaml") + manifest("japanese", "japanese.schema.yaml") +
            manifest("spanish", "spanish.schema.yaml") + manifest("french", "french.schema.yaml")
        BundledInputSync.install(target, backups, old) { path ->
            when {
                path.endsWith("cayboard_pinyin.schema.yaml") -> "chinese"
                path.endsWith("japanese.schema.yaml") -> "japanese"
                path.endsWith("spanish.schema.yaml") -> "spanish"
                else -> "french"
            }.byteInputStream()
        }
        File(target, "french.schema.yaml").writeText("personal edit")
        File(target, "japanese.userdb").writeText("personal learning")
        val next = manifest("chinese", "cayboard_pinyin.schema.yaml")
        assertTrue(BundledInputSync.install(target, backups, next) { error("Chinese resource is unchanged") })
        assertFalse(File(target, "japanese.schema.yaml").exists())
        assertFalse(File(target, "spanish.schema.yaml").exists())
        assertEquals("personal edit", File(target, "french.schema.yaml").readText())
        assertEquals("personal learning", File(target, "japanese.userdb").readText())
        assertEquals("chinese", File(target, "cayboard_pinyin.schema.yaml").readText())
        assertFalse(BundledInputSync.install(target, backups, next) { error("Already migrated") })
    }

    @Test fun retiredFixedCyDictionaryIsRemovedOnlyWhenItMatchesThePreviousBundle() {
        val target = folder.newFolder("fixed-dictionary"); val backups = folder.newFolder("fixed-dictionary-backup")
        val previous = manifest("old bundled table", "fixed_cy.dict.yaml")
        BundledInputSync.install(target, backups, previous) { "old bundled table".byteInputStream() }
        assertTrue(BundledInputSync.install(target, backups, manifest("new bundled table", "cayboard_corpus.dict.yaml")) {
            "new bundled table".byteInputStream()
        })
        assertFalse(File(target, "fixed_cy.dict.yaml").exists())
        assertEquals("new bundled table", File(target, "cayboard_corpus.dict.yaml").readText())

        val editedTarget = folder.newFolder("edited-fixed-dictionary")
        BundledInputSync.install(editedTarget, backups, previous) { "old bundled table".byteInputStream() }
        File(editedTarget, "fixed_cy.dict.yaml").writeText("my additional words")
        BundledInputSync.install(editedTarget, backups, manifest("new bundled table", "cayboard_corpus.dict.yaml")) {
            "new bundled table".byteInputStream()
        }
        assertEquals("my additional words", File(editedTarget, "fixed_cy.dict.yaml").readText())
    }

    @Test fun renamedFixedCyDictionaryKeepsItsPreviousManifestForBackup() {
        val target = folder.newFolder("renamed-fixed-dictionary")
        val backups = folder.newFolder("renamed-fixed-dictionary-backup")
        val rows = "词\tci\t1\n".repeat(150_000)
        val oldText = "---\nname: fixed_cy\nversion: '1'\n...\n$rows"
        BundledInputSync.install(target, backups, manifest(oldText, "fixed_cy.dict.yaml")) {
            oldText.byteInputStream()
        }

        val migratedText = oldText.replace("fixed_cy", "cayboard_corpus") + "自造词\tzi zao ci\t5\n"
        File(target, "fixed_cy.dict.yaml").delete()
        File(target, "cayboard_corpus.dict.yaml").writeText(migratedText)
        val bundledText = oldText.replace("fixed_cy", "cayboard_corpus")
        BundledInputSync.install(target, backups, manifest(bundledText, "cayboard_corpus.dict.yaml")) {
            bundledText.byteInputStream()
        }

        assertEquals(bundledText, File(target, "cayboard_corpus.dict.yaml").readText())
        assertEquals(migratedText, backups.walkTopDown().single { it.isFile }.readText())
    }

    @Test fun legacyManifestProtectsRenamedLargeDictionaryAndIsRetiredAfterSync() {
        val filesDir = folder.newFolder("legacy-manifest")
        val legacy = File(filesDir, "rime").apply { mkdirs() }
        val target = File(filesDir, LegacyNames.DATA_DIRECTORY)
        val backups = folder.newFolder("legacy-manifest-backup")
        val oldText = "---\nname: fixed_cy\nversion: '1'\n...\n" + "词\tci\t1\n".repeat(70_000)
        val old = manifest(oldText, "fixed_cy.dict.yaml")
        File(legacy, ".cyime-bundled-manifest").writeText(old)
        File(legacy, "fixed_cy.dict.yaml").writeText(oldText + "自造词\tzi zao ci\t5\n")
        File(legacy, "fixed_cy.userdb").mkdirs()
        File(legacy, ".cyime-bundled-revision").writeText("old")
        assertTrue(LegacyNames.migrateFiles(filesDir))

        val bundled = oldText.replace("fixed_cy", "cayboard_corpus")
        BundledInputSync.install(target, backups, manifest(bundled, "cayboard_corpus.dict.yaml")) {
            bundled.byteInputStream()
        }

        assertEquals(bundled, File(target, "cayboard_corpus.dict.yaml").readText())
        assertFalse(File(target, "fixed_cy.userdb").exists())
        assertTrue(File(target, "cayboard_corpus.userdb").isDirectory)
        assertTrue(backups.walkTopDown().any { it.isFile && it.readText().endsWith("自造词\tzi zao ci\t5\n") })
        assertFalse(File(target, ".cyime-bundled-manifest").exists())
        assertFalse(File(target, ".cyime-bundled-revision").exists())
        assertTrue(File(target, ".cayboard-bundled-manifest").isFile)
    }

    @Test fun unknownLargeDictionaryIsBackedUpButUnknownLargeModelIsNot() {
        val target = folder.newFolder("unknown-large"); val backups = folder.newFolder("unknown-large-backup")
        val customDictionary = "用户词\tci\t1\n".repeat(200_000)
        File(target, "cayboard_corpus.dict.yaml").writeText(customDictionary)
        File(target, "model.onnx").writeBytes(ByteArray(2 * 1024 * 1024))
        val replacement = "---\nname: cayboard_corpus\n...\n"

        BundledInputSync.install(target, backups, manifest(replacement, "cayboard_corpus.dict.yaml")) {
            replacement.byteInputStream()
        }

        assertTrue(backups.walkTopDown().any { it.isFile && it.length() == customDictionary.toByteArray().size.toLong() })
        assertTrue(backups.walkTopDown().none { it.isFile && it.name == "model.onnx" })
    }

    @Test fun replacesSameSizeOldDictionaryBacksUpAndKeepsCompiledCacheForIncrementalRebuild() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        File(target, "core.dict.yaml").writeText("old")
        File(target, "build").mkdir(); File(target, "build/core.table.bin").writeText("cache")
        listOf("personal.dict.yaml", "core.custom.yaml", "core.userdb").forEach { File(target, it).writeText("personal") }
        assertTrue(BundledInputSync.install(target, backups, manifest("new")) { "new".byteInputStream() })
        assertEquals("new", File(target, "core.dict.yaml").readText())
        assertEquals("old", backups.walkTopDown().single { it.isFile }.readText())
        // librime rebuilds only the tables whose source checksum changed; the cache is not wiped on upgrade.
        assertEquals("cache", File(target, "build/core.table.bin").readText())
        listOf("personal.dict.yaml", "core.custom.yaml", "core.userdb").forEach { assertEquals("personal", File(target, it).readText()) }
        assertFalse(BundledInputSync.install(target, backups, manifest("new")) { error("Must not reopen large assets") })
    }
    @Test fun interruptedUpgradeRetriesWithoutOverwritingOriginalBackup() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        File(target, "a.schema.yaml").writeText("old")
        val list = manifest("one", "a.schema.yaml") + manifest("two", "b.dict.yaml")
        try { BundledInputSync.install(target, backups, list) { if (it.endsWith("b.dict.yaml")) error("interruption") else "one".byteInputStream() }; fail() } catch (_: IllegalStateException) { }
        assertFalse(File(target, ".cayboard-bundled-revision").exists())
        assertTrue(BundledInputSync.install(target, backups, list) { if (it.endsWith("b.dict.yaml")) "two".byteInputStream() else error("already updated") })
        assertEquals("old", backups.walkTopDown().single { it.isFile }.readText())
    }
    @Test fun corruptAssetDoesNotReplaceExistingDictionaryOrCache() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        File(target, "core.dict.yaml").writeText("old"); File(target, "build").mkdir()
        try { BundledInputSync.install(target, backups, manifest("new")) { "bad".byteInputStream() }; fail() } catch (_: IllegalStateException) { }
        assertEquals("old", File(target, "core.dict.yaml").readText()); assertTrue(File(target, "build").exists())
    }
    @Test fun missingFileRepairedEvenWithCurrentRevision() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        BundledInputSync.install(target, backups, manifest("new")) { "new".byteInputStream() }
        File(target, "core.dict.yaml").delete()
        assertTrue(BundledInputSync.install(target, backups, manifest("new")) { "new".byteInputStream() })
    }

    @Test fun lightweightEditionKeepsPreviouslyInstalledEnhancementModels() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        val grammar = "wanxiang-lts-zh-hans.gram"
        val scorer = "t9_sentence.onnx"
        val previous = manifest("schema") + manifest("grammar", grammar) + manifest("scorer", scorer)
        BundledInputSync.install(target, backups, previous) {
            when (it.substringAfterLast('/')) {
                grammar -> "grammar"
                scorer -> "scorer"
                else -> "schema"
            }.byteInputStream()
        }
        assertTrue(BundledInputSync.install(target, backups, manifest("updated schema")) {
            "updated schema".byteInputStream()
        })
        assertEquals("grammar", File(target, grammar).readText())
        assertEquals("scorer", File(target, scorer).readText())
        assertEquals("updated schema", File(target, "core.dict.yaml").readText())
    }

    @Test fun cylmFileDroppedByNewPackageIsRemovedButOtherDroppedFilesStay() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        val previous = manifest("schema") + manifest("big", "cylm_model2.lm") + manifest("grammar", "a.gram")
        BundledInputSync.install(target, backups, previous) {
            when (it.substringAfterLast('/')) { "cylm_model2.lm" -> "big"; "a.gram" -> "grammar"; else -> "schema" }.byteInputStream()
        }
        assertTrue(BundledInputSync.install(target, backups, manifest("new schema")) { "new schema".byteInputStream() })
        assertFalse(File(target, "cylm_model2.lm").exists())
        assertEquals("grammar", File(target, "a.gram").readText())
    }

    @Test fun retiredJaroomajiDictionariesAreRemovedUnlessEdited() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        val names = listOf("jaroomaji.schema.yaml", "jaroomaji.mozc.dict.yaml", "jaroomaji.dict.yaml",
            "jaroomaji.user.dict.yaml", "cayboard_japanese_particles.dict.yaml", "lua/cayboard_japanese_completion.lua", "other.dict.yaml")
        BundledInputSync.install(target, backups, names.joinToString("") { manifest(it, it) }) { it.substringAfter('/').byteInputStream() }
        File(target, "jaroomaji.user.dict.yaml").writeText("my words")
        BundledInputSync.install(target, backups, manifest("azooKey schema", "jaroomaji.schema.yaml")) { "azooKey schema".byteInputStream() }
        assertEquals("azooKey schema", File(target, "jaroomaji.schema.yaml").readText())
        listOf("jaroomaji.mozc.dict.yaml", "jaroomaji.dict.yaml", "cayboard_japanese_particles.dict.yaml",
            "lua/cayboard_japanese_completion.lua").forEach { assertFalse(it, File(target, it).exists()) }
        assertEquals("my words", File(target, "jaroomaji.user.dict.yaml").readText())
        assertEquals("other.dict.yaml", File(target, "other.dict.yaml").readText())
    }

    @Test fun userReplacedCylmFileIsKept() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        BundledInputSync.install(target, backups, manifest("schema") + manifest("big", "cylm_model2.lm")) {
            (if (it.endsWith("cylm_model2.lm")) "big" else "schema").byteInputStream()
        }
        File(target, "cylm_model2.lm").writeText("mine")
        BundledInputSync.install(target, backups, manifest("new schema")) { "new schema".byteInputStream() }
        assertEquals("mine", File(target, "cylm_model2.lm").readText())
    }

    @Test fun unmodifiedPreviousBundleIsReplacedWithoutBackup() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        BundledInputSync.install(target, backups, manifest("one")) { "one".byteInputStream() }
        assertTrue(BundledInputSync.install(target, backups, manifest("two")) { "two".byteInputStream() })
        assertEquals("two", File(target, "core.dict.yaml").readText())
        assertTrue(backups.walkTopDown().none { it.isFile })
    }

    @Test fun userEditedFileIsStillBackedUp() {
        val target = folder.newFolder("cayboard_input"); val backups = folder.newFolder("backup")
        BundledInputSync.install(target, backups, manifest("one")) { "one".byteInputStream() }
        File(target, "core.dict.yaml").writeText("mine")
        assertTrue(BundledInputSync.install(target, backups, manifest("two")) { "two".byteInputStream() })
        assertEquals("mine", backups.walkTopDown().single { it.isFile }.readText())
    }

    @Test fun onlyNewestUpgradeBackupIsKept() {
        val backups = folder.newFolder("backup")
        val older = File(backups, "a".repeat(64)).apply { mkdirs(); File(this, "x.yaml").writeText("old"); setLastModified(1_000_000L) }
        val newer = File(backups, "b".repeat(64)).apply { mkdirs(); File(this, "x.yaml").writeText("new"); setLastModified(2_000_000L) }
        val other = File(backups, "english-policy").apply { mkdirs(); File(this, "p.yaml").writeText("keep") }
        BundledInputSync.pruneBackups(backups)
        assertFalse(older.exists())
        assertTrue(newer.exists())
        assertEquals("keep", File(other, "p.yaml").readText())
    }
}
