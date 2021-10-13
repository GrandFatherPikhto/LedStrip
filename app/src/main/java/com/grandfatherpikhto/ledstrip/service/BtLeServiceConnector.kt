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
    const val TAG = "BTLeSeviceConnector"

    private var btLeService = MutableStateFlow<BtLeService?>(null)
    val service:StateFlow<BtLeService?> = btLeService

    private val sharedState = MutableStateFlow<BtLeService.State>(BtLeService.State.Disconnected)
    val state:StateFlow<BtLeService.State> = sharedState

    private val isBond = MutableStateFlow(false)
    val bond:StateFlow<Boolean> get() = isBond

    private val sharedRegime = MutableSharedFlow<BtLeService.Regime>(replay = BtLeService.SHARED_REGIME_BUFFER_SIZE)
    val regime:SharedFlow<BtLeService.Regime> = sharedRegime

    private val sharedColor = MutableSharedFlow<Int>(replay = BtLeService.SHARED_COLOR_BUFFER_SIZE)
    val color:SharedFlow<Int> = sharedColor

    @DelicateCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onServiceConnected(p0: ComponentName?, serviceBinder: IBinder?) {
        Log.d(TAG, "Сервис подключён")
        val bindedService = (serviceBinder as BtLeService.LocalBinder).getService()
        GlobalScope.launch {
            btLeService.tryEmit(bindedService)
            bindedService.state.collect { state ->
                Log.d(TAG, "Статус: $state")
                sharedState.tryEmit(state)
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        GlobalScope.launch {
            btLeService.tryEmit(null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun connect(address:String) {
        btLeService.value?.connect(address)
    }

    fun close() {
        btLeService.value?.close()
    }
}