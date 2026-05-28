package com.yanye.home.ui.care

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.domain.model.CareCycle
import com.yanye.home.domain.model.CareMood
import com.yanye.home.domain.model.CarePainLevel
import com.yanye.home.ui.common.AutoSyncLifecycleEffect
import com.yanye.home.ui.theme.YanYeColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val DEFAULT_PERIOD_LENGTH_DAYS = 5L
private const val LUTEAL_DAYS = 14L

private enum class CyclePhase {
    ActualPeriod,
    PredictedPeriod,
    OvulationWindow,
    OvulationDay,
    None
}

private sealed interface PeriodAction {
    data object StartPeriod : PeriodAction
    data class CancelPeriod(val cycle: CareCycle) : PeriodAction
    data class EndPeriod(val cycle: CareCycle) : PeriodAction
}

@Composable
fun CareScreen(
    viewModel: CareViewModel = viewModel()
) {
    val careCycles by viewModel.careCycles.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    AutoSyncLifecycleEffect(
        onEnterSync = viewModel::syncCareCycles,
        onFlushSync = viewModel::flushSync
    )
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var cycleLengthText by remember { mutableStateOf("28") }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showCycleSetting by remember { mutableStateOf(false) }
    val cycleLength = cycleLengthText.toIntOrNull()?.coerceIn(21, 45) ?: 28
    val actualPeriodDates = remember(careCycles) { actualPeriodDates(careCycles) }
    val predictions = remember(careCycles, cycleLength) {
        periodPredictions(careCycles, cycleLength)
    }
    val latestPredictedPeriodDates = remember(careCycles, cycleLength) {
        latestPredictedPeriodDates(careCycles, cycleLength)
    }
    val editableCycle = editableCycleForDate(selectedDate, careCycles)
    val selectedRecord = careCycleForDate(selectedDate, careCycles) ?: editableCycle
    val periodAction = periodActionForDate(selectedDate, editableCycle)
    val openPredictedPeriodDates = remember(careCycles) { openPredictedPeriodDates(careCycles) }
    var painLevel by remember { mutableStateOf(CarePainLevel.None) }
    var mood by remember { mutableStateOf(CareMood.Stable) }
    var discharge by remember { mutableStateOf("") }
    var carePreference by remember { mutableStateOf("") }

    LaunchedEffect(selectedRecord?.id, selectedDate) {
        painLevel = selectedRecord?.painLevel ?: CarePainLevel.None
        mood = selectedRecord?.mood ?: CareMood.Stable
        discharge = selectedRecord?.avoidNotes.orEmpty()
        carePreference = selectedRecord?.carePreference.orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(YanYeColors.Paper),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PeriodHeader(
                month = visibleMonth,
                syncMessage = syncMessage,
                onJumpClick = { showJumpDialog = true },
                onCycleSettingClick = { showCycleSetting = true },
                onSyncClick = viewModel::syncCareCycles
            )
        }
        item {
            PeriodCalendar(
                month = visibleMonth,
                selectedDate = selectedDate,
                today = LocalDate.now(),
                actualPeriodDates = actualPeriodDates,
                openPredictedPeriodDates = openPredictedPeriodDates,
                latestPredictedPeriodDates = latestPredictedPeriodDates,
                predictions = predictions,
                onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                onDateSelected = { selectedDate = it }
            )
        }
        item {
            PhaseLegend()
        }
        item {
            SelectedDateRecord(
                selectedDate = selectedDate,
                periodAction = periodAction,
                painLevel = painLevel,
                mood = mood,
                discharge = discharge,
                carePreference = carePreference,
                onPeriodActionClick = {
                    when (periodAction) {
                        PeriodAction.StartPeriod -> {
                            viewModel.saveCareCycle(
                                CareCycle(
                                    startEpochDay = selectedDate.toEpochDay(),
                                    endEpochDay = null,
                                    cycleLengthDays = cycleLength,
                                    painLevel = painLevel,
                                    mood = mood,
                                    avoidNotes = discharge.trim(),
                                    carePreference = carePreference.trim()
                                )
                            )
                        }
                        is PeriodAction.EndPeriod -> {
                            val cycle = periodAction.cycle
                            viewModel.saveCareCycle(
                                cycle.copy(
                                    endEpochDay = selectedDate.toEpochDay(),
                                    cycleLengthDays = cycleLength,
                                    painLevel = painLevel,
                                    mood = mood,
                                    avoidNotes = discharge.trim(),
                                    carePreference = carePreference.trim()
                                )
                            )
                        }
                        is PeriodAction.CancelPeriod -> {
                            viewModel.deleteCareCycle(periodAction.cycle.id)
                        }
                    }
                },
                onPainChange = {
                    painLevel = it
                },
                onMoodChange = {
                    mood = it
                },
                onDischargeChange = {
                    discharge = it
                },
                onCarePreferenceChange = {
                    carePreference = it
                },
                canSaveDetails = selectedRecord != null,
                onSave = {
                    selectedRecord?.let { record ->
                        viewModel.saveCareCycle(
                            record.copy(
                                cycleLengthDays = cycleLength,
                                painLevel = painLevel,
                                mood = mood,
                                avoidNotes = discharge.trim(),
                                carePreference = carePreference.trim()
                            )
                        )
                    }
                }
            )
        }
    }

    if (showJumpDialog) {
        JumpMonthDialog(
            currentMonth = visibleMonth,
            onDismiss = { showJumpDialog = false },
            onJump = { month ->
                visibleMonth = month
                selectedDate = month.atDay(1)
                showJumpDialog = false
            }
        )
    }

    if (showCycleSetting) {
        CycleSettingDialog(
            cycleLengthText = cycleLengthText,
            onCycleLengthChange = { value ->
                cycleLengthText = value.filter(Char::isDigit).take(2)
            },
            onClearCareCycles = {
                viewModel.clearCareCycles()
                selectedDate = LocalDate.now()
                visibleMonth = YearMonth.now()
                showCycleSetting = false
            },
            onDismiss = { showCycleSetting = false }
        )
    }
}

@Composable
private fun PeriodHeader(
    month: YearMonth,
    syncMessage: String?,
    onJumpClick: () -> Unit,
    onCycleSettingClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onJumpClick) {
                Text("年月")
            }
            Text(
                text = "${month.monthValue}月",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSyncClick) {
                    Text("同步", color = YanYeColors.Muted)
                }
                TextButton(onClick = onCycleSettingClick) {
                    Text("周期")
                }
            }
        }
        syncMessage?.let { message ->
            Text(
                text = message,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun PeriodCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    actualPeriodDates: Set<LocalDate>,
    openPredictedPeriodDates: Set<LocalDate>,
    latestPredictedPeriodDates: Set<LocalDate>,
    predictions: List<PeriodPrediction>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var dragAmount by remember { mutableStateOf(0f) }
    Card(
        modifier = Modifier.pointerInput(month) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    when {
                        dragAmount > 80f -> onPreviousMonth()
                        dragAmount < -80f -> onNextMonth()
                    }
                    dragAmount = 0f
                },
                onHorizontalDrag = { _, amount ->
                    dragAmount += amount
                }
            )
        },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            monthGridDates(month).chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    week.forEach { date ->
                        PeriodDayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == month,
                            selected = date == selectedDate,
                            today = date == today,
                            phase = phaseForDate(
                                date = date,
                                actualPeriodDates = actualPeriodDates,
                                openPredictedPeriodDates = openPredictedPeriodDates,
                                latestPredictedPeriodDates = latestPredictedPeriodDates,
                                predictions = predictions
                            ),
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodDayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    phase: CyclePhase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = when (phase) {
        CyclePhase.ActualPeriod -> YanYeColors.Rose
        CyclePhase.PredictedPeriod -> YanYeColors.Rose.copy(alpha = 0.28f)
        CyclePhase.OvulationDay -> Color.White
        CyclePhase.OvulationWindow -> Color.White
        CyclePhase.None -> Color.White
    }
    val textColor = when {
        phase == CyclePhase.ActualPeriod -> Color.White
        phase == CyclePhase.OvulationDay || phase == CyclePhase.OvulationWindow -> Color(0xFFA85CF2)
        !inMonth -> YanYeColors.Muted.copy(alpha = 0.38f)
        phase == CyclePhase.None -> Color(0xFF57C878)
        else -> YanYeColors.Ink
    }
    val border = when {
        selected -> BorderStroke(2.dp, Color(0xFFE4465F))
        today -> BorderStroke(1.dp, YanYeColors.Line)
        else -> null
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(background, MaterialTheme.shapes.small)
            .then(if (border != null) Modifier.border(border, MaterialTheme.shapes.small) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected || phase != CyclePhase.None) FontWeight.SemiBold else FontWeight.Normal
            )
            if (today) {
                Text(
                    text = "今天",
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall
                )
            } else if (phase == CyclePhase.OvulationDay) {
                Text(
                    text = "★",
                    color = Color(0xFFA85CF2),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun PhaseLegend() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LegendItem("月经期", YanYeColors.Rose)
        LegendItem("预测经期", YanYeColors.Rose.copy(alpha = 0.28f))
        LegendItem("排卵期", Color(0xFFA85CF2))
        LegendItem("排卵日", Color(0xFFA85CF2))
    }
}

@Composable
private fun LegendItem(
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .background(color, CircleShape)
                .padding(6.dp)
        )
        Text(text = text, color = YanYeColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SelectedDateRecord(
    selectedDate: LocalDate,
    periodAction: PeriodAction,
    painLevel: CarePainLevel,
    mood: CareMood,
    discharge: String,
    carePreference: String,
    onPeriodActionClick: () -> Unit,
    onPainChange: (CarePainLevel) -> Unit,
    onMoodChange: (CareMood) -> Unit,
    onDischargeChange: (String) -> Unit,
    onCarePreferenceChange: (String) -> Unit,
    canSaveDetails: Boolean,
    onSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = formatDate(selectedDate),
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            RecordRow(title = "经期状态") {
                Button(onClick = onPeriodActionClick) {
                    Text(
                        when (periodAction) {
                            PeriodAction.StartPeriod -> "月经来了"
                            is PeriodAction.CancelPeriod -> "取消本次经期"
                            is PeriodAction.EndPeriod -> "月经走了"
                        }
                    )
                }
            }
            RecordRow(title = "症状") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarePainLevel.entries.forEach { level ->
                        FilterChip(
                            selected = painLevel == level,
                            onClick = { onPainChange(level) },
                            label = { Text(painLabel(level)) }
                        )
                    }
                }
            }
            RecordRow(title = "心情") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareMood.entries.forEach { moodOption ->
                        FilterChip(
                            selected = mood == moodOption,
                            onClick = { onMoodChange(moodOption) },
                            label = { Text(moodLabel(moodOption)) }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = discharge,
                onValueChange = onDischargeChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("白带 / 忌口") }
            )
            OutlinedTextField(
                value = carePreference,
                onValueChange = onCarePreferenceChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("照顾偏好") }
            )
            Button(
                onClick = onSave,
                enabled = canSaveDetails,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("保存当天记录")
            }
        }
    }
}

@Composable
private fun JumpMonthDialog(
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onJump: (YearMonth) -> Unit
) {
    var yearText by remember(currentMonth) { mutableStateOf(currentMonth.year.toString()) }
    var monthText by remember(currentMonth) { mutableStateOf(currentMonth.monthValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转月份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("年份") }
                )
                OutlinedTextField(
                    value = monthText,
                    onValueChange = { monthText = it.filter(Char::isDigit).take(2) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("月份") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val year = yearText.toIntOrNull()
                    val month = monthText.toIntOrNull()
                    if (year != null && month != null && month in 1..12) {
                        onJump(YearMonth.of(year, month))
                    }
                }
            ) {
                Text("跳转")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun CycleSettingDialog(
    cycleLengthText: String,
    onCycleLengthChange: (String) -> Unit,
    onClearCareCycles: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("经期周期") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = cycleLengthText,
                    onValueChange = onCycleLengthChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("周期天数") },
                    suffix = { Text("天") }
                )
                TextButton(onClick = onClearCareCycles) {
                    Text("清空经期记录", color = YanYeColors.Rose)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("确定") } }
    )
}

@Composable
private fun RecordRow(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

private data class PeriodPrediction(
    val nextPeriodDates: Set<LocalDate>,
    val ovulationWindowDates: Set<LocalDate>,
    val ovulationDate: LocalDate
) {
    companion object {
        fun from(periodStart: LocalDate, cycleLengthDays: Int): PeriodPrediction {
            val nextStart = periodStart.plusDays(cycleLengthDays.toLong())
            val nextPeriod = (0 until DEFAULT_PERIOD_LENGTH_DAYS).map { nextStart.plusDays(it) }.toSet()
            val ovulation = nextStart.minusDays(LUTEAL_DAYS)
            val ovulationWindow = (-4L..1L).map { ovulation.plusDays(it) }.toSet()
            return PeriodPrediction(
                nextPeriodDates = nextPeriod,
                ovulationWindowDates = ovulationWindow,
                ovulationDate = ovulation
            )
        }
    }
}

private fun phaseForDate(
    date: LocalDate,
    actualPeriodDates: Set<LocalDate>,
    openPredictedPeriodDates: Set<LocalDate>,
    latestPredictedPeriodDates: Set<LocalDate>,
    predictions: List<PeriodPrediction>
): CyclePhase =
    when {
        date in actualPeriodDates -> CyclePhase.ActualPeriod
        date in openPredictedPeriodDates -> CyclePhase.PredictedPeriod
        predictions.any { date == it.ovulationDate } -> CyclePhase.OvulationDay
        date in latestPredictedPeriodDates -> CyclePhase.PredictedPeriod
        predictions.any { date in it.ovulationWindowDates } -> CyclePhase.OvulationWindow
        else -> CyclePhase.None
    }

private fun actualPeriodDates(cycles: List<CareCycle>): Set<LocalDate> =
    cycles
        .flatMap { cycle ->
            val start = LocalDate.ofEpochDay(cycle.startEpochDay)
            val end = cycle.endEpochDay?.let(LocalDate::ofEpochDay) ?: start
            (0..ChronoUnit.DAYS.between(start, end)).map { start.plusDays(it) }
        }
        .toSet()

private fun openPredictedPeriodDates(cycles: List<CareCycle>): Set<LocalDate> =
    cycles
        .filter { it.endEpochDay == null }
        .flatMap { cycle ->
            val start = LocalDate.ofEpochDay(cycle.startEpochDay)
            (1L..10L).map { start.plusDays(it) }
        }
        .toSet()

private fun latestPredictedPeriodDates(
    cycles: List<CareCycle>,
    fallbackCycleLength: Int
): Set<LocalDate> {
    val latestCycle = cycles.maxByOrNull { it.startEpochDay } ?: return emptySet()
    val start = LocalDate.ofEpochDay(latestCycle.startEpochDay)
    val length = latestCycle.cycleLengthDays.takeIf { it in 21..45 } ?: fallbackCycleLength
    val nextStart = start.plusDays(length.toLong())
    return (0 until DEFAULT_PERIOD_LENGTH_DAYS).map { nextStart.plusDays(it) }.toSet()
}

private fun careCycleForDate(
    date: LocalDate,
    cycles: List<CareCycle>
): CareCycle? =
    cycles.firstOrNull { cycle ->
        val start = LocalDate.ofEpochDay(cycle.startEpochDay)
        val end = cycle.endEpochDay?.let(LocalDate::ofEpochDay) ?: start
        !date.isBefore(start) && !date.isAfter(end)
    }

private fun editableCycleForDate(
    date: LocalDate,
    cycles: List<CareCycle>
): CareCycle? =
    cycles
        .filter { cycle ->
            val start = LocalDate.ofEpochDay(cycle.startEpochDay)
            val editLastDay = start.plusDays(10)
            !date.isBefore(start) && !date.isAfter(editLastDay)
        }
        .maxByOrNull { it.startEpochDay }

private fun periodActionForDate(
    date: LocalDate,
    editableCycle: CareCycle?
): PeriodAction {
    val cycle = editableCycle ?: return PeriodAction.StartPeriod
    val start = LocalDate.ofEpochDay(cycle.startEpochDay)
    return if (date == start) {
        PeriodAction.CancelPeriod(cycle)
    } else {
        PeriodAction.EndPeriod(cycle)
    }
}

private fun periodPredictions(
    cycles: List<CareCycle>,
    fallbackCycleLength: Int
): List<PeriodPrediction> =
    cycles
        .sortedBy { it.startEpochDay }
        .distinctBy { it.startEpochDay }
        .map { cycle ->
            val start = LocalDate.ofEpochDay(cycle.startEpochDay)
            val length = cycle.cycleLengthDays.takeIf { it in 21..45 } ?: fallbackCycleLength
            PeriodPrediction.from(start, length)
        }

private fun monthGridDates(month: YearMonth): List<LocalDate> {
    val firstDay = month.atDay(1)
    val offset = firstDay.dayOfWeek.value % 7
    val start = firstDay.minusDays(offset.toLong())
    return (0 until 42).map { start.plusDays(it.toLong()) }
}

private fun painLabel(level: CarePainLevel): String =
    when (level) {
        CarePainLevel.None -> "无"
        CarePainLevel.Mild -> "轻微"
        CarePainLevel.Medium -> "中等"
        CarePainLevel.Strong -> "明显"
    }

private fun moodLabel(mood: CareMood): String =
    when (mood) {
        CareMood.Stable -> "稳定"
        CareMood.Tired -> "累"
        CareMood.Sensitive -> "敏感"
        CareMood.Low -> "低落"
    }

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ISO_LOCAL_DATE)
