package org.github.ewt45.winemulator

import org.junit.Test

import org.junit.Assert.*
import java.io.File
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission.*
import java.nio.file.attribute.PosixFilePermissions
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    @Test
    fun fun1() {

        val currDateTIme = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy-MM-dd_HH-mm-ss"))
        println("获取时间：$currDateTIme")
    }


}