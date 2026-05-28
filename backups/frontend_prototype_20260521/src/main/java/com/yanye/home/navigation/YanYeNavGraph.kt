package com.yanye.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yanye.home.ui.anniversary.AnniversaryScreen
import com.yanye.home.ui.care.CareScreen
import com.yanye.home.ui.food.FoodScreen
import com.yanye.home.ui.footprint.FootprintScreen
import com.yanye.home.ui.home.HomeScreen
import com.yanye.home.ui.schedule.ScheduleScreen
import com.yanye.home.ui.settings.SettingsScreen
import com.yanye.home.ui.wish.WishScreen

@Composable
fun YanYeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = YanYeDestination.Home.route,
        modifier = modifier
    ) {
        composable(YanYeDestination.Home.route) {
            HomeScreen()
        }
        composable(YanYeDestination.Anniversary.route) {
            AnniversaryScreen()
        }
        composable(YanYeDestination.Wish.route) {
            WishScreen()
        }
        composable(YanYeDestination.Schedule.route) {
            ScheduleScreen()
        }
        composable(YanYeDestination.Footprint.route) {
            FootprintScreen()
        }
        composable(YanYeDestination.Food.route) {
            FoodScreen()
        }
        composable(YanYeDestination.Care.route) {
            CareScreen()
        }
        composable(YanYeDestination.Settings.route) {
            SettingsScreen()
        }
    }
}
