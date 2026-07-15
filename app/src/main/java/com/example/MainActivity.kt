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

enum class Screen {
    Dashboard, Jamaah, Riwayat, Sync, Admin
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val viewModel: AppViewModel = viewModel()
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
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
                        label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
