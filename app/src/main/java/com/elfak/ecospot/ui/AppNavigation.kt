package com.elfak.ecospot.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.elfak.ecospot.ui.auth.AuthState
import com.elfak.ecospot.ui.auth.AuthViewModel
import com.elfak.ecospot.ui.auth.LoginScreen
import com.elfak.ecospot.ui.auth.RegistrationScreen
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth

object Routes {
    const val AUTH_GRAPH = "auth_graph"
    const val MAP_GRAPH = "map_graph"

    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val ADD_SPOT = "add_spot"
    const val SPOT_DETAIL = "spot_detail"

    const val ADD_SPOT_ROUTE = "$ADD_SPOT/{lat}/{lng}"
    const val SPOT_DETAIL_ROUTE = "$SPOT_DETAIL/{spotId}"

    const val RANKING = "ranking"

    const val SPOT_LIST = "spot_list"
}

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    val authState by authViewModel.authState.collectAsState()
    val errorState by authViewModel.errorState.collectAsState()

    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.MAP_GRAPH
    } else {
        Routes.AUTH_GRAPH
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // Graf za Autentifikaciju (Login, Register)
        navigation(startDestination = Routes.LOGIN, route = Routes.AUTH_GRAPH) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    authState = authState,
                    errorState = errorState,
                    onLoginClick = { email, password -> authViewModel.loginUser(email, password) },
                    onValidateEmail = { email -> authViewModel.isEmailValid(email) },
                    onValidatePassword = { password -> authViewModel.isPasswordValid(password) },
                    onNavigateToRegister = {
                        navController.navigate(Routes.REGISTER)
                        authViewModel.clearErrors()
                    },
                    onResetAuthState = { authViewModel.resetAuthState() }
                )
            }
            composable(Routes.REGISTER) {
                RegistrationScreen(
                    authState = authState,
                    errorState = errorState,
                    onRegisterClick = { email, pass, name, phone, uri -> authViewModel.registerUser(email, pass, name, phone, uri) },
                    onValidateEmail = { email -> authViewModel.isEmailValid(email) },
                    onValidatePassword = { password -> authViewModel.isPasswordValid(password) },
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                        authViewModel.clearErrors()
                    },
                    onResetAuthState = { authViewModel.resetAuthState() }
                )
            }
        }

        // Graf za Mape (Home, AddSpot, SpotDetail,Ranking,SpotList)
        navigation(startDestination = Routes.HOME, route = Routes.MAP_GRAPH) {
            composable(Routes.HOME) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) { navController.getBackStackEntry(Routes.MAP_GRAPH) }
                val mapViewModel: MapViewModel = viewModel(parentEntry)

                HomeScreen(
                    onLogoutClick = { authViewModel.logout() },
                    mapViewModel = mapViewModel,
                    navController = navController
                )
            }

            composable(
                route = Routes.ADD_SPOT_ROUTE,

                arguments = listOf(
                    navArgument("lat") { type = NavType.FloatType },
                    navArgument("lng") { type = NavType.FloatType }
                )
            ) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) { navController.getBackStackEntry(Routes.MAP_GRAPH) }
                val mapViewModel: MapViewModel = viewModel(parentEntry)

                val lat = navBackStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
                val lng = navBackStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
                val location = LatLng(lat, lng)

                AddSpotScreen(
                    location = location,
                    mapViewModel = mapViewModel,
                    navController = navController
                )
            }

            composable(
                route = Routes.SPOT_DETAIL_ROUTE,

                arguments = listOf(
                    navArgument("spotId") { type = NavType.StringType }
                )
            ) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) { navController.getBackStackEntry(Routes.MAP_GRAPH) }
                val mapViewModel: MapViewModel = viewModel(parentEntry)

                val spotId = navBackStackEntry.arguments?.getString("spotId")
                if (spotId != null) {
                    LaunchedEffect(spotId) { mapViewModel.observeSpotDetails(spotId) }
                    DisposableEffect(spotId) { onDispose { mapViewModel.clearSpotDetails() } }

                    SpotDetailScreen(
                        navController = navController,
                        mapViewModel = mapViewModel
                    )
                }
            }
            composable(Routes.RANKING) {
                RankingScreen(navController = navController)
            }
            composable(Routes.SPOT_LIST) { navBackStackEntry ->
                val parentEntry = remember(navBackStackEntry) { navController.getBackStackEntry(Routes.MAP_GRAPH) }
                val mapViewModel: MapViewModel = viewModel(parentEntry)

                SpotListScreen(
                    navController = navController,
                    mapViewModel = mapViewModel
                )
            }
        }
    }


    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate(Routes.MAP_GRAPH) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
                authViewModel.resetAuthState()
                authViewModel.clearErrors()
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(Routes.AUTH_GRAPH) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
                authViewModel.resetAuthState()
                authViewModel.clearErrors()
            }
            else -> {  }
        }
    }
}