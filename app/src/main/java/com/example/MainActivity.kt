package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MusicViewModel
import com.example.ui.NavTab
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.AppBackdrop
import com.example.ui.components.DirectVideoDialog
import com.example.ui.components.FullScreenPlayer
import com.example.ui.components.GlassThickness
import com.example.ui.components.MiniPlayer
import com.example.ui.components.ShrunkYouTubeView
import com.example.ui.components.frostedGlass
import com.example.ui.components.rememberGlassState
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MusicApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val youtubeApiKey by viewModel.youtubeApiKey.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    // One shared blur state for the whole app. AppBackdrop (below) is the single
    // thing every glass panel - top bar, bottom nav, search bar, song cards,
    // settings rows - blurs through. This is what makes "frosted glass" an
    // app-wide look instead of a one-screen effect.
    val hazeState = rememberGlassState()

    Box(modifier = modifier.fillMaxSize()) {
        AppBackdrop(state = hazeState)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .frostedGlass(state = hazeState, shape = RoundedCornerShape(24.dp), thickness = GlassThickness.THIN)
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "StreamMusic",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Live Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "LIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        },
                        actions = {
                            // API Key Config Button
                            IconButton(
                                onClick = { viewModel.setApiKeyDialogOpen(true) },
                                modifier = Modifier.testTag("top_bar_key_btn")
                            ) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "YouTube API Key",
                                        tint = if (youtubeApiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (youtubeApiKey.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                            }

                            // Theme toggle button
                            IconButton(
                                onClick = { viewModel.setDarkMode(!isDarkMode) },
                                modifier = Modifier.testTag("top_bar_theme_btn")
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                }
            },
            bottomBar = {
                Column {
                    // Persistent Mini Player above the navigation bar
                    AnimatedVisibility(
                        visible = playerState.currentSong != null,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        MiniPlayer(
                            playerState = playerState,
                            onPlayerClick = { viewModel.setFullScreenPlayerOpen(true) },
                            onTogglePlayPause = { viewModel.audioPlayer.togglePlayPause() },
                            onSkipNext = { viewModel.playNext() },
                            hazeState = hazeState
                        )
                    }

                    // Floating frosted-glass nav pill - blurs the same AppBackdrop as
                    // everything else, instead of just a flat translucent card.
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .frostedGlass(state = hazeState, shape = RoundedCornerShape(28.dp), thickness = GlassThickness.REGULAR)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            windowInsets = NavigationBarDefaults.windowInsets
                        ) {
                            NavigationBarItem(
                                selected = uiState.activeTab == NavTab.SEARCH,
                                onClick = { viewModel.setNavTab(NavTab.SEARCH) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                label = { Text("Live Search") },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                modifier = Modifier.testTag("nav_search")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == NavTab.FAVORITES,
                                onClick = { viewModel.setNavTab(NavTab.FAVORITES) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == NavTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorites"
                                    )
                                },
                                label = { Text("Favorites") },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                modifier = Modifier.testTag("nav_favorites")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == NavTab.HISTORY,
                                onClick = { viewModel.setNavTab(NavTab.HISTORY) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == NavTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                        contentDescription = "History"
                                    )
                                },
                                label = { Text("History") },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                modifier = Modifier.testTag("nav_history")
                            )

                            NavigationBarItem(
                                selected = uiState.activeTab == NavTab.SETTINGS,
                                onClick = { viewModel.setNavTab(NavTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == NavTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Primary Screens - each gets hazeState so its own cards/rows can be
                // glass too, all sharing the one AppBackdrop behind the whole app.
                when (uiState.activeTab) {
                    NavTab.SEARCH -> SearchScreen(
                        uiState = uiState,
                        playerState = playerState,
                        youtubeApiKey = youtubeApiKey,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onClearQuery = { viewModel.clearSearch() },
                        onTriggerSearch = { viewModel.triggerSearch(it) },
                        onSongClick = { viewModel.playSong(it) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        onOpenApiKeyDialog = { viewModel.setApiKeyDialogOpen(true) },
                        hazeState = hazeState
                    )

                    NavTab.FAVORITES -> FavoritesScreen(
                        favorites = favorites,
                        playerState = playerState,
                        onSongClick = { viewModel.playSong(it) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        onPlayAll = {
                            if (favorites.isNotEmpty()) {
                                viewModel.playSong(favorites.first())
                            }
                        },
                        hazeState = hazeState
                    )

                    NavTab.HISTORY -> HistoryScreen(
                        history = history,
                        playerState = playerState,
                        onSongClick = { viewModel.playSong(it) },
                        onFavoriteClick = { viewModel.toggleFavorite(it) },
                        hazeState = hazeState
                    )

                    NavTab.SETTINGS -> SettingsScreen(
                        isDarkMode = isDarkMode,
                        youtubeApiKey = youtubeApiKey,
                        onToggleDarkMode = { viewModel.setDarkMode(it) },
                        onOpenApiKeyDialog = { viewModel.setApiKeyDialogOpen(true) },
                        onOpenDirectVideoDialog = { viewModel.setDirectVideoDialogOpen(true) },
                        hazeState = hazeState
                    )
                }

                // Completely inert background YouTube iFrame Player engine (1dp, non-touchable)
                ShrunkYouTubeView(audioPlayer = viewModel.audioPlayer)

                // Full-Screen Now-Playing Player
                FullScreenPlayer(
                    isOpen = uiState.isFullScreenPlayerOpen,
                    playerState = playerState,
                    isShuffleEnabled = uiState.isShuffleEnabled,
                    isRepeatEnabled = uiState.isRepeatEnabled,
                    onClose = { viewModel.setFullScreenPlayerOpen(false) },
                    onTogglePlayPause = { viewModel.audioPlayer.togglePlayPause() },
                    onSeek = { seconds -> viewModel.audioPlayer.seekTo(seconds) },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onToggleFavorite = {
                        playerState.currentSong?.let { viewModel.toggleFavorite(it) }
                    },
                    onOpenApiKeyDialog = { viewModel.setApiKeyDialogOpen(true) }
                )

                // YouTube API Key Config Dialog
                ApiKeyDialog(
                    isOpen = uiState.isApiKeyDialogOpen,
                    currentKey = youtubeApiKey,
                    validationState = uiState.apiKeyValidationState,
                    onDismiss = { viewModel.setApiKeyDialogOpen(false) },
                    onValidateAndSave = { newKey -> viewModel.validateAndSaveApiKey(newKey) },
                    onForceSave = { newKey -> viewModel.saveYouTubeApiKey(newKey) }
                )

                // Direct YouTube Video Stream Dialog
                DirectVideoDialog(
                    isOpen = uiState.isDirectVideoDialogOpen,
                    onDismiss = { viewModel.setDirectVideoDialogOpen(false) },
                    onPlayDirect = { videoIdOrUrl, title ->
                        viewModel.playDirectYouTubeVideo(videoIdOrUrl, title)
                    }
                    // playDirectYouTubeVideo now returns Boolean: true = played, false = invalid link
                )
            }
        }
    }
}
