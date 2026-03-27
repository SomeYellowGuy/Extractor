package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer

class JukeboxSong : Extractor.Extractor {
    override fun fileName(): String {
        return "jukebox_song.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val finalJson = JsonObject()
        val registry =
            server.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG)
        for (setting in registry) {
            finalJson.addProperty(
                registry.getKey(setting)!!.path,
                registry.getId(setting)
            )
        }

        return finalJson
    }
}