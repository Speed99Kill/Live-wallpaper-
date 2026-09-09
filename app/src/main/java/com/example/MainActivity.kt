package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.WallpaperConfig
import com.example.ui.WallpaperViewModel
import com.example.ui.screens.CustomizerScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.MyCollectionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.collectLatest

enum class AppDestination(val label: String) {
    DISCOVER("Discover"),
    STUDIO("Live Studio"),
    COLLECTION("Collection"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private val wallpaperViewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by wallpaperViewModel.appTheme.collectAsState(initial = "Obsidian")
            MyApplicationTheme(appTheme = appTheme) {
                MainAppContent(viewModel = wallpaperViewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: WallpaperViewModel = viewModel()) {
    var currentDestination by remember { mutableStateOf(AppDestination.DISCOVER) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.notifications.collectLatest { notification ->
            snackbarHostState.showSnackbar(notification.message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Hide bottom bar when inside the full interactive Studio screen
            if (currentDestination != AppDestination.STUDIO) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentDestination == AppDestination.DISCOVER,
                        onClick = { currentDestination = AppDestination.DISCOVER },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.DISCOVER) Icons.Filled.Explore else Icons.Outlined.Explore,
                                contentDescription = "Discover"
                            )
                        },
                        label = { Text("Discover") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_discover")
                    )

                    NavigationBarItem(
                        selected = currentDestination == AppDestination.STUDIO,
                        onClick = { currentDestination = AppDestination.STUDIO },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.STUDIO) Icons.Filled.Tune else Icons.Outlined.Tune,
                                contentDescription = "Live Studio"
                            )
                        },
                        label = { Text("Studio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_studio")
                    )

                    NavigationBarItem(
                        selected = currentDestination == AppDestination.COLLECTION,
                        onClick = { currentDestination = AppDestination.COLLECTION },
                        icon = {
                            Icon(
                                imageVector = if (currentDestination == AppDestination.COLLECTION) Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial,
                                contentDescription = "My Collection"
                            )
                        },
                        label = { Text("Collection") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_collection")
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.padding(if (currentDestination == AppDestination.STUDIO) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) { destination ->
            when (destination) {
                AppDestination.DISCOVER -> {
                    DiscoverScreen(
                        viewModel = viewModel,
                        onNavigateToStudio = { config ->
                            viewModel.selectWallpaper(config)
                            currentDestination = AppDestination.STUDIO
                        },
                        onNavigateToSettings = { currentDestination = AppDestination.SETTINGS }
                    )
                }
                AppDestination.STUDIO -> {
                    CustomizerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentDestination = AppDestination.DISCOVER }
                    )
                }
                AppDestination.COLLECTION -> {
                    MyCollectionScreen(
                        viewModel = viewModel,
                        onNavigateToStudio = { config ->
                            viewModel.selectWallpaper(config)
                            currentDestination = AppDestination.STUDIO
                        },
                        onExploreCatalog = { currentDestination = AppDestination.DISCOVER }
                    )
                }
                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentDestination = AppDestination.DISCOVER }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
