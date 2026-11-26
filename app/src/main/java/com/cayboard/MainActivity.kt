package com.cayboard

import android.content.Intent
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.cayboard.engine.InputEngine
import com.cayboard.settings.ImportManager
import com.cayboard.settings.KeysConfigHelper
import com.cayboard.settings.SchemaManager
import com.cayboard.settings.SettingsPreferences
import com.cayboard.ui.settings.SettingsScreen
import com.cayboard.ui.settings.SettingsNavigationRequest
import com.cayboard.ui.settings.SetupWizardScreen
import com.cayboard.ui.theme.CayboardTheme
import com.cayboard.util.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private var settingsNavigationRequest by mutableStateOf<SettingsNavigationRequest?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
        // 键盘菜单「设置」带的是首页（main），回到设置首页；长按跳转带具体页面。没带页面的不动导航。
        intent.getStringExtra("open_fragment")?.let { route ->
            settingsNavigationRequest = SettingsNavigationRequest(
                route, (settingsNavigationRequest?.id ?: 0) + 1,
            )
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
        /** A real permission dialog takes the user longer than this to answer. */
        private const val NO_DIALOG_MS = 500L

    }

    override fun onDestroy() {
        prewarmScope.cancel()
        super.onDestroy()
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Denied at once with no dialog: refused before with "don't ask again" (or twice on Android 11+). The
        // system will never ask again, so the only way left is the app's settings page; the user is sent there.
        val noDialog = !isGranted && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) &&
            android.os.SystemClock.uptimeMillis() - permissionAskedAt < NO_DIALOG_MS
        if (noDialog) {
            PermissionHelper.onSentToAppSettings()
            Toast.makeText(this, "麦克风权限之前被拒绝过，请在设置的「权限」里允许麦克风", Toast.LENGTH_LONG).show()
            try {
                // Its own task: finishAndRemoveTask below must not take the settings page with it.
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) { Log.w(TAG, "App settings page unavailable", e) }
        } else {
            PermissionHelper.onRecordAudioResult(isGranted)
            if (isGranted) {
                Toast.makeText(this, "麦克风权限已授权", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "麦克风权限被拒绝，无法使用语音输入", Toast.LENGTH_SHORT).show()
            }
        }
        finishAndRemoveTask()
    }
    /** When the permission request started; a denial faster than this came without a dialog. */
    private var permissionAskedAt = 0L
    
    private val prewarmScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private fun prewarmKeyboardConfig() {
        if (InputEngine.isInitialized()) return
        prewarmScope.launch {
            try {
                KeysConfigHelper.loadConfig(this@MainActivity)
            } catch (e: Exception) {
                Log.w(TAG, "Keyboard config pre-warm failed, will load on demand", e)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // The Application owns engine preparation, including installation and deployment.
        prewarmKeyboardConfig()

        handleSharedIntent(intent)
        
        val requestPermission = intent?.getStringExtra("request_permission")
        if (requestPermission == PermissionHelper.PERMISSION_RECORD_AUDIO) {
            if (!PermissionHelper.hasRecordAudioPermission(this)) {
                permissionAskedAt = android.os.SystemClock.uptimeMillis()
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                PermissionHelper.onRecordAudioResult(true)
                Toast.makeText(this, "麦克风权限已授权", Toast.LENGTH_SHORT).show()
                finishAndRemoveTask()
            }
            return
        }
        
        val openFragment = intent?.getStringExtra("open_fragment")

        com.cayboard.model.BundledModelInstaller.start(applicationContext)

        setContent {
            val context = this
            val setupCompleted = SettingsPreferences.isSetupCompleted(context)
            var showWizard by remember { mutableStateOf(!setupCompleted) }
            var wizardToSettings by remember { mutableStateOf(false) }
            val darkMode = 1
            var keyboardTheme by remember { mutableStateOf(SettingsPreferences.getKeyboardTheme(context)) }

            // Theme changes can also come from the IME while this activity stays
            // open. Its existing composition must follow the same saved choice.
            DisposableEffect(context) {
                val preferences = SettingsPreferences.getPrefsPublic(context)
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == null || key == "keyboard_theme" || key == "dark_mode") {
                        keyboardTheme = SettingsPreferences.getKeyboardTheme(context)
                    }
                }
                preferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            
            val isDarkTheme = true

            CayboardTheme(darkTheme = isDarkTheme, themeId = keyboardTheme) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    DisposableEffect(isDarkTheme) {
                        val window = (view.context as? ComponentActivity)?.window
                        if (window != null) {
                            val controller = WindowInsetsControllerCompat(window, view)
                            controller.isAppearanceLightStatusBars = !isDarkTheme
                            controller.isAppearanceLightNavigationBars = !isDarkTheme
                        }
                        onDispose { }
                    }
                }

                val density = LocalDensity.current
                val navBarHeight = with(density) {
                    WindowInsets.navigationBars.getBottom(density).toDp()
                }
                val scrimHeight = if (navBarHeight > 0.dp) navBarHeight else 32.dp

                Box(modifier = Modifier.fillMaxSize()) {
                    com.cayboard.ui.settings.SettingsWidthLimit {
                    if (showWizard) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SetupWizardScreen(
                                visible = !wizardToSettings,
                                onNavigateToSchemaSettings = { wizardToSettings = true },
                                onCompleted = { showWizard = false }
                            )
                            if (wizardToSettings) {
                                SettingsScreen(
                                    initialRoute = "schema",
                                    onThemeChanged = {
                                        keyboardTheme = SettingsPreferences.getKeyboardTheme(context)
                                    },
                                    onWizardBack = { wizardToSettings = false }
                                )
                            }
                        }
                    } else {
                        SettingsScreen(
                            initialRoute = openFragment,
                            navigationRequest = settingsNavigationRequest,
                            onThemeChanged = {
                                keyboardTheme = SettingsPreferences.getKeyboardTheme(context)
                            }
                        )
                    }
                    }

                    // 装的不是作者签名的包就提示（每次打开设置一次）。
                    com.cayboard.security.InstallIntegrityWarning()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scrimHeight)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isDarkTheme) {
                                        listOf(Color.Transparent, Color.White.copy(alpha = 0.10f))
                                    } else {
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.10f))
                                    }
                                )
                            )
                    )
                }
            }
        }
    }
    
    private fun handleSharedIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                val mime = intent.type
                if (uri != null) {
                    if (mime?.startsWith("image/") == true) {
                        Toast.makeText(this, "请选择输入方案文件", Toast.LENGTH_SHORT).show()
                    } else {
                        importSchema(uri)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                if (uris != null) {
                    for (uri in uris) {
                        importSchema(uri)
                    }
                }
            }
        }
    }
    
    private fun importSchema(uri: android.net.Uri) {
        prewarmScope.launch {
            when (val result = ImportManager.import(this@MainActivity, uri)) {
                is ImportManager.ImportResult.Content -> {
                    launch(Dispatchers.Main) {
                        // 同名文件确认在「已安装的方案」页；装好的方案由 ImportManager 自动应用（#340）。
                        val msg = if (result.pendingPackageId != null) "${result.message}：到「已安装的方案」页确认"
                            else result.message
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }

                is ImportManager.ImportResult.Failed -> {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "导入失败：${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
                is ImportManager.ImportResult.Unsupported -> {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "不支持的文件类型", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /** 生成可在 cayboard.custom.yaml 中使用的主题配置模板并复制到剪贴板。 */

}
