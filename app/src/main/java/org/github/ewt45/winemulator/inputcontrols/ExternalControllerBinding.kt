package org.github.ewt45.winemulator.inputcontrols

import org.json.JSONObject

/**
 * External game controller binding
 * Added to support gamepad input in InputControlsView
 */
class ExternalControllerBinding {
    // Use different name to avoid clash
    var keyCodeForAxisVal: Int = 0
    var binding: Binding? = null

    constructor(keyCodeForAxis: Int, binding: Binding) {
        this.keyCodeForAxisVal = keyCodeForAxis
        this.binding = binding
    }

    fun getKeyCodeForAxisValue(): Int = keyCodeForAxisVal

    fun toJSONObject(): JSONObject? {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("keyCodeForAxis", keyCodeForAxisVal)
            jsonObject.put("binding", binding?.toString())
            jsonObject
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun getKeyCodeForAxis(axis: Int, direction: Int): Int {
            // Map axis + direction to key code
            return axis * 2 + if (direction > 0) 1 else 0
        }
    }
}