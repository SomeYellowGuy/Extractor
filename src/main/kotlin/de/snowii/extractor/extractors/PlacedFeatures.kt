package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.placement.PlacedFeature

class PlacedFeatures : Extractor.Extractor {
    override fun fileName(): String {
        return "placed_feature.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
        for (setting in registry) {
            finalJson.add(
                registry.getKey(setting)!!.path,
                PlacedFeature.DIRECT_CODEC.encodeStart(
                    JsonOps.INSTANCE,
                    setting
                ).getOrThrow()
            )
        }

        return finalJson
    }
}