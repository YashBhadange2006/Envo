package com.example.envo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.envo.ui.screens.CarbonFootprintScreen
import com.example.envo.ui.screens.EcoScopeDashboardScreen
import com.example.envo.ui.screens.NasaNewsScreen
import com.example.envo.ui.theme.EnvoTheme
import com.example.envo.ui.theme.rememberDarkModeState
import kotlinx.coroutines.launch

sealed class DrawerScreen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : DrawerScreen("dashboard", "Envo Dashboard", Icons.Default.Home)
    object CarbonFootprint : DrawerScreen("carbon_footprint", "Carbon Footprint Calculator", Icons.Default.Calculate)
    object NasaNews : DrawerScreen("nasa_news", "Latest NASA News", Icons.Default.Article)
}

val drawerScreens = listOf(
    DrawerScreen.Dashboard,
    DrawerScreen.CarbonFootprint,
    DrawerScreen.NasaNews
)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val (isDark, setDarkMode) = rememberDarkModeState()
            val navController = rememberNavController()
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: DrawerScreen.Dashboard.route
            EnvoTheme(darkTheme = isDark) {
                ModalNavigationDrawer(
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(24.dp))
                            drawerScreens.forEach { screen ->
                                NavigationDrawerItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = currentRoute == screen.route,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    },
                    drawerState = drawerState
                ) {
                    Scaffold(
                        topBar = {
                            SmallTopAppBar(
                                title = {
                                    Text(drawerScreens.find { it.route == currentRoute }?.title ?: "Envo Dashboard")
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Open navigation drawer")
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = DrawerScreen.Dashboard.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(DrawerScreen.Dashboard.route) { EcoScopeDashboardScreen() }
                            composable(DrawerScreen.CarbonFootprint.route) { CarbonFootprintScreen() }
                            composable(DrawerScreen.NasaNews.route) { NasaNewsScreen() }
                        }
                    }
                }
            }
        }
    }
}