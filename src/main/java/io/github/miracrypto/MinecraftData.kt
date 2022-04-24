package io.github.miracrypto

import com.google.gson.JsonParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.flattener.ComponentFlattener
import java.io.InputStreamReader

object MinecraftData {
    private val itemData: MutableMap<Int, String> = mutableMapOf()
    private val translation: MutableMap<String, String> = mutableMapOf()

    private val flattener = ComponentFlattener
        .basic()
        .toBuilder()
        .mapper(TranslatableComponent::class.java) { component ->
            getTranslation(component.key())
                .format(*component.args().map(::getTranslation).toTypedArray())
        }
        .build()

    init {
        loadData()
    }

    fun getTranslation(component: Component): String =
        StringBuilder()
            .apply { flattener.flatten(component, ::append) }.toString()

    fun getTranslation(vararg keys: String): String =
        keys.asSequence()
            .firstNotNullOfOrNull { translation[it] } ?: keys.first()

    fun getItemId(protocolId: Int): String =
        itemData[protocolId]!!

    fun getItemName(protocolId: Int): String =
        getItemId(protocolId).replace(":", ".").let {
            getTranslation("item.$it", "block.$it")
        }

    private fun loadData() {
        if (itemData.isNotEmpty()) return

        javaClass.getResourceAsStream("/registries.json")?.let { registries ->
            val reader = InputStreamReader(registries)
            JsonParser.parseReader(reader)
                .asJsonObject
                .getAsJsonObject("minecraft:item")
                .getAsJsonObject("entries")
                .entrySet()
                .associateTo(itemData) { it.value.asJsonObject["protocol_id"].asInt to it.key }
        }

        // Load translation
        javaClass.getResourceAsStream("/en_us.json")?.let { lang ->
            val reader = InputStreamReader(lang)
            JsonParser.parseReader(reader)
                .asJsonObject
                .entrySet()
                .associateTo(translation) { it.key to it.value.asString }
        }
    }
}

val String.translation: String
    get() = MinecraftData.getTranslation(this)

val Component.translation: String
    get() = MinecraftData.getTranslation(this)
