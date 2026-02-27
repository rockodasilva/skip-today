package com.groupalarm.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.groupalarm.app.ui.edit.EditAlarmScreen
import com.groupalarm.app.ui.home.HomeScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onAddAlarm = { navController.navigate("edit/-1") },
                onEditAlarm = { alarmId -> navController.navigate("edit/$alarmId") }
            )
        }

        composable(
            route = "edit/{alarmId}",
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) {
            EditAlarmScreen(onBack = { navController.popBackStack() })
        }
    }
}
