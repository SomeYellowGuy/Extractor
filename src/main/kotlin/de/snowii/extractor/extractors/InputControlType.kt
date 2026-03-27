package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer

class InputControlType : Extractor.Extractor {
    override fun fileName(): String {
        return "input_control_type.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val InputControlTypeJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.INPUT_CONTROL_TYPE)

        for (inputControlType in registry.stream()) {
            val id = registry.getId(inputControlType)
            InputControlTypeJson.addProperty(id.toString(), registry.getId(inputControlType))
        }

        return InputControlTypeJson
    }
}