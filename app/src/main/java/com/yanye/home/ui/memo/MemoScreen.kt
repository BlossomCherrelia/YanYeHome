package com.yanye.home.ui.memo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.domain.model.Memo
import com.yanye.home.domain.model.MemoCategory
import com.yanye.home.domain.model.Visibility
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.SecondaryTopBar
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private sealed interface MemoPage {
    data object List : MemoPage
    data class Editor(val memo: Memo?) : MemoPage
    data object HomeDisplay : MemoPage
}

private enum class MemoFilter {
    All,
    Shared,
    Private
}

private val MemoEditorLabelWidth = 104.dp

@Composable
fun MemoScreen(
    viewModel: MemoViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val memos by viewModel.memos.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var page by remember { mutableStateOf<MemoPage>(MemoPage.List) }
    var filter by remember { mutableStateOf(MemoFilter.All) }
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncMemos,
        onFlushSync = viewModel::flushSync
    )

    when (val currentPage = page) {
        MemoPage.List -> MemoListPage(
            memos = memos,
            filter = filter,
            syncMessage = syncMessage,
            onFilterChange = { filter = it },
            onBack = onBack,
            onAdd = { page = MemoPage.Editor(null) },
            onEdit = { page = MemoPage.Editor(it) },
            onHomeDisplay = { page = MemoPage.HomeDisplay },
            onToggleCompleted = { memo -> viewModel.setMemoCompleted(memo.id, !memo.isCompleted) },
            onDelete = { memo -> viewModel.deleteMemo(memo.id) }
        )

        is MemoPage.Editor -> MemoEditorPage(
            initialMemo = currentPage.memo,
            onBack = { page = MemoPage.List },
            onSave = { memo ->
                viewModel.saveMemo(memo)
                page = MemoPage.List
            },
            onDelete = currentPage.memo?.let { memo ->
                {
                    viewModel.deleteMemo(memo.id)
                    page = MemoPage.List
                }
            }
        )

        MemoPage.HomeDisplay -> MemoHomeDisplayPage(
            memos = memos,
            onBack = { page = MemoPage.List },
            onSave = { updated ->
                viewModel.saveHomeDisplaySettings(updated)
                page = MemoPage.List
            }
        )
    }
}

@Composable
private fun MemoListPage(
    memos: List<Memo>,
    filter: MemoFilter,
    syncMessage: String?,
    onFilterChange: (MemoFilter) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Memo) -> Unit,
    onHomeDisplay: () -> Unit,
    onToggleCompleted: (Memo) -> Unit,
    onDelete: (Memo) -> Unit
) {
    val visibleMemos = memos.filter { memo ->
        when (filter) {
            MemoFilter.All -> true
            MemoFilter.Shared -> memo.sharedWithPartner || memo.visibility == Visibility.Shared
            MemoFilter.Private -> !memo.sharedWithPartner || memo.visibility == Visibility.Private
        }
    }
    val pending = visibleMemos.filterNot { it.isCompleted }
    val completed = visibleMemos.filter { it.isCompleted }

    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PageChrome.secondaryPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SecondaryTopBar(
                    title = "备忘录",
                    actionText = "展示",
                    actionColor = YanYeColors.Muted,
                    onBack = onBack,
                    onActionClick = onHomeDisplay
                )
            }
            item {
                MemoFilterTabs(selected = filter, onSelected = onFilterChange)
            }
            if (filter != MemoFilter.Private && syncMessage != null) {
                item {
                    Text(
                        text = syncMessage,
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            item { MemoSectionTitle("待完成") }
            if (pending.isEmpty()) {
                item {
                    Text(
                        text = "还没有待办哦，快去加入备忘录吧！",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(pending, key = { it.id }) { memo ->
                    MemoListCard(
                        memo = memo,
                        onClick = { onEdit(memo) },
                        onToggleCompleted = { onToggleCompleted(memo) },
                        onDelete = { onDelete(memo) }
                    )
                }
            }
            item { MemoSectionTitle("已完成") }
            if (completed.isEmpty()) {
                item {
                    Text(
                        text = "完成以后会出现在这里",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(completed, key = { it.id }) { memo ->
                    MemoListCard(
                        memo = memo,
                        onClick = { onEdit(memo) },
                        onToggleCompleted = { onToggleCompleted(memo) },
                        onDelete = { onDelete(memo) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 110.dp),
            containerColor = Color(0xFFFF7FA1),
            contentColor = Color.White,
            shape = MaterialTheme.shapes.large
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun MemoFilterTabs(
    selected: MemoFilter,
    onSelected: (MemoFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MemoFilterTab("全部待办", MemoFilter.All, selected, onSelected)
        MemoFilterTab("共享待办", MemoFilter.Shared, selected, onSelected)
        MemoFilterTab("个人待办", MemoFilter.Private, selected, onSelected)
    }
}

@Composable
private fun MemoFilterTab(
    label: String,
    value: MemoFilter,
    selected: MemoFilter,
    onSelected: (MemoFilter) -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected == value) Color(0xFFFF7FA1) else Color.White.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onSelected(value) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected == value) Color.White else YanYeColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MemoSectionTitle(title: String) {
    Text(
        text = title,
        color = YanYeColors.Ink,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun MemoListCard(
    memo: Memo,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    var offsetX by remember(memo.id) { mutableStateOf(0f) }
    var revealed by remember(memo.id) { mutableStateOf(false) }
    val revealWidthPx = with(density) { 92.dp.toPx() }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val thresholdPx = with(density) { maxWidth.toPx() * 0.3f }
        val dragMaxPx = maxOf(revealWidthPx, thresholdPx)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(YanYeColors.Rose, MaterialTheme.shapes.medium)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "删除",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 27.dp)
            )
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(memo.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            revealed = offsetX >= thresholdPx
                            offsetX = if (revealed) revealWidthPx else 0f
                        },
                        onDragCancel = {
                            revealed = false
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val base = if (revealed) revealWidthPx else offsetX
                            offsetX = (base + dragAmount).coerceIn(0f, dragMaxPx)
                        }
                    )
                }
                .clickable {
                    if (revealed || offsetX > 0f) {
                        revealed = false
                        offsetX = 0f
                    } else {
                        onClick()
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFAFB)),
            border = BorderStroke(0.6.dp, Color(0xFFFFE3EB)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            val dueText = memoDueText(memo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MemoCheckBox(checked = memo.isCompleted, onClick = onToggleCompleted)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = memo.title,
                        color = if (memo.isCompleted) YanYeColors.Muted else YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (memo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (memo.content.isNotBlank()) {
                        Text(
                            text = memo.content,
                            color = YanYeColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (dueText != null) {
                        Text(
                            text = dueText,
                            color = YanYeColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                MemoScopePill(memo)
            }
        }
    }
}

@Composable
private fun MemoCheckBox(
    checked: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(
                color = if (checked) YanYeColors.Green.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (checked) 0.dp else 1.6.dp,
                color = YanYeColors.Line,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun MemoScopePill(memo: Memo) {
    val shared = memo.sharedWithPartner || memo.visibility == Visibility.Shared
    Box(
        modifier = Modifier
            .background(if (shared) YanYeColors.BlueSoft else YanYeColors.Soft, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (shared) "共享" else "个人",
            color = if (shared) YanYeColors.Blue else YanYeColors.Muted,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MemoEditorPage(
    initialMemo: Memo?,
    onBack: () -> Unit,
    onSave: (Memo) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember(initialMemo?.id) { mutableStateOf(initialMemo?.title.orEmpty()) }
    var content by remember(initialMemo?.id) { mutableStateOf(initialMemo?.content.orEmpty()) }
    var shared by remember(initialMemo?.id) {
        mutableStateOf(initialMemo?.let { it.sharedWithPartner || it.visibility == Visibility.Shared } ?: true)
    }
    var dueDate by remember(initialMemo?.id) {
        mutableStateOf(initialMemo?.reminderAtMillis?.let { millisToLocalDate(it) })
    }
    var dueTime by remember(initialMemo?.id) {
        mutableStateOf(
            initialMemo?.takeIf { it.reminderAtMillis != null && it.dueLabel != "DATE_ONLY" }
                ?.reminderAtMillis
                ?.let { millisToLocalTime(it).format(DateTimeFormatter.ofPattern("HH:mm")) }
                .orEmpty()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val canSave = title.isNotBlank()
    val saveMemo = {
        val parsedTime = parseMemoTime(dueTime)
        val effectiveDate = dueDate ?: if (parsedTime != null) LocalDate.now() else null
        val deadlineMillis = effectiveDate?.let { date ->
            val time = parsedTime ?: LocalTime.MIDNIGHT
            date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
        val dueLabel = when {
            effectiveDate == null -> ""
            parsedTime == null -> "DATE_ONLY"
            else -> "DATE_TIME"
        }
        onSave(
            Memo(
                id = initialMemo?.id ?: 0,
                title = title.trim(),
                content = content.trim(),
                category = MemoCategory.General,
                dueLabel = dueLabel,
                reminderAtMillis = deadlineMillis,
                reminderEnabled = false,
                visibility = if (shared) Visibility.Shared else Visibility.Private,
                sharedWithPartner = shared,
                isCompleted = initialMemo?.isCompleted ?: false,
                showOnHome = initialMemo?.showOnHome ?: false,
                homeSortOrder = initialMemo?.homeSortOrder ?: 100,
                createdAt = initialMemo?.createdAt ?: 0,
                updatedAt = initialMemo?.updatedAt ?: 0,
                isDeleted = initialMemo?.isDeleted ?: false,
                remoteId = initialMemo?.remoteId,
                coupleId = initialMemo?.coupleId,
                ownerUserId = initialMemo?.ownerUserId,
                syncStatus = initialMemo?.syncStatus ?: com.yanye.home.domain.model.SyncStatus.Synced,
                remoteUpdatedAt = initialMemo?.remoteUpdatedAt
            )
        )
    }

    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PageChrome.secondaryPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SecondaryTopBar(
                title = if (initialMemo == null) "新增备忘" else "编辑备忘",
                actionText = "保存",
                actionColor = if (canSave) YanYeColors.Blue else YanYeColors.Muted,
                onBack = onBack,
                onActionClick = if (canSave) saveMemo else null
            )
        }
        item {
            Text(
                text = title.ifBlank { "未命名备忘" },
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            MemoEditGroup(
                rows = listOf(
                    MemoEditorRow.Text("标题", title, { title = it }),
                    MemoEditorRow.Text("地点/备注", content, { content = it }, singleLine = false),
                    MemoEditorRow.Action("待完成日期", dueDate?.let(::formatMemoDate) ?: "未设置", { showDatePicker = true }),
                    MemoEditorRow.Action("待完成时间", dueTime.ifBlank { "可不填" }, { showTimePicker = true })
                )
            )
        }
        item {
            MemoToggleCard(
                title = "共享待办",
                checked = shared,
                onCheckedChange = { shared = it }
            )
        }
        if (dueDate != null || dueTime.isNotBlank()) {
            item {
                val clearTimeFirst = dueTime.isNotBlank()
                Button(
                    onClick = {
                        if (clearTimeFirst) {
                            dueTime = ""
                        } else {
                            dueDate = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YanYeColors.Soft,
                        contentColor = YanYeColors.Muted
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (clearTimeFirst) "清除待完成时间" else "清除待完成日期",
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }
        item {
            Button(
                onClick = saveMemo,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YanYeColors.Ink,
                    contentColor = Color.White,
                    disabledContainerColor = YanYeColors.Soft,
                    disabledContentColor = YanYeColors.Muted
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "保存备忘",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        if (onDelete != null) {
            item {
                Text(
                    text = "删除备忘",
                    color = YanYeColors.Rose,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
    }

    if (showDatePicker) {
        DatePickerPopup(
            initialDate = dueDate ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { selected ->
                dueDate = selected
                showDatePicker = false
            }
        )
    }
    if (showTimePicker) {
        TimePickerPopup(
            initialTime = parseMemoTime(dueTime) ?: LocalTime.of(18, 0),
            onDismiss = { showTimePicker = false },
            onClear = {
                dueTime = ""
                showTimePicker = false
            },
            onTimeSelected = { selected ->
                dueTime = selected.format(DateTimeFormatter.ofPattern("HH:mm"))
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun MemoHomeDisplayPage(
    memos: List<Memo>,
    onBack: () -> Unit,
    onSave: (List<Memo>) -> Unit
) {
    val pending = memos
        .filterNot { it.isCompleted }
        .sortedWith(compareBy<Memo> { it.homeSortOrder }.thenBy { it.reminderAtMillis ?: Long.MAX_VALUE })
    var selectedIds by remember(memos) {
        mutableStateOf(pending.filter { it.showOnHome }.sortedBy { it.homeSortOrder }.map { it.id })
    }
    val rows = remember(pending, selectedIds) {
        mutableStateListOf<Memo>().apply {
            val selectedRows = selectedIds.mapNotNull { id -> pending.firstOrNull { it.id == id } }
            val restRows = pending.filterNot { it.id in selectedIds }
            addAll(selectedRows + restRows)
        }
    }
    val save = {
        val orderedIds = rows.map { it.id }
        val updated = rows.mapIndexed { index, memo ->
            memo.copy(
                showOnHome = memo.id in selectedIds,
                homeSortOrder = if (memo.id in selectedIds) index else 100 + orderedIds.indexOf(memo.id)
            )
        }
        onSave(updated)
    }

    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PageChrome.secondaryPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SecondaryTopBar(
                title = "首页展示",
                actionText = "保存",
                actionColor = YanYeColors.Blue,
                onBack = onBack,
                onActionClick = save
            )
        }
        item {
            Text(
                text = "未手动选择时，首页自动展示最紧急的 3 条；手动开启后可展示多条，并按这里的顺序展示。",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        itemsIndexed(rows, key = { _, memo -> memo.id }) { index, memo ->
            MemoHomeDisplayCard(
                memo = memo,
                checked = memo.id in selectedIds,
                onCheckedChange = { checked ->
                    selectedIds = if (checked) {
                        (selectedIds + memo.id).distinct()
                    } else {
                        selectedIds.filterNot { it == memo.id }
                    }
                },
                onMove = { direction ->
                    val targetIndex = (index + direction).coerceIn(0, rows.lastIndex)
                    if (targetIndex != index) {
                        val moved = rows.removeAt(index)
                        rows.add(targetIndex, moved)
                        selectedIds = rows.filter { it.id in selectedIds }.map { it.id }
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun MemoHomeDisplayCard(
    memo: Memo,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit
) {
    var dragRemainder by remember(memo.id) { mutableStateOf(0f) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "☰",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.pointerInput(memo.id) {
                    detectDragGestures(
                        onDragEnd = { dragRemainder = 0f },
                        onDragCancel = { dragRemainder = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragRemainder += dragAmount.y
                            when {
                                dragRemainder > 40f -> {
                                    onMove(1)
                                    dragRemainder = 0f
                                }
                                dragRemainder < -40f -> {
                                    onMove(-1)
                                    dragRemainder = 0f
                                }
                            }
                        }
                    )
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memo.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val meta = memoListMeta(memo)
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.78f)
            )
        }
    }
}

private sealed interface MemoEditorRow {
    data class Text(
        val label: String,
        val value: String,
        val onValueChange: (String) -> Unit,
        val singleLine: Boolean = true,
        val placeholder: String = "未设置"
    ) : MemoEditorRow

    data class Action(
        val label: String,
        val value: String,
        val onClick: () -> Unit
    ) : MemoEditorRow
}

@Composable
private fun MemoEditGroup(rows: List<MemoEditorRow>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            rows.forEachIndexed { index, row ->
                when (row) {
                    is MemoEditorRow.Text -> MemoEditTextRow(row)
                    is MemoEditorRow.Action -> MemoEditActionRow(row)
                }
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = YanYeColors.Line, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun MemoEditTextRow(row: MemoEditorRow.Text) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (row.singleLine) 58.dp else 92.dp),
        verticalAlignment = if (row.singleLine) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = row.label,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = if (row.singleLine) 0.dp else 18.dp)
                .width(MemoEditorLabelWidth)
        )
        BasicTextField(
            value = row.value,
            onValueChange = row.onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(top = if (row.singleLine) 0.dp else 18.dp),
            singleLine = row.singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = YanYeColors.Ink,
                fontWeight = FontWeight.SemiBold
            ),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (row.value.isBlank()) {
                        Text(
                            text = row.placeholder,
                            color = YanYeColors.Muted.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun MemoEditActionRow(row: MemoEditorRow.Action) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = row.onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = row.label,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(MemoEditorLabelWidth)
        )
        Text(
            text = row.value,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text("›", color = YanYeColors.Muted, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun MemoToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .scale(0.76f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerPopup(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = pickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        onDateSelected(
                            Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun TimePickerPopup(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.6.dp, YanYeColors.Line),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "选择时间",
                            color = YanYeColors.Ink,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%02d:%02d".format(selectedHour, selectedMinute),
                            color = YanYeColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    TextButton(onClick = onClear) {
                        Text("不设置")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimeWheelColumn(
                        values = 0..23,
                        selected = selectedHour,
                        suffix = "时",
                        onSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimeWheelColumn(
                        values = 0..59,
                        selected = selectedMinute,
                        suffix = "分",
                        onSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YanYeColors.Soft,
                            contentColor = YanYeColors.Muted
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("取消", modifier = Modifier.padding(vertical = 7.dp))
                    }
                    Button(
                        onClick = { onTimeSelected(LocalTime.of(selectedHour, selectedMinute)) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YanYeColors.Ink,
                            contentColor = Color.White
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("确定", modifier = Modifier.padding(vertical = 7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    values: IntRange,
    selected: Int,
    suffix: String,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(values) { values.toList() }
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0)
    )
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0))
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .height(230.dp)
            .background(YanYeColors.Paper, MaterialTheme.shapes.medium)
            .padding(horizontal = 6.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(options.size) { index ->
            val value = options[index]
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        color = if (isSelected) YanYeColors.Soft else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelected(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d%s".format(value, suffix),
                    color = if (isSelected) YanYeColors.Ink else YanYeColors.Muted.copy(alpha = 0.62f),
                    style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun memoListMeta(memo: Memo): String {
    val parts = buildList {
        if (memo.content.isNotBlank()) add(memo.content)
        memoDueText(memo)?.let { add(it) }
    }
    return parts.joinToString(" · ")
}

private fun formatReminder(millis: Long): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val today = LocalDate.now()
    val dateLabel = when (dateTime.toLocalDate()) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("M/d"))
    }
    return "$dateLabel ${dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

private fun memoDueText(memo: Memo): String? {
    val millis = memo.reminderAtMillis
    return when {
        memo.dueLabel == "DATE_ONLY" && millis != null -> formatMemoDateOnly(millisToLocalDate(millis))
        memo.dueLabel == "DATE_TIME" && millis != null -> formatReminder(millis)
        memo.dueLabel == "本周内" -> "本周内"
        memo.dueLabel == "今天" && millis != null -> {
            val time = millisToLocalTime(millis).format(DateTimeFormatter.ofPattern("HH:mm"))
            "今天$time"
        }
        memo.dueLabel == "DDL" && millis != null -> {
            val date = millisToLocalDate(millis)
            formatMemoDateOnly(date)
        }
        millis != null -> formatReminder(millis)
        memo.dueLabel.isNotBlank() -> memo.dueLabel
        else -> null
    }
}

private fun formatMemoDateOnly(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> date.format(DateTimeFormatter.ofPattern("M/d"))
    }
}

private fun formatMemoDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun millisToLocalTime(millis: Long): LocalTime =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()

private fun parseMemoTime(value: String): LocalTime? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
}
