package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.dimension.DimensionType

class Dimension : Extractor.Extractor {
    override fun fileName(): String {
        return "dimension.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().getOrThrow(Registries.DIMENSION_TYPE).value()

        for (setting in registry) {
            finalJson.add(
                registry.getId(setting).toString(),
                DimensionType.DIRECT_CODEC.encodeStart(
                    JsonOps.INSTANCE,
                    setting
                ).getOrThrow()
            )
        }

        return finalJson
    }
}