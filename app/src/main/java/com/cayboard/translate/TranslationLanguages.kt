package com.cayboard.translate

/** Languages from Tencent's pinned Hy-MT2 model card, including its language variants. */
object TranslationLanguages {
    data class Language(val code: String, val name: String, val promptName: String)
    val all = listOf(
        Language("zh", "简体中文", "Chinese"),
        Language("en", "英文", "English"),
        Language("ja", "日语", "Japanese"),
        Language("ko", "韩语", "Korean"),
        Language("fr", "法语", "French"),
        Language("de", "德语", "German"),
        Language("es", "西班牙语", "Spanish"),
        Language("ru", "俄语", "Russian"),
        Language("pt", "葡萄牙语", "Portuguese"),
        Language("tr", "土耳其语", "Turkish"),
        Language("ar", "阿拉伯语", "Arabic"),
        Language("th", "泰语", "Thai"),
        Language("it", "意大利语", "Italian"),
        Language("vi", "越南语", "Vietnamese"),
        Language("ms", "马来语", "Malay"),
        Language("id", "印尼语", "Indonesian"),
        Language("tl", "菲律宾语", "Filipino"),
        Language("hi", "印地语", "Hindi"),
        Language("zh-Hant", "繁体中文", "Traditional Chinese"),
        Language("pl", "波兰语", "Polish"),
        Language("cs", "捷克语", "Czech"),
        Language("nl", "荷兰语", "Dutch"),
        Language("km", "高棉语", "Khmer"),
        Language("my", "缅甸语", "Burmese"),
        Language("fa", "波斯语", "Persian"),
        Language("gu", "古吉拉特语", "Gujarati"),
        Language("ur", "乌尔都语", "Urdu"),
        Language("te", "泰卢固语", "Telugu"),
        Language("mr", "马拉地语", "Marathi"),
        Language("he", "希伯来语", "Hebrew"),
        Language("bn", "孟加拉语", "Bengali"),
        Language("ta", "泰米尔语", "Tamil"),
        Language("uk", "乌克兰语", "Ukrainian"),
        Language("bo", "藏语", "Tibetan"),
        Language("kk", "哈萨克语", "Kazakh"),
        Language("mn", "蒙古语", "Mongolian"),
        Language("ug", "维吾尔语", "Uyghur"),
        Language("yue", "粤语", "Cantonese")
    )
    fun byCode(code: String) = all.firstOrNull { it.code.equals(code, ignoreCase = true) }
    fun byName(name: String?) = all.firstOrNull { it.name == name }
    fun supports(from: String, to: String) = (from == "auto" || byCode(from) != null) && byCode(to) != null
}
