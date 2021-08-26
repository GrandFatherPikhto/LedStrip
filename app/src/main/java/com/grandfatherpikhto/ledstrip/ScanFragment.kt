package com.grandfatherpikhto.ledstrip

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.ledstrip.databinding.FragmentScanBinding
import com.grandfatherpikhto.ledstrip.helper.LSHelper
import com.grandfatherpikhto.ledstrip.rvbtdadapter.BtLeDevice
import com.grandfatherpikhto.ledstrip.rvbtdadapter.RvBtDeviceAdapter
import com.grandfatherpikhto.ledstrip.rvbtdadapter.RvBtDeviceCallback
import com.grandfatherpikhto.ledstrip.service.BluetoothLeService

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ScanFragment : Fragment() {
    /** Константы класса */
    companion object {
        const val TAG:String = "ScanFragment"
    }

    private var _binding: FragmentScanBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    /** Адаптер списка для отображения списка сопряжённых или найденных устройств */
    private lateinit var rvBtDeviceAdapter: RvBtDeviceAdapter
    /** Сюда запишется адрес выбранного устройства для того, чтобы потом к нему можно было подключиться */
    private lateinit var preferences: SharedPreferences
    /** */
    private var isBound:Boolean = false
    /** Объект сервиса, к которому подключаемся */
    private var bluetoothLeService:BluetoothLeService? = null
    /**
     * Получатель широковещательных сообщений
     **/
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action!!) {
                BluetoothLeService.ACTION_DEVICE_SCAN_START -> {
                    rvBtDeviceAdapter.clearBtDevices()
                }
                BluetoothLeService.ACTION_DEVICE_SCAN_STOP -> {
                    Log.d(TAG, "Scan Stop")
                }
                BluetoothLeService.ACTION_DEVICE_SCAN -> {
                    val btDeviceAddress = intent.getStringExtra(LSHelper.btAddress)
                    val btDeviceName    = intent.getStringExtra(LSHelper.btName)
                    val btBound         = intent.getIntExtra(LSHelper.btBound, -1)

                    if(btDeviceName != null && btDeviceAddress != null) {
                        val btDevice = BtLeDevice(
                            btDeviceAddress,
                            btDeviceName,
                            btBound
                        )
                        rvBtDeviceAdapter.addBtDevice(btDevice)
                    }

                    Log.d(TAG, "Find Device")
                }
            }
        }
    }

    /** Объект подключения к сервису */
    private val serviceBluetoothLeConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.LocalLeServiceBinder
            bluetoothLeService = binder.getService()
            if(bluetoothLeService != null) {
                rvBtDeviceAdapter.setBtDevicesList(bluetoothLeService!!.devices)
                Log.e(MainActivity.TAG, "service Connected")
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            bluetoothLeService = null
            Log.e(MainActivity.TAG, "service Disconnected")
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentScanBinding.inflate(inflater, container, false)
        /**
         * Включить обработку кликов меню
         */
        bindRvBtDevices()
        loadPreferences()
        setHasOptionsMenu(true)
        doBindBluetoothLeService()

        return binding.root

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_scan, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.itemPairedBtDevices -> {
                rvBtDeviceAdapter.setBtDevicesList(bluetoothLeService!!.getPairedDevices())
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        doUnbindBluetoothLeService()
        super.onPause()
    }

    override fun onResume() {
        doBindBluetoothLeService()
        super.onResume()
    }

    private fun connectToDevice(bluetoothDevice: BtLeDevice) {
        Log.d(TAG, "Подключиться к устройству ${bluetoothDevice.name}")
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(LSHelper.btName, bluetoothDevice.name)
        editor.putString(LSHelper.btAddress, bluetoothDevice.address)
        editor.apply()

        findNavController().navigate(R.id.action_ScanFragment_to_LedstripFragment)
    }

    private fun bindRvBtDevices() {
        binding.apply {
            rvBtDeviceAdapter = RvBtDeviceAdapter()

            rvBtDeviceAdapter.setOnItemClickListener(object:
                RvBtDeviceCallback<BtLeDevice> {
                override fun onDeviceClick(model: BtLeDevice, view: View) {
                    Toast.makeText(
                        context, "Чтобы подключиться к устройству ${model.name} используйте долгое нажатие!",
                        Toast.LENGTH_LONG).show()
                }

                override fun onDeviceLongClick(model: BtLeDevice, view: View) {
                    connectToDevice(model)
                }

            })

            rvBtDevices.layoutManager = LinearLayoutManager(context)
            rvBtDevices.adapter       = rvBtDeviceAdapter
        }
    }

    private fun loadPreferences() {
        preferences = context?.getSharedPreferences(LSHelper.btPrefs, Context.MODE_PRIVATE)!!
    }

    /**
     * Привязывание сервиса
     */
    private fun doBindBluetoothLeService() {
        if (!isBound) {
            Intent(context, BluetoothLeService::class.java).also { intent ->
                isBound = requireContext().bindService(
                    intent,
                    serviceBluetoothLeConnection,
                    Context.BIND_AUTO_CREATE
                )
                Log.d(TAG, "Привязка сервиса serviceBluetoothLeConnection")
            }
            requireContext().registerReceiver(broadcastReceiver, makeIntentFilter())
        }
    }

    /**
     * Отвязывание сервиса
     */
    private fun doUnbindBluetoothLeService() {
        Log.d(TAG, "Сервис связан: $isBound")
        if (isBound) {
            context?.unbindService(serviceBluetoothLeConnection)
            isBound = false
        }
    }

    private fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_STOP)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_START)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_BATCH_SCAN)
        return intentFilter
    }
}