package io.github.martinschneider.baiyue.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.martinschneider.baiyue.data.model.HikeDescription
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.platform.MapScreenContent
import io.github.martinschneider.baiyue.ui.navigation.Screen
import io.github.martinschneider.baiyue.ui.screen.about.AboutScreen
import io.github.martinschneider.baiyue.ui.screen.detail.ElevationChart
import io.github.martinschneider.baiyue.ui.screen.detail.MountainInfoDialog
import io.github.martinschneider.baiyue.ui.screen.list.ListScreen
import io.github.martinschneider.baiyue.ui.screen.list.ListViewModel
import io.github.martinschneider.baiyue.ui.screen.map.MapViewModel
import io.github.martinschneider.baiyue.ui.screen.settings.SettingsScreen
import io.github.martinschneider.baiyue.ui.screen.settings.SettingsViewModel
import io.github.martinschneider.baiyue.ui.screen.stats.StatsScreen
import io.github.martinschneider.baiyue.ui.theme.BaiyueTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File

@Composable
actual fun BaiyueApp() {
    BaiyueTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        val listViewModel: ListViewModel = koinInject()
        val settingsViewModel: SettingsViewModel = koinInject()
        val mapViewModel: MapViewModel = koinInject()

        // Initialize data on first composition
        LaunchedEffect(Unit) {
            listViewModel.init()
        }

        val climbed by mapViewModel.climbedPeaks.collectAsState()
        val pendingNavigateToMap by mapViewModel.pendingNavigateToMap.collectAsState()
        var infoDialogMountain by remember { mutableStateOf<Mountain?>(null) }
        var trackDialogHike by remember { mutableStateOf<HikeDescription?>(null) }

        LaunchedEffect(pendingNavigateToMap) {
            if (pendingNavigateToMap) {
                mapViewModel.clearNavigateToMap()
                navController.navigate(Screen.Map.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LocationOn, "Map") },
                        label = { Text("Map") },
                        selected = currentRoute == Screen.Map.route,
                        onClick = {
                            navController.navigate(Screen.Map.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, "List") },
                        label = { Text("List") },
                        selected = currentRoute == Screen.List.route,
                        onClick = {
                            navController.navigate(Screen.List.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BarChart, "Stats") },
                        label = { Text("Stats") },
                        selected = currentRoute == Screen.Stats.route,
                        onClick = {
                            navController.navigate(Screen.Stats.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Map.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Map.route) {
                    MapScreenContent(
                        onMarkerClick = { osmId ->
                            val mountain = mapViewModel.getMountainByOsmId(osmId)
                            if (mountain != null) {
                                infoDialogMountain = mountain
                            }
                        },
                        onTrackClick = { hike ->
                            trackDialogHike = hike
                        }
                    )
                }
                composable(Screen.List.route) {
                    ListScreen(
                        onMountainClick = { osmId ->
                            launchDetailActivity(context, osmId)
                        }
                    )
                }
                composable(Screen.Stats.route) {
                    StatsScreen(
                        onMountainClick = { osmId ->
                            launchDetailActivity(context, osmId)
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    val scope = rememberCoroutineScope()

                    val restoreLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            scope.launch {
                                val success = withContext(Dispatchers.IO) {
                                    val inputStream = context.contentResolver.openInputStream(it)
                                    if (inputStream != null) {
                                        inputStream.use { stream ->
                                            settingsViewModel.importFromStream(
                                                stream,
                                                savePhotoFromBase64 = { osmId, base64 -> saveBase64Photo(context, osmId, base64) },
                                                savePhotoBytes = { osmId, bytes -> saveRawPhoto(context, osmId, bytes) }
                                            )
                                        }
                                    } else false
                                }
                                Toast.makeText(
                                    context,
                                    if (success) "Restore successful" else "Restore failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    SettingsScreen(
                        onBackup = {
                            scope.launch {
                                val file = File(context.cacheDir, "baiyue-backup.zip")
                                withContext(Dispatchers.IO) {
                                    file.outputStream().use { out ->
                                        settingsViewModel.exportNativeZip(out) { path ->
                                            readPhotoBytes(path)
                                        }
                                    }
                                }
                                shareFile(context, file, "application/zip")
                            }
                        },
                        onRestore = {
                            restoreLauncher.launch("*/*")
                        },
                        onDeleteAll = {
                            settingsViewModel.deleteAllData {
                                deleteAllPhotos(context)
                            }
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                        },
                        onAbout = {
                            navController.navigate("about")
                        }
                    )
                }
                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        // Mountain info dialog (from marker tap on map)
        infoDialogMountain?.let { mountain ->
            val isClimbed = climbed[mountain.osmId] == true
            MountainInfoDialog(
                mountain = mountain,
                isClimbed = isClimbed,
                onDetails = {
                    infoDialogMountain = null
                    launchDetailActivity(context, mountain.osmId)
                },
                onDismiss = { infoDialogMountain = null }
            )
        }

        // Track info dialog (from polyline tap on map)
        trackDialogHike?.let { hike ->
            val hikeMountains = remember(hike) {
                hike.xiaobaiyueIds.mapNotNull { id ->
                    mapViewModel.getMountainByXiaobaiyueId(id)?.let { id to it }
                }
            }
            AlertDialog(
                onDismissRequest = { trackDialogHike = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            hikeMountains.forEach { (id, mountain) ->
                                Text("#$id ${mountain.chinese} ${mountain.english}")
                            }
                            if (hikeMountains.size < hike.xiaobaiyueIds.size) {
                                hike.xiaobaiyueIds
                                    .filter { id -> hikeMountains.none { it.first == id } }
                                    .forEach { id -> Text("#$id") }
                            }
                        }
                        IconButton(
                            onClick = { trackDialogHike = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                text = {
                    Column {
                        hike.stats?.let { stats ->
                            Text(stats)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (hike.track.isNotEmpty() && hike.track.any { it.size >= 3 }) {
                            ElevationChart(
                                track = hike.track,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            hike.gpxFilename?.let { filename ->
                                OutlinedButton(onClick = {
                                    openGpxFile(context, filename)
                                    trackDialogHike = null
                                }) { Text("Open GPX") }
                            }
                            if (hikeMountains.size == 1) {
                                Button(onClick = {
                                    trackDialogHike = null
                                    launchDetailActivity(context, hikeMountains[0].second.osmId)
                                }) { Text("Details") }
                            }
                        }
                        if (hikeMountains.size > 1) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Details",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            hikeMountains.forEach { (id, mountain) ->
                                TextButton(
                                    onClick = {
                                        trackDialogHike = null
                                        launchDetailActivity(context, mountain.osmId)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "#$id ${mountain.chinese}",
                                        modifier = Modifier.fillMaxWidth(),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

private fun readPhotoBytes(path: String): ByteArray? {
    return try {
        val file = File(path)
        if (file.exists()) file.readBytes() else null
    } catch (e: Exception) {
        null
    }
}

private fun saveBase64Photo(context: Context, osmId: Long, base64: String): String? {
    return try {
        val photosDir = File(context.filesDir, "photos")
        photosDir.mkdirs()
        val photoFile = File(photosDir, "photo_$osmId.jpg")
        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        photoFile.writeBytes(bytes)
        photoFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun saveRawPhoto(context: Context, osmId: Long, bytes: ByteArray): String? {
    return try {
        val photosDir = File(context.filesDir, "photos")
        photosDir.mkdirs()
        val photoFile = File(photosDir, "photo_$osmId.jpg")
        photoFile.writeBytes(bytes)
        photoFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun deleteAllPhotos(context: Context) {
    val photosDir = File(context.filesDir, "photos")
    if (photosDir.exists()) {
        photosDir.listFiles()?.forEach { it.delete() }
    }
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share backup"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
    }
}
