package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.enchantment.Enchantment

class Enchantments : Extractor.Extractor {
    override fun fileName(): String {
        return "enchantments.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        for (enchantment in registry) {
            val sub = Enchantment.CODEC.encodeStart(
                JsonOps.INSTANCE, enchantment
            ).getOrThrow() as JsonObject
            sub.addProperty("id", registry.getRawId(enchantment))
            finalJson.add(
                registry.getId(enchantment)!!.toString(), sub
            )
        }
        return finalJson
    }
}