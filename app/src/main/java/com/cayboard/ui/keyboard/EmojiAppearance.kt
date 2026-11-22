package com.cayboard.ui.keyboard

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import com.cayboard.data.EmojiData
import com.cayboard.settings.SettingsPreferences

/** App-local emoji fonts. Text committed to the editor is always the original Unicode sequence. */
object EmojiAppearance {
    enum class Style { Android, iOS }
    const val KEY = "emoji_display_style"
    private const val CANDIDATES_MIGRATED = "emoji_candidates_default_v1"
    private var context: Context? = null
    var style by mutableStateOf(Style.iOS)
        private set
    private data class Face(val family: FontFamily, val paint: Paint)
    private val faces = mutableMapOf<Style, Face>()
    private val supported = object : LinkedHashMap<String, Boolean>(256, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?) = size > 512
    }
    fun initialize(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        val prefs = SettingsPreferences.getPrefsPublic(context)
        style = Style.entries.firstOrNull { it.name == prefs.getString(KEY, null) } ?: Style.iOS
    }
    fun setStyle(context: Context, value: Style) {
        initialize(context)
        SettingsPreferences.getPrefsPublic(context).edit().putString(KEY, value.name).apply()
        style = value
    }
    /** User requested emoji candidates enabled; migrate existing defaults once, retain later choices. */
    fun migrateCandidates(context: Context) {
        val prefs = SettingsPreferences.getPrefsPublic(context)
        if (!prefs.getBoolean(CANDIDATES_MIGRATED, false)) {
            prefs.edit().putBoolean(KeyboardInputPreferences.CANDIDATE_EMOJI_KEY, true)
                .putBoolean(CANDIDATES_MIGRATED, true).apply()
        }
    }
    private fun face(style: Style): Face = faces.getOrPut(style) {
        val assets = checkNotNull(context) { "EmojiAppearance.initialize must run before rendering" }.assets
        val typeface = Typeface.createFromAsset(assets, "emoji/${style.name}.ttf")
        Face(FontFamily(typeface), Paint().apply { this.typeface = typeface; textSize = 48f })
    }
    fun family(emoji: String): FontFamily {
        val selected = style
        if (selected == Style.iOS) {
            // Whole-sequence fallback: never split an unsupported ZWJ emoji into separate people/objects.
            val hasGlyph = supported.getOrPut(emoji) { face(selected).paint.hasGlyph(emoji) }
            if (hasGlyph) return face(selected).family
        }
        return face(Style.Android).family
    }
    private class Node {
        val next = HashMap<Char, Node>()
        var emoji: String? = null
    }
    private val trie by lazy {
        Node().apply {
            for (emoji in EmojiData.all) {
                var node = this
                for (char in emoji) node = node.next.getOrPut(char) { Node() }
                node.emoji = emoji
                // Also recognize text-default legacy candidates without VS16.
                if ('\uFE0F' in emoji) {
                    node = this
                    for (char in emoji.replace("\uFE0F", "")) node = node.next.getOrPut(char) { Node() }
                    node.emoji = emoji
                }
            }
        }
    }
    /** Same annotated text is used for candidate measurement and drawing, including number prefixes. */
    fun annotate(text: String): AnnotatedString {
        if (text.none { it.isSurrogate() || it in '\u2000'..'\u2BFF' || it == '\uFE0F' }) return AnnotatedString(text)
        val builder = AnnotatedString.Builder(text)
        var start = 0
        while (start < text.length) {
            var node = trie
            var end = start
            var found: String? = null
            var foundEnd = start
            while (end < text.length) {
                node = node.next[text[end]] ?: break
                end++
                node.emoji?.let { found = it; foundEnd = end }
            }
            if (found != null) {
                builder.addStyle(SpanStyle(fontFamily = family(found!!)), start, foundEnd)
                start = foundEnd
            } else start++
        }
        return builder.toAnnotatedString()
    }
}
