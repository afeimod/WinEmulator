package org.github.ewt45.winemulator

import org.junit.Test

import org.junit.Assert.*
import java.io.File
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission.*
import java.nio.file.attribute.PosixFilePermissions
import kotlin.math.pow

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

    fun parseString(input: String): Pair<String, Int> {
        // 正则表达式解释：
        // ^      : 匹配字符串的开头
        // (.*)   : 第一个捕获组，匹配任意数量的任意字符（非贪婪模式）
        // -      : 匹配字面字符 '-'
        // (\d+)  : 第二个捕获组，匹配一个或多个数字 (0-9)
        // $      : 匹配字符串的结尾

        return "^(.+)-(\\d+)$".toRegex().matchEntire(input)?.let {
            val (baseString, numberString) = it.destructured
            Pair(baseString, numberString.toInt())
        } ?: Pair("basename", 1)
    }

    fun main() {

    }

    @Test
    fun fun1() {
// 符合格式的例子
        println(parseString("abc-123"))        // Pair(abc, 123)
        println(parseString("abc-123-def-345"))        // Pair(abc, 123)
        println(parseString("hello-world-45")) // Pair(hello_world, 45)
        println(parseString("123abc-789"))     // Pair(123abc, 789)
        println(parseString("something-0"))    // Pair(something, 0)
        println(parseString("a-1"))            // Pair(a, 1)

        // 不符合格式的例子
        println(parseString("-1"))             // Pair(, 1) - 第一个部分为空字符串
        println(parseString("abc"))            // Pair(basename, 1) - 没有 '-'
        println(parseString("abc-"))           // Pair(basename, 1) - '-' 后面没有数字
        println(parseString("-abc"))           // Pair(basename, 1) - '-' 后面不是数字
        println(parseString("abc-123-def"))    // Pair(basename, 1) - 格式不完全匹配
        println(parseString("123"))            // Pair(basename, 1)
        println(parseString(""))               // Pair(basename, 1)
        println(parseString("test-123x"))      // Pair(basename, 1) - 后面有非数字
    }


}