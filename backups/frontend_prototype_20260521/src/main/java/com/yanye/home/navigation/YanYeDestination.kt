package com.yanye.home.navigation

import androidx.annotation.DrawableRes
import com.yanye.home.R

sealed class YanYeDestination(
    val route: String,
    val label: String,
    @param:DrawableRes val iconResId: Int
) {
    data object Home : YanYeDestination("home", "首页", R.drawable.ic_home)
    data object Anniversary : YanYeDestination("anniversary", "纪念日", R.drawable.ic_anniversary)
    data object Wish : YanYeDestination("wish", "愿望", R.drawable.ic_wish)
    data object Schedule : YanYeDestination("schedule", "日程", R.drawable.ic_schedule)
    data object Footprint : YanYeDestination("footprint", "地图", R.drawable.ic_footprint)
    data object Food : YanYeDestination("food", "吃什么", R.drawable.ic_food)
    data object Care : YanYeDestination("care", "关怀", R.drawable.ic_care)
    data object Settings : YanYeDestination("settings", "我的", R.drawable.ic_settings)
}

val bottomBarDestinations = listOf(
    YanYeDestination.Home,
    YanYeDestination.Anniversary,
    YanYeDestination.Wish,
    YanYeDestination.Schedule,
    YanYeDestination.Footprint,
    YanYeDestination.Food,
    YanYeDestination.Care,
    YanYeDestination.Settings
)
