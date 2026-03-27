package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.dialog.Dialog

class Dialog : Extractor.Extractor {
    override fun fileName(): String {
        return "dialog.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.DIALOG)
        for (dialog in registry) {
            val sub = Dialog.DIRECT_CODEC.encodeStart(
                JsonOps.INSTANCE, dialog
            ).getOrThrow() as JsonObject
            sub.addProperty("id", registry.getId(dialog))
            finalJson.add(
                registry.getId(dialog).toString(), sub
            )
        }
        return finalJson
    }
}