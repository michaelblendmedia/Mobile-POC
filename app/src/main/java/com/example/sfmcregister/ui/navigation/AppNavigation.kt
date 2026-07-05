package com.example.sfmcregister.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sfmcregister.ui.register.RegisterScreen
import com.example.sfmcregister.ui.success.SuccessScreen

object Routes {
    const val REGISTER = "register"
    const val SUCCESS = "success/{key}"
    fun success(key: String) = "success/$key"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.REGISTER) {

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { key ->
                    nav.navigate(Routes.success(key))
                }
            )
        }

        composable(
            route = Routes.SUCCESS,
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) { entry ->
            val key = entry.arguments?.getString("key").orEmpty()
            SuccessScreen(
                contactKey = key,
                onDone = {
                    nav.popBackStack(Routes.REGISTER, inclusive = false)
                }
            )
        }
    }
}
