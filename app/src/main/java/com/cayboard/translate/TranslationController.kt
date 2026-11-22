package com.cayboard.translate

import android.content.Context
import com.cayboard.ui.common.UiNode
import com.cayboard.ui.common.UiNodeType

data class TranslationResult(val id: String, val text: String)
data class TranslationPanelState(
    val inputText: String = "",
    val items: List<TranslationResult> = emptyList(),
    val loading: Boolean = false,
    val ui: List<UiNode> = emptyList(),
)

interface OfflineTranslateListener {
    fun onDone(text: String)
    fun onError(message: String)
}

/** The keyboard owns this controller; no script, installation or network host is involved. */
class TranslationController(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("translation", Context.MODE_PRIVATE)
    private var input = ""
    private var request = -1
    private var generation = 0L
    private var loading = false
    private var result = ""
    @Volatile var error: String? = null
        private set

    @Synchronized fun getPanelState(inputText: String = ""): TranslationPanelState {
        val source = prefs.getString("sourceLang", "自动检测").orEmpty()
        val target = prefs.getString("targetLang", "英文").orEmpty()
        val names = TranslationLanguages.all.map { it.name }
        return TranslationPanelState(input, if (result.isEmpty()) emptyList() else listOf(TranslationResult("translation", result)), loading,
            listOf(UiNode(UiNodeType.SELECT, key = "sourceLang", label = "原文语言", value = source, options = listOf("自动检测") + names),
                UiNode(UiNodeType.BUTTON, key = "swap", label = "交换语言", style = "swap"),
                UiNode(UiNodeType.SELECT, key = "targetLang", label = "翻译成", value = target, options = names, style = "quick"),
                UiNode(UiNodeType.TEXT, value = "输入要翻译的文字")))
    }

    @Synchronized fun onPanelInput(text: String) { input = text }

    @Synchronized fun onPanelAction(action: String) {
        when {
            action.startsWith("select:") -> {
                val fields = action.split(':', limit = 3)
                if (fields.size != 3 || fields[1] !in setOf("sourceLang", "targetLang")) return
                val selected = fields[2]
                if (TranslationLanguages.byName(selected) == null && !(fields[1] == "sourceLang" && selected == "自动检测")) return
                val other = if (fields[1] == "sourceLang") "targetLang" else "sourceLang"
                val editor = prefs.edit().putString(fields[1], selected)
                if (selected == prefs.getString(other, null)) editor.putString(other, if (selected == "英文") "简体中文" else "英文")
                editor.apply()
            }
            action == "swap" -> {
                val source = prefs.getString("sourceLang", "自动检测").orEmpty()
                val target = prefs.getString("targetLang", "英文").orEmpty()
                prefs.edit().putString("sourceLang", target)
                    .putString("targetLang", if (source == "自动检测") { if (target == "简体中文") "英文" else "简体中文" } else source).apply()
            }
            action == "generate" -> generate()
        }
    }

    @Synchronized private fun generate() {
        cancel()
        val token = generation
        if (input.isBlank()) return
        val from = TranslationLanguages.byName(prefs.getString("sourceLang", null))?.code ?: "auto"
        val to = TranslationLanguages.byName(prefs.getString("targetLang", null))?.code ?: "en"
        error = null; result = ""; loading = true
        request = OfflineTranslation.translate(app, input, from, to, object : OfflineTranslateListener {
            override fun onDone(text: String) = synchronized(this@TranslationController) {
                if (generation == token) { result = text; loading = false }
            }
            override fun onError(message: String) = synchronized(this@TranslationController) {
                if (generation == token) { error = message; loading = false }
            }
        })
    }

    @Synchronized fun cancel() {
        generation++
        OfflineTranslation.cancel(request)
        request = -1; loading = false
    }
}
