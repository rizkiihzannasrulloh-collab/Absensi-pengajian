package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.foundation.isSystemInDarkTheme

enum class Screen {
    Dashboard, Jamaah, Rekap, Riwayat, Sync, Admin
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val systemIsDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemIsDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: AppViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val items = listOf(
                    NavigationItem(
                        screen = Screen.Dashboard,
                        title = "Dashboard",
                        selectedIcon = Icons.Filled.Dashboard,
                        unselectedIcon = Icons.Outlined.Dashboard
                    ),
                    NavigationItem(
                        screen = Screen.Jamaah,
                        title = "Jamaah",
                        selectedIcon = Icons.Filled.PeopleAlt,
                        unselectedIcon = Icons.Outlined.PeopleAlt
                    ),
                    NavigationItem(
                        screen = Screen.Rekap,
                        title = "Rekap",
                        selectedIcon = Icons.Filled.PieChart,
                        unselectedIcon = Icons.Outlined.PieChart
                    ),
                    NavigationItem(
                        screen = Screen.Riwayat,
                        title = "Riwayat",
                        selectedIcon = Icons.Filled.History,
                        unselectedIcon = Icons.Outlined.History
                    ),
                    NavigationItem(
                        screen = Screen.Sync,
                        title = "Sinkronisasi",
                        selectedIcon = Icons.Filled.Sync,
                        unselectedIcon = Icons.Outlined.Sync
                    ),
                    NavigationItem(
                        screen = Screen.Admin,
                        title = "Admin",
                        selectedIcon = Icons.Filled.Security,
                        unselectedIcon = Icons.Outlined.Security
                    )
                )

                items.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = item.screen },
                        label = { 
                            Text(
                                item.title, 
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            ) 
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        // Render screen content with seamless animations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                Screen.Jamaah -> JamaahScreen(viewModel = viewModel)
                Screen.Rekap -> RekapScreen(viewModel = viewModel)
                Screen.Riwayat -> RiwayatScreen(viewModel = viewModel)
                Screen.Sync -> SyncScreen(viewModel = viewModel)
                Screen.Admin -> AdminScreen(viewModel = viewModel)
            }
        }
    }
}

data class NavigationItem(
    val screen: Screen,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
