package com.example.dementiaclock

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var locationEdit: EditText
    private lateinit var morningMessageEdit: EditText
    private lateinit var afternoonMessageEdit: EditText
    private lateinit var eveningMessageEdit: EditText
    private lateinit var nightMessageEdit: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        // Initialize views
        locationEdit = findViewById(R.id.locationEdit)
        morningMessageEdit = findViewById(R.id.morningMessageEdit)
        afternoonMessageEdit = findViewById(R.id.afternoonMessageEdit)
        eveningMessageEdit = findViewById(R.id.eveningMessageEdit)
        nightMessageEdit = findViewById(R.id.nightMessageEdit)
        saveButton = findViewById(R.id.saveButton)

        // Load current settings
        val sharedPrefs = getSharedPreferences("DementiaClockPrefs", MODE_PRIVATE)

        // Load location with a default of "New York" instead of "Home"
        val savedLocation = sharedPrefs.getString("location", "New York") ?: "New York"
        Log.d("WeatherDebug", "Loading saved location: $savedLocation")
        locationEdit.setText(savedLocation)

        morningMessageEdit.setText(sharedPrefs.getString("message_morning", "Good morning! Have a wonderful day!"))
        afternoonMessageEdit.setText(sharedPrefs.getString("message_afternoon", "Good afternoon! I hope you're having a nice day!"))
        eveningMessageEdit.setText(sharedPrefs.getString("message_evening", "Good evening! Time to start winding down."))
        nightMessageEdit.setText(sharedPrefs.getString("message_night", "Good night! Time to rest."))

        // Save button click handler
        saveButton.setOnClickListener {
            val newLocation = locationEdit.text.toString().trim()
            Log.d("WeatherDebug", "Saving new location: $newLocation")

            // Save settings
            sharedPrefs.edit().apply {
                putString("location", newLocation)
                putString("message_morning", morningMessageEdit.text.toString())
                putString("message_afternoon", afternoonMessageEdit.text.toString())
                putString("message_evening", eveningMessageEdit.text.toString())
                putString("message_night", nightMessageEdit.text.toString())
                apply()
            }

            // Close settings activity
            finish()
        }
    }
}