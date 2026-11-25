package com.cayboard.ui.settings
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.cayboard.ui.menubar.CLIPBOARD_CLOSE_AFTER_PASTE_KEY
import com.cayboard.ui.menubar.clipboardClosesAfterPaste

@Composable
internal fun ClipboardPasteSettings() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("clipboard_board", Context.MODE_PRIVATE) }
    var closeAfterPaste by remember { mutableStateOf(clipboardClosesAfterPaste(prefs)) }
    SettingsSection(title = "粘贴", content = {
        SettingsToggleItem(
            icon = lucideIcon(LucideSettingIcons.CLIPBOARD),
            title = "上屏后自动关闭剪贴板",
            subtitle = "关掉后留在剪贴板里，可以连续贴多条",
            checked = closeAfterPaste,
            onCheckedChange = {
                closeAfterPaste = it
                prefs.edit().putBoolean(CLIPBOARD_CLOSE_AFTER_PASTE_KEY, it).apply()
            },
        )
    })
}

