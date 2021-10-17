package com.grandfatherpikhto.ledstrip.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.helper.AppConst
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

object BtLeScanServiceConnector:ServiceConnection {
    const val TAG = "ScannerConnector"

    /** Геттер для сервиса */
    private val sharedService = MutableStateFlow<BtLeScanService?>(null)
    val service: StateFlow<BtLeScanService> get() = sharedService as StateFlow<BtLeScanService>

    private val sharedDevice = MutableSharedFlow<BtLeDevice>(
        replay = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val device:SharedFlow<BtLeDevice> = sharedDevice

    private val sharedState = MutableStateFlow<BtLeScanService.State>(BtLeScanService.State.Stop)
    val state = sharedState


    @DelicateCoroutinesApi
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override fun onServiceConnected(componentName: ComponentName?, binderService: IBinder?) {
        val btLeScanService = (binderService as BtLeScanService.LocalBinder).getService()
        GlobalScope.launch {
            sharedService.tryEmit(btLeScanService)
            btLeScanService!!.state.collect { state ->
                sharedState.tryEmit(state)
            }
        }
        GlobalScope.launch {
            btLeScanService!!.device.collect{ item ->
                sharedDevice.tryEmit(item)
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        GlobalScope.launch {
            sharedService.tryEmit(null)
        }
    }

    fun stop() {
        sharedService.value?.stopScan()
    }
}