package io.github.miracrypto.client

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundDisconnectPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetExperiencePacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetSlotPacket
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectingEvent
import com.github.steveice10.packetlib.event.session.SessionAdapter
import com.github.steveice10.packetlib.packet.Packet

open class ClientSessionAdapter : SessionAdapter() {
    open fun connected(client: MinecraftClient, event: ConnectedEvent) {}
    open fun disconnected(client: MinecraftClient, event: DisconnectedEvent) {}
    open fun disconnecting(client: MinecraftClient, event: DisconnectingEvent) {}

    open fun packetReceived(client: MinecraftClient, packet: ClientboundSetHealthPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundSetExperiencePacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundAddEntityPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundSetEntityDataPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundLoginPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundContainerSetSlotPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundContainerSetContentPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundSystemChatPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundPlayerChatPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundLoginDisconnectPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundDisconnectPacket) {}
    open fun packetReceived(client: MinecraftClient, packet: ClientboundPlayerCombatKillPacket) {}

    open fun onChatBatch(client: MinecraftClient, messages: List<String>) {}

    fun packetReceived(client: MinecraftClient, packet: Packet) {
        when (packet) {
            is ClientboundSetHealthPacket -> packetReceived(client, packet)
            is ClientboundSetExperiencePacket -> packetReceived(client, packet)
            is ClientboundAddEntityPacket -> packetReceived(client, packet)
            is ClientboundSetEntityDataPacket -> packetReceived(client, packet)
            is ClientboundLoginPacket -> packetReceived(client, packet)
            is ClientboundContainerSetSlotPacket -> packetReceived(client, packet)
            is ClientboundContainerSetContentPacket -> packetReceived(client, packet)
            is ClientboundSystemChatPacket -> packetReceived(client, packet)
            is ClientboundPlayerChatPacket -> packetReceived(client, packet)
            is ClientboundLoginDisconnectPacket -> packetReceived(client, packet)
            is ClientboundDisconnectPacket -> packetReceived(client, packet)
            is ClientboundPlayerCombatKillPacket -> packetReceived(client, packet)
        }
    }
}
