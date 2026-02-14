package io.github.martinschneider.baiyue.ui.screen.detail

import io.github.martinschneider.baiyue.data.model.HikeDescription
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.data.model.MountainType
import io.github.martinschneider.baiyue.data.repository.MountainRepository
import io.github.martinschneider.baiyue.data.source.HikeDescriptionDataSource
import io.github.martinschneider.baiyue.platform.OrganicMaps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DetailViewModel(
    private val repository: MountainRepository,
    private val hikeDescriptions: HikeDescriptionDataSource
) {
    private val _selectedMountain = MutableStateFlow<Mountain?>(null)
    val selectedMountain: StateFlow<Mountain?> = _selectedMountain.asStateFlow()

    val climbedPeaks = repository.climbedPeaks
    val photoPeaks = repository.photoPeaks
    val climbDates = repository.climbDates

    fun selectMountain(osmId: Long) {
        _selectedMountain.value = repository.getMountainByOsmId(osmId)
    }

    fun clearSelection() {
        _selectedMountain.value = null
    }

    fun toggleClimbed(osmId: Long): Boolean {
        return repository.toggleClimbed(osmId)
    }

    fun isClimbed(osmId: Long): Boolean = repository.isClimbed(osmId)

    fun hasPhoto(osmId: Long): Boolean = repository.hasPhoto(osmId)

    fun getPhotoPath(osmId: Long): String? = repository.getPhotoPath(osmId)

    fun setPhotoPath(osmId: Long, path: String) = repository.setPhotoPath(osmId, path)

    fun deletePhoto(osmId: Long) = repository.deletePhoto(osmId)

    fun addClimbDate(osmId: Long, date: String) = repository.addClimbDate(osmId, date)

    fun deleteClimbDate(id: Long) = repository.deleteClimbDate(id)

    fun getGoogleMapsUrl(mountain: Mountain): String {
        return "https://maps.google.com/?q=${mountain.lat},${mountain.lng}"
    }

    fun getAppleMapsUrl(mountain: Mountain): String {
        return "http://maps.apple.com/?q=${mountain.lat},${mountain.lng}"
    }

    fun getOrganicMapsUrl(mountain: Mountain): String {
        return OrganicMaps.encodeUrl(mountain.lat, mountain.lng, mountain.english)
    }

    fun getHikeDescription(mountain: Mountain): HikeDescription? {
        val id = mountain.xiaobaiyueId ?: return null
        return hikeDescriptions.getByXiaobaiyueId(id)
    }

    fun getCoordinatesString(mountain: Mountain): String {
        return "${mountain.lat}, ${mountain.lng}"
    }

    private fun getOrderedList(mountain: Mountain): List<Mountain> {
        val list = when (mountain.type) {
            MountainType.BAIYUE -> repository.getBaiyue()
            MountainType.XIAOBAIYUE, MountainType.XIAOBAIYUE_OLD -> repository.getXiaobaiyue()
        }
        return list.sortedBy { it.id ?: Int.MAX_VALUE }
    }

    fun getPreviousMountain(mountain: Mountain): Mountain? {
        val list = getOrderedList(mountain)
        val index = list.indexOfFirst { it.osmId == mountain.osmId }
        return if (index > 0) list[index - 1] else null
    }

    fun getNextMountain(mountain: Mountain): Mountain? {
        val list = getOrderedList(mountain)
        val index = list.indexOfFirst { it.osmId == mountain.osmId }
        return if (index in 0 until list.size - 1) list[index + 1] else null
    }
}
