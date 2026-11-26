package com.cayboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 简/繁开关要真的能转：每个方案引用的 OpenCC 配置和它用到的 .ocd2 表都要随包，
 * 否则原生引擎加载失败、静默不转（1.15.6 之前 cayboard_pinyin 的「繁」就是这样）。
 * 引擎实测见 tools/ime_lab/host_replay/TraditionalOutputProbe.java。
 */
class OpenCcAssetsTest {
    private val opencc = File("src/main/assets/cayboard_chinese/opencc")
    // 雾凇自带的 opencc（表情 emoji.json + emoji.txt/others.txt）由 tasks-chinese.gradle.kts 生成到这里，
    // 和 cayboard_chinese/opencc 一起由 BundledInputSync 装进同一个 opencc/ 目录，九键、14 键方案也能引用。
    private val iceOpencc = File("build/generated/chinese-assets/cayboard_pinyin/opencc")
    private val configPattern = Regex("""^\s*opencc_config:\s*(\S+\.json)""", RegexOption.MULTILINE)
    private val tablePattern = Regex(""""file":\s*"([^"]+)"""")

    private fun configsOf(text: String) = configPattern.findAll(text).map { it.groupValues[1] }.toList()

    @Test fun everyTraditionalizeConfigShipsWithItsTables() {
        val schemas = File("src/main/assets/cayboard_chinese").listFiles { f -> f.name.endsWith(".schema.yaml") }!!.toList()
        val referenced = schemas.flatMap { configsOf(it.readText()) }.toSet()
        assertTrue("九键、14 键应引用台湾正体配置", "cayboard_s2tw.json" in referenced)
        // 26 键和双拼由 tasks-chinese.gradle.kts 从 rime-ice 生成，同样改成这张表。
        assertTrue(File("build-logic/tasks-chinese.gradle.kts").readText().contains("opencc_config: cayboard_s2tw.json"))
        for (config in referenced + setOf("s2t.json", "s2tw.json", "s2hk.json", "cayboard_s2tw.json")) {
            val dir = listOf(opencc, iceOpencc).firstOrNull { File(it, config).isFile }
            assertTrue("缺少 $config", dir != null)
            val file = File(dir!!, config)
            val tables = tablePattern.findAll(file.readText()).map { it.groupValues[1] }.toList()
            assertTrue("$config 没有引用任何表", tables.isNotEmpty())
            tables.forEach { table ->
                assertTrue("$config 用到的 $table 没有随包",
                    File(opencc, table).isFile || File(iceOpencc, table).isFile)
            }
        }
    }

    @Test fun taiwanFormsComeLastInTheChain() {
        val chain = File(opencc, "cayboard_s2tw.json").readText()
        assertTrue(chain.indexOf("CayboardTWPhrases.ocd2") > chain.indexOf("TWVariants.ocd2"))
        val forms = File("../tools/opencc/cayboard_tw_phrases.txt").readLines().associate { it.substringBefore('\t') to it.substringAfter('\t') }
        assertEquals("台", forms["臺"])
        assertEquals("計畫", forms["計劃"])
        assertEquals("了解", forms["瞭解"])
    }

    // 繁体输出时用户写进「我的短语」的词（user_table）不经通用表，改走 cayboard_s2tw_user.json：
    // 余宛如这类姓「余」的名字照原样出；其余候选仍走 traditionalize，「其余」「十余年」照旧转「餘」。
    @Test fun userPhrasesKeepSurnameForms() {
        val user = File(opencc, "cayboard_s2tw_user.json").readText()
        assertTrue(user.indexOf("CayboardUserKeep.txt") in 0 until user.indexOf("STCharacters.ocd2"))
        assertEquals("余\t余", File(opencc, "CayboardUserKeep.txt").readLines().first())
        assertTrue(File(opencc, "cayboard_s2tw.json").readText().contains("CayboardSurnames.txt"))
        val schemas = listOf("t9_pinyin", "pinyin_14jian", "double_pinyin_flypy_14jian")
        for (name in schemas) {
            val text = File("src/main/assets/cayboard_chinese/$name.schema.yaml").readText()
            assertTrue("$name 缺 traditionalize_user", text.contains("- simplifier@traditionalize_user"))
            assertTrue("$name 应引用 cayboard_s2tw_user.json", "cayboard_s2tw_user.json" in configsOf(text))
            assertTrue("$name 的 traditionalize 应排除 user_table", text.contains("excluded_types: [user_table]"))
        }
        assertTrue(File("build-logic/tasks-chinese.gradle.kts").readText().contains("cayboard_s2tw_user.json"))
    }
}
