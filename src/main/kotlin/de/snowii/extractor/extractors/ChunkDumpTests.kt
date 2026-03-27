/* :cry:
package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import de.snowii.extractor.Extractor
import net.minecraft.SharedConstants
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ProtoChunk
import net.minecraft.world.level.chunk.UpgradeData
import net.minecraft.world.level.levelgen.*
import net.minecraft.world.level.levelgen.blending.Blender
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.biome.BiomeManager
import net.minecraft.world.level.biome.BiomeSource
import net.minecraft.world.level.levelgen.structure.StructureSet
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.system.exitProcess

class ChunkDumpTests {

    companion object {
        private fun createFluidLevelSampler(settings: NoiseGeneratorSettings): Aquifer.FluidPicker {
            val fluidLevel = Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState())
            val i = settings.seaLevel()
            val fluidLevel2 = Aquifer.FluidStatus(i, settings.defaultFluid())
            return Aquifer.FluidPicker { _, y, _ -> if (y < Math.min(-54, i)) fluidLevel else fluidLevel2 }
        }

        private fun getIndex(config: NoiseSettings, x: Int, y: Int, z: Int): Int {
            if (x < 0 || y < 0 || z < 0) {
                println("Bad local pos")
                exitProcess(1)
            }
            return config.height() * 16 * x + 16 * y + z
        }

        private fun populateNoise(
            settings: NoiseGeneratorSettings,
            chunkNoiseSampler: NoiseChunk,
            shapeConfig: NoiseSettings,
            chunk: ProtoChunk,
        ): ProtoChunk {
            var sampleBlockState: KFunction<BlockState?>? = null
            for (method: KFunction<*> in chunkNoiseSampler::class.declaredFunctions) {
                // In 26.1 (unobfuscated), the method is "getInterpolatedState"
                if (method.name == "getInterpolatedState") {
                    sampleBlockState = method as KFunction<BlockState?>
                }
            }

            val heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG)
            val heightmap2 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG)
            val chunkPos = chunk.pos
            val i = chunkPos.minBlockX
            val j = chunkPos.minBlockZ
            val aquiferSampler = chunkNoiseSampler.aquifer()
            chunkNoiseSampler.initializeForFirstCellX()
            val mutable = net.minecraft.core.BlockPos.MutableBlockPos()
            val k = shapeConfig.getCellWidth()
            val l = shapeConfig.getCellHeight()
            val m = 16 / k
            val n = 16 / k

            val cellHeight = shapeConfig.getCellCountY()
            val minimumCellY = shapeConfig.getMinCellY()

            for (o in 0..<m) {
                chunkNoiseSampler.advanceCellX(o)

                for (p in 0..<n) {
                    var q = chunk.sectionsCount - 1
                    var chunkSection = chunk.getSection(q)

                    for (r in cellHeight - 1 downTo 0) {
                        chunkNoiseSampler.selectCellYZ(r, p)

                        for (s in l - 1 downTo 0) {
                            val t = (minimumCellY + r) * l + s
                            val u = t and 15
                            val v = chunk.getSectionIndex(t)
                            if (q != v) {
                                q = v
                                chunkSection = chunk.getSection(v)
                            }

                            val d = s.toDouble() / l
                            chunkNoiseSampler.updateForY(t, d)

                            for (w in 0..<k) {
                                val x = i + o * k + w
                                val y = x and 15
                                val e = w.toDouble() / k
                                chunkNoiseSampler.updateForX(x, e)

                                for (z in 0..<k) {
                                    val aa = j + p * k + z
                                    val ab = aa and 15
                                    val f = z.toDouble() / k
                                    chunkNoiseSampler.updateForZ(aa, f)
                                    var blockState = sampleBlockState!!.call(chunkNoiseSampler)
                                    if (blockState == null) {
                                        blockState = settings.defaultBlock()
                                    }

                                    if (!blockState!!.isAir && !SharedConstants.isOutsideGenerationArea(
                                            chunk.pos
                                        )
                                    ) {
                                        chunkSection.setBlockState(y, u, ab, blockState, false)
                                        heightmap.update(y, t, ab, blockState)
                                        heightmap2.update(y, t, ab, blockState)
                                        if (aquiferSampler.shouldScheduleFluidUpdate() && !blockState.fluidState.isEmpty) {
                                            mutable.set(x, t, aa)
                                            chunk.markPosForPostprocessing(mutable)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                chunkNoiseSampler.swapSlices()
            }

            chunkNoiseSampler.stopInterpolation()
            return chunk
        }


        // This is basically just what NoiseBasedChunkGenerator is doing
        private fun dumpPopulateNoise(
            startX: Int,
            startZ: Int,
            sampler: NoiseChunk,
            config: NoiseSettings,
            settings: NoiseGeneratorSettings
        ): IntArray? {
            val result = IntArray(16 * 16 * config.height())

            for (method: KFunction<*> in sampler::class.declaredFunctions) {
                // In 26.1 (unobfuscated), the private method is "getInterpolatedState"
                if (method.name.equals("getInterpolatedState")) {
                    sampler.initializeForFirstCellX()
                    val k = config.getCellWidth()
                    val l = config.getCellHeight()

                    val m = 16 / k
                    val n = 16 / k

                    val cellHeight = config.getCellCountY()
                    val minimumCellY = config.getMinCellY()

                    for (o in 0..<m) {
                        sampler.advanceCellX(o)
                        for (p in 0..<n) {
                            for (r in (0..<cellHeight).reversed()) {
                                sampler.selectCellYZ(r, p)
                                for (s in (0..<l).reversed()) {
                                    val t = (minimumCellY + r) * l + s
                                    val d = s.toDouble() / l.toDouble()
                                    sampler.updateForY(t, d)
                                    for (w in 0..<k) {
                                        val x = startX + o * k + w
                                        val y = x and 15
                                        val e = w.toDouble() / k.toDouble()
                                        sampler.updateForX(x, e)
                                        for (z in 0..<k) {
                                            val aa = startZ + p * k + z
                                            val ab = aa and 15
                                            val f = z.toDouble() / k.toDouble()
                                            sampler.updateForZ(aa, f)
                                            var blockState = method.call(sampler) as BlockState?
                                            if (blockState == null) {
                                                blockState = settings.defaultBlock()
                                            }
                                            val index = this.getIndex(config, y, t - config.minY(), ab)
                                            result[index] = Block.getId(blockState)
                                        }
                                    }
                                }
                            }
                        }
                        sampler.swapSlices()
                    }
                    sampler.stopInterpolation()
                    return result
                }
            }
            System.err.println("No valid method found for block state sampler!")
            return null
        }

        class WrapperRemoverVisitor(private val wrappersToKeep: Iterable<String>) : DensityFunctionVisitor {
            override fun apply(densityFunction: DensityFunction?): DensityFunction {
                when (densityFunction) {
                    is Marker -> {
                        val name = densityFunction.type().toString()
                        if (wrappersToKeep.contains(name)) {
                            return densityFunction
                        }
                        return this.apply(densityFunction.wrapped())
                    }

                    is DensityFunctions.HolderHolder -> {
                        return this.apply(densityFunction.function().value())
                    }

                    else -> return densityFunction!!
                }
            }
        }

        class WrapperValidateVisitor(private val wrappersToKeep: Iterable<String>) : DensityFunctionVisitor {
            override fun apply(densityFunction: DensityFunction?): DensityFunction {
                when (densityFunction) {
                    is Marker -> {
                        val name = densityFunction.type().toString()
                        if (wrappersToKeep.contains(name)) {
                            return densityFunction
                        }
                        throw Exception(name + "is still in the function!")
                    }

                    is DensityFunctions.HolderHolder -> {
                        return this.apply(densityFunction.function().value())
                    }

                    else -> return densityFunction!!
                }
            }
        }

        // Available wrapper types in Mojang mappings:
        // Interpolated  -> Marker.Type.Interpolated
        // CacheOnce     -> Marker.Type.CacheOnce
        // FlatCache     -> Marker.Type.FlatCache
        // Cache2D       -> Marker.Type.Cache2D
        //
        // CellCache is only added inside the NoiseChunk itself so it cannot be removed
        private fun removeWrappers(config: RandomState, wrappersToKeep: Iterable<String>) {
            val noiseRouter = config.router.apply(WrapperRemoverVisitor(wrappersToKeep))
            for (field in config.javaClass.declaredFields) {
                if (field.name.equals("router")) {
                    field.trySetAccessible()
                    field.set(config, noiseRouter)
                    return
                }
            }
            throw Exception("Failed to replace router")
        }

        fun createMultiNoiseSampler(config: RandomState, sampler: NoiseChunk): net.minecraft.world.level.biome.Climate.Sampler {
            var createMultiNoiseSampler: Method? = null
            for (m: Method in sampler.javaClass.declaredMethods) {
                if (m.name == "cachedClimateSampler") {
                    m.trySetAccessible()
                    createMultiNoiseSampler = m
                    break
                }
            }

            val noiseSampler = createMultiNoiseSampler!!.invoke(
                sampler,
                config.router,
                listOf<net.minecraft.world.level.biome.Climate.ParameterPoint>()
            ) as net.minecraft.world.level.biome.Climate.Sampler

            return noiseSampler
        }
    }

    internal class SurfaceDump(
        private val filename: String,
        private val seed: Long,
        private val chunkX: Int,
        private val chunkZ: Int,
    ) : Extractor.Extractor {
        override fun fileName(): String = this.filename

        override fun extract(server: MinecraftServer): JsonElement {
            val biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME)

            val chunkPos = ChunkPos(this.chunkX, this.chunkZ)

            val lookup = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                net.minecraft.core.registries.BuiltInRegistries.REGISTRY
            )
            // In 26.1, BuiltinRegistries.createWrapperLookup() becomes
            // accessing through RegistryDataLoader / VanillaRegistries
            val wrapperLookup = net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE,
                server.registryAccess()
            ).let { server.registryAccess() }

            val ref = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD)
            val settings = ref.value()

            val noiseParams = server.registryAccess().lookupOrThrow(Registries.NOISE)
            val config = RandomState.create(settings, noiseParams, seed)

            val chunkGenerator = server.overworld().chunkSource.generator
            val biomeSource = chunkGenerator.biomeSource

            // Overworld shape config
            val shape = NoiseSettings.create(-64, 384, 1, 2)
            val testSampler =
                NoiseChunk.forChunk(
                    16 / shape.getCellWidth(),
                    config,
                    chunkPos.minBlockX,
                    chunkPos.minBlockZ,
                    shape,
                    object : DensityFunctions.BeardifierOrMarker {
                        override fun maxValue(): Double = 0.0
                        override fun minValue(): Double = 0.0
                        override fun compute(pos: FunctionContext): Double = 0.0
                        override fun fillArray(densities: DoubleArray, contextProvider: ContextProvider) {
                            densities.fill(0.0)
                        }
                    },
                    settings,
                    createFluidLevelSampler(settings),
                    Blender.empty()
                )

            val levelHeightAccessor = net.minecraft.world.level.LevelHeightAccessor.create(
                chunkGenerator.getMinY(),
                chunkGenerator.getGenDepth()
            )

            val chunk = ProtoChunk(
                chunkPos,
                UpgradeData.EMPTY,
                levelHeightAccessor,
                server.registryAccess().lookupOrThrow(Registries.BIOME),
                null
            )

            val biomeNoiseSampler = createMultiNoiseSampler(config, testSampler)
            chunk.fillBiomesFromNoise(biomeSource, biomeNoiseSampler)
            chunk.setStatus(ChunkStatus.BIOMES)

            populateNoise(settings, testSampler, shape, chunk)
            chunk.setStatus(ChunkStatus.NOISE)

            val biomeMixer = BiomeManager(chunk, BiomeManager.obfuscateSeed(seed))
            val heightContext = WorldGenerationContext(chunkGenerator, chunk)
            config.surfaceSystem.buildSurface(
                config,
                biomeMixer,
                server.registryAccess(),
                settings.useLegacyRandomSource(),
                heightContext,
                chunk,
                testSampler,
                settings.surfaceRule()
            )
            chunk.setStatus(ChunkStatus.SURFACE)

            val result = IntArray(16 * 16 * chunk.height)
            for (x in 0..15) {
                for (y in chunk.minBuildHeight..chunk.maxBuildHeight) {
                    for (z in 0..15) {
                        val pos = net.minecraft.core.BlockPos(x, y, z)
                        val blockState = chunk.getBlockState(pos)
                        val index = getIndex(shape, x, y - chunk.minBuildHeight, z)
                        result[index] = Block.getId(blockState)
                    }
                }
            }

            val topLevelJson = JsonArray()
            result.forEach { state ->
                topLevelJson.add(state)
            }
            return topLevelJson
        }
    }

    internal class NoiseDump(
        private val filename: String,
        private val seed: Long,
        private val chunkX: Int,
        private val chunkZ: Int,
        private val allowedWrappers: Iterable<String>
    ) : Extractor.Extractor {
        override fun fileName(): String = this.filename

        // Dumps a chunk to an array of block state ids
        override fun extract(server: MinecraftServer): JsonElement {
            val topLevelJson = JsonArray()
            val chunkPos = ChunkPos(this.chunkX, this.chunkZ)

            val ref = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD)
            val settings = ref.value()

            val noiseParams = server.registryAccess().lookupOrThrow(Registries.NOISE)
            val config = RandomState.create(settings, noiseParams, seed)

            // Always have cellcache wrappers
            removeWrappers(config, this.allowedWrappers)
            config.router.apply(WrapperValidateVisitor(this.allowedWrappers))

            // Overworld shape config
            val shape = NoiseSettings.create(-64, 384, 1, 2)
            val testSampler =
                NoiseChunk.forChunk(
                    16 / shape.getCellWidth(),
                    config,
                    chunkPos.minBlockX,
                    chunkPos.minBlockZ,
                    shape,
                    object : DensityFunctions.BeardifierOrMarker {
                        override fun maxValue(): Double = 0.0
                        override fun minValue(): Double = 0.0
                        override fun compute(pos: FunctionContext): Double = 0.0
                        override fun fillArray(densities: DoubleArray, contextProvider: ContextProvider) {
                            densities.fill(0.0)
                        }
                    },
                    settings,
                    createFluidLevelSampler(settings),
                    Blender.empty()
                )

            val data = dumpPopulateNoise(chunkPos.minBlockX, chunkPos.minBlockZ, testSampler, shape, settings)
            data?.forEach { state ->
                topLevelJson.add(state)
            }

            return topLevelJson
        }
    }
}*/
