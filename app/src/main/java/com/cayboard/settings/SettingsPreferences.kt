package com.cayboard.settings

import android.content.Context
import android.content.SharedPreferences

object SettingsPreferences {
    fun isSmsCodeEnabled(context: Context): Boolean = getPrefs(context).getBoolean("sms_code_enabled", false)
    fun setSmsCodeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("sms_code_enabled", enabled).apply()
    }

    private const val PREFS_NAME = "kime_settings"
    const val KEY_ROUNDED_KEYBOARD_BOTTOM = "rounded_keyboard_bottom"
    fun roundedKeyboardBottom(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ROUNDED_KEYBOARD_BOTTOM, false)
    fun setRoundedKeyboardBottom(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ROUNDED_KEYBOARD_BOTTOM, enabled).apply()
    }
    /** 固定键盘顶部两角是否圆角；默认开（原样）。悬浮键盘是卡片，四角始终圆。 */
    const val KEY_ROUNDED_KEYBOARD_TOP = "rounded_keyboard_top"
    fun roundedKeyboardTop(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ROUNDED_KEYBOARD_TOP, true)
    fun setRoundedKeyboardTop(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ROUNDED_KEYBOARD_TOP, enabled).apply()
    }
    const val KEY_PUNCTUATION_FULL_WIDTH = "punctuation_full_width"
    fun punctuationFullWidth(context: Context, default: Boolean): Boolean =
        getPrefs(context).getBoolean(KEY_PUNCTUATION_FULL_WIDTH, default)
    fun setPunctuationFullWidth(context: Context, full: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PUNCTUATION_FULL_WIDTH, full).apply()
    }

    /** 输入成对符号：点开头的引号、括号、书名号时连结尾一起上屏，光标停在中间；默认开。 */
    const val KEY_PAIRED_SYMBOLS = "paired_symbols"
    fun isPairedSymbolsEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_PAIRED_SYMBOLS, true)
    fun setPairedSymbolsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PAIRED_SYMBOLS, enabled).apply()
    }

    /**
     * 回车发送：应用自己把回车设成发送时（微信「回车键发送消息」等），普通回车直接发送、键面写发送；
     * 关闭时回车换行、Ctrl+回车发送。见 EnterKeyAction。
     */
    const val KEY_ENTER_SENDS = "enter_sends"
    fun isEnterSendsEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_ENTER_SENDS, true)
    fun setEnterSendsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENTER_SENDS, enabled).apply()
    }

    /**
     * 标点上屏首选：中文打字中点逗号、句号这类断句标点时，先上屏首选候选再接标点（nihao 点逗号得到「你好，」）。
     * 默认开启。关着时照旧先上屏打的字母。
     */
    const val KEY_PUNCTUATION_COMMITS_CANDIDATE = "punctuation_commits_candidate"
    fun isPunctuationCommitsCandidateEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_PUNCTUATION_COMMITS_CANDIDATE, true)
    fun setPunctuationCommitsCandidateEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PUNCTUATION_COMMITS_CANDIDATE, enabled).apply()
    }

    /**
     * 用户是否显式选择过标点宽度。
     *
     * 未选择时界面回退到方案自身的 ascii_punct，显示与引擎同源，无需回写引擎；
     * 只有用户显式选择过，全角/半角才是唯一权威状态，切换方案/中英时必须回写。
     */
    fun hasPunctuationFullWidth(context: Context): Boolean =
        getPrefs(context).contains(KEY_PUNCTUATION_FULL_WIDTH)

    private const val KEY_CURRENT_SCHEMA = "current_schema"
    /** 双写标记：仅新版本双写后置 true，本地值才可信（旧版本只写 rime，本地是过时迁移值） */
    private const val KEY_CURRENT_SCHEMA_DUAL = "current_schema_dual"
    private const val KEY_DEPLOYMENT_DONE = "deployment_done"
    private const val KEY_BUILTIN_SCHEMAS_MERGED = "builtin_schemas_merged"
    private const val KEY_DEPLOYMENT_HASH = "deployment_hash"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_VERBOSE_LOGGING = "verbose_logging"
    
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_SOUND_VOLUME = "sound_volume"
    private const val KEY_KEYBOARD_THEME = "keyboard_theme"
    
    const val KEY_SMART_PREDICTION_ENABLED = "smart_prediction_enabled"
    const val KEY_DISABLE_HARDWARE_ASSOCIATION = "disable_hardware_association"
    private const val KEY_SPACE_COMMIT_ASSOCIATION = "space_commit_association"
    internal const val KEY_ASSOCIATION_SINGLE_MODE = "association_single_mode"
    /** 已删的「滑动清空时跳过联想」，只留着给 isDeleteSkipsAssociation 迁移旧值。 */
    private const val KEY_CLEAR_SWIPE_SKIPS_ASSOCIATION = "clear_swipe_skips_association"
    private const val KEY_DELETE_SKIPS_ASSOCIATION = "delete_skips_association"
    private const val KEY_DELETE_RECOVERY = "delete_recovery_candidates"
    
    const val KEY_STT_ENABLED = "stt_enabled"
    const val KEY_STT_KEEP_ENGINE_ALIVE = "stt_keep_engine_alive"
    const val KEY_STT_ENGINE_IDLE_MINUTES = "stt_engine_idle_minutes"
    const val KEY_STT_WHOLE_SENTENCE = "stt_whole_sentence"
    const val KEY_STT_AUTO_PUNCTUATION = "stt_auto_punctuation"
    const val KEY_STT_PUNCTUATION_COMMA = "stt_punctuation_comma"
    const val KEY_STT_PUNCTUATION_PERIOD = "stt_punctuation_period"
    const val KEY_STT_PUNCTUATION_QUESTION = "stt_punctuation_question"
    const val KEY_STT_PUNCTUATION_TRAILING = "stt_punctuation_trailing"
    const val KEY_STT_ARABIC_NUMBERS = "stt_arabic_numbers"
    const val KEY_STT_PILL_RESUME = "stt_pill_resume"
    const val KEY_STT_OTHER_AUDIO = "stt_other_audio"
    private const val KEY_STT_MUSIC_MUTED_BY_VOICE = "stt_music_muted_by_voice"
    private const val KEY_STT_MUSIC_VOLUME_BEFORE_VOICE = "stt_music_volume_before_voice"
    
    /** 默认主题 ID，可从 xime.yaml 的 style.color_scheme 初始化。 */
    @JvmStatic
    var defaultKeyboardTheme: String = com.cayboard.ui.theme.FixedKeyboardStyle.ID

    /** 默认显示模式，可从 xime.yaml 的 style.dark_mode 初始化。 */
    @JvmStatic
    var defaultDarkMode: Int = 1
    
    const val KEY_SWIPE_UP_HINTS_ENABLED = "swipe_up_hints_enabled"
    const val KEY_SWIPE_DOWN_HINTS_ENABLED = "swipe_down_hints_enabled"

    private const val KEY_AUTO_NUMBER_KEYBOARD = "auto_number_keyboard_for_number_fields"

    /** 进入纯数字输入框（号码/验证码等）时自动弹出数字键盘，默认开启。 */
    fun isAutoNumberKeyboardEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_NUMBER_KEYBOARD, true)
    }

    fun setAutoNumberKeyboardEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_NUMBER_KEYBOARD, enabled).apply()
    }
    
    const val KEY_SPLIT_KEYBOARD = "split_keyboard_enabled"

    /** 完整键盘为默认；仅用户在调节界面选择后启用26键分体。 */
    fun isSplitKeyboardEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SPLIT_KEYBOARD, false)

    fun setSplitKeyboardEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SPLIT_KEYBOARD, enabled).apply()
    }

    /** 这块屏幕上分体键盘是否真的分开：「分体键盘」开着，且屏幕够宽；屏幕太窄时用完整键盘，存着的开关不改。 */
    fun isSplitKeyboardShown(context: Context, screenWidthDp: Int = context.resources.configuration.screenWidthDp): Boolean =
        isSplitKeyboardEnabled(context) && com.cayboard.ui.keyboard.SplitKeyboardGeometry.fitsScreen(screenWidthDp)

    const val KEY_SPLIT_RIGHT_G = "split_keyboard_right_g"

    /** 分体 26 键右半中排开头再放一个 G（别人提的需求，默认关）；只影响分体键盘。 */
    fun isSplitRightGEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SPLIT_RIGHT_G, false)

    fun setSplitRightGEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SPLIT_RIGHT_G, enabled).apply()
    }

    const val KEY_FILL_KEYBOARD_BLANK = "fill_keyboard_blank"

    /**
     * 分体中缝、固定键盘两侧的留空默认镂空，透出后面的应用（用户的设计）；
     * 打开后用键盘底色填满，遮住后面的内容，也不再把点按让给后面的应用。
     */
    fun isKeyboardBlankFilled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_FILL_KEYBOARD_BLANK, false)

    fun setKeyboardBlankFilled(context: Context, filled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FILL_KEYBOARD_BLANK, filled).apply()
    }

    const val KEY_WIDE_KEYS = "wide_keys"

    /**
     * 「加宽按键」默认关：打开后键盘两侧留白从 8dp 收到 3dp、横向键缝减三分之一，
     * 手机上每个键更宽，触摸键距也更大（10-31 用户反馈手机上按键窄、容易误触）。
     */
    fun isWideKeysEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_WIDE_KEYS, false)

    fun setWideKeysEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WIDE_KEYS, enabled).apply()
    }

    const val KEY_EVEN_MERGED_KEYS = "even_merged_keys"

    /**
     * 「合并键均匀排布」默认关：打开后 14 键和自建的合并键布局（17、18、20 键等）里合并键只比单字母键宽三分之一，
     * 单字母键相应变宽，每行排得更匀，小屏上好按（10-31 别人的需求）。关闭时严格对齐 26 键键缝。
     */
    fun isEvenMergedKeysEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_EVEN_MERGED_KEYS, false)

    fun setEvenMergedKeysEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EVEN_MERGED_KEYS, enabled).apply()
    }

    const val KEY_NUMBER_ROW = "number_row"

    /**
     * 「数字行」默认开：打开后 26 键、14 键（含自建字母布局）字母上方加一排数字，点按直接上屏，
     * 键和顺序在「按键符号 → 数字行」里改（10-31 用户反馈：平板想要一排数字、数字键位置能自己排）。
     * 键盘总高度不变，五排分高度；注音（大千）首排本身就是数字位，不加。
     */
    fun isNumberRowEnabled(context: Context): Boolean = getPrefs(context).getBoolean(KEY_NUMBER_ROW, true)

    fun setNumberRowEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NUMBER_ROW, enabled).apply()
    }

    const val KEY_SPLIT_GAP_PORTRAIT = "split_gap_fraction_portrait"
    const val KEY_SPLIT_GAP_LANDSCAPE = "split_gap_fraction_landscape"

    /** 分体中缝占整张键盘宽度的比例；未调整过返回 null，由屏宽和键距自动给出。 */
    fun getSplitGapFraction(context: Context, landscape: Boolean): Float? =
        getPrefs(context).getFloat(if (landscape) KEY_SPLIT_GAP_LANDSCAPE else KEY_SPLIT_GAP_PORTRAIT, -1f)
            .takeIf { it.isFinite() && it > 0f }

    fun setSplitGapFraction(context: Context, landscape: Boolean, fraction: Float?) {
        val key = if (landscape) KEY_SPLIT_GAP_LANDSCAPE else KEY_SPLIT_GAP_PORTRAIT
        getPrefs(context).edit().apply {
            if (fraction == null || !fraction.isFinite() || fraction <= 0f) remove(key) else putFloat(key, fraction)
        }.apply()
    }

    
    private const val KEY_KEYBOARD_HEIGHT_DP = "keyboard_height_dp"
    private const val KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE = "keyboard_height_dp_landscape"
    const val DEFAULT_KEYBOARD_HEIGHT_PERCENT = 32
    /** 横屏默认高度 = 竖屏高度（长边）× 此百分比。约等于旧值（横屏短边 × 49%），但基数稳定、与竖屏语义统一 */
    const val DEFAULT_KEYBOARD_HEIGHT_PERCENT_LANDSCAPE = 25

    private const val KEY_TOOLBAR_BUTTONS = "toolbar_buttons"
    private val DEFAULT_TOOLBAR_BUTTONS = com.cayboard.keyboard.ToolbarButton.DEFAULT_VISIBLE.joinToString(",") { it.id }

    private const val KEY_MENU_FIRST_ROW_UPDATED = "quick_actions_first_row_2_0_3"

    /** Move the four shortcuts into the first menu row once, including saved arrangements. */
    fun migrateQuickActionMenu(context: Context) {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_MENU_FIRST_ROW_UPDATED, false)) return
        val firstRow = com.cayboard.keyboard.ToolbarButton.MENU_FIRST_ROW.map { it.id }
        val editor = prefs.edit().putBoolean(KEY_MENU_FIRST_ROW_UPDATED, true)
            .remove("toolbar_default_2_0_2")
        prefs.getString(KEY_TOOLBAR_BUTTONS, null)?.let { saved ->
            val buttons = saved.split(",")
            if (buttons.any { it in firstRow }) {
                editor.putString(KEY_TOOLBAR_BUTTONS, (buttons - firstRow.toSet()).joinToString(","))
            }
        }
        prefs.getString(KEY_MENU_BUTTONS, null)?.let { saved ->
            val buttons = com.cayboard.keyboard.ToolbarButton.normalizeIds(saved.split(","))
            editor.putString(KEY_MENU_BUTTONS, (firstRow + (buttons - firstRow.toSet())).joinToString(","))
        }
        editor.apply()
    }

    fun getToolbarButtons(context: Context): List<String> {
        val raw = getPrefs(context).getString(KEY_TOOLBAR_BUTTONS, DEFAULT_TOOLBAR_BUTTONS) ?: DEFAULT_TOOLBAR_BUTTONS
        return com.cayboard.keyboard.ToolbarButton.normalizeIds(raw.split(",").filter { it.isNotEmpty() })
    }

    fun setToolbarButtons(context: Context, buttons: List<String>) {
        getPrefs(context).edit().putString(KEY_TOOLBAR_BUTTONS,
            com.cayboard.keyboard.ToolbarButton.normalizeIds(buttons).joinToString(",")).apply()
    }

    private const val KEY_MENU_BUTTONS = "menu_buttons"

    /** 菜单与工具栏共用一组入口；读出时去掉已在工具栏上的项，并保证“设置”可达。 */
    fun getMenuButtons(context: Context): List<String> {
        val toolbar = getToolbarButtons(context)
        restoreFullShapeMenuItemOnce(context, toolbar)
        insertMenuItemOnce(context, toolbar, com.cayboard.keyboard.ToolbarButton.VIBRATION.id,
            "quick_actions_vibration_split_v1", com.cayboard.keyboard.ToolbarButton.SOUND.id)
        // 悬浮、简繁：放在全角/半角后面（不在菜单里就放在“排列”前），与新装默认菜单同一位置。
        var after = com.cayboard.keyboard.ToolbarButton.FULL_SHAPE.id
        com.cayboard.keyboard.QuickActionLayout.MENU_TOGGLES_1031.forEach {
            insertMenuItemOnce(context, toolbar, it.id, "quick_actions_${it.id}_inserted", after)
            after = it.id
        }
        com.cayboard.keyboard.ToolbarButton.LAYOUT_GESTURES.forEach {
            insertMenuItemOnce(context, toolbar, it.id, "quick_actions_${it.id}_inserted")
        }
        val raw = getPrefs(context).getString(KEY_MENU_BUTTONS, null)
        val menu = raw?.split(",")?.filter { it.isNotEmpty() }
            ?: com.cayboard.keyboard.QuickActionLayout.defaultMenu(toolbar)
        return com.cayboard.keyboard.QuickActionLayout.normalize(toolbar, menu).menu
    }

    /**
     * The menu/toolbar merge left 全角／半角 out of the default menu, so saved layouts from that
     * time hide it. Put it back once, before 排列; a later choice to remove it is kept.
     */
    private fun restoreFullShapeMenuItemOnce(context: Context, toolbar: List<String>) {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_FULL_SHAPE_MENU_RESTORED, false)) return
        val id = com.cayboard.keyboard.ToolbarButton.FULL_SHAPE.id
        val raw = prefs.getString(KEY_MENU_BUTTONS, null)
        val editor = prefs.edit().putBoolean(KEY_FULL_SHAPE_MENU_RESTORED, true)
        if (raw != null && id !in toolbar) {
            val menu = raw.split(",").filter { it.isNotEmpty() }
            if (id !in menu) {
                val arrange = com.cayboard.keyboard.ToolbarButton.ARRANGE.id
                val at = menu.indexOf(arrange).takeIf { it >= 0 } ?: menu.size
                editor.putString(KEY_MENU_BUTTONS, (menu.take(at) + id + menu.drop(at)).joinToString(","))
            }
        }
        editor.apply()
    }
    private const val KEY_FULL_SHAPE_MENU_RESTORED = "quick_actions_full_shape_restored"

    /**
     * A new built-in entry joins an already saved menu once, right after [after] when that is in
     * the menu, otherwise before 排列; removing it later is kept.
     */
    private fun insertMenuItemOnce(context: Context, toolbar: List<String>, id: String, flagKey: String, after: String? = null) {
        val prefs = getPrefs(context)
        if (prefs.getBoolean(flagKey, false)) return
        val raw = prefs.getString(KEY_MENU_BUTTONS, null)
        val editor = prefs.edit().putBoolean(flagKey, true)
        if (raw != null && id !in toolbar) {
            val menu = raw.split(",").filter { it.isNotEmpty() }
            if (id !in menu) {
                val arrange = com.cayboard.keyboard.ToolbarButton.ARRANGE.id
                val at = after?.let { menu.indexOf(it) }?.takeIf { it >= 0 }?.plus(1)
                    ?: menu.indexOf(arrange).takeIf { it >= 0 } ?: menu.size
                editor.putString(KEY_MENU_BUTTONS, (menu.take(at) + id + menu.drop(at)).joinToString(","))
            }
        }
        editor.apply()
    }
    fun setQuickActions(context: Context, layout: com.cayboard.keyboard.QuickActionLayout) {
        getPrefs(context).edit()
            .putString(KEY_TOOLBAR_BUTTONS, com.cayboard.keyboard.ToolbarButton.normalizeIds(layout.toolbar).joinToString(","))
            .putString(KEY_MENU_BUTTONS, layout.menu.joinToString(","))
            .apply()
    }

    /**
     * 「记录密码等敏感内容」(#95)，默认关：密码管理器复制的、在密码框里复制的内容记进剪贴板历史，
     * 显示成圆点、1 小时后删除、不同步不导出。
     */
    private const val KEY_CLIPBOARD_SENSITIVE_CAPTURE = "clipboard_sensitive_capture"

    fun isClipboardSensitiveCaptureEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_CLIPBOARD_SENSITIVE_CAPTURE, false)

    fun setClipboardSensitiveCaptureEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CLIPBOARD_SENSITIVE_CAPTURE, enabled).apply()
    }

    /** 剪贴板历史最多保存几条（0 = 不限）；没存过时由剪贴板按已有条数定一次，见 ClipboardRetention.initialCount。 */
    private const val KEY_CLIPBOARD_MAX_COUNT = "clipboard_max_count"
    /** 剪贴板历史保存几天后自动删除（0 = 不过期，默认）。 */
    private const val KEY_CLIPBOARD_KEEP_DAYS = "clipboard_keep_days"

    fun getClipboardMaxCount(context: Context): Int? =
        getPrefs(context).takeIf { it.contains(KEY_CLIPBOARD_MAX_COUNT) }?.getInt(KEY_CLIPBOARD_MAX_COUNT, 0)

    fun setClipboardMaxCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_CLIPBOARD_MAX_COUNT, count).apply()
    }

    fun getClipboardKeepDays(context: Context): Int = getPrefs(context).getInt(KEY_CLIPBOARD_KEEP_DAYS, 0)

    fun setClipboardKeepDays(context: Context, days: Int) {
        getPrefs(context).edit().putInt(KEY_CLIPBOARD_KEEP_DAYS, days).apply()
    }

    private const val KEY_SCHEMA_IMPORT_WARNING_DISMISSED = "schema_import_warning_dismissed"

    private const val KEY_INSTALLED_MARKET_IDS = "installed_market_ids"
    const val KEY_SHOW_CANDIDATE_COMMENTS = "show_candidate_comments"
    const val KEY_CANDIDATE_COMMENT_MODE = "candidate_comment_mode"
    const val KEY_CANDIDATE_COMMENT_CONTENT = "candidate_comment_content"
    const val KEY_SHOW_CORRECTION_MARKS = "show_correction_marks"
    const val KEY_SHOW_CANDIDATE_CANCEL_BUTTON = "show_candidate_cancel_button"
    const val KEY_ASSOCIATION_CANCEL_CROSS = "association_cancel_cross"
    const val KEY_HANDWRITING_WUBI_HINTS = "handwriting_wubi_hints"
    const val KEY_CANDIDATE_PRESS_HIGHLIGHT = "candidate_press_highlight"
    const val KEY_INPUT_TEXT_LOCATION = "input_text_location"
    const val KEY_SHOW_PREEDIT_PREVIEW = "show_preedit_preview"
    const val KEY_INLINE_PREEDIT_PREVIEW = "inline_preedit_preview"
    const val KEY_PREEDIT_BUBBLE_OPACITY = "preedit_bubble_opacity"

    fun showPreeditPreview(context: Context) = getPrefs(context).getBoolean(KEY_SHOW_PREEDIT_PREVIEW, true)
    fun setShowPreeditPreview(context: Context, show: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_SHOW_PREEDIT_PREVIEW, show).apply()
    fun inlinePreeditPreview(context: Context) = getPrefs(context).getBoolean(KEY_INLINE_PREEDIT_PREVIEW, false)
    fun setInlinePreeditPreview(context: Context, inline: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_INLINE_PREEDIT_PREVIEW, inline).apply()
    /** 拼音气泡底色的不透明度：20%~100%，按 5% 一档；默认 100%（实底，看得清），用户喜欢透的可以调低（11-01）。 */
    fun normalizePreeditBubbleOpacity(value: Float): Float =
        if (!value.isFinite()) 1f else kotlin.math.round(value.coerceIn(0.2f, 1f) * 20f) / 20f
    fun preeditBubbleOpacity(context: Context) =
        normalizePreeditBubbleOpacity(getPrefs(context).getFloat(KEY_PREEDIT_BUBBLE_OPACITY, 1f))
    fun setPreeditBubbleOpacity(context: Context, opacity: Float) =
        getPrefs(context).edit().putFloat(KEY_PREEDIT_BUBBLE_OPACITY, normalizePreeditBubbleOpacity(opacity)).apply()
    private const val KEY_PAGE_SIZE = "page_size"
    private const val KEY_CANDIDATE_TEXT_SIZE = "candidate_text_size"
    const val KEY_WIDE_CANDIDATE_SPACING = "wide_candidate_spacing"
    const val INPUT_TEXT_INPUT_BOX = "input_box"
    const val INPUT_TEXT_CANDIDATE_BAR = "candidate_bar"
    const val DEFAULT_PAGE_SIZE = 20 // 手机候选栏每页候选词数；schema 里的 page_size 来自 PC 版（5），太短，默认用 20

    fun showCorrectionMarks(context: Context): Boolean = true

    fun setShowCorrectionMarks(context: Context, show: Boolean) {
        // Fixed default; legacy preference writes are intentionally ignored.
    }

    /** Candidate presentation is fixed in Cayboard; settings remain only for source compatibility. */
    fun candidateCommentMode(context: Context): CandidateCommentMode = CandidateCommentMode.AUTO
    fun setCandidateCommentMode(context: Context, mode: CandidateCommentMode) { }
    fun candidateCommentContent(context: Context): CandidateCommentContent = CandidateCommentContent.PINYIN
    fun setCandidateCommentContent(context: Context, content: CandidateCommentContent) { }
    fun showCandidateComments(context: Context): Boolean = true
    fun setShowCandidateComments(context: Context, show: Boolean) { }

    /** The candidate cancel button is always enabled. */
    fun shouldShowCandidateCancelButton(context: Context): Boolean = true
    fun setShowCandidateCancelButton(context: Context, show: Boolean) { }

    /**
     * 联想关闭键用叉号（用户 11-03，默认关）：默认上屏后的联想和打字时一样，最左边是「<」；
     * 打开后联想时换成 ⊗。只在取消按钮开着时有用，动作不变。
     */
    fun isAssociationCancelCross(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ASSOCIATION_CANCEL_CROSS, false)

    fun setAssociationCancelCross(context: Context, cross: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ASSOCIATION_CANCEL_CROSS, cross).apply()
    }

    /**
     * 候选词间距加宽（B站反馈 10-31，别人的需求，默认关）：候选栏里两个候选之间约空出一个字，
     * 单字候选不挤在一起。关着时间距不变。
     */
    fun isWideCandidateSpacing(context: Context): Boolean =
        false

    fun setWideCandidateSpacing(context: Context, wide: Boolean) {
        // Fixed default; legacy preference writes are intentionally ignored.
    }

    fun getInputTextLocation(context: Context): String {
        return INPUT_TEXT_CANDIDATE_BAR
    }

    fun setInputTextLocation(context: Context, location: String) {
        // Fixed default; legacy preference writes are intentionally ignored.
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getPrefsPublic(context: Context): SharedPreferences {
        return getPrefs(context)
    }
    
    fun getCurrentSchema(context: Context): String {
        val prefs = getPrefs(context)
        // 双写标记已置：本地值是新版本写入的最新方案，进程重建后 rime 未初始化也能恢复
        if (prefs.getBoolean(KEY_CURRENT_SCHEMA_DUAL, false)) {
            val local = prefs.getString(KEY_CURRENT_SCHEMA, null)
            if (!local.isNullOrBlank()) return local
        }
        // 未双写（旧版本升级/首次）：rime user.yaml 为权威，本地仅作迁移兜底
        val fromRime = try {
            if (com.cayboard.rime.RimeEngine.isInitialized()) {
                com.cayboard.rime.RimeEngine.getInstance()
                    .getUserConfigString("var/previously_selected_schema")
            } else null
        } catch (_: Throwable) {
            null
        }
        if (!fromRime.isNullOrBlank()) {
            // 回写 SharedPreferences 并置双写标记，之后本地优先
            prefs.edit()
                .putString(KEY_CURRENT_SCHEMA, fromRime)
                .putBoolean(KEY_CURRENT_SCHEMA_DUAL, true)
                .apply()
            return fromRime
        }
        val legacy = prefs.getString(KEY_CURRENT_SCHEMA, null)
        if (!legacy.isNullOrBlank()) return legacy
        return CayboardInputDefaults.CHINESE_SCHEMA
    }

    fun setCurrentSchema(context: Context, schemaId: String, language: InputLanguage = InputLanguage.forSchema(schemaId)) {
        InputModes.rememberMode(context, schemaId, language)
        // 双写：SharedPreferences 保证进程重建后不依赖 rime 即可恢复，
        // librime user.yaml 保持引擎侧状态一致（switchSchema 后由 librime 持久化）
        getPrefs(context).edit()
            .putString(KEY_CURRENT_SCHEMA, schemaId)
            .putBoolean(KEY_CURRENT_SCHEMA_DUAL, true)
            .apply()
        try {
            if (com.cayboard.rime.RimeEngine.isInitialized()) {
                com.cayboard.rime.RimeEngine.getInstance()
                    .setUserConfigString("var/previously_selected_schema", schemaId)
            }
        } catch (_: Throwable) {
        }
    }
    
    fun isDeploymentDone(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEPLOYMENT_DONE, false)
    }

    fun setDeploymentDone(context: Context, done: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEPLOYMENT_DONE, done).apply()
    }

    /**
     * 内置方案补齐已执行标记：新版本首次运行时把缺失的内置方案（如 t9_pinyin，
     * 老版本升级用户列表里没有）补进 schema_list 一次；之后用户在方案管理里
     * 移除内置方案是有效选择，getEnabledSchemas 不得再强行补回。
     */
    fun isBuiltinSchemasMerged(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BUILTIN_SCHEMAS_MERGED, false)
    }

    fun setBuiltinSchemasMerged(context: Context, merged: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BUILTIN_SCHEMAS_MERGED, merged).apply()
    }

    fun getDeploymentHash(context: Context): String {
        return getPrefs(context).getString(KEY_DEPLOYMENT_HASH, "") ?: ""
    }

    fun setDeploymentHash(context: Context, hash: String) {
        getPrefs(context).edit().putString(KEY_DEPLOYMENT_HASH, hash).apply()
    }

    /** 上次完成 rime assets 同步的 versionCode（0 表示从未同步）。 */

    /** 调试 verbose 日志总开关（仅 Debug 构建生效，Release 恒关闭）。 */
    fun isVerboseLoggingEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VERBOSE_LOGGING, false)
    }

    fun setVerboseLoggingEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_VERBOSE_LOGGING, enabled).apply()
    }

    fun isSetupCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setSetupCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
    }

    fun getDarkMode(context: Context): Int = 1
    
    fun setDarkMode(context: Context, mode: Int) {
        getPrefs(context).edit().putInt(KEY_DARK_MODE, 1).apply()
    }

    fun getEffectiveDarkMode(context: Context): Int = 1
    
    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }
    
    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
    
    fun getSoundVolume(context: Context): Int {
        return getPrefs(context).getInt(KEY_SOUND_VOLUME, 20)
    }
    
    fun setSoundVolume(context: Context, volume: Int) {
        getPrefs(context).edit().putInt(KEY_SOUND_VOLUME, volume).apply()
    }
    
    /** 振动实际由「按键效果」的振动模式决定；关闭即“不振动”。 */
    fun isVibrationEnabled(context: Context): Boolean = getHapticMode(context) != "disabled"

    /** 菜单「按键振动」用：关闭时记住原模式，重新打开时恢复（默认跟随系统）。旧 vibration_enabled 键不再使用。 */
    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        val prefs = getPrefs(context)
        val current = getHapticMode(context)
        if (enabled == (current != "disabled")) return
        if (enabled) {
            prefs.edit().putString(KEY_HAPTIC_MODE, prefs.getString(KEY_HAPTIC_MODE_BEFORE_MUTE, null) ?: "following_system")
                .remove(KEY_HAPTIC_MODE_BEFORE_MUTE).apply()
        } else {
            prefs.edit().putString(KEY_HAPTIC_MODE_BEFORE_MUTE, current).putString(KEY_HAPTIC_MODE, "disabled").apply()
        }
    }
    private const val KEY_HAPTIC_MODE_BEFORE_MUTE = "haptic_mode_before_mute"
    
    

    private const val KEY_HAPTIC_MODE = "haptic_mode"
    private const val KEY_HAPTIC_ON_KEYUP = "haptic_on_keyup"
    private const val KEY_VIBRATION_PRESS_DURATION = "vibration_press_duration"
    private const val KEY_VIBRATION_LONG_PRESS_DURATION = "vibration_long_press_duration"
    private const val KEY_VIBRATION_PRESS_AMPLITUDE = "vibration_press_amplitude"
    private const val KEY_VIBRATION_LONG_PRESS_AMPLITUDE = "vibration_long_press_amplitude"

    fun getHapticMode(context: Context): String {
        return getPrefs(context).getString(KEY_HAPTIC_MODE, "following_system") ?: "following_system"
    }

    fun setHapticMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_HAPTIC_MODE, mode).apply()
    }

    fun isHapticOnKeyUp(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAPTIC_ON_KEYUP, false)
    }

    fun setHapticOnKeyUp(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAPTIC_ON_KEYUP, enabled).apply()
    }

    fun getVibrationPressDuration(context: Context): Int {
        return getPrefs(context).getInt(KEY_VIBRATION_PRESS_DURATION, 0)
    }

    fun setVibrationPressDuration(context: Context, duration: Int) {
        getPrefs(context).edit().putInt(KEY_VIBRATION_PRESS_DURATION, duration).apply()
    }

    fun getVibrationLongPressDuration(context: Context): Int {
        return getPrefs(context).getInt(KEY_VIBRATION_LONG_PRESS_DURATION, 0)
    }

    fun setVibrationLongPressDuration(context: Context, duration: Int) {
        getPrefs(context).edit().putInt(KEY_VIBRATION_LONG_PRESS_DURATION, duration).apply()
    }

    fun getVibrationPressAmplitude(context: Context): Int {
        return getPrefs(context).getInt(KEY_VIBRATION_PRESS_AMPLITUDE, 0)
    }

    fun setVibrationPressAmplitude(context: Context, amplitude: Int) {
        getPrefs(context).edit().putInt(KEY_VIBRATION_PRESS_AMPLITUDE, amplitude).apply()
    }

    fun getVibrationLongPressAmplitude(context: Context): Int {
        return getPrefs(context).getInt(KEY_VIBRATION_LONG_PRESS_AMPLITUDE, 0)
    }

    fun setVibrationLongPressAmplitude(context: Context, amplitude: Int) {
        getPrefs(context).edit().putInt(KEY_VIBRATION_LONG_PRESS_AMPLITUDE, amplitude).apply()
    }

    /** 新安装使用配置中的默认配色；保留已有主题选择及原迁移标记。 */
    fun applyForkThemeDefaults(context: Context) {
        val prefs = getPrefs(context)
        if (prefs.getBoolean("cyime_soft_blue_v1", false)) return
        val theme = prefs.getString(KEY_KEYBOARD_THEME, null)
        prefs.edit().apply {
            if (theme.isNullOrBlank()) putString(KEY_KEYBOARD_THEME, defaultKeyboardTheme)
            putInt(KEY_DARK_MODE, 1)
            putBoolean("cyime_soft_blue_v1", true)
        }.apply()
    }

    fun getKeyboardTheme(context: Context): String = com.cayboard.ui.theme.FixedKeyboardStyle.ID

    const val KEY_VISUAL_STYLE = "cyime_visual_style"

    /** 选过「流光玻璃」「毛玻璃」的按「原有外观」读（[com.cayboard.ui.theme.VisualStyle.offered]）；存的值不改。 */
    fun getVisualStyle(context: Context) = com.cayboard.ui.theme.VisualStyle.ORIGINAL

    fun setVisualStyle(context: Context, style: com.cayboard.ui.theme.VisualStyle) {
        com.cayboard.ui.theme.IconAppearance.setKeyboardStyle(context, style)
    }

    const val KEY_KEY_SHAPE = "cyime_key_shape"

    fun getKeyShape(context: Context) = com.cayboard.ui.theme.KeyShape.SQUARE

    fun setKeyShape(context: Context, shape: com.cayboard.ui.theme.KeyShape) {
        getPrefs(context).edit().putString(KEY_KEY_SHAPE, shape.id).apply()
        com.cayboard.ui.theme.KeyShapes.current = shape
    }

    
    fun setKeyboardTheme(context: Context, themeId: String) {
        getPrefs(context).edit().putString(KEY_KEYBOARD_THEME, themeId).apply()
        // 桌面图标（开启“跟随主题色”时）和字幕快捷方式随主题换色。
        com.cayboard.ui.theme.IconAppearance.onKeyboardThemeChanged(context, themeId)
    }

    
    fun isHardwareAssociationDisabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_DISABLE_HARDWARE_ASSOCIATION, false)

    fun setHardwareAssociationDisabled(context: Context, disabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DISABLE_HARDWARE_ASSOCIATION, disabled).apply()
    }

    fun isSmartPredictionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SMART_PREDICTION_ENABLED, true)
    }
    
    fun setSmartPredictionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SMART_PREDICTION_ENABLED, enabled).apply()
    }
    
    
    
    /** 空格上屏联想候选：有联想候选时按空格键直接上屏第一个联想词（而非空格字符） */
    fun isSpaceCommitAssociationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SPACE_COMMIT_ASSOCIATION, false)
    }

    fun setSpaceCommitAssociationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SPACE_COMMIT_ASSOCIATION, enabled).apply()
    }

    /**
     * 联想模式：false = 连续联想（联想上屏后继续推理，可连续上屏），true = 单次联想（只推理一次）。
     * 没存过值时算单次（老用户照旧）；2026-11-08 起新装由 [com.cayboard.model.DeviceDefaults] 写入连续。
     */
    fun isSingleAssociationMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ASSOCIATION_SINGLE_MODE, true)
    }

    fun setAssociationSingleMode(context: Context, single: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ASSOCIATION_SINGLE_MODE, single).apply()
    }

    /**
     * 退格直接删字（B站反馈 10-31，别人的需求，默认关）：联想词出现时退格直接删掉刚上屏的字、顺手去掉联想词，
     * 滑动清空也直接按已选范围清正文。关着时照旧：第一下只去掉联想词，第二下才删字或清空。
     *
     * 原「滑动清空时跳过联想」（默认关）11-08 被用户删掉，并进这一项：旧值是开的，第一次读时把这一项打开，
     * 并删掉旧键，之后只看这一项。没开过旧项的人不受影响，默认值不变。
     */
    fun isDeleteSkipsAssociation(context: Context): Boolean {
        val prefs = getPrefs(context)
        if (prefs.contains(KEY_CLEAR_SWIPE_SKIPS_ASSOCIATION)) {
            val oldOn = prefs.getBoolean(KEY_CLEAR_SWIPE_SKIPS_ASSOCIATION, false)
            val edit = prefs.edit().remove(KEY_CLEAR_SWIPE_SKIPS_ASSOCIATION)
            if (oldOn) edit.putBoolean(KEY_DELETE_SKIPS_ASSOCIATION, true)
            edit.apply()
            if (oldOn) return true
        }
        return prefs.getBoolean(KEY_DELETE_SKIPS_ASSOCIATION, false)
    }

    fun setDeleteSkipsAssociation(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DELETE_SKIPS_ASSOCIATION, enabled).apply()
    }

    /**
     * 删字后给同音字（用户反馈 10-31，别人的需求，默认关）：退格删掉一个字后，候选栏先放回删掉的字，后面跟同音字，
     * 删错了点一下补回来，打错了同音字直接点对的。不挡接着删。见 DeletionRecovery。
     */
    fun isDeleteRecovery(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_DELETE_RECOVERY, false)

    fun setDeleteRecovery(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DELETE_RECOVERY, enabled).apply()
    }

    fun isSttEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STT_ENABLED, true)
    }
    
    fun setSttEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STT_ENABLED, enabled).apply()
    }

    /** Speech is always offline; retained for generic runtime capability callers. */
    fun isSttUseLocal(context: Context): Boolean = true

    /**
     * 语音结束后是否保持识别引擎常驻（不随会话结束销毁）。
     * 开启后闲置一段时间再使用无需重新加载引擎，"开始聆听"响应快。
     * 仅本地（离线）模式生效：模型本就常驻 :asr 进程，保留 wrapper 代价极小；
     * 在线插件常驻需保持 WebSocket 长连接（耗电、占用服务端资源），不提供常驻。
     */
    fun isSttKeepEngineAlive(context: Context): Boolean {
        return isSttUseLocal(context) && getSttEngineIdleMinutes(context) == 0
    }

    /** 旧开关：开 = 空闲时保留模型；关 = 恢复原来的 1 分钟空闲释放。 */


    /** 空闲这么多分钟后释放全部语音模型（0 = 常驻）。 */
    fun getSttEngineIdleMinutes(context: Context): Int = 1



    /** 是否把本地语音识别收到的录音与识别过程保存到应用自己的 speech_tuning 目录，供电脑回放调优。 */
    /**
     * 整句定稿：在真停顿或结束时校正一整句，结束时整段重识别。默认开：平板实测说完 30 秒的话
     * 约 1.2–1.4 秒出定稿（2026-10-30，SenseVoice / X-ASR，sherpa-onnx 1.13.8 两线程）。
     */
    fun isSttWholeSentence(context: Context): Boolean = true




    /**
     * 语音自动加标点：保留识别结果自带的标点（本地整句定稿的模型标点，以及内置在线接口返回的原文标点）。
     * 默认关，和 1.15.0 以前一样：标点变成空格，只有口述「逗号」「句号」「问号」「感叹号」出标点。
     * 1.15.0–1.15.21 把标点绑在整句定稿上、又默认开，用户没法只去掉标点（2026-10-31 用户：「不要改我的习惯」）。
     */
    fun isSttAutoPunctuation(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STT_AUTO_PUNCTUATION, false)
    }

    fun setSttAutoPunctuation(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STT_AUTO_PUNCTUATION, enabled).apply()
    }

    /** 自动加标点开着时逐项选留哪些模型标点；默认全留（打开自动加标点就和原来一样带全部标点）。 */
    fun isSttPunctuationComma(context: Context) = getPrefs(context).getBoolean(KEY_STT_PUNCTUATION_COMMA, true)
    fun setSttPunctuationComma(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_STT_PUNCTUATION_COMMA, enabled).apply()
    fun isSttPunctuationPeriod(context: Context) = getPrefs(context).getBoolean(KEY_STT_PUNCTUATION_PERIOD, true)
    fun setSttPunctuationPeriod(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_STT_PUNCTUATION_PERIOD, enabled).apply()
    fun isSttPunctuationQuestion(context: Context) = getPrefs(context).getBoolean(KEY_STT_PUNCTUATION_QUESTION, true)
    fun setSttPunctuationQuestion(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_STT_PUNCTUATION_QUESTION, enabled).apply()
    /** 整段话最后的标点；默认留（2026-10-31 用户反馈：想要中间有标点、末尾不带）。 */
    fun isSttPunctuationTrailing(context: Context) = getPrefs(context).getBoolean(KEY_STT_PUNCTUATION_TRAILING, true)
    fun setSttPunctuationTrailing(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_STT_PUNCTUATION_TRAILING, enabled).apply()

    /**
     * 数字转阿拉伯数字：中文语音结果里读出来的数字写成 325、3.5、50%(VoiceNumbers)。
     * 别人提的需求(2026-11-04)，默认关；只是一次文字扫描，没有可测的性能开销，不登记。
     */
    fun isSttArabicNumbers(context: Context) = getPrefs(context).getBoolean(KEY_STT_ARABIC_NUMBERS, false)
    fun setSttArabicNumbers(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean(KEY_STT_ARABIC_NUMBERS, enabled).apply()

    /**
     * 开车连续语音：语音悬浮条听写时发出消息或离开输入框，回到同一应用的输入框自动接着听。
     * 别人提的需求，默认关（用户 10-30 规则）；见 VoicePillResume。
     */
    fun isSttPillResume(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STT_PILL_RESUME, false)
    }

    fun setSttPillResume(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STT_PILL_RESUME, enabled).apply()
    }

    /**
     * 屏蔽外放音（用户 2026-11-04 要的，参照他截图的「压低音量 / 暂停播放 / 不屏蔽」，默认压低音量）：
     * 语音开麦时把正在放的音乐、视频压低或暂停，关麦恢复。开关麦各一次系统调用，没有可测的性能开销，不登记；见 VoiceAudioFocus。
     */
    fun getSttOtherAudio(context: Context): com.cayboard.speech.VoiceOtherAudio =
        com.cayboard.speech.VoiceOtherAudio.fromId(getPrefs(context).getString(KEY_STT_OTHER_AUDIO, null))

    fun setSttOtherAudio(context: Context, mode: com.cayboard.speech.VoiceOtherAudio) {
        getPrefs(context).edit().putString(KEY_STT_OTHER_AUDIO, mode.id).apply()
    }

    /** VoiceAudioFocus 退回静音媒体音量时记下，进程中途结束也能在下次补回声音。 */
    internal fun isSttMusicMutedByVoice(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_STT_MUSIC_MUTED_BY_VOICE, false)

    internal fun setSttMusicMutedByVoice(context: Context, muted: Boolean) {
        // 记“静了”要立刻落盘，进程随后被杀也能补回；清除可以慢一点，残留只会多解一次静音。
        val editor = getPrefs(context).edit().putBoolean(KEY_STT_MUSIC_MUTED_BY_VOICE, muted)
        if (muted) editor.commit() else editor.apply()
    }

    /** VoiceAudioFocus 压低媒体音量时记下原音量（-1 表示没压），用途同上。 */
    internal fun getSttMusicVolumeBeforeVoice(context: Context): Int =
        getPrefs(context).getInt(KEY_STT_MUSIC_VOLUME_BEFORE_VOICE, -1)

    internal fun setSttMusicVolumeBeforeVoice(context: Context, volume: Int) {
        val editor = getPrefs(context).edit().putInt(KEY_STT_MUSIC_VOLUME_BEFORE_VOICE, volume)
        if (volume >= 0) editor.commit() else editor.apply()
    }

    fun isSwipeUpHintsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SWIPE_UP_HINTS_ENABLED, true)
    }
    
    fun setSwipeUpHintsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SWIPE_UP_HINTS_ENABLED, enabled).apply()
    }
    
    fun isSwipeDownHintsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SWIPE_DOWN_HINTS_ENABLED, true)
    }

    fun setSwipeDownHintsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SWIPE_DOWN_HINTS_ENABLED, enabled).apply()
    }

    /** 获取方案偏好的键盘布局，默认全键盘 */
    
    /** 保存方案偏好的键盘布局 */
    
    fun getKeyboardHeightDp(context: Context): Int {
        return getKeyboardHeightDp(context, false)
    }

    fun getKeyboardHeightDp(context: Context, isLandscape: Boolean): Int {
        val key = if (isLandscape) KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE else KEY_KEYBOARD_HEIGHT_DP
        val stored = getPrefs(context).getInt(key, -1)
        if (stored > 0) return if (isLandscape) stored.coerceIn(com.cayboard.ui.keyboard.keyboardHeightBounds(context.resources.configuration.screenHeightDp, true)) else stored
        // 横竖屏各存一份：横屏没调过就用横屏默认高度，不再借竖屏存的高度（竖屏 40% 多的高度放到横屏会盖住大半个屏幕，用户 11-02）。
        return getDefaultKeyboardHeightDp(context, isLandscape)
    }

    fun setKeyboardHeightDp(context: Context, heightDp: Int, isLandscape: Boolean = false) {
        val key = if (isLandscape) KEY_KEYBOARD_HEIGHT_DP_LANDSCAPE else KEY_KEYBOARD_HEIGHT_DP
        getPrefs(context).edit().putInt(key, heightDp).apply()
    }

    fun getDefaultKeyboardHeightDp(context: Context, isLandscape: Boolean = false): Int {
        val config = context.resources.configuration
        if (!isLandscape) return maxOf(config.screenWidthDp, config.screenHeightDp) * DEFAULT_KEYBOARD_HEIGHT_PERCENT / 100
        // 横屏以竖屏高度（长边）为基数：横屏短边随宽高比/系统栏波动大，
        // 长边是设备稳定值，且与悬浮模式 fallback（portraitScreenHeightDp × 百分比）语义一致
        val portraitHeightDp = maxOf(config.screenWidthDp, config.screenHeightDp)
        return (portraitHeightDp * DEFAULT_KEYBOARD_HEIGHT_PERCENT_LANDSCAPE / 100).coerceIn(com.cayboard.ui.keyboard.keyboardHeightBounds(config.screenHeightDp, true))
    }

    const val KEY_KEYBOARD_BOTTOM_PADDING_DP = "keyboard_bottom_padding_dp"
    internal const val KEY_KEYBOARD_BOTTOM_LOWERING_DP = "keyboard_bottom_lowering_dp"
    const val KEY_EDGE_TO_EDGE_KEYBOARD = "edge_to_edge_keyboard"

    /**
     * 「手势导航时键盘贴底」默认开（11-03 用户「这个必须得给我解决了 拖了好几个版本」：手势导航下键盘底下那段默认就该没有）。
     * 没动过这项的老用户也跟着开；自己关掉过的（存了 false）照旧关着。三键导航和任务栏不受它影响。
     */
    fun edgeToEdgeKeyboard(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_EDGE_TO_EDGE_KEYBOARD, true)

    fun setEdgeToEdgeKeyboard(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EDGE_TO_EDGE_KEYBOARD, enabled).apply()
    }

    /**
     * 「底部位置」（用户 11-08：贴底改了多少版都有人说坏了，给几个简单选项）：
     * 自动 = 1.15.66 的贴底算法原样不动；贴到最底 = 不给导航按钮留位（全面屏隐藏了切换键盘、收起键盘按钮的机型）；
     * 留出导航按钮 = 整条导航栏都留着，键盘不盖住也不被那两个按钮盖住。原「手势导航时键盘贴底」开关并进这里：
     * 没选过时，关过贴底的老用户算「留出导航按钮」，其余算「自动」。
     */
    enum class KeyboardBottomPosition(val label: String, val description: String) {
        AUTO("自动", "按导航方式自动判断，1.15.66 的贴底效果。"),
        FLUSH("贴到最底", "不给导航按钮留位，键盘一直贴到屏幕底边。适合隐藏了切换输入法、收起键盘按钮的全面屏。"),
        KEEP_BUTTONS("留出导航按钮", "整条导航栏都留着：底下有切换输入法、收起键盘按钮时，键盘不盖住它们，也不被它们盖住。"),
    }

    const val KEY_KEYBOARD_BOTTOM_POSITION = "keyboard_bottom_position"

    fun keyboardBottomPosition(context: Context): KeyboardBottomPosition {
        val prefs = getPrefs(context)
        prefs.getString(KEY_KEYBOARD_BOTTOM_POSITION, null)?.let { stored ->
            KeyboardBottomPosition.entries.firstOrNull { it.name == stored }?.let { return it }
        }
        return if (!prefs.getBoolean(KEY_EDGE_TO_EDGE_KEYBOARD, true)) KeyboardBottomPosition.KEEP_BUTTONS
            else KeyboardBottomPosition.AUTO
    }

    /** 选一档同时写贴底开关：自动和贴到最底按贴底算（调低从贴底位置往下），留出导航按钮按不贴底算。 */
    fun setKeyboardBottomPosition(context: Context, position: KeyboardBottomPosition) {
        getPrefs(context).edit().putString(KEY_KEYBOARD_BOTTOM_POSITION, position.name)
            .putBoolean(KEY_EDGE_TO_EDGE_KEYBOARD, position != KeyboardBottomPosition.KEEP_BUTTONS).apply()
    }

    private const val DEFAULT_KEYBOARD_BOTTOM_PADDING_DP = 0

    fun getKeyboardBottomPaddingDp(context: Context): Int {
        return getPrefs(context).getInt(KEY_KEYBOARD_BOTTOM_PADDING_DP, DEFAULT_KEYBOARD_BOTTOM_PADDING_DP).coerceAtLeast(0)
    }

    /**
     * 旧字段始终保存非负留白，低于系统建议值的校准单独保存。
     * 贴底时的「调低」另存一项（[KEY_KEYBOARD_BOTTOM_EDGE_LOWERING_DP]，从贴底位置往下算）：贴底默认打开后，
     * 原来按系统建议调低的值不会被搬到贴底位置下面去，三键导航时也就不会压到系统按钮。
     */
    internal fun getKeyboardBottomAdjustmentDp(context: Context): Int =
        getKeyboardBottomPaddingDp(context).takeIf { it > 0 }
            ?: -getPrefs(context).getInt(loweringKey(context), 0).coerceAtLeast(0)

    internal const val KEY_KEYBOARD_BOTTOM_EDGE_LOWERING_DP = "keyboard_bottom_edge_lowering_dp"

    private fun loweringKey(context: Context) =
        if (edgeToEdgeKeyboard(context)) KEY_KEYBOARD_BOTTOM_EDGE_LOWERING_DP else KEY_KEYBOARD_BOTTOM_LOWERING_DP

    fun setKeyboardBottomPaddingDp(context: Context, paddingDp: Int) {
        setKeyboardBottomAdjustmentDp(context, paddingDp.coerceAtLeast(0))
    }

    internal fun setKeyboardBottomAdjustmentDp(context: Context, adjustmentDp: Int) {
        val edit = getPrefs(context).edit().putInt(KEY_KEYBOARD_BOTTOM_PADDING_DP, adjustmentDp.coerceAtLeast(0))
        val lowering = loweringKey(context)
        if (adjustmentDp < 0) edit.putInt(lowering, -adjustmentDp)
        else edit.remove(lowering)
        edit.apply()
    }

    /** 键盘上次量到的「最多还能调低多少 dp」（系统建议值减安全下限，按当前方向）；没量过为 null。 */
    internal const val KEY_KEYBOARD_BOTTOM_LOWERING_LIMIT_DP = "keyboard_bottom_lowering_limit_dp"

    internal fun getKeyboardBottomLoweringLimitDp(context: Context): Int? =
        getPrefs(context).getInt(KEY_KEYBOARD_BOTTOM_LOWERING_LIMIT_DP, -1).takeIf { it >= 0 }

    /** 键盘量到新的下限时写一次；值没变不写，免得每次 insets 回调都触发设置页刷新。 */
    internal fun setKeyboardBottomLoweringLimitDp(context: Context, limitDp: Int) {
        val value = limitDp.coerceAtLeast(0)
        if (getKeyboardBottomLoweringLimitDp(context) != value)
            getPrefs(context).edit().putInt(KEY_KEYBOARD_BOTTOM_LOWERING_LIMIT_DP, value).apply()
    }

    internal fun resetKeyboardBottomCalibration(context: Context) {
        // 「恢复系统建议」就是用系统给的底边：明确记下贴底关，不回到默认的开。
        // 「底部位置」回到自动（66 的效果），留白清零。
        getPrefs(context).edit().putBoolean(KEY_EDGE_TO_EDGE_KEYBOARD, true)
            .putString(KEY_KEYBOARD_BOTTOM_POSITION, KeyboardBottomPosition.AUTO.name).remove(KEY_KEYBOARD_BOTTOM_PADDING_DP)
            .remove(KEY_KEYBOARD_BOTTOM_LOWERING_DP).remove(KEY_KEYBOARD_BOTTOM_EDGE_LOWERING_DP).apply()
    }

    fun isSchemaImportWarningDismissed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SCHEMA_IMPORT_WARNING_DISMISSED, false)
    }

    fun setSchemaImportWarningDismissed(context: Context, dismissed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SCHEMA_IMPORT_WARNING_DISMISSED, dismissed).apply()
    }

    // ── 悬浮键盘设置 ──

    private const val KEY_FLOATING_MODE = "floating_mode"
    /** 展开菜单里直接编辑快捷入口（PR #232）；默认关，关着时菜单和以前一样，排列在「排列」面板里改。 */
    internal const val KEY_INLINE_QUICK_ACTION_EDITING = "inline_quick_action_editing"
    fun inlineQuickActionEditing(context: Context): Boolean = getPrefs(context).getBoolean(KEY_INLINE_QUICK_ACTION_EDITING, false)
    fun setInlineQuickActionEditing(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_INLINE_QUICK_ACTION_EDITING, enabled).apply()
    }

    internal const val KEY_FLOATING_GEOMETRY_LOCKED = "floating_geometry_locked"
    fun floatingGeometryLocked(context: Context): Boolean = getPrefs(context).getBoolean(KEY_FLOATING_GEOMETRY_LOCKED, false)
    fun setFloatingGeometryLocked(context: Context, locked: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FLOATING_GEOMETRY_LOCKED, locked).apply()
    }
    private const val KEY_FLOATING_MODE_LANDSCAPE = "floating_mode_landscape"
    private const val KEY_FLOATING_OFFSET_X = "floating_offset_x"
    private const val KEY_FLOATING_OFFSET_X_LANDSCAPE = "floating_offset_x_landscape"
    private const val KEY_FLOATING_OFFSET_Y = "floating_offset_y"
    private const val KEY_FLOATING_OFFSET_Y_LANDSCAPE = "floating_offset_y_landscape"
    private const val KEY_FLOATING_ROTATION = "floating_rotation"
    private const val KEY_FLOATING_ROTATION_LANDSCAPE = "floating_rotation_landscape"
    private const val KEY_FLOATING_WIDTH_DP = "floating_width_dp"
    private const val KEY_FLOATING_WIDTH_DP_LANDSCAPE = "floating_width_dp_landscape"
    private const val KEY_KEYBOARD_OPACITY = "keyboard_opacity"

    fun getKeyboardOpacity(context: Context): Float {
        val landscape = context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp
        // 横屏新装和竖屏一样固定、不透明（用户 10-31）。早先版本第一次横屏时写入了半透明悬浮（landscape_layout_v2），
        // 写过的人保留原值；没写过的按这里的默认。
        return getPrefs(context).getFloat(if (landscape) "keyboard_opacity_landscape" else KEY_KEYBOARD_OPACITY, 1f).coerceIn(0.3f, 1f)
    }

    fun setKeyboardOpacity(context: Context, opacity: Float) {
        val landscape = context.resources.configuration.screenWidthDp > context.resources.configuration.screenHeightDp
        getPrefs(context).edit().putFloat(if (landscape) "keyboard_opacity_landscape" else KEY_KEYBOARD_OPACITY, opacity.coerceIn(0.3f, 1f)).apply()
    }

    fun isFloatingMode(context: Context, isLandscape: Boolean = false): Boolean {
        val key = if (isLandscape) KEY_FLOATING_MODE_LANDSCAPE else KEY_FLOATING_MODE
        return getPrefs(context).getBoolean(key, false)
    }

    fun setFloatingMode(context: Context, enabled: Boolean, isLandscape: Boolean = false) {
        val key = if (isLandscape) KEY_FLOATING_MODE_LANDSCAPE else KEY_FLOATING_MODE
        getPrefs(context).edit().putBoolean(key, enabled).apply()
    }

    fun getFloatingOffsetX(context: Context, isLandscape: Boolean = false): Int {
        val key = if (isLandscape) KEY_FLOATING_OFFSET_X_LANDSCAPE else KEY_FLOATING_OFFSET_X
        return getPrefs(context).getInt(key, 0)
    }

    fun setFloatingOffsetX(context: Context, offset: Int, isLandscape: Boolean = false) {
        val key = if (isLandscape) KEY_FLOATING_OFFSET_X_LANDSCAPE else KEY_FLOATING_OFFSET_X
        getPrefs(context).edit().putInt(key, offset).apply()
    }

    fun getFloatingOffsetY(context: Context, isLandscape: Boolean = false): Int =
        getFloatingOffsetY(context, isLandscape, 0)

    internal fun getFloatingOffsetY(context: Context, isLandscape: Boolean = false, defaultDp: Int): Int {
        val key = if (isLandscape) KEY_FLOATING_OFFSET_Y_LANDSCAPE else KEY_FLOATING_OFFSET_Y
        return getPrefs(context).getInt(key, defaultDp)
    }

    fun setFloatingOffsetY(context: Context, offset: Int, isLandscape: Boolean = false) {
        val key = if (isLandscape) KEY_FLOATING_OFFSET_Y_LANDSCAPE else KEY_FLOATING_OFFSET_Y
        getPrefs(context).edit().putInt(key, offset).apply()
    }

    /** 悬浮键盘旋转角（度，顺时针为正，-90..90），和位置一样横竖屏各存一份。 */
    fun getFloatingRotation(context: Context, isLandscape: Boolean = false): Float {
        val key = if (isLandscape) KEY_FLOATING_ROTATION_LANDSCAPE else KEY_FLOATING_ROTATION
        return getPrefs(context).getFloat(key, 0f).coerceIn(-90f, 90f)
    }

    fun setFloatingRotation(context: Context, degrees: Float, isLandscape: Boolean = false) {
        val key = if (isLandscape) KEY_FLOATING_ROTATION_LANDSCAPE else KEY_FLOATING_ROTATION
        getPrefs(context).edit().putFloat(key, degrees.coerceIn(-90f, 90f)).apply()
    }

    /**
     * 悬浮键盘宽度（dp）；0 = 未设置，按高度自动推导（历史行为）。
     * 用户在键盘调节里拖左右边或四角后写入具体值，之后宽度与高度各自独立，
     * 「点哪个边只调哪个方向」才有意义。
     */
    fun getFloatingWidthDp(context: Context, isLandscape: Boolean = false): Int {
        val key = if (isLandscape) KEY_FLOATING_WIDTH_DP_LANDSCAPE else KEY_FLOATING_WIDTH_DP
        return getPrefs(context).getInt(key, 0)
    }

    fun setFloatingWidthDp(context: Context, widthDp: Int, isLandscape: Boolean = false) {
        val key = if (isLandscape) KEY_FLOATING_WIDTH_DP_LANDSCAPE else KEY_FLOATING_WIDTH_DP
        getPrefs(context).edit().putInt(key, widthDp).apply()
    }

    /**
     * 固定键盘横竖屏独立，并且每块屏各存一份；0 宽度代表铺满宿主，偏移相对宿主中心。
     * 折叠屏的外屏、内屏都是竖屏，原来共用一份，外屏调好的宽度展开后带到内屏，内屏占不满（GitHub #37）。
     * 屏按最短边 dp 区分（转屏不变，外屏和内屏差得很远）。
     */
    fun getFixedWidthDp(context: Context, landscape: Boolean): Int = fixedGeometry(context, "fixed_width_dp", landscape)

    fun getFixedOffsetX(context: Context, landscape: Boolean): Int = fixedGeometry(context, "fixed_offset_x", landscape)

    fun setFixedHorizontalGeometry(context: Context, width: Int, offset: Int, landscape: Boolean) {
        val suffix = if (landscape) "_landscape" else ""
        val screen = fixedGeometryScreen(context)
        // 不带屏的旧键照样写，并记下是哪块屏调的；另存一份带屏的，换回这块屏时用。
        getPrefs(context).edit().putInt("fixed_width_dp$suffix", width)
            .putInt("fixed_offset_x$suffix", offset)
            .putInt("fixed_width_dp${suffix}_sw$screen", width)
            .putInt("fixed_offset_x${suffix}_sw$screen", offset)
            .putInt("fixed_geometry_screen$suffix", screen).apply()
    }

    private fun fixedGeometryScreen(context: Context): Int = context.resources.configuration.smallestScreenWidthDp

    private fun fixedGeometry(context: Context, name: String, landscape: Boolean): Int {
        val prefs = getPrefs(context)
        val suffix = if (landscape) "_landscape" else ""
        val screen = fixedGeometryScreen(context)
        val own = "$name${suffix}_sw$screen"
        return resolveFixedGeometry(
            ownValue = if (prefs.contains(own)) prefs.getInt(own, 0) else null,
            sharedValue = prefs.getInt("$name$suffix", 0),
            sharedScreen = prefs.getInt("fixed_geometry_screen$suffix", 0),
            screen = screen,
        )
    }

    /**
     * 不带屏的那一份是最后一次调节写的，在调它的那块屏上照旧用它；旧版本存的值不知道是哪块屏，也照旧用，
     * 单屏手机、平板升级后不变，改系统显示大小这类小变化（最短边差不到 15%）也算同一块屏。
     * 换到大小差很多的另一块屏（折叠屏外屏↔内屏）时用那块屏自己存的；那块屏没调过就铺满、居中（默认）。
     */
    internal fun resolveFixedGeometry(ownValue: Int?, sharedValue: Int, sharedScreen: Int, screen: Int): Int {
        if (sharedScreen <= 0 || screen <= 0) return sharedValue
        val sameScreen = kotlin.math.abs(sharedScreen - screen) * 100 <= maxOf(sharedScreen, screen) * 15
        return if (sameScreen) sharedValue else ownValue ?: 0
    }

    fun getPageSize(context: Context): Int = DEFAULT_PAGE_SIZE

    fun setPageSize(context: Context, pageSize: Int) { }

    const val DEFAULT_CANDIDATE_TEXT_SIZE = 19

    fun getCandidateTextSize(context: Context): Int = DEFAULT_CANDIDATE_TEXT_SIZE

    fun setCandidateTextSize(context: Context, size: Int) { }

    // ── 方案市场「已安装」的持久记录 ──
    // 记录用户通过市场主动安装过的方案 id；与本地文件存在性解耦（方案可能仅作为依赖落盘，
    // 文件存在不代表用户装过它），且跨重启保持。
    fun getInstalledMarketIds(context: Context): Set<String> =
        getPrefs(context).getStringSet(KEY_INSTALLED_MARKET_IDS, emptySet())?.toSet() ?: emptySet()

    fun addInstalledMarketId(context: Context, id: String) {
        val cur = getInstalledMarketIds(context).toMutableSet()
        if (cur.add(id)) {
            getPrefs(context).edit().putStringSet(KEY_INSTALLED_MARKET_IDS, cur).apply()
        }
    }

    fun removeInstalledMarketId(context: Context, id: String) {
        val cur = getInstalledMarketIds(context).toMutableSet()
        if (cur.remove(id)) {
            getPrefs(context).edit().putStringSet(KEY_INSTALLED_MARKET_IDS, cur).apply()
        }
    }


}




