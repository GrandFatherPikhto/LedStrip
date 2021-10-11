package com.grandfatherpikhto.ledstrip

import android.app.Application
import android.util.Log

class LedstripApplication: Application() {
    companion object {
        const val TAG = "LedstripApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "onTerminate()")
    }
}