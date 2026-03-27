package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings

class ChunkGenSetting : Extractor.Extractor {
    override fun fileName(): String = "chunk_gen_settings.json"

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()

        val registry = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)

        val ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE)

        registry.asHolderIdMap().forEach { holder ->
            val settings = holder.value()
            val name = holder.unwrapKey().get().identifier() .path
            finalJson.add(
                name,
                NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops, settings).getOrThrow()
            )
        }

        return finalJson
    }
}