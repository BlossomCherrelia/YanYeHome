package com.yanye.home.ui.settings

import androidx.compose.runtime.Composable
import com.yanye.home.ui.common.FeaturePlaceholderScreen
import com.yanye.home.ui.common.PlaceholderSection
import com.yanye.home.ui.theme.YanYeColors

@Composable
fun SettingsScreen() {
    FeaturePlaceholderScreen(
        title = "我的",
        subtitle = "用于放置本地设置、隐私开关、提醒时间和后续双人空间入口。",
        accentColor = YanYeColors.Rose,
        sections = listOf(
            PlaceholderSection(
                title = "本地偏好",
                description = "后续用 DataStore 保存主题、首次引导、下班提醒时间等轻量设置。"
            ),
            PlaceholderSection(
                title = "隐私边界",
                description = "为私密愿望、姨妈记录、时光信箱和冲突复盘保留统一设置入口。"
            ),
            PlaceholderSection(
                title = "双人版本",
                description = "V0.1 稳定后再加入账号、情侣绑定、共享权限和云同步。"
            )
        )
    )
}
