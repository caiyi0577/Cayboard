package com.cayboard.settings

import android.content.Context
import kotlinx.coroutines.launch

object LanguagePreferences {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    const val KEY = "enabled_input_languages"
    /** 开关语言后的后台准备做完时改一次：键盘据此重读方案列表，语言键马上带上刚编译好的语言。 */
    const val SCHEMAS_READY_KEY = "language_schemas_ready_at"
    /** Chinese and English are always available; legacy disabled-language preferences no longer apply. */
    fun enabled(context: Context): Set<InputLanguage> = InputLanguage.supported.toSet() + InputLanguage.UNSPECIFIED
    fun initialize(context: Context) {
        val prefs = SettingsPreferences.getPrefsPublic(context)
        val languages = enabled(context)
        val ids = languages.map { it.id }.toSet()
        if (prefs.getStringSet(KEY, null) != ids) {
            prefs.edit().putStringSet(KEY, ids).commit()
        }
        val current = SettingsPreferences.getCurrentSchema(context)
        if (InputLanguage.forSchema(current) !in languages) {
            SettingsPreferences.setCurrentSchema(context, nativeSchema(current, SchemaManager.getEnabledSchemas(context), languages))
        }
    }
    /**
     * Turning a schema on in 方案管理 (or installing it from the market) also turns its language on: otherwise a
     * freshly deployed Japanese schema can never be reached, because the language key only cycles enabled
     * languages and switching to a schema of a disabled language is ignored (2026-10-19, tablet after 清除数据).
     */
    internal fun withSchemaLanguage(enabled: Set<InputLanguage>, schemaId: String): Set<InputLanguage> {
        val language = InputLanguage.forSchema(schemaId)
        return if (language in InputLanguage.supported && language !in enabled) enabled + language else enabled
    }

    /** Enable the schema's language without touching the schema list (the caller already changed it). */
    fun enableForSchema(context: Context, schemaId: String) {
        val current = enabled(context)
        val next = withSchemaLanguage(current, schemaId)
        if (next == current) return
        (next - current).forEach { InputModes.moveLanguageLast(context, it) }
        SettingsPreferences.getPrefsPublic(context).edit().putStringSet(KEY, next.map { it.id }.toSet()).commit()
    }

    /** The engine needs a real schema even when English is the only visible language. */
    internal fun nativeSchema(saved: String, available: List<String>, languages: Set<InputLanguage>): String {
        val native = available.filter { it.isNotBlank() && it != InputModes.ENGLISH && InputResourcePolicy.supportsSchema(it) }
        return saved.takeIf { it in native && InputLanguage.forSchema(it) in languages }
            ?: native.firstOrNull { InputLanguage.forSchema(it) in languages }
            ?: native.firstOrNull()
            ?: "cayboard_pinyin"
    }

    /** English always remains available; language switches never delete dictionaries. */
    fun save(context: Context, language: InputLanguage, enabled: Boolean) {
        require(language in InputLanguage.supported && language != InputLanguage.ENGLISH)
        val before = this.enabled(context)
        val next = before.toMutableSet().apply { if (enabled) add(language) else remove(language) }
        if (enabled && language !in before) InputModes.moveLanguageLast(context, language)
        val schemasBefore = SchemaManager.getEnabledSchemas(context)
        // Bundled languages bring their own schemas; others only show what is already installed.
        val bundled = when (language) {
            InputLanguage.JAPANESE -> JapaneseSchemas.ids
            else -> LetterLanguage.of(language)?.let { listOf(it.schemaId) }
        }
        if (bundled != null) {
            val schemas = SchemaManager.getEnabledSchemas(context)
            SchemaManager.setEnabledSchemas(context, if (enabled) (schemas + bundled).distinct()
                else schemas.filterNot { InputLanguage.forSchema(it) == language })
        }
        // 补全词表是商店里的下载包：开启时就在后台下载，打字前多半已经下好。
        if (enabled) LetterLanguage.of(language)?.let { com.cayboard.association.WordCompletion.download(context, it) }
        // The English entry is virtual. Keep a real decoder schema as the engine fallback.
        val current = SettingsPreferences.getCurrentSchema(context)
        if (current == InputModes.ENGLISH || InputLanguage.forSchema(current) !in next) {
            val fallback = nativeSchema(current, SchemaManager.getEnabledSchemas(context), next)
            SettingsPreferences.setCurrentSchema(context, fallback)
        }
        // 只有启用的方案列表真的变了（开关日语、德语等自带方案的语言）才需要重新部署。
        // 开关中文、或方案本来就在列表里时，只改语言键轮换的语言，不让键盘重新「准备」一遍。
        val schemasChanged = SchemaManager.getEnabledSchemas(context) != schemasBefore
        if (schemasChanged) SettingsPreferences.setDeploymentDone(context, false)
        SettingsPreferences.getPrefsPublic(context).edit().putStringSet(KEY, next.map { it.id }.toSet()).commit()
        if (schemasChanged) scope.launch {
            com.cayboard.engine.EngineConfig.prepareAutomatically(context.applicationContext)
            // 键盘自己的那次刷新可能早于编译完成，读到的方案列表里还没有新语言：开了意大利语、准备完了，
            // 语言键还是只有中英，要关掉再开才出现（11-09）。准备完再通知键盘重读一次。
            SettingsPreferences.getPrefsPublic(context).edit().putLong(SCHEMAS_READY_KEY, System.currentTimeMillis()).apply()
        }
    }
}
