package com.cayboard.keyboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Assignment
import androidx.compose.material.icons.twotone.EmojiEmotions
import androidx.compose.material.icons.twotone.KeyboardAlt
import androidx.compose.material.icons.twotone.LastPage
import androidx.compose.material.icons.twotone.Paid
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.twotone.OpenWith
import androidx.compose.material.icons.twotone.Mic
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.VolumeUp
import androidx.compose.material.icons.twotone.Vibration
import androidx.compose.material.icons.twotone.PictureInPictureAlt
import androidx.compose.material.icons.twotone.VisibilityOff
import androidx.compose.material.icons.twotone.WidthFull
import androidx.compose.material.icons.twotone.Padding
import androidx.compose.material.icons.twotone.Explore
import androidx.compose.material.icons.twotone.SwipeRight
import androidx.compose.material.icons.twotone.Translate
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolbarButton(
    val id: String,
    val label: String,
    val icon: ImageVector
) {
    EMOJI("emoji", "表情", Icons.TwoTone.EmojiEmotions),
    CLIPBOARD("clipboard", "剪贴板", Icons.AutoMirrored.TwoTone.Assignment),
    TRANSLATE("translate", "翻译", Icons.TwoTone.Translate),
    OCR("ocr", "拍照识别", Icons.Default.DocumentScanner),
    SCHEMA("schema", "输入方式", Icons.TwoTone.KeyboardAlt),
    SYMBOL("symbol", "符号", Icons.TwoTone.Paid),
    // 保留已保存的旧 ID，悬浮模式统一在键盘调节中设置。
    FLOAT("float", "键盘调节", Icons.TwoTone.Tune),
    HANDWRITING_LOOKUP("handwriting_lookup", "手写", Icons.Outlined.Gesture),
    EDIT("edit", "编辑", Icons.TwoTone.OpenWith),
    VOICE("voice", "语音", Icons.TwoTone.Mic),
    // 以下原为菜单专属；工具栏与菜单共用一组入口，可放在任一处或不使用。
    SETTINGS("settings", "设置", Icons.TwoTone.Settings),
    SOUND("sound", "按键声音", Icons.TwoTone.VolumeUp),
    VIBRATION("vibration", "按键振动", Icons.TwoTone.Vibration),
    FLOATING("floating", "悬浮", Icons.TwoTone.PictureInPictureAlt),
    // 图标随状态切换（半角月牙 / 全角满圆），见 WidthShapeIcons.kt。
    // 名称用于自定义列表；键盘上按当前状态显示“全角”或“半角”（见 [stateLabel]）。
    FULL_SHAPE("full_shape", "全角/半角", HalfWidthIcon),
    // 切换当前方案的输出字形；注音使用独立的简体设置。
    // 键盘上按当前状态显示“繁体”或“简体”（见 [stateLabel]）；图标是「简」或「繁」字（ToolbarButtonIcon），这里的图标不显示。
    TRADITIONAL("traditional", "简繁", Icons.TwoTone.Translate),
    // 布局手势开关：只在对应布局里出现（见 LayoutGestureMenuItems.kt），其他布局自动隐藏。
    GLIDE("glide", "滑行输入", Icons.Outlined.Gesture),
    FOURTEEN_SWIPE("fourteen_swipe", "14键滑选", Icons.TwoTone.SwipeRight),
    ARRANGE("arrange", "排列", Icons.TwoTone.Padding);

    companion object {
        // 仅在用户还没有保存工具栏偏好时使用，保留已有选择及顺序。
        val DEFAULT_VISIBLE = linkedSetOf(EMOJI, CLIPBOARD, TRANSLATE, VOICE)

        val MENU_FIRST_ROW = listOf(SCHEMA, SYMBOL, HANDWRITING_LOOKUP, EDIT)
        val DEFAULT_MENU = MENU_FIRST_ROW + listOf(SETTINGS, FLOAT, OCR, TRANSLATE, SOUND, VIBRATION, FULL_SHAPE, ARRANGE)
        /** 布局手势开关：默认排在菜单的“排列”之前（见 QuickActionLayout.defaultMenu），每个布局只显示适用的一两个。 */
        val LAYOUT_GESTURES = listOf(GLIDE, FOURTEEN_SWIPE)
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
