package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import de.snowii.extractor.Extractor
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DropExperienceBlock
import net.minecraft.world.level.block.FireBlock
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.phys.AABB
import java.util.*

class Blocks : Extractor.Extractor {

    companion object {
        private const val AIR               : Int = 1 shl 0
        private const val BURNABLE          : Int = 1 shl 1
        private const val TOOL_REQUIRED     : Int = 1 shl 2
        private const val SIDED_TRANSPARENCY: Int = 1 shl 3
        private const val REPLACEABLE       : Int = 1 shl 4
        private const val IS_LIQUID         : Int = 1 shl 5
        private const val IS_SOLID          : Int = 1 shl 6
        private const val IS_FULL_CUBE      : Int = 1 shl 7
        private const val IS_SOLID_BLOCK    : Int = 1 shl 8
        private const val HAS_RANDOM_TICKS  : Int = 1 shl 9

        private const val DOWN_SIDE_SOLID   : Int = 1 shl 0;
        private const val UP_SIDE_SOLID     : Int = 1 shl 1;
        private const val NORTH_SIDE_SOLID  : Int = 1 shl 2;
        private const val SOUTH_SIDE_SOLID  : Int = 1 shl 3;
        private const val WEST_SIDE_SOLID   : Int = 1 shl 4;
        private const val EAST_SIDE_SOLID   : Int = 1 shl 5;
        private const val DOWN_CENTER_SOLID : Int = 1 shl 6;
        private const val UP_CENTER_SOLID   : Int = 1 shl 7;
    }

    override fun fileName(): String {
        return "blocks.json"
    }

    private fun getFlammableData(): Map<Block, Pair<Int, Int>> {
        val flammableData = mutableMapOf<Block, Pair<Int, Int>>()
        val fireBlock = Blocks.FIRE as FireBlock;
        for (block in BuiltInRegistries.BLOCK) {
            val defaultState = block.defaultBlockState()
            val spreadChance = fireBlock.getIgniteOdds(defaultState)
            val burnChance = fireBlock.getBurnOdds(defaultState)
            if (spreadChance > 0 || burnChance > 0) {
                flammableData[block] = Pair(spreadChance, burnChance)
            }
        }

        return flammableData
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val topLevelJson = JsonObject()

        val blocksJson = JsonArray()

        val shapes: LinkedHashMap<AABB, Int> = LinkedHashMap()

        val flammableData = getFlammableData()

        for (block in BuiltInRegistries.BLOCK) {
            val blockJson = JsonObject()
            blockJson.addProperty("id", BuiltInRegistries.BLOCK.getId(block))
            blockJson.addProperty("name", BuiltInRegistries.BLOCK.getKey(block).path)
            blockJson.addProperty("translation_key", block.descriptionId)
            blockJson.addProperty("slipperiness", block .friction)
            blockJson.addProperty("velocity_multiplier", block.speedFactor)
            blockJson.addProperty("jump_velocity_multiplier", block.jumpFactor)
            blockJson.addProperty("hardness", block.defaultBlockState().getDestroySpeed())
            blockJson.addProperty("blast_resistance", block.blastResistance)
            blockJson.addProperty("item_id", BuiltInRegistries.ITEM.getId(block.asItem()))

            // Add flammable data if this block is flammable
            flammableData[block]?.let { (spreadChance, burnChance) ->
                val flammableJson = JsonObject()
                flammableJson.addProperty("spread_chance", spreadChance)
                flammableJson.addProperty("burn_chance", burnChance)
                blockJson.add("flammable", flammableJson)
            }
            if (block is DropExperienceBlock) {
                blockJson.add(
                    "experience", DropExperienceBlock.CODEC.codec().encodeStart(
                        JsonOps.INSTANCE,
                        block,
                    ).getOrThrow()
                )
            }
            if (block.lootTable.isPresent) {
                val table = server.reloadableRegistries()
                    .getLootTable(block.lootTable.get())
                blockJson.add(
                    "loot_table", LootTable::CODEC.get().encodeStart(
                        JsonOps.INSTANCE,
                        table as Holder<LootTable>?
                    ).getOrThrow()
                )
            }
            val propsJson = JsonArray()
            for (prop in block.stateDefinition.properties) {
                // Use the hashcode to map to a property later; the property names are not unique
                propsJson.add(prop.hashCode())
            }
            blockJson.add("properties", propsJson)

            val statesJson = JsonArray()
            for (state in block.stateDefinition.states) {
                val stateJson = JsonObject()
                var stateFlags = 0
                var sideFlags = 0
                
                if (state.isAir) stateFlags = stateFlags or AIR
                if (state.isBurnable) stateFlags = stateFlags or BURNABLE
                if (state.isToolRequired) stateFlags = stateFlags or TOOL_REQUIRED
                if (state.hasSidedTransparency()) stateFlags = stateFlags or SIDED_TRANSPARENCY
                if (state.isReplaceable) stateFlags = stateFlags or REPLACEABLE
                if (state.liquid()) stateFlags = stateFlags or IS_LIQUID
                if (state.isSolid) stateFlags = stateFlags or IS_SOLID
                if (state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) stateFlags = stateFlags or IS_FULL_CUBE
                if (state.isSolidBlock(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) stateFlags = stateFlags or IS_SOLID_BLOCK
                if (state.hasRandomTicks()) stateFlags = stateFlags or HAS_RANDOM_TICKS


                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.DOWN)) sideFlags = sideFlags or DOWN_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP)) sideFlags = sideFlags or UP_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.NORTH)) sideFlags = sideFlags or NORTH_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.SOUTH)) sideFlags = sideFlags or SOUTH_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.WEST)) sideFlags = sideFlags or WEST_SIDE_SOLID
                if (state.isSideSolidFullSquare(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.EAST)) sideFlags = sideFlags or EAST_SIDE_SOLID
                if (state.isSideSolid(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.DOWN, SideShapeType.CENTER)) sideFlags = sideFlags or DOWN_CENTER_SOLID
                if (state.isSideSolid(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, Direction.UP, SideShapeType.CENTER)) sideFlags = sideFlags or UP_CENTER_SOLID
                
                stateJson.addProperty("id", Block.getId(state))
                stateJson.addProperty("state_flags", stateFlags and 0xFFFF)
                stateJson.addProperty("side_flags", sideFlags and 0xFF)
                stateJson.addProperty("instrument", state.instrument.name)
                stateJson.addProperty("luminance", state.luminance)
                stateJson.addProperty("piston_behavior", state.pistonBehavior.name)
                stateJson.addProperty("hardness", state.getHardness(null, null))

                    stateJson.addProperty("opacity", state.opacity)

                if (block.defaultBlockState() == state) {
                    blockJson.addProperty("default_state_id", Block.getId(state))
                }

                val collisionShapeIdxsJson = JsonArray()
                for (box in state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs()) {
                    val idx = shapes.putIfAbsent(box, shapes.size)
                    collisionShapeIdxsJson.add(Objects.requireNonNullElseGet(idx) { shapes.size - 1 })
                }

                stateJson.add("collision_shapes", collisionShapeIdxsJson)

                val outlineShapeIdxsJson = JsonArray()
                for (box in state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs()) {
                    val idx = shapes.putIfAbsent(box, shapes.size)
                    outlineShapeIdxsJson.add(Objects.requireNonNullElseGet(idx) { shapes.size - 1 })
                }

                stateJson.add("outline_shapes", outlineShapeIdxsJson)

                for (blockEntity in BuiltInRegistries.BLOCK_ENTITY_TYPE) {
                    if (blockEntity.isValid(state)) {
                        stateJson.addProperty("block_entity_type", BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(blockEntity))
                    }
                }

                statesJson.add(stateJson)
            }
            blockJson.add("states", statesJson)

            blocksJson.add(blockJson)
        }

        val blockEntitiesJson = JsonArray()
        for (blockEntity in BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            blockEntitiesJson.add(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity)!!.path)
        }

        val shapesJson = JsonArray()
        for (shape in shapes.keys) {
            val shapeJson = JsonObject()
            val min = JsonArray()
            min.add(shape.minX)
            min.add(shape.minY)
            min.add(shape.minZ)
            val max = JsonArray()
            max.add(shape.maxX)
            max.add(shape.maxY)
            max.add(shape.maxZ)
            shapeJson.add("min", min)
            shapeJson.add("max", max)
            shapesJson.add(shapeJson)
        }

        topLevelJson.add("block_entity_types", blockEntitiesJson)
        topLevelJson.add("shapes", shapesJson)
        topLevelJson.add("blocks", blocksJson)

        return topLevelJson
    }
}
