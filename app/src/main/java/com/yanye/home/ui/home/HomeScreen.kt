package com.yanye.home.ui.home

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.data.session.UserSession
import com.yanye.home.domain.model.Anniversary
import com.yanye.home.domain.model.AnniversaryDisplayMode
import com.yanye.home.domain.model.AnniversaryType
import com.yanye.home.domain.model.Memory
import com.yanye.home.domain.model.Memo
import com.yanye.home.domain.model.Schedule
import com.yanye.home.navigation.YanYeDestination
import com.yanye.home.ui.common.ImagePreviewBox
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.PrimaryPageHeader
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.delay

private val HomeSummaryCardMinHeight = 64.dp
private val HomeCardHorizontalPadding = 11.dp
private val HomeCardVerticalPadding = 9.dp
private val HomeCardGap = 9.dp
private val HomeBodyLineHeight = 20.sp
private val HomeSmallLineHeight = 18.sp
private val HomeLeadingIconSize = 46.dp
private val HomeCardBorderColor = YanYeColors.Line.copy(alpha = 0.42f)
private val HomeDividerColor = YanYeColors.Line.copy(alpha = 0.38f)
private val HomeRoseBorderColor = Color(0xFFFFC4CE).copy(alpha = 0.58f)
private val HomeGlassBorderColor = Color.White.copy(alpha = 0.74f)
private val HomeSectionTitleStyleSize = 13.sp

@Composable
fun HomeScreen(
    session: UserSession,
    viewModel: HomeViewModel = viewModel(),
    onNavigate: (String) -> Unit = {}
) {
    val anniversaries by viewModel.anniversaries.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val memos by viewModel.memos.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val homeAnniversarySummary = rememberHomeAnniversarySummary(anniversaries)
    val todayMemory = rememberTodayMemory(memories)
    val todayEpochDay = LocalDate.now().toEpochDay()
    val todaySchedules = schedules
        .filter { it.startEpochDay == todayEpochDay }
        .sortedBy { it.startMinuteOfDay }
    val pendingMemos = memos.filterNot { it.isCompleted }
    val selectedHomeMemos = pendingMemos
        .filter { it.showOnHome }
        .sortedWith(compareBy<Memo> { it.homeSortOrder }.thenBy { it.reminderAtMillis ?: Long.MAX_VALUE })
    val homeMemos = if (selectedHomeMemos.isNotEmpty()) {
        selectedHomeMemos
    } else {
        pendingMemos.sortedWith(homeMemoComparator()).take(3)
    }

    WallpaperBackground(imageResId = R.drawable.home_wallpaper) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PageChrome.primaryPadding)
    ) {
        PrimaryPageHeader(title = session.spaceName?.takeIf { it.isNotBlank() } ?: "妍叶之庭")
        Spacer(modifier = Modifier.height(10.dp))

        TogetherHeroCard(summary = homeAnniversarySummary)

        if (homeAnniversarySummary.displayRows.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            AnniversaryHomeCard(
                summary = homeAnniversarySummary,
                onClick = {
                    onNavigate(YanYeDestination.Schedule.withTab(YanYeDestination.Schedule.TAB_ANNIVERSARY))
                }
            )

            Spacer(modifier = Modifier.height(HomeCardGap))
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }

        ScheduleHomeCard(
            schedules = todaySchedules,
            onClick = { onNavigate(YanYeDestination.Schedule.route) }
        )

        Spacer(modifier = Modifier.height(HomeCardGap))

        QuickActions(onNavigate = onNavigate)

        Spacer(modifier = Modifier.height(HomeCardGap))

        TodoCard(
            memos = homeMemos,
            hasPendingMemos = pendingMemos.isNotEmpty(),
            onClick = { onNavigate(YanYeDestination.Memo.route) },
            onToggleCompleted = { memo -> viewModel.setMemoCompleted(memo.id, true) }
        )

        Spacer(modifier = Modifier.height(HomeCardGap))

        TodayMemoryCard(
            memory = todayMemory,
            onClick = {
                todayMemory?.let { memory ->
                    onNavigate(YanYeDestination.Memory.withMemory(memory.id))
                }
            }
        )
    }
    }
}

@Composable
private fun TogetherHeroCard(summary: HomeAnniversarySummary) {
    val startDateText = summary.relationship?.dateEpochDay?.let {
        LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    } ?: "未设置"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Text(
            text = "我们在一起",
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 23.sp),
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${summary.relationshipDays}",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 46.sp, lineHeight = 48.sp),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "天",
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 22.sp),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 7.dp, bottom = 5.dp)
            )
        }
        Text(
            text = "从 $startDateText 开始",
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

@Composable
private fun HomeGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.8.dp, HomeGlassBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.82f),
                            Color(0xFFFFF6F8).copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.78f)
                        )
                    )
                ),
            content = content
        )
    }
}

@Composable
private fun CardHeader(
    iconResId: Int,
    title: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(accent.copy(alpha = 0.16f), RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(accent),
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = title,
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeSmallLineHeight),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LeadingIcon(
    iconResId: Int,
    background: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(HomeLeadingIconSize),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(HomeLeadingIconSize)
        )
    }
}

@Composable
private fun HomeIconTitleBlock(
    iconResId: Int,
    title: String,
    headline: String,
    background: Color,
    accent: Color,
    headlineColor: Color = YanYeColors.Ink,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LeadingIcon(iconResId = iconResId, background = background, accent = accent)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeSmallLineHeight),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = headline,
                color = headlineColor,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 25.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun HomeSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, HomeCardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeCardHorizontalPadding, vertical = HomeCardVerticalPadding),
            content = content
        )
    }
}

@Composable
private fun ScheduleHomeCard(
    schedules: List<Schedule>,
    onClick: () -> Unit
) {
    HomeSurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LeadingIcon(
                iconResId = R.drawable.home_icon_schedule,
                background = Color(0xFFE7EEFF),
                accent = Color(0xFF7A95FF)
            )
            Column(modifier = Modifier.weight(1f)) {
                HomeSectionTitle("今日日程")
                Spacer(modifier = Modifier.height(5.dp))
                if (schedules.isEmpty()) {
                    Text(
                        text = "今天还没有日程哦，请新建日程",
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = HomeBodyLineHeight),
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    schedules.forEachIndexed { index, schedule ->
                        HomeScheduleRow(schedule = schedule)
                        if (index != schedules.lastIndex) {
                            HorizontalDivider(
                                color = HomeDividerColor,
                                thickness = 0.6.dp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(text: String) {
    Text(
        text = text,
        color = YanYeColors.Ink,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = HomeSectionTitleStyleSize, lineHeight = HomeSmallLineHeight),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AnniversaryHomeCard(
    summary: HomeAnniversarySummary,
    onClick: () -> Unit
) {
    HomeGlassCard(onClick = onClick) {
        val rows = summary.displayRows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            rows.forEachIndexed { index, row ->
                AnniversaryHomeRow(row = row)
                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.92f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnniversaryHomeRow(row: HomeAnniversaryDisplayRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = row.dateText,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1
            )
        }
        Text(
            text = row.value,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeScheduleRow(schedule: Schedule) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp, end = 8.dp)
                .size(7.dp)
                .background(Color(0xFF42D6BD), CircleShape)
        )
        Text(
            text = homeTimeLabel(schedule.startMinuteOfDay),
            color = YanYeColors.Muted,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeBodyLineHeight),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(end = 10.dp)
                .alignByBaseline()
        )
        Text(
            text = schedule.title,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeBodyLineHeight),
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline()
        )
    }
}

@Composable
private fun HomeModuleCard(
    label: String,
    headline: String,
    detail: String,
    accent: Color,
    showArrow: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HomeSummaryCardMinHeight)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, HomeCardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = headline,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 5.dp)
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = detail,
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (showArrow) {
                    ArrowBadge()
                }
            }
        }
    }
}

@Composable
private fun QuickActions(onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, HomeCardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionChip(
                text = "吃什么",
                iconResId = R.drawable.home_icon_food,
                background = Color(0xFFFFE6BA),
                accent = Color(0xFFF2A12C),
                modifier = Modifier.weight(1f)
            ) {
                onNavigate(YanYeDestination.Food.route)
            }
            QuickActionDivider()
            QuickActionChip(
                text = "新日程",
                iconResId = R.drawable.home_icon_new_schedule,
                background = Color(0xFFE2EAFF),
                accent = Color(0xFF7A95FF),
                modifier = Modifier.weight(1f)
            ) {
                onNavigate(YanYeDestination.Schedule.route)
            }
            QuickActionDivider()
            QuickActionChip(
                text = "记回忆",
                iconResId = R.drawable.home_icon_memory,
                background = Color(0xFFDDF9EF),
                accent = Color(0xFF37CDAF),
                modifier = Modifier.weight(1f)
            ) {
                onNavigate(YanYeDestination.Memory.route)
            }
            QuickActionDivider()
            QuickActionChip(
                text = "冷静",
                iconResId = R.drawable.home_icon_care,
                background = Color(0xFFFFE7EE),
                accent = YanYeColors.Rose,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    text: String,
    iconResId: Int,
    background: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = text,
            color = YanYeColors.Ink,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeBodyLineHeight),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun QuickActionDivider() {
    Box(
        modifier = Modifier
            .height(46.dp)
            .width(0.6.dp)
            .background(HomeDividerColor)
    )
}

@Composable
private fun ArrowBadge() {
    Box(
        modifier = Modifier
            .background(YanYeColors.Rose, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "→",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class HomeAnniversarySummary(
    val relationship: Anniversary?,
    val relationshipDays: Int,
    val relationshipText: String,
    val extraRows: List<HomeAnniversaryRow>,
    val displayRows: List<HomeAnniversaryDisplayRow>
)

private data class HomeAnniversaryRow(
    val text: String,
    val anniversary: Anniversary
)

private data class HomeAnniversaryDisplayRow(
    val title: String,
    val value: String,
    val dateText: String
)

private fun rememberHomeAnniversarySummary(anniversaries: List<Anniversary>): HomeAnniversarySummary {
    val displayItems = anniversaries
        .filter { it.showOnHome }
        .sortedWith(compareBy<Anniversary> { it.homeSortOrder }.thenBy { it.dateEpochDay })
    val relationship = anniversaries.firstOrNull { it.name == "在一起" }
        ?: anniversaries.firstOrNull {
            it.type == AnniversaryType.Relationship && it.displayMode == AnniversaryDisplayMode.CountUp
        }
    val relationshipDays = relationship?.let { anniversary ->
        ChronoUnit.DAYS.between(LocalDate.ofEpochDay(anniversary.dateEpochDay), LocalDate.now()).toInt().coerceAtLeast(0)
    } ?: 0
    val relationshipText = if (relationship != null) {
        "在一起 $relationshipDays 天"
    } else {
        "还没有设置纪念日"
    }
    val extraRows = displayItems
        .take(3)
        .map { HomeAnniversaryRow(text = anniversaryHomeText(it), anniversary = it) }
    val displayRows = buildList {
        extraRows.forEach { row ->
            add(
                HomeAnniversaryDisplayRow(
                    title = row.anniversary.name.ifBlank { "纪念日" },
                    value = anniversaryMetricText(row.anniversary),
                    dateText = formatAnniversaryCardDate(row.anniversary)
                )
            )
        }
    }
    return HomeAnniversarySummary(
        relationship = relationship,
        relationshipDays = relationshipDays,
        relationshipText = relationshipText,
        extraRows = extraRows,
        displayRows = displayRows.take(3)
    )
}

private fun anniversaryHomeText(anniversary: Anniversary): String {
    val date = LocalDate.ofEpochDay(anniversary.dateEpochDay)
    val today = LocalDate.now()
    return when (anniversary.displayMode) {
        AnniversaryDisplayMode.CountUp -> {
            val days = ChronoUnit.DAYS.between(date, today)
            if (days >= 0) "${anniversary.name} $days 天" else "${anniversary.name} ${abs(days)} 天"
        }
        AnniversaryDisplayMode.Countdown -> {
            val days = ChronoUnit.DAYS.between(today, nextOccurrenceDate(date, today))
            "${anniversary.name} $days 天"
        }
        AnniversaryDisplayMode.Anniversary -> {
            if (date.isAfter(today)) {
                "${anniversary.name} 未开始"
            } else {
                val years = java.time.Period.between(date, today).years
                if (years <= 0) "${anniversary.name} 未满 1 年" else "${anniversary.name} $years 年"
            }
        }
    }
}

private fun anniversaryMetricText(anniversary: Anniversary): String {
    val date = LocalDate.ofEpochDay(anniversary.dateEpochDay)
    val today = LocalDate.now()
    return when (anniversary.displayMode) {
        AnniversaryDisplayMode.CountUp -> {
            val days = ChronoUnit.DAYS.between(date, today).coerceAtLeast(0)
            "$days 天"
        }
        AnniversaryDisplayMode.Countdown -> {
            val days = ChronoUnit.DAYS.between(today, nextOccurrenceDate(date, today)).coerceAtLeast(0)
            "还有 $days 天"
        }
        AnniversaryDisplayMode.Anniversary -> {
            if (date.isAfter(today)) {
                "未开始"
            } else {
                val years = java.time.Period.between(date, today).years
                if (years <= 0) "未满1年" else "$years 周年"
            }
        }
    }
}

private fun formatAnniversaryCardDate(anniversary: Anniversary): String =
    LocalDate.ofEpochDay(anniversary.dateEpochDay).format(DateTimeFormatter.ofPattern("MM.dd"))

private fun nextOccurrenceDate(date: LocalDate, today: LocalDate): LocalDate {
    if (date.isAfter(today)) return date
    var next = date.withYear(today.year)
    if (!next.isAfter(today)) next = next.plusYears(1)
    return next
}

private fun homeTimeLabel(minuteOfDay: Int): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

@Composable
private fun HomeDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = YanYeColors.Line,
        thickness = 1.dp
    )
}

@Composable
private fun TodayMemoryCard(
    memory: Memory?,
    onClick: () -> Unit
) {
    val memoryDate = memory?.let { LocalDate.ofEpochDay(it.dateEpochDay) }
    val header = memoryDate?.let { "那年今天 · ${it.format(DateTimeFormatter.ofPattern("yyyy/M/d"))}" }
        ?: "那年今天"
    val title = memory?.title?.takeIf { it.isNotBlank() }
        ?: "那年的今天没有记录回忆，不过一定也是美好的一天～"
    val detail = memory?.note?.takeIf { it.isNotBlank() }
        ?: memory?.foodNotes?.takeIf { it.isNotBlank() }
        ?: memory?.locationName?.takeIf { it.isNotBlank() }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (memory != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.8.dp, HomeRoseBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFFF1F4), Color.White))
                )
                .padding(horizontal = HomeCardHorizontalPadding, vertical = HomeCardVerticalPadding)
        ) {
            Text(
                text = "♡♡",
                color = Color(0xFFFFC4CE).copy(alpha = 0.45f),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LeadingIcon(
                        iconResId = R.drawable.home_icon_today_memory,
                        background = Color(0xFFFFE4EC),
                        accent = YanYeColors.Rose
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = header,
                            color = YanYeColors.Rose,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeSmallLineHeight),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = title,
                            color = YanYeColors.Ink,
                            style = if (memory == null) {
                                MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeBodyLineHeight)
                            } else {
                                MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)
                            },
                            fontWeight = if (memory == null) FontWeight.Medium else FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
                if (detail != null) {
                    Text(
                        text = detail,
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = HomeBodyLineHeight),
                        modifier = Modifier.padding(top = 9.dp)
                    )
                }
                firstMemoryPhotoUri(memory?.photoUris.orEmpty())?.let { uri ->
                    HomeMemoryPhotoPreview(
                        photoUri = uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeMemoryPhotoPreview(
    photoUri: String,
    modifier: Modifier = Modifier
) {
    ImagePreviewBox(
        imageUri = photoUri,
        contentDescription = "回忆图片",
        modifier = modifier,
        minHeight = 0.dp,
        maxHeight = 360.dp
    )
}

private fun firstMemoryPhotoUri(photoUris: String): String? =
    photoUris.split('\n', ',', ';').map(String::trim).firstOrNull { it.isNotBlank() }

private fun rememberTodayMemory(memories: List<Memory>): Memory? {
    val today = LocalDate.now()
    return memories
        .filterNot { it.isDeleted }
        .filter { memory ->
            val date = LocalDate.ofEpochDay(memory.dateEpochDay)
            date.month == today.month && date.dayOfMonth == today.dayOfMonth && date.year < today.year
        }
        .maxByOrNull { it.dateEpochDay }
}

@Composable
private fun TodoCard(
    memos: List<Memo>,
    hasPendingMemos: Boolean,
    onClick: () -> Unit,
    onToggleCompleted: (Memo) -> Unit
) {
    HomeSurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LeadingIcon(
                iconResId = R.drawable.home_icon_memo,
                background = Color(0xFFF1DEFF),
                accent = Color(0xFFB77CFF)
            )
            Column(modifier = Modifier.weight(1f)) {
                HomeSectionTitle("备忘录")
                Spacer(modifier = Modifier.height(5.dp))
                if (!hasPendingMemos) {
                    Text(
                        text = "还没有待办哦，快去加入备忘录吧！",
                        color = YanYeColors.Ink,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = HomeBodyLineHeight),
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    memos.forEachIndexed { index, memo ->
                        HomeMemoRow(
                            memo = memo,
                            modifier = Modifier.padding(top = if (index == 0) 0.dp else 6.dp),
                            onToggleCompleted = { onToggleCompleted(memo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMemoRow(
    memo: Memo,
    modifier: Modifier = Modifier,
    onToggleCompleted: () -> Unit
) {
    var completing by remember(memo.id) { mutableStateOf(false) }
    LaunchedEffect(completing) {
        if (completing) {
            delay(420)
            onToggleCompleted()
        }
    }
    val dueText = memoDueText(memo)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = if (completing) YanYeColors.Green.copy(alpha = 0.45f) else Color.Transparent,
                    shape = RoundedCornerShape(3.dp)
                )
                .border(
                    width = if (completing) 0.dp else 1.4.dp,
                    color = YanYeColors.Line,
                    shape = RoundedCornerShape(3.dp)
                )
                .clickable(enabled = !completing) { completing = true },
            contentAlignment = Alignment.Center
        ) {}
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = memo.title,
                color = if (completing) YanYeColors.Green else YanYeColors.Ink,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeBodyLineHeight),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (completing) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f, fill = false)
            )
            AnimatedVisibility(visible = dueText != null) {
                Text(
                    text = dueText.orEmpty(),
                    color = YanYeColors.Muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = HomeBodyLineHeight),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

private fun homeMemoComparator(): Comparator<Memo> =
    compareBy<Memo> { it.reminderAtMillis ?: Long.MAX_VALUE }
        .thenByDescending { it.updatedAt }

private fun memoDueText(memo: Memo): String? =
    when {
        memo.dueLabel == "DATE_ONLY" && memo.reminderAtMillis != null ->
            formatMemoDateOnly(
                Instant.ofEpochMilli(memo.reminderAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            )
        memo.dueLabel == "DATE_TIME" && memo.reminderAtMillis != null -> formatMemoReminder(memo.reminderAtMillis)
        memo.dueLabel.isNotBlank() && memo.reminderAtMillis == null -> memo.dueLabel
        memo.reminderAtMillis != null -> formatMemoReminder(memo.reminderAtMillis)
        else -> null
    }

private fun formatMemoDateOnly(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> date.format(DateTimeFormatter.ofPattern("M/d"))
    }
}

private fun formatMemoReminder(millis: Long): String {
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val today = LocalDate.now()
    val dateLabel = when (dateTime.toLocalDate()) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> dateTime.toLocalDate().format(DateTimeFormatter.ofPattern("M/d"))
    }
    return "$dateLabel ${dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}"
}
