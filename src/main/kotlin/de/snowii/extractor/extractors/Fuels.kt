package de.snowii.extractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import de.snowii.extractor.Extractor
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack


class Fuels : Extractor.Extractor {
    override fun fileName(): String {
        return "fuels.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val fuelsJson = JsonObject()
        server.fuelValues().fuelItems().forEach { fuel ->
            fuelsJson.add(Item.getId(fuel).toString(), JsonPrimitive(server.fuelValues().burnDuration(ItemStack(fuel))))
        }
        return fuelsJson
    }
}