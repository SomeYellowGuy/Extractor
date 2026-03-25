package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.storage.loot.LootTable

class Entities : Extractor.Extractor {
    override fun fileName(): String {
        return "entities.json"
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val entitiesJson = JsonObject()
        for (entityType in BuiltInRegistries.ENTITY_TYPE) {
            val entityJson = JsonObject()
            entityJson.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getId(entityType))
            val entity = entityType.create(server.overworld!!, EntitySpawnReason.NATURAL)
            if (entity != null) {
                if (entity is LivingEntity) {
                    entityJson.addProperty("max_health", entity.maxHealth)
                }
                entityJson.addProperty("attackable", entity.isAttackable)
                entityJson.addProperty("mob", entity is MobEntity)
                entityJson.addProperty("limit_per_chunk", (entity as? MobEntity)?.limitPerChunk?: 0)
            }
            entityJson.addProperty("summonable", entityType.canSummon())
            entityJson.addProperty("saveable", entityType.canSerialize())
            entityJson.addProperty("fire_immune", entityType.fireImmune())
            entityJson.addProperty("category", entityType.spawnGroup.name)
            entityJson.addProperty("can_spawn_far_from_player", entityType.canSpawnFarFromPlayer())
            val dimension = JsonArray()
            dimension.add(entityType.width)
            dimension.add(entityType.height)
            entityJson.add("dimension", dimension)
            entityJson.addProperty("eye_height", entityType.dimensions.eyeHeight)
            if (entityType.defaultLootTable.isPresent) {
                val table = server.reloadableRegistries()
                    .getLootTable(entityType.defaultLootTable.get());
                entityJson.add(
                    "loot_table", LootTable::CODEC.get().encodeStart(
                        JsonOps.INSTANCE,
                        table
                    ).getOrThrow()
                )
            }
            val spawnRestriction = JsonObject()
            val location = SpawnRestriction.getLocation(entityType)
            val locationName = when (location) {
                SpawnLocationTypes::IN_LAVA.get() -> {
                    "IN_LAVA"
                }

                SpawnLocationTypes::IN_WATER.get() -> {
                    "IN_WATER"
                }

                SpawnLocationTypes::ON_GROUND.get() -> {
                    "ON_GROUND"
                }

                SpawnLocationTypes::UNRESTRICTED.get() -> {
                    "UNRESTRICTED"
                }

                else -> {
                    ""
                }
            }

            spawnRestriction.addProperty("location", locationName)
            spawnRestriction.addProperty("heightmap", SpawnRestriction.getHeightmapType(entityType).toString())
            entityJson.add("spawn_restriction", spawnRestriction)

            entitiesJson.add(
                BuiltInRegistries.ENTITY_TYPE.getKey(entityType).path, entityJson
            )
        }

        return entitiesJson
    }
}
