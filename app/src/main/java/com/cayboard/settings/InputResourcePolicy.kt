package com.cayboard.settings

import com.cayboard.engine.compat.LegacyNames
/** Resource boundaries for the Chinese/English edition, including upgrades from multilingual builds. */
object InputResourcePolicy {
    val retiredSchemas = setOf("japanese", "japanese_kana", "jaroomaji", "spanish", "german", "french", "italian", "russian", "korean")
    val retiredModels = setOf("japanese-dictionary", "japanese-engine", "words-es", "words-de", "words-fr", "words-it", "words-ru")

    fun supportsSchema(id: String): Boolean = id !in retiredSchemas &&
        InputLanguage.forSchema(id).let { it in InputLanguage.supported || it == InputLanguage.UNSPECIFIED }

    internal fun isRetiredAsset(relative: String): Boolean =
        relative.removeSuffix(".schema.yaml").let { relative.endsWith(".schema.yaml") && it in retiredSchemas } ||
            relative in setOf("lua/cayboard_japanese_romaji.lua", "lua/cayboard_japanese_completion.lua",
                LegacyNames.OLD_BUNDLED_CORPUS)
}
