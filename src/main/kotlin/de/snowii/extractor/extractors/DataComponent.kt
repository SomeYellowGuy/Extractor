package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer


class DataComponent : Extractor.Extractor {
    override fun fileName(): String {
        return "data_component.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val dataComponentJson = JsonObject()
        val list = BuiltInRegistries.DATA_COMPONENT_TYPE.stream();
        for (item in list) {
            dataComponentJson.addProperty(item.toString(), BuiltInRegistries.DATA_COMPONENT_TYPE.getId(item));
        }
        return dataComponentJson
    }
}
