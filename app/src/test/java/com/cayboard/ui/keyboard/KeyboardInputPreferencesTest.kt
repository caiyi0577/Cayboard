package com.cayboard.ui.keyboard

import android.content.Context
import android.content.SharedPreferences
import com.cayboard.settings.SettingsPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class KeyboardInputPreferencesTest {
    @Test fun newDefaultsApplyOnlyWhenNoPreferenceWasSaved() {
        val before = KeyboardInputPreferences.read(context, isDark = false)
        assertTrue(before.datetimeCandidates && before.candidateEmoji && before.symbolNames && before.greekLetters)
        assertTrue(before.numberRow)
        assertTrue(SettingsPreferences.isPunctuationCommitsCandidateEnabled(context))
        assertTrue(SettingsPreferences.isPairedSymbolsEnabled(context))
        assertTrue(SettingsPreferences.isEnterSendsEnabled(context))
        val toolbar = listOf("emoji", "clipboard", "translate", "voice")
        val menuFirstRow = listOf("schema", "symbol", "handwriting_lookup", "edit")
        assertEquals(toolbar, SettingsPreferences.getToolbarButtons(context))
        assertEquals(menuFirstRow, SettingsPreferences.getMenuButtons(context).take(4))

        val chosenMenu = listOf("settings", "edit", "sound", "arrange")
        val saved = com.cayboard.keyboard.QuickActionLayout(menuFirstRow + toolbar, chosenMenu)
        SettingsPreferences.setQuickActions(context, saved)
        SettingsPreferences.migrateQuickActionMenu(context)
        assertEquals(toolbar, SettingsPreferences.getToolbarButtons(context))
        assertEquals(menuFirstRow + listOf("settings", "sound", "arrange"), SettingsPreferences.getMenuButtons(context))
        SettingsPreferences.setQuickActions(context, com.cayboard.keyboard.QuickActionLayout(toolbar, chosenMenu))
        SettingsPreferences.migrateQuickActionMenu(context)
        assertEquals(chosenMenu, SettingsPreferences.getMenuButtons(context))
        assertEquals("t9_pinyin", SettingsPreferences.getCurrentSchema(context))

        before.copy(datetimeCandidates = false, candidateEmoji = false, symbolNames = false, greekLetters = false).save(context)
        SettingsPreferences.setNumberRowEnabled(context, false)
        SettingsPreferences.setPunctuationCommitsCandidateEnabled(context, false)
        SettingsPreferences.setPairedSymbolsEnabled(context, false)
        SettingsPreferences.setEnterSendsEnabled(context, false)
        SettingsPreferences.setToolbarButtons(context, listOf("voice", "emoji"))
        SettingsPreferences.setCurrentSchema(context, "cayboard_pinyin")
        val after = KeyboardInputPreferences.read(context, isDark = false)
        assertEquals(listOf(false, false, false, false, false),
            listOf(after.datetimeCandidates, after.candidateEmoji, after.symbolNames, after.greekLetters, after.numberRow))
        assertEquals(false, SettingsPreferences.isPunctuationCommitsCandidateEnabled(context))
        assertEquals(false, SettingsPreferences.isPairedSymbolsEnabled(context))
        assertEquals(false, SettingsPreferences.isEnterSendsEnabled(context))
        assertEquals(listOf("voice", "emoji"), SettingsPreferences.getToolbarButtons(context))
        assertEquals("cayboard_pinyin", SettingsPreferences.getCurrentSchema(context))
    }

    @Test fun `choosing voice for long-press space keeps cursor gestures available`() {
        val cursorOnSpace = KeyboardInputPreferences(cursorGesture = CursorGestureMode.SPACE)
        assertEquals(SpaceHoldAction.CURSOR, cursorOnSpace.effectiveSpaceHold)
        val voice = cursorOnSpace.withSpaceHold(SpaceHoldAction.VOICE_TOGGLE)
        assertEquals(SpaceHoldAction.VOICE_TOGGLE, voice.effectiveSpaceHold)
        assertEquals(CursorGestureMode.KEYBOARD, voice.cursorGesture)
        // 已移除字母滑行后，键盘滑动可以独立用于移动光标。
        val glide = cursorOnSpace.copy(glideTyping = true).withSpaceHold(SpaceHoldAction.REPEAT)
        assertEquals(SpaceHoldAction.REPEAT, glide.effectiveSpaceHold)
        assertEquals(CursorGestureMode.KEYBOARD, glide.cursorGesture)
        assertEquals(true, glide.glideTyping)
        // 没占着空格时只换空格功能，移动光标方式原样不动。
        for (mode in listOf(CursorGestureMode.KEYBOARD, CursorGestureMode.NONE)) {
            val other = KeyboardInputPreferences(spaceHold = SpaceHoldAction.REPEAT, cursorGesture = mode)
                .withSpaceHold(SpaceHoldAction.VOICE_TOGGLE)
            assertEquals(mode, other.cursorGesture)
            assertEquals(SpaceHoldAction.VOICE_TOGGLE, other.effectiveSpaceHold)
        }
    }

    @Test fun `uppercase key labels default off and persist without touching other settings`() {
        val before = KeyboardInputPreferences.read(context)
        assertEquals(false, before.uppercaseKeyLabels)
        assertTrue(KeyboardInputPreferences.UPPERCASE_KEY_LABELS_KEY in KeyboardInputPreferences.watchedKeys)
        for (enabled in listOf(true, false)) {
            before.copy(uppercaseKeyLabels = enabled).save(context)
            val after = KeyboardInputPreferences.read(context)
            assertEquals(enabled, after.uppercaseKeyLabels)
            assertEquals(before, after.copy(uppercaseKeyLabels = false))
        }
    }

    @Test fun `chinese uppercase key labels default off and persist without touching other settings`() {
        val before = KeyboardInputPreferences.read(context)
        assertEquals(false, before.chineseUppercaseKeyLabels)
        assertTrue(KeyboardInputPreferences.CHINESE_UPPERCASE_KEY_LABELS_KEY in KeyboardInputPreferences.watchedKeys)
        for (enabled in listOf(true, false)) {
            before.copy(chineseUppercaseKeyLabels = enabled).save(context)
            val after = KeyboardInputPreferences.read(context)
            assertEquals(enabled, after.chineseUppercaseKeyLabels)
            assertEquals(before, after.copy(chineseUppercaseKeyLabels = false))
        }
    }

    @Test fun `symbol direction defaults to pulling into key and persists either habit`() {
        assertEquals(false, KeyboardInputPreferences.read(context).reverseSymbolSwipe)
        for (reversed in listOf(true, false)) {
            val before = KeyboardInputPreferences.read(context)
            before.copy(reverseSymbolSwipe = reversed).save(context)
            val after = KeyboardInputPreferences.read(context)
            assertEquals(reversed, after.reverseSymbolSwipe)
            assertEquals(before, after.copy(reverseSymbolSwipe = before.reverseSymbolSwipe))
        }
    }
    @Test fun `fourteen swipe preferences reload without changing shared cursor or symbol settings`() {
        val previous = KeyboardInputPreferences.read(context)
        assertEquals(true, previous.fourteenLetterSwipe)
        assertEquals(24f, previous.fourteenLetterSwipeDp, 0f)
        previous.copy(fourteenLetterSwipe = false, fourteenLetterSwipeDp = 51f).save(context)
        val restored = KeyboardInputPreferences.read(context)
        assertEquals(false, restored.fourteenLetterSwipe)
        assertEquals(52f, restored.fourteenLetterSwipeDp, 0f)
        assertEquals(previous.cursorGesture, restored.cursorGesture)
        assertEquals(previous.symbolInputMode, restored.symbolInputMode)
        assertEquals(previous.neighborCorrection, restored.neighborCorrection)
    }

    @Test fun `comma and period keys are both on by default and the old saved period default does not turn it off`() {
        val fresh = KeyboardInputPreferences.read(context)
        assertEquals(true, fresh.qwertyCommaKey)
        assertEquals(true, fresh.qwertyPeriodKey)
        // 旧版 save() 会把旧默认 false 连同其他设置一起写进 qwerty_period_key：不能因此关掉。
        values["qwerty_period_key"] = false
        assertEquals(true, KeyboardInputPreferences.read(context).qwertyPeriodKey)
        // 用户在新版自己关掉后保持关。
        KeyboardInputPreferences.read(context).copy(qwertyPeriodKey = false).save(context)
        assertEquals(false, KeyboardInputPreferences.read(context).qwertyPeriodKey)
    }

    @Test fun `corner hint opacity defaults to today's look, persists and is bounded`() {
        assertEquals(1f, KeyboardInputPreferences().cornerHintOpacity, 0f)
        assertEquals(1f, KeyboardInputPreferences.read(context).cornerHintOpacity, 0f)
        KeyboardInputPreferences.read(context).copy(cornerHintOpacity = 0.35f).save(context)
        assertEquals(0.35f, KeyboardInputPreferences.read(context).cornerHintOpacity, 0.001f)
        assertEquals(0.1f, normalizeCornerHintOpacity(0f), 0f)
        assertEquals(1f, normalizeCornerHintOpacity(3f), 0f)
        assertEquals(1f, normalizeCornerHintOpacity(Float.NaN), 0f)
        assertTrue(KeyboardInputPreferences.CORNER_HINT_OPACITY_KEY in KeyboardInputPreferences.watchedKeys)
    }

    @Test fun `time and date candidates default on and persist either choice`() {
        assertEquals(true, KeyboardInputPreferences().datetimeCandidates)
        assertEquals(true, KeyboardInputPreferences.read(context).datetimeCandidates)
        KeyboardInputPreferences.read(context).copy(datetimeCandidates = false).save(context)
        assertEquals(false, values[KeyboardInputPreferences.DATETIME_CANDIDATES_KEY])
        assertEquals(false, KeyboardInputPreferences.read(context).datetimeCandidates)
        KeyboardInputPreferences.read(context).copy(datetimeCandidates = true).save(context)
        assertEquals(true, KeyboardInputPreferences.read(context).datetimeCandidates)
    }

    @Test fun `symbol name candidates default on and persist either choice`() {
        assertEquals(true, KeyboardInputPreferences().symbolNames)
        assertEquals(true, KeyboardInputPreferences.read(context).symbolNames)
        KeyboardInputPreferences.read(context).copy(symbolNames = true).save(context)
        assertEquals(true, values[KeyboardInputPreferences.SYMBOL_NAMES_KEY])
        assertEquals(true, KeyboardInputPreferences.read(context).symbolNames)
        KeyboardInputPreferences.read(context).copy(symbolNames = false).save(context)
        assertEquals(false, KeyboardInputPreferences.read(context).symbolNames)
    }

    @Test fun `greek letter candidates default on and persist either choice`() {
        assertEquals(true, KeyboardInputPreferences().greekLetters)
        assertEquals(true, KeyboardInputPreferences.read(context).greekLetters)
        KeyboardInputPreferences.read(context).copy(greekLetters = true).save(context)
        assertEquals(true, values[KeyboardInputPreferences.GREEK_LETTERS_KEY])
        assertEquals(true, KeyboardInputPreferences.read(context).greekLetters)
        KeyboardInputPreferences.read(context).copy(greekLetters = false).save(context)
        assertEquals(false, KeyboardInputPreferences.read(context).greekLetters)
    }

    @Test fun `bottom lift cursor is off by default and persists without touching the cursor gesture choice`() {
        val before = KeyboardInputPreferences.read(context)
        assertEquals(false, before.bottomLiftCursor)
        for (enabled in listOf(true, false)) {
            before.copy(bottomLiftCursor = enabled).save(context)
            val after = KeyboardInputPreferences.read(context)
            assertEquals(enabled, after.bottomLiftCursor)
            assertEquals(before, after.copy(bottomLiftCursor = false))
        }
        assertTrue(KeyboardInputPreferences.BOTTOM_LIFT_CURSOR_KEY in KeyboardInputPreferences.watchedKeys)
    }

    @Test fun `delete repeat speed defaults to the old feel and persists every step`() {
        val before = KeyboardInputPreferences.read(context)
        assertEquals(com.cayboard.keyboard.DeleteRepeatSpeed.STANDARD, before.deleteRepeatSpeed)
        for (speed in com.cayboard.keyboard.DeleteRepeatSpeed.entries) {
            before.copy(deleteRepeatSpeed = speed).save(context)
            val after = KeyboardInputPreferences.read(context)
            assertEquals(speed, after.deleteRepeatSpeed)
            assertEquals(before, after.copy(deleteRepeatSpeed = before.deleteRepeatSpeed))
        }
        assertTrue(com.cayboard.keyboard.DeleteRepeatSpeed.KEY in KeyboardInputPreferences.watchedKeys)
    }

    @Test fun `delete swipe reverse and clear distance default to the old behaviour and persist`() {
        val before = KeyboardInputPreferences.read(context)
        assertEquals(false, before.reverseDeleteSwipe)
        assertEquals(100f, before.clearSwipePercent, 0.001f)
        assertEquals(80f, before.clearSwipeDistanceDp, 0.001f)
        assertEquals(before.reverseSymbolSwipe, before.deleteSwipeReversed)
        before.copy(reverseDeleteSwipe = true, clearSwipePercent = 70f).save(context)
        val after = KeyboardInputPreferences.read(context)
        assertEquals(true, after.reverseDeleteSwipe)
        assertEquals(56f, after.clearSwipeDistanceDp, 0.001f)
        assertEquals(before, after.copy(reverseDeleteSwipe = false, clearSwipePercent = 100f))
        // 单独反向在「反转上下滑方向」之上再对调
        assertEquals(true, KeyboardInputPreferences(reverseDeleteSwipe = true).deleteSwipeReversed)
        assertEquals(false, KeyboardInputPreferences(reverseSymbolSwipe = true, reverseDeleteSwipe = true).deleteSwipeReversed)
        assertTrue(KeyboardInputPreferences.REVERSE_DELETE_SWIPE_KEY in KeyboardInputPreferences.watchedKeys)
        assertTrue(KeyboardInputPreferences.CLEAR_SWIPE_PERCENT_KEY in KeyboardInputPreferences.watchedKeys)
    }

    @Test fun `clear distance stays inside the narrow fine-tune range`() {
        assertEquals(70f, normalizeClearSwipePercent(10f), 0.001f)
        assertEquals(120f, normalizeClearSwipePercent(500f), 0.001f)
        assertEquals(90f, normalizeClearSwipePercent(88f), 0.001f)
        assertEquals(100f, normalizeClearSwipePercent(Float.NaN), 0.001f)
    }

    @Test fun `every saved setting refreshes the keyboard already on screen`() {
        SettingsPreferences.setNumberRowEnabled(context, false)
        val unwatched = values.keys - KeyboardInputPreferences.watchedKeys
        assertTrue("save() 写入但键盘不监听：$unwatched", unwatched.isEmpty())
        assertTrue(SettingsPreferences.KEY_NUMBER_ROW in values)
    }

    private val context = mock<Context>()
    private val prefs = mock<SharedPreferences>()
    private val editor = mock<SharedPreferences.Editor>()
    private val values = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(prefs.contains(any())).thenAnswer { values.containsKey(it.getArgument<String>(0)) }
        whenever(prefs.getInt(any(), any())).thenAnswer {
            values[it.getArgument<String>(0)] as? Int ?: it.getArgument<Int>(1)
        }
        whenever(prefs.getString(any(), anyOrNull())).thenAnswer {
            values[it.getArgument<String>(0)] as? String ?: it.getArgument<String?>(1)
        }
        whenever(prefs.getBoolean(any(), any())).thenAnswer {
            values[it.getArgument<String>(0)] as? Boolean ?: it.getArgument<Boolean>(1)
        }
        whenever(prefs.getFloat(any(), any())).thenAnswer {
            values[it.getArgument<String>(0)] as? Float ?: it.getArgument<Float>(1)
        }
        whenever(editor.putString(any(), anyOrNull())).thenAnswer {
            val value = it.getArgument<String?>(1)
            if (value != null) values[it.getArgument(0)] = value
            editor
        }
        whenever(editor.putBoolean(any(), any())).thenAnswer {
            values[it.getArgument(0)] = it.getArgument<Boolean>(1)
            editor
        }
        whenever(editor.putFloat(any(), any())).thenAnswer {
            values[it.getArgument(0)] = it.getArgument<Float>(1)
            editor
        }
        whenever(editor.remove(any())).thenAnswer {
            values.remove(it.getArgument<String>(0))
            editor
        }
    }

    @Test
    fun `existing installs default to cursor hold and usable sensitivity`() {
        val settings = KeyboardInputPreferences.read(context)
        assertEquals(SpaceHoldAction.CURSOR, settings.spaceHold)
        assertEquals(0.2f, settings.cursorHoldSeconds, 0f)
        assertEquals(200L, settings.spaceHoldDelayMs)
        assertEquals(10f, settings.cursorStepDp, 0f)
        assertEquals(1.2f, settings.keyTextScale, 0f)
        assertEquals(1.2f, KeyboardInputPreferences().keyTextScale, 0f)
        values["key_text_scale"] = 1.15f
        assertEquals(1.15f, KeyboardInputPreferences.read(context).keyTextScale, 0f)
        assertEquals(0.5f, settings.handwritingPauseSeconds, 0f)
        assertNull(settings.symbols())
    }

    

    

    

    @Test
    fun `saved input preferences can be read without restarting the app`() {
        val changed = KeyboardInputPreferences(spaceHold = SpaceHoldAction.REPEAT, cursorStepDp = 6f, keyTextScale = 1.4f, fixedSymbols = "…\n→")
        changed.save(context)
        assertEquals(changed, KeyboardInputPreferences.read(context))
        changed.copy(spaceHold = SpaceHoldAction.CURSOR, cursorStepDp = 24f).save(context)
        assertEquals(SpaceHoldAction.CURSOR, KeyboardInputPreferences.read(context).spaceHold)
        assertEquals(24f, KeyboardInputPreferences.read(context).cursorStepDp, 0f)
    }

    @Test fun `cursor hold delay persists tenths normalizes invalid values and leaves other actions unchanged`() {
        for (tenth in 1..10) {
            val seconds = tenth / 10f
            KeyboardInputPreferences(cursorHoldSeconds = seconds).save(context)
            val restored = KeyboardInputPreferences.read(context)
            assertEquals(seconds, restored.cursorHoldSeconds, 0f)
            assertEquals(tenth * 100L, restored.spaceHoldDelayMs)
            for (action in listOf(SpaceHoldAction.REPEAT, SpaceHoldAction.VOICE_TOGGLE)) {
                assertEquals("every space hold action uses the space hold delay", tenth * 100L,
                    restored.copy(spaceHold = action, cursorGesture = CursorGestureMode.NONE).spaceHoldDelayMs)
                assertEquals("Space cursor override uses its configured delay", tenth * 100L,
                    restored.copy(spaceHold = action, cursorGesture = CursorGestureMode.SPACE).spaceHoldDelayMs)
            }
        }
        val invalid = listOf(
            Float.NaN to 0.2f,
            Float.POSITIVE_INFINITY to 0.2f,
            Float.NEGATIVE_INFINITY to 0.2f,
            -1f to 0.1f,
            9f to 1f,
            0.26f to 0.3f,
        )
        for ((stored, expected) in invalid) {
            values["cursor_hold_seconds"] = stored
            assertEquals("Read normalizes $stored", expected, KeyboardInputPreferences.read(context).cursorHoldSeconds, 0f)
            KeyboardInputPreferences(cursorHoldSeconds = stored).save(context)
            assertEquals("Save normalizes $stored", expected, values["cursor_hold_seconds"] as Float, 0f)
            assertEquals(expected, KeyboardInputPreferences.read(context).cursorHoldSeconds, 0f)
        }
    }

    @Test
    fun `legacy voice hold migrates to cursor`() {
        values["space_hold_action"] = "VOICE"
        assertEquals(SpaceHoldAction.CURSOR, KeyboardInputPreferences.read(context).spaceHold)
    }

    @Test fun `symbol input keeps swipe default and persists an explicit hold choice`() {
        assertEquals(SymbolInputMode.SWIPE_UP, KeyboardInputPreferences.read(context).symbolInputMode)
        val hold = KeyboardInputPreferences(symbolInputMode = SymbolInputMode.LONG_PRESS,
            spaceHold = SpaceHoldAction.VOICE_TOGGLE, cursorGesture = CursorGestureMode.KEYBOARD)
        hold.save(context)
        assertEquals(hold, KeyboardInputPreferences.read(context))
        hold.copy(symbolInputMode = SymbolInputMode.SWIPE_UP).save(context)
        assertEquals(SymbolInputMode.SWIPE_UP, KeyboardInputPreferences.read(context).symbolInputMode)
        values["symbol_input_mode"] = "unknown-future-mode"
        assertEquals(SymbolInputMode.SWIPE_UP, KeyboardInputPreferences.read(context).symbolInputMode)
        assertEquals(SpaceHoldAction.VOICE_TOGGLE, KeyboardInputPreferences.read(context).effectiveSpaceHold)
    }

    @Test fun `explicit new voice toggle survives save while cursor override preserves it`() {
        val voice = KeyboardInputPreferences(spaceHold = SpaceHoldAction.VOICE_TOGGLE, cursorGesture = CursorGestureMode.KEYBOARD)
        voice.save(context)
        assertEquals(SpaceHoldAction.VOICE_TOGGLE, KeyboardInputPreferences.read(context).effectiveSpaceHold)
        voice.copy(cursorGesture = CursorGestureMode.SPACE).save(context)
        assertEquals(SpaceHoldAction.CURSOR, KeyboardInputPreferences.read(context).effectiveSpaceHold)
        assertEquals(SpaceHoldAction.VOICE_TOGGLE, KeyboardInputPreferences.read(context).spaceHold)
    }

    @Test
    fun `unknown modes and invalid numeric settings recover to usable defaults`() {
        values["space_hold_action"] = "REMOVED_MODE"
        values["cursor_step_dp"] = Float.NaN
        values["key_text_scale"] = Float.POSITIVE_INFINITY
        val settings = KeyboardInputPreferences.read(context)
        assertEquals(SpaceHoldAction.CURSOR, settings.spaceHold)
        assertEquals(10f, settings.cursorStepDp, 0f)
        assertEquals(1.2f, settings.keyTextScale, 0f)
    }

    @Test
    fun `fixed symbols preserve phrases and emoji while removing blank duplicate lines`() {
        val settings = KeyboardInputPreferences(fixedSymbols = " … \n\n👨‍👩‍👧‍👦\n自定义短语\n…\n")
        assertEquals(listOf("…", "👨‍👩‍👧‍👦", "自定义短语"), settings.symbols())
        assertNull(settings.copy(fixedSymbols = " \n \n").symbols())
    }
    @Test fun `handwriting pause persists and rejects invalid intervals`() {
        KeyboardInputPreferences(handwritingPauseSeconds = 1.8f).save(context)
        assertEquals(1.8f, KeyboardInputPreferences.read(context).handwritingPauseSeconds, 0f)
        for ((stored, expected) in listOf(Float.NaN to 0.5f, -1f to 0.1f, 9f to 2.5f)) {
            values["handwriting_pause_seconds_v2"] = stored
            assertEquals(expected, KeyboardInputPreferences.read(context).handwritingPauseSeconds, 0f)
        }
    }

    @Test fun `legacy one second default migrates but new explicit one second persists`() {
        values["handwriting_pause_seconds"] = 1f
        assertEquals(0.5f, KeyboardInputPreferences.read(context).handwritingPauseSeconds, 0f)
        KeyboardInputPreferences(handwritingPauseSeconds = 1f).save(context)
        assertEquals(1f, KeyboardInputPreferences.read(context).handwritingPauseSeconds, 0f)
    }
    @Test fun `handwriting delay saves tenths with a half second default`() {
        for (tenth in 1..25) {
            val value = tenth / 10f
            KeyboardInputPreferences(handwritingPauseSeconds = value).save(context)
            assertEquals(value, KeyboardInputPreferences.read(context).handwritingPauseSeconds, 0f)
        }
        KeyboardInputPreferences(handwritingPauseSeconds = 0.56f).save(context)
        assertEquals(0.6f, KeyboardInputPreferences.read(context).handwritingPauseSeconds, 0f)
    }

    @Test fun `split keyboard is opt in and persists independently of input settings`() {
        assertEquals(false, KeyboardInputPreferences.read(context).splitKeyboardEnabled)
        SettingsPreferences.setSplitKeyboardEnabled(context, true)
        assertEquals(true, KeyboardInputPreferences.read(context).splitKeyboardEnabled)
        KeyboardInputPreferences().save(context)
        assertEquals(true, SettingsPreferences.isSplitKeyboardEnabled(context))
        SettingsPreferences.setSplitKeyboardEnabled(context, false)
        assertEquals(false, KeyboardInputPreferences.read(context).splitKeyboardEnabled)
    }

    /** 屏幕太窄时分体不分开，但存着的「分体键盘」不改；转到够宽的方向再分开。 */
    @Test fun `split keyboard shows only on wide screens and keeps the stored choice`() {
        SettingsPreferences.setSplitKeyboardEnabled(context, true)
        assertEquals(false, SettingsPreferences.isSplitKeyboardShown(context, 393))
        assertEquals(true, SettingsPreferences.isSplitKeyboardEnabled(context))
        assertEquals(true, KeyboardInputPreferences.read(context).splitKeyboardEnabled)
        assertEquals(true, SettingsPreferences.isSplitKeyboardShown(context, 853))
        SettingsPreferences.setSplitKeyboardEnabled(context, false)
        for (width in listOf(393, 853)) assertEquals(false, SettingsPreferences.isSplitKeyboardShown(context, width))
    }

    

    

    @Test fun `language key and space have separate hold delays with their own defaults`() {
        val defaults = KeyboardInputPreferences()
        assertEquals(0.15f, defaults.languageMenuHoldSeconds, 0f)
        assertEquals(200L, defaults.spaceHoldDelayMs)
        KeyboardInputPreferences(languageMenuHoldSeconds = 0.35f, cursorHoldSeconds = 0.6f).save(context)
        val restored = KeyboardInputPreferences.read(context)
        assertEquals(0.35f, restored.languageMenuHoldSeconds, 0f)
        assertEquals(600L, restored.spaceHoldDelayMs)
    }

    @Test fun `stored old language hold default migrates to new default but custom values stay`() {
        values["language_key_hold_seconds"] = 0.1f
        assertEquals(0.15f, KeyboardInputPreferences.read(context).languageMenuHoldSeconds, 0f)
        values["language_key_hold_seconds"] = 0.35f
        assertEquals(0.35f, KeyboardInputPreferences.read(context).languageMenuHoldSeconds, 0f)
        KeyboardInputPreferences(languageMenuHoldSeconds = 0.1f).save(context)
        assertEquals(0.1f, KeyboardInputPreferences.read(context).languageMenuHoldSeconds, 0f)
    }

    @Test fun `corner long press delay persists and stays independent from retired glide input`() {
        assertEquals(0.3f, KeyboardInputPreferences().symbolLongPressSeconds, 0f)
        assertEquals(300L, KeyboardInputPreferences.read(context).symbolLongPressMs)
        KeyboardInputPreferences(symbolInputMode = SymbolInputMode.LONG_PRESS, symbolLongPressSeconds = 0.55f).save(context)
        val restored = KeyboardInputPreferences.read(context)
        assertEquals(0.55f, restored.symbolLongPressSeconds, 0.001f)
        assertEquals(550L, restored.symbolLongPressMs)
        assertTrue("symbol_long_press_seconds" in KeyboardInputPreferences.watchedKeys)
        assertEquals(0.1f, normalizeSymbolLongPressSeconds(0f), 0f)
        assertEquals(1f, normalizeSymbolLongPressSeconds(5f), 0f)
        assertEquals(0.3f, normalizeSymbolLongPressSeconds(Float.NaN), 0f)
        assertEquals(0.25f, normalizeSymbolLongPressSeconds(0.26f), 0.001f)
        assertEquals(0L, KeyboardInputPreferences(symbolLongPressSeconds = 0.15f).glideHoldMs)
        assertEquals(0L, KeyboardInputPreferences(symbolInputMode = SymbolInputMode.LONG_PRESS,
            symbolLongPressSeconds = 0.15f).glideHoldMs)
    }
}
