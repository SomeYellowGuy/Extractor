package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.TagNetworkSerialization


class Tags : Extractor.Extractor {
    override fun fileName(): String {
        return "tags.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val tagsJson = JsonObject()

        val layeredRegistries = server.registries()
        val registryAccess = layeredRegistries.compositeAccess()
        val tags = TagNetworkSerialization.serializeTagsToNetwork(layeredRegistries)

        for ((registryKey, networkPayload) in tags) {
            val tagGroupTagsJson = JsonObject()
            val registry = registryAccess.lookupOrThrow(registryKey)
            val resolvedTags = networkPayload.resolve(registry)

            for ((tagKey, holders) in resolvedTags.tags) {
                val tagGroupTagsJsonArray = JsonArray()
                for (holder in holders) {
                    tagGroupTagsJsonArray.add(
                        holder.unwrapKey().orElseThrow().identifier().path
                    )
                }
                tagGroupTagsJson.add(tagKey.location().toString(), tagGroupTagsJsonArray)
            }
            tagsJson.add(registryKey.identifier().path, tagGroupTagsJson)
        }

        return tagsJson
    }
}