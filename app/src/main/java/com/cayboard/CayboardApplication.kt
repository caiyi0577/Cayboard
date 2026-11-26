package com.cayboard

import android.app.Application
import android.util.Log
import com.cayboard.util.FileLogger
import com.cayboard.engine.EngineConfig
import com.cayboard.model.ModelRuntime
import com.cayboard.engine.InputEngine
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.ui.keyboard.AppFonts
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.theme.KeyboardThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CayboardApplication : Application() {

    companion object {
        private const val TAG = "CayboardApplication"
    }
    
    private val applicationScope = CoroutineScope(Dispatchers.IO)
    


    override fun onCreate() {
        super.onCreate()

        FileLogger.fileHeader = com.cayboard.platform.AndroidPlatform.diagnosticHeader(
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        FileLogger.init(this)
        // 语音、联想、文字识别进程也记崩溃：以前只有主进程登记，模型进程闪退时日志里什么都没有。
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                FileLogger.e("CrashHandler", "Uncaught exception on thread: ${thread.name}", throwable)
                FileLogger.flushNow()
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        // Model services run in separate processes. They must not copy/deploy the
        // same input data or load the keyboard runtime a second time.
        val processName = getProcessName()
        if (processName != packageName) return
        com.cayboard.engine.compat.LegacyNameMigration.migrate(this)
        applicationScope.launch {
            runCatching { com.cayboard.ui.keyboard.FixedKeycapAtlas.get(this@CayboardApplication) }
                .onFailure { FileLogger.e(TAG, "Cannot preload fixed keycaps", it) }
        }
        SettingsPreferences.migrateQuickActionMenu(this)
        com.cayboard.settings.FixedAppearancePreferences.migrate(this)
        com.cayboard.ui.keyboard.EmojiAppearance.initialize(this)
        com.cayboard.ui.keyboard.EmojiAppearance.migrateCandidates(this)
        SettingsPreferences.getPrefsPublic(this).edit()
            .remove("t9_flick_letters").remove("flick_direction_disc").remove("flick_direction_disc_v2")
            .remove("show_press_bubble").remove("show_press_bubble_v2").apply()
        // 先于任何下载、导入和词库准备列出上次中断的残留，后台删除。
        com.cayboard.settings.StorageHousekeeping.sweepAtStartup(this, applicationScope)
        com.cayboard.model.DeviceDefaults.initialize(this)
        com.cayboard.settings.LanguagePreferences.initialize(this)
        com.cayboard.ui.theme.VisualStyles.current = SettingsPreferences.getVisualStyle(this)
        com.cayboard.ui.theme.KeyShapes.current = SettingsPreferences.getKeyShape(this)
        com.cayboard.ui.theme.IconAppearance.reload(this)
        try {
            com.cayboard.ui.theme.LauncherIcons.apply(this, com.cayboard.ui.theme.IconAppearance.effective)
        } catch (e: RuntimeException) {
            // A launcher failure must not prevent typing. The explicit settings action reports it too.
            FileLogger.e(TAG, "Unable to synchronize launcher icon", e)
        }
        AppFonts.initialize(this)
        applicationScope.launch { com.cayboard.settings.ClipboardWordSegmenter.prewarm(this@CayboardApplication) }

        // 从 cayboard.yaml 加载配色方案
        KeyboardThemes.initFromConfig(this)

        // 从 cayboard.yaml 的 style 读取默认主题和显示模式
        // color_scheme 可指定 dynamic（Material You 动态配色，仅 Android 12+，
        // 低版本 getThemeById 自动回退到内置配色）
        SettingsPreferences.defaultKeyboardTheme = com.cayboard.ui.theme.FixedKeyboardStyle.ID
        SettingsPreferences.defaultDarkMode = 1
        SettingsPreferences.applyForkThemeDefaults(this)

        // 初始化模型运行时（内存管理 + 生命周期）
        ModelRuntime.attach(this)

        com.cayboard.model.BundledModelInstaller.start(this)
        preInitializeInputEngine()
    }
    
    private fun preInitializeInputEngine() {
        if (InputEngine.isInitialized()) {
            return
        }
        
        applicationScope.launch {
            try {
                EngineConfig.prepareAutomatically(this@CayboardApplication)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-initialize input engine", e)
            }
        }
    }
}

