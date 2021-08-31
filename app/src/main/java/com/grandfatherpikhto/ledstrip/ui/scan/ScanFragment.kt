package com.grandfatherpikhto.ledstrip.ui.scan

import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.ledstrip.ui.MainActivity
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentScanBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter.BtLeDevice
import com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter.RvBtDeviceAdapter
import com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter.RvBtDeviceCallback
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
    private lateinit var settings: SharedPreferences
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
                    Log.d("SCAN_START", "Начато сканирование")
                }
                BluetoothLeService.ACTION_DEVICE_SCAN_STOP -> {
                    Log.d("SCAN_STOP", "Сканирование остановлено")
                }
                BluetoothLeService.ACTION_DEVICE_SCAN_FIND -> {
                    val btDevice = BtLeDevice(
                        intent.getStringExtra(AppConst.btAddress) ?: context!!.getString(R.string.default_bt_device_address),
                        intent.getStringExtra(AppConst.btName) ?: context!!.getString(R.string.default_bt_device_name),
                        intent.getIntExtra(AppConst.btBound, -1)
                    )
                    Log.d("SCAN_FIND", "Найдено устройство $btDevice")
                    rvBtDeviceAdapter.addBtDevice(btDevice)
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
                if(bluetoothLeService!!.state == BluetoothLeService.STATE_SCANNING) {
                    rvBtDeviceAdapter.setBtDevicesList(bluetoothLeService!!.devices)
                    Log.d(TAG, "Устанавливаем список уже найденных устройств")
                } else {
                    rvBtDeviceAdapter.setBtDevicesList(bluetoothLeService!!.getPairedDevices())
                    Log.d(TAG, "Устанавливаем список сопряжённых устройств")
                }
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
        setHasOptionsMenu(true)
        loadPreferences()
        bindRvBtDevices()
        doBindBluetoothLeService()

        return binding.root

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_scan, menu)
    }

    /**
     * Обработка событий меню
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.itemPairedBtDevices -> {
                rvBtDeviceAdapter.setBtDevicesList(bluetoothLeService!!.getPairedDevices())
                return true
            }
            R.id.itemScanBtDevices -> {
                bluetoothLeService?.scanLeDevices()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        doUnbindBluetoothLeService()
        _binding = null
    }

    override fun onPause() {
        requireContext().unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    override fun onResume() {
        requireContext().registerReceiver(broadcastReceiver, makeIntentFilter())
        super.onResume()
    }

    private fun connectBTDevice(btLeDevice: BtLeDevice) {
        Log.d(TAG, "Подключаемся к устройству ${btLeDevice.name}")
        if(bluetoothLeService?.state == BluetoothLeService.STATE_SCANNING) {
            bluetoothLeService?.stopScan()
        }

        if(settings.getBoolean(AppConst.saveDevice, true)) {
            val editor: SharedPreferences.Editor = preferences.edit()
            editor.putString(AppConst.btName, btLeDevice.name)
            editor.putString(AppConst.btAddress, btLeDevice.address)
            editor.apply()
        }

        if(bluetoothLeService?.state == BluetoothLeService.STATE_SCANNING) {
            bluetoothLeService?.stopScan()
        }

        findNavController().navigate(R.id.action_ScanFragment_to_LedstripFragment)
    }

    /**
     *
     */
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
                    connectBTDevice(model)
                }

            })

            rvBtDevices.layoutManager = LinearLayoutManager(context)
            rvBtDevices.adapter       = rvBtDeviceAdapter
        }
    }

    private fun loadPreferences() {
        preferences = requireContext().getSharedPreferences(AppConst.btPrefs, Context.MODE_PRIVATE)!!
        settings    = PreferenceManager.getDefaultSharedPreferences(requireContext())
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
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_STOP)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_START)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_FIND)
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICE_SCAN_BATCH_FIND)
        return intentFilter
    }
}