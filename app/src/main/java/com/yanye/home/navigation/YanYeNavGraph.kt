package com.yanye.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yanye.home.data.session.InviteCodeResult
import com.yanye.home.data.session.UserSession
import com.yanye.home.ui.care.CareScreen
import com.yanye.home.ui.food.FoodScreen
import com.yanye.home.ui.footprint.FootprintScreen
import com.yanye.home.ui.home.HomeScreen
import com.yanye.home.ui.memo.MemoScreen
import com.yanye.home.ui.memory.MemoryScreen
import com.yanye.home.ui.schedule.ScheduleScreen
import com.yanye.home.ui.settings.ProfileEditorScreen
import com.yanye.home.ui.settings.SettingsScreen
import com.yanye.home.ui.space.SpaceScreen
import com.yanye.home.ui.wish.WishScreen

@Composable
fun YanYeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    session: UserSession,
    onLogout: () -> Unit,
    onCreateInvite: () -> Unit,
    onUpdateProfile: (String, String, String?, String, () -> Unit) -> Unit,
    inviteCode: InviteCodeResult?,
    authMessage: String?,
    isUpdatingProfile: Boolean,
    onClearMessage: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = YanYeDestination.Home.route,
        modifier = modifier
    ) {
        composable(YanYeDestination.Home.route) {
            HomeScreen(
                session = session,
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(YanYeDestination.Space.route) {
            SpaceScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(YanYeDestination.Anniversary.route) {
            ScheduleScreen(initialTab = YanYeDestination.Schedule.TAB_ANNIVERSARY)
        }
        composable(YanYeDestination.Wish.route) {
            WishScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(YanYeDestination.Space.route)
                    }
                },
                onOpenSchedule = {
                    navController.navigate(YanYeDestination.Schedule.route)
                }
            )
        }
        composable(YanYeDestination.Memory.route) {
            MemoryScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(YanYeDestination.Space.route)
                    }
                }
            )
        }
        composable(
            route = YanYeDestination.Memory.ROUTE_WITH_ID,
            arguments = listOf(
                navArgument(YanYeDestination.Memory.MEMORY_ID_ARGUMENT) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val memoryId = backStackEntry.arguments
                ?.getLong(YanYeDestination.Memory.MEMORY_ID_ARGUMENT)
                ?.takeIf { it > 0 }
            MemoryScreen(
                initialMemoryId = memoryId,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(YanYeDestination.Home.route)
                    }
                }
            )
        }
        composable(YanYeDestination.Memo.route) {
            MemoScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(YanYeDestination.Home.route)
                    }
                }
            )
        }
        composable(
            route = YanYeDestination.Schedule.ROUTE_WITH_TAB,
            arguments = listOf(
                navArgument(YanYeDestination.Schedule.TAB_ARGUMENT) {
                    type = NavType.StringType
                    defaultValue = YanYeDestination.Schedule.TAB_SCHEDULE
                }
            )
        ) { backStackEntry ->
            ScheduleScreen(
                initialTab = backStackEntry.arguments
                    ?.getString(YanYeDestination.Schedule.TAB_ARGUMENT)
                    ?: YanYeDestination.Schedule.TAB_SCHEDULE
            )
        }
        composable(YanYeDestination.Footprint.route) {
            FootprintScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(YanYeDestination.Space.route)
                    }
                }
            )
        }
        composable(YanYeDestination.Food.route) {
            FoodScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(YanYeDestination.Home.route)
                    }
                }
            )
        }
        composable(YanYeDestination.Care.route) {
            CareScreen()
        }
        composable(YanYeDestination.Settings.route) {
            SettingsScreen(
                session = session,
                onCreateInvite = onCreateInvite,
                onOpenProfile = { navController.navigate(YanYeDestination.Profile.route) },
                inviteCode = inviteCode
            )
        }
        composable(YanYeDestination.Profile.route) {
            ProfileEditorScreen(
                session = session,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(YanYeDestination.Settings.route)
                    }
                },
                onLogout = onLogout,
                onSave = { username, password, avatarUrl, spaceName ->
                    onUpdateProfile(username, password, avatarUrl, spaceName) {}
                },
                message = authMessage,
                isSaving = isUpdatingProfile,
                onClearMessage = onClearMessage
            )
        }
    }
}
