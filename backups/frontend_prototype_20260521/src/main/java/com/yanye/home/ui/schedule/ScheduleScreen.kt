package com.yanye.home.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.MemoryMood
import com.yanye.home.domain.model.Schedule
import com.yanye.home.domain.model.Visibility
import com.yanye.home.domain.model.Wish
import com.yanye.home.domain.model.WishCategory
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.theme.YanYeColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = viewModel()
) {
    val schedules by viewModel.schedules.collectAsState()
    val wishes by viewModel.wishes.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncSchedules,
        onFlushSync = viewModel::flushSync
    )
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var compactCalendar by remember { mutableStateOf(false) }
    var expandedScheduleId by remember { mutableStateOf<Long?>(null) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var archivingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var editingMemory by remember { mutableStateOf<Memory?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val selectedEpochDay = selectedDate.toEpochDay()
    val daySchedules = schedules.filter { it.startEpochDay == selectedEpochDay }
    val dayMemories = memories.filter { it.dateEpochDay == selectedEpochDay }

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
                ScheduleHeader(
                    totalCount = schedules.size,
                    memoryCount = memories.size,
                    syncMessage = syncMessage,
                    onSyncClick = viewModel::syncSchedules,
                    onClearClick = { showClearConfirm = true }
                )
            }
            item {
                CalendarPanel(
                    month = calendarMonth,
                    selectedDate = selectedDate,
                    compact = compactCalendar,
                    schedules = schedules,
                    onPreviousMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                    onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                    onToggleCompact = { compactCalendar = !compactCalendar },
                    onDateSelected = { date ->
                        selectedDate = date
                        calendarMonth = YearMonth.from(date)
                    }
                )
            }
            item {
                Text(
                    text = "${formatDate(selectedDate)} 的日程",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (daySchedules.isEmpty()) {
                item {
                    EmptyScheduleCard(
                        onCreateClick = {
                            editingSchedule = null
                            showEditor = true
                        }
                    )
                }
            } else {
                items(
                    items = daySchedules,
                    key = { schedule -> schedule.id }
                ) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        linkedWish = wishes.firstOrNull { it.id == schedule.linkedWishId },
                        expanded = expandedScheduleId == schedule.id,
                        onToggleExpanded = {
                            expandedScheduleId = if (expandedScheduleId == schedule.id) null else schedule.id
                        },
                        onEditClick = {
                            editingSchedule = schedule
                            showEditor = true
                        },
                        onArchiveClick = { archivingSchedule = schedule },
                        onDeleteClick = { viewModel.deleteSchedule(schedule.id) }
                    )
                }
            }
            if (dayMemories.isNotEmpty()) {
                item {
                    Text(
                        text = "这天的回忆卡",
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(
                    items = dayMemories,
                    key = { memory -> memory.id }
                ) { memory ->
                    MemoryCard(
                        memory = memory,
                        onEditClick = { editingMemory = memory }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingSchedule = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            containerColor = YanYeColors.Green,
            contentColor = Color.White
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (showEditor) {
        ScheduleEditorDialog(
            initialSchedule = editingSchedule,
            initialDate = selectedDate,
            wishes = wishes.filter { wish ->
                wish.scheduleReady &&
                    (wish.id == editingSchedule?.linkedWishId ||
                        (!wish.isCompleted && wish.linkedScheduleId == null))
            },
            onDismiss = { showEditor = false },
            onSave = { schedule ->
                viewModel.saveSchedule(schedule)
                selectedDate = LocalDate.ofEpochDay(schedule.startEpochDay)
                calendarMonth = YearMonth.from(selectedDate)
                showEditor = false
            }
        )
    }

    archivingSchedule?.let { schedule ->
        ArchiveScheduleDialog(
            schedule = schedule,
            onDismiss = { archivingSchedule = null },
            onArchive = { memory ->
                viewModel.completeAndArchive(schedule, memory)
                archivingSchedule = null
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
            text = { Text("会删除本机所有日程和回忆卡，并解除愿望里的日程关联。云端集合你已经手动清空，这里只处理本机数据。") },
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
private fun ScheduleHeader(
    totalCount: Int,
    memoryCount: Int,
    syncMessage: String?,
    onSyncClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "情侣日历",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSyncClick) {
                Text("同步", color = YanYeColors.Green)
            }
            TextButton(onClick = onClearClick) {
                Text("清空", color = YanYeColors.Rose)
            }
        }
        Text(
            text = "把愿望变成具体时间，把完成的日程沉淀成回忆。",
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
                    text = "$totalCount 个共同事件 · $memoryCount 张回忆卡",
                    color = YanYeColors.Green,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = syncMessage ?: "旅行愿望会自动建议开启攻略模式，餐厅、活动、路线和备选方案可以直接写在事件里。",
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarPanel(
    month: YearMonth,
    selectedDate: LocalDate,
    compact: Boolean,
    schedules: List<Schedule>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleCompact: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onPreviousMonth) { Text("上月") }
                Text(
                    text = "${month.year} 年 ${month.monthValue} 月",
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onNextMonth) { Text("下月") }
            }
            TextButton(onClick = onToggleCompact) {
                Text(if (compact) "展开月历" else "缩短到一行")
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
                Spacer(modifier = Modifier.height(6.dp))
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
    val background = when {
        selected -> YanYeColors.Green
        hasSchedule -> YanYeColors.GreenSoft
        else -> YanYeColors.Soft
    }
    val contentColor = when {
        selected -> Color.White
        !inCurrentMonth -> YanYeColors.Muted.copy(alpha = 0.45f)
        else -> YanYeColors.Ink
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(background, MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (hasSchedule || selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyScheduleCard(onCreateClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "这天还没有安排",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "可以新建共同事件，或从愿望清单里挑一个变成日程。",
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = onCreateClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("新增日程")
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    linkedWish: Wish?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable(onClick = onToggleExpanded),
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
                        .background(
                            if (schedule.isGuideMode) YanYeColors.GoldSoft else YanYeColors.GreenSoft,
                            CircleShape
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (schedule.isGuideMode) "攻" else "约",
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.title,
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${timeLabel(schedule.startMinuteOfDay)} · ${schedule.locationName.ifBlank { "地点待定" }}",
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(schedule.participants) })
                schedule.budgetCents?.let {
                    AssistChip(onClick = {}, label = { Text("预算 ${centsToBudgetText(it)}") })
                }
                linkedWish?.let {
                    AssistChip(onClick = {}, label = { Text("愿望：${it.title}") })
                }
                if (schedule.isCompleted) {
                    AssistChip(onClick = {}, label = { Text("已归档") })
                }
            }

            if (expanded) {
                ScheduleDetail(schedule = schedule, linkedWish = linkedWish)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onEditClick) { Text("编辑") }
                    if (!schedule.isCompleted) {
                        TextButton(onClick = onArchiveClick) { Text("完成归档") }
                    }
                    TextButton(onClick = onDeleteClick) {
                        Text("删除", color = YanYeColors.Rose)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDetail(
    schedule: Schedule,
    linkedWish: Wish?
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        DetailLine("提醒", "提前 ${schedule.reminderMinutesBefore} 分钟")
        linkedWish?.preparationItems?.takeIf(String::isNotBlank)?.let {
            DetailLine("愿望准备", it)
        }
        schedule.note.takeIf(String::isNotBlank)?.let {
            DetailLine("备注", it)
        }
        if (schedule.isGuideMode) {
            DetailLine("攻略餐厅", schedule.guideRestaurants.ifBlank { "待补充" })
            DetailLine("活动安排", schedule.guideActivities.ifBlank { "待补充" })
            DetailLine("路线", schedule.guideRoute.ifBlank { "待补充" })
            DetailLine("备选方案", schedule.backupPlan.ifBlank { "待补充" })
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text(
        text = "$label：$value",
        color = YanYeColors.Muted,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun MemoryCard(
    memory: Memory,
    onEditClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
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
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (memory.foodNotes.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text("吃了：${memory.foodNotes}") })
                }
                memory.expenseCents?.let {
                    AssistChip(onClick = {}, label = { Text("花费 ${centsToBudgetText(it)}") })
                }
                if (memory.photoUris.isNotBlank()) {
                    AssistChip(onClick = {}, label = { Text("有照片") })
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
                item {
                    OutlinedTextField(
                        value = photoUris,
                        onValueChange = { photoUris = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("照片路径或 URI") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = foodNotes,
                        onValueChange = { foodNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("吃了什么") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = expense,
                        onValueChange = { value -> expense = value.filter { it.isDigit() || it == '.' }.take(10) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("实际花费") }
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
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("回忆备注") }
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditorDialog(
    initialSchedule: Schedule?,
    initialDate: LocalDate,
    wishes: List<Wish>,
    onDismiss: () -> Unit,
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
    val linkedWish = wishes.firstOrNull { it.id == linkedWishId }
    val canSave = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialSchedule == null) "新增日程" else "编辑日程") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (wishes.isNotEmpty()) {
                        ChipGroupTitle("关联愿望")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = linkedWishId == null,
                                onClick = { linkedWishId = null },
                                label = { Text("不关联") }
                            )
                            wishes.forEach { wish ->
                                FilterChip(
                                    selected = linkedWishId == wish.id,
                                    onClick = {
                                        linkedWishId = wish.id
                                        if (title.isBlank()) title = wish.title
                                        if (locationName.isBlank()) locationName = wish.locationName
                                        if (budget.isBlank()) budget = wish.budgetCents?.let(::centsToBudgetText).orEmpty()
                                        if (note.isBlank()) note = wish.preparationItems
                                        if (wish.category == WishCategory.Travel) isGuideMode = true
                                    },
                                    label = { Text(wish.title) }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("标题") }
                    )
                }
                item {
                    Button(onClick = { showDatePicker = true }) {
                        Text("日期：${formatDate(date)}")
                    }
                }
                item {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it.take(5) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("开始时间，例如 19:30") }
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
                    OutlinedTextField(
                        value = participants,
                        onValueChange = { participants = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("参与人") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = budget,
                        onValueChange = { value -> budget = value.filter { it.isDigit() || it == '.' }.take(10) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("预算") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = reminderMinutes,
                        onValueChange = { value -> reminderMinutes = value.filter(Char::isDigit).take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("提前提醒分钟") }
                    )
                }
                item {
                    ToggleRow(
                        title = linkedWish?.let { "攻略模式，来自 ${it.title}" } ?: "攻略模式",
                        checked = isGuideMode,
                        onCheckedChange = { isGuideMode = it }
                    )
                }
                if (isGuideMode) {
                    item {
                        OutlinedTextField(
                            value = guideRestaurants,
                            onValueChange = { guideRestaurants = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            label = { Text("餐厅") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = guideActivities,
                            onValueChange = { guideActivities = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            label = { Text("活动") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = guideRoute,
                            onValueChange = { guideRoute = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            label = { Text("路线") }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = backupPlan,
                            onValueChange = { backupPlan = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            label = { Text("备选方案") }
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("备注或准备事项") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
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
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

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
                item {
                    OutlinedTextField(
                        value = photoUris,
                        onValueChange = { photoUris = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("照片路径或 URI") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = foodNotes,
                        onValueChange = { foodNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("吃了什么") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = expense,
                        onValueChange = { value -> expense = value.filter { it.isDigit() || it == '.' }.take(10) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("实际花费") }
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
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        label = { Text("回忆备注") }
                    )
                }
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
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = pickerState)
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

private fun timeLabel(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

private fun parseTimeToMinute(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

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

private fun moodLabel(mood: MemoryMood): String =
    when (mood) {
        MemoryMood.Happy -> "开心"
        MemoryMood.Touched -> "感动"
        MemoryMood.TiredButWorth -> "累但值得"
        MemoryMood.Ordinary -> "平常但很好"
    }
