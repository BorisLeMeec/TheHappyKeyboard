package com.example.thehappykeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

fun isMyImeEnabled(context: Context, imeId: String): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledImeList = imm.enabledInputMethodList

    for (ime in enabledImeList) {
        if (ime.id == imeId) {
            return true
        }
    }
    return false
}

class SettingsActivity : AppCompatActivity() {
    private lateinit var imeEnabledSwitch: SwitchCompat
    private lateinit var imeEnabledLayout: LinearLayout
    private lateinit var myImeId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        imeEnabledSwitch = findViewById(R.id.imeEnabledSwitch)
        imeEnabledLayout = findViewById(R.id.imeEnabledLayout)

        myImeId = packageName + "/." + TheHappyKeyboardService::class.java.simpleName

        updateImeSwitchState()
        imeEnabledLayout.setOnClickListener {
            if (!isMyImeEnabled(this, myImeId)) {
                showEnableImeDialog()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateImeSwitchState()
    }

    private fun updateImeSwitchState() {
        val isEnabled = isMyImeEnabled(this, myImeId)
        imeEnabledSwitch.isChecked = isEnabled
    }

    private fun showEnableImeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable TheHappyKeyboard")
            .setMessage("You need to enable TheHappyKeyboard in your Android settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                openImeSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openImeSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent)
    }
}