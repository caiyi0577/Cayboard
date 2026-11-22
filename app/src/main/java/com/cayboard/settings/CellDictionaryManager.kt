package com.cayboard.settings

import android.content.Context
import java.io.File

object CellDictionaryManager {
    fun store(context: Context) = CellDictionaryStore(File(context.filesDir, "cell-dictionaries"))

    private fun bundledUpdates(context: Context): CellWordUpdates = context.assets.open(CellWordUpdates.ASSET).use {
        CellWordUpdates.parse(ScelDictionary.readBounded(it, CellWordUpdates.MAX_BYTES))
    }

    fun prepare(context: Context, ids: Set<String> = store(context).read().applied,
        english: Boolean = store(context).read().appliedEnglish, wordRevision: Int? = null): List<CellDictionaryRime.Binding> {
        val store = store(context)
        val bundled = runCatching { bundledUpdates(context) }.getOrDefault(CellWordUpdates.empty)
        // Main dictionary words apply with or without cell dictionaries (CellWordUpdates.main).
        val updates = if (wordRevision != null) store.wordUpdates(wordRevision, bundled)
        else runCatching { store.wordUpdates(store.read().appliedWordRevision, bundled) }.getOrDefault(bundled)
        return CellDictionaryRime.prepare(SchemaManager.getRimeDir(context), store,
            SchemaManager.discoverSchemas(context), ids, english, updates)
    }

}
