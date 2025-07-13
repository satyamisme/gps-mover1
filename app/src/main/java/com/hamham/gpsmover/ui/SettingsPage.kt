package com.hamham.gpsmover.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.hamham.gpsmover.R
import com.hamham.gpsmover.utils.PrefManager
import com.hamham.gpsmover.viewmodel.MainViewModel
import com.hamham.gpsmover.utils.ext.showCustomSnackbar
import com.hamham.gpsmover.utils.ext.SnackbarType
import androidx.appcompat.app.AppCompatDelegate
import com.hamham.gpsmover.utils.ext.performHapticClick

class SettingsPage @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var viewModel: MainViewModel? = null
    private var onSettingsChanged: (() -> Unit)? = null
    private var onBackClick: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.fragment_settings, this, true)
        setupBackArrow()
        setupViews()
        updateSummaries()
    }

    fun setViewModel(viewModel: MainViewModel) {
        this.viewModel = viewModel
        updateSummaries()
    }

    fun setOnSettingsChangedListener(listener: () -> Unit) {
        this.onSettingsChanged = listener
    }

    fun setOnBackClick(listener: () -> Unit) {
        this.onBackClick = listener
    }

    private fun setupBackArrow() {
        val backArrow = findViewById<android.widget.ImageView>(R.id.settings_back_arrow)
        backArrow.setOnClickListener {
            onBackClick?.invoke()
        }
    }

    private fun setupViews() {
        // Dark Theme
        findViewById<MaterialButton>(R.id.dark_theme_button).setOnClickListener {
            it.performHapticClick()
            showDarkThemeDialog()
        }

        // Map Type
        findViewById<MaterialButton>(R.id.map_type_button).setOnClickListener {
            it.performHapticClick()
            showMapTypeDialog()
        }

        // Advanced Hook Switch
        findViewById<SwitchMaterial>(R.id.advance_hook_switch).apply {
            isChecked = PrefManager.isHookSystem
            setOnCheckedChangeListener { _, isChecked ->
                performHapticClick()
                PrefManager.isHookSystem = isChecked
                onSettingsChanged?.invoke()
            }
        }

        // Accuracy Settings
        findViewById<MaterialButton>(R.id.accuracy_button).setOnClickListener {
            it.performHapticClick()
            showAccuracyDialog()
        }

        // Random Position
        findViewById<MaterialButton>(R.id.random_position_button).setOnClickListener {
            it.performHapticClick()
            showRandomPositionDialog()
        }

        // Developer dialog with custom view
        findViewById<android.widget.ImageView>(R.id.settings_developer_button).setOnClickListener {
            it.performHapticClick()
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_about_developer, null)
            val supportButton = dialogView.findViewById<LinearLayout>(R.id.support_button)
            supportButton.setOnClickListener { btn ->
                btn.performHapticClick()
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://www.paypal.com/paypalme/mohammedhamham")
                }
                context.startActivity(intent)
            }
            
            val dialog = MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create()
            
            // Make dialog dismissible by tapping outside
            dialog.setCanceledOnTouchOutside(true)
            dialog.show()
        }
    }

    private fun updateSummaries() {
        // Dark Theme Summary
        val darkThemeSummary = when (PrefManager.darkTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> context.getString(R.string.light)
            AppCompatDelegate.MODE_NIGHT_YES -> context.getString(R.string.dark)
            else -> context.getString(R.string.system)
        }
        findViewById<TextView>(R.id.dark_theme_summary).text = darkThemeSummary

        // Map Type Summary
        val mapTypeSummary = when (PrefManager.mapType) {
            1 -> "Normal"
            2 -> "Satellite"
            3 -> "Terrain"
            else -> "Normal"
        }
        findViewById<TextView>(R.id.map_type_summary).text = mapTypeSummary

        // Accuracy Summary
        findViewById<TextView>(R.id.accuracy_summary).text = "${PrefManager.accuracy} m."

        // Random Position Summary
        val randomSummary = if (PrefManager.isRandomPosition) {
            "${PrefManager.randomPositionRange} m."
        } else {
            "Disabled"
        }
        findViewById<TextView>(R.id.random_position_summary).text = randomSummary
    }

    private fun showDarkThemeDialog() {
        val themes = arrayOf(context.getString(R.string.system), context.getString(R.string.light), context.getString(R.string.dark))
        val currentIndex = when (PrefManager.darkTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.dark_theme))
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                val newMode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                if (PrefManager.darkTheme != newMode) {
                    PrefManager.darkTheme = newMode
                    AppCompatDelegate.setDefaultNightMode(newMode)
                    onSettingsChanged?.invoke()
                }
                updateSummaries()
                dialog.dismiss()
            }
            .show()
    }

    private fun showMapTypeDialog() {
        val mapTypes = arrayOf("Normal", "Satellite", "Terrain")
        val currentIndex = PrefManager.mapType - 1

        MaterialAlertDialogBuilder(context)
            .setTitle("Map Type")
            .setSingleChoiceItems(mapTypes, currentIndex) { dialog, which ->
                PrefManager.mapType = which + 1
                viewModel?.updateMapType(PrefManager.mapType)
                onSettingsChanged?.invoke()
                updateSummaries()
                dialog.dismiss()
            }
            .show()
    }

    private fun showAccuracyDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_accuracy, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.accuracy_edit)
        
        editText.setText(PrefManager.accuracy)
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val newValue = editText.text.toString()
                if (newValue.isNotEmpty()) {
                    PrefManager.accuracy = newValue
                    onSettingsChanged?.invoke()
                    updateSummaries()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRandomPositionDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_random_position, null)
        val switchView = dialogView.findViewById<SwitchMaterial>(R.id.random_position_switch)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.random_position_edit)
        val statusText = dialogView.findViewById<TextView>(R.id.random_position_status_text)

        // Set current values
        switchView.isChecked = PrefManager.isRandomPosition
        val currentValue = PrefManager.randomPositionRange?.ifEmpty { "2" } ?: "2"
        editText.setText(currentValue)

        // Update initial state
        updateRandomPositionUI(switchView.isChecked, statusText)

        // Handle switch changes
        switchView.setOnCheckedChangeListener { _, isChecked ->
            performHapticClick()
            PrefManager.isRandomPosition = isChecked
            updateRandomPositionUI(isChecked, statusText)
            onSettingsChanged?.invoke()
        }

        // Handle text input
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                try {
                    val newValue = editText.text.toString()
                    if (newValue.isNotEmpty()) {
                        PrefManager.randomPositionRange = newValue
                        onSettingsChanged?.invoke()
                        updateSummaries()
                    }
                } catch (e: Exception) {
                    context.showCustomSnackbar(context.getString(R.string.enter_valid_input), SnackbarType.ERROR)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRandomPositionUI(isEnabled: Boolean, statusText: TextView) {
        statusText.text = if (isEnabled) "Enabled" else "Disabled"
    }
} 