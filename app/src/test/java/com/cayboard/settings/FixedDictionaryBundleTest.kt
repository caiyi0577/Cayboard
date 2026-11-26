package com.cayboard.settings

import org.junit.Assert.*
import org.junit.Test

class FixedDictionaryBundleTest {
    @Test fun bundledTablesUseTheCayboardOwnedDictionaryId() {
        assertTrue("cayboard_corpus" in FixedDictionaryBundle.tables)
        assertFalse("fixed_cy" in FixedDictionaryBundle.tables)
    }

    @Test fun migrationRetainsLocalListsAndNeverTreatsUnknownUserIdsAsBundled() {
        val local = setOf("local_aaaaaaaaaaaaaaaaaaaaaaaa", "my_own_words")
        val official = CellDictionaryCatalog.builtins.map { it.id } + CellDictionaryCatalog.offers.map { it.id }
        assertEquals(local, FixedDictionaryBundle.retainLocal(local + official))
        assertEquals(emptySet<String>(), FixedDictionaryBundle.retainLocal(official.toSet()))
    }
}
