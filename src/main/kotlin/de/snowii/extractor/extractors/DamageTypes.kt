package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.damagesource.DamageType

class DamageTypes : Extractor.Extractor {
    override fun fileName(): String {
        return "damage_type.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val damageTypesJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
        for (type in registry) {
            val json = JsonObject()
            json.addProperty("id", registry.getId(type))
            json.add(
                "components",
                DamageType.DIRECT_CODEC
                    .encodeStart(
                        JsonOps.INSTANCE,
                        type
                    )
                    .getOrThrow()
            )
            damageTypesJson.add(registry.getKey(type)!!.path, json)
        }

        return damageTypesJson
    }
}
