package io.github.miracrypto.config.modules

@kotlinx.serialization.Serializable
data class MobNotifyConfig(
    var channel: Long?,
    var entityTypes: Set<String> = setOf()
)
