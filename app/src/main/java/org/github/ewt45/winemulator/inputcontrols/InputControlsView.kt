package org.github.ewt45.winemulator.inputcontrols

import android.content.Context
import android.graphics.*
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.termux.x11.controller.math.Mathf
import com.termux.x11.controller.widget.TouchpadView
import com.termux.x11.LorieView
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.HashMap
import java.util.Timer
import java.util.TimerTask

class InputControlsView(context: Context?) : View(context) {
    companion object {
        const val DEFAULT_OVERLAY_OPACITY = 0.4f
        const val MAX_TAP_TRAVEL_DISTANCE: Byte = 10
        const val MAX_TAP_MILLISECONDS: Short = 200
        const val CURSOR_ACCELERATION = 1.25f
        const val CURSOR_ACCELERATION_THRESHOLD: Byte = 6
    }

    private var editMode = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val colorFilter = PorterDuffColorFilter(0xffffffff.toInt(), PorterDuff.Mode.SRC_IN)
    private val cursor = Point()
    private var readyToDraw = false
    private var moveCursor = false
    private var snappingSize = 0
    private var offsetX = 0f
    private var offsetY = 0f
    private var selectedElement: ControlElement? = null
    private var profile: ControlsProfile? = null
    private var overlayOpacity = DEFAULT_OVERLAY_OPACITY
    private var touchpadView: TouchpadView? = null
    private var xServer: LorieView? = null
    private val icons = arrayOfNulls<Bitmap>(17)
    private var mouseMoveTimer: Timer? = null
    private val mouseMoveOffset = PointF()
    private var showTouchscreenControls = true
    private val counterMap = HashMap<String, Int>()

    fun counterMapIncrease(iconId: String) {
        var v = counterMap[iconId]
        if (v == null) {
            v = 0
        }
        v++
        counterMap[iconId] = v
    }

    fun counterMapDecrease(iconId: String) {
        var v = counterMap[iconId]
        if (v != null) {
            v--
            counterMap[iconId] = v
        }
    }

    fun counterMapZero(iconId: String): Boolean {
        val v = counterMap[iconId]
        if (v == null) {
            return true
        }
        return v <= 0
    }

    init {
        setClickable(true)
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(0x00000000)
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun setEditMode(editMode: Boolean) {
        this.editMode = editMode
    }

    fun setOverlayOpacity(overlayOpacity: Float) {
        this.overlayOpacity = overlayOpacity
    }

    fun getSnappingSize(): Int {
        return snappingSize
    }

    override fun onDraw(canvas: Canvas) {
        val width = width
        val height = height

        if (width == 0 || height == 0) {
            readyToDraw = false
            return
        }
        snappingSize = maxOf(width, height) / 100

        readyToDraw = true

        if (editMode) {
            drawGrid(canvas)
            drawCursor(canvas)
        }
        if (profile != null) {
            if (!profile!!.isElementsLoaded) {
                profile!!.loadElements(this)
            }
            if (showTouchscreenControls) {
                for (element in profile!!.getElements()) {
                    element.draw(canvas)
                }
            }
        }

        super.onDraw(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = snappingSize * 0.0625f
        paint.color = 0xff000000.toInt()
        canvas.drawColor(Color.BLACK)

        paint.isAntiAlias = false
        paint.color = 0xff303030.toInt()

        val width = maxWidth
        val height = maxHeight

        var i = 0
        while (i < width) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
            canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
            i += snappingSize
        }

        val cx = Mathf.roundTo(width * 0.5f, snappingSize.toFloat())
        val cy = Mathf.roundTo(height * 0.5f, snappingSize.toFloat())
        paint.color = 0xff424242.toInt()

        i = 0
        while (i < width) {
            canvas.drawLine(cx, i.toFloat(), cx, (i + snappingSize).toFloat(), paint)
            canvas.drawLine(i.toFloat(), cy, (i + snappingSize).toFloat(), cy, paint)
            i += snappingSize * 2
        }

        paint.isAntiAlias = true
    }

    private fun drawCursor(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.strokeWidth = snappingSize * 0.0625f
        paint.color = 0xffc62828.toInt()

        paint.isAntiAlias = false
        canvas.drawLine(0f, cursor.y.toFloat(), maxWidth.toFloat(), cursor.y.toFloat(), paint)
        canvas.drawLine(cursor.x.toFloat(), 0f, cursor.x.toFloat(), maxHeight.toFloat(), paint)

        paint.isAntiAlias = true
    }

    fun addElement(): Boolean {
        if (editMode && profile != null) {
            val element = ControlElement(this)
            element.x = cursor.x
            element.y = cursor.y
            profile!!.addElement(element)
            profile!!.save()
            selectElement(element)
            return true
        }
        return false
    }

    fun removeElement(): Boolean {
        if (editMode && selectedElement != null && profile != null) {
            profile!!.removeElement(selectedElement!!)
            selectedElement = null
            profile!!.save()
            invalidate()
            return true
        }
        return false
    }

    fun getSelectedElement(): ControlElement? {
        return selectedElement
    }

    private fun deselectAllElements() {
        selectedElement = null
        if (profile != null) {
            for (element in profile!!.getElements()) {
                element.isSelected = false
            }
        }
    }

    private fun selectElement(element: ControlElement?) {
        deselectAllElements()
        if (element != null) {
            selectedElement = element
            selectedElement!!.isSelected = true
        }
        invalidate()
    }

    fun getProfile(): ControlsProfile? {
        return profile
    }

    fun setProfile(profile: ControlsProfile?) {
        if (profile != null) {
            this.profile = profile
            deselectAllElements()
        } else {
            this.profile = null
        }
    }

    fun isShowTouchscreenControls(): Boolean {
        return showTouchscreenControls
    }

    fun setShowTouchscreenControls(showTouchscreenControls: Boolean) {
        this.showTouchscreenControls = showTouchscreenControls
    }

    fun getPrimaryColor(): Int {
        return Color.argb((overlayOpacity * 255).toInt(), 255, 255, 255)
    }

    fun getSecondaryColor(): Int {
        return Color.argb((overlayOpacity * 255).toInt(), 2, 119, 189)
    }

    private fun intersectElement(x: Float, y: Float): ControlElement? {
        if (profile != null) {
            for (element in profile!!.getElements()) {
                if (element.containsPoint(x, y)) return element
            }
        }
        return null
    }

    fun getPaint(): Paint {
        return paint
    }

    fun getPath(): Path {
        return path
    }

    fun getColorFilter(): ColorFilter {
        return colorFilter
    }

    fun getTouchpadView(): TouchpadView? {
        return touchpadView
    }

    fun setTouchpadView(touchpadView: TouchpadView) {
        this.touchpadView = touchpadView
    }

    fun getXServer(): LorieView? {
        return xServer
    }

    fun setXServer(xServer: LorieView) {
        this.xServer = xServer
        createMouseMoveTimer()
    }

    val maxWidth: Int
        get() = Mathf.roundTo(width.toFloat(), snappingSize.toFloat()).toInt()

    val maxHeight: Int
        get() = Mathf.roundTo(height.toFloat(), snappingSize.toFloat()).toInt()

    private fun createMouseMoveTimer() {
        if (profile != null && mouseMoveTimer == null) {
            val cursorSpeed = profile!!.cursorSpeed
            mouseMoveTimer = Timer()
            mouseMoveTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    xServer?.injectPointerMoveDelta(
                        (mouseMoveOffset.x * 10 * cursorSpeed).toInt(),
                        (mouseMoveOffset.y * 10 * cursorSpeed).toInt()
                    )
                }
            }, 0, 1000 / 60)
        }
    }

    private fun processJoystickInput(controller: ExternalController) {
        var controllerBinding: ExternalControllerBinding?
        val axes = intArrayOf(
            MotionEvent.AXIS_X, MotionEvent.AXIS_Y,
            MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y
        )
        val values = floatArrayOf(
            controller.state.thumbLX, controller.state.thumbLY,
            controller.state.thumbRX, controller.state.thumbRY,
            controller.state.getDPadX().toFloat(), controller.state.getDPadY().toFloat()
        )

        for (i in axes.indices) {
            if (kotlin.math.abs(values[i]) > ControlElement.STICK_DEAD_ZONE) {
                controllerBinding = controller.getControllerBinding(
                    ExternalControllerBinding.getKeyCodeForAxis(axes[i], Mathf.sign(values[i]).toByte())
                )
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding!!, true, values[i])
                }
            } else {
                controllerBinding = controller.getControllerBinding(
                    ExternalControllerBinding.getKeyCodeForAxis(axes[i], 1.toByte())
                )
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding!!, false, values[i])
                }
                controllerBinding = controller.getControllerBinding(
                    ExternalControllerBinding.getKeyCodeForAxis(axes[i], (-1).toByte())
                )
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding!!, false, values[i])
                }
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!editMode && profile != null) {
            val controller = profile!!.getController(event.deviceId)
            if (controller != null && controller.updateStateFromMotionEvent(event)) {
                var controllerBinding: ExternalControllerBinding?
                controllerBinding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_L2)
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding!!, controller.state.isPressed(ExternalController.IDX_BUTTON_L2))
                }

                controllerBinding = controller.getControllerBinding(KeyEvent.KEYCODE_BUTTON_R2)
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.binding!!, controller.state.isPressed(ExternalController.IDX_BUTTON_R2))
                }

                processJoystickInput(controller)
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onHoverEvent(event: MotionEvent): Boolean {
        return touchpadView?.onHoverEvent(event) ?: false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (editMode && readyToDraw) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y

                    val element = intersectElement(x, y)
                    moveCursor = true
                    if (element != null) {
                        offsetX = x - element.x
                        offsetY = y - element.y
                        moveCursor = false
                    }
                    selectElement(element)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (selectedElement != null) {
                        selectedElement!!.x = Mathf.roundTo(event.x - offsetX, snappingSize.toFloat()).toInt()
                        selectedElement!!.y = Mathf.roundTo(event.y - offsetY, snappingSize.toFloat()).toInt()
                        invalidate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (selectedElement != null && profile != null) profile!!.save()
                    if (moveCursor) {
                        cursor.set(
                            Mathf.roundTo(event.x, snappingSize.toFloat()).toInt(),
                            Mathf.roundTo(event.y, snappingSize.toFloat()).toInt()
                        )
                    }
                    invalidate()
                }
            }
        }
        return true
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            return touchpadView?.onTouchEvent(event) ?: false
        }
        if (!editMode && profile != null) {
            val actionIndex = event.actionIndex
            val pointerId = event.getPointerId(actionIndex)
            val actionMasked = event.actionMasked
            var handled = false

            when (actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val x = event.getX(actionIndex)
                    val y = event.getY(actionIndex)
                    touchpadView?.setPointerButtonLeftEnabled(true)
                    for (element in profile!!.getElements()) {
                        if (element.handleTouchDown(pointerId, x, y)) {
                            handled = true
                            if (element.getBindingAt(0) === Binding.MOUSE_LEFT_BUTTON) {
                                touchpadView?.setPointerButtonLeftEnabled(false)
                            }
                        }
                    }
                    if (!handled) {
                        touchpadView?.onTouchEvent(event)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val x = event.getX(i)
                        val y = event.getY(i)
                        handled = false
                        for (element in profile!!.getElements()) {
                            if (element.handleTouchMove(i, x, y)) {
                                handled = true
                            }
                        }
                        if (!handled) {
                            touchpadView?.onTouchEvent(event)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    for (i in 0 until event.pointerCount) {
                        val x = event.getX(i)
                        val y = event.getY(i)
                        for (element in profile!!.getElements()) {
                            if (element.handleTouchUp(pointerId, x, y)) {
                                handled = true
                            }
                        }
                        if (!handled) {
                            touchpadView?.onTouchEvent(event)
                        }
                    }
                }
            }
            return handled
        }
        return false
    }

    fun handleInputEvent(binding: Binding, isActionDown: Boolean) {
        handleInputEvent(binding, isActionDown, 0f)
    }

    fun handleInputEvent(binding: Binding, isActionDown: Boolean, offset: Float) {
        if (binding.isGamepad) {
            val winHandler = xServer?.winHandler
            val state = profile?.gamepadState
            val buttonIdx = binding.ordinal - Binding.GAMEPAD_BUTTON_A.ordinal
            if (buttonIdx <= 11) {
                state?.setPressed(buttonIdx, isActionDown)
            } else if (binding === Binding.GAMEPAD_LEFT_THUMB_UP || binding === Binding.GAMEPAD_LEFT_THUMB_DOWN) {
                if (state != null) state.thumbLY = if (isActionDown) offset else 0f
            } else if (binding === Binding.GAMEPAD_LEFT_THUMB_LEFT || binding === Binding.GAMEPAD_LEFT_THUMB_RIGHT) {
                if (state != null) state.thumbLX = if (isActionDown) offset else 0f
            } else if (binding === Binding.GAMEPAD_RIGHT_THUMB_UP || binding === Binding.GAMEPAD_RIGHT_THUMB_DOWN) {
                if (state != null) state.thumbRY = if (isActionDown) offset else 0f
            } else if (binding === Binding.GAMEPAD_RIGHT_THUMB_LEFT || binding === Binding.GAMEPAD_RIGHT_THUMB_RIGHT) {
                if (state != null) state.thumbRX = if (isActionDown) offset else 0f
            } else if (binding === Binding.GAMEPAD_DPAD_UP || binding === Binding.GAMEPAD_DPAD_RIGHT ||
                binding === Binding.GAMEPAD_DPAD_DOWN || binding === Binding.GAMEPAD_DPAD_LEFT) {
                if (state != null) {
                    state.dpad[binding.ordinal - Binding.GAMEPAD_DPAD_UP.ordinal] = isActionDown
                }
            }

            if (winHandler != null) {
                val controller = winHandler.currentController
                if (controller != null) {
                    controller.state.copy(state!!)
                }
                winHandler.sendGamepadState()
            }
        } else {
            if (binding === Binding.MOUSE_MOVE_LEFT || binding === Binding.MOUSE_MOVE_RIGHT) {
                mouseMoveOffset.x = if (isActionDown) {
                    if (offset != 0f) offset else {
                        if (binding === Binding.MOUSE_MOVE_LEFT) -1f else 1f
                    }
                } else {
                    0f
                }
                if (isActionDown) createMouseMoveTimer()
            } else if (binding === Binding.MOUSE_MOVE_DOWN || binding === Binding.MOUSE_MOVE_UP) {
                mouseMoveOffset.y = if (isActionDown) {
                    if (offset != 0f) offset else {
                        if (binding === Binding.MOUSE_MOVE_UP) -1f else 1f
                    }
                } else {
                    0f
                }
                if (isActionDown) createMouseMoveTimer()
            } else {
                val pointerButton = binding.pointerButton
                if (isActionDown) {
                    if (pointerButton != null) {
                        xServer?.injectPointerButtonPress(pointerButton)
                    } else {
                        xServer?.injectKeyPress(binding.keycode)
                    }
                } else {
                    if (pointerButton != null) {
                        xServer?.injectPointerButtonRelease(pointerButton)
                    } else {
                        xServer?.injectKeyRelease(binding.keycode)
                    }
                }
            }
        }
    }

    fun sendText(text: String?) {
        xServer?.injectText(text)
        xServer?.injectKeyPress(XKeycode.KEY_ENTER)
        xServer?.injectKeyRelease(XKeycode.KEY_ENTER)
    }

    fun getIcon(id: Byte): Bitmap? {
        if (icons[id.toInt()] == null) {
            val context = context
            try {
                context.assets.open("inputcontrols/icons/$id.png").use(`is` ->
                    icons[id.toInt()] = BitmapFactory.decodeStream(`is`))
            } catch (e: IOException) {
            }
        }
        return icons[id.toInt()]
    }

    fun getCustomIcon(iconId: String?): Bitmap? {
        val buttonIconFile = File(context.filesDir.path + "/home/.buttonIcons", iconId + ".png")
        if (!buttonIconFile.exists()) {
            return null
        }
        return BitmapFactory.decodeFile(buttonIconFile.path)
    }

    fun clipBitmap(bitmap: Bitmap?, isCircular: Boolean): Bitmap? {
        if (bitmap == null) return null
        val clippedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(clippedBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        if (isCircular) {
            val centerX = bitmap.width / 2
            val centerY = bitmap.height / 2
            val radius = minOf(centerX, centerY)
            canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), paint)
        } else {
            val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            canvas.drawRect(rect, paint)
        }
        return clippedBitmap
    }

    companion object {
        fun createShapeBitmap(width: Float, height: Float, color: Int, isCircular: Boolean): Bitmap {
            val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = color

            if (isCircular) {
                val radius = (minOf(width, height) / 2).toInt()
                canvas.drawCircle(width / 2, height / 2, radius.toFloat(), paint)
            } else {
                val rect = RectF(0f, 0f, width, height)
                canvas.drawRect(rect, paint)
            }
            return bitmap
        }
    }
}
