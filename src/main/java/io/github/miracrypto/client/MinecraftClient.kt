package io.github.miracrypto.client

import com.github.steveice10.mc.protocol.data.game.ClientCommand
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket
import com.github.steveice10.packetlib.Session
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectingEvent
import com.github.steveice10.packetlib.packet.Packet
import io.github.miracrypto.NotReconnectableException
import io.github.miracrypto.config.DiscordConfig
import io.github.miracrypto.config.MinecraftConfig
import io.github.miracrypto.translation
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MinecraftClient(
    private val minecraftConfig: MinecraftConfig,
    val discordConfig: DiscordConfig,
    val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()) : ClientSessionAdapter() {

    private val logger = LoggerFactory.getLogger(javaClass)

    val player: Player = Player()

    private val listeners: MutableSet<ClientSessionAdapter> = mutableSetOf()

    lateinit var session: Session

    private val chatBuffer: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private var chatEventFuture: ScheduledFuture<*>? = null

    private val allListeners
        get() = listOf(this) + listeners

    fun addListener(listener: ClientSessionAdapter) {
        listeners.add(listener)
    }

    fun removeListener(listener: ClientSessionAdapter) {
        listeners.remove(listener)
    }

    override fun connected(event: ConnectedEvent) {
        session = event.session
        listeners.forEach { it.connected(event) }
        allListeners.forEach { it.connected(this, event) }
    }

    override fun disconnecting(event: DisconnectingEvent) {
        listeners.forEach { it.disconnecting(event) }
        allListeners.forEach { it.disconnecting(this, event) }
    }

    override fun disconnected(event: DisconnectedEvent) {
        listeners.forEach { it.disconnected(event) }
        allListeners.forEach { it.disconnected(this, event) }
    }

    override fun packetReceived(session: Session, packet: Packet) {
        listeners.forEach { it.packetReceived(session, packet) }
        allListeners.forEach { it.packetReceived(this, packet) }
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundLoginPacket) {
        player.entityId = packet.entityId
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundSetHealthPacket) {
        player.run {
            health = packet.health
            food = packet.food
            saturation = packet.saturation
            logger.info("SetHealth: Health = $health, Food = $food, Saturation = $saturation")
        }

        // Quit if health is too low
        if (packet.health <= minecraftConfig.lowHealthLeave) {
            session.disconnect("Health is too low!", NotReconnectableException("Disconnected due to low health!"))
        }
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundSetExperiencePacket) {
        player.run {
            experience = packet.experience
            level = packet.level
            totalExperience = packet.totalExperience
            logger.debug("SetExp: experience = $experience, level = $level, totalExperience = $totalExperience")
        }
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundChatPacket) {
        chatBuffer.add(packet.message.translation)

        chatEventFuture?.cancel(false)
        chatEventFuture = scheduler.schedule({
            val chatBatch = chatBuffer.toList()
            chatBuffer.clear()
            listeners.forEach { it.onChatBatch(client, chatBatch) }
        }, 500, TimeUnit.MILLISECONDS)
    }

    fun useItem() {
        session.run {
            send(ServerboundUseItemPacket(Hand.MAIN_HAND))
            send(ServerboundSwingPacket(Hand.MAIN_HAND))
        }
    }

    fun respawn() {
        session.send(ServerboundClientCommandPacket(ClientCommand.RESPAWN))
    }

    fun sendChat(message: String) {
        session.send(ServerboundChatPacket(message))
    }
}
