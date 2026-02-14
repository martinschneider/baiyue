package io.github.martinschneider.baiyue.ui.screen.map

import io.github.martinschneider.baiyue.data.MapPreferences
import io.github.martinschneider.baiyue.data.model.HikeDescription
import io.github.martinschneider.baiyue.data.model.LatLng
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.data.model.Region
import io.github.martinschneider.baiyue.data.repository.MountainRepository
import io.github.martinschneider.baiyue.data.source.HikeDescriptionDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel(
    private val repository: MountainRepository,
    private val hikeDescriptionDataSource: HikeDescriptionDataSource,
    val mapPreferences: MapPreferences
) {
    private val _showBaiyue = MutableStateFlow(mapPreferences.showBaiyue)
    val showBaiyue: StateFlow<Boolean> = _showBaiyue.asStateFlow()

    private val _showXiaobaiyue = MutableStateFlow(mapPreferences.showXiaobaiyue)
    val showXiaobaiyue: StateFlow<Boolean> = _showXiaobaiyue.asStateFlow()

    private val _showTracks = MutableStateFlow(mapPreferences.showTracks)
    val showTracks: StateFlow<Boolean> = _showTracks.asStateFlow()

    private val _selectedMountainId = MutableStateFlow<Long?>(null)
    val selectedMountainId: StateFlow<Long?> = _selectedMountainId.asStateFlow()

    private val _pendingZoomTarget = MutableStateFlow<LatLng?>(null)
    val pendingZoomTarget: StateFlow<LatLng?> = _pendingZoomTarget.asStateFlow()

    private val _pendingBoundsZoom = MutableStateFlow<Pair<LatLng, LatLng>?>(null)
    val pendingBoundsZoom: StateFlow<Pair<LatLng, LatLng>?> = _pendingBoundsZoom.asStateFlow()

    private val _pendingNavigateToMap = MutableStateFlow(false)
    val pendingNavigateToMap: StateFlow<Boolean> = _pendingNavigateToMap.asStateFlow()

    fun setPendingZoomTarget(target: LatLng) { _pendingZoomTarget.value = target }
    fun clearPendingZoomTarget() { _pendingZoomTarget.value = null }

    fun setPendingBoundsZoom(sw: LatLng, ne: LatLng) { _pendingBoundsZoom.value = sw to ne }
    fun clearPendingBoundsZoom() { _pendingBoundsZoom.value = null }

    fun enableTracks() {
        _showTracks.value = true
        mapPreferences.showTracks = true
    }

    fun requestNavigateToMap() { _pendingNavigateToMap.value = true }
    fun clearNavigateToMap() { _pendingNavigateToMap.value = false }

    val climbedPeaks = repository.climbedPeaks
    val photoPeaks = repository.photoPeaks

    fun toggleShowBaiyue() {
        _showBaiyue.value = !_showBaiyue.value
        mapPreferences.showBaiyue = _showBaiyue.value
    }
    fun toggleShowXiaobaiyue() {
        _showXiaobaiyue.value = !_showXiaobaiyue.value
        mapPreferences.showXiaobaiyue = _showXiaobaiyue.value
    }
    fun toggleShowTracks() {
        _showTracks.value = !_showTracks.value
        mapPreferences.showTracks = _showTracks.value
    }

    fun getTracksForMap(): List<HikeDescription> = hikeDescriptionDataSource.getAllWithTracks()

    fun selectMountain(osmId: Long?) { _selectedMountainId.value = osmId }

    fun getMountains(): List<Mountain> = repository.getMountains()

    fun getMountainByOsmId(osmId: Long): Mountain? = repository.getMountainByOsmId(osmId)

    fun getMountainByXiaobaiyueId(id: String): Mountain? =
        repository.getMountains().find { it.xiaobaiyueId == id }

    fun getVisibleMountains(showBaiyue: Boolean, showXiaobaiyue: Boolean): List<Mountain> {
        return repository.getMountains().filter { mountain ->
            when {
                mountain.isBaiyue -> showBaiyue
                mountain.isXiaobaiyue -> showXiaobaiyue
                else -> false
            }
        }
    }

    fun getRegionBounds(region: Region): Pair<LatLng, LatLng> {
        val mountains = repository.getMountainsByRegion(region.name)
        if (mountains.isEmpty()) return LatLng(23.5, 120.5) to LatLng(24.0, 121.0)

        val minLat = mountains.minOf { it.lat }
        val maxLat = mountains.maxOf { it.lat }
        val minLng = mountains.minOf { it.lng }
        val maxLng = mountains.maxOf { it.lng }

        val padding = 0.15
        return LatLng(minLat - padding, minLng - padding) to
               LatLng(maxLat + padding, maxLng + padding)
    }

    fun getTaiwanBounds(): Pair<LatLng, LatLng> {
        val mountains = repository.getMountains()
        val minLat = mountains.minOf { it.lat }
        val maxLat = mountains.maxOf { it.lat }
        val minLng = mountains.minOf { it.lng }
        val maxLng = mountains.maxOf { it.lng }
        val padding = 0.2
        return LatLng(minLat - padding, minLng - padding) to
               LatLng(maxLat + padding, maxLng + padding)
    }
}
