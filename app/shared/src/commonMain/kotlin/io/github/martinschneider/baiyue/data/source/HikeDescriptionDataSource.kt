package io.github.martinschneider.baiyue.data.source

import io.github.martinschneider.baiyue.data.model.HikeDescription
import kotlinx.serialization.json.Json

class HikeDescriptionDataSource {
    private val json = Json { ignoreUnknownKeys = true }
    private var descriptions: List<HikeDescription> = emptyList()
    private var byId: Map<String, HikeDescription> = emptyMap()

    fun load(jsonString: String) {
        descriptions = json.decodeFromString<List<HikeDescription>>(jsonString)
        byId = buildMap {
            for (desc in descriptions) {
                for (id in desc.xiaobaiyueIds) {
                    put(id, desc)
                }
            }
        }
    }

    fun getByXiaobaiyueId(id: String): HikeDescription? = byId[id]

    fun getAllWithTracks(): List<HikeDescription> = descriptions.filter { it.track.isNotEmpty() }
}
