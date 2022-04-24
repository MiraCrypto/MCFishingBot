package io.github.miracrypto.config.modules

@kotlinx.serialization.Serializable
data class FishingConfig(
    var status: Long?,
    var announceEnchanted: List<String> = listOf(),
    val idleMaxSec: Int = 45,
    val averageOverLast: Int = 50,
)
