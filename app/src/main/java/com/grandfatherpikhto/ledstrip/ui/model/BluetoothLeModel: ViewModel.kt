package com.grandfatherpikhto.ledstrip.ui.model

import android.app.Application
import android.content.ComponentName
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.service.BluetoothLeService
import com.grandfatherpikhto.ledstrip.ui.ledstrip.LedstripFragment
import kotlin.coroutines.coroutineContext

class BluetoothLeModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val defaultAddress:String = "00:00:00:00:00:00"
        const val defaultName:String = "Unknown Device"
    }

    /** */
    private lateinit var btDeviceName: String

    /** */
    private lateinit var btDeviceAddress: String

    /** Объект сервиса, к которому подключаемся */
    private var bluetoothLeService: BluetoothLeService? = null

    /** */
    private var isBond: Boolean = false

    /** Объект подключения к сервису */
    private val serviceBluetoothLeConnection = object : ServiceConnection {
        /**
         *
         */
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.LocalLeServiceBinder
            bluetoothLeService = binder.getService()

            if (LedstripFragment.showLog) {
                Log.d(LedstripFragment.TAG, "Связь $name с устройством $btDeviceAddress установлена")
            }

            if (btDeviceAddress != application.applicationContext!!.getString(R.string.default_bt_device_address)) {
                bluetoothLeService!!.connect(btDeviceAddress)
            }
        }

        /**
         *
         */
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothLeService?.close()
            Log.d(LedstripFragment.TAG, "Отвязка сервиса ${name.toString()}")
        }

        /**
         *
         */
        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            Log.d(LedstripFragment.TAG, "Привязка пала $name")
        }

        /**
         *
         */
        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            Log.e(LedstripFragment.TAG, "Нулевой биндинг $name")
        }
    }

    /**
     *
     */
    private fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTING)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTING)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_START)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_STOP)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_FIND)
        return intentFilter
    }

}