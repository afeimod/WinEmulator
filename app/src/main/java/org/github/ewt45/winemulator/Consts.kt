package org.github.ewt45.winemulator

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

object Consts {
    private val TAG = "Consts"
    lateinit var cacheDir: File

    /** 用于proot绑定 /tmp 的安卓路径 */
    lateinit var tmpDir: File

    /** 此文件夹内包含各种rootfs. files/rootfs */
    lateinit var rootfsAllDir: File

    /** 当前激活的rootfs, 应该为一个指向实际rootfs的软链接. files/rootfs/current */
    lateinit var rootfsCurrDir: File

    /** 一个用于测试的alpine rootfs. files/rootfs/alpine-aarch64 */
    lateinit var alpineRootfsDir: File

    /** proot二进制文件. files/proot  */
    lateinit var prootBin: File

    /** 定义在assets中的默认值，此map中的值会优先于代码中的默认值生效。key为datastore的某个key, value为对应value */
    private lateinit var prefInAssets: Map<String, Any>

    object Ui {
        /** 最小化时的宽高dp值 */
        val minimizedIconSize = 48
    }

    /**
     * 用户偏好相关.
     * 如果assets中指定了默认值，会覆盖这里的默认值
     */
    object Pref {
        data class Item<T>(val key: Preferences.Key<T>, val default: T)

        val proot_bool_options by item("proot_bool_options", setOf("--root-id", "-L", "--link2symlink", "--kill-on-exit"))
        val proot_startup_cmd by item("proot_startup_cmd", "")

        /**
         * 初始化Item需要在读取assets之后，lazy的话 第一次用到Pref时Consts应该已经初始化好了吧。用lateinit的话还需要多写一行
         */
        private inline fun <reified T> item(name: String, default: T): Lazy<Item<T>> = lazy {
            val key: Preferences.Key<T> = when (T::class) {
                Set::class -> stringSetPreferencesKey(name)
                String::class -> stringPreferencesKey(name)
                Boolean::class -> booleanPreferencesKey(name)
                Int::class -> intPreferencesKey(name)
                Float::class -> floatPreferencesKey(name)
                Long::class -> longPreferencesKey(name)
                Double::class -> doublePreferencesKey(name)
                else -> throw IllegalArgumentException("Unsupported type: ${T::class.simpleName}")
            } as Preferences.Key<T>
            val finalDefault = (prefInAssets[name].takeIf { it is T } ?: default) as T
            return@lazy Item(key, finalDefault)
        }
    }

    /**
     * 初始化。使用前先调用一次
     */
    fun init(ctx: Context) {
        cacheDir = ctx.cacheDir
        cacheDir.mkdirs()

        tmpDir = File(cacheDir, "tmp")
        tmpDir.mkdirs()
//        Os.chmod(tmpDir.absolutePath, 0777)

        val fileDir = ctx.filesDir
        rootfsAllDir = File(fileDir, "rootfs")
        rootfsAllDir.mkdirs()

        rootfsCurrDir = File(rootfsAllDir, "current")

        alpineRootfsDir = File(rootfsAllDir, "alpine-aarch64") //这个等解压的时候再创建吧

        //proot从assets解压
        prootBin = File(fileDir, "proot")
        if (!prootBin.exists()) {
            Utils.streamCopy(ctx.assets.open("proot"), FileOutputStream(prootBin))
        }
        prootBin.setExecutable(true)

        //优先生效的用户偏好
        prefInAssets = kotlin.runCatching {
            val str = IOUtils.toString(ctx.assets.open("preferences.json"), StandardCharsets.UTF_8)
            val mapSerializer = MapSerializer(String.serializer(), PrefValueSerializer)
            return@runCatching Json.decodeFromString<Map<String, Any>>(mapSerializer, str)
        }.onFailure { Log.e(TAG, "init: 获取assets/preferences.json失败", it) }.getOrNull() ?: mapOf()

    }


    /**
     * 用于序列化/反序列化 [prefInAssets] -> json,
     */
    private object PrefValueSerializer : KSerializer<Any> {
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

        override fun deserialize(decoder: Decoder): Any {
            return when (val el = (decoder as JsonDecoder).decodeJsonElement()) {
                is JsonPrimitive -> {
                    when {
                        el.isString -> el.content
                        el.booleanOrNull is Boolean -> el.content.toBoolean()
                        el.intOrNull is Int -> el.content.toInt()
                        el.floatOrNull is Float -> el.content.toFloat()
                        el.longOrNull is Long -> el.content.toLong()
                        el.doubleOrNull is Double -> el.content.toDouble()
                        else -> el.content
                    }
                }

                is JsonArray -> el.toSet()
                else -> Unit//throw IllegalArgumentException("Unsupported JSON element for deserialization: $jsonElement")
            }
        }
    }
}

