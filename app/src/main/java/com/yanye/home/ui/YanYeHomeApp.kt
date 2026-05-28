package com.yanye.home.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yanye.home.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yanye.home.data.session.InviteCodeResult
import com.yanye.home.data.session.UserSession
import com.yanye.home.navigation.YanYeDestination
import com.yanye.home.navigation.YanYeNavGraph
import com.yanye.home.navigation.bottomBarDestinations
import com.yanye.home.navigation.topLevelDestinationForRoute
import com.yanye.home.ui.auth.AuthEntryScreen
import com.yanye.home.ui.auth.SpaceSetupScreen
import com.yanye.home.ui.auth.AuthViewModel
import com.yanye.home.ui.common.WallpaperBackground
import com.yanye.home.ui.theme.YanYeColors
import com.yanye.home.ui.theme.YanYeHomeTheme

@Composable
fun YanYeHomeApp() {
    YanYeHomeTheme {
        val authViewModel: AuthViewModel = viewModel()
        val session by authViewModel.session.collectAsState()

        val currentSession = session
        if (currentSession == null) {
            AuthEntryScreen(viewModel = authViewModel)
        } else if (currentSession.currentSpaceId.isNullOrBlank()) {
            SpaceSetupScreen(viewModel = authViewModel)
        } else {
            MainAppShell(
                session = currentSession,
                onLogout = authViewModel::logout,
                onCreateInvite = authViewModel::createInviteCode,
                onUpdateProfile = authViewModel::updateProfile,
                inviteCode = authViewModel.inviteCode.collectAsState().value,
                authMessage = authViewModel.authMessage.collectAsState().value,
                isUpdatingProfile = authViewModel.isUpdatingProfile.collectAsState().value,
                onClearMessage = authViewModel::clearMessage
            )
        }
    }
}

@Composable
private fun MainAppShell(
    session: UserSession,
    onLogout: () -> Unit,
    onCreateInvite: () -> Unit,
    onUpdateProfile: (String, String, String?, String, () -> Unit) -> Unit,
    inviteCode: InviteCodeResult?,
    authMessage: String?,
    isUpdatingProfile: Boolean,
    onClearMessage: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val selectedTopLevel = topLevelDestinationForRoute(currentDestination?.route)
    val shellWallpaper = if (selectedTopLevel == YanYeDestination.Home) {
        R.drawable.home_wallpaper
    } else {
        R.drawable.simple_wallpaper
    }

    WallpaperBackground(imageResId = shellWallpaper) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                YanYeBottomBar(
                    currentDestinationRoute = currentDestination?.route,
                    onDestinationClick = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(YanYeDestination.Home.route) {
                                inclusive = false
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
        ) { innerPadding ->
            YanYeNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                session = session,
                onLogout = onLogout,
                onCreateInvite = onCreateInvite,
                onUpdateProfile = onUpdateProfile,
                inviteCode = inviteCode,
                authMessage = authMessage,
                isUpdatingProfile = isUpdatingProfile,
                onClearMessage = onClearMessage
            )
        }
    }
}

@Composable
private fun YanYeBottomBar(
    currentDestinationRoute: String?,
    onDestinationClick: (YanYeDestination) -> Unit
) {
    val selectedTopLevel = topLevelDestinationForRoute(currentDestinationRoute)
    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.White,
        tonalElevation = 0.dp
    ) {
        bottomBarDestinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedTopLevel == destination,
                onClick = { onDestinationClick(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconResId),
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = YanYeColors.Ink,
                    selectedTextColor = YanYeColors.Ink,
                    indicatorColor = YanYeColors.Soft,
                    unselectedIconColor = YanYeColors.Muted,
                    unselectedTextColor = YanYeColors.Muted
                )
            )
        }
    }
}
