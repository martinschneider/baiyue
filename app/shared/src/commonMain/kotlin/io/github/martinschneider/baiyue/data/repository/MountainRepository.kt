package io.github.martinschneider.baiyue.data.repository

import io.github.martinschneider.baiyue.data.db.BaiyueDatabase
import io.github.martinschneider.baiyue.data.model.ClimbDate
import io.github.martinschneider.baiyue.data.model.ClimbProgress
import io.github.martinschneider.baiyue.data.model.Mountain
import io.github.martinschneider.baiyue.data.model.MountainType
import io.github.martinschneider.baiyue.data.model.PeakStatus
import io.github.martinschneider.baiyue.data.source.MountainDataSource
import io.github.martinschneider.baiyue.platform.currentTimeString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MountainRepository(
    private val dataSource: MountainDataSource,
    private val database: BaiyueDatabase
) {
    private val queries get() = database.baiyueDatabaseQueries

    private val _climbedPeaks = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val climbedPeaks: StateFlow<Map<Long, Boolean>> = _climbedPeaks.asStateFlow()

    private val _climbDates = MutableStateFlow<Map<Long, List<ClimbDate>>>(emptyMap())
    val climbDates: StateFlow<Map<Long, List<ClimbDate>>> = _climbDates.asStateFlow()

    private val _photoPeaks = MutableStateFlow<Set<Long>>(emptySet())
    val photoPeaks: StateFlow<Set<Long>> = _photoPeaks.asStateFlow()

    fun getMountains(): List<Mountain> = dataSource.getMountains()

    fun getBaiyue(): List<Mountain> = getMountains().filter { it.isBaiyue }

    fun getXiaobaiyue(): List<Mountain> = getMountains().filter { it.isXiaobaiyue }

    fun getCurrentXiaobaiyue(): List<Mountain> = getMountains().filter { it.isCurrentXiaobaiyue }

    fun getOldXiaobaiyue(): List<Mountain> = getMountains().filter { it.isOldXiaobaiyue }

    fun getMountainByOsmId(osmId: Long): Mountain? = getMountains().find { it.osmId == osmId }

    fun getMountainsByRegion(region: String): List<Mountain> =
        getMountains().filter { it.region == region }

    fun loadClimbedStatus() {
        val climbed = queries.getAllClimbedStatus().executeAsList()
            .associate { it.osm_id to (it.climbed == 1L) }
        _climbedPeaks.value = climbed

        val photos = queries.getPhotoOsmIds().executeAsList().toSet()
        _photoPeaks.value = photos

        loadClimbDates()
    }

    private fun loadClimbDates() {
        val dates = queries.getAllClimbDates().executeAsList()
            .map { ClimbDate(it.id, it.osm_id, it.climb_date) }
            .groupBy { it.osmId }
        _climbDates.value = dates
    }

    fun getClimbDates(osmId: Long): List<ClimbDate> {
        return _climbDates.value[osmId] ?: emptyList()
    }

    fun addClimbDate(osmId: Long, date: String) {
        queries.addClimbDate(osmId, date)
        loadClimbDates()
        // Auto-mark as climbed
        if (_climbedPeaks.value[osmId] != true) {
            setClimbed(osmId, true)
        }
    }

    fun deleteClimbDate(id: Long) {
        // Find the osmId before deleting
        val allDates = _climbDates.value
        val osmId = allDates.values.flatten().find { it.id == id }?.osmId
        queries.deleteClimbDate(id)
        loadClimbDates()
        // Auto-unmark if no dates remain
        if (osmId != null && (_climbDates.value[osmId] ?: emptyList()).isEmpty()) {
            setClimbed(osmId, false)
        }
    }

    fun getAllClimbDatesMap(): Map<Long, List<ClimbDate>> = _climbDates.value

    fun toggleClimbed(osmId: Long): Boolean {
        val currentlyClimbed = _climbedPeaks.value[osmId] == true
        val newClimbed = !currentlyClimbed

        if (newClimbed) {
            queries.setClimbed(osmId, 1L, null)
        } else {
            queries.setClimbed(osmId, 0L, null)
        }

        _climbedPeaks.value = _climbedPeaks.value.toMutableMap().apply {
            put(osmId, newClimbed)
        }
        return newClimbed
    }

    fun setClimbed(osmId: Long, climbed: Boolean, date: String? = null) {
        queries.setClimbed(osmId, if (climbed) 1L else 0L, date)
        _climbedPeaks.value = _climbedPeaks.value.toMutableMap().apply {
            put(osmId, climbed)
        }
    }

    fun isClimbed(osmId: Long): Boolean = _climbedPeaks.value[osmId] == true

    fun hasPhoto(osmId: Long): Boolean = osmId in _photoPeaks.value

    fun getPhotoPath(osmId: Long): String? {
        return queries.getPhoto(osmId).executeAsOneOrNull()?.photo_path
    }

    fun setPhotoPath(osmId: Long, path: String) {
        val now = currentTimeString()
        queries.setPhoto(osmId, path, now)
        _photoPeaks.value = _photoPeaks.value + osmId
    }

    fun deletePhoto(osmId: Long) {
        queries.deletePhoto(osmId)
        _photoPeaks.value = _photoPeaks.value - osmId
    }

    fun getProgress(): ClimbProgress {
        val mountains = getMountains()
        val climbed = _climbedPeaks.value

        return ClimbProgress(
            baiyueClimbed = mountains.count { it.isBaiyue && climbed[it.osmId] == true },
            xiaobaiyueClimbed = mountains.count { it.isCurrentXiaobaiyue && climbed[it.osmId] == true },
            oldXiaobaiyueClimbed = mountains.count { it.isOldXiaobaiyue && climbed[it.osmId] == true }
        )
    }

    fun deleteAllData() {
        queries.deleteAllClimbed()
        queries.deleteAllPhotos()
        queries.deleteAllClimbDates()
        _climbedPeaks.value = emptyMap()
        _photoPeaks.value = emptySet()
        _climbDates.value = emptyMap()
    }

    fun getAllClimbedMap(): Map<Long, Boolean> {
        return queries.getAllClimbedStatus().executeAsList()
            .associate { it.osm_id to (it.climbed == 1L) }
    }

    fun getAllPhotoEntries(): List<Pair<Long, String>> {
        return queries.getAllPhotos().executeAsList()
            .map { it.osm_id to it.photo_path }
    }

    fun getAllClimbedWithDates(): List<Triple<Long, Boolean, String?>> {
        return queries.getAllClimbedStatus().executeAsList()
            .map { Triple(it.osm_id, it.climbed == 1L, it.climbed_date) }
    }
}
