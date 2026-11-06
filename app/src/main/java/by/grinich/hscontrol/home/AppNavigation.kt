package by.grinich.hscontrol

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import by.grinich.hscontrol.auth.AuthState
import by.grinich.hscontrol.auth.AuthViewModel
import by.grinich.hscontrol.auth.LoginScreen
import by.grinich.hscontrol.auth.RegisterScreen
import by.grinich.hscontrol.auth.UserProfile
import by.grinich.hscontrol.home.MainScreen
import by.grinich.hscontrol.home.MonthlyReportScreen
import by.grinich.hscontrol.home.SalesHistoryScreen
import by.grinich.hscontrol.home.TeamScreen
import by.grinich.hscontrol.utils.MonthUtils

@Composable
fun AppNavigation(
    darkMode: Boolean?,
    onDarkModeChange: (Boolean?) -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val state by authViewModel.state.collectAsState()
    val navController = rememberNavController()

    var viewAsProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(state) {
        if (state !is AuthState.Authenticated) viewAsProfile = null
    }

    // Загружаем feature flags при входе пользователя
    LaunchedEffect(state) {
        if (state is AuthState.Authenticated) {
            by.grinich.hscontrol.data.FeatureFlagsState.load()
        }
    }

    if (state is AuthState.Loading) {
        SplashScreen()
        return
    }

    LaunchedEffect(state) {
        val currentRoute = navController
            .currentBackStackEntry?.destination?.route

        when (state) {
            is AuthState.Authenticated -> {
                if (currentRoute != "home") {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                // Не пересоздаём login, если уже на login/register
                if (currentRoute != "login" && currentRoute != "register") {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (state is AuthState.Authenticated) "home" else "login"
    ) {
        composable("login") {
            LoginScreen(
                state = state,
                onSignIn = { email, password -> authViewModel.signIn(email, password) },
                onGoToRegister = { navController.navigate("register") },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable("register") {
            RegisterScreen(
                state = state,
                onSignUp = { email, password, name -> authViewModel.signUp(email, password, name) },
                onBack = { navController.popBackStack() },
                onClearError = { authViewModel.clearError() }
            )
        }

        composable("home") {
            val current = state
            if (current is AuthState.Authenticated) {
                val realProfile = current.profile
                val viewedProfile = viewAsProfile
                val isSupervisor = realProfile.role == "supervisor"

                if (isSupervisor && viewedProfile == null) {
                    TeamScreen(
                        currentProfile = realProfile,
                        onSelectPromoter = { p -> viewAsProfile = p },
                        onSignOut = { authViewModel.signOut() }
                    )
                } else {
                    MainScreen(
                        profile = viewedProfile ?: realProfile,
                        isReadOnly = viewedProfile != null,
                        onBackFromView = if (viewedProfile != null) {
                            { viewAsProfile = null }
                        } else null,
                        onOpenTeam = if (viewedProfile == null && realProfile.role == "developer") {
                            { navController.navigate("team") }
                        } else null,
                        onOpenHistory = { navController.navigate("history") },
                        onOpenReport = { navController.navigate("report") },
                        onSignOut = { authViewModel.signOut() },
                        darkMode = darkMode,
                        onDarkModeChange = onDarkModeChange
                    )
                }
            }
        }

        composable("team") {
            val current = state
            if (current is AuthState.Authenticated) {
                TeamScreen(
                    currentProfile = current.profile,
                    onSelectPromoter = { p ->
                        viewAsProfile = p
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("history") {
            val current = state
            if (current is AuthState.Authenticated) {
                val target = viewAsProfile ?: current.profile
                SalesHistoryScreen(
                    userId = target.id,
                    month = MonthUtils.currentMonth(),
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("report") {
            val current = state
            if (current is AuthState.Authenticated) {
                val target = viewAsProfile ?: current.profile
                MonthlyReportScreen(
                    userId = target.id,
                    month = MonthUtils.currentMonth(),
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "HS-Control",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator()
            }
        }
    }
}