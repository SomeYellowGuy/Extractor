package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer

class DialogType : Extractor.Extractor {
    override fun fileName(): String {
        return "dialog_type.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val dialogTypeJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.DIALOG_TYPE)

        for (dialogType in registry.stream()) {
            val id = registry.getId(dialogType)
            dialogTypeJson.addProperty(id.toString(), registry.getId(dialogType))
        }

        return dialogTypeJson
    }
}