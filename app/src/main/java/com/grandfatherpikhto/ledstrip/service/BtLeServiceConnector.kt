package com.grandfatherpikhto.ledstrip.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object BtLeServiceConnector:ServiceConnection {
    const val TAG = "BtLeServiceConnector"

    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService

    private var sharedBond = MutableStateFlow<Boolean>(false)
    val bond:StateFlow<Boolean> = sharedBond

    private val sharedState = MutableStateFlow<BtLeService.State>(BtLeService.State.Disconnected)
    val state:StateFlow<BtLeService.State> = sharedState

    private val sharedRegime = MutableSharedFlow<BtLeService.Regime>(replay = BtLeService.SHARED_REGIME_BUFFER_SIZE)
    val regime:SharedFlow<BtLeService.Regime> = sharedRegime

    private val sharedColor = MutableSharedFlow<Int>(replay = BtLeService.SHARED_COLOR_BUFFER_SIZE)
    val color:SharedFlow<Int> = sharedColor

    @DelicateCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        // Log.d(TAG, "Сервис подключён")
        btLeService = (serviceBinder as BtLeService.LocalBinder).getService()
        GlobalScope.launch {
            sharedBond.tryEmit(true)
            btLeService?.state?.collect { state ->
                sharedState.tryEmit(state)
                // Log.d(TAG, "Статус: $state")
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        GlobalScope.launch {
            sharedBond.tryEmit(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun connect(address:String) {
        btLeService?.connect(address)
    }

    fun close() {
        btLeService?.close()
    }
}