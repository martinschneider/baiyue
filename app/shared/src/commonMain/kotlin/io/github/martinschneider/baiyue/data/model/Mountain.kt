package io.github.martinschneider.baiyue.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MountainType {
    BAIYUE,
    XIAOBAIYUE,
    XIAOBAIYUE_OLD
}

@Serializable
enum class Region(val displayName: String) {
    north("Northern Taiwan"),
    central("Central Taiwan"),
    south("Southern Taiwan"),
    east("Eastern Taiwan"),
    islands("Outlying Islands");

    companion object {
        val taiwanCenter = LatLng(23.9739881, 120.9097797)
    }
}

data class LatLng(val lat: Double, val lng: Double)

@Serializable
data class Description(
    val name: String,
    val url: String
)

@Serializable
data class Mountain(
    val osmId: Long,
    val type: MountainType,
    val id: Int? = null,
    val xiaobaiyueId: String? = null,
    val chinese: String,
    val english: String,
    val lat: Double,
    val lng: Double,
    val elevation: Double,
    val region: String,
    val descriptions: List<Description> = emptyList()
) {
    val regionEnum: Region
        get() = Region.entries.first { it.name == region }

    val latLng: LatLng
        get() = LatLng(lat, lng)

    val displayId: String?
        get() = when (type) {
            MountainType.BAIYUE -> id?.toString()
            MountainType.XIAOBAIYUE, MountainType.XIAOBAIYUE_OLD -> xiaobaiyueId
        }

    val elevationInt: Int
        get() = elevation.toInt()

    val isBaiyue: Boolean get() = type == MountainType.BAIYUE
    val isXiaobaiyue: Boolean get() = type == MountainType.XIAOBAIYUE || type == MountainType.XIAOBAIYUE_OLD
    val isCurrentXiaobaiyue: Boolean get() = type == MountainType.XIAOBAIYUE
    val isOldXiaobaiyue: Boolean get() = type == MountainType.XIAOBAIYUE_OLD

    val hikingBijiCategory: Int?
        get() = when (type) {
            MountainType.BAIYUE -> 1
            MountainType.XIAOBAIYUE -> 2
            MountainType.XIAOBAIYUE_OLD -> null
        }

    val hikingBijiUrl: String?
        get() = hikingBijiCategory?.let {
            "https://hiking.biji.co/index.php?q=mountain&category=$it&page=1&keyword=$chinese"
        }
}
