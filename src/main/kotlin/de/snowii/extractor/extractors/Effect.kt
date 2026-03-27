package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffect
import java.util.Optional

class Effect : Extractor.Extractor {
    override fun fileName(): String {
        return "effect.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val json = JsonObject()
        val registryAccess = server.registries().compositeAccess()

        val attributeRegistry = registryAccess.lookupOrThrow(Registries.MOB_EFFECT)

        for (effect in BuiltInRegistries.MOB_EFFECT) {
            val itemJson = JsonObject()

            itemJson.addProperty("id", BuiltInRegistries.MOB_EFFECT.getId(effect))
            itemJson.addProperty("category", effect.category.name)
            itemJson.addProperty("color", effect.color)

            if (effect.blendInDurationTicks != 0 || effect.blendOutDurationTicks != 0 || effect.blendOutAdvanceTicks != 0) {
                itemJson.addProperty("fade_in_ticks", effect.blendInDurationTicks)
                itemJson.addProperty("fade_out_ticks", effect.blendOutDurationTicks)
                itemJson.addProperty("fade_out_threshold_ticks", effect.blendOutAdvanceTicks)
            }

            itemJson.addProperty("translation_key", effect.descriptionId)

            // applySound field via reflection
            val applySoundField = MobEffect::class.java.getDeclaredField("applySound")
            applySoundField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val applySound = applySoundField.get(effect) as? Optional<SoundEvent>
            applySound?.ifPresent { soundEvent ->
                itemJson.addProperty(
                    "apply_sound",
                    BuiltInRegistries.SOUND_EVENT.getKey(soundEvent)!!.path
                )
            }

            val attributeModifiersJson = JsonArray()
            effect.createModifiers(0) { attributeHolder, modifier ->
                val modJson = JsonObject()
                modJson.addProperty(
                    "attribute",
                    attributeHolder.unwrapKey().get().identifier().path
                )
                modJson.addProperty("operation", modifier.operation().toString())
                modJson.addProperty("id", modifier.id().toString())
                modJson.addProperty("baseValue", modifier.amount())
                attributeModifiersJson.add(modJson)
            }
            itemJson.add("attribute_modifiers", attributeModifiersJson)

            json.add(attributeRegistry.getKey(effect)!!.path, itemJson)
        }

        return json
    }
}