package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.Registry
import net.minecraft.resources.RegistryDataLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.resources.ResourceKey


class SyncedRegistries : Extractor.Extractor {
    override fun fileName(): String {
        return "synced_registries.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        RegistryDataLoader.SYNCHRONIZED_REGISTRIES.forEach { entry ->
            json.add(entry.key().identifier().path, mapJson(entry, server))
        }
        return json
    }

    private fun <T : Any> mapJson(
        registryData: RegistryDataLoader.RegistryData<T>,
        server: MinecraftServer
    ): JsonObject {
        val codec: Codec<T> = registryData.elementCodec()
        val registryAccess = server.registries().compositeAccess()
        val registry = registryAccess.lookupOrThrow(registryData.key())
        val ops = registryAccess.createSerializationContext(JsonOps.INSTANCE)
        val json = JsonObject()
        registry.asHolderIdMap().forEach { entry ->
            json.add(
                entry.unwrapKey().get().identifier().path,
                codec.encodeStart(ops, entry.value()).orThrow
            )
        }
        return json
    }
}