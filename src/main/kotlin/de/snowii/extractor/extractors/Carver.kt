package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver

class Carver : Extractor.Extractor {
    override fun fileName(): String {
        return "carver.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()

        val registry = server.registryAccess().lookupOrThrow(Registries.CONFIGURED_CARVER)

        val ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE)

        registry.listElements().forEach { holder ->
            val carver = holder.value()
            val key = holder.key()

            val json = ConfiguredWorldCarver.DIRECT_CODEC.encodeStart(
                ops,
                carver
            ).getOrThrow()

            finalJson.add(
                key.identifier().path,
                json
            )
        }

        return finalJson
    }
}