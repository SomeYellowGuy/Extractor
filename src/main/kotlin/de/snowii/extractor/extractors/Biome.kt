package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.biome.Biome

class Biome : Extractor.Extractor {
    override fun fileName(): String {
        return "biome.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val biomeData = JsonObject()
        val biomeRegistry =
            server.registryAccess().getOrThrow(Registries.BIOME).value()
        for (biome in biomeRegistry) {
            val json = Biome.DIRECT_CODEC.encodeStart(
                JsonOps.INSTANCE,
                biome
            ).getOrThrow().asJsonObject
            json.addProperty("id", biomeRegistry.getId(biome))
            biomeData.add(
                biomeRegistry.getKey(biome)!!.path, json
            )

        }

        return biomeData
    }
}