package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.ChatType
import net.minecraft.server.MinecraftServer

class MessageType : Extractor.Extractor {
    override fun fileName(): String {
        return "message_type.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val messagesJson = JsonObject()
        val registryAccess = server.registries().compositeAccess()
        val messageTypeRegistry = registryAccess.lookupOrThrow(Registries.CHAT_TYPE)
        val ops = registryAccess.createSerializationContext(JsonOps.INSTANCE)

        messageTypeRegistry.asHolderIdMap().forEach { entry ->
            val json = JsonObject()
            json.addProperty("id", messageTypeRegistry.getId(entry.value()))
            json.add(
                "components",
                ChatType.DIRECT_CODEC.encodeStart(ops, entry.value()).orThrow
            )
            messagesJson.add(entry.unwrapKey().get().identifier().path, json)
        }

        return messagesJson
    }
}