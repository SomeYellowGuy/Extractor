package de.snowii.extractor.extractors.structures

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.levelgen.structure.StructureSet

class StructureSet : Extractor.Extractor {
    override fun fileName(): String {
        return "structure_set.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET)
        for (setting in registry) {
            finalJson.add(
                registry.getKey(setting)!!.path,
                StructureSet.DIRECT_CODEC.encodeStart(
                    JsonOps.INSTANCE,
                    setting
                ).getOrThrow()
            )
        }

        return finalJson
    }
}