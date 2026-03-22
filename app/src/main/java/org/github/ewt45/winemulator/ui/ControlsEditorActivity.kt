package org.github.ewt45.winemulator.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.github.ewt45.winemulator.R
import org.github.ewt45.winemulator.inputcontrols.Binding
import org.github.ewt45.winemulator.inputcontrols.ControlElement
import org.github.ewt45.winemulator.inputcontrols.ControlsProfile
import org.github.ewt45.winemulator.inputcontrols.InputControlsManager
import org.github.ewt45.winemulator.inputcontrols.InputControlsView
import java.io.IOException
import java.io.InputStream
import java.util.Arrays

class ControlsEditorActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var inputControlsView: InputControlsView
    private var profile: ControlsProfile? = null

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.controls_editor_activity)

        inputControlsView = InputControlsView(this)
        inputControlsView.setEditMode(true)
        inputControlsView.setOverlayOpacity(0.6f)

        val profileId = intent.getIntExtra(EXTRA_PROFILE_ID, 0)
        profile = ControlsProfile.loadProfile(this, ControlsProfile.getProfileFile(this, profileId))

        if (profile == null) {
            Toast.makeText(this, R.string.no_profile_selected, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.TVProfileName).text = profile!!.name
        inputControlsView.setProfile(profile)

        val container = findViewById<FrameLayout>(R.id.FLContainer)
        container.addView(inputControlsView, 0)

        container.findViewById<View>(R.id.BTAddElement).setOnClickListener(this)
        container.findViewById<View>(R.id.BTRemoveElement).setOnClickListener(this)
        container.findViewById<View>(R.id.BTElementSettings).setOnClickListener(this)
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.BTAddElement -> {
                if (!inputControlsView.addElement()) {
                    Toast.makeText(this, R.string.no_profile_selected, Toast.LENGTH_SHORT).show()
                }
            }
            R.id.BTRemoveElement -> {
                if (!inputControlsView.removeElement()) {
                    Toast.makeText(this, R.string.no_control_element_selected, Toast.LENGTH_SHORT).show()
                }
            }
            R.id.BTElementSettings -> {
                val selectedElement = inputControlsView.getSelectedElement()
                if (selectedElement != null) {
                    showControlElementSettings(v, selectedElement)
                } else {
                    Toast.makeText(this, R.string.no_control_element_selected, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showControlElementSettings(anchorView: View, element: ControlElement) {
        val view = LayoutInflater.from(this).inflate(R.layout.control_element_settings, null)

        val updateLayout = Runnable {
            val type = element.type
            view.findViewById<View>(R.id.LLShape)?.visibility = if (type == ControlElement.Type.BUTTON) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.CBToggleSwitch)?.visibility = if (type == ControlElement.Type.BUTTON) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.LLCustomTextIcon)?.visibility = if (type == ControlElement.Type.BUTTON) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.LLRangeOptions)?.visibility = if (type == ControlElement.Type.RANGE_BUTTON) View.VISIBLE else View.GONE

            loadBindingSpinners(element, view.findViewById(R.id.LLBindings))
        }

        loadTypeSpinner(element, view.findViewById(R.id.SType), updateLayout)
        loadShapeSpinner(element, view.findViewById<Spinner>(R.id.SShape).apply {
            visibility = if (element.type == ControlElement.Type.BUTTON) View.VISIBLE else View.GONE
        })
        loadRangeSpinner(element, view.findViewById<Spinner>(R.id.SRange).apply {
            visibility = if (element.type == ControlElement.Type.RANGE_BUTTON) View.VISIBLE else View.GONE
        })

        val rgOrientation = view.findViewById<RadioGroup>(R.id.RGOrientation)
        rgOrientation.check(if (element.orientation == 1.toByte()) R.id.RBVertical else R.id.RBHorizontal)
        rgOrientation.setOnCheckedChangeListener { _, checkedId ->
            element.orientation = if (checkedId == R.id.RBVertical) 1 else 0
            profile?.save()
            inputControlsView.invalidate()
        }

        val tvScale = view.findViewById<TextView>(R.id.TVScale)
        val sbScale = view.findViewById<SeekBar>(R.id.SBScale)
        sbScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvScale.text = "$progress%"
                if (fromUser) {
                    element.scale = progress / 100.0f
                    profile?.save()
                    inputControlsView.invalidate()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        sbScale.progress = (element.scale * 100).toInt()

        val cbToggleSwitch = view.findViewById<CheckBox>(R.id.CBToggleSwitch)
        cbToggleSwitch.isChecked = element.isToggleSwitch
        cbToggleSwitch.visibility = if (element.type == ControlElement.Type.BUTTON) View.VISIBLE else View.GONE
        cbToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            element.isToggleSwitch = isChecked
            profile?.save()
        }

        val etCustomText = view.findViewById<EditText>(R.id.ETCustomText)
        etCustomText.setText(element.text)
        etCustomText.visibility = if (element.type == ControlElement.Type.BUTTON) View.VISIBLE else View.GONE

        val llIconList = view.findViewById<LinearLayout>(R.id.LLIconList)
        llIconList.visibility = if (element.type == ControlElement.Type.BUTTON) View.VISIBLE else View.GONE
        loadIcons(llIconList, element.iconId)

        updateLayout.run()

        val popupWindow = PopupWindow(view, 340.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popupWindow.showAsDropDown(anchorView)

        popupWindow.setOnDismissListener {
            val text = etCustomText.text.toString().trim()
            var iconId: Byte = 0
            for (i in 0 until llIconList.childCount) {
                val child = llIconList.getChildAt(i)
                if (child.isSelected) {
                    iconId = child.tag as? Byte ?: 0
                    break
                }
            }

            element.text = text
            element.iconId = iconId
            profile?.save()
            inputControlsView.invalidate()
        }
    }

    private fun loadTypeSpinner(element: ControlElement, spinner: Spinner, callback: Runnable) {
        val typeNames = ControlElement.Type.entries.map { it.name.replace("_", "-") }.toTypedArray()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, typeNames)
        spinner.setSelection(element.type.ordinal, false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                element.type = ControlElement.Type.entries[position]
                profile?.save()
                callback.run()
                inputControlsView.invalidate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadShapeSpinner(element: ControlElement, spinner: Spinner) {
        val shapeNames = ControlElement.Shape.entries.map { it.name.replace("_", " ") }.toTypedArray()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shapeNames)
        spinner.setSelection(element.shape.ordinal, false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                element.shape = ControlElement.Shape.entries[position]
                profile?.save()
                inputControlsView.invalidate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadRangeSpinner(element: ControlElement, spinner: Spinner) {
        val rangeNames = ControlElement.Range.entries.map { it.name.replace("_", " ") }.toTypedArray()
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, rangeNames)
        if (element.range != null) {
            spinner.setSelection(element.range!!.ordinal, false)
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                element.range = ControlElement.Range.entries[position]
                profile?.save()
                inputControlsView.invalidate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadBindingSpinners(element: ControlElement, container: LinearLayout) {
        container.removeAllViews()

        when (element.type) {
            ControlElement.Type.BUTTON -> {
                loadBindingSpinner(element, container, 0, getString(R.string.binding))
                loadBindingSpinner(element, container, 1, getString(R.string.binding_secondary))
            }
            ControlElement.Type.D_PAD, ControlElement.Type.STICK, ControlElement.Type.TRACKPAD -> {
                loadBindingSpinner(element, container, 0, getString(R.string.binding_up))
                loadBindingSpinner(element, container, 1, getString(R.string.binding_right))
                loadBindingSpinner(element, container, 2, getString(R.string.binding_down))
                loadBindingSpinner(element, container, 3, getString(R.string.binding_left))
            }
            else -> {}
        }
    }

    private fun loadBindingSpinner(element: ControlElement, container: LinearLayout, index: Int, titleResId: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.binding_field, container, false)
        (view.findViewById<TextView>(R.id.TVTitle)).setText(titleResId)
        val sBindingType = view.findViewById<Spinner>(R.id.SBindingType)
        val sBinding = view.findViewById<Spinner>(R.id.SBinding)

        val update = Runnable {
            val bindingEntries: Array<String>
            when (sBindingType.selectedItemPosition) {
                0 -> bindingEntries = Binding.keyboardBindingLabels()
                1 -> bindingEntries = Binding.mouseBindingLabels()
                2 -> bindingEntries = Binding.gamepadBindingLabels()
                else -> bindingEntries = Binding.keyboardBindingLabels()
            }

            sBinding.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bindingEntries)
            setSpinnerSelectionFromValue(sBinding, element.getBindingAt(index).name)
        }

        sBindingType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                update.run()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val selectedBinding = element.getBindingAt(index)
        sBindingType.setSelection(
            when {
                selectedBinding.isKeyboard -> 0
                selectedBinding.isMouse -> 1
                selectedBinding.isGamepad -> 2
                else -> 0
            }, false
        )

        sBinding.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val binding = when (sBindingType.selectedItemPosition) {
                    0 -> Binding.keyboardBindingValues()[position]
                    1 -> Binding.mouseBindingValues()[position]
                    2 -> Binding.gamepadBindingValues()[position]
                    else -> Binding.NONE
                }

                if (binding != element.getBindingAt(index)) {
                    element.setBindingAt(index, binding)
                    profile?.save()
                    inputControlsView.invalidate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        update.run()
        container.addView(view)
    }

    private fun setSpinnerSelectionFromValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == value) {
                spinner.setSelection(i, false)
                return
            }
        }
    }

    private fun loadIcons(parent: LinearLayout, selectedId: Byte) {
        var iconIds = byteArrayOf()
        try {
            val filenames = assets.list("inputcontrols/icons/") ?: return
            iconIds = ByteArray(filenames.size)
            for (i in filenames.indices) {
                iconIds[i] = filenames[i].substringBefore(".").toByteOrNull() ?: 0
            }
        } catch (e: IOException) {}

        Arrays.sort(iconIds)

        val size = (40 * resources.displayMetrics.density).toInt()
        val margin = (2 * resources.displayMetrics.density).toInt()
        val padding = (4 * resources.displayMetrics.density).toInt()
        val params = LinearLayout.LayoutParams(size, size)
        params.setMargins(margin, 0, margin, 0)

        for (id in iconIds) {
            val imageView = ImageView(this)
            imageView.layoutParams = params
            imageView.setPadding(padding, padding, padding, padding)
            imageView.setBackgroundResource(R.drawable.icon_background)
            imageView.tag = id
            imageView.isSelected = id == selectedId
            imageView.setOnClickListener {
                for (i in 0 until parent.childCount) {
                    parent.getChildAt(i).isSelected = false
                }
                imageView.isSelected = true
            }

            try {
                assets.open("inputcontrols/icons/$id.png").use { `is` ->
                    imageView.setImageBitmap(BitmapFactory.decodeStream(`is`))
                }
            } catch (e: IOException) {}

            parent.addView(imageView)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
