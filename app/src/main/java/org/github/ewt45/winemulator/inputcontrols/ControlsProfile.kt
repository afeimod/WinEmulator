package org.github.ewt45.winemulator.inputcontrols

import android.content.Context
import androidx.annotation.NonNull
import com.termux.x11.controller.core.FileUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.Locale

/** The {@link Class} that represents a controls profile containing control elements and controller bindings. */
class ControlsProfile(
    private val context: Context,
    val id: Int
) : Comparable<ControlsProfile> {
    
    var name: String = ""
    var cursorSpeed: Float = 1.0f
    
    private val elements = ArrayList<ControlElement>()
    private val controllers = ArrayList<ExternalController>()
    private val immutableElements: List<ControlElement> = Collections.unmodifiableList(elements)
    private var elementsLoaded = false
    private var controllersLoaded = false
    private var virtualGamepad = false
    private var gamepadState: GamepadState? = null

    fun getName(): String = name
    fun setName(name: String) { this.name = name }

    fun getCursorSpeed(): Float = cursorSpeed
    fun setCursorSpeed(cursorSpeed: Float) { this.cursorSpeed = cursorSpeed }

    fun isVirtualGamepad(): Boolean = virtualGamepad

    fun getGamepadState(): GamepadState {
        if (gamepadState == null) gamepadState = GamepadState()
        return gamepadState!!
    }

    fun addController(id: String): ExternalController? {
        var controller = getController(id)
        if (controller == null) {
            controller = ExternalController.getController(id)
            controllers.add(controller!!)
        }
        controllersLoaded = true
        return controller
    }

    fun removeController(controller: ExternalController) {
        if (!controllersLoaded) loadControllers()
        controllers.remove(controller)
    }

    fun getController(id: String): ExternalController? {
        if (!controllersLoaded) loadControllers()
        for (c in controllers) {
            if (c.id == id) return c
        }
        return null
    }

    fun getController(deviceId: Int): ExternalController? {
        if (!controllersLoaded) loadControllers()
        for (c in controllers) {
            if (c.getControllerDeviceId() == deviceId) return c
        }
        return null
    }

    @NonNull
    override fun toString(): String = name

    override fun compareTo(other: ControlsProfile): Int = Integer.compare(id, other.id)

    fun isElementsLoaded(): Boolean = elementsLoaded

    fun save() {
        val file = getProfileFile(context, id)

        try {
            val data = JSONObject()
            data.put("id", id)
            data.put("name", name)
            data.put("cursorSpeed", cursorSpeed.toDouble())

            val elementsJSONArray = JSONArray()
            if (!elementsLoaded && file.isFile) {
                val profileJSONObject = JSONObject(FileUtils.readString(file))
                elementsJSONArray.put(profileJSONObject.getJSONArray("elements"))
            } else {
                for (element in elements) {
                    element.toJSONObject()?.let { elementsJSONArray.put(it) }
                }
            }
            data.put("elements", elementsJSONArray)

            val controllersJSONArray = JSONArray()
            if (!controllersLoaded && file.isFile) {
                val profileJSONObject = JSONObject(FileUtils.readString(file))
                if (profileJSONObject.has("controllers")) {
                    controllersJSONArray.put(profileJSONObject.getJSONArray("controllers"))
                }
            } else {
                for (controller in controllers) {
                    controller.toJSONObject()?.let { controllerJSONObject ->
                        controllersJSONArray.put(controllerJSONObject)
                    }
                }
            }
            if (controllersJSONArray.length() > 0) data.put("controllers", controllersJSONArray)

            FileUtils.writeString(file, data.toString())
        } catch (e: JSONException) {
            // Ignore JSON errors
        }
    }

    fun addElement(element: ControlElement) {
        elements.add(element)
        elementsLoaded = true
    }

    fun removeElement(element: ControlElement) {
        elements.remove(element)
        elementsLoaded = true
    }

    fun getElements(): List<ControlElement> = immutableElements

    fun isTemplate(): Boolean = name.lowercase(Locale.ENGLISH).contains("template")

    fun loadControllers(): ArrayList<ExternalController> {
        controllers.clear()
        controllersLoaded = false

        val file = getProfileFile(context, id)
        if (!file.isFile) return controllers

        try {
            val profileJSONObject = JSONObject(FileUtils.readString(file))
            if (!profileJSONObject.has("controllers")) return controllers
            
            val controllersJSONArray = profileJSONObject.getJSONArray("controllers")
            for (i in 0 until controllersJSONArray.length()) {
                val controllerJSONObject = controllersJSONArray.getJSONObject(i)
                val controllerId = controllerJSONObject.getString("id")
                val controller = ExternalController()
                controller.setControllerId(controllerId)
                controller.setControllerName(controllerJSONObject.getString("name"))

                val controllerBindingsJSONArray = controllerJSONObject.getJSONArray("controllerBindings")
                for (j in 0 until controllerBindingsJSONArray.length()) {
                    val controllerBindingJSONObject = controllerBindingsJSONArray.getJSONObject(j)
                    val controllerBinding = ExternalControllerBinding()
                    controllerBinding.setKeyCode(controllerBindingJSONObject.getInt("keyCode"))
                    controllerBinding.setBinding(Binding.fromString(controllerBindingJSONObject.getString("binding")))
                    controller.addControllerBinding(controllerBinding)
                }
                controllers.add(controller)
            }
            controllersLoaded = true
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return controllers
    }

    fun loadElements(inputControlsView: InputControlsView) {
        elements.clear()
        elementsLoaded = false
        virtualGamepad = false

        val file = getProfileFile(context, id)
        if (!file.isFile) return

        try {
            val profileJSONObject = JSONObject(FileUtils.readString(file))
            val elementsJSONArray = profileJSONObject.getJSONArray("elements")
            
            for (i in 0 until elementsJSONArray.length()) {
                val elementJSONObject = elementsJSONArray.getJSONObject(i)
                val element = ControlElement(inputControlsView)
                
                element.setType(ControlElement.Type.valueOf(elementJSONObject.getString("type")))
                element.setShape(ControlElement.Shape.valueOf(elementJSONObject.getString("shape")))
                element.setToggleSwitch(elementJSONObject.getBoolean("toggleSwitch"))
                element.setX((elementJSONObject.getDouble("x") * inputControlsView.maxWidth).toInt())
                element.setY((elementJSONObject.getDouble("y") * inputControlsView.maxHeight).toInt())
                element.setScale(elementJSONObject.getDouble("scale").toFloat())
                element.setText(elementJSONObject.getString("text"))
                element.setIconId(elementJSONObject.getInt("iconId"))
                
                if (elementJSONObject.has("cheatCodeText")) {
                    element.setCheatCodeText(elementJSONObject.getString("cheatCodeText"))
                }
                if (elementJSONObject.has("customIconId")) {
                    element.setCustomIconId(elementJSONObject.getString("customIconId"))
                }
                if (elementJSONObject.has("backgroundColor")) {
                    element.setBackgroundColor(elementJSONObject.getInt("backgroundColor"))
                }
                if (elementJSONObject.has("range")) {
                    element.setRange(ControlElement.Range.valueOf(elementJSONObject.getString("range")))
                }
                if (elementJSONObject.has("orientation")) {
                    element.setOrientation(elementJSONObject.getInt("orientation").toByte())
                }

                var hasGamepadBinding = true
                val bindingsJSONArray = elementJSONObject.getJSONArray("bindings")
                for (j in 0 until bindingsJSONArray.length()) {
                    val binding = Binding.fromString(bindingsJSONArray.getString(j))
                    element.setBindingAt(j, binding)
                    if (!binding.isGamepad()) hasGamepadBinding = false
                }

                if (!virtualGamepad && hasGamepadBinding) virtualGamepad = true
                elements.add(element)
            }
            elementsLoaded = true
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    companion object {
        fun getProfileFile(context: Context, id: Int): File {
            return File(InputControlsManager.getProfilesDir(context), "controls-$id.icp")
        }
    }
}
