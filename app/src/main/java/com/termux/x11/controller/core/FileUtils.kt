package com.termux.x11.controller.core

import android.content.Context
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Utility class for file operations - simplified version for Linbox compatibility.
 */
abstract class FileUtils {
    companion object {
        /**
         * Read entire file content as string
         */
        fun readString(file: File): String? {
            return try {
                BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } catch (e: IOException) {
                null
            }
        }

        /**
         * Write string content to file
         */
        fun writeString(file: File, content: String): Boolean {
            return try {
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { writer ->
                    writer.write(content)
                }
                true
            } catch (e: IOException) {
                false
            }
        }

        /**
         * Copy file from assets to target file
         */
        fun copy(context: Context, assetPath: String, targetFile: File): Boolean {
            return try {
                context.assets.open(assetPath).use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } catch (e: IOException) {
                false
            }
        }

        /**
         * Check if directory is empty
         */
        fun isEmpty(directory: File): Boolean {
            return !directory.exists() || directory.listFiles()?.isEmpty() != false
        }

        /**
         * Read JSON array from file
         */
        fun readJSONArray(file: File): org.json.JSONArray? {
            val content = readString(file) ?: return null
            return try {
                org.json.JSONArray(content)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Read JSON object from file
         */
        fun readJSONObject(file: File): org.json.JSONObject? {
            val content = readString(file) ?: return null
            return try {
                org.json.JSONObject(content)
            } catch (e: Exception) {
                null
            }
        }
    }
}