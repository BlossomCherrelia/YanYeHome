package com.yanye.home.ui.memory

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.MemoryMood
import com.yanye.home.domain.model.Schedule
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.common.ImagePickerField
import com.yanye.home.ui.common.ImagePreviewBox
import com.yanye.home.ui.common.ImageUploadState
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.SecondaryTopBar
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.common.isLocalOnlyImageUri
import com.yanye.home.ui.schedule.ScheduleViewModel
import com.yanye.home.ui.theme.YanYeColors
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private sealed interface MemoryPage {
    data object List : MemoryPage
    data class Detail(val memory: Memory) : MemoryPage
    data class Editor(val memory: Memory?) : MemoryPage
}

private sealed interface MemoryTimelineEntry {
    data class Year(val year: Int) : MemoryTimelineEntry
    data class Item(
        val memory: Memory,
        val rowIndex: Int
    ) : MemoryTimelineEntry
}

@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    initialMemoryId: Long? = null,
    viewModel: ScheduleViewModel = viewModel()
) {
    val memories by viewModel.memories.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncMemories,
        onFlushSync = viewModel::flushSync
    )
    var page by remember { mutableStateOf<MemoryPage>(MemoryPage.List) }
    var didOpenInitialMemory by remember(initialMemoryId) { mutableStateOf(false) }

    LaunchedEffect(initialMemoryId, memories) {
        if (!didOpenInitialMemory && initialMemoryId != null) {
            memories.firstOrNull { it.id == initialMemoryId }?.let { memory ->
                page = MemoryPage.Detail(memory)
                didOpenInitialMemory = true
            }
        }
    }

    when (val current = page) {
        MemoryPage.List -> MemoryListPage(
            memories = memories,
            onBack = onBack,
            onAdd = { page = MemoryPage.Editor(null) },
            onOpen = { page = MemoryPage.Detail(it) }
        )
        is MemoryPage.Detail -> {
            val latest = memories.firstOrNull { it.id == current.memory.id } ?: current.memory
            MemoryDetailPage(
                memory = latest,
                schedules = schedules,
                onBack = { page = MemoryPage.List },
                onEdit = { page = MemoryPage.Editor(latest) }
            )
        }
        is MemoryPage.Editor -> MemoryEditorPage(
            initialMemory = current.memory,
            onBack = { page = MemoryPage.List },
            onSave = { memory ->
                viewModel.saveMemory(memory)
                page = MemoryPage.List
            }
        )
    }
}

@Composable
private fun MemoryListPage(
    memories: List<Memory>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Memory) -> Unit
) {
    val rows = remember(memories) {
        memories.filterNot { it.isDeleted }.sortedWith(compareByDescending<Memory> { it.dateEpochDay }.thenByDescending { it.createdAt })
    }
    val entries = remember(rows) { buildMemoryTimelineEntries(rows) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showJumpDialog by remember { mutableStateOf(false) }
    val currentYear = remember(entries, listState.firstVisibleItemIndex) {
        currentStickyYear(entries, listState.firstVisibleItemIndex)
            ?: rows.firstOrNull()?.let { LocalDate.ofEpochDay(it.dateEpochDay).year }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = PageChrome.horizontalPadding,
                    top = PageChrome.secondaryTopPadding,
                    end = PageChrome.horizontalPadding
                )
        ) {
            SecondaryTopBar(
                title = "回忆",
                actionText = "+",
                actionColor = YanYeColors.Muted,
                onBack = onBack,
                onActionClick = onAdd
            )
            if (rows.isEmpty()) {
                Text(
                    text = "还没有回忆哦，记录一次共同瞬间吧。",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                currentYear?.let { year ->
                    MemoryYearHeader(
                        year = year,
                        onClick = { showJumpDialog = true }
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        count = entries.size,
                        key = { index ->
                            when (val entry = entries[index]) {
                                is MemoryTimelineEntry.Year -> "year-divider-${entry.year}"
                                is MemoryTimelineEntry.Item -> "memory-${entry.memory.id}"
                            }
                        }
                    ) { index ->
                        when (val entry = entries[index]) {
                            is MemoryTimelineEntry.Year -> {
                                if (index != 0) {
                                    MemoryYearHeader(
                                        year = entry.year,
                                        onClick = { showJumpDialog = true }
                                    )
                                }
                            }
                            is MemoryTimelineEntry.Item -> {
                                val memory = entry.memory
                                val rowIndex = entry.rowIndex
                                TimelineMemoryCard(
                                    memory = memory,
                                    isFirst = rowIndex == 0,
                                    isLast = rowIndex == rows.lastIndex,
                                    isMonthStart = isMonthStart(rows, rowIndex),
                                    onClick = { onOpen(memory) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showJumpDialog) {
        MemoryMonthJumpDialog(
            initialMonth = YearMonth.now(),
            onDismiss = { showJumpDialog = false },
            onJump = { target ->
                showJumpDialog = false
                val targetIndex = rows.indexOfFirst { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == target }
                    .takeIf { it >= 0 }
                    ?: rows.indexOfFirst { LocalDate.ofEpochDay(it.dateEpochDay).isBefore(target.atEndOfMonth()) }
                        .takeIf { it >= 0 }
                    ?: rows.lastIndex.coerceAtLeast(0)
                scope.launch {
                    if (rows.isNotEmpty()) listState.animateScrollToItem(memoryEntryIndex(entries, rows[targetIndex].id))
                }
            }
        )
    }
}

@Composable
private fun MemoryYearHeader(
    year: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 30.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(top = 5.dp, bottom = 5.dp)
    ) {
        Text(
            text = "${year}年 ˅",
            color = YanYeColors.Rose,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimelineMemoryCard(
    memory: Memory,
    isFirst: Boolean,
    isLast: Boolean,
    isMonthStart: Boolean,
    onClick: () -> Unit
) {
    val date = LocalDate.ofEpochDay(memory.dateEpochDay)
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .heightIn(min = if (firstPhotoUri(memory.photoUris) != null) 200.dp else 124.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(YanYeColors.Line)
                        .align(Alignment.TopCenter)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxSize()
                        .padding(top = 32.dp)
                        .background(YanYeColors.Line)
                        .align(Alignment.TopCenter)
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .size(16.dp)
                    .background(YanYeColors.Rose, CircleShape)
            )
            if (isMonthStart) {
                Text(
                    text = date.monthValue.toString().padStart(2, '0'),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 40.dp)
                )
            }
        }
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 14.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(0.6.dp, YanYeColors.Line),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = memoryListMeta(memory),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = memory.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 3.dp)
                )
                val summary = memoryListSummary(memory)
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                firstPhotoUri(memory.photoUris)?.let { photoUri ->
                    MemoryPhotoPreview(
                        photoUri = photoUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        maxHeight = 360.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryDetailPage(
    memory: Memory,
    schedules: List<Schedule>,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PageChrome.secondaryPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SecondaryTopBar(
                title = "回忆详情",
                actionText = "编辑",
                actionColor = YanYeColors.Muted,
                onBack = onBack,
                onActionClick = onEdit
            )
        }
        item {
            MemoryPhotoPreview(
                photoUri = firstPhotoUri(memory.photoUris),
                modifier = Modifier.fillMaxWidth(),
                maxHeight = 900.dp
            )
        }
        item {
            Column {
                Text(
                    text = memory.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = memoryDetailMeta(memory),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        if (memory.note.isNotBlank()) {
            item {
                Text(
                    text = "内容",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(0.6.dp, YanYeColors.Line),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = memory.note,
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        /*
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.6.dp, YanYeColors.Line),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = memory.note.ifBlank { "null" },
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        */
        /*
        item {
            HorizontalDivider(color = YanYeColors.Line, thickness = 1.dp)
        }
        item {
            Text(
                text = "关联",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.6.dp, YanYeColors.Line),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📅", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = linkedSchedule?.let { "关联日程：${formatMonthDay(memory.dateEpochDay)} ${it.title}" } ?: "关联日程：null",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text("→", color = YanYeColors.Rose, fontWeight = FontWeight.Bold)
                }
            }
        }
        */
    }
    }
}

@Composable
private fun MemoryEditorPage(
    initialMemory: Memory?,
    onBack: () -> Unit,
    onSave: (Memory) -> Unit
) {
    var title by remember(initialMemory?.id) { mutableStateOf(initialMemory?.title.orEmpty()) }
    var date by remember(initialMemory?.id) { mutableStateOf(initialMemory?.dateEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()) }
    var locationName by remember(initialMemory?.id) { mutableStateOf(initialMemory?.locationName.orEmpty()) }
    var photoUris by remember(initialMemory?.id) { mutableStateOf(initialMemory?.photoUris.orEmpty()) }
    var mood by remember(initialMemory?.id) { mutableStateOf(initialMemory?.mood ?: MemoryMood.Happy) }
    var note by remember(initialMemory?.id) { mutableStateOf(initialMemory?.note.orEmpty()) }
    var imageUploadState by remember { mutableStateOf(ImageUploadState()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val hasPendingLocalImage = photoUris.isNotBlank() && isLocalOnlyImageUri(photoUris)
    val imageBlockingMessage = when {
        imageUploadState.isUploading -> "图片上传中，请稍后再保存"
        imageUploadState.errorMessage != null -> "图片上传失败，请重试或移除图片"
        hasPendingLocalImage -> "图片尚未变成云端地址，暂时不能保存"
        else -> null
    }
    val canSave = title.isNotBlank() && imageBlockingMessage == null
    val saveMemory = {
        onSave(
            Memory(
                id = initialMemory?.id ?: 0,
                title = title.trim(),
                dateEpochDay = date.toEpochDay(),
                scheduleId = initialMemory?.scheduleId,
                linkedWishId = initialMemory?.linkedWishId,
                locationName = locationName.trim(),
                photoUris = photoUris.trim(),
                foodNotes = initialMemory?.foodNotes.orEmpty(),
                expenseCents = initialMemory?.expenseCents,
                mood = mood,
                note = note.trim(),
                createdAt = initialMemory?.createdAt ?: 0,
                updatedAt = initialMemory?.updatedAt ?: 0,
                isDeleted = initialMemory?.isDeleted ?: false,
                remoteId = initialMemory?.remoteId,
                coupleId = initialMemory?.coupleId,
                ownerUserId = initialMemory?.ownerUserId,
                syncStatus = initialMemory?.syncStatus ?: com.yanye.home.domain.model.SyncStatus.Synced,
                remoteUpdatedAt = initialMemory?.remoteUpdatedAt
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
                title = if (initialMemory == null) "新增回忆" else "编辑回忆",
                actionText = "保存",
                actionColor = if (canSave) YanYeColors.Blue else YanYeColors.Muted,
                onBack = onBack,
                onActionClick = if (canSave) saveMemory else null
            )
        }
        item {
            Text(
                text = title.ifBlank { "未命名回忆" },
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            MemoryEditGroup(
                rows = listOf(
                    MemoryEditorRow.Text("标题", title, { title = it }),
                    MemoryEditorRow.Action("日期", formatIsoDate(date), { showDatePicker = true }),
                    MemoryEditorRow.Text("地点", locationName, { locationName = it }),
                    MemoryEditorRow.Text("内容", note, { note = it }, singleLine = false)
                )
            )
        }
        item {
            ImagePickerField(
                label = "照片",
                imageUri = photoUris,
                onImageUriChange = { photoUris = it },
                height = 300.dp,
                module = "memories",
                onUploadStateChange = { imageUploadState = it }
            )
        }
        imageBlockingMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = YanYeColors.Rose,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.6.dp, YanYeColors.Line),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("心情", color = YanYeColors.Muted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemoryMood.entries.forEach { option ->
                            FilterChip(
                                selected = mood == option,
                                onClick = { mood = option },
                                label = { Text(moodLabel(option)) }
                            )
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = saveMemory,
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
                Text("保存回忆", modifier = Modifier.padding(vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
    }

    if (showDatePicker) {
        DatePickerPopup(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                date = it
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun MemoryPhotoPreview(
    photoUri: String?,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 360.dp
) {
    ImagePreviewBox(
        imageUri = photoUri,
        contentDescription = "照片",
        modifier = modifier,
        minHeight = 0.dp,
        maxHeight = maxHeight
    )
}

@Composable
private fun MemoryMonthJumpDialog(
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onJump: (YearMonth) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(initialMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            tonalElevation = 0.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "跳转时间",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(YanYeColors.Soft, MaterialTheme.shapes.medium)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MemoryWheelColumn(
                            modifier = Modifier.weight(1f),
                            values = (-2..2).map { selectedMonth.year + it },
                            selectedValue = selectedMonth.year,
                            label = { "${it}年" },
                            onSelected = { selectedMonth = YearMonth.of(it, selectedMonth.monthValue) }
                        )
                        MemoryWheelColumn(
                            modifier = Modifier.weight(1f),
                            values = (-2..2).map { selectedMonth.plusMonths(it.toLong()) },
                            selectedValue = selectedMonth,
                            label = { "${it.monthValue}月" },
                            onSelected = { selectedMonth = it }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MemoryPickerShortcut(text = "本月") {
                        selectedMonth = YearMonth.now()
                    }
                    MemoryPickerShortcut(text = "上月") {
                        selectedMonth = YearMonth.now().minusMonths(1)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = YanYeColors.Rose, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { onJump(selectedMonth) }) {
                        Text("跳转", color = YanYeColors.Rose, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> MemoryWheelColumn(
    modifier: Modifier = Modifier,
    values: List<T>,
    selectedValue: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        values.forEach { value ->
            val selected = value == selectedValue
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable { onSelected(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(value),
                    color = if (selected) YanYeColors.Ink else YanYeColors.Muted.copy(alpha = if (values.indexOf(value) == 0 || values.indexOf(value) == values.lastIndex) 0.35f else 0.65f),
                    style = if (selected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MemoryPickerShortcut(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color.White, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = YanYeColors.Ink, style = MaterialTheme.typography.bodyMedium)
    }
}

private sealed interface MemoryEditorRow {
    data class Text(
        val label: String,
        val value: String,
        val onValueChange: (String) -> Unit,
        val singleLine: Boolean = true,
        val placeholder: String = "未设置"
    ) : MemoryEditorRow

    data class Action(
        val label: String,
        val value: String,
        val onClick: () -> Unit
    ) : MemoryEditorRow
}

@Composable
private fun MemoryEditGroup(rows: List<MemoryEditorRow>) {
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
                    is MemoryEditorRow.Action -> MemoryActionRow(row)
                    is MemoryEditorRow.Text -> MemoryTextRow(row)
                }
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = YanYeColors.Line, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun MemoryTextRow(row: MemoryEditorRow.Text) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (row.singleLine) 58.dp else 92.dp),
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
                .width(92.dp)
        )
        BasicTextField(
            value = row.value,
            onValueChange = row.onValueChange,
            singleLine = row.singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = YanYeColors.Ink, fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .weight(1f)
                .padding(top = if (row.singleLine) 0.dp else 18.dp, bottom = if (row.singleLine) 0.dp else 18.dp),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (row.value.isBlank()) {
                        Text(row.placeholder, color = YanYeColors.Muted.copy(alpha = 0.55f), style = MaterialTheme.typography.bodyLarge)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun MemoryActionRow(row: MemoryEditorRow.Action) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = row.onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(row.label, color = YanYeColors.Muted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(92.dp))
        Text(row.value, color = YanYeColors.Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("›", color = YanYeColors.Muted, style = MaterialTheme.typography.titleLarge)
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
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private fun firstPhotoUri(photoUris: String): String? =
    photoUris.split('\n', ',', ';').map(String::trim).firstOrNull { it.isNotBlank() }

private fun buildMemoryTimelineEntries(rows: List<Memory>): List<MemoryTimelineEntry> {
    val entries = mutableListOf<MemoryTimelineEntry>()
    rows.forEachIndexed { index, memory ->
        val year = LocalDate.ofEpochDay(memory.dateEpochDay).year
        val previousYear = rows.getOrNull(index - 1)?.let { LocalDate.ofEpochDay(it.dateEpochDay).year }
        if (previousYear != year) {
            entries += MemoryTimelineEntry.Year(year)
        }
        entries += MemoryTimelineEntry.Item(memory = memory, rowIndex = index)
    }
    return entries
}

private fun currentStickyYear(entries: List<MemoryTimelineEntry>, firstVisibleIndex: Int): Int? {
    if (entries.isEmpty()) return null
    return entries
        .take(firstVisibleIndex.coerceIn(0, entries.lastIndex) + 1)
        .lastOrNull { it is MemoryTimelineEntry.Year }
        ?.let { (it as MemoryTimelineEntry.Year).year }
        ?: (entries.firstOrNull() as? MemoryTimelineEntry.Year)?.year
}

private fun memoryEntryIndex(entries: List<MemoryTimelineEntry>, memoryId: Long): Int =
    entries.indexOfFirst { entry -> entry is MemoryTimelineEntry.Item && entry.memory.id == memoryId }
        .coerceAtLeast(0)

private fun isMonthStart(rows: List<Memory>, index: Int): Boolean {
    if (index !in rows.indices) return false
    val current = YearMonth.from(LocalDate.ofEpochDay(rows[index].dateEpochDay))
    val previous = rows.getOrNull(index - 1)?.let { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) }
    return previous != current
}

private fun memoryListMeta(memory: Memory): String {
    val date = LocalDate.ofEpochDay(memory.dateEpochDay)
    return buildList {
        add("${date.monthValue}/${date.dayOfMonth}")
        if (memory.locationName.isNotBlank()) add(memory.locationName)
    }.joinToString(" · ")
}

private fun memoryListSummary(memory: Memory): String =
    buildList {
        val body = memory.note.ifBlank { memory.foodNotes }
        if (body.isNotBlank()) {
            add(body)
        } else if (memory.scheduleId != null) {
            add("来自日程")
        }
        add(moodEmoji(memory.mood))
    }.joinToString(" · ")

private fun memoryDetailMeta(memory: Memory): String =
    buildList {
        add(formatChineseMonthDay(memory.dateEpochDay))
        if (memory.locationName.isNotBlank()) add(memory.locationName)
        add("${moodEmoji(memory.mood)}${moodLabel(memory.mood)}")
    }.joinToString(" · ")

private fun formatMonthDay(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return "${date.monthValue}/${date.dayOfMonth}"
}

private fun formatChineseMonthDay(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return "${date.monthValue}月${date.dayOfMonth}日"
}

private fun formatIsoDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun moodLabel(mood: MemoryMood): String =
    when (mood) {
        MemoryMood.Happy -> "开心"
        MemoryMood.Touched -> "感动"
        MemoryMood.TiredButWorth -> "累但值得"
        MemoryMood.Ordinary -> "普通"
        MemoryMood.Unwell -> "难受"
    }

private fun moodEmoji(mood: MemoryMood): String =
    when (mood) {
        MemoryMood.Happy -> "😊"
        MemoryMood.Touched -> "🥹"
        MemoryMood.TiredButWorth -> "✨"
        MemoryMood.Ordinary -> "🌿"
        MemoryMood.Unwell -> "😣"
    }

private fun budgetTextToCents(value: String): Long? {
    val normalized = value.trim()
    if (normalized.isBlank()) return null
    val amount = normalized.toBigDecimalOrNull() ?: return null
    return amount.multiply(java.math.BigDecimal(100)).toLong()
}

private fun centsToBudgetText(cents: Long): String =
    if (cents % 100 == 0L) {
        "${cents / 100}"
    } else {
        "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
    }
