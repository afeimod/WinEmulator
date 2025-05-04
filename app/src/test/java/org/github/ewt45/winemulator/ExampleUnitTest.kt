package org.github.ewt45.winemulator

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    class PrefValueSerializer : KSerializer<Any> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

        private val setSerializer = SetSerializer(String.serializer())

        override fun serialize(encoder: Encoder, value: Any) {
            when (value) {
                is Boolean -> encoder.encodeBoolean(value)
                is String -> encoder.encodeString(value)
                is Int -> encoder.encodeInt(value)
                is Float -> encoder.encodeFloat(value)
                is Long -> encoder.encodeLong(value)
                is Double -> encoder.encodeDouble(value)
                is Set<*> -> {
                    if (value.first()?.takeIf { it is String } != null)
                        encoder.encodeSerializableValue(setSerializer, value as Set<String>)
                }
                else -> Unit //throw IllegalArgumentException("Unsupported type for serialization: ${value::class}")
            }
        }

        override fun deserialize(decoder: Decoder): Any{
            return when (val el = (decoder as JsonDecoder).decodeJsonElement()) {
                is JsonPrimitive -> {
                    when {
                        el.isString -> el.content
                        el.booleanOrNull is Boolean -> el.content.toBoolean()
                        el.intOrNull is Int -> el.content.toInt()
                        el.floatOrNull is Float -> el.content.toFloat()
                        el.longOrNull is Long ->el.content.toLong()
                        el.doubleOrNull is Double -> el.content.toDouble()
                        else -> el.content
                    }
                }
                is JsonArray -> el.toSet()
                else -> Unit//throw IllegalArgumentException("Unsupported JSON element for deserialization: $jsonElement")
            }
        }
    }


    @Test
    fun fun1() {
        val map = mutableMapOf<String, Any>()
        map["aaa"] = setOf("aa", "bb")
        val mapSerializer = MapSerializer(String.serializer(), PrefValueSerializer())
        val str = Json.encodeToString(mapSerializer, map)
        println("编码成字符串："+str)
        val map2:Map<String,Any> = Json.decodeFromString(mapSerializer,str)
        print("变回map:" +map2)
    }


}