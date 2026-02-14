package io.github.martinschneider.baiyue.platform

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.martinschneider.baiyue.data.model.HikeDescription
import io.github.martinschneider.baiyue.ui.screen.map.MapViewModel
import io.github.martinschneider.baiyue.ui.theme.*
import org.koin.compose.koinInject

@Composable
fun MapScreenContent(
    viewModel: MapViewModel = koinInject(),
    onMarkerClick: (Long) -> Unit,
    onTrackClick: (HikeDescription) -> Unit = {},
) {
    val showBaiyue by viewModel.showBaiyue.collectAsState()
    val showXiaobaiyue by viewModel.showXiaobaiyue.collectAsState()
    val showTracks by viewModel.showTracks.collectAsState()
    val climbed by viewModel.climbedPeaks.collectAsState()
    val photos by viewModel.photoPeaks.collectAsState()
    val mountains = remember(showBaiyue, showXiaobaiyue) {
        viewModel.getVisibleMountains(showBaiyue, showXiaobaiyue)
    }
    val tracks = remember { viewModel.getTracksForMap() }

    OsmMapContent(
        viewModel = viewModel,
        mountains = mountains,
        climbed = climbed,
        photos = photos,
        onMarkerClick = onMarkerClick,
        showBaiyue = showBaiyue,
        showXiaobaiyue = showXiaobaiyue,
        tracks = tracks,
        showTracks = showTracks,
        onTrackClick = onTrackClick,
    )
}

@Composable
internal fun MapOverlay(
    viewModel: MapViewModel,
    showBaiyue: Boolean,
    showXiaobaiyue: Boolean,
    showTracks: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = showBaiyue,
                onClick = { viewModel.toggleShowBaiyue() },
                label = { Text("百岳") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BaiyuePrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = showXiaobaiyue,
                onClick = { viewModel.toggleShowXiaobaiyue() },
                label = { Text("小百岳") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = XiaobaiyuePrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = showTracks,
                onClick = { viewModel.toggleShowTracks() },
                label = { Text("Tracks") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFF7043),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
