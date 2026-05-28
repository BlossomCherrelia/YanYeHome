package com.yanye.home.ui.space

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yanye.home.R
import com.yanye.home.navigation.YanYeDestination
import com.yanye.home.ui.footprint.FootprintViewModel
import com.yanye.home.ui.common.PageChrome
import com.yanye.home.ui.common.PrimaryPageHeader
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.schedule.ScheduleViewModel
import com.yanye.home.ui.theme.YanYeColors
import com.yanye.home.ui.wish.WishViewModel

@Composable
fun SpaceScreen(
    onNavigate: (String) -> Unit = {},
    wishViewModel: WishViewModel = viewModel(),
    footprintViewModel: FootprintViewModel = viewModel(),
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    val wishes by wishViewModel.wishes.collectAsState()
    val provinceLights by footprintViewModel.provinceLights.collectAsState()
    val memories by scheduleViewModel.memories.collectAsState()
    val litProvinceCount = provinceLights.count { it.isLit }
    val pendingWishCount = wishes.count { !it.isCompleted }

    WallpaperBackground(imageResId = R.drawable.simple_wallpaper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PageChrome.primaryPadding)
        ) {
            PrimaryPageHeader(title = "我们的空间")
            Spacer(modifier = Modifier.height(28.dp))

            SpaceHeroCard(
                litProvinceCount = litProvinceCount,
                onClick = { onNavigate(YanYeDestination.Footprint.route) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            SpaceLargeEntryCard(
                title = "愿望清单",
                value = pendingWishCount.toString(),
                subtitle = "个待实现",
                accent = YanYeColors.Blue,
                background = listOf(Color(0xFFEAF3FF), Color(0xFFF7F1FF)),
                backgroundImageResId = R.drawable.space_wish_card,
                aspectRatio = 5f / 2f,
                onClick = { onNavigate(YanYeDestination.Wish.route) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            SpaceLargeEntryCard(
                title = "回忆",
                value = memories.size.toString(),
                subtitle = "条共同回忆",
                accent = YanYeColors.Green,
                background = listOf(Color(0xFFEAF9F1), Color(0xFFFFF8EA)),
                backgroundImageResId = R.drawable.space_memory_card,
                aspectRatio = 5f / 2f,
                onClick = { onNavigate(YanYeDestination.Memory.route) }
            )

            Spacer(modifier = Modifier.height(18.dp))

            SpaceReadonlyModule(title = "冒险挑战", subtitle = "1.1版本上线")
            SpaceReadonlyModule(title = "时光信箱", subtitle = "1.1版本上线")
        }
    }
}

@Composable
private fun SpaceHeroCard(
    litProvinceCount: Int,
    onClick: () -> Unit
) {
    SpaceLargeEntryCard(
        title = "点亮地图",
        value = "$litProvinceCount / 34",
        subtitle = "省份已点亮",
        accent = YanYeColors.Green,
        background = listOf(Color(0xFFEAF9F1), Color(0xFFEAF3FF)),
        backgroundImageResId = R.drawable.space_map_card,
        aspectRatio = 5f / 2f,
        onClick = onClick
    )
}

@Composable
private fun SpaceLargeEntryCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    background: List<Color>,
    backgroundImageResId: Int? = null,
    aspectRatio: Float? = null,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    val cardModifier = if (aspectRatio != null) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = cardShape
    ) {
        Box(
            modifier = cardModifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = background
                    ),
                    shape = cardShape
                )
        ) {
            if (backgroundImageResId != null) {
                Image(
                    painter = painterResource(backgroundImageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = title,
                    color = accent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    color = YanYeColors.Ink,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Text(
                        text = subtitle,
                        color = YanYeColors.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceReadonlyModule(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.6.dp, YanYeColors.Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                color = YanYeColors.Ink,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = YanYeColors.Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
