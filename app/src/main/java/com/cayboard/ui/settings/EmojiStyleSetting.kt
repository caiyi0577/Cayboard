package com.cayboard.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.cayboard.ui.keyboard.EmojiAppearance

@Composable
internal fun EmojiStyleSetting() {
    val context = LocalContext.current
    remember(context) { EmojiAppearance.initialize(context); true }
    var open by remember { mutableStateOf(false) }
    SettingsItem(icon = lucideIcon(LucideSettingIcons.SMILE), title = "Emoji 显示样式", searchKey = "Emoji 显示样式",
        subtitle = "仅改变键盘内的表情外观", status = EmojiAppearance.style.name,
        onClick = { open = true }, showArrow = true)
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Emoji 显示样式") },
        text = {
            Column {
                Text("影响表情面板和候选栏；发送后的样式由接收方应用决定。iOS 未支持的新表情使用 Android 字形补齐。")
                EmojiAppearance.Style.entries.forEach { option ->
                    CompositionLocalProvider(LocalSettingsShowIcons provides false) {
                        SettingsToggleItem(lucideIcon(LucideSettingIcons.SMILE), option.name,
                            if (option == EmojiAppearance.Style.Android) "Google Noto Emoji" else "Apple Emoji",
                            checked = EmojiAppearance.style == option,
                            onCheckedChange = { checked -> if (checked) { EmojiAppearance.setStyle(context, option); open = false } })
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text("完成") } })
}

