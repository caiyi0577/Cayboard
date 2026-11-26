package com.cayboard.settings

import org.junit.Assert.*
import org.junit.Test

class CayboardInputDefaultsTest {
    @Test fun modesHaveOneEntryPerLayoutWithOneEnglish() {
        val ids = listOf("pinyin_simp", "cayboard_pinyin", "t9", "t9_pinyin", "melt_eng", "jaroomaji", "japanese", "japanese_kana", "pinyin_14jian")
        assertEquals(listOf("cayboard_pinyin", "t9_pinyin", "japanese", "japanese_kana", "pinyin_14jian"), CayboardInputDefaults.canonicalIds(ids, ids.toSet()))
        val modes = InputModes.available(ids.map { SchemaInfo(it, it, "", "", "") })
        assertEquals(1, modes.count { it.schemaId == InputModes.ENGLISH })
        assertFalse(modes.any { it.schemaId in setOf("melt_eng", "pinyin_simp", "jaroomaji", "t9") })
    }
    @Test fun marketOnlyAndUnknownSchemasKeepTheirIdsAndOrder() {
        val ids = listOf("my_personal", "pinyin_simp", "t9", "jaroomaji", "wanxiang")
        assertEquals(ids, CayboardInputDefaults.canonicalIds(ids, ids.toSet()))
    }
    @Test fun defaultChineseModesShareOneOfflineDictionaryFamily() {
        assertEquals(listOf("t9_pinyin", "cayboard_pinyin"), CayboardInputDefaults.recommended)
        assertFalse(CayboardInputDefaults.recommended.any { it in CayboardInputDefaults.dependencies })
    }
    @Test fun freshInstallAlsoShowsFourteenKeysButUpgradesKeepTheirList() {
        assertEquals(listOf("t9_pinyin", "cayboard_pinyin", "double_pinyin_flypy", "pinyin_14jian"),
            CayboardInputDefaults.firstDefaults(freshInstall = true))
        assertEquals(CayboardInputDefaults.recommended, CayboardInputDefaults.firstDefaults(freshInstall = false))
        // 关着的 14 键在「已安装的方案」里直接列出，不藏进高级。
        assertTrue("pinyin_14jian" in CayboardInputDefaults.listedWhenOff)
        assertTrue(CayboardInputDefaults.listedWhenOff.containsAll(CayboardInputDefaults.doublePinyin))
        // 形码、笔画、注音关着也直接列出，但不进默认启用。
        assertTrue(CayboardInputDefaults.listedWhenOff.containsAll(listOf("wubi86", "cayboard_cangjie", "cayboard_quick", "stroke", "cayboard_zhuyin")))
        assertTrue(CayboardInputDefaults.firstDefaults(freshInstall = true).none { it in CayboardInputDefaults.shapeAndStroke })
        // 新装：14 键和双拼都开着时，各双拼的 14 键版本跟着出现（组合配置），不单独占一项。
        val available = setOf("cayboard_pinyin", "t9_pinyin", "pinyin_14jian", "double_pinyin_flypy", "double_pinyin_flypy_14jian")
        assertEquals(listOf("cayboard_pinyin", "double_pinyin_flypy", "t9_pinyin", "pinyin_14jian", "double_pinyin_flypy_14jian"),
            CayboardInputDefaults.withCompanions(listOf("cayboard_pinyin", "double_pinyin_flypy", "t9_pinyin", "pinyin_14jian"), available))
    }
    @Test fun builtinDoublePinyinGoesRightAfterFullPinyinWithoutMovingOtherChoices() {
        val dp = CayboardInputDefaults.doublePinyin
        assertEquals(listOf("double_pinyin_flypy", "double_pinyin_shoudao", "double_pinyin_ziranma",
            "double_pinyin_znabc", "double_pinyin_jiajia", "double_pinyin_microsoft"), dp)
        val available = setOf("cayboard_pinyin", "t9_pinyin", "japanese") + dp
        assertEquals(listOf("cayboard_pinyin") + dp + listOf("t9_pinyin", "japanese"),
            CayboardInputDefaults.withDoublePinyin(listOf("cayboard_pinyin", "t9_pinyin", "japanese"), available))
        // 已经开着小鹤的不重复、不挪位置；没有中文26键时排在最后。
        assertEquals(listOf("t9_pinyin", "double_pinyin_flypy") + dp.drop(1),
            CayboardInputDefaults.withDoublePinyin(listOf("t9_pinyin", "double_pinyin_flypy"), available))
        assertEquals(listOf("cayboard_pinyin") + dp + "t9_pinyin",
            CayboardInputDefaults.withDoublePinyin(listOf("cayboard_pinyin", "double_pinyin_flypy", "t9_pinyin"), available))
    }
    @Test fun freshInstallKeepsDoublePinyinOptionalWithoutChangingUpgradeDefaults() {
        assertFalse(CayboardInputDefaults.addsDoublePinyinToBar(freshInstall = true, elderEdition = false))
        assertTrue(CayboardInputDefaults.addsDoublePinyinToBar(freshInstall = false, elderEdition = false))
        assertTrue(CayboardInputDefaults.addsDoublePinyinToBar(freshInstall = true, elderEdition = true))
        // 关着的内置双拼仍能在「已安装的方案」里手动开启。
        assertTrue(CayboardInputDefaults.listedWhenOff.containsAll(CayboardInputDefaults.doublePinyin))
    }
    @Test fun doublePinyinWaitsUntilItsSchemasAreGeneratedAndBringsFourteenKeyCompanions() {
        val dp = CayboardInputDefaults.doublePinyin
        assertNull(CayboardInputDefaults.withDoublePinyin(listOf("cayboard_pinyin"), setOf("cayboard_pinyin", "double_pinyin_flypy")))
        // 微软双拼没有 14 键版本（没有分号键），文件不存在就不补。
        val k14 = dp.filterNot { it == DoublePinyinScheme.MICROSOFT_ID }.map { DoublePinyinScheme.k14Id(it) }
        val available = setOf("cayboard_pinyin", "pinyin_14jian") + dp + k14
        assertEquals(listOf("cayboard_pinyin") + dp + listOf("pinyin_14jian") + k14,
            CayboardInputDefaults.withDoublePinyin(listOf("cayboard_pinyin", "pinyin_14jian"), available))
    }
}
