package org.github.ewt45.winemulator.inputcontrols

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.ArrayList

/**
 * External game controller support
 * Added to support gamepad input in InputControlsView
 */
class ExternalController {
    companion object {
        const val IDX_BUTTON_A: Byte = 0
        const val IDX_BUTTON_B: Byte = 1
        const val IDX_BUTTON_X: Byte = 2
        const val IDX_BUTTON_Y: Byte = 3
        const val IDX_BUTTON_L1: Byte = 4
        const val IDX_BUTTON_R1: Byte = 5
        const val IDX_BUTTON_SELECT: Byte = 6
        const val IDX_BUTTON_START: Byte = 7
        const val IDX_BUTTON_L3: Byte = 8
        const val IDX_BUTTON_R3: Byte = 9
        const val IDX_BUTTON_L2: Byte = 10
        const val IDX_BUTTON_R2: Byte = 11

        fun getControllers(): ArrayList<ExternalController> {
            val deviceIds = InputDevice.getDeviceIds()
            val controllers = ArrayList<ExternalController>()
            for (i in deviceIds.indices.reversed()) {
                val device = InputDevice.getDevice(deviceIds[i])
                if (isGameController(device)) {
                    val controller = ExternalController()
                    controller.setId(device!!.descriptor)
                    controller.setName(device.name)
                    controllers.add(controller)
                }
            }
            return controllers
        }

        fun getController(id: String): ExternalController? {
            return getControllers().find { it.id == id }
        }

        fun getController(deviceId: Int): ExternalController? {
            val deviceIds = InputDevice.getDeviceIds()
            for (i in deviceIds.indices.reversed()) {
                if (deviceIds[i] == deviceId || deviceId == 0) {
                    val device = InputDevice.getDevice(deviceIds[i])
                    if (isGameController(device)) {
                        val controller = ExternalController()
                        controller.setId(device!!.descriptor)
                        controller.setName(device.name)
                        controller.deviceId = deviceIds[i]
                        return controller
                    }
                }
            }
            return null
        }

        fun isGameController(device: InputDevice?): Boolean {
            if (device == null) return false
            val sources = device.sources
            return !device.isVirtual() && ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
        }
    }

    var name: String = ""
    var id: String = ""
    var deviceId: Int = -1
    val state = GamepadState()
    private val controllerBindings = ArrayList<ExternalControllerBinding>()

    fun getName(): String = name
    fun setName(name: String) { this.name = name }

    fun getId(): String = id
    fun setId(id: String) { this.id = id }

    fun getDeviceId(): Int {
        if (deviceId == -1) {
            for (dId in InputDevice.getDeviceIds()) {
                val device = InputDevice.getDevice(dId)
                if (device != null && device.descriptor == id) {
                    deviceId = dId
                    break
                }
            }
        }
        return deviceId
    }

    fun isConnected(): Boolean {
        for (dId in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(dId)
            if (device != null && device.descriptor == id) return true
        }
        return false
    }

    fun getControllerBinding(keyCode: Int): ExternalControllerBinding? {
        return controllerBindings.find { it.getKeyCodeForAxis() == keyCode }
    }

    fun getControllerBindingAt(index: Int): ExternalControllerBinding? {
        return if (index >= 0 && index < controllerBindings.size) controllerBindings[index] else null
    }

    fun addControllerBinding(controllerBinding: ExternalControllerBinding) {
        if (getControllerBinding(controllerBinding.getKeyCodeForAxis()) == null) {
            controllerBindings.add(controllerBinding)
        }
    }

    fun getControllerBindingCount(): Int = controllerBindings.size

    fun updateStateFromMotionEvent(event: MotionEvent): Boolean {
        if (isJoystickDevice(event)) {
            processJoystickInput(event)
            return true
        }
        return false
    }

    fun updateStateFromKeyEvent(event: KeyEvent): Boolean {
        val pressed = event.action == KeyEvent.ACTION_DOWN
        val keyCode = event.keyCode
        val buttonIdx = getButtonIdxByKeyCode(keyCode)
        if (buttonIdx != -1) {
            state.setPressed(buttonIdx, pressed)
            return true
        }
        return false
    }

    private fun processJoystickInput(event: MotionEvent) {
        state.thumbLX = getCenteredAxis(event, MotionEvent.AXIS_X)
        state.thumbLY = getCenteredAxis(event, MotionEvent.AXIS_Y)
        state.thumbRX = getCenteredAxis(event, MotionEvent.AXIS_Z)
        state.thumbRY = getCenteredAxis(event, MotionEvent.AXIS_RZ)

        // Handle D-pad via HAT axes
        val axisX = getCenteredAxis(event, MotionEvent.AXIS_HAT_X)
        val axisY = getCenteredAxis(event, MotionEvent.AXIS_HAT_Y)

        state.dpad[0] = axisY == -1.0f && kotlin.math.abs(state.thumbLY) < ControlElement.STICK_DEAD_ZONE
        state.dpad[1] = axisX == 1.0f && kotlin.math.abs(state.thumbLX) < ControlElement.STICK_DEAD_ZONE
        state.dpad[2] = axisY == 1.0f && kotlin.math.abs(state.thumbLY) < ControlElement.STICK_DEAD_ZONE
        state.dpad[3] = axisX == -1.0f && kotlin.math.abs(state.thumbLX) < ControlElement.STICK_DEAD_ZONE
    }

    private fun getCenteredAxis(event: MotionEvent, axis: Int): Float {
        if (axis == MotionEvent.AXIS_HAT_X || axis == MotionEvent.AXIS_HAT_Y) {
            val value = event.getAxisValue(axis)
            if (kotlin.math.abs(value) == 1.0f) return value
        } else {
            val device = event.device
            val range = device?.getMotionRange(axis, event.source)
            if (range != null) {
                val flat = range.flat
                val value = event.getAxisValue(axis)
                if (kotlin.math.abs(value) > flat) return value
            }
        }
        return 0f
    }

    private fun isJoystickDevice(event: MotionEvent): Boolean {
        return (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE
    }

    private fun getButtonIdxByKeyCode(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> IDX_BUTTON_A.toInt()
            KeyEvent.KEYCODE_BUTTON_B -> IDX_BUTTON_B.toInt()
            KeyEvent.KEYCODE_BUTTON_X -> IDX_BUTTON_X.toInt()
            KeyEvent.KEYCODE_BUTTON_Y -> IDX_BUTTON_Y.toInt()
            KeyEvent.KEYCODE_BUTTON_L1 -> IDX_BUTTON_L1.toInt()
            KeyEvent.KEYCODE_BUTTON_R1 -> IDX_BUTTON_R1.toInt()
            KeyEvent.KEYCODE_BUTTON_SELECT -> IDX_BUTTON_SELECT.toInt()
            KeyEvent.KEYCODE_BUTTON_START -> IDX_BUTTON_START.toInt()
            KeyEvent.KEYCODE_BUTTON_THUMBL -> IDX_BUTTON_L3.toInt()
            KeyEvent.KEYCODE_BUTTON_THUMBR -> IDX_BUTTON_R3.toInt()
            KeyEvent.KEYCODE_BUTTON_L2 -> IDX_BUTTON_L2.toInt()
            KeyEvent.KEYCODE_BUTTON_R2 -> IDX_BUTTON_R2.toInt()
            else -> -1
        }
    }

    fun toJSONObject(): JSONObject? {
        return try {
            if (controllerBindings.isEmpty()) return null
            val controllerJSONObject = JSONObject()
            controllerJSONObject.put("id", id)
            controllerJSONObject.put("name", name)

            val controllerBindingsJSONArray = JSONArray()
            for (cb in controllerBindings) {
                controllerBindingsJSONArray.put(cb.toJSONObject())
            }
            controllerJSONObject.put("controllerBindings", controllerBindingsJSONArray)

            controllerJSONObject
        } catch (e: JSONException) {
            null
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is ExternalController && other.id == id
    }
}