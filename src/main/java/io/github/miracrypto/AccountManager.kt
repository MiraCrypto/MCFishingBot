package io.github.miracrypto

import com.github.steveice10.mc.auth.service.AuthenticationService
import com.github.steveice10.mc.auth.service.MojangAuthenticationService
import com.github.steveice10.mc.auth.service.MsaAuthenticationService
import com.github.steveice10.mc.auth.service.SessionService
import com.github.steveice10.mc.protocol.MinecraftConstants
import com.github.steveice10.mc.protocol.MinecraftProtocol
import com.github.steveice10.packetlib.tcp.TcpClientSession
import io.github.miracrypto.config.AccountConfig
import io.github.miracrypto.config.MinecraftConfig
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

private const val ACCOUNT_EXPIRE_HOURS = 12

private class AccountInfo(val auth: AuthenticationService, val session: SessionService) {
    private var lastLogin = Instant.now()

    fun hasExpired(): Boolean {
        return Duration.between(lastLogin, Instant.now()).toHours() > ACCOUNT_EXPIRE_HOURS
    }

    fun login() {
        lastLogin = Instant.now()
        auth.login()
    }
}

class AccountManager(configs: Iterable<AccountConfig>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val configList: List<AccountConfig>
    private val accounts: MutableMap<String, AccountInfo>

    init {
        configList = configs.toList()
        accounts = mutableMapOf()
    }

    fun login() {
        configList.forEach { login(it) }
    }

    private fun login(config: AccountConfig) {
        val authService = if (config.refreshToken == null) {
            MojangAuthenticationService().apply {
                username = config.username
                password = config.password
                accessToken = config.accessToken
            }
        } else {
            MsaAuthenticationService(config.clientToken).apply {
                refreshToken = config.refreshToken
            }
        }

        val sessionService = SessionService()
        config.authServer?.let {
            authService.setBaseUri("$it/authserver/")
            sessionService.setBaseUri("$it/sessionserver/session/minecraft/")
        }

        val info = AccountInfo(authService, sessionService)
        info.login()

        val key = config.name ?: authService.selectedProfile.name
        accounts[key] = info
    }

    fun createClientSession(config: MinecraftConfig): TcpClientSession {
        val key = config.account

        val session = accounts[key]?.session
        val protocol = createProtocol(key)

        return TcpClientSession(config.host, config.port, protocol).apply {
            setFlag(MinecraftConstants.SESSION_SERVICE_KEY, session)
        }
    }

    private fun createProtocol(name: String): MinecraftProtocol? =
        accounts[name]?.run {
            if (hasExpired()) {
                logger.warn("Account refresh for {}!", name)
                login()
            }
            return MinecraftProtocol(auth.selectedProfile, auth.accessToken)
        }

    private val accountNames
        get() = accounts.keys.toSet()

    fun findAccount(name: String): String? {
        return accountNames.find { it.startsWith(name, true) }
    }
}
