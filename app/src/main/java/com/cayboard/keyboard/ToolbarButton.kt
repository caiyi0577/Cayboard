package com.cayboard.keyboard

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Padding
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Translate

enum class ToolbarButton(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    EMOJI("emoji", "表情", Icons.Filled.EmojiEmotions),
    CLIPBOARD("clipboard", "剪贴板", Icons.AutoMirrored.Filled.Assignment),
    TRANSLATE("translate", "翻译", Icons.Filled.Translate),
    SCHEMA("schema", "输入方式", Icons.Filled.KeyboardAlt),
    SYMBOL("symbol", "符号", Icons.Filled.Paid),
    // 保留已保存的旧 ID，悬浮模式统一在键盘调节中设置。
    FLOAT("float", "键盘调节", Icons.Filled.Tune),
    HANDWRITING_LOOKUP("handwriting_lookup", "手写", Icons.Filled.Gesture),
    EDIT("edit", "编辑", Icons.Filled.OpenWith),
    VOICE("voice", "语音", Icons.Filled.Mic),
    // 以下原为菜单专属；工具栏与菜单共用一组入口，可放在任一处或不使用。
    SETTINGS("settings", "设置", Icons.Filled.Settings),
    SOUND("sound", "按键声音", Icons.Filled.VolumeUp),
    VIBRATION("vibration", "按键振动", Icons.Filled.Vibration),
    FLOATING("floating", "悬浮", Icons.Filled.PictureInPictureAlt),
    // 图标随状态切换（半角月牙 / 全角满圆），见 WidthShapeIcons.kt。
    // 名称用于自定义列表；键盘上按当前状态显示“全角”或“半角”（见 [stateLabel]）。
    FULL_SHAPE("full_shape", "全角/半角", HalfWidthIcon),
    // 切换当前方案的输出字形；注音使用独立的简体设置。
    // 键盘上按当前状态显示“繁体”或“简体”（见 [stateLabel]）；图标是「简」或「繁」字（ToolbarButtonIcon），这里的图标不显示。
    TRADITIONAL("traditional", "简繁", Icons.Filled.Translate),
    FOURTEEN_SWIPE("fourteen_swipe", "14键滑选", Icons.Filled.SwipeRight),
    ARRANGE("arrange", "排列", Icons.Filled.Padding);

    companion object {
        // 仅在用户还没有保存工具栏偏好时使用，保留已有选择及顺序。
        val DEFAULT_VISIBLE = linkedSetOf(EMOJI, CLIPBOARD, TRANSLATE, VOICE)

        val MENU_FIRST_ROW = listOf(SCHEMA, SYMBOL, HANDWRITING_LOOKUP, EDIT)
        val DEFAULT_MENU = MENU_FIRST_ROW + listOf(SETTINGS, FLOAT, TRANSLATE, SOUND, VIBRATION, FULL_SHAPE, ARRANGE)
        /** 布局手势开关：默认排在菜单的“排列”之前（见 QuickActionLayout.defaultMenu），每个布局只显示适用的一两个。 */
        val LAYOUT_GESTURES = listOf(FOURTEEN_SWIPE)
        /** 开关类入口：菜单中显示开/关状态，工具栏中开启时高亮。 */
        val TOGGLES = setOf(SOUND, VIBRATION, FLOATING, FULL_SHAPE, TRADITIONAL) + LAYOUT_GESTURES

        val CUSTOMIZABLE = entries.toList()

        /** 已移除的旧版“固定内容”（quick_phrase）并入剪贴板。 */
        private const val LEGACY_QUICK_PHRASE = "quick_phrase"

        /**
         * 已移除的功能按钮白名单过滤与映射：
         * - 全选、复制、粘贴、段首、段尾 → 编辑
         * - 翻译旧 ID → 翻译新 ID
         * - 玻璃、T9划动、划盘、无痕 → 过滤移除
         */
        private val LEGACY_EDITING = setOf("select_all", "copy", "paste", "home", "end")

        fun normalizeIds(ids: List<String>): List<String> =
            ids.map { when (it) { "com.kingzcheung.xime.plugin.translate:open" -> TRANSLATE.id; LEGACY_QUICK_PHRASE -> CLIPBOARD.id; in LEGACY_EDITING -> EDIT.id; else -> it } }.filter { fromId(it) != null }.distinct()

        fun fromId(id: String): ToolbarButton? =
            entries.find { it.id == id }
    }
}

data class ToolbarAction(
    val item: ToolbarButtonItem,
    val active: Boolean = false,
    /** 菜单里长按：打开这一项在设置里的对应页；没有对应设置时为 null。 */
    val onLongClick: (() -> Unit)? = null,
    /** 工具栏上长按弹出、滑到一项松手就选它的列表（如「翻译」选目标语言）；null 时没有。 */
    val pressMenu: ToolbarPressMenu? = null,
    val onClick: () -> Unit
)

/** 长按列表：[load] 在长按时才取选项，[onChoose] 收到选中的那一项。 */
class ToolbarPressMenu(
    val load: suspend () -> PressMenuOptions?,
    val onChoose: (key: String, option: String) -> Unit,
)

/** 一组选项：[key] 是这一项设置的名字，[selected] 是现在选着的。 */
data class PressMenuOptions(val key: String, val title: String, val options: List<String>, val selected: String?)
