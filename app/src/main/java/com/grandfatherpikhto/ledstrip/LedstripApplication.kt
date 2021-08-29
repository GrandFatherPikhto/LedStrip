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

    private var isBound:Boolean = false
    private var bluetoothLeService:BluetoothLeService? = null

    /**
     * Получатель широковещательных сообщений
     **/
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
        }
    }

    /** Объект подключения к сервису */
    private val serviceBluetoothLeConnection = object : ServiceConnection {
        /**
         *
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.LocalLeServiceBinder
            bluetoothLeService = binder.getService()
            Log.d(MainActivity.TAG, "Сервис подключён ${name.toString()}")
        }

        /**
         *
         */
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothLeService = null
            Log.d(MainActivity.TAG, "Сервис отключён ${name.toString()}")
        }

        /**
         *
         */
        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            Log.d(MainActivity.TAG, "Привязка пала ${name.toString()}")
        }

        /**
         *
         */
        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            Log.d(MainActivity.TAG, "Нулевой биндинг $name")
        }
    }


    override fun onCreate() {
        super.onCreate()
        doBindBluetoothLeService()
        Log.e(TAG, "Приложение создано")
    }

    override fun onTerminate() {
        super.onTerminate()
        doUnbindBluetoothLeService()
        Log.e(TAG, "Приложение уничтожено")
    }

    /**
     * Привязывание сервиса
     */
    private fun doBindBluetoothLeService() {
        //if (!isBound) {
        Intent(this, BluetoothLeService::class.java).also { intent ->
            isBound = bindService(
                intent,
                serviceBluetoothLeConnection,
                Context.BIND_AUTO_CREATE
            )
            Log.d(MainActivity.TAG, "doBindBluetoothLeService() Привязка сервиса serviceBluetoothLeConnection")
        }
        registerReceiver(broadcastReceiver, makeIntentFilter())
        //}
    }

    /**
     * Отвязывание сервиса
     */
    private fun doUnbindBluetoothLeService() {
        Log.d(MainActivity.TAG, "Сервис связан: $isBound")
        //if (isBound) {
        unbindService(serviceBluetoothLeConnection)
        isBound = false
        //}
    }

    /**
     * Заглушка
     */
    private fun makeIntentFilter(): IntentFilter {
        return IntentFilter()
    }
}