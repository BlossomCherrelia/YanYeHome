package com.yanye.home.navigation

import androidx.annotation.DrawableRes
import com.yanye.home.R

sealed class YanYeDestination(
    val route: String,
    val label: String,
    @param:DrawableRes val iconResId: Int
) {
    data object Home : YanYeDestination("home", "今天", R.drawable.ic_home)
    data object Space : YanYeDestination("space", "空间", R.drawable.ic_wish)
    data object Anniversary : YanYeDestination("anniversary", "纪念日", R.drawable.ic_anniversary)
    data object Wish : YanYeDestination("wish", "愿望", R.drawable.ic_wish)
    data object Memory : YanYeDestination("memory", "回忆", R.drawable.ic_wish) {
        const val MEMORY_ID_ARGUMENT = "memoryId"
        const val ROUTE_WITH_ID = "memory?memoryId={memoryId}"

        fun withMemory(memoryId: Long): String = "memory?memoryId=$memoryId"
    }
    data object Memo : YanYeDestination("memo", "备忘录", R.drawable.ic_care)
    data object Schedule : YanYeDestination("schedule", "日历", R.drawable.ic_schedule) {
        const val TAB_ARGUMENT = "tab"
        const val TAB_SCHEDULE = "schedule"
        const val TAB_ANNIVERSARY = "anniversary"
        const val ROUTE_WITH_TAB = "schedule?tab={tab}"

        fun withTab(tab: String): String = "schedule?tab=$tab"
    }
    data object Footprint : YanYeDestination("footprint", "地图", R.drawable.ic_footprint)
    data object Food : YanYeDestination("food", "吃什么", R.drawable.ic_food)
    data object Care : YanYeDestination("care", "关怀", R.drawable.ic_care)
    data object Settings : YanYeDestination("settings", "我的", R.drawable.ic_settings)
    data object Profile : YanYeDestination("profile", "个人信息", R.drawable.ic_settings)
}

val bottomBarDestinations = listOf(
    YanYeDestination.Home,
    YanYeDestination.Schedule,
    YanYeDestination.Space,
    YanYeDestination.Care,
    YanYeDestination.Settings
)

fun topLevelDestinationForRoute(route: String?): YanYeDestination =
    when {
        route == YanYeDestination.Schedule.route ||
            route == YanYeDestination.Schedule.ROUTE_WITH_TAB ||
            route == YanYeDestination.Anniversary.route -> YanYeDestination.Schedule
        route == YanYeDestination.Space.route ||
        route == YanYeDestination.Wish.route ||
            route == YanYeDestination.Memory.route ||
            route == YanYeDestination.Memory.ROUTE_WITH_ID ||
            route == YanYeDestination.Footprint.route -> YanYeDestination.Space
        route == YanYeDestination.Care.route ||
            route == YanYeDestination.Memo.route -> YanYeDestination.Care
        route == YanYeDestination.Settings.route ||
            route == YanYeDestination.Profile.route -> YanYeDestination.Settings
        else -> YanYeDestination.Home
    }
