package com.cayboard.ui.keyboard

import com.cayboard.keyboard.GestureAction
import com.cayboard.engine.EngineComposition
import com.cayboard.engine.EngineProcessResult
import com.cayboard.viewmodel.SchemaSwitchUiState

data class KeyboardCallbacks(
    val onKeyPress: (String, Boolean) -> Unit,
    val onLetterNeighbors: ((String, String) -> Unit)? = null,
    val inputAdmissionTicket: () -> Long? = { 0L },
    val captureInputContext: () -> kotlin.coroutines.CoroutineContext = { kotlin.coroutines.EmptyCoroutineContext },
    /** 九键组句的上文：组合为空时光标前的已上屏文字；null 表示未知或正在组合。 */
    val t9LeftContext: () -> String? = { null },
    val rerankT9Candidates: (EngineProcessResult) -> EngineProcessResult = { it },
    val inputCommands: com.cayboard.engine.InputCommandQueue? = null,
    val onT9UnconsumedDelete: (suspend (com.cayboard.keyboard.RepeatInput?) -> Unit)? = null,
    val onKeyPressDown: ((String) -> Unit)? = null,
    val onKeyRelease: ((String) -> Unit)? = null,
    val onCandidateSelect: (Int) -> Unit,
    val onNextCandidate: (com.cayboard.service.CandidateState) -> Unit = {},
    val onPreviousCandidate: (com.cayboard.service.CandidateState) -> Unit = {},
    val onRetryRejectedCommit: () -> Unit = {},
    val onCancelRejectedCommit: () -> Unit = {},
    /** Validate the rendered list again when a tap or delayed deletion is delivered. */
    val isCandidateSnapshotCurrent: (com.cayboard.service.CandidateState) -> Boolean = { true },
    val onOpenPreeditEditor: (((com.cayboard.engine.PinyinEditSession?) -> Unit) -> Unit)? = null,
    var onT9RefreshAfterPreeditEdit: (() -> Unit)? = null,
    /** Installed by the visible editor; the IME service consumes Back before hiding. */
    var onDismissPreeditEditor: (() -> Unit)? = null,
    var onPreeditKeyInput: ((String) -> Boolean)? = null,
    /** Touch cursor gestures can enter preedit editing; physical arrows never call this. */
    var onPreeditCursorMove: ((String) -> Boolean)? = null,
    val onLivePreeditEdit: ((com.cayboard.engine.PinyinEditSession, String?, Int?, String?, (com.cayboard.engine.PinyinEditSession?) -> Unit) -> Unit)? = null,
    /**
     * 候选展开页点选（本地分页）：globalIndex 为跨页全量候选列表中的索引，
     * 服务层走 select_candidate 全局索引（区别于候选栏的当前页内索引）。
     */
    val onGlobalCandidateSelect: ((Int) -> Unit)? = null,
    /** 用户展开候选页时请求服务层拉取跨页全量候选（本地分页数据源）。 */
    val onRequestExpandedCandidates: (() -> Unit)? = null,
    /** 收起的候选栏滑到最后几个：服务层把展开列表接到候选栏后面。 */
    val onExtendCandidateStrip: (() -> Unit)? = null,
    /** 展开页打开了「单字」：服务层在已读候选里单字不够时接着往后读。 */
    val onSingleCharFilterOn: (() -> Unit)? = null,
    val onRequestPinyinSyllables: ((Long, (com.cayboard.engine.PinyinSyllableChoices?) -> Unit) -> Unit)? = null,
    val onSelectPinyinSyllable: ((com.cayboard.engine.PinyinSyllableChoices, Int,
        (com.cayboard.engine.PinyinSyllableChoices?) -> Unit) -> Unit)? = null,
    /** Takes back the last chosen syllable; the second lambda runs when nothing was taken back. */
    val onUndoPinyinSyllable: ((com.cayboard.engine.PinyinSyllableChoices, () -> Unit,
        (com.cayboard.engine.PinyinSyllableChoices?) -> Unit) -> Unit)? = null,
    val onAssociationSelect: ((Int) -> Unit)? = null,
    val onClearAssociation: (() -> Unit)? = null,
    val onClipboard: (() -> Unit)? = null,
    val onDismissClipboardPreview: (() -> Unit)? = null,
    val onClipboardSelect: ((String) -> Unit)? = null,
    /** A clipboard record (text or picture) was put into the text from the clipboard board. */
    val onClipboardRecordCommitted: (() -> Unit)? = null,
    val onCommitText: ((String) -> Unit)? = null,
    val onDeleteText: ((Int) -> Unit)? = null,
    val onKeyboardResize: (() -> Unit)? = null,
    val onReloadConfig: (() -> Unit)? = null,
    val onSettings: (() -> Unit)? = null,
    val onSettingsPage: ((String) -> Unit)? = null,
    val onSwitchSchema: ((String) -> Unit)? = null,
    /** 在「布局」一排点了布局：和 [onSwitchSchema] 一样换方案，但开着手写时回到键盘，不再重开手写。 */
    val onSwitchSchemaForLayout: ((String) -> Unit)? = null,
    val onHandwritingExpand: ((Boolean) -> Unit)? = null,
    val onHandwritingToggle: (() -> Unit)? = null,
    val onToggleSchemaSwitch: ((SchemaSwitchUiState) -> Unit)? = null,
    val onHideKeyboard: (() -> Unit)? = null,
    val onSwitchKeyboard: (() -> Unit)? = null,
    val onToolbarEditingAction: ((String) -> Unit)? = null,
    val onVoiceModeChange: ((Boolean) -> Unit)? = null,
    val onVoiceStickyToggle: (() -> Unit)? = null,
    val onVoiceHoldEnd: (() -> Unit)? = null,
    val onVoiceHoldCancel: (() -> Unit)? = null,
    val onPageDown: (() -> Unit)? = null,
    val onPageUp: (() -> Unit)? = null,
    val onCursorMove: ((Int) -> Unit)? = null,
    val onCursorMoveVertical: ((Int) -> Unit)? = null,
    val onGestureAction: ((GestureAction, String) -> Unit)? = null,
    val onUpdateToolbarButtons: ((List<String>) -> Unit)? = null,
    /** 工具栏与菜单同时更新：排列编辑器跨区拖动后一次保存。 */
    val onUpdateQuickActions: ((com.cayboard.keyboard.QuickActionLayout) -> Unit)? = null,
    /**
     * 插件工具栏按钮点击（manifest 声明的 action，如 open_panel）触发。
     * 宿主据此打开对应插件的通用面板。
     */
    val onOpenTranslation: (() -> Unit)? = null,
    /**
     * 通用工具面板关闭。
     */
    val onToolPanelClose: (() -> Unit)? = null,
    /**
     * 通用工具面板候选条目点击（AI 生成结果上屏，宿主处理选区替换/追加）。
     */
    /**
     * passive 纯展示面板的 action 节点点击（InfoPanel）：通知插件 actionId 并刷新面板数据。
     */
    val onToolPanelAction: ((actionId: String) -> Unit)? = null,
    /** 工具栏长按插件按钮：取插件给的长按列表（翻译的目标语言）；没有就是 null。 */
    val onToolPanelQuickOptions: (suspend () -> com.cayboard.keyboard.PressMenuOptions?)? = null,
    /** 长按列表里选了一项：翻译栏开着就换，没开就打开再换。 */
    val onToolPanelQuickSelect: ((key: String, option: String) -> Unit)? = null,
    /**
     * 通用工具面板输入框焦点变化。
     */
    val onToolPanelFocusChange: ((Boolean) -> Unit)? = null,
    /** 翻译栏（display: live）里的字变了。 */
    val onToolPanelTextChange: ((String) -> Unit)? = null,
    /** Clipboard search bar: open (from the clipboard board), close, submit to the board, paste a result. */
    val onClipboardSearchOpen: (() -> Unit)? = null,
    val onClipboardSearchClose: (() -> Unit)? = null,
    val onClipboardSearchSubmit: (() -> Unit)? = null,
    val onClipboardSearchSelect: ((String) -> Unit)? = null,
    /** 剪贴板长按「翻译」：用 AI 改写翻这条记录；[clipboardTranslateAvailable] 为 false 时菜单里没有这一项。 */
    val onClipboardTranslate: ((String) -> Unit)? = null,
    val onKeyboardModeChange: ((Boolean) -> Unit)? = null,
    val onDismissDeploying: (() -> Unit)? = null,
    val onFloatingModeChange: ((Boolean) -> Unit)? = null,
    val onFloatingKeyboardDrag: ((dx: Float, dy: Float) -> Unit)? = null,
    val onFloatingKeyboardDragEnd: (() -> Unit)? = null,
    /** 调节界面确认：保存新的悬浮旋转角（度）。位置随调节结果一起保存。 */
    val onFloatingKeyboardRotate: ((degrees: Float) -> Unit)? = null,
    /** 普通悬浮态拖四角角标松手：保存新的悬浮宽高与位置（不经过调节界面）。 */
    val onFloatingKeyboardResize: ((widthDp: Int, heightDp: Int, offsetXDp: Int, bottomOffsetDp: Int) -> Unit)? = null,
    val onT9ReplaceFullPinyin: ((String) -> Unit)? = null,
    /**
     * 回退最近一次 T9 半提交：清除累积的半提交文本（及输入框中已上屏的文字）。
     * @param count 需回删的文本长度（由 C++ T9UndoManager 计算并消费）
     */
    val onT9RightCommitUndone: ((Int) -> Unit)? = null,
    /**
     * 右侧候选词即将被引擎选中前同步通知 T9 控制器。
     * 返回 true 表示控制器判断输入序列已被该候选词完整消费。
     * @param pinyin 引擎候选词注释
     * @param text 候选词文本（可空），用于 C++ (comment, text) 双条件精确定位
     * @param textLength 候选词文字长度（汉字数），0 表示未知
     */
    var onT9RightCandidateWillBeSelected: (suspend (String?, String?, Int, Long) -> Boolean?)? = null,
    /**
     * T9 键盘切换离开（至数字/英文键盘）时调用。
     * 服务层负责提交首位候选词并清理 T9 状态。
     */
    val onT9SwitchAway: (() -> Unit)? = null,
    /**
     * 强制 T9 控制器重新发送当前 inputBuffer 到输入引擎。
     * 用于右侧候选 partial commit 后，引擎组合态被清除需要重新构建。
     */
    var onT9ResendInput: (suspend () -> Unit)? = null,
    /** 与普通九键触摸共用 FIFO，字面提交也等待之前的输入。 */
    var onT9RunLiteralInput: ((suspend () -> Unit) -> Unit)? = null,
    /** 引擎已在队列中清空，仅同步复位 T9 控制器显示状态。 */
    var onT9CompositionCleared: (() -> Unit)? = null,
    /**
     * T9 候选词过滤器。服务层在获取 引擎候选词后调用，由键盘层根据
     * [com.cayboard.engine.T9InputController.selectionHistory] 过滤不匹配的候选词。
     * @param candidates 候选词文本列表
     * @param comments 候选词拼音注释列表
     * @return 过滤后的 (候选词列表, 注释列表)
     */
    var onFilterT9Candidates: ((List<String>, List<String>) -> Pair<List<String>, List<String>>)? = null,
    /**
     * T9 键盘状态变更后通知服务层刷新 UI（候选区、preedit 等）。
     * 携带 flush 后一次取回的 composition，服务层直接应用，避免内部重复 getComposition。
     */
    var onT9RefreshComposition: ((EngineComposition, List<com.cayboard.service.T9CandidateInjection>) -> Unit)? = null,
    /** T9 候选词变换（hotPath 插件能力）：T9 控制器后台取数后调用，
     *  返回带引擎锚点的插件候选注入列表（text 追加项），null/空 = 不干预。 */
    val onTCandidateTransform: ((EngineProcessResult) -> List<com.cayboard.service.T9CandidateInjection>?)? = null,
    /** 快捷发送表单"触发编码"输入框焦点变化（决定按键输入路由到编码框）。 */
    /**
     * 切到语音/手写前调用。
     * 服务层负责上屏首位候选词或待确认英文，再由键盘层切换布局。
     */
    val onCommitCandidateBeforeModeChange: (() -> Unit)? = null,
    /** 打拼音时切到数字面板、按中/英键：按打的字母原样上屏（不上屏首选，Gboard 规则，用户 2026-10-24）。 */
    val onCommitTypedBeforeModeChange: (() -> Unit)? = null,
    /** 假名按序进入输入引擎；变音仅修改仍在组合中的末尾编码。 */
    val onJapaneseKanaAction: ((JapaneseKanaAction) -> Unit)? = null,
    /** Explicit symbol categories commit the chosen glyph without width conversion. */
    val onCommitExactText: ((String) -> Unit)? = null,
    val onDeleteKaomoji: ((String?) -> String?)? = null,
    /** Create a gesture-local editor; its argument is the net number of characters to remove
     *  before the caret, or minus the number to remove after it (right swipe = forward delete). */
    val startDeleteDrag: (() -> DeleteDragSession)? = null,
    val isDeleteDragging: () -> Boolean = { false },
)
