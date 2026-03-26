package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings
import net.minecraft.world.level.chunk.ChunkGenerator

class ChunkGenSetting : Extractor.Extractor {
    override fun fileName(): String {
        return "chunk_gen_settings.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().getOrThrow(Registries.CHUNK_GENERATOR).value()
        for (setting in registry) {
            finalJson.add(
                registry.getKey(setting)!!.path,
                ChunkGenerator.CODEC.encodeStart(
                    JsonOps.INSTANCE,
                    setting
                ).getOrThrow()
            )
        }

        return finalJson
    }
}