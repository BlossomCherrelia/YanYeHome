package com.yanye.home.ui.home

import androidx.compose.runtime.Composable
import com.yanye.home.ui.common.FeaturePlaceholderScreen
import com.yanye.home.ui.common.PlaceholderSection
import com.yanye.home.ui.theme.YanYeColors

@Composable
fun HomeScreen() {
    FeaturePlaceholderScreen(
        title = "今日面板",
        subtitle = "聚合最近纪念日、今日日程、待完成备忘、状态提醒和照顾提示。",
        accentColor = YanYeColors.Rose,
        sections = listOf(
            PlaceholderSection(
                title = "今日卡片",
                description = "后续接入纪念日、日程、愿望、挑战和冷静状态。"
            ),
            PlaceholderSection(
                title = "快捷行动",
                description = "预留吃什么、新增日程、记录回忆、冷静模式和新备忘入口。"
            ),
            PlaceholderSection(
                title = "轻量状态",
                description = "为开心、累、想贴贴、需要独处、在忙等状态灯保留展示位置。"
            )
        )
    )
}
