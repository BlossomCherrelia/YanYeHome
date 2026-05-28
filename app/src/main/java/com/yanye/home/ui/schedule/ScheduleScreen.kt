package com.yanye.home.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.AnniversaryDisplayMode
import com.yanye.home.domain.model.AnniversaryType
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.MemoryMood
import com.yanye.home.domain.model.Schedule
import com.yanye.home.domain.model.Visibility
import com.yanye.home.domain.model.Wish
import com.yanye.home.domain.model.WishCategory
import com.yanye.home.navigation.YanYeDestination
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.common.HeaderTextAction
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.PrimaryPageHeader
import com.yanye.home.ui.common.SecondaryTopBar
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private enum class CalendarTab {
    Schedule,
    Anniversary
}

private val DetailLabelWidth = 96.dp
private val CalendarPalePink = Color(0xFFFFF4F7)
private val CalendarDeepPink = Color(0xFFFF5F86)
private val CalendarFabPink = Color(0xFFFFC9D6)

private enum class DatePickerMode {
    Day,
    Month,
    Year
}

private sealed interface CalendarPage {
    data object List : CalendarPage
    data class ScheduleDetail(val schedule: Schedule) : CalendarPage
    data class ScheduleEditor(val schedule: Schedule?) : CalendarPage
    data class AnniversaryDetail(val anniversary: Anniversary) : CalendarPage
    data class AnniversaryEditor(val anniversary: Anniversary?) : CalendarPage
    data object AnniversaryHomeDisplayEditor : CalendarPage
}

@Composable
fun ScheduleScreen(
    initialTab: String = YanYeDestination.Schedule.TAB_SCHEDULE,
    viewModel: ScheduleViewModel = viewModel()
) {
    val schedules by viewModel.schedules.collectAsState()
    val wishes by viewModel.wishes.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val anniversaries by viewModel.anniversaries.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncSchedules,
        onFlushSync = viewModel::flushSync
    )

    var page by remember { mutableStateOf<CalendarPage>(CalendarPage.List) }
    var selectedTab by remember(initialTab) {
        mutableStateOf(
            if (initialTab == YanYeDestination.Schedule.TAB_ANNIVERSARY) {
                CalendarTab.Anniversary
            } else {
                CalendarTab.Schedule
            }
        )
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var compactCalendar by remember { mutableStateOf(false) }
    var archivingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var editingMemory by remember { mutableStateOf<Memory?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val daySchedules = schedules.filter { it.startEpochDay == selectedDate.toEpochDay() }
    val dayMemories = memories.filter { it.dateEpochDay == selectedDate.toEpochDay() }

    when (val currentPage = page) {
        CalendarPage.List -> {
            CalendarListPage(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                syncMessage = syncMessage,
                onSyncClick = {
                    if (selectedTab == CalendarTab.Schedule) viewModel.syncSchedules() else viewModel.syncAnniversaries()
                },
                onClearClick = { showClearConfirm = true },
                selectedDate = selectedDate,
                calendarMonth = calendarMonth,
                compactCalendar = compactCalendar,
                schedules = schedules,
                daySchedules = daySchedules,
                anniversaries = anniversaries,
                onCalendarDrag = { delta ->
                    if (delta < -18f) compactCalendar = true
                    if (delta > 18f) compactCalendar = false
                },
                onPreviousMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                onDateSelected = { date ->
                    selectedDate = date
                    calendarMonth = YearMonth.from(date)
                },
                onScheduleClick = { page = CalendarPage.ScheduleDetail(it) },
                onAnniversaryClick = { page = CalendarPage.AnniversaryDetail(it) },
                onHomeDisplaySettingsClick = { page = CalendarPage.AnniversaryHomeDisplayEditor },
                onCreateClick = {
                    page = if (selectedTab == CalendarTab.Schedule) {
                        CalendarPage.ScheduleEditor(null)
                    } else {
                        CalendarPage.AnniversaryEditor(null)
                    }
                }
            )
        }

        is CalendarPage.ScheduleDetail -> {
            val schedule = schedules.firstOrNull { it.id == currentPage.schedule.id } ?: currentPage.schedule
            val linkedWish = wishes.firstOrNull { it.id == schedule.linkedWishId }
            ScheduleDetailPage(
                schedule = schedule,
                linkedWish = linkedWish,
                onBack = { page = CalendarPage.List },
                onEdit = { page = CalendarPage.ScheduleEditor(schedule) },
                onArchive = { archivingSchedule = schedule },
                onDelete = {
                    viewModel.deleteSchedule(schedule.id)
                    page = CalendarPage.List
                }
            )
        }

        is CalendarPage.ScheduleEditor -> {
            ScheduleEditorPage(
                initialSchedule = currentPage.schedule,
                initialDate = selectedDate,
                wishes = wishes.filter { wish ->
                    wish.scheduleReady &&
                        (wish.id == currentPage.schedule?.linkedWishId ||
                            (!wish.isCompleted && wish.linkedScheduleId == null))
                },
                onBack = { page = CalendarPage.List },
                onSave = { schedule ->
                    viewModel.saveSchedule(schedule)
                    selectedDate = LocalDate.ofEpochDay(schedule.startEpochDay)
                    calendarMonth = YearMonth.from(selectedDate)
                    page = CalendarPage.List
                }
            )
        }

        is CalendarPage.AnniversaryDetail -> {
            val anniversary = anniversaries.firstOrNull { it.id == currentPage.anniversary.id } ?: currentPage.anniversary
            AnniversaryDetailPage(
                anniversary = anniversary,
                onBack = { page = CalendarPage.List },
                onEdit = { page = CalendarPage.AnniversaryEditor(anniversary) },
                onDelete = {
                    viewModel.deleteAnniversary(anniversary.id)
                    page = CalendarPage.List
                }
            )
        }

        is CalendarPage.AnniversaryEditor -> {
            AnniversaryEditorPage(
                initialAnniversary = currentPage.anniversary,
                onBack = { page = CalendarPage.List },
                onSave = { anniversary ->
                    viewModel.saveAnniversary(anniversary)
                    page = CalendarPage.List
                }
            )
        }

        CalendarPage.AnniversaryHomeDisplayEditor -> {
            AnniversaryHomeDisplaySettingsPage(
                anniversaries = anniversaries,
                onBack = { page = CalendarPage.List },
                onSave = { updatedAnniversaries ->
                    viewModel.saveAnniversaryHomeDisplaySettings(updatedAnniversaries)
                    page = CalendarPage.List
                }
            )
        }
    }

    archivingSchedule?.let { schedule ->
        ArchiveScheduleDialog(
            schedule = schedule,
            onDismiss = { archivingSchedule = null },
            onArchive = { memory ->
                viewModel.completeAndArchive(schedule, memory)
                archivingSchedule = null
                page = CalendarPage.List
            }
        )
    }

    editingMemory?.let { memory ->
        MemoryEditorDialog(
            memory = memory,
            onDismiss = { editingMemory = null },
            onSave = { updatedMemory ->
                viewModel.saveMemory(updatedMemory)
                editingMemory = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空本机日程") },
            text = { Text("会删除本机所有日程和回忆卡，并解除愿望里的日程关联。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSchedulesAndMemories()
                        showClearConfirm = false
                    }
                ) {
                    Text("清空", color = YanYeColors.Rose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun CalendarListPage(
    selectedTab: CalendarTab,
    onTabSelected: (CalendarTab) -> Unit,
    syncMessage: String?,
    onSyncClick: () -> Unit,
    onClearClick: () -> Unit,
    selectedDate: LocalDate,
    calendarMonth: YearMonth,
    compactCalendar: Boolean,
    schedules: List<Schedule>,
    daySchedules: List<Schedule>,
    anniversaries: List<Anniversary>,
    onCalendarDrag: (Float) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onScheduleClick: (Schedule) -> Unit,
    onAnniversaryClick: (Anniversary) -> Unit,
    onHomeDisplaySettingsClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PageChrome.primaryPadding,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CalendarHeader(
                    syncMessage = syncMessage,
                    onSyncClick = onSyncClick,
                    onClearClick = onClearClick
                )
            }
            item {
                CalendarSegmentedTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }
            if (selectedTab == CalendarTab.Schedule) {
                item {
                    CalendarPanel(
                        month = calendarMonth,
                        selectedDate = selectedDate,
                        compact = compactCalendar,
                        schedules = schedules,
                        onCalendarDrag = onCalendarDrag,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onDateSelected = onDateSelected
                    )
                }
                if (daySchedules.isEmpty()) {
                    item {
                        Text(
                            text = "今天还没有日程哦",
                            color = YanYeColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                } else {
                    item {
                        ScheduleListGroup(
                            schedules = daySchedules,
                            onScheduleClick = onScheduleClick
                        )
                    }
                }
            } else {
                item {
                    AnniversaryHomeDisplayEntryCard(onClick = onHomeDisplaySettingsClick)
                }
                if (anniversaries.isEmpty()) {
                    item {
                        Text(
                            text = "还没有纪念日哦",
                            color = YanYeColors.Muted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                } else {
                    items(anniversaries, key = { it.id }) { anniversary ->
                        AnniversaryListCard(
                            anniversary = anniversary,
                            onClick = { onAnniversaryClick(anniversary) }
                        )
                    }
                    item {
                        Text(
                            text = "— 长按可拖动排序 —",
                            color = YanYeColors.Muted.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 18.dp)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            containerColor = CalendarFabPink,
            contentColor = Color.White
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun CalendarHeader(
    syncMessage: String?,
    onSyncClick: () -> Unit,
    onClearClick: () -> Unit
) {
    PrimaryPageHeader(
        title = "日历",
        message = syncMessage
    ) {
        HeaderTextAction(text = "同步", color = YanYeColors.Green, onClick = onSyncClick)
        HeaderTextAction(text = "清空", color = YanYeColors.Rose, onClick = onClearClick)
    }
}

@Composable
private fun CalendarSegmentedTabs(
    selectedTab: CalendarTab,
    onTabSelected: (CalendarTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalendarPalePink, MaterialTheme.shapes.medium)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        CalendarTabCell(
            text = "日程",
            selected = selectedTab == CalendarTab.Schedule,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(CalendarTab.Schedule) }
        )
        CalendarTabCell(
            text = "纪念日",
            selected = selectedTab == CalendarTab.Anniversary,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(CalendarTab.Anniversary) }
        )
    }
}

@Composable
private fun CalendarTabCell(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (selected) Color.White else CalendarPalePink, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) YanYeColors.Ink else YanYeColors.Muted,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CalendarPanel(
    month: YearMonth,
    selectedDate: LocalDate,
    compact: Boolean,
    schedules: List<Schedule>,
    onCalendarDrag: (Float) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        onCalendarDrag(dragAmount)
                    }
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onPreviousMonth) { Text("‹", color = YanYeColors.Muted) }
                Text(
                    text = "${month.year} 年 ${month.monthValue} 月",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onNextMonth) { Text("›", color = YanYeColors.Muted) }
            }
            WeekLabels()
            val dates = if (compact) weekDates(selectedDate) else monthGridDates(month)
            dates.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { date ->
                        CalendarDayCell(
                            date = date,
                            inCurrentMonth = YearMonth.from(date) == month || compact,
                            selected = date == selectedDate,
                            hasSchedule = schedules.any { it.startEpochDay == date.toEpochDay() },
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun WeekLabels() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    inCurrentMonth: Boolean,
    selected: Boolean,
    hasSchedule: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .background(if (selected) CalendarDeepPink else Color.Transparent, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = when {
                        selected -> Color.White
                        !inCurrentMonth -> YanYeColors.Muted.copy(alpha = 0.35f)
                        else -> YanYeColors.Ink
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(4.dp)
                    .fillMaxWidth(0.2f)
                    .background(if (hasSchedule) CalendarDeepPink else Color.Transparent, MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun ScheduleListGroup(
    schedules: List<Schedule>,
    onScheduleClick: (Schedule) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text(
                text = selectedScheduleDateLabel(schedules.first().startEpochDay),
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            schedules.forEachIndexed { index, schedule ->
                ScheduleListRow(
                    schedule = schedule,
                    onClick = { onScheduleClick(schedule) }
                )
                if (index != schedules.lastIndex) {
                    HorizontalDivider(
                        color = YanYeColors.Line,
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleListRow(
    schedule: Schedule,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 0.dp)
    ) {
        val metaItems = buildList {
            schedule.locationName.visibleTextOrNull()?.let { add(it) }
            schedule.budgetCents?.let { add("预算 ${centsToBudgetText(it)}") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, end = 10.dp)
                    .size(7.dp)
                    .background(CalendarDeepPink, CircleShape)
            )
            Text(
                text = timeLabel(schedule.startMinuteOfDay),
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (metaItems.isNotEmpty()) {
                    Text(
                        text = metaItems.joinToString(" · "),
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnniversaryListCard(
    anniversary: Anniversary,
    onClick: () -> Unit
) {
    val metricText = anniversaryListMetricText(anniversary)
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = anniversary.name,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anniversaryListDateText(anniversary),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = metricText,
                    color = CalendarDeepPink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Text(
                    text = "›",
                    color = YanYeColors.Muted.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun AnniversaryHomeDisplayEntryCard(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "首页展示设置",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "›",
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class AnniversaryHomeDisplayDraft(
    val anniversary: Anniversary,
    val showOnHome: Boolean
)

@Composable
private fun AnniversaryHomeDisplaySettingsPage(
    anniversaries: List<Anniversary>,
    onBack: () -> Unit,
    onSave: (List<Anniversary>) -> Unit
) {
    val rows = remember(anniversaries) {
        mutableStateListOf<AnniversaryHomeDisplayDraft>().apply {
            addAll(
                anniversaries
                    .sortedWith(
                        compareBy<Anniversary> { it.homeSortOrder }
                            .thenBy { it.dateEpochDay }
                    )
                    .map { AnniversaryHomeDisplayDraft(it, it.showOnHome) }
            )
        }
    }
    val selectedCount = rows.count { it.showOnHome }
    val selectedPreview = rows
        .filter { it.showOnHome }
        .take(4)
    val saveDisplaySettings = {
        onSave(
            rows.mapIndexed { index, row ->
                row.anniversary.copy(
                    showOnHome = row.showOnHome && indexOfVisible(rows, index) < 4,
                    homeSortOrder = index + 1
                )
            }
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
                title = "首页展示",
                actionText = "保存",
                actionColor = YanYeColors.Blue,
                onBack = onBack,
                onActionClick = saveDisplaySettings
            )
        }
        items(rows, key = { it.anniversary.id }) { row ->
            val index = rows.indexOf(row)
            AnniversaryHomeDisplayRow(
                draft = row,
                selectedCount = selectedCount,
                onToggle = { checked ->
                    if (checked && selectedCount >= 4 && !row.showOnHome) return@AnniversaryHomeDisplayRow
                    rows[index] = row.copy(showOnHome = checked)
                },
                onMove = { direction ->
                    val targetIndex = (index + direction).coerceIn(0, rows.lastIndex)
                    if (targetIndex != index) {
                        val moved = rows.removeAt(index)
                        rows.add(targetIndex, moved)
                    }
                }
            )
        }
        item {
            Text(
                text = "预览（首页效果）",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        item {
            AnniversaryHomePreviewCard(selectedPreview)
        }
    }
}
}

@Composable
private fun AnniversaryHomeDisplayRow(
    draft: AnniversaryHomeDisplayDraft,
    selectedCount: Int,
    onToggle: (Boolean) -> Unit,
    onMove: (Int) -> Unit
) {
    var dragRemainder by remember(draft.anniversary.id) { mutableStateOf(0f) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "☰",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.pointerInput(draft.anniversary.id) {
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
                    text = draft.anniversary.name,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = anniversaryStatusText(draft.anniversary),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Switch(
                checked = draft.showOnHome,
                enabled = draft.showOnHome || selectedCount < 4,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun AnniversaryHomePreviewCard(
    selectedRows: List<AnniversaryHomeDisplayDraft>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (selectedRows.isEmpty()) {
                Text(
                    text = "首页暂不展示纪念日",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                selectedRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.anniversary.name,
                            color = YanYeColors.Ink,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = anniversaryStatusText(row.anniversary),
                            color = YanYeColors.Rose,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun indexOfVisible(
    rows: List<AnniversaryHomeDisplayDraft>,
    rowIndex: Int
): Int =
    rows.take(rowIndex + 1).count { it.showOnHome } - 1

@Composable
private fun ScheduleDetailPage(
    schedule: Schedule,
    linkedWish: Wish?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
            contentPadding = PageChrome.secondaryPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SecondaryTopBar(
                title = "日程详情",
                actionText = "编辑",
                actionColor = YanYeColors.Muted,
                onBack = onBack,
                onActionClick = onEdit
            )
        }
        item {
            Column {
                Text(
                    text = schedule.title,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = linkedWish?.let { "来自愿望清单" } ?: "共同日程",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            DetailGroup(
                rows = buildList {
                    add("时间" to monthDayTimeLabel(LocalDate.ofEpochDay(schedule.startEpochDay), schedule.startMinuteOfDay))
                    schedule.locationName.visibleTextOrNull()?.let { add("地点" to it) }
                    schedule.budgetCents?.let { add("预算" to "¥${centsToBudgetText(it)}") }
                    schedule.participants.visibleTextOrNull()?.let { add("参与人" to it) }
                    linkedWish?.let { add("关联愿望" to it.title) }
                    add("提醒" to "提前 ${schedule.reminderMinutesBefore} 分钟")
                    schedule.note.visibleTextOrNull()?.let { add("备注" to it) }
                    if (schedule.isGuideMode) {
                        schedule.guideRestaurants.visibleTextOrNull()?.let { add("餐厅" to it) }
                        schedule.guideActivities.visibleTextOrNull()?.let { add("活动" to it) }
                        schedule.guideRoute.visibleTextOrNull()?.let { add("路线" to it) }
                        schedule.backupPlan.visibleTextOrNull()?.let { add("备选方案" to it) }
                    }
                }
            )
        }
        item {
            Button(
                onClick = onArchive,
                modifier = Modifier.fillMaxWidth(),
                enabled = !schedule.isCompleted,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = YanYeColors.Ink,
                    contentColor = Color.White,
                    disabledContainerColor = YanYeColors.Soft,
                    disabledContentColor = YanYeColors.Muted
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (schedule.isCompleted) "已完成" else "标记完成 → 记录回忆",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        item {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除日程", color = YanYeColors.Rose)
            }
        }
    }
}

@Composable
private fun DetailGroup(
    rows: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            rows.forEachIndexed { index, row ->
                DetailRow(label = row.first, value = row.second)
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = YanYeColors.Line, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
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
            modifier = Modifier.width(DetailLabelWidth)
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

@Composable
private fun ScheduleEditorPage(
    initialSchedule: Schedule?,
    initialDate: LocalDate,
    wishes: List<Wish>,
    onBack: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    var title by remember(initialSchedule) { mutableStateOf(initialSchedule?.title ?: "") }
    var date by remember(initialSchedule, initialDate) {
        mutableStateOf(initialSchedule?.startEpochDay?.let(LocalDate::ofEpochDay) ?: initialDate)
    }
    var startTime by remember(initialSchedule) {
        mutableStateOf(initialSchedule?.startMinuteOfDay?.let(::timeLabel) ?: "19:00")
    }
    var locationName by remember(initialSchedule) { mutableStateOf(initialSchedule?.locationName.orEmpty()) }
    var reminderMinutes by remember(initialSchedule) {
        mutableStateOf((initialSchedule?.reminderMinutesBefore ?: 60).toString())
    }
    var budget by remember(initialSchedule) {
        mutableStateOf(initialSchedule?.budgetCents?.let(::centsToBudgetText).orEmpty())
    }
    var participants by remember(initialSchedule) {
        mutableStateOf(initialSchedule?.participants ?: "我们俩")
    }
    var linkedWishId by remember(initialSchedule) { mutableStateOf(initialSchedule?.linkedWishId) }
    var isGuideMode by remember(initialSchedule) { mutableStateOf(initialSchedule?.isGuideMode ?: false) }
    var guideRestaurants by remember(initialSchedule) { mutableStateOf(initialSchedule?.guideRestaurants.orEmpty()) }
    var guideActivities by remember(initialSchedule) { mutableStateOf(initialSchedule?.guideActivities.orEmpty()) }
    var guideRoute by remember(initialSchedule) { mutableStateOf(initialSchedule?.guideRoute.orEmpty()) }
    var backupPlan by remember(initialSchedule) { mutableStateOf(initialSchedule?.backupPlan.orEmpty()) }
    var note by remember(initialSchedule) { mutableStateOf(initialSchedule?.note.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val linkedWish = wishes.firstOrNull { it.id == linkedWishId }
    val canSave = title.isNotBlank()
    val saveSchedule = {
        onSave(
            Schedule(
                id = initialSchedule?.id ?: 0,
                title = title.trim(),
                startEpochDay = date.toEpochDay(),
                startMinuteOfDay = parseTimeToMinute(startTime) ?: 19 * 60,
                locationName = locationName.trim(),
                reminderMinutesBefore = reminderMinutes.toIntOrNull() ?: 60,
                budgetCents = budgetTextToCents(budget),
                participants = participants.trim().ifBlank { "我们俩" },
                linkedWishId = linkedWishId,
                linkedWishRemoteId = initialSchedule?.linkedWishRemoteId,
                isGuideMode = isGuideMode,
                guideRestaurants = guideRestaurants.trim(),
                guideActivities = guideActivities.trim(),
                guideRoute = guideRoute.trim(),
                backupPlan = backupPlan.trim(),
                note = note.trim(),
                isCompleted = initialSchedule?.isCompleted ?: false,
                memoryId = initialSchedule?.memoryId,
                createdAt = initialSchedule?.createdAt ?: 0,
                updatedAt = initialSchedule?.updatedAt ?: 0,
                visibility = initialSchedule?.visibility ?: Visibility.Shared,
                sharedWithPartner = initialSchedule?.sharedWithPartner ?: true,
                isDeleted = initialSchedule?.isDeleted ?: false,
                remoteId = initialSchedule?.remoteId,
                coupleId = initialSchedule?.coupleId,
                ownerUserId = initialSchedule?.ownerUserId,
                syncStatus = initialSchedule?.syncStatus ?: com.yanye.home.domain.model.SyncStatus.Synced,
                remoteUpdatedAt = initialSchedule?.remoteUpdatedAt
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PageChrome.secondaryPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SecondaryTopBar(
                title = if (initialSchedule == null) "新增日程" else "编辑日程",
                actionText = "保存",
                actionColor = if (canSave) YanYeColors.Green else YanYeColors.Muted,
                onBack = onBack,
                onActionClick = if (canSave) saveSchedule else null
            )
        }
        item {
            ScheduleEditorHero(
                title = title,
                subtitle = linkedWish?.let { "来自愿望清单" } ?: "共同日程"
            )
        }
        if (wishes.isNotEmpty()) {
            item {
                WishLinkGroup(
                    wishes = wishes,
                    linkedWishId = linkedWishId,
                    onUnlink = { linkedWishId = null },
                    onWishSelected = { wish ->
                        linkedWishId = wish.id
                        if (title.isBlank()) title = wish.title
                        if (locationName.isBlank()) locationName = wish.locationName
                        if (budget.isBlank()) budget = wish.budgetCents?.let(::centsToBudgetText).orEmpty()
                        if (note.isBlank()) note = wish.preparationItems
                        if (wish.category == WishCategory.Travel) isGuideMode = true
                    }
                )
            }
        }
        item {
            ScheduleEditGroup(
                rows = listOf(
                    EditorRow.Text("标题", title, { title = it }),
                    EditorRow.Action("日期", formatDate(date), { showDatePicker = true }),
                    EditorRow.Action("时间", startTime, { showTimePicker = true }),
                    EditorRow.Text("地点", locationName, { locationName = it }),
                    EditorRow.Text("预算", budget, { value -> budget = value.filter { it.isDigit() || it == '.' }.take(10) }),
                    EditorRow.Text("参与人", participants, { participants = it }),
                    EditorRow.Text("提醒", reminderMinutes, { value -> reminderMinutes = value.filter(Char::isDigit).take(4) }, suffix = "分钟")
                )
            )
        }
        item {
            GuideModeCard(
                title = linkedWish?.let { "攻略模式，来自 ${it.title}" } ?: "攻略模式",
                checked = isGuideMode,
                onCheckedChange = { isGuideMode = it }
            )
        }
        if (isGuideMode) {
            item {
                ScheduleEditGroup(
                    rows = listOf(
                        EditorRow.Text("餐厅", guideRestaurants, { guideRestaurants = it }, singleLine = false, compactMultiline = true),
                        EditorRow.Text("活动", guideActivities, { guideActivities = it }, singleLine = false, compactMultiline = true),
                        EditorRow.Text("路线", guideRoute, { guideRoute = it }, singleLine = false, compactMultiline = true),
                        EditorRow.Text("备选方案", backupPlan, { backupPlan = it }, singleLine = false, compactMultiline = true)
                    )
                )
            }
        }
        item {
            ScheduleEditGroup(
                rows = listOf(
                    EditorRow.Text("备注", note, { note = it }, singleLine = false)
                )
            )
        }
        item {
            Button(
                onClick = saveSchedule,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = YanYeColors.Ink,
                    contentColor = Color.White,
                    disabledContainerColor = YanYeColors.Soft,
                    disabledContentColor = YanYeColors.Muted
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "保存日程",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
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
    if (showTimePicker) {
        ScheduleTimePickerPopup(
            initialTime = parseTimeToLocalTime(startTime) ?: LocalTime.of(19, 0),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { selected ->
                startTime = selected.format(DateTimeFormatter.ofPattern("HH:mm"))
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun ScheduleEditorHero(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title.ifBlank { "未命名日程" },
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private sealed interface EditorRow {
    data class Text(
        val label: String,
        val value: String,
        val onValueChange: (String) -> Unit,
        val suffix: String? = null,
        val singleLine: Boolean = true,
        val compactMultiline: Boolean = false
    ) : EditorRow

    data class Action(
        val label: String,
        val value: String,
        val onClick: () -> Unit
    ) : EditorRow
}

@Composable
private fun ScheduleEditGroup(rows: List<EditorRow>) {
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
                    is EditorRow.Text -> ScheduleEditRow(row)
                    is EditorRow.Action -> ScheduleActionRow(row)
                }
                if (index != rows.lastIndex) {
                    HorizontalDivider(color = YanYeColors.Line, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun ScheduleEditRow(row: EditorRow.Text) {
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
                .width(DetailLabelWidth)
                .padding(top = if (row.singleLine) 0.dp else 14.dp)
        )
        val textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = YanYeColors.Ink,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = when {
                    row.singleLine -> 48.dp
                    row.compactMultiline -> 48.dp
                    else -> 96.dp
                }),
            verticalAlignment = if (row.singleLine) Alignment.CenterVertically else Alignment.Top
        ) {
            BasicTextField(
                value = row.value,
                onValueChange = row.onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = if (row.singleLine) 0.dp else 14.dp),
                singleLine = row.singleLine,
                textStyle = textStyle
            )
            row.suffix?.let { suffix ->
                Text(
                    text = suffix,
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
private fun ScheduleActionRow(row: EditorRow.Action) {
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
            modifier = Modifier.width(DetailLabelWidth)
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
private fun WishLinkGroup(
    wishes: List<Wish>,
    linkedWishId: Long?,
    onUnlink: () -> Unit,
    onWishSelected: (Wish) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember(wishes) {
        mutableStateOf(wishes.firstOrNull { it.id == linkedWishId }?.category ?: wishes.first().category)
    }
    val groupedWishes = remember(wishes) { wishes.groupBy { it.category } }
    val categories = remember(wishes) { WishCategory.entries.filter { groupedWishes[it].orEmpty().isNotEmpty() } }
    val linkedWish = wishes.firstOrNull { it.id == linkedWishId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "关联愿望",
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = linkedWish?.title ?: "不关联",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Text(
                    text = if (expanded) "⌃" else "⌄",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(0.22f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            WishCategoryTab(
                                label = wishCategoryLabel(category),
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(0.78f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WishChoiceRow(
                            title = "不关联",
                            selected = linkedWishId == null,
                            onClick = onUnlink
                        )
                        groupedWishes[selectedCategory].orEmpty().forEach { wish ->
                            WishChoiceRow(
                                title = wish.title,
                                selected = linkedWishId == wish.id,
                                onClick = { onWishSelected(wish) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishCategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (selected) YanYeColors.Ink else YanYeColors.Muted,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(
                color = if (selected) YanYeColors.GreenSoft else YanYeColors.Soft,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun WishChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) YanYeColors.GreenSoft else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (selected) YanYeColors.Green else YanYeColors.Line,
                    shape = CircleShape
                )
        )
        Text(
            text = title,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun wishCategoryLabel(category: WishCategory): String =
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

@Composable
private fun GuideModeCard(
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

/*
@Composable
private fun LegacyGuideModeCard(
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
*/

@Composable
private fun ScheduleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        singleLine = minLines == 1,
        label = { Text(label) }
    )
}

@Composable
private fun AnniversaryDetailPage(
    anniversary: Anniversary,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PageChrome.secondaryPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SecondaryTopBar(
                title = "纪念日",
                actionText = "编辑",
                actionColor = YanYeColors.Muted,
                onBack = onBack,
                onActionClick = onEdit
            )
        }
        item {
            AnniversaryDetailHero(anniversary)
        }
        /*
        item { HorizontalDivider(color = YanYeColors.Line, thickness = 0.8.dp) }
        item { AnniversarySectionTitle("历年庆祝") }
        item { AnniversaryArchiveCard(anniversary) }
        item { HorizontalDivider(color = YanYeColors.Line, thickness = 0.8.dp) }
        item { AnniversarySectionTitle("礼物灵感") }
        item { AnniversaryGiftCard(anniversary) }
        */
        item {
            DetailGroup(
                rows = buildList {
                    add("纪念日名称" to anniversary.name)
                    add("日期" to formatDate(LocalDate.ofEpochDay(anniversary.dateEpochDay)))
                    add("类型" to anniversaryTypeLabel(anniversary.type))
                    add("显示方式" to anniversaryDisplayModeLabel(anniversary.displayMode))
                    add("提醒" to "提前 ${anniversary.reminderDaysBefore} 天")
                    if (anniversary.note.isNotBlank()) add("备注" to anniversary.note)
                }
            )
        }
        item {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("删除纪念日", color = YanYeColors.Rose)
            }
        }
    }
}

@Composable
private fun AnniversaryDetailHero(anniversary: Anniversary) {
    val metric = anniversaryHeroMetric(anniversary)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = metric.number,
            color = YanYeColors.Rose,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = metric.label,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "${anniversaryReadableDate(LocalDate.ofEpochDay(anniversary.dateEpochDay))}开始",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AnniversarySectionTitle(title: String) {
    Text(
        text = title,
        color = YanYeColors.Ink,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun AnniversaryArchiveCard(anniversary: Anniversary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${LocalDate.now().year} · ${anniversaryTypeLabel(anniversary.type)}",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (anniversary.celebrationArchiveEnabled) "庆祝档案预留" else "null",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = anniversary.note.ifBlank { "null" },
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AnniversaryGiftCard(anniversary: Anniversary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (anniversary.giftWishLinkEnabled) "公开愿望：null" else "null",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→愿望",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(YanYeColors.Rose, MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun AnniversaryEditorPage(
    initialAnniversary: Anniversary?,
    onBack: () -> Unit,
    onSave: (Anniversary) -> Unit
) {
    var name by remember(initialAnniversary) { mutableStateOf(initialAnniversary?.name.orEmpty()) }
    var date by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.dateEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now())
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
    var note by remember(initialAnniversary) { mutableStateOf(initialAnniversary?.note.orEmpty()) }
    var giftWishLinkEnabled by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.giftWishLinkEnabled ?: true)
    }
    var celebrationArchiveEnabled by remember(initialAnniversary) {
        mutableStateOf(initialAnniversary?.celebrationArchiveEnabled ?: true)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val canSave = name.isNotBlank()
    val saveAnniversary = {
        onSave(
            Anniversary(
                id = initialAnniversary?.id ?: 0,
                name = name.trim(),
                dateEpochDay = date.toEpochDay(),
                type = type,
                displayMode = displayMode,
                reminderDaysBefore = reminderDaysBefore.toIntOrNull() ?: 7,
                note = note.trim(),
                coverImageUri = initialAnniversary?.coverImageUri,
                giftWishLinkEnabled = giftWishLinkEnabled,
                celebrationArchiveEnabled = celebrationArchiveEnabled,
                createdAt = initialAnniversary?.createdAt ?: 0,
                updatedAt = initialAnniversary?.updatedAt ?: 0,
                visibility = initialAnniversary?.visibility ?: Visibility.Shared,
                sharedWithPartner = initialAnniversary?.sharedWithPartner ?: true,
                lockedUntilEpochDay = initialAnniversary?.lockedUntilEpochDay,
                isDeleted = initialAnniversary?.isDeleted ?: false,
                remoteId = initialAnniversary?.remoteId,
                coupleId = initialAnniversary?.coupleId,
                ownerUserId = initialAnniversary?.ownerUserId,
                syncStatus = initialAnniversary?.syncStatus ?: com.yanye.home.domain.model.SyncStatus.Synced,
                remoteUpdatedAt = initialAnniversary?.remoteUpdatedAt,
                showOnHome = initialAnniversary?.showOnHome ?: false,
                homeSortOrder = initialAnniversary?.homeSortOrder ?: 100
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
                title = if (initialAnniversary == null) "新增纪念日" else "编辑纪念日",
                actionText = "保存",
                actionColor = if (canSave) YanYeColors.Rose else YanYeColors.Muted,
                onBack = onBack,
                onActionClick = if (canSave) saveAnniversary else null
            )
        }
        item {
            ScheduleEditorHero(
                title = name,
                subtitle = "${anniversaryTypeLabel(type)} · ${anniversaryDisplayModeLabel(displayMode)}"
            )
        }
        item {
            AnniversaryOptionGroup(
                title = "类型",
                options = AnniversaryType.entries,
                selected = type,
                label = ::anniversaryTypeLabel,
                onSelected = { type = it }
            )
        }
        item {
            AnniversaryOptionGroup(
                title = "显示方式",
                options = AnniversaryDisplayMode.entries,
                selected = displayMode,
                label = ::anniversaryDisplayModeLabel,
                onSelected = { displayMode = it }
            )
        }
        item {
            ScheduleEditGroup(
                rows = listOf(
                    EditorRow.Text("名称", name, { name = it }),
                    EditorRow.Action("日期", formatDate(date), { showDatePicker = true }),
                    EditorRow.Text(
                        "提醒",
                        reminderDaysBefore,
                        { value -> reminderDaysBefore = value.filter(Char::isDigit).take(3) },
                        suffix = "天"
                    ),
                    EditorRow.Text("备注", note, { note = it }, singleLine = false)
                )
            )
        }
        /*
        item {
            GuideModeCard(
                title = "预留礼物愿望",
                checked = giftWishLinkEnabled,
                onCheckedChange = { giftWishLinkEnabled = it }
            )
        }
        item {
            GuideModeCard(
                title = "预留庆祝档案",
                checked = celebrationArchiveEnabled,
                onCheckedChange = { celebrationArchiveEnabled = it }
            )
        }
        */
        item {
            Button(
                onClick = saveAnniversary,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = YanYeColors.Ink,
                    contentColor = Color.White,
                    disabledContainerColor = YanYeColors.Soft,
                    disabledContentColor = YanYeColors.Muted
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "保存纪念日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
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
private fun <T> AnniversaryOptionGroup(
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

@Composable
private fun DetailLine(label: String, value: String) {
    Text(
        text = "$label：$value",
        color = YanYeColors.Muted,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun MemoryCard(
    memory: Memory,
    onEditClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = memory.title,
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${memory.locationName.ifBlank { "地点未记" }} · ${moodLabel(memory.mood)}",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                TextButton(onClick = onEditClick) {
                    Text("编辑")
                }
            }
            if (memory.note.isNotBlank()) {
                Text(
                    text = memory.note,
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun MemoryEditorDialog(
    memory: Memory,
    onDismiss: () -> Unit,
    onSave: (Memory) -> Unit
) {
    var photoUris by remember(memory) { mutableStateOf(memory.photoUris) }
    var foodNotes by remember(memory) { mutableStateOf(memory.foodNotes) }
    var expense by remember(memory) {
        mutableStateOf(memory.expenseCents?.let(::centsToBudgetText).orEmpty())
    }
    var mood by remember(memory) { mutableStateOf(memory.mood) }
    var note by remember(memory) { mutableStateOf(memory.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑回忆卡") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { ScheduleTextField(photoUris, { photoUris = it }, "照片路径或 URI") }
                item { ScheduleTextField(foodNotes, { foodNotes = it }, "吃了什么") }
                item {
                    ScheduleTextField(
                        value = expense,
                        onValueChange = { value -> expense = value.filter { it.isDigit() || it == '.' }.take(10) },
                        label = "实际花费"
                    )
                }
                item {
                    ChipGroupTitle("心情")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemoryMood.entries.forEach { memoryMood ->
                            FilterChip(
                                selected = mood == memoryMood,
                                onClick = { mood = memoryMood },
                                label = { Text(moodLabel(memoryMood)) }
                            )
                        }
                    }
                }
                item { ScheduleTextField(note, { note = it }, "回忆备注", minLines = 2) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        memory.copy(
                            photoUris = photoUris.trim(),
                            foodNotes = foodNotes.trim(),
                            expenseCents = budgetTextToCents(expense),
                            mood = mood,
                            note = note.trim()
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ArchiveScheduleDialog(
    schedule: Schedule,
    onDismiss: () -> Unit,
    onArchive: (Memory) -> Unit
) {
    var photoUris by remember(schedule) { mutableStateOf("") }
    var foodNotes by remember(schedule) { mutableStateOf("") }
    var expense by remember(schedule) {
        mutableStateOf(schedule.budgetCents?.let(::centsToBudgetText).orEmpty())
    }
    var mood by remember(schedule) { mutableStateOf(MemoryMood.Happy) }
    var note by remember(schedule) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("完成归档") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text(
                        text = "为“${schedule.title}”生成一张回忆卡。",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                item { ScheduleTextField(photoUris, { photoUris = it }, "照片路径或 URI") }
                item { ScheduleTextField(foodNotes, { foodNotes = it }, "吃了什么") }
                item {
                    ScheduleTextField(
                        value = expense,
                        onValueChange = { value -> expense = value.filter { it.isDigit() || it == '.' }.take(10) },
                        label = "实际花费"
                    )
                }
                item {
                    ChipGroupTitle("心情")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MemoryMood.entries.forEach { memoryMood ->
                            FilterChip(
                                selected = mood == memoryMood,
                                onClick = { mood = memoryMood },
                                label = { Text(moodLabel(memoryMood)) }
                            )
                        }
                    }
                }
                item { ScheduleTextField(note, { note = it }, "回忆备注", minLines = 2) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onArchive(
                        Memory(
                            title = schedule.title,
                            dateEpochDay = schedule.startEpochDay,
                            scheduleId = schedule.id,
                            linkedWishId = schedule.linkedWishId,
                            locationName = schedule.locationName,
                            photoUris = photoUris.trim(),
                            foodNotes = foodNotes.trim(),
                            expenseCents = budgetTextToCents(expense),
                            mood = mood,
                            note = note.trim()
                        )
                    )
                }
            ) {
                Text("生成回忆卡")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DatePickerPopup(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var pickerMode by remember { mutableStateOf(DatePickerMode.Day) }
    val dates = remember(visibleMonth) { monthGridDates(visibleMonth) }
    val decadeStart = (visibleMonth.year / 10) * 10
    Dialog(onDismissRequest = onDismiss) {
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
                Column {
                    Text(
                        text = "选择日期",
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DateNavButton(text = "‹") {
                        visibleMonth = when (pickerMode) {
                            DatePickerMode.Day -> visibleMonth.minusMonths(1)
                            DatePickerMode.Month -> visibleMonth.minusYears(1)
                            DatePickerMode.Year -> visibleMonth.minusYears(10)
                        }
                    }
                    DatePickerHeaderTitle(
                        mode = pickerMode,
                        visibleMonth = visibleMonth,
                        decadeStart = decadeStart,
                        onYearClick = { pickerMode = DatePickerMode.Year },
                        onMonthClick = { pickerMode = DatePickerMode.Month }
                    )
                    DateNavButton(text = "›") {
                        visibleMonth = when (pickerMode) {
                            DatePickerMode.Day -> visibleMonth.plusMonths(1)
                            DatePickerMode.Month -> visibleMonth.plusYears(1)
                            DatePickerMode.Year -> visibleMonth.plusYears(10)
                        }
                    }
                }

                when (pickerMode) {
                    DatePickerMode.Day -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                                Text(
                                    text = label,
                                    color = YanYeColors.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dates.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    week.forEach { date ->
                                        DatePickerDayCell(
                                            date = date,
                                            visibleMonth = visibleMonth,
                                            selectedDate = selectedDate,
                                            onDateSelected = { selectedDate = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    DatePickerMode.Month -> {
                        DatePickerMonthGrid(
                            visibleMonth = visibleMonth,
                            selectedDate = selectedDate,
                            onMonthSelected = { month ->
                                visibleMonth = YearMonth.of(visibleMonth.year, month)
                                selectedDate = selectedDate.withYear(visibleMonth.year)
                                    .withMonth(month)
                                    .withDayOfMonth(selectedDate.dayOfMonth.coerceAtMost(YearMonth.of(visibleMonth.year, month).lengthOfMonth()))
                                pickerMode = DatePickerMode.Day
                            }
                        )
                    }
                    DatePickerMode.Year -> {
                        DatePickerYearGrid(
                            decadeStart = decadeStart,
                            selectedYear = selectedDate.year,
                            onYearSelected = { year ->
                                visibleMonth = visibleMonth.withYear(year)
                                selectedDate = selectedDate.withYear(year)
                                    .withDayOfMonth(selectedDate.dayOfMonth.coerceAtMost(YearMonth.of(year, selectedDate.monthValue).lengthOfMonth()))
                                pickerMode = DatePickerMode.Month
                            }
                        )
                    }
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
                        onClick = { onDateSelected(selectedDate) },
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
private fun DateNavButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DatePickerHeaderTitle(
    mode: DatePickerMode,
    visibleMonth: YearMonth,
    decadeStart: Int,
    onYearClick: () -> Unit,
    onMonthClick: () -> Unit
) {
    when (mode) {
        DatePickerMode.Day -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${visibleMonth.year}年",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onYearClick)
            )
            Text(
                text = "${visibleMonth.monthValue}月",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onMonthClick)
            )
        }
        DatePickerMode.Month -> Text(
            text = "${visibleMonth.year}年",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onYearClick)
        )
        DatePickerMode.Year -> Text(
            text = "$decadeStart - ${decadeStart + 9}",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DatePickerMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    onMonthSelected: (Int) -> Unit
) {
    val monthLabels = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        monthLabels.chunked(4).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEachIndexed { columnIndex, label ->
                    val month = rowIndex * 4 + columnIndex + 1
                    DatePickerGridCell(
                        text = label,
                        selected = selectedDate.year == visibleMonth.year && selectedDate.monthValue == month,
                        onClick = { onMonthSelected(month) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DatePickerYearGrid(
    decadeStart: Int,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val years = (decadeStart..decadeStart + 9).toList()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        years.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { year ->
                    DatePickerGridCell(
                        text = year.toString(),
                        selected = year == selectedYear,
                        onClick = { onYearSelected(year) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DatePickerGridCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = if (selected) YanYeColors.Ink else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else YanYeColors.Ink,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DatePickerDayCell(
    date: LocalDate,
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = date == selectedDate
    val isCurrentMonth = YearMonth.from(date) == visibleMonth
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                color = if (isSelected) YanYeColors.Ink else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = when {
                isSelected -> Color.White
                isCurrentMonth -> YanYeColors.Ink
                else -> YanYeColors.Muted.copy(alpha = 0.48f)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScheduleTimePickerPopup(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialTime.hour) }
    var selectedMinute by remember { mutableStateOf(initialTime.minute) }
    Dialog(onDismissRequest = onDismiss) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScheduleTimeWheelColumn(
                        values = 0..23,
                        selected = selectedHour,
                        suffix = "时",
                        onSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f)
                    )
                    ScheduleTimeWheelColumn(
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
private fun ScheduleTimeWheelColumn(
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

private fun monthGridDates(month: YearMonth): List<LocalDate> {
    val firstDay = month.atDay(1)
    val start = firstDay.minusDays((firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    return (0 until 42).map { start.plusDays(it.toLong()) }
}

private fun weekDates(date: LocalDate): List<LocalDate> {
    val start = date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    return (0 until 7).map { start.plusDays(it.toLong()) }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun selectedScheduleDateLabel(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val weekLabel = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
    return "${date.format(DateTimeFormatter.ofPattern("MM.dd"))} $weekLabel"
}

private fun timeLabel(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun monthDayTimeLabel(date: LocalDate, minuteOfDay: Int): String =
    "${date.monthValue}月${date.dayOfMonth}日 ${timeLabel(minuteOfDay)}"

private fun parseTimeToMinute(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun parseTimeToLocalTime(value: String): LocalTime? {
    val minuteOfDay = parseTimeToMinute(value) ?: return null
    return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
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

private fun moodLabel(mood: MemoryMood): String =
    when (mood) {
        MemoryMood.Happy -> "开心"
        MemoryMood.Touched -> "感动"
        MemoryMood.TiredButWorth -> "累但值得"
        MemoryMood.Ordinary -> "普通"
        MemoryMood.Unwell -> "难受"
    }

private data class AnniversaryHeroMetric(
    val number: String,
    val label: String
)

private fun anniversaryHeroMetric(anniversary: Anniversary): AnniversaryHeroMetric {
    val date = LocalDate.ofEpochDay(anniversary.dateEpochDay)
    val today = LocalDate.now()
    return when (anniversary.displayMode) {
        AnniversaryDisplayMode.Countdown -> {
            val days = ChronoUnit.DAYS.between(today, nextOccurrenceDate(date, today))
            AnniversaryHeroMetric(days.toString(), "天到下一次")
        }
        AnniversaryDisplayMode.CountUp -> {
            val days = ChronoUnit.DAYS.between(date, today)
            if (days >= 0) {
                AnniversaryHeroMetric(days.toString(), "天在一起")
            } else {
                AnniversaryHeroMetric(abs(days).toString(), "天后开始")
            }
        }
        AnniversaryDisplayMode.Anniversary -> {
            val years = if (date.isAfter(today)) 0 else Period.between(date, today).years
            AnniversaryHeroMetric(years.toString(), "周年")
        }
    }
}

private fun nextOccurrenceDate(date: LocalDate, today: LocalDate): LocalDate {
    if (date.isAfter(today)) return date
    var next = date.withYear(today.year)
    if (!next.isAfter(today)) next = next.plusYears(1)
    return next
}

private fun anniversaryStatusColor(anniversary: Anniversary): Color {
    val date = LocalDate.ofEpochDay(anniversary.dateEpochDay)
    val today = LocalDate.now()
    return when {
        anniversary.displayMode == AnniversaryDisplayMode.CountUp -> YanYeColors.Gold
        date.isBefore(today) -> YanYeColors.Gold
        ChronoUnit.DAYS.between(today, date) <= 14 -> YanYeColors.Rose
        else -> YanYeColors.Muted
    }
}

private fun anniversaryListDateText(anniversary: Anniversary): String {
    val date = LocalDate.ofEpochDay(anniversary.dateEpochDay)
    val base = date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    return if (anniversary.note.isBlank()) base else "$base · ${anniversary.note}"
}

private fun anniversaryListMetricText(anniversary: Anniversary): String {
    val date = LocalDate.ofEpochDay(anniversary.dateEpochDay)
    val today = LocalDate.now()
    return when (anniversary.displayMode) {
        AnniversaryDisplayMode.Countdown -> {
            val days = ChronoUnit.DAYS.between(today, nextOccurrenceDate(date, today)).coerceAtLeast(0)
            "还有 $days 天"
        }
        AnniversaryDisplayMode.CountUp -> {
            val days = ChronoUnit.DAYS.between(date, today)
            if (days >= 0) "已经 $days 天" else "还有 ${abs(days)} 天"
        }
        AnniversaryDisplayMode.Anniversary -> {
            if (date.isAfter(today)) {
                val days = ChronoUnit.DAYS.between(today, date).coerceAtLeast(0)
                "还有 $days 天"
            } else {
                val years = Period.between(date, today).years
                if (years <= 0) {
                    val days = ChronoUnit.DAYS.between(date, today).coerceAtLeast(0)
                    "已经 $days 天"
                } else {
                    "已经 $years 周年"
                }
            }
        }
    }
}

private fun anniversaryReadableDate(date: LocalDate): String =
    "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

private fun anniversaryDisplayModeLabel(mode: AnniversaryDisplayMode): String =
    when (mode) {
        AnniversaryDisplayMode.Countdown -> "倒数"
        AnniversaryDisplayMode.CountUp -> "正数"
        AnniversaryDisplayMode.Anniversary -> "周年"
    }

private fun anniversaryTypeLabel(type: AnniversaryType): String =
    when (type) {
        AnniversaryType.Relationship -> "恋爱"
        AnniversaryType.Birthday -> "生日"
        AnniversaryType.Travel -> "旅行"
        AnniversaryType.Custom -> "自定义"
    }

private fun anniversaryStatusText(anniversary: Anniversary): String {
    val date = LocalDate.ofEpochDay(anniversary.dateEpochDay)
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
    return if (years <= 0) {
        "未满 1 年"
    } else {
        "已满 $years 年"
    }
}

private fun String.visibleTextOrNull(): String? {
    val value = trim()
    return value.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
}
