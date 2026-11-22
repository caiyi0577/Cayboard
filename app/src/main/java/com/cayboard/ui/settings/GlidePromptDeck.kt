package com.cayboard.ui.settings

internal object GlidePromptDeck {
    const val POOL_ASSET = "handwriting/contribute_pool.tsv"

    /** 贡献编号（16 位十六进制）当种子；不是十六进制时用字符串哈希。[salt] 让同一个人不同页面的顺序也不同。 */
    fun seedOf(writer: String, salt: String = ""): Long =
        (writer.toULongOrNull(16)?.toLong() ?: writer.hashCode().toLong()) xor salt.hashCode().toLong()

    fun <T> shuffled(items: List<T>, seed: Long): List<T> = items.shuffled(java.util.Random(seed))

    private fun rows(pool: Sequence<String>): Sequence<List<String>> =
        pool.filter { it.isNotBlank() && !it.startsWith("#") }.map { it.split('\t') }
            .filter { it.size >= 2 && it[1].isNotBlank() }


    const val MAX_OWN_CHARS = 8
    private val OWN_SEPARATORS = Regex("[\\s,，、;；。.!！?？/|]+")

    data class Own<T>(val prompts: List<T>, val tooLong: Int = 0, val unknown: Int = 0)

    private fun ownTokens(text: String): List<String> =
        text.split(OWN_SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /**
     * 滑行自己输入的题。[english] 时只收 2～16 个字母的英文词（转小写）；中文、九键时字母直接当拼音，
     * 汉字词用 [pinyinOf] 查拼音（查不到计入 unknown，请用户直接输拼音）。
     */
    fun ownGlide(text: String, english: Boolean, pinyinOf: (String) -> String?): Own<GlidePrompt> {
        var tooLong = 0
        var unknown = 0
        val prompts = ownTokens(text).mapNotNull { raw ->
            val token = raw.lowercase()
            when {
                LETTERS.matches(token) -> GlidePrompt(token)
                token.all { it in 'a'..'z' } -> { if (token.length > 16) tooLong++ else unknown++; null }
                english -> { unknown++; null }
                else -> pinyinOf(raw)?.takeIf { LETTERS.matches(it) }?.let { GlidePrompt(it, raw) }
                    ?: run { if (raw.length > MAX_OWN_CHARS) tooLong++ else unknown++; null }
            }
        }.distinctBy { it.letters }
        return Own(prompts, tooLong, unknown)
    }

    private val LETTERS = Regex("[a-z]{2,16}")

    /**
     * 汉字词查拼音：先查 [POOL_ASSET] 的两字词，查不到就逐字拼 hanzi_pinyin.tsv 的第一个读音（去掉声调，ü 写成 v）。
     * 多音字可能拼错，题目上会显示出来，用户可以改输拼音。
     */
    fun pinyinLookup(pool: Sequence<String>, hanziPinyin: Sequence<String>): (String) -> String? {
        val words = HashMap<String, String>()
        rows(pool).filter { it[0] == "word" }.forEach { cols ->
            cols.getOrNull(2)?.replace(" ", "")?.takeIf { it.isNotEmpty() }?.let { words.putIfAbsent(cols[1].trim(), it) }
        }
        val chars = HashMap<String, String>()
        hanziPinyin.forEach { line ->
            val cols = line.split('\t')
            if (cols.size >= 2 && cols[1].isNotBlank()) chars.putIfAbsent(cols[0], toneless(cols[1].substringBefore('/')))
        }
        return { word -> words[word] ?: spell(word, chars) }
    }

    private fun spell(word: String, chars: Map<String, String>): String? {
        val out = StringBuilder()
        var i = 0
        while (i < word.length) {
            val cp = word.codePointAt(i)
            out.append(chars[String(Character.toChars(cp))] ?: return null)
            i += Character.charCount(cp)
        }
        return out.toString().takeIf { it.isNotEmpty() }
    }

    internal fun toneless(reading: String): String =
        java.text.Normalizer.normalize(reading.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace("u\u0308", "v").filter { it in 'a'..'z' }

    /** 滑行贡献的一道题：[letters] 是要滑的字母，[label] 是显示用的词（中文题），英文题为空。 */
    data class GlidePrompt(val letters: String, val label: String = "")

    /** english.txt 是网页词频，前几千里有这些不适合当题的词。 */
    private val ENGLISH_BLOCK = setOf("sex", "sexy", "porn", "xxx", "teen", "teens", "gay", "lesbian", "naked", "nude",
        "fetish", "cialis", "viagra", "casino", "poker", "gambling", "escort", "fuck", "shit", "damn", "ass", "bitch")
    private val ENGLISH_WORD = Regex("[a-z]{3,10}")
    private val HAS_VOWEL = Regex("[aeiouy]")

    /** 英文题：english.txt 前 [limit] 个常用词里 3～10 个字母的（没有元音的缩写如 www、html 不要），按 [seed] 打乱。 */
    fun englishGlide(lines: Sequence<String>, seed: Long, limit: Int = 5000): List<GlidePrompt> =
        shuffled(lines.take(limit).map { it.trim() }
            .filter { ENGLISH_WORD.matches(it) && HAS_VOWEL.containsMatchIn(it) && it !in ENGLISH_BLOCK }
            .distinct().map { GlidePrompt(it) }.toList(), seed)

    private val PINYIN_LETTERS = Regex("[a-z]{2,16}")

    /** 中文、九键题：[POOL_ASSET] 里的两字词，滑全拼（拼音去掉空格），按 [seed] 打乱。 */
    fun pinyinGlide(pool: Sequence<String>, seed: Long): List<GlidePrompt> =
        shuffled(rows(pool).filter { it[0] == "word" }.mapNotNull { cols ->
            val letters = cols.getOrNull(2)?.replace(" ", "")?.takeIf { PINYIN_LETTERS.matches(it) }
            letters?.let { GlidePrompt(it, cols[1].trim()) }
        }.distinctBy { it.letters }.toList(), seed)
}
