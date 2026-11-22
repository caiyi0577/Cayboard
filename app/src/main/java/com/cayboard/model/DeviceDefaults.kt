package com.cayboard.model

import android.content.Context
import com.cayboard.keyboard.ToolbarButton
import com.cayboard.runtime.adaptation.DeviceCapabilities
import com.cayboard.runtime.adaptation.DeviceCapabilityProbe
import com.cayboard.settings.SettingsPreferences
import com.cayboard.speech.SpeechModelCatalog

/** A conservative first-run recommendation, not a benchmark or an ongoing override. */
object DeviceDefaults {
    internal const val VERSION = "device_model_defaults_version"
    internal const val TIER = "device_model_defaults_tier"
    private const val VOICE = "device_default_voice_model"

    fun initialize(context: Context) = initialize(context) { DeviceCapabilityProbe.read(context) }

    /** Must run before other first-run preference writers. One atomic defaults snapshot. */
    @Synchronized internal fun initialize(context: Context, probe: () -> DeviceCapabilities) {
        val prefs = SettingsPreferences.getPrefsPublic(context)
        if (prefs.contains(VERSION)) {
            return
        }
        val fresh = prefs.all.isEmpty() && listOf("asr_model", "default_models", "bundled_models").all {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).all.isEmpty()
        }
        val editor = prefs.edit()
        if (fresh) {
            val hardware = probe()
            val profile = DeviceModelProfiles.choose(hardware,
                if (com.cayboard.BuildConfig.BUNDLED_MODELS) BundledModelInstaller.bundledIds(context) else emptySet())
            editor.putString(TIER, profile.tier.name)
                .putString(VOICE, profile.voiceModel)
                // 新装默认连续联想（用户 11-08）；老用户没存过这项时仍按单次读，不跟着变。
                .putBoolean(SettingsPreferences.KEY_ASSOCIATION_SINGLE_MODE, false)
                .putLong("device_initial_memory_bytes", hardware.totalMemoryBytes ?: -1)
                .putInt("device_initial_cores", hardware.processors ?: -1)
                .putLong("device_initial_max_cpu_khz", hardware.maxCpuKHz ?: -1)
                .putString("device_initial_low_ram", hardware.lowRam?.toString() ?: "unknown")
                .putInt("device_initial_sdk", hardware.sdk)
                .putString("device_initial_abis", hardware.abis.joinToString(","))
        } else {
            // Missing keys on an existing installation do not prove the user wanted new defaults.
            editor.putString(TIER, "PRESERVED")
        }
        check(editor.putInt(VERSION, 1).commit()) { "无法保存设备模型默认配置" }
        if (fresh && com.cayboard.BuildConfig.ELDER) applyElderDefaults(context)
    }

    /**
     * The elder package (BuildConfig.ELDER, docs/development/elder-pack-spec.md). Fresh
     * installs only; existing settings are never changed:
     * - 九键拼音 as the keyboard, with 手写 one tap away on the toolbar;
     * - toolbar 语音、手写、拍照识别、表情、剪贴板、输入方式 (no cursor editing tools);
     * - larger candidates (24 sp) and a taller keyboard (40 % of the long screen side);
     * - the high-contrast 黑白 theme, which is already the default.
     */
    private fun applyElderDefaults(context: Context) {
        SettingsPreferences.setToolbarButtons(context,
            listOf(ToolbarButton.VOICE, ToolbarButton.HANDWRITING_LOOKUP, ToolbarButton.OCR,
                ToolbarButton.EMOJI, ToolbarButton.CLIPBOARD, ToolbarButton.SCHEMA).map { it.id })
        SettingsPreferences.setCurrentSchema(context, "t9_pinyin")
        SettingsPreferences.setCandidateTextSize(context, ELDER_CANDIDATE_SP)
        val config = context.resources.configuration
        SettingsPreferences.setKeyboardHeightDp(context, maxOf(config.screenWidthDp, config.screenHeightDp) * ELDER_HEIGHT_PERCENT / 100)
        SettingsPreferences.setKeyboardTheme(context, com.cayboard.ui.theme.FixedKeyboardStyle.ID)

    }

    private const val ELDER_CANDIDATE_SP = 24
    private const val ELDER_HEIGHT_PERCENT = 40

    fun voiceModel(context: Context): String = com.cayboard.speech.SpeechModelCatalog.XASR

    fun supportsRefinement(lowRam: Boolean, memoryBytes: Long, cores: Int): Boolean =
        !lowRam && memoryBytes >= 6L * 1024 * 1024 * 1024 && cores >= 4

    fun supportsRefinement(context: Context): Boolean {
        return DeviceModelProfiles.choose(DeviceCapabilityProbe.read(context)).tier == DeviceModelTier.ENHANCED
    }
}

