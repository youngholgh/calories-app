package ru.chsu.calories.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.chsu.calories.data.Meal
import ru.chsu.calories.ui.screens.AddProductScreen
import ru.chsu.calories.ui.screens.CompetitionScreen
import ru.chsu.calories.ui.screens.CustomProductScreen
import ru.chsu.calories.ui.screens.DiaryScreen
import ru.chsu.calories.ui.screens.LoginScreen
import ru.chsu.calories.ui.screens.MealScreen
import ru.chsu.calories.ui.screens.ProductsScreen
import ru.chsu.calories.ui.screens.ProfileScreen
import ru.chsu.calories.ui.screens.StatsScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("diary", "Дневник", Icons.AutoMirrored.Filled.List),
    Tab("products", "Продукты", Icons.Filled.Search),
    Tab("stats", "Статистика", Icons.Filled.DateRange),
    Tab("competition", "Рейтинг", Icons.Filled.Star),
    Tab("profile", "Профиль", Icons.Filled.Person)
)

private fun NavHostController.goToTab(route: String) {
    navigate(route) {
        popUpTo("diary") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun CaloriesApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    // показываем сообщения из ViewModel внизу экрана
    LaunchedEffect(vm.message) {
        val m = vm.message ?: return@LaunchedEffect
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(m.text)
        if (vm.message == m) vm.message = null
    }

    // пока данные читаются из Room — показываем индикатор загрузки
    if (!vm.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    // если пользователь уже входил раньше, сразу открываем дневник
    val start = remember { if (vm.userEmail != null) "diary" else "login" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.goToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = start, modifier = Modifier.padding(padding)) {

            composable("login") {
                LoginScreen(vm) { registered ->
                    nav.navigate(if (registered) "onboarding" else "diary") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }

            composable("onboarding") {
                ProfileScreen(
                    vm, onboarding = true,
                    onFinish = {
                        vm.show("Норма рассчитана: ${vm.norm.kcal} ккал")
                        nav.navigate("diary") { popUpTo("onboarding") { inclusive = true } }
                    },
                    onLogout = {}
                )
            }

            composable("diary") {
                DiaryScreen(
                    vm,
                    onOpenMeal = { nav.navigate("meal/${it.name}") },
                    onAdd = { nav.navigate("add/${it.name}") }
                )
            }

            composable("meal/{meal}") { entry ->
                val meal = Meal.valueOf(entry.arguments?.getString("meal") ?: Meal.BREAKFAST.name)
                MealScreen(
                    vm, meal,
                    onBack = { nav.popBackStack() },
                    onEdit = { index -> nav.navigate("add/${meal.name}?edit=$index") },
                    onAdd = { nav.navigate("add/${meal.name}") }
                )
            }

            composable(
                route = "add/{meal}?pid={pid}&edit={edit}&from={from}",
                arguments = listOf(
                    navArgument("meal") { type = NavType.StringType },
                    navArgument("pid") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("edit") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("from") { type = NavType.BoolType; defaultValue = false }
                )
            ) { entry ->
                val args = entry.arguments
                val editIndex = args?.getInt("edit") ?: -1
                AddProductScreen(
                    vm = vm,
                    initialMeal = Meal.valueOf(args?.getString("meal") ?: Meal.BREAKFAST.name),
                    presetProductId = args?.getString("pid"),
                    editIndex = editIndex,
                    fromProducts = args?.getBoolean("from") ?: false,
                    onBack = { nav.popBackStack() },
                    onDone = {
                        if (editIndex >= 0) nav.popBackStack()
                        else nav.popBackStack("diary", inclusive = false)
                    },
                    onCustom = { nav.navigate("custom") }
                )
            }

            composable("custom") {
                CustomProductScreen(vm, onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
            }

            composable("products") {
                ProductsScreen(
                    vm,
                    onPick = { id ->
                        vm.openDate(vm.today)
                        nav.navigate("add/${Meal.LUNCH.name}?pid=$id&from=true")
                    },
                    onCustom = { nav.navigate("custom") }
                )
            }

            composable("stats") {
                StatsScreen(vm) { date ->
                    vm.openDate(date)
                    nav.goToTab("diary")
                }
            }

            composable("competition") {
                CompetitionScreen(vm)
            }

            composable("profile") {
                ProfileScreen(
                    vm, onboarding = false,
                    onFinish = {},
                    onLogout = {
                        vm.logout()
                        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
                    }
                )
            }
        }
    }
}
