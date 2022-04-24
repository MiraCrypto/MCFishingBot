package io.github.miracrypto.config

@kotlinx.serialization.Serializable
class DiscordConfig(
    var token: String,
    var sudo: List<Long> = listOf(),
    var control: Long
)
