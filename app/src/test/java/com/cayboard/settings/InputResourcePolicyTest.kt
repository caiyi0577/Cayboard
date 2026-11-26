package com.cayboard.settings

import com.cayboard.model.BuiltinModelCatalog
import org.junit.Assert.*
import org.junit.Test

class InputResourcePolicyTest {
    @Test fun chineseAndEnglishSchemesRemainAvailable() {
        val schemas = listOf("cayboard_pinyin", "t9_pinyin", "pinyin_14jian", "double_pinyin_flypy",
            "double_pinyin_mspy", "wubi86", "wubi98", "stroke", "cayboard_cangjie", "cayboard_quick",
            "cayboard_zhuyin", "cayboard_english", "cayboard_t9_english", "melt_eng")
        schemas.forEach { assertTrue(it, InputResourcePolicy.supportsSchema(it)) }
    }

    @Test fun multilingualResourcesCannotBeReenabledOrDownloaded() {
        listOf("japanese", "japanese_kana", "jaroomaji", "spanish", "german", "french", "italian", "russian", "korean")
            .forEach { assertFalse(it, InputResourcePolicy.supportsSchema(it)) }
        assertTrue(BuiltinModelCatalog.models.none { it.id in InputResourcePolicy.retiredModels })
        assertTrue(BuiltinModelCatalog.models.any { it.id == "ochwpro" })
    }

    @Test fun retirementIsLimitedToKnownBundledFiles() {
        assertTrue(InputResourcePolicy.isRetiredAsset("japanese.schema.yaml"))
        assertTrue(InputResourcePolicy.isRetiredAsset("lua/cayboard_japanese_romaji.lua"))
        assertTrue(InputResourcePolicy.isRetiredAsset("fixed_cy.dict.yaml"))
        listOf("cayboard_pinyin.schema.yaml", "japanese.custom.yaml", "japanese.userdb", "personal.dict.yaml",
            "cayboard_corpus.dict.yaml")
            .forEach { assertFalse(it, InputResourcePolicy.isRetiredAsset(it)) }
    }
}
