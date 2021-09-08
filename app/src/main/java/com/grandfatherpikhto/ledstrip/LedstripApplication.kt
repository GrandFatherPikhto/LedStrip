package com.grandfatherpikhto.ledstrip

import android.app.Application
import android.content.*
import android.os.IBinder
import android.util.Log
import com.grandfatherpikhto.ledstrip.service.BluetoothLeService
import com.grandfatherpikhto.ledstrip.ui.MainActivity

class LedstripApplication: Application() {
    companion object {
        const val TAG:String = "LedstripApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "Приложение создано")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.e(TAG, "Приложение уничтожено")
    }
}