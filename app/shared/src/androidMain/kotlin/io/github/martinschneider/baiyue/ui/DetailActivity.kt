package io.github.martinschneider.baiyue.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.martinschneider.baiyue.data.model.LatLng
import io.github.martinschneider.baiyue.platform.PeakMapPreview
import io.github.martinschneider.baiyue.platform.TrackMapPreview
import io.github.martinschneider.baiyue.ui.screen.detail.DetailContent
import io.github.martinschneider.baiyue.ui.screen.detail.DetailViewModel
import io.github.martinschneider.baiyue.ui.screen.map.MapViewModel
import io.github.martinschneider.baiyue.ui.theme.BaiyueTheme
import org.koin.compose.koinInject
import java.io.File

class DetailActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OSM_ID = "osm_id"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val osmId = intent.getLongExtra(EXTRA_OSM_ID, -1L)
        if (osmId == -1L) {
            finish()
            return
        }

        setContent {
            BaiyueTheme {
                val detailViewModel: DetailViewModel = koinInject()
                val mapViewModel: MapViewModel = koinInject()
                val context = LocalContext.current

                LaunchedEffect(osmId) {
                    detailViewModel.selectMountain(osmId)
                }

                val selectedMountain by detailViewModel.selectedMountain.collectAsState()
                val climbed by detailViewModel.climbedPeaks.collectAsState()
                val photos by detailViewModel.photoPeaks.collectAsState()
                val allClimbDates by detailViewModel.climbDates.collectAsState()

                val photoPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { handlePhotoSelected(context, it, selectedMountain, detailViewModel) }
                }

                val photoViewerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val mountain = selectedMountain ?: return@rememberLauncherForActivityResult
                    when (result.resultCode) {
                        PhotoActivity.RESULT_UPDATE -> {
                            photoPickerLauncher.launch("image/*")
                        }
                        PhotoActivity.RESULT_DELETE -> {
                            val photoFile = File(context.filesDir, "photos/photo_${mountain.osmId}.jpg")
                            if (photoFile.exists()) photoFile.delete()
                            detailViewModel.deletePhoto(mountain.osmId)
                        }
                    }
                }

                val mountain = selectedMountain
                val previousMountain = remember(mountain) {
                    mountain?.let { detailViewModel.getPreviousMountain(it) }
                }
                val nextMountain = remember(mountain) {
                    mountain?.let { detailViewModel.getNextMountain(it) }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                if (mountain != null) {
                                    val idStr = mountain.displayId?.let { "#$it " } ?: ""
                                    Text("$idStr${mountain.chinese}")
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        previousMountain?.let { m ->
                                            detailViewModel.selectMountain(m.osmId)
                                        }
                                    },
                                    enabled = previousMountain != null
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                                }
                                IconButton(
                                    onClick = {
                                        nextMountain?.let { m ->
                                            detailViewModel.selectMountain(m.osmId)
                                        }
                                    },
                                    enabled = nextMountain != null
                                ) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.LocationOn, "Map") },
                                label = { Text("Map") },
                                selected = false,
                                onClick = { finish() }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, "List") },
                                label = { Text("List") },
                                selected = false,
                                onClick = { finish() }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.BarChart, "Stats") },
                                label = { Text("Stats") },
                                selected = false,
                                onClick = { finish() }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, "Settings") },
                                label = { Text("Settings") },
                                selected = false,
                                onClick = { finish() }
                            )
                        }
                    }
                ) { paddingValues ->
                    if (mountain != null) {
                        val isClimbed = climbed[mountain.osmId] == true
                        val hasPhoto = mountain.osmId in photos
                        val hikeDescription = remember(mountain) { detailViewModel.getHikeDescription(mountain) }

                        DetailContent(
                            mountain = mountain,
                            isClimbed = isClimbed,
                            hasPhoto = hasPhoto,
                            onToggleClimbed = { detailViewModel.toggleClimbed(mountain.osmId) },
                            onNavigateToMaps = { url -> openUrl(context, url) },
                            onNavigateToOrganicMaps = { url -> openUrl(context, url) },
                            onCopyToClipboard = { text -> copyToClipboard(context, text) },
                            onOpenUrl = { url -> openUrl(context, url) },
                            hikeDescription = hikeDescription,
                            climbDates = allClimbDates[mountain.osmId] ?: emptyList(),
                            onAddClimbDate = { date -> detailViewModel.addClimbDate(mountain.osmId, date) },
                            onDeleteClimbDate = { id -> detailViewModel.deleteClimbDate(id) },
                            onDownloadGpx = { gpxFilename -> openGpxFile(context, gpxFilename) },
                            onShowPeakOnMap = {
                                mapViewModel.setPendingZoomTarget(LatLng(mountain.lat, mountain.lng))
                                mapViewModel.requestNavigateToMap()
                                navigateToMainActivity(context)
                            },
                            peakMapContent = {
                                PeakMapPreview(
                                    lat = mountain.lat,
                                    lng = mountain.lng,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            onShowTrackOnMap = { track ->
                                val minLat = track.minOf { it[0] }
                                val maxLat = track.maxOf { it[0] }
                                val minLng = track.minOf { it[1] }
                                val maxLng = track.maxOf { it[1] }
                                mapViewModel.enableTracks()
                                mapViewModel.setPendingBoundsZoom(
                                    LatLng(minLat, minLng),
                                    LatLng(maxLat, maxLng)
                                )
                                mapViewModel.requestNavigateToMap()
                                navigateToMainActivity(context)
                            },
                            trackMapContent = { track ->
                                TrackMapPreview(
                                    track = track,
                                    modifier = Modifier.fillMaxSize()
                                )
                            },
                            onPhotoClick = { photoPickerLauncher.launch("image/*") },
                            onDeletePhoto = {
                                val photoFile = File(context.filesDir, "photos/photo_${mountain.osmId}.jpg")
                                if (photoFile.exists()) photoFile.delete()
                                detailViewModel.deletePhoto(mountain.osmId)
                            },
                            onViewPhoto = {
                                val photoPath = detailViewModel.getPhotoPath(mountain.osmId)
                                if (photoPath != null) {
                                    val intent = Intent()
                                    intent.setClassName(context, "io.github.martinschneider.baiyue.ui.PhotoActivity")
                                    intent.putExtra("photo_path", photoPath)
                                    intent.putExtra("osm_id", mountain.osmId)
                                    photoViewerLauncher.launch(intent)
                                }
                            },
                            googleMapsUrl = detailViewModel.getGoogleMapsUrl(mountain),
                            organicMapsUrl = detailViewModel.getOrganicMapsUrl(mountain),
                            coordinatesString = detailViewModel.getCoordinatesString(mountain),
                            modifier = Modifier.padding(paddingValues),
                        )
                    }
                }
            }
        }
    }
}
