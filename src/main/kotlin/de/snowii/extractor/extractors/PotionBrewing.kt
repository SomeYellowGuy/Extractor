package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.Holder
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.crafting.Ingredient

class PotionBrewing : Extractor.Extractor {
    override fun fileName(): String {
        return "potion_brewing.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        val reg = server.potionBrewing()

        // potionTypes
        val t = reg.javaClass.getDeclaredField("potionTypes")
        t.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val potionTypes = t.get(reg) as List<Ingredient>
        val types = JsonArray()
        for (type in potionTypes) {
            val items = JsonArray()
            for (item in type.items()) {
                items.add(item.registeredName)
            }
            types.add(items)
        }
        json.add("potion_types", types)

        // potionRecipes
        val t2 = reg.javaClass.getDeclaredField("potionRecipes")
        t2.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val potionRecipes = t2.get(reg) as List<*>
        val recipes = JsonArray()
        for (recipe in potionRecipes) {
            val recipeJson = JsonObject()
            recipe?.javaClass?.let { clazz ->
                for (field in clazz.declaredFields) {
                    field.isAccessible = true
                    val value = field.get(recipe)
                    when (value) {
                        is Holder<*> -> recipeJson.addProperty(
                            field.name,
                            value.unwrapKey().orElseThrow().identifier().toString()
                        )
                        is Ingredient -> {
                            val tags = JsonArray()
                            for (item in value.items()) {
                                tags.add(item.registeredName)
                            }
                            recipeJson.add(field.name, tags)
                        }
                    }
                }
            }
            recipes.add(recipeJson)
        }
        json.add("potion_recipes", recipes)

        // itemRecipes
        val t3 = reg.javaClass.getDeclaredField("itemRecipes")
        t3.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val itemRecipes = t3.get(reg) as List<*>
        val recipes2 = JsonArray()
        for (recipe in itemRecipes) {
            val recipeJson = JsonObject()
            recipe?.javaClass?.let { clazz ->
                for (field in clazz.declaredFields) {
                    field.isAccessible = true
                    val value = field.get(recipe)
                    when (value) {
                        is Holder<*> -> recipeJson.addProperty(
                            field.name,
                            value.unwrapKey().orElseThrow().identifier().toString()
                        )
                        is Ingredient -> {
                            val tags = JsonArray()
                            for (item in value.items()) {
                                tags.add(item.registeredName)
                            }
                            recipeJson.add(field.name, tags)
                        }
                    }
                }
            }
            recipes2.add(recipeJson)
        }
        json.add("item_recipes", recipes2)

        return json
    }
}