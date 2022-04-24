package io.github.miracrypto.bot

import com.github.steveice10.packetlib.Session
import io.github.miracrypto.config.MinecraftConfig
import java.util.concurrent.ScheduledFuture

class MinecraftState(val config: MinecraftConfig) {
    var session: Session? = null

    var reconnectFuture: ScheduledFuture<*>? = null
        set(value) {
            field?.cancel(false)
            field = value
        }

    val isConnected: Boolean
        get() = session?.isConnected == true

    val description: String
        get() = config.name?.let { "%s (%s)".format(it, config.account) } ?: config.account

    val statusEmoji: String
        get() = when {
            reconnectFuture?.isDone == false -> Emoji.HOURGLASS_DONE  // waiting to reconnect
            isConnected -> Emoji.GREEN_CIRCLE  // Green circle
            else -> Emoji.RED_CIRCLE  // Red circle
        }
}
