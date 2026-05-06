package org.github.ewt45.winemulator.inputcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.PopupWindow
import androidx.annotation.NonNull
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ScheduledExecutorService

/**
 * TermuxExtraKeysView - Extra keys view based on termux-app implementation.
 * Provides extra keys (Escape, Ctrl, Alt) not available on Android soft keyboards.
 * Supports long press for key repeat, popup menus on swipe up, and special toggle buttons.
 */
public final class TermuxExtraKeysView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GridLayout(context, attrs) {

    companion object {
        const val DEFAULT_BUTTON_TEXT_COLOR = 0xFFFFFFFF.toInt()
        const val DEFAULT_BUTTON_ACTIVE_TEXT_COLOR = 0xFF80DEEA.toInt()
        const val DEFAULT_BUTTON_BACKGROUND_COLOR = 0x00000000
        const val DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR = 0xFF7F7F7F.toInt()

        const val MIN_LONG_PRESS_DURATION = 200
        const val MAX_LONG_PRESS_DURATION = 3000
        const val FALLBACK_LONG_PRESS_DURATION = 400

        const val MIN_LONG_PRESS__REPEAT_DELAY = 5
        const val MAX_LONG_PRESS__REPEAT_DELAY = 2000
        const val DEFAULT_LONG_PRESS_REPEAT_DELAY = 80

        val PRIMARY_REPETITIVE_KEYS = listOf(
            "UP", "DOWN", "LEFT", "RIGHT",
            "BKSP", "DEL",
            "PGUP", "PGDN"
        )

        val PRIMARY_KEY_CODES_FOR_STRINGS = mapOf(
            "SPACE" to android.view.KeyEvent.KEYCODE_SPACE,
            "ESC" to android.view.KeyEvent.KEYCODE_ESCAPE,
            "TAB" to android.view.KeyEvent.KEYCODE_TAB,
            "HOME" to android.view.KeyEvent.KEYCODE_MOVE_HOME,
            "END" to android.view.KeyEvent.KEYCODE_MOVE_END,
            "PGUP" to android.view.KeyEvent.KEYCODE_PAGE_UP,
            "PGDN" to android.view.KeyEvent.KEYCODE_PAGE_DOWN,
            "INS" to android.view.KeyEvent.KEYCODE_INSERT,
            "DEL" to android.view.KeyEvent.KEYCODE_FORWARD_DEL,
            "BKSP" to android.view.KeyEvent.KEYCODE_DEL,
            "UP" to android.view.KeyEvent.KEYCODE_DPAD_UP,
            "LEFT" to android.view.KeyEvent.KEYCODE_DPAD_LEFT,
            "RIGHT" to android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
            "DOWN" to android.view.KeyEvent.KEYCODE_DPAD_DOWN,
            "ENTER" to android.view.KeyEvent.KEYCODE_ENTER,
            "F1" to android.view.KeyEvent.KEYCODE_F1,
            "F2" to android.view.KeyEvent.KEYCODE_F2,
            "F3" to android.view.KeyEvent.KEYCODE_F3,
            "F4" to android.view.KeyEvent.KEYCODE_F4,
            "F5" to android.view.KeyEvent.KEYCODE_F5,
            "F6" to android.view.KeyEvent.KEYCODE_F6,
            "F7" to android.view.KeyEvent.KEYCODE_F7,
            "F8" to android.view.KeyEvent.KEYCODE_F8,
            "F9" to android.view.KeyEvent.KEYCODE_F9,
            "F10" to android.view.KeyEvent.KEYCODE_F10,
            "F11" to android.view.KeyEvent.KEYCODE_F11,
            "F12" to android.view.KeyEvent.KEYCODE_F12
        )
    }

    /**
     * Client interface for receiving button click callbacks
     */
    interface IExtraKeysViewClient {
        fun onExtraKeyButtonClick(view: View, buttonInfo: TermuxX11ExtraKeyButton, button: Button)
        fun performExtraKeyButtonHapticFeedback(view: View, buttonInfo: TermuxX11ExtraKeyButton, button: Button): Boolean
    }

    var extraKeysViewClient: IExtraKeysViewClient? = null

    private var specialButtons: MutableMap<TermuxX11SpecialButton, TermuxX11SpecialButtonState> = getDefaultSpecialButtons()
    private var specialButtonsKeys: Set<String> = specialButtons.keys.map { it.key }.toSet()

    private var repetitiveKeys = PRIMARY_REPETITIVE_KEYS

    var buttonTextColor = DEFAULT_BUTTON_TEXT_COLOR
    var buttonActiveTextColor = DEFAULT_BUTTON_ACTIVE_TEXT_COLOR
    var buttonBackgroundColor = DEFAULT_BUTTON_BACKGROUND_COLOR
    var buttonActiveBackgroundColor = DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR

    var buttonTextAllCaps = true

    var longPressTimeout = ViewConfiguration.getLongPressTimeout()
    var longPressRepeatDelay = DEFAULT_LONG_PRESS_REPEAT_DELAY

    private var popupWindow: PopupWindow? = null

    private var scheduledExecutor: ScheduledExecutorService? = null
    private var handler: Handler? = null
    private var specialButtonsLongHoldRunnable: SpecialButtonsLongHoldRunnable? = null
    private var longPressCount = 0

    init {
        setRepetitiveKeys(PRIMARY_REPETITIVE_KEYS)
        setSpecialButtons(getDefaultSpecialButtons())
        setButtonColors(
            DEFAULT_BUTTON_TEXT_COLOR,
            DEFAULT_BUTTON_ACTIVE_TEXT_COLOR,
            DEFAULT_BUTTON_BACKGROUND_COLOR,
            DEFAULT_BUTTON_ACTIVE_BACKGROUND_COLOR
        )
        setLongPressTimeout(ViewConfiguration.getLongPressTimeout())
        setLongPressRepeatDelay(DEFAULT_LONG_PRESS_REPEAT_DELAY)
    }

    fun setButtonColors(
        textColor: Int,
        activeTextColor: Int,
        backgroundColor: Int,
        activeBackgroundColor: Int
    ) {
        buttonTextColor = textColor
        buttonActiveTextColor = activeTextColor
        buttonBackgroundColor = backgroundColor
        buttonActiveBackgroundColor = activeBackgroundColor
    }

    fun setLongPressTimeout(duration: Int) {
        longPressTimeout = when {
            duration >= MIN_LONG_PRESS_DURATION && duration <= MAX_LONG_PRESS_DURATION -> duration
            else -> FALLBACK_LONG_PRESS_DURATION
        }
    }

    fun setLongPressRepeatDelay(delay: Int) {
        longPressRepeatDelay = when {
            delay >= MIN_LONG_PRESS__REPEAT_DELAY && delay <= MAX_LONG_PRESS__REPEAT_DELAY -> delay
            else -> DEFAULT_LONG_PRESS_REPEAT_DELAY
        }
    }

    fun getDefaultSpecialButtons(): MutableMap<TermuxX11SpecialButton, TermuxX11SpecialButtonState> {
        return mutableMapOf(
            TermuxX11SpecialButton.CTRL to TermuxX11SpecialButtonState(this),
            TermuxX11SpecialButton.ALT to TermuxX11SpecialButtonState(this),
            TermuxX11SpecialButton.SHIFT to TermuxX11SpecialButtonState(this),
            TermuxX11SpecialButton.META to TermuxX11SpecialButtonState(this),
            TermuxX11SpecialButton.FN to TermuxX11SpecialButtonState(this)
        )
    }

    fun setSpecialButtons(buttons: MutableMap<TermuxX11SpecialButton, TermuxX11SpecialButtonState>) {
        specialButtons = buttons
        specialButtonsKeys = specialButtons.keys.map { it.key }.toSet()
    }

    fun setRepetitiveKeys(keys: List<String>) {
        repetitiveKeys = keys
    }

    @SuppressLint("ClickableViewAccessibility")
    fun reload(termuxX11ExtraKeysInfo: TermuxX11ExtraKeysInfo?, heightPx: Float) {
        if (termuxX11ExtraKeysInfo == null) return

        for (state in specialButtons.values) {
            state.buttons = ArrayList()
        }

        removeAllViews()

        val buttons = termuxX11ExtraKeysInfo.matrix

        rowCount = buttons.size
        columnCount = maximumLength(buttons)

        for (row in buttons.indices) {
            for (col in buttons[row].indices) {
                val buttonInfo = buttons[row][col]

                val button: Button
                if (isSpecialButton(buttonInfo)) {
                    button = createSpecialButton(buttonInfo.key, true)
                    if (button == null) return
                } else {
                    button = Button(context, null, android.R.attr.buttonBarButtonStyle)
                }

                button.background = object : ColorDrawable(Color.BLACK) {
                    override fun isStateful() = true
                    override fun hasFocusStateSpecified() = true
                }
                button.text = buttonInfo.display
                button.setTextColor(buttonTextColor)
                button.isAllCaps = buttonTextAllCaps
                button.setPadding(0, 0, 0, 0)

                button.setOnClickListener {
                    performExtraKeyButtonHapticFeedback(it, buttonInfo, button)
                    onAnyExtraKeyButtonClick(it, buttonInfo, button)
                }

                button.setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            view.setBackgroundColor(buttonActiveBackgroundColor)
                            startScheduledExecutors(view, buttonInfo, button)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (buttonInfo.popup != null) {
                                if (popupWindow == null && event.y < 0) {
                                    stopScheduledExecutors()
                                    view.setBackgroundColor(buttonBackgroundColor)
                                    showPopup(view, buttonInfo.popup)
                                }
                                if (popupWindow != null && event.y > 0) {
                                    view.setBackgroundColor(buttonActiveBackgroundColor)
                                    dismissPopup()
                                }
                            }
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            view.setBackgroundColor(buttonBackgroundColor)
                            stopScheduledExecutors()
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            view.setBackgroundColor(buttonBackgroundColor)
                            stopScheduledExecutors()
                            if (longPressCount == 0 || popupWindow != null) {
                                if (popupWindow != null) {
                                    dismissPopup()
                                    if (buttonInfo.popup != null) {
                                        onAnyExtraKeyButtonClick(view, buttonInfo.popup, button)
                                    }
                                } else {
                                    view.performClick()
                                }
                            }
                            true
                        }
                        else -> true
                    }
                }

                val param = LayoutParams()
                param.width = 0
                param.height = 0
                param.setMargins(0, 0, 0, 0)
                param.columnSpec = spec(col, GridLayout.FILL, 1f)
                param.rowSpec = spec(row, GridLayout.FILL, 1f)
                button.layoutParams = param

                addView(button)
            }
        }
    }

    fun performExtraKeyButtonHapticFeedback(view: View, buttonInfo: TermuxX11ExtraKeyButton, button: Button) {
        if (extraKeysViewClient != null) {
            if (extraKeysViewClient!!.performExtraKeyButtonHapticFeedback(view, buttonInfo, button)) {
                return
            }
        }

        if (Settings.System.getInt(context.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0) {

            if (Build.VERSION.SDK_INT >= 28) {
                button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } else {
                if (Settings.Global.getInt(context.contentResolver, "zen_mode", 0) != 2) {
                    button.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        }
    }

    fun onAnyExtraKeyButtonClick(view: View, @NonNull buttonInfo: TermuxX11ExtraKeyButton, button: Button) {
        if (isSpecialButton(buttonInfo)) {
            if (longPressCount > 0) return
            val state = specialButtons[TermuxX11SpecialButton.fromKey(buttonInfo.key)]
            if (state == null) return

            state.isActive = !state.isActive
            if (!state.isActive) {
                state.isLocked = false
            }
        } else {
            extraKeysViewClient?.onExtraKeyButtonClick(view, buttonInfo, button)
        }
    }

    fun startScheduledExecutors(view: View, buttonInfo: TermuxX11ExtraKeyButton, button: Button) {
        stopScheduledExecutors()
        longPressCount = 0

        if (repetitiveKeys.contains(buttonInfo.key)) {
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
            scheduledExecutor!!.scheduleWithFixedDelay({
                longPressCount++
                extraKeysViewClient?.onExtraKeyButtonClick(view, buttonInfo, button)
            }, longPressTimeout.toLong(), longPressRepeatDelay.toLong(), TimeUnit.MILLISECONDS)
        } else if (isSpecialButton(buttonInfo)) {
            val state = specialButtons[TermuxX11SpecialButton.fromKey(buttonInfo.key)]
            if (state == null) return

            if (handler == null) {
                handler = Handler(Looper.getMainLooper())
            }
            specialButtonsLongHoldRunnable = SpecialButtonsLongHoldRunnable(state)
            handler!!.postDelayed(specialButtonsLongHoldRunnable!!, longPressTimeout.toLong())
        }
    }

    fun stopScheduledExecutors() {
        scheduledExecutor?.shutdown()
        scheduledExecutor = null

        specialButtonsLongHoldRunnable?.let {
            handler?.removeCallbacks(it)
            specialButtonsLongHoldRunnable = null
        }
    }

    fun showPopup(view: View, extraButton: TermuxX11ExtraKeyButton) {
        val width = view.measuredWidth
        val height = view.measuredHeight

        val button: Button
        if (isSpecialButton(extraButton)) {
            button = createSpecialButton(extraButton.key, false) ?: return
        } else {
            button = Button(context, null, android.R.attr.buttonBarButtonStyle)
            button.setTextColor(buttonTextColor)
        }

        button.text = extraButton.display
        button.isAllCaps = buttonTextAllCaps
        button.setPadding(0, 0, 0, 0)
        button.minHeight = 0
        button.minWidth = 0
        button.minimumWidth = 0
        button.minimumHeight = 0
        button.width = width
        button.height = height
        button.setBackgroundColor(buttonActiveBackgroundColor)

        popupWindow = PopupWindow(this)
        popupWindow!!.width = LayoutParams.WRAP_CONTENT
        popupWindow!!.height = LayoutParams.WRAP_CONTENT
        popupWindow!!.contentView = button
        popupWindow!!.isOutsideTouchable = true
        popupWindow!!.isFocusable = false
        popupWindow!!.showAsDropDown(view, 0, -2 * height)
    }

    fun dismissPopup() {
        popupWindow?.contentView = null
        popupWindow?.dismiss()
        popupWindow = null
    }

    fun isSpecialButton(button: TermuxX11ExtraKeyButton): Boolean {
        return specialButtonsKeys.contains(button.key)
    }

    fun readSpecialButton(termuxX11SpecialButton: TermuxX11SpecialButton, autoSetInActive: Boolean): Boolean? {
        val state = specialButtons[termuxX11SpecialButton]
        if (state == null) return null

        if (!state.isCreated || !state.isActive) {
            return false
        }

        if (autoSetInActive && !state.isLocked) {
            state.isActive = false
        }

        return true
    }

    fun createSpecialButton(buttonKey: String, needUpdate: Boolean): Button? {
        val state = specialButtons[TermuxX11SpecialButton.fromKey(buttonKey)]
        if (state == null) return null
        state.isCreated = true

        val button = Button(context, null, android.R.attr.buttonBarButtonStyle)
        button.setTextColor(if (state.isActive) buttonActiveTextColor else buttonTextColor)

        if (needUpdate) {
            state.buttons.add(button)
        }
        return button
    }

    private fun maximumLength(matrix: Array<Array<TermuxX11ExtraKeyButton>>): Int {
        var max = 0
        for (row in matrix) {
            max = maxOf(max, row.size)
        }
        return max
    }

    inner class SpecialButtonsLongHoldRunnable(val mState: TermuxX11SpecialButtonState) : Runnable {
        override fun run() {
            mState.isLocked = !mState.isActive
            mState.isActive = !mState.isActive
            longPressCount++
        }
    }
}