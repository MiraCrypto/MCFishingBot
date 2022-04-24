package io.github.miracrypto.bot.modules

import net.dv8tion.jda.api.JDA
import io.github.miracrypto.config.modules.MobNotifyConfig
import io.github.miracrypto.client.ClientSessionAdapter
import io.github.miracrypto.client.MinecraftClient
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddMobPacket
import io.github.miracrypto.translation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class MobNotify(discord: JDA, var config: MobNotifyConfig) : ClientSessionAdapter() {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val notifyChannel = config.channel?.let { discord.getTextChannelById(it) }!!
    private val seenEntities: MutableSet<UUID> = mutableSetOf()

    override fun packetReceived(client: MinecraftClient, packet: ClientboundAddMobPacket) {
        val mobId = packet.type.toString().lowercase()
        if (mobId in config.entityTypes && packet.uuid !in seenEntities) {
            val mobName = "entity.minecraft.$mobId".translation
            packet.run { "**%s** spawned: (%.1f, %.1f, %.1f)".format(mobName, x, y, z) }.also {
                logger.info(it)
                notifyChannel.sendMessage(it).queue()
            }
            seenEntities.add(packet.uuid)
        }
    }
}
