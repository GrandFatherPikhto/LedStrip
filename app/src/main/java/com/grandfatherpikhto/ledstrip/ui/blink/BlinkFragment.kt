package com.grandfatherpikhto.ledstrip.ui.blink

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentBlinkBinding
import com.grandfatherpikhto.ledstrip.service.BluetoothLeService
import com.grandfatherpikhto.ledstrip.ui.ledstrip.LedstripFragment

/**
 * A simple [Fragment] subclass.
 * Use the [BlinkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlinkFragment : Fragment() {

    var regime:Int = 0

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

            if (btDeviceAddress != context!!.getString(R.string.default_bt_device_address)) {
                bluetoothLeService!!.connect(btDeviceAddress)
            }
        }

        /**
         *
         */
        override fun onServiceDisconnected(name: ComponentName?) {
            this@BlinkFragment.bluetoothLeService?.close()
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

    private val broadcastReceiver = object : BroadcastReceiver() {
        /**
         *
         */
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action!!) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    intent.extras?.keySet()?.forEach {
                        when (it) {
                            BluetoothLeService.REGIME_DATA -> {
                                val regime =
                                    intent.getIntExtra(BluetoothLeService.REGIME_DATA, 0)
                            }
                            BluetoothLeService.COLOR_DATA -> {
                                val color = intent.getIntExtra(BluetoothLeService.COLOR_DATA, -1)
                            }
                            else -> {

                            }
                        }
                    }
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                }

                BluetoothLeService.ACTION_GATT_DISCOVERED -> {
                }
            }
        }
    }

    /** */
    private var _binding: FragmentBlinkBinding? = null

    /** Это свойство валидно только между onCreateView и onDestroyView */
    private val binding get() = _binding!!


    /**
     *
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBlinkBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        doBindBluetoothLeService()
        return inflater.inflate(R.layout.fragment_blink, container, false)
    }

    /**
     *
     */
    override fun onDestroyView() {
        super.onDestroyView()
        doUnbindBluetoothLeService()
    }

    /**
     *
     */
    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    /**
     *
     */
    override fun onResume() {
        super.onResume()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    /**
     * Привязывание сервиса
     */
    private fun doBindBluetoothLeService() {
        if (!isBond) {
            Intent(context, BluetoothLeService::class.java).also { intent ->
                isBond = requireContext().bindService(
                    intent,
                    serviceBluetoothLeConnection,
                    Context.BIND_AUTO_CREATE
                )
                Log.e(
                    LedstripFragment.TAG,
                    "doBindBluetoothService() serviceBluetoothLeConnection isBond=$isBond"
                )
            }
            requireContext().registerReceiver(broadcastReceiver, makeIntentFilter())
        }
    }

    /**
     * Отвязывание сервиса
     */
    private fun doUnbindBluetoothLeService() {
        if (isBond) {
            context?.unbindService(serviceBluetoothLeConnection)
            isBond = false
            Log.e(LedstripFragment.TAG, "doUnbindBluetoothLeService: $isBond")
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
        return intentFilter
    }
}