package io.github.miracrypto.bot.modules

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ObjectEntityMetadata
import com.github.steveice10.mc.protocol.data.game.entity.`object`.ProjectileData
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import io.github.miracrypto.*
import io.github.miracrypto.client.ClientSessionAdapter
import io.github.miracrypto.client.MinecraftClient
import io.github.miracrypto.config.modules.FishingConfig
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class FishingBot(discord: JDA, private val config: FishingConfig) : ClientSessionAdapter() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var client: MinecraftClient

    private var fishHookId = -1
    private var fishItemId = 0
    private val fishCaught: MutableList<Instant> = mutableListOf()
    private val loot: MutableList<ItemStack> = mutableListOf()

    private val statusChannel = config.status?.let { discord.getTextChannelById(it) }!!
    private lateinit var discordStatus: Message
    private lateinit var checkFishingFuture: ScheduledFuture<*>

    private val fmt = DecimalFormat("0.0")

    private fun checkFishCaught() {
        fishCaught.lastOrNull()?.let {
            if (Duration.between(it, Instant.now()).toSeconds() > config.idleMaxSec) {
                client.session.disconnect("Fishing time out")
            }
        }
    }

    private fun updateStatus() {
        val message = loot.groupBy { it.id }
            .entries
            .sortedByDescending { it.value.size }
            .joinToString("\n") { (key, value) ->
                "${MinecraftData.getItemName(key)}: ${value.sumOf { it.amount }}"
            }

        val total = Duration.between(fishCaught.first(), Instant.now())
        val lastN = fishCaught.takeLast(config.averageOverLast)
        val lastNSecs = Duration.between(lastN.first(), Instant.now()).toSeconds()
        val average = lastNSecs.toDouble() / lastN.size
        val uptimeStr = "%d:%02d".format(total.toHours(), total.toMinutesPart())

        val statusMessage: String = client.player.run {
            listOf(
                "**Health**: ${fmt.format(health)}, **Food**: $food, **Saturation**: ${fmt.format(saturation)}",
                "**Level**: $level, **Exp**: $totalExperience (${fmt.format(experience * 100)}%)",
                "**Count**: ${fishCaught.size}, **Avg**: ${fmt.format(average)}s, **Uptime**: $uptimeStr",
                "",
                message
            ).joinToString("\n")
        }

        if (this::discordStatus.isInitialized) {
            discordStatus.editMessage(statusMessage).queue()
        }
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundLoginPacket) {
        scheduleUseItem(1500)
        statusChannel
            .sendMessage("AutoFish bot is logged in! Entity ID: ${client.player.entityId}")
            .queue { discordStatus = it }
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundAddEntityPacket) {
        logger.debug("Get Spawn Entity! $packet")

        if (fishItemId < 0 && packet.type == EntityType.ITEM) {
            fishItemId = packet.entityId
        }

        // Check if fishing fishhook spawns
        else if (packet.type == EntityType.FISHING_BOBBER && (packet.data as ProjectileData).ownerId == client.player.entityId) {
            fishHookId = packet.entityId
        }
    }

    override fun packetReceived(client: MinecraftClient, packet: ClientboundSetEntityDataPacket) {
        val meta = packet.metadata.associateBy { it.id }

        when (packet.entityId) {
            fishItemId -> {
                fishItemId = 0
                ((meta[8] as ObjectEntityMetadata<*>).value as ItemStack).let {
                    logger.debug("Item Metadata: ${it.itemName}, amount = ${it.amount}")

                    loot.add(it)
                    it.nbt?.let { tag ->
                        logger.info("NBT: $tag")
                        it.enchantmentsDescription.run {
                            if (!isEmpty() && config.announceEnchanted.contains(it.itemId)) {
                                statusChannel.sendMessage("**${it.itemName}**\n$this").queue()
                            }
                        }
                    }
                    updateStatus()
                }
            }

            fishHookId -> {
                logger.debug("Get Fish Hook Metadata! $packet")

                // Check if caught a fish
                (meta[9] as? BooleanEntityMetadata)?.primitiveValue?.let {
                    if (it) {
                        scheduleUseItem(200)
                        scheduleUseItem(1000)
                    }
                }

                // Check if hooked an entity. Value is hooked entity ID + 1
                (meta[8] as? IntEntityMetadata)?.primitiveValue?.let {
                    if (it != 0) {
                        logger.info("Hooked an entity: ID = $it")
                        scheduleUseItem(500)
                        scheduleUseItem(3000)
                    }
                }
            }
        }
    }

    private fun scheduleUseItem(delay: Long) {
        client.scheduler.schedule({ useItem() }, delay, TimeUnit.MILLISECONDS)
    }

    private fun useItem() {
        if (fishHookId > 0) {
            fishHookId = -1
            fishItemId = -1
            fishCaught.add(Instant.now())
        }

        client.useItem()
    }

    override fun connected(client: MinecraftClient, event: ConnectedEvent) {
        this.client = client
        checkFishingFuture = client.scheduler.scheduleWithFixedDelay({ checkFishCaught() }, 10, 10, TimeUnit.SECONDS)
    }

    override fun disconnected(client: MinecraftClient, event: DisconnectedEvent) {
        checkFishingFuture.cancel(false)
    }
}
