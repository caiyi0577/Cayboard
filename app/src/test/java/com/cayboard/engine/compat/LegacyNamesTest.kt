package com.cayboard.engine.compat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LegacyNamesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun migratesKnownIdentifiersWithoutRewritingOrdinaryText() {
        val yaml = """
            schema: rime_ice
            schema_id: cyime_t9_pinyin
            import_tables:
              - fixed_cy
            description: "The words Rime and CyIME are part of this user description."
        """.trimIndent()

        val migrated = LegacyNames.canonical(yaml)

        assertTrue(migrated.contains("schema: cayboard_pinyin"))
        assertTrue(migrated.contains("schema_id: cayboard_t9_pinyin"))
        assertTrue(migrated.contains("  - cayboard_corpus"))
        assertTrue(migrated.contains("The words Rime and CyIME are part of this user description."))
    }

    @Test fun rewritesLegacyPreferenceNamesAndSchemaValuesButKeepsUserText() {
        assertEquals("__cayboard_english", LegacyNames.canonical("__xime_english"))
        assertEquals("cayboard_deployment_revision", LegacyNames.canonical("rime_deployment_revision"))
        assertEquals("CyIME phrase", LegacyNames.canonical("CyIME phrase"))
        assertEquals("cayboard_t9_pinyin",
            LegacyNames.preferenceValue("selected_input_profile_zh", "cyime_t9_pinyin"))
        assertEquals("CyIME phrase",
            LegacyNames.preferenceValue("user_custom_phrase", "CyIME phrase"))
    }

    @Test fun canonicalPreferenceWinsWhenMergingOldSettings() {
        val merged = LegacyNames.mergeLegacyPreferences(
            mapOf("last_input_mode_zh" to "cayboard_t9_pinyin"),
            mapOf("last_input_mode_zh" to "cyime_t9_pinyin", "old_key" to "kept"),
        )
        assertEquals("cayboard_t9_pinyin", merged["last_input_mode_zh"])
        assertEquals("kept", merged["old_key"])
    }

    @Test fun movesLegacyDataDirectoryAndPreservesUserDatabaseBytes() {
        val filesDir = temporary.newFolder("files")
        val old = File(filesDir, "rime").apply { mkdirs() }
        val schema = File(old, "cyime_t9_pinyin.schema.yaml")
        schema.writeText("schema_id: cyime_t9_pinyin\nname: \"CyIME user phrase\"\n")
        File(old, "fixed_cy.dict.yaml").writeText("---\nname: fixed_cy\n...\ncyime_custom\tci\t1\n")
        val database = File(old, "cyime_t9_pinyin.userdb/000001.ldb").apply { parentFile.mkdirs() }
        val databaseBytes = byteArrayOf(0, 1, 2, 0x7f, -1)
        database.writeBytes(databaseBytes)
        val legacyDictionaryData = File(old, "fixed_cy.userdb/000001.ldb").apply { parentFile.mkdirs() }
        legacyDictionaryData.writeBytes(databaseBytes)

        assertTrue(LegacyNames.migrateFiles(filesDir))

        val root = File(filesDir, "cayboard_input")
        assertFalse(old.exists())
        assertEquals("cayboard_t9_pinyin.schema.yaml", File(root, "cayboard_t9_pinyin.schema.yaml").name)
        assertEquals("schema_id: cayboard_t9_pinyin\nname: \"CyIME user phrase\"\n", File(root, "cayboard_t9_pinyin.schema.yaml").readText())
        val migratedDictionary = File(root, "cayboard_corpus.dict.yaml")
        assertTrue(migratedDictionary.exists())
        assertEquals("---\nname: cayboard_corpus\n...\ncyime_custom\tci\t1\n", migratedDictionary.readText())
        assertTrue(File(root, "cayboard_corpus.userdb/000001.ldb").readBytes().contentEquals(databaseBytes))
        assertTrue(File(root, "cayboard_t9_pinyin.userdb/000001.ldb").readBytes().contentEquals(databaseBytes))
    }

    @Test fun preservesBothUserDatabasesWhenNamesCollide() {
        val filesDir = temporary.newFolder("conflicting-databases")
        val legacy = File(filesDir, "rime/cyime_t9_pinyin.userdb").apply { mkdirs() }
        val current = File(filesDir, "cayboard_input/cayboard_t9_pinyin.userdb").apply { mkdirs() }
        File(legacy, "MANIFEST").writeText("legacy")
        File(legacy, "000002.ldb").writeText("legacy learned words")
        File(current, "MANIFEST").writeText("current")

        assertTrue(LegacyNames.migrateFiles(filesDir))

        assertEquals(listOf("MANIFEST"), current.list()?.toList())
        assertEquals("current", File(current, "MANIFEST").readText())
        val saved = File(filesDir, "cayboard-name-conflicts/cyime_t9_pinyin.userdb")
        assertEquals("legacy", File(saved, "MANIFEST").readText())
        assertEquals("legacy learned words", File(saved, "000002.ldb").readText())
    }
}
