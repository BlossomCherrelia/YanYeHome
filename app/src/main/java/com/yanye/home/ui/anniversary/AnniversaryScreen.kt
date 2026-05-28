package com.yanye.home.ui.anniversary

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.AnniversaryDisplayMode
import com.yanye.home.domain.model.AnniversaryType
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.common.HeaderTextAction
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.PrimaryPageHeader
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@Composable
fun AnniversaryScreen(
    viewModel: AnniversaryViewModel = viewModel()
) {
    val anniversaries by viewModel.anniversaries.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var editingAnniversary by remember { mutableStateOf<Anniversary?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncAnniversaries,
        onFlushSync = viewModel::flushSync
    )

    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PageChrome.primaryPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AnniversaryHeader(
                    syncMessage = syncMessage,
                    onSyncClick = viewModel::syncAnniversaries
                )
            }

            if (anniversaries.isEmpty()) {
                item {
                    EmptyAnniversaryCard(
                        onCreateClick = {
                            editingAnniversary = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(
                    items = anniversaries,
                    key = { anniversary -> anniversary.id }
                ) { anniversary ->
                    AnniversaryCard(
                        anniversary = anniversary,
                        onEditClick = {
                            editingAnniversary = anniversary
                            showEditor = true
                        },
                        onDeleteClick = { viewModel.deleteAnniversary(anniversary.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingAnniversary = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            containerColor = YanYeColors.Rose,
            contentColor = Color.White
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showEditor) {
        AnniversaryEditorDialog(
            initialAnniversary = editingAnniversary,
            onDismiss = { showEditor = false },
            onSave = { anniversary ->
                viewModel.saveAnniversary(anniversary)
                showEditor = false
            }
        )
    }
}

@Composable
private fun AnniversaryHeader(
    syncMessage: String?,
    onSyncClick: () -> Unit
) {
    PrimaryPageHeader(
        title = "纪念日",
        message = syncMessage
    ) {
        HeaderTextAction(text = "同步", color = YanYeColors.Rose, onClick = onSyncClick)
    }
}

@Composable
private fun EmptyAnniversaryCard(onCreateClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "从第一个共同日子开始",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "建议先建“在一起”，后续可以把礼物愿望和每年庆祝档案接到这里。",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = onCreateClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("新建纪念日")
            }
        }
    }
}

@Composable
private fun AnniversaryCard(
    anniversary: Anniversary,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = anniversaryStatusText(anniversary),
                        color = YanYeColors.Rose,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = anniversary.name,
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatEpochDay(anniversary.dateEpochDay),
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
private fun AnniversaryEditorDialog(
    initialAnniversary: Anniversary?,
    onDismiss: () -> Unit,
    onSave: (Anniversary) -> Unit
) {
    var name by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.name.orEmpty())
    }
    var date by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.dateEpochDay?.let(::localDateFromEpochDay) ?: LocalDate.now())
    }
    var type by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.type ?: AnniversaryType.Relationship)
    }
    var displayMode by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.displayMode ?: AnniversaryDisplayMode.CountUp)
    }
    var reminderDaysBefore by remember(initialAnniversary) {
        mutableStateOf((initialAnniversary?.reminderDaysBefore ?: 7).toString())
    }
    var coverImageUri by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.coverImageUri.orEmpty())
    }
    var note by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.note.orEmpty())
    }
    var giftWishLinkEnabled by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.giftWishLinkEnabled ?: true)
    }
    var celebrationArchiveEnabled by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.celebrationArchiveEnabled ?: true)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val canSave = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialAnniversary == null) "新建纪念日" else "编辑纪念日") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickNameChip("在一起") {
                            name = "在一起"
                            type = AnniversaryType.Relationship
                            displayMode = AnniversaryDisplayMode.CountUp
                        }
                        QuickNameChip("生日") {
                            name = "生日"
                            type = AnniversaryType.Birthday
                            displayMode = AnniversaryDisplayMode.Anniversary
                        }
                        QuickNameChip("第一次旅行") {
                            name = "第一次旅行"
                            type = AnniversaryType.Travel
                            displayMode = AnniversaryDisplayMode.Anniversary
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("纪念日名字") }
                    )
                }
                item {
                    Button(onClick = { showDatePicker = true }) {
                        Text("日期：${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                    }
                }
                item {
                    ChipGroupTitle("展示方式")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnniversaryDisplayMode.entries.forEach { mode ->
                            FilterChip(
                                selected = displayMode == mode,
                                onClick = { displayMode = mode },
                                label = { Text(displayModeLabel(mode)) }
                            )
                        }
                    }
                }
                item {
                    ChipGroupTitle("类型")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnniversaryType.entries.forEach { anniversaryType ->
                            FilterChip(
                                selected = type == anniversaryType,
                                onClick = { type = anniversaryType },
                                label = { Text(typeLabel(anniversaryType)) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = reminderDaysBefore,
                        onValueChange = { value ->
                            reminderDaysBefore = value.filter(Char::isDigit).take(3)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("提前几天提醒") }
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
                /*
                item {
                    ToggleRow(
                        title = "预留礼物愿望联动",
                        checked = giftWishLinkEnabled,
                        onCheckedChange = { giftWishLinkEnabled = it }
                    )
                }
                item {
                    ToggleRow(
                        title = "预留庆祝档案入口",
                        checked = celebrationArchiveEnabled,
                        onCheckedChange = { celebrationArchiveEnabled = it }
                    )
                }
                */
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        Anniversary(
                            id = initialAnniversary?.id ?: 0,
                            name = name.trim(),
                            dateEpochDay = date.toEpochDay(),
                            type = type,
                            displayMode = displayMode,
                            reminderDaysBefore = reminderDaysBefore.toIntOrNull() ?: 7,
                            note = note.trim(),
                            coverImageUri = coverImageUri.trim().takeIf(String::isNotEmpty),
                            giftWishLinkEnabled = giftWishLinkEnabled,
                            celebrationArchiveEnabled = celebrationArchiveEnabled,
                            createdAt = initialAnniversary?.createdAt ?: 0,
                            updatedAt = initialAnniversary?.updatedAt ?: 0,
                            visibility = initialAnniversary?.visibility ?: com.yanye.home.domain.model.Visibility.Shared,
                            sharedWithPartner = initialAnniversary?.sharedWithPartner ?: true,
                            lockedUntilEpochDay = initialAnniversary?.lockedUntilEpochDay,
                            isDeleted = initialAnniversary?.isDeleted ?: false,
                            remoteId = initialAnniversary?.remoteId,
                            coupleId = initialAnniversary?.coupleId,
                            ownerUserId = initialAnniversary?.ownerUserId,
                            syncStatus = initialAnniversary?.syncStatus ?: com.yanye.home.domain.model.SyncStatus.Synced,
                            remoteUpdatedAt = initialAnniversary?.remoteUpdatedAt
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun QuickNameChip(
    label: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) }
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

private fun anniversaryStatusText(anniversary: Anniversary): String {
    val date = localDateFromEpochDay(anniversary.dateEpochDay)
    val today = LocalDate.now()
    return when (anniversary.displayMode) {
        AnniversaryDisplayMode.Countdown -> {
            val days = ChronoUnit.DAYS.between(today, nextOccurrenceDate(date, today))
            "还有 $days 天"
        }
        AnniversaryDisplayMode.CountUp -> {
            val days = ChronoUnit.DAYS.between(date, today)
            if (days >= 0) "已经 $days 天" else "还有 ${abs(days)} 天开始"
        }
        AnniversaryDisplayMode.Anniversary -> anniversaryYearsText(date, today)
    }
}

private fun anniversaryYearsText(date: LocalDate, today: LocalDate): String {
    if (date.isAfter(today)) {
        return "未开始"
    }
    val years = Period.between(date, today).years
    return if (years <= 0) "未满 1 年" else "已满 $years 年"
}

private fun nextOccurrenceDate(date: LocalDate, today: LocalDate): LocalDate {
    if (date.isAfter(today)) return date
    var next = date.withYear(today.year)
    if (!next.isAfter(today)) {
        next = next.plusYears(1)
    }
    return next
}

private fun daysUntilNextOccurrence(epochDay: Long): Long {
    val date = localDateFromEpochDay(epochDay)
    val today = LocalDate.now()
    var next = date.withYear(today.year)
    if (next.isBefore(today)) {
        next = next.plusYears(1)
    }
    return ChronoUnit.DAYS.between(today, next)
}

private fun localDateFromEpochDay(epochDay: Long): LocalDate =
    LocalDate.ofEpochDay(epochDay)

private fun formatEpochDay(epochDay: Long): String =
    localDateFromEpochDay(epochDay).format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun displayModeLabel(mode: AnniversaryDisplayMode): String =
    when (mode) {
        AnniversaryDisplayMode.Countdown -> "倒数"
        AnniversaryDisplayMode.CountUp -> "正数"
        AnniversaryDisplayMode.Anniversary -> "周年"
    }

private fun typeLabel(type: AnniversaryType): String =
    when (type) {
        AnniversaryType.Relationship -> "恋爱"
        AnniversaryType.Birthday -> "生日"
        AnniversaryType.Travel -> "旅行"
        AnniversaryType.Custom -> "自定义"
    }

private fun typeEmoji(type: AnniversaryType): String =
    when (type) {
        AnniversaryType.Relationship -> "恋"
        AnniversaryType.Birthday -> "生"
        AnniversaryType.Travel -> "旅"
        AnniversaryType.Custom -> "记"
    }

private fun typeSoftColor(type: AnniversaryType): Color =
    when (type) {
        AnniversaryType.Relationship -> YanYeColors.RoseSoft
        AnniversaryType.Birthday -> YanYeColors.GoldSoft
        AnniversaryType.Travel -> YanYeColors.GreenSoft
        AnniversaryType.Custom -> YanYeColors.BlueSoft
    }
