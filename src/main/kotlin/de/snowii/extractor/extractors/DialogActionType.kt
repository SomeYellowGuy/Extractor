package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer

class DialogActionType : Extractor.Extractor {
    override fun fileName(): String {
        return "dialog_action_type.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val dialogActionJson = JsonObject()
        val registry =
            server.registryAccess().getOrThrow(Registries.DIALOG_ACTION_TYPE).value()

        for (dialogType in registry.stream()) {
            val id = registry.getId(dialogType)
            dialogActionJson.addProperty(id.toString(), registry.getId(dialogType))
        }

        return dialogActionJson
    }
}