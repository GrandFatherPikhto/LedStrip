package com.grandfatherpikhto.ledstrip

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector

class LedstripApplication: Application() {
    companion object {
        const val TAG = "LedstripApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
    }

    override fun onTerminate() {
        Log.d(TAG, "onTerminate()")
        super.onTerminate()
    }
}