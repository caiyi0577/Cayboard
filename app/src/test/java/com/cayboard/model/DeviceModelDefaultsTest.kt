package com.cayboard.model

import android.content.Context
import android.content.SharedPreferences
import com.cayboard.runtime.adaptation.DeviceCapabilities
import com.cayboard.settings.SettingsPreferences
import com.cayboard.speech.AsrModelManager
import com.cayboard.speech.SpeechModelCatalog
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.*

class DeviceModelDefaultsTest {
    @get:Rule val files = TemporaryFolder()
    private val gib = DeviceModelProfiles.GIB
    private fun device(ram: Long? = 8 * gib, cores: Int? = 8, frequency: Long? = 2_400_000,
        lowRam: Boolean? = false) = DeviceCapabilities(0, 35, listOf("arm64-v8a"), lowRam, ram, cores, frequency)

    private class Stores {
        val context = mock<Context>()
        val data = mutableMapOf<String, MutableMap<String, Any>>()
        private val preferences = mutableMapOf<String, SharedPreferences>()
        fun values(name: String = "cayboard_settings") = data.getOrPut(name) { mutableMapOf() }
        init {
            whenever(context.getSharedPreferences(any(), any())).thenAnswer { call ->
                val name = call.getArgument<String>(0)
                preferences.getOrPut(name) {
                    val values = values(name)
                    mock<SharedPreferences>().also { prefs ->
                        whenever(prefs.all).thenAnswer { values.toMap() }
                        whenever(prefs.contains(any())).thenAnswer { values.containsKey(it.getArgument<String>(0)) }
                        whenever(prefs.getString(any(), anyOrNull())).thenAnswer { values[it.getArgument<String>(0)] ?: it.getArgument<String?>(1) }
                        whenever(prefs.getStringSet(any(), anyOrNull())).thenAnswer { values[it.getArgument<String>(0)] as? Set<String> ?: it.getArgument<Set<String>?>(1) }
                        whenever(prefs.getBoolean(any(), any())).thenAnswer { values[it.getArgument<String>(0)] ?: it.getArgument<Boolean>(1) }
                        whenever(prefs.getInt(any(), any())).thenAnswer { values[it.getArgument<String>(0)] ?: it.getArgument<Int>(1) }
                        whenever(prefs.getLong(any(), any())).thenAnswer { values[it.getArgument<String>(0)] ?: it.getArgument<Long>(1) }
                        whenever(prefs.edit()).thenAnswer {
                            val pending = mutableMapOf<String, Any?>()
                            val editor = mock<SharedPreferences.Editor>()
                            fun change(call: org.mockito.invocation.InvocationOnMock): SharedPreferences.Editor {
                                pending[call.getArgument(0)] = call.getArgument(1)
                                return editor
                            }
                            whenever(editor.putString(any(), anyOrNull())).thenAnswer(::change)
                            whenever(editor.putBoolean(any(), any())).thenAnswer(::change)
                            whenever(editor.putInt(any(), any())).thenAnswer(::change)
                            whenever(editor.putLong(any(), any())).thenAnswer(::change)
                            whenever(editor.remove(any())).thenAnswer { pending[it.getArgument(0)] = null; editor }
                            fun save() { pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value } }
                            whenever(editor.commit()).thenAnswer { save(); true }
                            doAnswer { save(); null }.whenever(editor).apply()
                            editor
                        }
                    }
                }
            }
        }
    }

    @Test fun automaticVoiceDefaultsMigrateButExplicitSelectionsRemain() {
        for (old in listOf(SpeechModelCatalog.ZIPFORMER, SpeechModelCatalog.ZIPFORMER_TWO_PASS, SpeechModelCatalog.TWO_PASS)) {
            val stores = Stores()
            stores.values()["device_default_voice_model"] = old
            stores.values()[DeviceDefaults.VERSION] = 1
            assertEquals(SpeechModelCatalog.PARAFORMER, AsrModelManager(stores.context).getSelectedModelId())
            AsrModelManager(stores.context).setModel(old)
            DeviceDefaults.initialize(stores.context) { error("must not re-probe") }
            assertEquals(old, AsrModelManager(stores.context).getSelectedModelId())
        }
    }

    @Test fun memoryAndFrequencyBoundariesSelectConcreteModels() {
        val cases = listOf(
            device(4 * gib - 1) to DeviceModelTier.LIGHT,
            device(4 * gib, frequency = 1_800_000) to DeviceModelTier.STANDARD,
            device(6 * gib - 1) to DeviceModelTier.STANDARD,
            device(6 * gib, cores = 4, frequency = 2_200_000) to DeviceModelTier.ENHANCED,
            device(frequency = 2_199_999) to DeviceModelTier.STANDARD,
            device(frequency = 1_799_999) to DeviceModelTier.LIGHT,
            device(cores = 3) to DeviceModelTier.LIGHT,
            device(lowRam = true) to DeviceModelTier.LIGHT,
        )
        cases.forEach { (hardware, tier) ->
            val profile = DeviceModelProfiles.choose(hardware)
            assertEquals(hardware.toString(), tier, profile.tier)
            assertEquals(SpeechModelCatalog.SENSEVOICE, profile.voiceModel)
        }
    }

    @Test fun aMeasuredCpuReplacesTheClockForModelTiers() {
        // 测试用的分档线，不是实测值。
        val lines = com.cayboard.runtime.adaptation.PerformanceTiers.CpuThresholds(
            20_000_000, 25_000_000, 60_000_000, 70_000_000)
        fun cpu(single: Long, dual: Long) = com.cayboard.runtime.adaptation.CpuMeasurement(single, dual)
        assertEquals(DeviceModelTier.LIGHT, DeviceModelProfiles.choose(device().copy(cpu = cpu(85_160_000, 91_000_000))).tier)
        assertEquals(DeviceModelTier.ENHANCED, DeviceModelProfiles.choose(device().copy(cpu = cpu(16_520_000, 23_060_000))).tier)
        // 2.4 GHz、8 GB 但实测慢：不再推荐边说边出字那一套。
        assertEquals(DeviceModelTier.LIGHT, DeviceModelProfiles.choose(device().copy(cpu = cpu(80_000_000, 90_000_000)),
            thresholds = lines).tier)
        assertEquals(DeviceModelTier.STANDARD, DeviceModelProfiles.choose(device().copy(cpu = cpu(40_000_000, 45_000_000)),
            thresholds = lines).tier)
        // 主频读数低但实测快。
        assertEquals(DeviceModelTier.ENHANCED, DeviceModelProfiles.choose(device(frequency = 1_500_000)
            .copy(cpu = cpu(15_000_000, 18_000_000)), thresholds = lines).tier)
        // 分档线没定时测了也按原来的主频估计。
        assertEquals(DeviceModelTier.ENHANCED, DeviceModelProfiles.choose(device().copy(cpu = cpu(80_000_000, 90_000_000)),
            thresholds = null).tier)
    }

    @Test fun onlyEditionsThatBundleParaformerStartOnIt() {
        val voice = { bundled: Set<String> -> DeviceModelProfiles.choose(device(), bundled).voiceModel }
        assertEquals(SpeechModelCatalog.SENSEVOICE, voice(emptySet()))
        assertEquals(SpeechModelCatalog.PARAFORMER, voice(setOf(SpeechModelCatalog.PARAFORMER, SpeechModelCatalog.SENSEVOICE)))
        assertEquals(SpeechModelCatalog.SENSEVOICE, voice(setOf("ochwpro", SpeechModelCatalog.SENSEVOICE)))
    }

    @Test fun senseVoiceDefaultNeedsNoDownloadAndGainsTheStreamingPreviewOnceParaformerIsThere() {
        val stores = Stores()
        val context = stores.context
        whenever(context.filesDir).thenReturn(files.root)
        stores.values()["device_default_voice_model"] = SpeechModelCatalog.SENSEVOICE
        stores.values()[DeviceDefaults.VERSION] = 1
        fun install(id: String, vararg names: String) = java.io.File(files.root, "models/$id").run {
            mkdirs(); names.forEach { java.io.File(this, it).writeText("x") }
        }
        install(SpeechModelCatalog.SENSEVOICE, "model.int8.onnx", "tokens.txt")
        val manager = AsrModelManager(context)
        assertEquals(SpeechModelCatalog.SENSEVOICE, manager.getSelectedModelId(com.cayboard.settings.InputLanguage.CHINESE))
        install(SpeechModelCatalog.PARAFORMER, "decoder.int8.onnx", "encoder.int8.onnx", "tokens.txt")
        assertEquals(SpeechModelCatalog.TWO_PASS, manager.getSelectedModelId(com.cayboard.settings.InputLanguage.CHINESE))
        assertEquals(SpeechModelCatalog.TWO_PASS, manager.getSelectedModelId(com.cayboard.settings.InputLanguage.ENGLISH))
        assertThrows(IllegalArgumentException::class.java) {
            manager.getSelectedModelId(com.cayboard.settings.InputLanguage.JAPANESE)
        }
        // An explicit Chinese choice is never upgraded.
        manager.setModel(SpeechModelCatalog.SENSEVOICE, com.cayboard.settings.InputLanguage.CHINESE)
        assertEquals(SpeechModelCatalog.SENSEVOICE, manager.getSelectedModelId(com.cayboard.settings.InputLanguage.CHINESE))
    }

    @Test fun legacyJapaneseChoiceDoesNotTurnTheChineseDefaultIntoZipformer() {
        val stores = Stores()
        whenever(stores.context.filesDir).thenReturn(files.root)
        stores.values("asr_model")["selected_model"] = SpeechModelCatalog.ZIPFORMER_JA
        assertEquals(SpeechModelCatalog.PARAFORMER,
            AsrModelManager(stores.context).getSelectedModelId(com.cayboard.settings.InputLanguage.CHINESE))
        assertEquals(SpeechModelCatalog.ZIPFORMER_JA, AsrModelManager(stores.context).getSelectedModelId())
        assertFalse(stores.values("asr_model").containsKey("selected_model_zh"))
    }

    @Test fun missingFrequencyFallsBackToMemoryAndCoresButMissingEssentialsStayConservative() {
        assertEquals(DeviceModelTier.ENHANCED, DeviceModelProfiles.choose(device(frequency = null)).tier)
        assertEquals(DeviceModelTier.STANDARD, DeviceModelProfiles.choose(device(4 * gib, frequency = null)).tier)
        listOf(device(ram = null), device(ram = 0), device(cores = null), device(cores = 0), device(lowRam = null))
            .forEach { assertEquals(DeviceModelTier.LIGHT, DeviceModelProfiles.choose(it).tier) }
    }

    

    @Test fun laterLaunchDoesNotProbeAgainOrOverrideManualChoices() {
        val stores = Stores()
        val context = stores.context
        DeviceDefaults.initialize(context) { device() }
        AsrModelManager(context).setModel(SpeechModelCatalog.SENSEVOICE)
        DeviceDefaults.initialize(context) { error("must not re-probe") }
        assertEquals(SpeechModelCatalog.SENSEVOICE, AsrModelManager(context).getSelectedModelId())
    }

    

    @Test fun legacySettingsIncludingValuesEqualToOldDefaultsArePreserved() {
        listOf("cayboard_settings", "asr_model", "default_models", "bundled_models").forEach { existingStore ->
            val stores = Stores()
            stores.values(existingStore)["legacy_marker"] = true
            DeviceDefaults.initialize(stores.context) { error("existing installation must not re-probe") }
            assertEquals("PRESERVED", stores.values()[DeviceDefaults.TIER])
            // 老用户没碰过联想方式：仍是单次，不被新装默认改成连续。
            assertTrue(SettingsPreferences.isSingleAssociationMode(stores.context))
            assertEquals(SpeechModelCatalog.PARAFORMER, AsrModelManager(stores.context).getSelectedModelId())
        }
        val stores = Stores()
        AsrModelManager(stores.context).setModel(SpeechModelCatalog.ZIPFORMER_TWO_PASS)
        SettingsPreferences.setSmartPredictionEnabled(stores.context, false)
        DeviceDefaults.initialize(stores.context) { error("existing choices") }
        assertEquals(SpeechModelCatalog.ZIPFORMER_TWO_PASS, AsrModelManager(stores.context).getSelectedModelId())
        assertFalse(SettingsPreferences.isSmartPredictionEnabled(stores.context))
    }

}
