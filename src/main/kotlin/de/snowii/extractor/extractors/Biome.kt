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

        val biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME)

        val ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE)

        biomeRegistry.listElements().forEach { holder ->
            val biome = holder.value()
            val key = holder.key()

            val json = Biome.DIRECT_CODEC.encodeStart(
                ops,
                biome
            ).getOrThrow().asJsonObject

            json.addProperty("id", biomeRegistry.getId(biome))

            biomeData.add(
                key.identifier().path,
                json
            )
        }

        return biomeData
    }
}