package io.github.miracrypto.config

@kotlinx.serialization.Serializable
data class AccountConfig(
    var name: String? = null,
    var username: String? = null,
    var password: String? = null,
    var accessToken: String? = null,
    var clientToken: String? = null,
    var refreshToken: String? = null,
    var authServer: String? = null
)
