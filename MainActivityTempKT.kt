package com.example.dementiaclock

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ImageView
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import com.example.dementiaclock.weather.WeatherClient
import java.text.DecimalFormat
import retrofit2.Response
import java.net.HttpURLConnection
import java.net.URL
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var timeText: TextView
    private lateinit var dayText: TextView
    private lateinit var dateText: TextView
    private lateinit var messageText: TextView
    private lateinit var weatherText: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var settingsButton: ImageButton
    private val handler = Handler(Looper.getMainLooper())
    private var hideButtonRunnable: Runnable? = null
    private val weatherScope = CoroutineScope(Dispatchers.Main + Job())

    // Firebase references
    private val database = Firebase.database("https://dementiaclock-5f752-default-rtdb.firebaseio.com")
    private val settingsRef = database.getReference("settings")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase persistence and check connection
        Firebase.database.setPersistenceEnabled(true)
        database.reference.child(".info/connected").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                Log.d("FirebaseDebug", "Connected to Firebase: $connected")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "Connection listener was cancelled", error.toException())
            }
        })

        setContentView(R.layout.activity_main)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize views
        timeText = findViewById(R.id.timeText)
        dayText = findViewById(R.id.dayText)
        dateText = findViewById(R.id.dateText)
        messageText = findViewById(R.id.messageText)
        weatherText = findViewById(R.id.weatherText)
        weatherIcon = findViewById(R.id.weatherIcon)
        settingsButton = findViewById(R.id.settingsButton)

        // Debug log for views initialization
        Log.d("FirebaseDebug", "Views initialized")

        // Ensure weather views are visible
        weatherText.visibility = View.VISIBLE
        weatherIcon.visibility = View.VISIBLE

        // Settings button
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Make the entire frame layout area respond to long press
        findViewById<FrameLayout>(android.R.id.content).setOnLongClickListener {
            showSettingsButton()
            true
        }

        // Start updating time
        startTimeUpdates()

        // Start weather updates
        startWeatherUpdates()

        // Initialize Firebase listeners
        setupFirebaseListeners()

        Log.d("FirebaseDebug", "onCreate completed")
    }

    private fun setupFirebaseListeners() {
        Log.d("FirebaseDebug", "Setting up Firebase listeners")

        // Listen for location changes
        settingsRef.child("location").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newLocation = snapshot.getValue(String::class.java)
                Log.d("FirebaseDebug", "New location received: $newLocation")
                if (!newLocation.isNullOrEmpty()) {
                    getSharedPreferences("DementiaClockPrefs", MODE_PRIVATE).edit()
                        .putString("location", newLocation)
                        .apply()
                    // Update weather with new location
                    weatherScope.launch {
                        updateWeather()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "Location listener cancelled", error.toException())
            }
        })

        // Listen for broadcast messages
        settingsRef.child("broadcast_message").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val broadcastMessage = snapshot.getValue(String::class.java)
                Log.d("FirebaseDebug", "Broadcast message received: $broadcastMessage")
                if (!broadcastMessage.isNullOrEmpty()) {
                    messageText.text = broadcastMessage
                    adjustMessageTextSize(broadcastMessage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseDebug", "Broadcast message listener cancelled", error.toException())
            }
        })

        // Listen for time-specific messages
        val messageTypes = listOf("morning", "afternoon", "evening", "night")
        messageTypes.forEach { timeOfDay ->
            settingsRef.child("message_$timeOfDay").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newMessage = snapshot.getValue(String::class.java)
                    Log.d("FirebaseDebug", "New $timeOfDay message received: $newMessage")
                    if (!newMessage.isNullOrEmpty()) {
                        getSharedPreferences("DementiaClockPrefs", MODE_PRIVATE).edit()
                            .putString("message_$timeOfDay", newMessage)
                            .apply()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseDebug", "$timeOfDay message listener cancelled", error.toException())
                }
            })
        }
    }

    private fun showSettingsButton() {
        settingsButton.visibility = View.VISIBLE

        // Cancel any existing hide operation
        hideButtonRunnable?.let { handler.removeCallbacks(it) }

        // Create new hide operation
        hideButtonRunnable = Runnable {
            settingsButton.visibility = View.INVISIBLE
        }

        // Schedule the button to hide after 5 seconds
        handler.postDelayed(hideButtonRunnable!!, 5000)
    }

    private fun startTimeUpdates() {
        Thread {
            while (true) {
                updateDateTime()
                Thread.sleep(1000)
            }
        }.start()
    }

    private fun startWeatherUpdates() {
        weatherScope.launch {
            while (true) {
                updateWeather()
                delay(900000) // Update every 15 minutes
            }
        }
    }

    private suspend fun updateWeather() {
        try {
            val location = getSharedPreferences("DementiaClockPrefs", MODE_PRIVATE)
                .getString("location", "New York")?.takeIf { it.isNotBlank() } ?: "New York"

            Log.d("WeatherDebug", "Starting weather update for location: $location")

            val response = withContext(Dispatchers.IO) {
                WeatherClient.service.getWeather(
                    city = location,
                    apiKey = "51a01a11da38fcb12d5327773921af90",
                    units = "imperial"
                )
            }

            Log.d("WeatherDebug", "Got response from API: ${response.isSuccessful}")

            withContext(Dispatchers.Main) {
                if (response.isSuccessful && response.body() != null) {
                    val weather = response.body()!!
                    val temp = DecimalFormat("#.0").format(weather.main.temp)
                    val description = weather.weather.firstOrNull()?.description?.capitalize() ?: ""
                    val iconCode = weather.weather.firstOrNull()?.icon ?: ""

                    Log.d("WeatherDebug", "Setting weather text: $temp°F - $description")

                    weatherText.text = "$temp°F - $description"
                    weatherText.visibility = View.VISIBLE
                    weatherText.invalidate()
                    weatherText.requestLayout()

                    // Load weather icon
                    val iconUrl = "https://openweathermap.org/img/wn/${iconCode}@2x.png"
                    Log.d("WeatherDebug", "Loading icon from: $iconUrl")
                    weatherIcon.visibility = View.VISIBLE
                    loadWeatherIcon(iconUrl)
                } else {
                    Log.e("WeatherDebug", "API Error: ${response.code()} - ${response.errorBody()?.string()}")
                    weatherText.text = "Weather Unavailable"
                    weatherText.visibility = View.VISIBLE
                    weatherIcon.setImageDrawable(null)
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherDebug", "Error fetching weather", e)
            withContext(Dispatchers.Main) {
                weatherText.text = "Weather Unavailable"
                weatherText.visibility = View.VISIBLE
                weatherIcon.setImageDrawable(null)
            }
        }
    }

    private fun loadWeatherIcon(iconUrl: String) {
        weatherScope.launch(Dispatchers.IO) {
            try {
                val url = URL(iconUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()

                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)

                withContext(Dispatchers.Main) {
                    weatherIcon.setImageBitmap(bitmap)
                    weatherIcon.visibility = View.VISIBLE
                    Log.d("WeatherDebug", "Icon loaded successfully")
                }
            } catch (e: Exception) {
                Log.e("WeatherDebug", "Error loading icon", e)
                withContext(Dispatchers.Main) {
                    weatherIcon.setImageDrawable(null)
                }
            }
        }
    }

    private fun updateDateTime() {
        runOnUiThread {
            val calendar = Calendar.getInstance()

            // Determine time of day
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val timeOfDay = when (hour) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }

            // Update time with period of day
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeText.text = "${timeFormat.format(calendar.time)} - $timeOfDay"

            // Update day
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayText.text = dayFormat.format(calendar.time)

            // Update date
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            dateText.text = dateFormat.format(calendar.time)

            // Update message based on time of day
            val sharedPrefs = getSharedPreferences("DementiaClockPrefs", MODE_PRIVATE)
            val message = when (timeOfDay) {
                "Morning" -> sharedPrefs.getString("message_morning", "Good morning! Have a wonderful day!")
                "Afternoon" -> sharedPrefs.getString("message_afternoon", "Good afternoon! I hope you're having a nice day!")
                "Evening" -> sharedPrefs.getString("message_evening", "Good evening! Time to start winding down.")
                else -> sharedPrefs.getString("message_night", "Good night! Time to rest.")
            }
            messageText.text = message
            adjustMessageTextSize(message ?: "")
        }
    }

    private fun adjustMessageTextSize(message: String) {
        messageText.post {
            var targetTextSize = 90f * resources.displayMetrics.scaledDensity
            val minTextSize = 40f * resources.displayMetrics.scaledDensity

            val availableWidth = messageText.width - messageText.paddingLeft - messageText.paddingRight
            val availableHeight = messageText.height - messageText.paddingTop - messageText.paddingBottom

            while (targetTextSize > minTextSize) {
                messageText.setTextSize(TypedValue.COMPLEX_UNIT_PX, targetTextSize)

                messageText.measure(
                    View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(availableHeight, View.MeasureSpec.AT_MOST)
                )

                if (messageText.lineCount <= 4 &&
                    messageText.measuredHeight <= availableHeight &&
                    messageText.measuredWidth <= availableWidth) {
                    break
                }

                targetTextSize -= 2f
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update weather immediately when resuming
        weatherScope.launch {
            updateWeather()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        weatherScope.cancel()
    }
}