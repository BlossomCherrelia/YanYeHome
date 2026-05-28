package com.yanye.home.ui.wish

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.domain.model.Schedule
import com.yanye.home.domain.model.Visibility
import com.yanye.home.domain.model.Wish
import com.yanye.home.domain.model.WishCategory
import com.yanye.home.domain.model.WishPriority
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.common.ImagePickerField
import com.yanye.home.ui.common.ImageUploadState
import com.yanye.home.ui.common.isLocalOnlyImageUri
import com.yanye.home.ui.common.ImagePreviewBox
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.SecondaryTopBar
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed interface WishPage {
    data object List : WishPage
    data class Detail(val wish: Wish) : WishPage
    data class Editor(val wish: Wish?) : WishPage
}

@Composable
fun WishScreen(
    viewModel: WishViewModel = viewModel(),
    onBack: () -> Unit = {},
    onOpenSchedule: () -> Unit = {}
) {
    val wishes by viewModel.wishes.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var selectedCategory by remember { mutableStateOf<WishCategory?>(null) }
    var selectedVisibility by remember { mutableStateOf<Visibility?>(null) }
    var showCompleted by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf<WishPage>(WishPage.List) }
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncWishes,
        onFlushSync = viewModel::flushSync
    )
    val visibleWishes = wishes
        .filter { wish -> showCompleted || !wish.isCompleted }
        .filter { wish -> selectedCategory == null || wish.category == selectedCategory }
        .filter { wish -> selectedVisibility == null || wish.visibility == selectedVisibility }

    when (val currentPage = page) {
        WishPage.List -> {
            WishListPage(
                visibleWishes = visibleWishes,
                selectedCategory = selectedCategory,
                selectedVisibility = selectedVisibility,
                showCompleted = showCompleted,
                syncMessage = syncMessage,
                onBack = onBack,
                onCategorySelected = { selectedCategory = it },
                onVisibilitySelected = { selectedVisibility = it },
                onShowCompletedChange = { showCompleted = it },
                onWishClick = { page = WishPage.Detail(it) },
                onCreateClick = { page = WishPage.Editor(null) }
            )
        }
        is WishPage.Detail -> {
            val wish = wishes.firstOrNull { it.id == currentPage.wish.id } ?: currentPage.wish
            val linkedSchedule = schedules.firstOrNull { it.id == wish.linkedScheduleId }
            WishDetailPage(
                wish = wish,
                linkedSchedule = linkedSchedule,
                onBack = { page = WishPage.List },
                onEdit = { page = WishPage.Editor(wish) },
                onCompletedChange = { completed -> viewModel.setWishCompleted(wish.id, completed) },
                onScheduleClick = onOpenSchedule,
                onDelete = {
                    viewModel.deleteWish(wish.id)
                    page = WishPage.List
                }
            )
        }
        is WishPage.Editor -> {
            WishEditorPage(
                initialWish = currentPage.wish,
                onBack = { page = WishPage.List },
                onSave = { wish ->
                    viewModel.saveWish(wish)
                    page = WishPage.List
                }
            )
        }
    }
}

@Composable
private fun WishListPage(
    visibleWishes: List<Wish>,
    selectedCategory: WishCategory?,
    selectedVisibility: Visibility?,
    showCompleted: Boolean,
    syncMessage: String?,
    onBack: () -> Unit,
    onCategorySelected: (WishCategory?) -> Unit,
    onVisibilitySelected: (Visibility?) -> Unit,
    onShowCompletedChange: (Boolean) -> Unit,
    onWishClick: (Wish) -> Unit,
    onCreateClick: () -> Unit
) {
    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PageChrome.secondaryPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                WishHeader(
                    syncMessage = syncMessage,
                    onBack = onBack,
                    onCreateClick = onCreateClick
                )
            }

            item {
                WishFilters(
                    selectedCategory = selectedCategory,
                    selectedVisibility = selectedVisibility,
                    showCompleted = showCompleted,
                    onCategorySelected = onCategorySelected,
                    onVisibilitySelected = onVisibilitySelected,
                    onShowCompletedChange = onShowCompletedChange
                )
            }

            if (visibleWishes.isEmpty()) {
                item { EmptyWishCard(onCreateClick = onCreateClick) }
            } else {
                items(items = visibleWishes, key = { wish -> wish.id }) { wish ->
                    WishCard(wish = wish, onClick = { onWishClick(wish) })
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            containerColor = Color(0xFFFF7FA1),
            contentColor = Color.White
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun WishHeader(
    syncMessage: String?,
    onBack: () -> Unit,
    onCreateClick: () -> Unit
) {
    Column {
        SecondaryTopBar(
            title = "愿望清单",
            actionText = "+",
            actionColor = YanYeColors.Muted,
            onBack = onBack,
            onActionClick = onCreateClick
        )
        syncMessage?.let {
            Text(
                text = it,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun WishFilters(
    selectedCategory: WishCategory?,
    selectedVisibility: Visibility?,
    showCompleted: Boolean,
    onCategorySelected: (WishCategory?) -> Unit,
    onVisibilitySelected: (Visibility?) -> Unit,
    onShowCompletedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WishDropdownFilter(
                value = selectedCategory?.let(::categoryLabel) ?: "全部类别",
                options = listOf<Pair<String, WishCategory?>>("全部类别" to null) +
                    WishCategory.entries.map { categoryLabel(it) to it },
                onSelected = onCategorySelected,
                modifier = Modifier.weight(1f)
            )
            WishDropdownFilter(
                value = selectedVisibility?.let(::visibilityFilterLabel) ?: "全部可见",
                options = listOf<Pair<String, Visibility?>>("全部可见" to null) +
                    listOf(
                        "双方可见" to Visibility.Shared,
                        "仅自己可见" to Visibility.Private,
                        "指定时效可见" to Visibility.RevealAfterDate
                    ),
                onSelected = onVisibilitySelected,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "显示已完成",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = showCompleted,
                onCheckedChange = onShowCompletedChange,
                modifier = Modifier.scale(0.78f)
            )
        }
    }
}

@Composable
private fun <T> WishDropdownFilter(
    value: String,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.9f), MaterialTheme.shapes.medium)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "⌄",
                color = Color(0xFFFF7FA1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth(0.42f)
        ) {
            options.forEach { (text, option) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = text,
                            color = YanYeColors.Ink,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WishFilterTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (selected) Color.White else YanYeColors.Muted,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(if (selected) YanYeColors.Ink else YanYeColors.Soft, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    )
}

@Composable
private fun EmptyWishCard(onCreateClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "先放一个小愿望",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "比如一顿想吃很久的饭、一次周末散步、一个礼物灵感。",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = onCreateClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("新建愿望")
            }
        }
    }
}

@Composable
private fun WishCard(
    wish: Wish,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 17.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wish.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.5.sp, lineHeight = 22.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (wish.isCompleted) TextDecoration.LineThrough else null
                )
                Text(
                    text = wishListMetaLine(wish),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
            WishStatusBadge(wish)
        }
    }
}

@Composable
private fun WishStatusBadge(wish: Wish) {
    val text = when {
        wish.isCompleted -> "已完成"
        wish.linkedScheduleId != null -> "已安排"
        else -> "待确认"
    }
    val background = when {
        wish.isCompleted -> YanYeColors.GreenSoft
        wish.linkedScheduleId != null -> YanYeColors.BlueSoft
        else -> YanYeColors.Soft
    }
    val content = when {
        wish.isCompleted -> YanYeColors.Green
        wish.linkedScheduleId != null -> YanYeColors.Blue
        else -> YanYeColors.Muted
    }
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(background, MaterialTheme.shapes.small)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    )
}

@Composable
private fun WishDetailPage(
    wish: Wish,
    linkedSchedule: Schedule?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onScheduleClick: () -> Unit,
    onDelete: () -> Unit
) {
    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PageChrome.secondaryPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SecondaryTopBar(
                title = "愿望详情",
                actionText = "编辑",
                actionColor = YanYeColors.Muted,
                onBack = onBack,
                onActionClick = onEdit
            )
        }
        item {
            Column {
                Text(
                    text = wish.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WishPill(categoryLabel(wish.category), YanYeColors.BlueSoft, YanYeColors.Blue)
                    WishPill(visibilityLabel(wish), YanYeColors.GreenSoft, YanYeColors.Green)
                    if (wish.isCompleted) WishPill("已完成", YanYeColors.GreenSoft, YanYeColors.Green)
                }
            }
        }
        if (!wish.coverImageUri.isNullOrBlank()) {
            item {
                ImagePreviewBox(
                    imageUri = wish.coverImageUri,
                    contentDescription = "愿望封面",
                    modifier = Modifier.fillMaxWidth(),
                    maxHeight = 280.dp
                )
            }
        }
        item {
            WishDetailGroup(
                rows = buildList {
                    wish.budgetCents?.let { add("预算" to "¥${centsToBudgetText(it)}") }
                    if (wish.locationName.isNotBlank()) add("地点" to wish.locationName)
                    if (wish.note.isNotBlank()) add("描述" to wish.note)
                    wish.targetDateEpochDay?.let { add("目标日期" to formatDate(localDateFromEpochDay(it))) }
                    if (wish.preparationItems.isNotBlank()) add("准备事项" to wish.preparationItems)
                    add("优先级" to priorityLabel(wish.priority))
                }
            )
        }
        item {
            Button(
                onClick = { onCompletedChange(!wish.isCompleted) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (wish.isCompleted) YanYeColors.Soft else YanYeColors.GreenSoft,
                    contentColor = if (wish.isCompleted) YanYeColors.Muted else YanYeColors.Green
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (wish.isCompleted) "取消完成愿望" else "完成愿望",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        item { HorizontalDivider(color = YanYeColors.Line, thickness = 0.8.dp) }
        item {
            Text(
                text = "关联日程",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onScheduleClick),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.6.dp, YanYeColors.Line),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = linkedSchedule?.let { "${formatDate(localDateFromEpochDay(it.startEpochDay))} ${it.title}" }
                            ?: wish.linkedScheduleId?.let { "日程 ID $it" }
                            ?: "还没有关联日程，去日历安排",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(YanYeColors.Rose, MaterialTheme.shapes.small)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        item {
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                Text("删除愿望", color = YanYeColors.Rose)
            }
        }
    }
    }
}

@Composable
private fun WishPill(
    text: String,
    background: Color,
    color: Color
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(background, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun WishDetailGroup(rows: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            rows.forEachIndexed { index, row ->
                WishDetailRow(label = row.first, value = row.second)
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = YanYeColors.Line, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun WishDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(74.dp)
        )
        Text(
            text = value,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishEditorPage(
    initialWish: Wish?,
    onBack: () -> Unit,
    onSave: (Wish) -> Unit
) {
    var title by remember(initialWish) { mutableStateOf(initialWish?.title ?: "") }
    var category by remember(initialWish) { mutableStateOf(initialWish?.category ?: WishCategory.Restaurant) }
    var visibility by remember(initialWish) { mutableStateOf(initialWish?.visibility ?: Visibility.Shared) }
    var revealAfterDate by remember(initialWish) {
        mutableStateOf(initialWish?.revealAfterEpochDay?.let(::localDateFromEpochDay) ?: LocalDate.now().plusDays(7))
    }
    var budget by remember(initialWish) {
        mutableStateOf(initialWish?.budgetCents?.let { cents -> centsToBudgetText(cents) }.orEmpty())
    }
    var locationName by remember(initialWish) { mutableStateOf(initialWish?.locationName.orEmpty()) }
    var priority by remember(initialWish) { mutableStateOf(initialWish?.priority ?: WishPriority.Medium) }
    var targetDate by remember(initialWish) {
        mutableStateOf(initialWish?.targetDateEpochDay?.let(::localDateFromEpochDay))
    }
    var note by remember(initialWish) { mutableStateOf(initialWish?.note.orEmpty()) }
    var preparationItems by remember(initialWish) { mutableStateOf(initialWish?.preparationItems.orEmpty()) }
    var scheduleReady by remember(initialWish) { mutableStateOf(initialWish?.scheduleReady ?: true) }
    var coverImageUri by remember(initialWish) { mutableStateOf(initialWish?.coverImageUri.orEmpty()) }
    var imageUploadState by remember { mutableStateOf(ImageUploadState()) }
    var showRevealDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }
    val hasPendingLocalImage = coverImageUri.isNotBlank() && isLocalOnlyImageUri(coverImageUri)
    val imageBlockingMessage = when {
        imageUploadState.isUploading -> "图片上传中，请稍后再保存"
        imageUploadState.errorMessage != null -> "图片上传失败，请重试或移除图片"
        hasPendingLocalImage -> "图片尚未变成云端地址，暂时不能保存"
        else -> null
    }
    val canSave = title.isNotBlank() && imageBlockingMessage == null
    val saveWish = {
        onSave(
            Wish(
                id = initialWish?.id ?: 0,
                title = title.trim(),
                category = category,
                visibility = visibility,
                revealAfterEpochDay = revealAfterDate.toEpochDay()
                    .takeIf { visibility == Visibility.RevealAfterDate },
                budgetCents = budgetTextToCents(budget),
                locationName = locationName.trim(),
                priority = priority,
                targetDateEpochDay = targetDate?.toEpochDay(),
                note = note.trim(),
                preparationItems = preparationItems.trim(),
                coverImageUri = coverImageUri.trim().takeIf(String::isNotEmpty),
                isCompleted = initialWish?.isCompleted ?: false,
                scheduleReady = scheduleReady,
                linkedScheduleId = initialWish?.linkedScheduleId,
                giftCandidateForAnniversaryId = initialWish?.giftCandidateForAnniversaryId,
                createdBy = initialWish?.createdBy,
                createdAt = initialWish?.createdAt ?: 0,
                updatedAt = initialWish?.updatedAt ?: 0,
                sharedWithPartner = visibility != Visibility.Private,
                isDeleted = initialWish?.isDeleted ?: false,
                remoteId = initialWish?.remoteId,
                coupleId = initialWish?.coupleId,
                ownerUserId = initialWish?.ownerUserId,
                syncStatus = initialWish?.syncStatus ?: com.yanye.home.domain.model.SyncStatus.Synced,
                remoteUpdatedAt = initialWish?.remoteUpdatedAt
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
                title = if (initialWish == null) "新增愿望" else "编辑愿望",
                actionText = "保存",
                actionColor = if (canSave) YanYeColors.Blue else YanYeColors.Muted,
                onBack = onBack,
                onActionClick = if (canSave) saveWish else null
            )
        }
        item {
            Column {
                Text(
                    text = title.ifBlank { "未命名愿望" },
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${categoryLabel(category)} · ${visibilityLabel(visibility, revealAfterDate)}",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickWishChip("想吃的店", WishCategory.Restaurant) {
                    title = "想吃的店"
                    category = WishCategory.Restaurant
                }
                QuickWishChip("礼物灵感", WishCategory.Gift) {
                    title = "礼物灵感"
                    category = WishCategory.Gift
                    visibility = Visibility.Private
                }
                QuickWishChip("周末小计划", WishCategory.Travel) {
                    title = "周末小计划"
                    category = WishCategory.Travel
                    scheduleReady = true
                }
            }
        }
        item {
            WishOptionGroup(
                title = "分类",
                options = WishCategory.entries,
                selected = category,
                label = ::categoryLabel,
                onSelected = { category = it }
            )
        }
        item {
            WishOptionGroup(
                title = "可见性",
                options = listOf(Visibility.Shared, Visibility.Private, Visibility.RevealAfterDate),
                selected = visibility,
                label = { visibilityLabel(it, revealAfterDate) },
                onSelected = { visibility = it }
            )
        }
        if (visibility == Visibility.RevealAfterDate) {
            item {
                WishEditGroup(
                    rows = listOf(
                        WishEditorRow.Action("可见日期", formatDate(revealAfterDate), { showRevealDatePicker = true })
                    )
                )
            }
        }
        item {
            WishOptionGroup(
                title = "优先级",
                options = WishPriority.entries,
                selected = priority,
                label = ::priorityLabel,
                onSelected = { priority = it }
            )
        }
        item {
            WishEditGroup(
                rows = listOf(
                    WishEditorRow.Text("标题", title, { title = it }),
                    WishEditorRow.Text("预算", budget, { value -> budget = value.filter { it.isDigit() || it == '.' }.take(10) }),
                    WishEditorRow.Text("地点", locationName, { locationName = it }),
                    WishEditorRow.Action("目标日期", targetDate?.let(::formatDate) ?: "未设置", { showTargetDatePicker = true }),
                    WishEditorRow.Text("准备事项", preparationItems, { preparationItems = it }, singleLine = false),
                    WishEditorRow.Text("备注", note, { note = it }, singleLine = false)
                )
            )
        }
        item {
            ImagePickerField(
                label = "封面图片",
                imageUri = coverImageUri,
                onImageUriChange = { coverImageUri = it },
                height = 280.dp,
                module = "wishes",
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
            WishToggleCard(
                title = "可转日程",
                checked = scheduleReady,
                onCheckedChange = { scheduleReady = it }
            )
        }
        item {
            Button(
                onClick = saveWish,
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
                    text = "保存愿望",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
    }

    if (showRevealDatePicker) {
        DatePickerPopup(
            initialDate = revealAfterDate,
            onDismiss = { showRevealDatePicker = false },
            onDateSelected = { selected ->
                revealAfterDate = selected
                showRevealDatePicker = false
            }
        )
    }

    if (showTargetDatePicker) {
        DatePickerPopup(
            initialDate = targetDate ?: LocalDate.now(),
            onDismiss = { showTargetDatePicker = false },
            onDateSelected = { selected ->
                targetDate = selected
                showTargetDatePicker = false
            }
        )
    }
}

@Composable
private fun QuickWishChip(
    label: String,
    category: WishCategory,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text("${categoryShortLabel(category)} $label") }
    )
}

@Composable
private fun <T> WishOptionGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelected(option) },
                        label = { Text(label(option)) }
                    )
                }
            }
        }
    }
}

private sealed interface WishEditorRow {
    data class Text(
        val label: String,
        val value: String,
        val onValueChange: (String) -> Unit,
        val singleLine: Boolean = true
    ) : WishEditorRow

    data class Action(
        val label: String,
        val value: String,
        val onClick: () -> Unit
    ) : WishEditorRow
}

@Composable
private fun WishEditGroup(rows: List<WishEditorRow>) {
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
                    is WishEditorRow.Text -> WishEditTextRow(row)
                    is WishEditorRow.Action -> WishEditActionRow(row)
                }
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = YanYeColors.Line, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun WishEditTextRow(row: WishEditorRow.Text) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = if (row.singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        Text(
            text = row.label,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .width(74.dp)
                .padding(top = if (row.singleLine) 0.dp else 14.dp)
        )
        OutlinedTextField(
            value = row.value,
            onValueChange = row.onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = row.singleLine,
            minLines = if (row.singleLine) 1 else 2,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = YanYeColors.Ink,
                fontWeight = FontWeight.SemiBold
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = YanYeColors.Ink
            )
        )
    }
}

@Composable
private fun WishEditActionRow(row: WishEditorRow.Action) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = row.onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.label,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(74.dp)
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
private fun WishToggleCard(
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.76f)
            )
        }
    }
}

@Composable
private fun ChipGroupTitle(title: String) {
    Text(
        text = title,
        color = YanYeColors.Muted,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
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
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
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

private fun wishMetaLine(wish: Wish): String {
    val parts = buildList {
        wish.budgetCents?.let { add("预算 ${centsToBudgetText(it)}") }
        if (wish.locationName.isNotBlank()) add(wish.locationName)
        wish.targetDateEpochDay?.let { add("目标 ${formatDate(localDateFromEpochDay(it))}") }
    }
    return parts.ifEmpty { listOf("还没有预算、地点或目标日期") }.joinToString(" · ")
}

private fun wishListMetaLine(wish: Wish): String {
    val parts = buildList {
        add(categoryLabel(wish.category))
        wish.budgetCents?.let { add("¥${centsToBudgetText(it)}") }
    }
    return parts.joinToString(" · ")
}

private fun visibilityLabel(wish: Wish): String =
    when (wish.visibility) {
        Visibility.Shared -> "双方可见"
        Visibility.Private -> "仅自己可见"
        Visibility.Partial -> "部分共享"
        Visibility.RevealAfterDate -> {
            val date = wish.revealAfterEpochDay?.let { formatDate(localDateFromEpochDay(it)) }
            if (date == null) "指定日期后可见" else "$date 后可见"
        }
    }

private fun visibilityLabel(
    visibility: Visibility,
    revealAfterDate: LocalDate
): String =
    when (visibility) {
        Visibility.Shared -> "双方可见"
        Visibility.Private -> "仅自己可见"
        Visibility.Partial -> "部分共享"
        Visibility.RevealAfterDate -> "${formatDate(revealAfterDate)} 后可见"
    }

private fun visibilityFilterLabel(visibility: Visibility): String =
    when (visibility) {
        Visibility.Shared -> "双方可见"
        Visibility.Private -> "仅自己可见"
        Visibility.Partial -> "部分共享"
        Visibility.RevealAfterDate -> "指定时效可见"
    }

private fun categoryLabel(category: WishCategory): String =
    when (category) {
        WishCategory.Shopping -> "购物"
        WishCategory.Travel -> "游玩"
        WishCategory.Restaurant -> "餐厅"
        WishCategory.Gift -> "礼物"
        WishCategory.Home -> "家具"
        WishCategory.Pet -> "养狗"
        WishCategory.Movie -> "电影"
        WishCategory.Game -> "游戏"
        WishCategory.Custom -> "其他"
    }

private fun categoryShortLabel(category: WishCategory): String =
    when (category) {
        WishCategory.Shopping -> "购"
        WishCategory.Travel -> "游"
        WishCategory.Restaurant -> "餐"
        WishCategory.Gift -> "礼"
        WishCategory.Home -> "家"
        WishCategory.Pet -> "宠"
        WishCategory.Movie -> "影"
        WishCategory.Game -> "玩"
        WishCategory.Custom -> "愿"
    }

private fun categorySoftColor(category: WishCategory): Color =
    when (category) {
        WishCategory.Shopping -> YanYeColors.RoseSoft
        WishCategory.Travel -> YanYeColors.GreenSoft
        WishCategory.Restaurant -> YanYeColors.GoldSoft
        WishCategory.Gift -> YanYeColors.RoseSoft
        WishCategory.Home -> YanYeColors.BlueSoft
        WishCategory.Pet -> YanYeColors.GreenSoft
        WishCategory.Movie -> YanYeColors.BlueSoft
        WishCategory.Game -> YanYeColors.GoldSoft
        WishCategory.Custom -> YanYeColors.Soft
    }

private fun priorityLabel(priority: WishPriority): String =
    when (priority) {
        WishPriority.Low -> "低优先级"
        WishPriority.Medium -> "普通"
        WishPriority.High -> "高优先级"
    }

private fun localDateFromEpochDay(epochDay: Long): LocalDate =
    LocalDate.ofEpochDay(epochDay)

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun budgetTextToCents(value: String): Long? {
    val normalized = value.trim()
    if (normalized.isEmpty()) return null
    return normalized.toBigDecimalOrNull()
        ?.movePointRight(2)
        ?.toLong()
}

private fun centsToBudgetText(cents: Long): String {
    val yuan = cents / 100
    val fen = cents % 100
    return if (fen == 0L) yuan.toString() else "$yuan.${fen.toString().padStart(2, '0')}"
}
