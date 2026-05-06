package org.github.ewt45.winemulator.inputcontrols

import org.json.JSONObject

/**
 * External game controller binding
 * Added to support gamepad input in InputControlsView
 */
class ExternalControllerBinding {
    // Use different name to avoid clash
    var keyCodeForAxisVal: Int = 0
    var bindingVal: Binding? = null

    constructor()

    constructor(keyCodeForAxis: Int, binding: Binding) {
        this.keyCodeForAxisVal = keyCodeForAxis
        this.bindingVal = binding
    }

    fun setKeyCode(keyCode: Int) {
        this.keyCodeForAxisVal = keyCode
    }

    fun setBinding(binding: Binding?) {
        this.bindingVal = binding
    }

    val binding: Binding?
        get() = bindingVal

    fun getKeyCodeForAxisValue(): Int = keyCodeForAxisVal

    fun toJSONObject(): JSONObject? {
        return try {
            val jsonObject = JSONObject()
            jsonObject.put("keyCodeForAxis", keyCodeForAxisVal)
            jsonObject.put("binding", bindingVal?.toString())
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