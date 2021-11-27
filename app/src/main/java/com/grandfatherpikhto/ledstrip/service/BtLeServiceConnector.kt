package com.grandfatherpikhto.ledstrip.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.grandfatherpikhto.ledstrip.model.Regime
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
object BtLeServiceConnector:ServiceConnection {
    private const val TAG = "BtLeServiceConnector"
    private const val START_COLOR = 0x200100

    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService

    private var sharedBond = MutableStateFlow<Boolean>(false)
    val bond:StateFlow<Boolean> = sharedBond

    private val sharedState = MutableStateFlow(BtLeService.State.Disconnected)
    val state:StateFlow<BtLeService.State> = sharedState

    private val sharedRegime = MutableStateFlow(Regime.Off)
    val regime:SharedFlow<Regime> = sharedRegime.asStateFlow()

    private val sharedColor = MutableStateFlow(START_COLOR)
    val color:SharedFlow<Int> = sharedColor.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        // Log.d(TAG, "Сервис подключён")
        btLeService = (serviceBinder as BtLeService.LocalBinder).getService()
        GlobalScope.launch {
            sharedBond.tryEmit(true)
            btLeService?.state?.collect { state ->
                sharedState.tryEmit(state)
                Log.d(TAG, "Статус: $state")
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