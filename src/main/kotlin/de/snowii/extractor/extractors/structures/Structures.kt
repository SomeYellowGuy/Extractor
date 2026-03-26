package de.snowii.extractor.extractors.structures

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.structure.Structure

class Structures : Extractor.Extractor {
    override fun fileName(): String {
        return "structures.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().getOrThrow(Registries.STRUCTURE).value()
        for (setting in registry) {
            finalJson.add(
                registry.getKey(setting).toString(),
                Structure.DIRECT_CODEC.encodeStart(
                    JsonOps.INSTANCE,
                    setting
                ).getOrThrow()
            )
        }

        return finalJson
    }
}