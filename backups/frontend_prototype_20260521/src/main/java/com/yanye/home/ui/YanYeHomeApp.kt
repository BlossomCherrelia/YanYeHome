package com.yanye.home.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yanye.home.navigation.YanYeDestination
import com.yanye.home.navigation.YanYeNavGraph
import com.yanye.home.navigation.bottomBarDestinations
import com.yanye.home.ui.theme.YanYeHomeTheme

@Composable
fun YanYeHomeApp() {
    YanYeHomeTheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        Scaffold(
            bottomBar = {
                YanYeBottomBar(
                    currentDestinationRoute = currentDestination?.route,
                    isSelected = { destination ->
                        currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    },
                    onDestinationClick = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        ) { innerPadding ->
            YanYeNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun YanYeBottomBar(
    currentDestinationRoute: String?,
    isSelected: (YanYeDestination) -> Boolean,
    onDestinationClick: (YanYeDestination) -> Unit
) {
    NavigationBar {
        bottomBarDestinations.forEach { destination ->
            NavigationBarItem(
                selected = isSelected(destination) ||
                    currentDestinationRoute == null && destination == YanYeDestination.Home,
                onClick = { onDestinationClick(destination) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconResId),
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}
