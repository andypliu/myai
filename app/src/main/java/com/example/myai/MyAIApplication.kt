package com.example.myai

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Manual initialization to avoid crash when google-services.json is missing.
        // This allows the app to start and use on-device features.
        try {
            FirebaseApp.getInstance()
        } catch (e: IllegalStateException) {
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:abc1234567890") // Proper format
                .setProjectId("myai-local")
                .setApiKey("unused_but_required_format_key")
                .setGcmSenderId("1234567890")
                .build()
            FirebaseApp.initializeApp(this, options)
        }
    }
}
