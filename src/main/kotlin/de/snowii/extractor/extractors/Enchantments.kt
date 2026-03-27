package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.enchantment.Enchantment

class Enchantments : Extractor.Extractor {
    override fun fileName(): String {
        return "enchantments.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        for (enchantment in registry) {
            val sub = Enchantment.DIRECT_CODEC.encodeStart(
                JsonOps.INSTANCE, enchantment
            ).getOrThrow() as JsonObject
            sub.addProperty("id", registry.getId(enchantment))
            finalJson.add(
                registry.getId(enchantment)!!.toString(), sub
            )
        }
        return finalJson
    }
}