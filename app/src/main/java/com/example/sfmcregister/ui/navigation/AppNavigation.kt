package com.example.sfmcregister.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.sfmcregister.ui.dashboard.DashboardScreen
import com.example.sfmcregister.ui.dashboard.TransferScreen
import com.example.sfmcregister.ui.dashboard.QrisScreen
import com.example.sfmcregister.ui.dashboard.KartuScreen
import com.example.sfmcregister.ui.login.LoginScreen
import com.example.sfmcregister.ui.register.RegisterScreen
import com.example.sfmcregister.ui.success.SuccessScreen

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val TRANSFER = "transfer"
    const val QRIS = "qris"
    const val KARTU = "kartu"
    const val REGISTER = "register"
    const val GAMIFICATION = "gamification"
    const val SUCCESS = "success/{key}"
    fun success(key: String) = "success/$key"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.LOGIN) {
        
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginClick = { 
                    nav.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    } 
                },
                onRegisterClick = { nav.navigate(Routes.REGISTER) }
            )
        }
        
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onTransferClick = { nav.navigate(Routes.TRANSFER) },
                onQrisClick = { nav.navigate(Routes.QRIS) },
                onKartuClick = { nav.navigate(Routes.KARTU) }
            )
        }

        composable(Routes.TRANSFER) {
            TransferScreen(
                onBackClick = { nav.popBackStack() }
            )
        }

        composable(Routes.QRIS) {
            QrisScreen(
                onBackClick = { nav.popBackStack() }
            )
        }

        composable(Routes.KARTU) {
            KartuScreen(
                onBackClick = { nav.popBackStack() }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { key ->
                    nav.navigate(Routes.success(key)) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onBackClick = { nav.popBackStack() }
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
                    nav.navigate(Routes.DASHBOARD) {
                        popUpTo(0) // clear stack
                    }
                }
            )
        }

        composable(
            route = Routes.GAMIFICATION,
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "ocbcmobile://gamification" })
        ) {
            com.example.sfmcregister.ui.dashboard.GamificationScreen(
                onBackClick = { nav.popBackStack() }
            )
        }
    }
}
