package com.grandfatherpikhto.ledstrip.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.helper.AppConst
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
object BtLeScanServiceConnector:ServiceConnection {
    const val TAG = "ScannerConnector"

    /** Геттер для сервиса */
    private var btLeScanService:BtLeScanService ?= null
    val service:BtLeScanService? get() = btLeScanService

    private val isBound = MutableStateFlow(false)
    val bound:StateFlow<Boolean> get() = isBound.asStateFlow()

    private val sharedDevice = MutableSharedFlow<BtLeDevice>(
        replay = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val device:SharedFlow<BtLeDevice> = sharedDevice

    private val sharedState = MutableStateFlow<BtLeScanService.State>(BtLeScanService.State.Stop)
    val state = sharedState


    override fun onServiceConnected(componentName: ComponentName?, binderService: IBinder?) {
        btLeScanService = (binderService as BtLeScanService.LocalBinder).getService()
        isBound.tryEmit(true)

        GlobalScope.launch {
            btLeScanService!!.state.collect { state ->
                sharedState.tryEmit(state)
            }
        }
        GlobalScope.launch {
            btLeScanService!!.device.collect{ item ->
                if(item != null) {
                    sharedDevice.tryEmit(item)
                }
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        GlobalScope.launch {
            isBound.tryEmit(false)
        }
    }
}