package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.advancements.Advancement
import net.minecraft.server.MinecraftServer

class Advancement : Extractor.Extractor {
    override fun fileName(): String {
        return "advancements.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()

        val ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE)

        val advancementEntries = server.advancements.allAdvancements
        for (advancement in advancementEntries) {
            val sub = Advancement.CODEC.encodeStart(
                ops,
                advancement.value
            ).getOrThrow() as JsonObject

            finalJson.add(
                advancement.id.toString(),
                sub
            )
        }
        return finalJson
    }
}