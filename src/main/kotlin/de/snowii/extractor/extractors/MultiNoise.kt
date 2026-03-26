package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.snowii.extractor.Extractor
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Climate
import net.minecraft.world.level.biome.MultiNoiseBiomeSource
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings
import net.minecraft.world.level.levelgen.RandomState
import java.lang.reflect.Field
import java.lang.reflect.Method

class MultiNoise : Extractor.Extractor {

    override fun fileName(): String {
        return "multi_noise_biome_tree.json"
    }

    private fun extractTreeNode(node: Any?): JsonObject {
        val json = JsonObject()
        for (f: Field in node!!::class.java.fields) {
            if (f.name == "parameters") {
                f.trySetAccessible()
                val parameters = JsonArray()
                @Suppress("UNCHECKED_CAST")
                val ranges = f.get(node) as Array<Climate.Parameter>
                for (range in ranges) {
                    val parameter = JsonObject()
                    parameter.addProperty("min", range.min())
                    parameter.addProperty("max", range.max())
                    parameters.add(parameter)
                }
                json.add("parameters", parameters)
            }
            if (f.name == "subTree") {
                f.trySetAccessible()
                val subTree = JsonArray()
                val nodes = f.get(node) as Array<Any>
                for (childNode in nodes) {
                    subTree.add(extractTreeNode(childNode))
                }
                json.add("subTree", subTree)
                json.addProperty("_type", "branch")
            }
            if (f.name == "value") {
                f.trySetAccessible()
                @Suppress("UNCHECKED_CAST")
                val value = f.get(node) as Holder<Biome>
                json.addProperty("biome", value.unwrapKey().orElseThrow().identifier().toString())
                json.addProperty("_type", "leaf")
            }
        }
        return json
    }

    private fun extractSearchTree(tree: Any?): JsonObject {
        var field: Field? = null
        for (f: Field in tree!!::class.java.declaredFields) {
            if (f.name == "firstNode") {
                f.trySetAccessible()
                field = f
                break
            }
        }
        return extractTreeNode(field!!.get(tree))
    }

    private fun getBiomeEntries(biomeSource: MultiNoiseBiomeSource): Climate.ParameterList<Holder<Biome>> {
        var method: Method? = null
        for (m: Method in biomeSource::class.java.declaredMethods) {
            if (m.name == "parameters") {
                m.trySetAccessible()
                method = m
                break
            }
        }
        @Suppress("UNCHECKED_CAST")
        return method!!.invoke(biomeSource) as Climate.ParameterList<Holder<Biome>>
    }

    private fun getTree(entries: Climate.ParameterList<*>): Any? {
        var field: Field? = null
        for (f: Field in entries::class.java.declaredFields) {
            if (f.name == "tree") {
                f.trySetAccessible()
                field = f
                break
            }
        }
        return field!!.get(entries)
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val registryAccess = server.registries().compositeAccess()
        val multiNoiseRegistry = registryAccess.lookupOrThrow(
            Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST
        )

        val overworldBiomeSource = MultiNoiseBiomeSource.createFromPreset(
            multiNoiseRegistry.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
        )
        val overworldEntries = getBiomeEntries(overworldBiomeSource)
        val overworld = extractSearchTree(getTree(overworldEntries))

        val netherBiomeSource = MultiNoiseBiomeSource.createFromPreset(
            multiNoiseRegistry.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER)
        )
        val netherEntries = getBiomeEntries(netherBiomeSource)
        val nether = extractSearchTree(getTree(netherEntries))

        val returnValue = JsonObject()
        returnValue.add("overworld", overworld)
        returnValue.add("nether", nether)
        return returnValue
    }

    inner class Sample : Extractor.Extractor {
        override fun fileName(): String {
            return "multi_noise_sample_no_blend_no_beard_0_0_0.json"
        }

        override fun extract(server: MinecraftServer): JsonElement {
            val rootJson = JsonArray()
            val seed = 0L

            val registryAccess = server.registries().compositeAccess()
            val noiseSettings = registryAccess.lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD)
            val noiseParams = registryAccess.lookupOrThrow(Registries.NOISE)

            val randomState = RandomState.create(
                noiseSettings.value(),
                noiseParams,
                seed
            )

            val sampler: Climate.Sampler = randomState.sampler()

            for (x in 0..15) {
                for (y in -64..319) {
                    for (z in 0..15) {
                        val result: Climate.TargetPoint = sampler.sample(x, y, z)

                        val valueArr = JsonArray()
                        valueArr.add(x)
                        valueArr.add(y)
                        valueArr.add(z)
                        valueArr.add(result.temperature())
                        valueArr.add(result.humidity())
                        valueArr.add(result.continentalness())
                        valueArr.add(result.erosion())
                        valueArr.add(result.depth())
                        valueArr.add(result.weirdness())

                        rootJson.add(valueArr)
                    }
                }
            }

            return rootJson
        }
    }
}