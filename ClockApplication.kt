package com.package com.example.dementiaclock

import android.app.Application
import com.google.firebase.FirebaseApp

class ClockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}