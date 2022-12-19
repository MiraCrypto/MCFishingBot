package io.github.miracrypto.bot

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisconnectPacket
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundLoginDisconnectPacket
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import io.github.miracrypto.*
import io.github.miracrypto.bot.modules.Chat
import io.github.miracrypto.bot.modules.FishingBot
import io.github.miracrypto.bot.modules.MobNotify
import io.github.miracrypto.client.ClientSessionAdapter
import io.github.miracrypto.client.MinecraftClient
import io.github.miracrypto.config.BotConfig
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class Main(val config: BotConfig): EventListener {
    private val logger = LoggerFactory.getLogger(Main::class.java)

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private val accountManager: AccountManager = AccountManager(config.accounts)
    private val sessions = config.minecraft.map { MinecraftState(it) }.toMutableList()

    private val discordConfig = config.discord
    private val discord = JDABuilder.createDefault(discordConfig.token)
        .addEventListeners(this)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .build()
        .awaitReady()

    private var updateStatusFuture: ScheduledFuture<*>? = null
    private val controlChannel = discord.getTextChannelById(discordConfig.control)!!
    private var statusMessage: Message? = null


    init {
        accountManager.login()

        scheduleUpdateStatus()

        Runtime.getRuntime().addShutdownHook(Thread {
            sessions.forEach { it.session?.run { disconnect("Shutdown", NotReconnectableException()) } }
        })
    }

    private fun connect(idx: Int) {
        val state = sessions[idx]
        if (state.isConnected) {
            return
        }

        // Create a new TCP Minecraft session
        val minecraftConfig = state.config
        val session = accountManager.createClientSession(minecraftConfig)
        state.session = session

        // Initialize the client
        val client = MinecraftClient(minecraftConfig, config.discord, scheduler)
        session.addListener(client)

        client.addListener(object : ClientSessionAdapter() {
            override fun connected(client: MinecraftClient, event: ConnectedEvent) {
                scheduleUpdateStatus()
            }

            override fun disconnected(client: MinecraftClient, event: DisconnectedEvent) {
                val cause = event.cause?.also {
                    logger.warn("Disconnected with cause!", it)
                }

                if (cause !is NotReconnectableException) {
                    val delay = Random.nextLong(15000, 60000)
                    state.reconnectFuture = scheduler.schedule({ connect(idx) }, delay, TimeUnit.MILLISECONDS)
                } else if (cause.message != null) {
                    controlChannel.sendMessage("${state.description} disconnected! ${cause.message}").queue()
                }

                scheduleUpdateStatus()
            }

            override fun packetReceived(client: MinecraftClient, packet: ClientboundDisconnectPacket) {
                controlChannel
                    .sendMessage("${state.description} received disconnect: ${packet.reason.translation}")
                    .queue()
            }

            override fun packetReceived(client: MinecraftClient, packet: ClientboundLoginDisconnectPacket) {
                controlChannel
                    .sendMessage("${state.description} join fail: ${packet.reason.translation}")
                    .queue()
            }
        })

        // Add modules based on config
        minecraftConfig.chat?.let { client.addListener(Chat(discord, it)) }
        minecraftConfig.fishing?.let { client.addListener(FishingBot(discord, it)) }
        minecraftConfig.mobNotify?.let { client.addListener(MobNotify(discord, it)) }

        session.connect()
    }

    private fun scheduleUpdateStatus() {
        updateStatusFuture?.cancel(false)
        updateStatusFuture = scheduler.schedule(this::updateStatus, 5, TimeUnit.SECONDS);
    }

    private fun updateStatus() {
        val accounts = sessions.mapIndexed { idx, state ->
            "%s %d. %s".format(state.statusEmoji, idx + 1, state.description)
        }.joinToString("\n")

        val text = "Minecraft bot logged in! Accounts:\n$accounts"

        if (statusMessage == null) {
            controlChannel.sendMessage(text).queue { statusMessage = it }
        } else {
            statusMessage?.editMessage(text)?.queue()
        }
    }

    override fun onEvent(event: GenericEvent) {
        if (event is MessageReceivedEvent && !event.author.isBot && event.channel == controlChannel) {
            if (!discordConfig.sudo.contains(event.author.idLong)) {
                event.channel.sendMessage("You are not an owner!").queue()
                return
            }

            val content = event.message.contentStripped.trim().lowercase()

            val match = Regex("(\\d+)\\s*(\\w)").matchEntire(content)
            if (match == null) {
                event.message.delete().queue()
                return
            }

            val idx = match.groupValues[1].toInt() - 1
            val cmd = match.groupValues[2]
            val state: MinecraftState

            try {
                state = sessions[idx]
            } catch (e: IndexOutOfBoundsException) {
                event.message.addReaction(Emoji.SKULL).queue()
                event.message.delete().queueAfter(10, TimeUnit.SECONDS)
                return
            }

            when {
                cmd.startsWith("c") -> {
                    logger.info("Connect requested for {}", state.description)
                    if (state.isConnected) {
                        event.message.addReaction(Emoji.PROHIBITED).queue()
                    } else {
                        event.message.addReaction(Emoji.OK_BUTTON).queue()
                        connect(idx)
                    }
                }

                cmd.startsWith("d") -> {
                    logger.info("Disconnect requested for {}", state.description)
                    if (!state.isConnected) {
                        event.message.addReaction(Emoji.PROHIBITED).queue()
                    } else {
                        event.message.addReaction(Emoji.OK_BUTTON).queue()
                        state.session?.disconnect("Disconnected from discord!", NotReconnectableException())
                    }
                }

                else -> {
                    event.message.addReaction(Emoji.CROSS_MARK).queue()
                }
            }

            event.message.delete().queueAfter(10, TimeUnit.SECONDS)
        }
    }
}
