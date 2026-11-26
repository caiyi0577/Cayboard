package com.cayboard.audit

import android.content.Context
import android.content.res.AssetManager
import com.cayboard.association.AssociationService
import com.cayboard.service.PredictionManager
import com.cayboard.settings.CellDictionaryManager
import com.cayboard.settings.CellDictionaryEngine
import com.cayboard.settings.SchemaMeta
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.*
import java.io.File

class ImportedEnglishAuditTest {
    @get:Rule val temp = TemporaryFolder()
    @After fun release() = AssociationService.release()

    @Test fun activeImportedWordReachesIndependentEnglishCompletion() = runBlocking {
        AssociationService.release()
        val app = mock<Context>()
        val assets = mock<AssetManager>()
        whenever(app.filesDir).thenReturn(temp.newFolder())
        whenever(app.assets).thenReturn(assets)
        val trie = File("src/main/assets/english_trie.bin").readBytes()
        whenever(assets.list("")).thenReturn(arrayOf("english_trie.bin"))
        whenever(assets.open("english_trie.bin")).thenAnswer { trie.inputStream() }
        val prediction = PredictionManager(app, this, {})
        val word = "cayboardquokka"
        suspend fun hasWord() = prediction.getEnglishAssociations("cayboardquok").any { it.equals(word, true) }
        assertTrue("Bundled completion must work", "hello" in prediction.getEnglishAssociations("hel"))
        assertFalse("Unique word must initially be absent", hasWord())
        val store = CellDictionaryManager.store(app)
        val entry = store.install("你好 ni hao\n$word\n".toByteArray())
        assertEquals(listOf(word), store.englishWords(entry.id))
        val rime = temp.newFolder()
        val schemas = listOf(SchemaMeta("cayboard_pinyin", "拼音"))
        File(rime, "cayboard_pinyin.schema.yaml").writeText("translator:\n  dictionary: cayboard_pinyin\n")
        fun apply(enabled: Boolean) {
            val ids = if (enabled) setOf(entry.id) else emptySet()
            store.select(entry.id, enabled)
            store.selectEnglish(enabled)
            store.activate(ids, enabled) { active, english, _ ->
                CellDictionaryEngine.prepare(rime, store, schemas, active, includeEnglish = english)
            }
        }
        apply(true)
        assertTrue(store.read().appliedEnglish)
        assertTrue(File(rime, "user_cayboard_cells_en26.dict.yaml").readText().contains(word))
        val withDictionary = hasWord()
        apply(false)
        assertFalse("Disabling the import should remove the word", hasWord())
        assertTrue("Imported and active in Rime, absent from independent English completions", withDictionary)
    }
}
