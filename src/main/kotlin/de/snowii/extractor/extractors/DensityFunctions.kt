package de.snowii.extractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import de.snowii.extractor.Extractor
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.CubicSpline
import net.minecraft.world.level.levelgen.DensityFunction
import net.minecraft.world.level.levelgen.DensityFunctions
import net.minecraft.world.level.levelgen.NoiseRouter
import net.minecraft.world.level.levelgen.synth.NormalNoise
import net.minecraft.world.level.levelgen.synth.PerlinNoise
import net.minecraft.world.level.levelgen.synth.SimplexNoise

class DensityFunctions : Extractor.Extractor {
    override fun fileName(): String = "density_function.json"

    private fun serializeSpline(spline: CubicSpline<*, *>): JsonObject {
        val obj = JsonObject()

        when (spline) {
            is CubicSpline.Multipoint<*, *> -> {
                obj.add("_type", JsonPrimitive("standard"))

                val value = JsonObject()
                @Suppress("UNCHECKED_CAST")
                val functionWrapper =
                    spline.coordinate() as DensityFunctions.Spline.Coordinate
                value.add("locationFunction", serializeFunction(functionWrapper.function().value()))

                val locationArr = JsonArray()
                for (location in spline.locations()) {
                    locationArr.add(JsonPrimitive(location))
                }
                value.add("locations", locationArr)

                val valueArr = JsonArray()
                for (splineValue in spline.values()) {
                    valueArr.add(serializeSpline(splineValue))
                }
                value.add("values", valueArr)

                val derivativeArr = JsonArray()
                for (derivative in spline.derivatives()) {
                    derivativeArr.add(JsonPrimitive(derivative))
                }
                value.add("derivatives", derivativeArr)

                obj.add("value", value)
            }

            is CubicSpline.Constant<*, *> -> {
                obj.add("_type", JsonPrimitive("fixed"))

                val value = JsonObject()
                value.add("value", JsonPrimitive(spline.value()))
                obj.add("value", value)
            }

            else -> throw Exception("Unknown spline: $spline (${spline.javaClass})")
        }

        return obj
    }

    private fun serializeValue(name: String, obj: Any, parent: String): JsonElement {
        return when (obj) {
            is DensityFunction -> serializeFunction(obj)
            is CubicSpline<*, *> -> serializeSpline(obj)
            is Int -> JsonPrimitive(obj)
            is Float -> JsonPrimitive(obj)
            is Double -> JsonPrimitive(obj)
            is Boolean -> JsonPrimitive(obj)
            is String -> JsonPrimitive(obj)
            is Char -> JsonPrimitive(obj)
            is Enum<*> -> JsonPrimitive(obj.name)
            else -> throw Exception("Unknown value to serialize: $obj ($name) from $parent")
        }
    }

    private fun serializeFunction(function: DensityFunction): JsonObject {
        val obj = JsonObject()

        // Unwrap registry holder transparently
        if (function is DensityFunctions.HolderHolder) {
            return serializeFunction(function.function().value())
        }

        obj.add("_class", JsonPrimitive(function.javaClass.simpleName))

        val value = JsonObject()
        for (field in function.javaClass.declaredFields) {
            if (field.name.first().isUpperCase()) {
                continue
            }

            if (function.javaClass.simpleName == "BlendDensity") {
                if (field.name == "maxValue" || field.name == "minValue") continue
            }

            if (function is DensityFunctions.Spline) {
                value.add("minValue", JsonPrimitive(function.minValue()))
                value.add("maxValue", JsonPrimitive(function.maxValue()))
            }

            if (field.name.startsWith("field_")) {
                continue
            }

            field.trySetAccessible()
            val fieldValue = field.get(function)
            when (fieldValue) {
                is SimplexNoise -> continue
                is PerlinNoise -> continue
                is NormalNoise -> continue
                null -> continue
            }

            val serialized = serializeValue(field.name, fieldValue, function.javaClass.simpleName)
            value.add(field.name, serialized)
        }

        if (!value.isEmpty) {
            obj.add("value", value)
        }

        return obj
    }

    private fun serializeRouter(router: NoiseRouter): JsonObject {
        val obj = JsonObject()

        for (field in router.javaClass.declaredFields) {
            if (field.name.first().isUpperCase()) {
                continue
            }
            field.trySetAccessible()
            val function = field.get(router)
            val serialized = serializeFunction(function as DensityFunction)
            obj.add(field.name, serialized)
        }

        return obj
    }

    override fun extract(server: MinecraftServer): JsonElement {
        val topLevelJson = JsonObject()

        val registryAccess = server.registries().compositeAccess()
        val noiseSettingsLookup = registryAccess.lookupOrThrow(Registries.NOISE_SETTINGS)

        noiseSettingsLookup.listElements().forEach { entry ->
            val settings = entry.value()
            val obj = serializeRouter(settings.noiseRouter())
            topLevelJson.add(entry.key().identifier().path, obj)
        }

        return topLevelJson
    }

    inner class Tests : Extractor.Extractor {
        override fun fileName(): String = "density_function_tests.json"

        override fun extract(server: MinecraftServer): JsonElement {
            val topLevelJson = JsonObject()

            val functionNames = arrayOf(
                "overworld/base_3d_noise",
                "overworld/caves/entrances",
                "overworld/caves/noodle",
                "overworld/caves/pillars",
                "overworld/caves/spaghetti_2d",
                "overworld/caves/spaghetti_2d_thickness_modulator",
                "overworld/caves/spaghetti_roughness_function",
                "overworld/offset",
                "overworld/depth",
                "overworld/factor",
                "overworld/sloped_cheese"
            )

            val registryAccess = server.registries().compositeAccess()
            val functionLookup = registryAccess.lookupOrThrow(Registries.DENSITY_FUNCTION)

            for (functionName in functionNames) {
                val functionKey = ResourceKey.create(
                    Registries.DENSITY_FUNCTION,
                    Identifier.withDefaultNamespace(functionName)
                )
                val function = functionLookup.getOrThrow(functionKey).value()
                topLevelJson.add(functionName, serializeFunction(function))
            }

            return topLevelJson
        }
    }
}