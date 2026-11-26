package com.cayboard.settings

import org.junit.Assert.*
import org.junit.Test

class ChineseAssetMigrationPolicyTest {
    @Test fun dictionariesNeverEnterTheStartupComparisonPathEvenWhenSmall() {
        listOf("cayboard_pinyin.dict.yaml", "cn_dicts/tencent.dict.yaml", "en_dicts/en.dict.yaml").forEach {
            assertFalse(ChineseSchemas.isSmallMigrationCandidate(it, 100))
            assertFalse(ChineseSchemas.isSmallMigrationCandidate(it, 20_000_000))
        }
    }
    @Test fun hugeConfigurationOrLuaFilesCannotTriggerLargeStartupAllocations() {
        listOf("t9_pinyin.schema.yaml", "pinyin_14jian.schema.yaml", "lua/uuid.lua").forEach {
            assertTrue(ChineseSchemas.isSmallMigrationCandidate(it, 2048))
            assertFalse(ChineseSchemas.isSmallMigrationCandidate(it, 262145))
        }
    }
    @Test fun userPatchesAndResourcesAreNeverMigrationCandidates() {
        listOf("cayboard_pinyin.custom.yaml", "opencc/emoji.txt", "README.md", "LICENSE").forEach {
            assertFalse(ChineseSchemas.isSmallMigrationCandidate(it, 2048))
        }
    }
}
