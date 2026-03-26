package de.snowii.extractor.extractors.non_registry

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class RecipeRemainder : Extractor.Extractor  {
    override fun fileName(): String {
        return "recipe_remainder.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val recipeRemainderJson = JsonObject()

        for (item in BuiltInRegistries.ITEM.stream()) {
            val realItem: Item = item
            val remainder = realItem.craftingRemainder;
            if (remainder == ItemStack.EMPTY) {
                continue
            }


            recipeRemainderJson.add(
                BuiltInRegistries.ITEM.getId(realItem).toString(),
                JsonPrimitive(BuiltInRegistries.ITEM.getId(remainder.item)),
            )

        }
        return recipeRemainderJson
    }
}