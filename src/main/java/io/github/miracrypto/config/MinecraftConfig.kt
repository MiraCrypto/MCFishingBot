package io.github.miracrypto.config

import io.github.miracrypto.config.modules.FishingConfig
import io.github.miracrypto.config.modules.ChatConfig
import io.github.miracrypto.config.modules.MobNotifyConfig

@kotlinx.serialization.Serializable
data class MinecraftConfig(
    var name: String? = null,
    var account: String,
    var host: String,
    var port: Int = 25565,
    var fishing: FishingConfig? = null,
    var chat: ChatConfig? = null,
    var mobNotify: MobNotifyConfig? = null,
    var lowHealthLeave: Int = 15
)
