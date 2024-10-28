package com.example.dementiaclock

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton  // Add this instead of Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var timeText: TextView
    private lateinit var dayText: TextView
    private lateinit var dateText: TextView
    private lateinit var locationText: TextView
    private lateinit var messageText: TextView
    private lateinit var settingsButton: ImageButton
    private val handler = Handler(Looper.getMainLooper())
    private var hideButtonRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize views
        timeText = findViewById(R.id.timeText)
        dayText = findViewById(R.id.dayText)
        dateText = findViewById(R.id.dateText)
        locationText = findViewById(R.id.locationText)
        messageText = findViewById(R.id.messageText)
        settingsButton = findViewById(R.id.settingsButton)
        val touchArea = findViewById<View>(R.id.touchArea)

        // Settings button
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Long press detection
        touchArea.setOnLongClickListener {
            showSettingsButton()
            true
        }

        // Start updating time
        startTimeUpdates()
    }

    override fun onResume() {
        super.onResume()
        // Load saved settings
        val sharedPrefs = getSharedPreferences("DementiaClockPrefs", MODE_PRIVATE)
        locationText.text = sharedPrefs.getString("location", "Home")
        val message = sharedPrefs.getString("message", "Have a wonderful day!")
        messageText.text = message
        adjustMessageTextSize(message ?: "")
    }

    private fun adjustMessageTextSize(message: String) {
        messageText.post {
            // Start with a larger initial size
            var targetTextSize = 90f * resources.displayMetrics.scaledDensity  // Starting with 90sp
            val minTextSize = 40f * resources.displayMetrics.scaledDensity  // Minimum 40sp

            val availableWidth = messageText.width - messageText.paddingLeft - messageText.paddingRight
            val availableHeight = messageText.height - messageText.paddingTop - messageText.paddingBottom

            // Binary search for the best text size
            while (targetTextSize > minTextSize) {
                messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetTextSize)

                // Create a layout for measuring
                messageText.measure(
                    availableWidth + messageText.paddingLeft + messageText.paddingRight,
                    availableHeight + messageText.paddingTop + messageText.paddingBottom
                )

                // Check if text fits within the TextView
                if (messageText.lineCount <= 4 &&
                    messageText.measuredHeight <= availableHeight &&
                    messageText.measuredWidth <= availableWidth) {
                    break
                }

                targetTextSize -= 2f
            }
        }
    }

    private fun showSettingsButton() {
        runOnUiThread {
            settingsButton.visibility = View.VISIBLE
            findViewById<View>(R.id.touchArea).visibility = View.GONE  // Hide touch area

            // Cancel any existing hide operation
            hideButtonRunnable?.let { handler.removeCallbacks(it) }

            // Create new hide operation
            hideButtonRunnable = Runnable {
                settingsButton.visibility = View.INVISIBLE
                findViewById<View>(R.id.touchArea).visibility = View.VISIBLE  // Show touch area again
            }

            // Schedule the button to hide after 5 seconds
            handler.postDelayed(hideButtonRunnable!!, 5000) // 5 seconds
        }
    }

    private fun startTimeUpdates() {
        Thread {
            while (true) {
                updateDateTime()
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun updateDateTime() {
        runOnUiThread {
            val calendar = Calendar.getInstance()

            // Update time
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeText.text = timeFormat.format(calendar.time)

            // Update day
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayText.text = dayFormat.format(calendar.time)

            // Update date
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            dateText.text = dateFormat.format(calendar.time)
        }
    }
}