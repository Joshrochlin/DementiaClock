package com.example.dementiaclock

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val locationInput = findViewById<EditText>(R.id.locationInput)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // Load current values
        val sharedPrefs = getSharedPreferences("DementiaClockPrefs", MODE_PRIVATE)
        locationInput.setText(sharedPrefs.getString("location", "Home"))
        messageInput.setText(sharedPrefs.getString("message", "Have a wonderful day!"))

        saveButton.setOnClickListener {
            // Save new values
            sharedPrefs.edit().apply {
                putString("location", locationInput.text.toString())
                putString("message", messageInput.text.toString())
                apply()
            }
            finish()  // Close settings and return to clock
        }
    }
}