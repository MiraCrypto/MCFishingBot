package io.github.miracrypto.bot.modules

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import io.github.miracrypto.bot.Emoji
import io.github.miracrypto.client.ClientSessionAdapter
import io.github.miracrypto.client.MinecraftClient
import io.github.miracrypto.config.modules.ChatConfig
import io.github.miracrypto.translation
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.slf4j.LoggerFactory

class Chat(private val discord: JDA,
           config: ChatConfig) : ClientSessionAdapter(), EventListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var client: MinecraftClient

    private val chatChannel = config.channel?.let { discord.getTextChannelById(it) }!!

    private lateinit var allowedUsers: List<Long>

    override fun onChatBatch(client: MinecraftClient, messages: List<String>) {
        val message = messages.joinToString("\n") { MarkdownSanitizer.escape(it) }
        chatChannel.sendMessage(message).queue()
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundPlayerCombatKillPacket) {
        val message = packet.message
        val text = message.translation
        chatChannel.sendMessage("**You died!** $text").queue()
        logger.info("Death message: {}", text)
    }

    override fun connected(client: MinecraftClient, event: ConnectedEvent) {
        discord.addEventListener(this)
        allowedUsers = client.discordConfig.sudo
        this.client = client
    }

    override fun disconnected(client: MinecraftClient, event: DisconnectedEvent) {
        discord.removeEventListener(this)
    }

    override fun onEvent(event: GenericEvent) {
        if (event is MessageReceivedEvent && !event.author.isBot && event.channel == chatChannel) {
            if (allowedUsers.contains(event.author.idLong)) {
                when (event.message.contentStripped.lowercase()) {
                    "respawn" -> {
                        client.respawn()
                        event.message.addReaction(Emoji.CHECK_MARK_BUTTON).queue()
                    }
                    else -> client.sendChat(event.message.contentStripped)
                }
            } else {
                event.message.delete().queue()
            }
        }
    }
}
