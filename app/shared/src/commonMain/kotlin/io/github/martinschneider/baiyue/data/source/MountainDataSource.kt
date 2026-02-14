package io.github.martinschneider.baiyue.data.source

import io.github.martinschneider.baiyue.data.model.Mountain
import kotlinx.serialization.json.Json

class MountainDataSource {
    private val json = Json { ignoreUnknownKeys = true }
    private var cachedMountains: List<Mountain>? = null

    fun loadMountains(jsonString: String): List<Mountain> {
        return cachedMountains ?: json.decodeFromString<List<Mountain>>(jsonString).also {
            cachedMountains = it
        }
    }

    fun getMountains(): List<Mountain> {
        return cachedMountains ?: error("Mountains not loaded. Call loadMountains() first.")
    }
}
