package com.yanye.home.ui.wish

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.domain.model.Visibility
import com.yanye.home.domain.model.Wish
import com.yanye.home.domain.model.WishCategory
import com.yanye.home.domain.model.WishPriority
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.theme.YanYeColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WishScreen(
    viewModel: WishViewModel = viewModel()
) {
    val wishes by viewModel.wishes.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var selectedCategory by remember { mutableStateOf<WishCategory?>(null) }
    var showCompleted by remember { mutableStateOf(false) }
    var editingWish by remember { mutableStateOf<Wish?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncWishes,
        onFlushSync = viewModel::flushSync
    )
    val visibleWishes = wishes
        .filter { wish -> showCompleted || !wish.isCompleted }
        .filter { wish -> selectedCategory == null || wish.category == selectedCategory }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YanYeColors.Paper)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, top = 28.dp, end = 22.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WishHeader(
                    totalCount = wishes.size,
                    activeCount = wishes.count { !it.isCompleted },
                    syncMessage = syncMessage,
                    onSyncClick = viewModel::syncWishes
                )
            }

            item {
                WishFilters(
                    selectedCategory = selectedCategory,
                    showCompleted = showCompleted,
                    onCategorySelected = { selectedCategory = it },
                    onShowCompletedChange = { showCompleted = it }
                )
            }

            if (visibleWishes.isEmpty()) {
                item {
                    EmptyWishCard(
                        onCreateClick = {
                            editingWish = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(
                    items = visibleWishes,
                    key = { wish -> wish.id }
                ) { wish ->
                    WishCard(
                        wish = wish,
                        onCompletedChange = { completed ->
                            viewModel.setWishCompleted(wish.id, completed)
                        },
                        onEditClick = {
                            editingWish = wish
                            showEditor = true
                        },
                        onDeleteClick = { viewModel.deleteWish(wish.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingWish = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            containerColor = YanYeColors.Blue,
            contentColor = Color.White
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showEditor) {
        WishEditorDialog(
            initialWish = editingWish,
            onDismiss = { showEditor = false },
            onSave = { wish ->
                viewModel.saveWish(wish)
                showEditor = false
            }
        )
    }
}

@Composable
private fun WishHeader(
    totalCount: Int,
    activeCount: Int,
    syncMessage: String?,
    onSyncClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "愿望清单",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSyncClick) {
                Text("同步", color = YanYeColors.Blue)
            }
        }
        Text(
            text = "把想买、想去、想吃、想一起完成的灵感先安放在这里。",
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (totalCount == 0) "还没有愿望" else "$activeCount 个待完成愿望",
                    color = YanYeColors.Blue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = syncMessage ?: "重要愿望可以标成高优先级，也可以先设成仅自己可见或到指定日期后可见。",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun WishFilters(
    selectedCategory: WishCategory?,
    showCompleted: Boolean,
    onCategorySelected: (WishCategory?) -> Unit,
    onShowCompletedChange: (Boolean) -> Unit
) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("全部") }
            )
            WishCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(categoryLabel(category)) }
                )
            }
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
                onCheckedChange = onShowCompletedChange
            )
        }
    }
}

@Composable
private fun EmptyWishCard(onCreateClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
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
    onCompletedChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(categorySoftColor(wish.category), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = categoryShortLabel(wish.category),
                        style = MaterialTheme.typography.titleMedium,
                        color = YanYeColors.Ink,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wish.title,
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (wish.isCompleted) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = wishMetaLine(wish),
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = wish.isCompleted,
                    onCheckedChange = onCompletedChange
                )
            }

            if (wish.note.isNotBlank()) {
                Text(
                    text = wish.note,
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(categoryLabel(wish.category)) })
                AssistChip(onClick = {}, label = { Text(priorityLabel(wish.priority)) })
                AssistChip(onClick = {}, label = { Text(visibilityLabel(wish)) })
                if (wish.scheduleReady) {
                    AssistChip(onClick = {}, label = { Text("可安排") })
                }
                if (wish.giftCandidateForAnniversaryId != null || wish.category == WishCategory.Gift) {
                    AssistChip(onClick = {}, label = { Text("礼物灵感") })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEditClick) {
                    Text("编辑")
                }
                TextButton(onClick = onDeleteClick) {
                    Text("删除", color = YanYeColors.Rose)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishEditorDialog(
    initialWish: Wish?,
    onDismiss: () -> Unit,
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
    var showRevealDatePicker by remember { mutableStateOf(false) }
    var showTargetDatePicker by remember { mutableStateOf(false) }
    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialWish == null) "新建愿望" else "编辑愿望") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("愿望标题") }
                    )
                }
                item {
                    ChipGroupTitle("分类")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WishCategory.entries.forEach { wishCategory ->
                            FilterChip(
                                selected = category == wishCategory,
                                onClick = { category = wishCategory },
                                label = { Text(categoryLabel(wishCategory)) }
                            )
                        }
                    }
                }
                item {
                    ChipGroupTitle("可见性")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = visibility == Visibility.Shared,
                            onClick = { visibility = Visibility.Shared },
                            label = { Text("双方可见") }
                        )
                        FilterChip(
                            selected = visibility == Visibility.Private,
                            onClick = { visibility = Visibility.Private },
                            label = { Text("仅自己可见") }
                        )
                        FilterChip(
                            selected = visibility == Visibility.RevealAfterDate,
                            onClick = { visibility = Visibility.RevealAfterDate },
                            label = { Text("指定日期后可见") }
                        )
                    }
                }
                if (visibility == Visibility.RevealAfterDate) {
                    item {
                        Button(onClick = { showRevealDatePicker = true }) {
                            Text("可见日期：${formatDate(revealAfterDate)}")
                        }
                    }
                }
                item {
                    ChipGroupTitle("优先级")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WishPriority.entries.forEach { wishPriority ->
                            FilterChip(
                                selected = priority == wishPriority,
                                onClick = { priority = wishPriority },
                                label = { Text(priorityLabel(wishPriority)) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { value ->
                            budget = value.filter { it.isDigit() || it == '.' }.take(10)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("预算") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("地点") }
                    )
                }
                item {
                    Button(onClick = { showTargetDatePicker = true }) {
                        Text("目标日期：${targetDate?.let(::formatDate) ?: "未设置"}")
                    }
                }
                item {
                    OutlinedTextField(
                        value = preparationItems,
                        onValueChange = { preparationItems = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("准备事项") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = coverImageUri,
                        onValueChange = { coverImageUri = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("封面图片路径或 URI") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("备注") }
                    )
                }
                item {
                    ToggleRow(
                        title = "可转日程",
                        checked = scheduleReady,
                        onCheckedChange = { scheduleReady = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
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
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

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
