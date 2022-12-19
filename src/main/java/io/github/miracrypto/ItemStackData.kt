package io.github.miracrypto

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack
import com.github.steveice10.opennbt.tag.builtin.CompoundTag
import com.github.steveice10.opennbt.tag.builtin.ListTag
import com.github.steveice10.opennbt.tag.builtin.ShortTag
import com.github.steveice10.opennbt.tag.builtin.StringTag

val ItemStack.itemName: String
    get() = MinecraftData.getItemName(this.id)

val ItemStack.itemId: String
    get() = MinecraftData.getItemId(this.id)

val ItemStack.enchantmentsDescription: String
    get() = this.enchantments.map { (id, level) ->
        val name = "enchantment.${id.replace(":", ".")}".translation
        if (level > 1) "$name $level" else name
    }
        .sorted()
        .joinToString(", ")


val ItemStack.enchantments: Map<String, Short>
    get() {
        val nbt = this.nbt

        val enchantments = if (nbt.contains("Enchantments")) {
            nbt.get("Enchantments")
        } else if (nbt.contains("StoredEnchantments")) {
            nbt.get<ListTag>("StoredEnchantments")
        } else {
            return emptyMap()
        }

        return enchantments.value
            .map { it as CompoundTag }
            .associate { it.get<StringTag>("id").value to it.get<ShortTag>("lvl").value }
    }
