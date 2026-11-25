package com.cayboard.ui.settings

import com.cayboard.keyboard.ToolbarButton
import com.cayboard.keyboard.ToolbarButtonItem

/**
 * 键盘菜单长按打开的设置项，写设置搜索索引里的标题：跳转、定位和高亮与搜索点开同一条路。
 * 纯动作（编辑）、临时状态（全角/半角）、设置入口本身，
 * 以及表情、符号面板没有可设的东西，不在这里，长按不响应。
 */
internal val MenuSettingsTitles: Map<ToolbarButton, String> = mapOf(
    ToolbarButton.SCHEMA to "语言与方案",
    ToolbarButton.CLIPBOARD to "剪贴板",
    ToolbarButton.FLOAT to "键盘尺寸与位置",
    ToolbarButton.FLOATING to "键盘尺寸与位置",
    ToolbarButton.ARRANGE to "工具栏与菜单排列",
    ToolbarButton.HANDWRITING_LOOKUP to "手写",
    ToolbarButton.VOICE to "语音转文本",
    ToolbarButton.TRANSLATE to "翻译",
    ToolbarButton.TRADITIONAL to "繁体输出",
    ToolbarButton.FOURTEEN_SWIPE to "左右滑动选字母",
)

/** [item] 长按要去的设置。没有对应设置时为 null。 */
internal fun menuSettingsEntry(item: ToolbarButtonItem): SettingsSearchEntry? = when (item) {
    is ToolbarButtonItem.Builtin ->
        MenuSettingsTitles[item.button]?.let { label -> SettingsSearchIndex.firstOrNull { it.title == label } }
}

