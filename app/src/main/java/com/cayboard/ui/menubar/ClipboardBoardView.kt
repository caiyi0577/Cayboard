package com.cayboard.ui.menubar

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cayboard.clipboard.*
import com.cayboard.ui.keyboard.*
import com.cayboard.ui.theme.selectionColors
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

internal const val CLIPBOARD_CLOSE_AFTER_PASTE_KEY = "close_after_paste"
internal fun clipboardClosesAfterPaste(prefs: android.content.SharedPreferences) = prefs.getBoolean(CLIPBOARD_CLOSE_AFTER_PASTE_KEY,true)
internal fun clipboardClosesAfterPaste(context: Context) = clipboardClosesAfterPaste(context.getSharedPreferences("clipboard_board",Context.MODE_PRIVATE))

@Composable
internal fun ConnectedClipboardBoard(
    textItems: List<ClipboardItem>, onSelectText: (String)->Unit,
    onRemoveText: (List<Long>)->Unit, onSplit: (String,Long)->Unit,
    modifier: Modifier=Modifier, onOpenSearch: (()->Unit)?=null,
    onTranslateText: ((String)->Unit)?=null,
    keepQueryOnDispose: Boolean=false, liveQuery: ClipboardLiveQuery?=null, showControls: Boolean=true,
) {
    val context=LocalContext.current
    val manager=remember(context) { ClipboardManager.getInstance(context) }
    val prefs=remember(context) { context.getSharedPreferences("clipboard_board",Context.MODE_PRIVATE) }
    var listLayout by remember { mutableStateOf(prefs.getBoolean(CLIPBOARD_LIST_LAYOUT_KEY,false)) }
    val boardQuery by manager.boardQuery.collectAsState()
    val count by manager.historyCount.collectAsState()
    val room by manager.room.collectAsState()
    var results by remember { mutableStateOf<List<ClipboardItem>?>(null) }
    LaunchedEffect(boardQuery,liveQuery,textItems) {
        val query=liveQuery?.display ?: boardQuery
        if(query.isNotBlank()) kotlinx.coroutines.delay(120)
        results=when { liveQuery!=null && !liveQuery.isBlank -> manager.search(liveQuery)
            query.isNotBlank() -> manager.search(query)
            else -> null }
    }
    LaunchedEffect(manager) { runCatching { manager.readMissedCopy(boardOpened=true) } }
    DisposableEffect(manager) { onDispose { manager.resetHistoryWindow(); if(!keepQueryOnDispose) manager.boardQuery.value="" } }
    ClipboardBoardView(textItems=results ?: textItems,onSelectText=onSelectText,onSplit=onSplit,
        onRemove={ onRemoveText(it.map { row -> row.id }) },
        onLoadMore={ if(results==null) manager.loadMoreHistory() }, totalCount=count,
        modifier=modifier, listLayout=listLayout, onListLayoutChange={ listLayout=it; prefs.edit().putBoolean(CLIPBOARD_LIST_LAYOUT_KEY,it).apply() },
        onOpenSearch=onOpenSearch,onTranslateText=onTranslateText,
        onOpenSettings={ context.startActivity(com.cayboard.service.settingsActivityIntent(context,
            com.cayboard.ui.settings.SettingsRoutes.Clipboard)) },
        searchQuery=(liveQuery?.display ?: boardQuery).takeIf { it.isNotBlank() && liveQuery==null },
        onClearSearch={ manager.boardQuery.value="" },showControls=showControls,full=room.full)
}

/** Every action is on the board. A record has no context menu and no permanent pin/edit state. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClipboardBoardView(
    textItems: List<ClipboardItem>,onSelectText:(String)->Unit,onSplit:(String,Long)->Unit,
    onRemove:(List<ClipboardItem>)->Unit,
    modifier:Modifier=Modifier,onLoadMore:()->Unit={},totalCount:Int=-1,
    listLayout:Boolean=false,onListLayoutChange:((Boolean)->Unit)?=null,
    onOpenSearch:(()->Unit)?=null,onOpenSettings:(()->Unit)?=null,onTranslateText:((String)->Unit)?=null,
    searchQuery:String?=null,onClearSearch:()->Unit={},showControls:Boolean=true,
    full:Boolean=false,
) {
    val colors=MaterialTheme.colorScheme
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    var removing by remember { mutableStateOf(emptySet<Long>()) }
    var openId by remember { mutableStateOf<Long?>(null) }
    var openSide by remember { mutableIntStateOf(0) }
    var armedId by remember { mutableStateOf<Long?>(null) }
    var bulkArmed by remember { mutableStateOf<String?>(null) }
    var focusedId by remember { mutableStateOf<Long?>(null) }
    val rows=remember(textItems,removing) { textItems.distinctBy { it.id }.filterNot { it.id in removing }.sortedByDescending { it.timestamp } }
    val grid=rememberLazyStaggeredGridState()
    val keyboard=LocalClipboardKeyboardNavigation.current
    fun reset() { openId=null;openSide=0;armedId=null;bulkArmed=null }
    fun remove(row:ClipboardItem) { removing=removing+row.id;selected=selected-row.id;reset();onRemove(listOf(row)) }
    fun confirmDelete(row:ClipboardItem) { if(armedId==row.id) remove(row) else armedId=row.id }
    LaunchedEffect(textItems) {
        val ids=textItems.mapTo(mutableSetOf()) { it.id }
        selected=selected.intersect(ids);removing=removing.intersect(ids)
        reset()
    }
    val nearEnd by remember { derivedStateOf { grid.layoutInfo.let { it.totalItemsCount>0 && (it.visibleItemsInfo.lastOrNull()?.index ?: -1)>=it.totalItemsCount-30 } } }
    LaunchedEffect(nearEnd,textItems.size) { if(nearEnd) onLoadMore() }
    DisposableEffect(keyboard) { onDispose { keyboard?.attach(null) } }
    BoxWithConstraints(modifier.fillMaxSize().keyboardPanelBackground(colors.surface).testTag("clipboard-board")) {
        val columns=if(listLayout) 1 else clipboardColumnCount(maxWidth.value)
        val focused=rows.firstOrNull { it.id==focusedId } ?: rows.firstOrNull()
        SideEffect { keyboard?.attach { action ->
            when(action) {
                ClipboardNavigation.DELETE -> focused?.let { reset();openId=it.id;openSide=-1 }
                ClipboardNavigation.CONFIRM -> focused?.let { if(openId==it.id) confirmDelete(it) else onSelectText(it.text) }
                ClipboardNavigation.MENU -> focused?.let { reset();onSplit(it.text,it.id) }
                ClipboardNavigation.CANCEL -> if(openId!=null || selecting) { reset();selecting=false;selected=emptySet() } else keyboard.close()
                else -> {
                    val key=nextClipboardKey(rows.map { "text:${it.id}" },focused?.let { "text:${it.id}" },action,columns,
                        grid.layoutInfo.visibleItemsInfo.map { ClipboardCell(it.key.toString(),it.offset.x,it.offset.y,it.size.width,it.size.height) })
                    focusedId=key?.removePrefix("text:")?.toLongOrNull();reset()
                }
            }; true
        } }
        Column(Modifier.fillMaxSize()) {
            if(showControls) FlowRow(Modifier.fillMaxWidth().testTag("clipboard-controls").clipboardPanelExpandGesture().padding(horizontal=8.dp,vertical=4.dp),
                horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                if(selecting) {
                    ClipboardAction(Icons.Default.Close,"完成",{reset();selecting=false;selected=emptySet()},"clipboard-exit-selection")
                    ClipboardAction(Icons.Default.DoneAll,"全选",{reset();selected=if(selected.size==rows.size) emptySet() else rows.mapTo(mutableSetOf()) { it.id }},"clipboard-select-all")
                    ClipboardAction(Icons.Default.DeleteOutline,if(bulkArmed=="selected") "确认删除" else "删除 ${selected.size}",{
                        if(bulkArmed=="selected") { val gone=rows.filter { it.id in selected };removing=removing+selected;onRemove(gone);selected=emptySet();selecting=false;reset() }
                        else { reset();bulkArmed="selected" }
                    },"clipboard-delete-selected",selected.isNotEmpty(),danger=true)
                    if(selected.size==1 && onTranslateText!=null) ClipboardAction(Icons.Default.Translate,"翻译",{
                        rows.firstOrNull { it.id in selected && !it.sensitive }?.let { onTranslateText(it.text) }
                    },"clipboard-translate")
                } else {
                    onListLayoutChange?.let { change -> ClipboardAction(if(listLayout) Icons.Default.GridView else Icons.Default.ViewAgenda,
                        if(listLayout) "平铺" else "列表",{reset();change(!listLayout)},"clipboard-layout") }
                    onOpenSearch?.let { ClipboardAction(Icons.Default.Search,"搜索",{reset();it()},"clipboard-search") }
                    ClipboardAction(Icons.Default.DoneAll,"多选",{reset();selecting=true},"clipboard-select",rows.isNotEmpty())
                    onOpenSettings?.let { ClipboardAction(Icons.Default.Settings,"设置",{reset();it()},"clipboard-settings") }
                }
            }
            searchQuery?.let { TextButton(onClick={reset();onClearSearch()},modifier=Modifier.testTag("clipboard-search-chip")) { Text("$it  ×",maxLines=1,overflow=TextOverflow.Ellipsis) } }
            LazyVerticalStaggeredGrid(StaggeredGridCells.Fixed(columns),Modifier.weight(1f).fillMaxWidth()
                .testTag("clipboard-records").clipboardPanelCollapseGesture(enabled=openId==null && !selecting) { !grid.canScrollBackward },state=grid,
                contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalItemSpacing=8.dp) {
                if(full) item(key="full",span=StaggeredGridItemSpan.FullLine) { Text("剪贴板空间不足，请删除部分记录。",color=colors.error,fontSize=13.sp) }
                if(rows.isEmpty()) item(key="empty",span=StaggeredGridItemSpan.FullLine) { Text(if(searchQuery!=null) "没有找到「$searchQuery」" else "暂无剪贴板记录，复制后会显示在这里。",Modifier.padding(16.dp),color=colors.onSurfaceVariant,fontSize=14.sp) }
                items(rows,key={ "text:${it.id}" }) { row ->
                    SwipeClipboardRecord(row,selected=row.id in selected,selecting=selecting,
                        focused=keyboard?.active==true && focused?.id==row.id,
                        revealedSide=if(openId==row.id) openSide else 0,armed=armedId==row.id,
                        onReveal={ side -> bulkArmed=null;armedId=null;openId=row.id.takeIf { side!=0 };openSide=side },
                        onDelete={confirmDelete(row)},onClick={
                            focusedId=row.id
                            if(openId==row.id) reset()
                            else { reset(); if(selecting) selected=if(row.id in selected) selected-row.id else selected+row.id else onSelectText(row.text) }
                        },onLongClick={reset();onSplit(row.text,row.id)})
                }
            }
        }
    }
}

@Composable
private fun ClipboardAction(icon:androidx.compose.ui.graphics.vector.ImageVector,label:String,onClick:()->Unit,tag:String,
    enabled:Boolean=true,danger:Boolean=false,chosen:Boolean=false) {
    val colors=MaterialTheme.colorScheme
    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if(chosen) colors.primary.copy(alpha=.12f) else Color.Transparent)
        .clickable(enabled=enabled,role=Role.Button,onClick=onClick).testTag(tag).heightIn(min=34.dp)
        .padding(horizontal=7.dp,vertical=5.dp).semantics { if(chosen) selected=true },verticalAlignment=Alignment.CenterVertically) {
        val color=(if(danger) colors.error else colors.onSurface).copy(alpha=if(enabled) 1f else .35f)
        Icon(icon,null,Modifier.size(18.dp),tint=color)
        Text(label,Modifier.padding(start=4.dp),fontSize=12.sp,color=color,maxLines=1)
    }
}

/** Either swipe reveals the action. Only two separate button clicks delete the row. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwipeClipboardRecord(item:ClipboardItem,selected:Boolean=false,selecting:Boolean=false,focused:Boolean=false,
    revealedSide:Int=0,armed:Boolean=false,onReveal:(Int)->Unit,onDelete:()->Unit,onClick:()->Unit,onLongClick:()->Unit,
    modifier:Modifier=Modifier) {
    val colors=MaterialTheme.colorScheme
    val selection=selectionColors()
    val density=LocalDensity.current
    val width=with(density) { 88.dp.toPx() }
    var dragging by remember(item.id) { mutableStateOf(false) }
    var dragOffset by remember(item.id) { mutableFloatStateOf(0f) }
    val offset by animateFloatAsState(if(dragging) dragOffset else revealedSide*width,
        animationSpec=if(dragging) snap() else tween(140),label="clipboard-reveal")
    val shape=RoundedCornerShape(12.dp)
    val cardColor=lerp(colors.surface,colors.primary,if(colors.surface.luminance()<.5f) .10f else .065f).copy(alpha=1f)
    Box(modifier.fillMaxWidth().clip(shape).testTag("clipboard-card:text:${item.id}")
        .semantics { customActions=listOf(CustomAccessibilityAction("显示删除按钮") { onReveal(-1);true }) }
        .draggable(rememberDraggableState { dragOffset=(dragOffset+it).coerceIn(-width,width) },Orientation.Horizontal,
            onDragStarted={dragOffset=offset;dragging=true;onReveal(revealedSide)},
            onDragStopped={velocity -> val side=if(abs(dragOffset)>=width*.35f || abs(velocity)>900f && abs(dragOffset)>8f) dragOffset.sign.toInt() else 0;onReveal(side);dragging=false })) {
        if(abs(offset)>1f || revealedSide!=0) Box(Modifier.matchParentSize().background(Color(0xFFC83C41))) {
            TextButton(onClick=onDelete,modifier=Modifier.align(if(offset>0) Alignment.CenterStart else Alignment.CenterEnd)
                .width(88.dp).fillMaxHeight().testTag("clipboard-delete:${item.id}")
                .semantics { contentDescription=if(armed) "再次点击确认删除此记录" else "删除此记录" }) {
                Text(if(armed) "确认删除" else "删除",color=Color.White,fontSize=14.sp,fontWeight=FontWeight.Medium)
            }
        }
        Box(Modifier.fillMaxWidth().padding(start=with(density) { offset.coerceAtLeast(0f).toDp() },
            end=with(density) { (-offset).coerceAtLeast(0f).toDp() }).clip(shape)
            .background(if(selected || focused) selection.quietFill else cardColor)
            .semantics { if(selecting) this.selected=selected;this.focused=focused }
            .combinedClickable(onClick=onClick,onLongClick=onLongClick,onLongClickLabel="分词",role=if(selecting) Role.Checkbox else Role.Button)
            .heightIn(min=64.dp).padding(horizontal=12.dp,vertical=10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                if(selecting) { Icon(if(selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,null,Modifier.size(18.dp),tint=colors.primary);Spacer(Modifier.width(8.dp)) }
                Text(if(item.sensitive) ClipboardLinkChoices.SENSITIVE_MASK else remember(item.text) { clipboardRecordPreview(item.text) },
                    color=if(selected || focused) selection.quietContent else colors.onSurface,fontSize=15.sp,lineHeight=21.sp,
                    maxLines=2,overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
            }
        }
    }
}
