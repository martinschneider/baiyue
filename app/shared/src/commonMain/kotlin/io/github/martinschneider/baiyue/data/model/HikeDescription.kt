package io.github.martinschneider.baiyue.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HikeDescription(
    val xiaobaiyueIds: List<String>,
    val intro: String,
    val route: String,
    val transport: String,
    val start: HikeCoordinate? = null,
    val end: HikeCoordinate? = null,
    val waypoints: List<HikeWaypoint> = emptyList(),
    val peaks: List<HikeCoordinate> = emptyList(),
    val stats: String? = null,
    val track: List<List<Double>> = emptyList(),
    val gpxFilename: String? = null
)

@Serializable
data class HikeWaypoint(val number: Int, val name: String, val lat: Double, val lng: Double)

@Serializable
data class HikeCoordinate(val name: String, val lat: Double, val lng: Double)
