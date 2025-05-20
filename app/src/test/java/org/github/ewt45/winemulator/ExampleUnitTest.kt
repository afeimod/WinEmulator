package org.github.ewt45.winemulator

import org.junit.Test

import org.junit.Assert.*
import java.io.File

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

    fun removeNonAlphanumeric(input: String): String {
        // 正则表达式 ^[a-zA-Z0-9] 匹配任何非字母或非数字的字符
        // `toRegex()` 将字符串转换为正则表达式对象
        return input.replace("[^a-zA-Z0-9]".toRegex(), "")
    }


    @Test
    fun fun1() {
        val originalString1 = "Hello, World! 123 @#$"
        val cleanedString1 = removeNonAlphanumeric(originalString1)
        println("原始字符串: \"$originalString1\"")
        println("清理后: \"$cleanedString1\"")
        // Output: 原始字符串: "Hello, World! 123 @#$"，清理后: "HelloWorld123"

        val originalString2 = "Kotlin is Fun! #123_abc"
        val cleanedString2 = removeNonAlphanumeric(originalString2)
        println("原始字符串: \"$originalString2\"")
        println("清理后: \"$cleanedString2\"")
        // Output: 原始字符串: "Kotlin is Fun! #123_abc"，清理后: "KotlinisFun123abc"

        print(removeNonAlphanumeric("en_US.UTF-8"))

    }


}