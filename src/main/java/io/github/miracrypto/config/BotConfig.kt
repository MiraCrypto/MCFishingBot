package io.github.miracrypto.config

@kotlinx.serialization.Serializable
data class BotConfig(
    var accounts: List<AccountConfig>,
    var minecraft: List<MinecraftConfig>,
    var discord: DiscordConfig
)
