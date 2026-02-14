package io.github.martinschneider.baiyue.data.model

data class ClimbProgress(
    val baiyueClimbed: Int,
    val baiyueTotal: Int = 100,
    val xiaobaiyueClimbed: Int,
    val xiaobaiyueTotal: Int = 100,
    val oldXiaobaiyueClimbed: Int,
    val oldXiaobaiyueTotal: Int = 16
)

data class PeakStatus(
    val osmId: Long,
    val climbed: Boolean = false,
    val climbedDate: String? = null,
    val hasPhoto: Boolean = false,
    val photoPath: String? = null
)

data class ClimbDate(
    val id: Long,
    val osmId: Long,
    val date: String
)
