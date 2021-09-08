package com.grandfatherpikhto.ledstrip.ui.scan

import android.bluetooth.BluetoothDevice
import android.content.Intent
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
import com.grandfatherpikhto.ledstrip.service.BluetoothLeScanService
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

    /** */
    private var _binding: FragmentScanBinding? = null
    /** Это свойство действительно только между onCreateView и onDestroyView */
    private val binding get() = _binding!!
    /** Адаптер списка для отображения списка сопряжённых или найденных устройств */
    private lateinit var rvBtDeviceAdapter: RvBtDeviceAdapter
    /** Сюда запишется адрес выбранного устройства для того, чтобы потом к нему можно было подключиться */
    private lateinit var preferences: SharedPreferences
    /** */
    private lateinit var settings: SharedPreferences
    /** */
    private var isScan: Boolean = false


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
        setObservers()

        return binding.root

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_scan, menu)
        Log.e(TAG, "onCreateOptionsMenu")
    }

    /**
     * Обработка событий меню
     */
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.itemPairedBtDevices -> {
                sendCommandToScanService(BluetoothLeScanService.ACTION_PAIRED_DEVICES)
                return true
            }
            R.id.itemScanBtDevices -> {
                startScan(menuItem)
                return true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveDevice(btLeDevice: BtLeDevice) {
        //if(settings.getBoolean(AppConst.saveDevice, true)) {
        val editor: SharedPreferences.Editor = preferences.edit()
        editor.putString(AppConst.btName, btLeDevice.name)
        editor.putString(AppConst.btAddress, btLeDevice.address)
        editor.apply()
        //}
    }

    /**
     * Вызываем подключение к устройству после долгого клика
     */
    private fun connectBTDevice(btLeDevice: BtLeDevice) {
        Log.d(TAG, "Подключаемся к устройству ${btLeDevice.name}")
        sendCommandToScanService(BluetoothLeScanService.ACTION_STOP_SCAN)
        saveDevice(btLeDevice)

        findNavController().navigate(R.id.action_ScanFragment_to_LedstripFragment)
    }

    /**
     * Привязать события клика к элементам списка
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

    /**
     * Загружаем предпочтения. Не лучший метод, но самый простой
     */
    private fun loadPreferences() {
        preferences = requireContext().getSharedPreferences(AppConst.btPrefs, Context.MODE_PRIVATE)!!
        settings    = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    /**
     * Прицепляемся к событиям
     */
    private fun setObservers() {
        BluetoothLeScanService.devicesList.observe(viewLifecycleOwner, { btLeDevicesList ->
            rvBtDeviceAdapter.setBtDevices(btLeDevicesList)
        })
        BluetoothLeScanService.changeState.observe(viewLifecycleOwner, { state ->
            when (state) {
                BluetoothLeScanService.STATE_STARTED_SCAN-> {
                    isScan = true
                }
                BluetoothLeScanService.STATE_STOPPED_SCAN -> {
                    isScan = false
                }
                else -> {
                }
            }
        })
        BluetoothLeScanService.pairedDevicesList.observe(viewLifecycleOwner, { btPairedDevicesList ->
            rvBtDeviceAdapter.setBtDevices(btPairedDevicesList)
        })
    }

    /**
     *
     */
    private fun startScan(scanMenuItem: MenuItem) {
        if(isScan) {
            sendCommandToScanService(BluetoothLeScanService.ACTION_STOP_SCAN)
            scanMenuItem.setIcon(R.drawable.ic_baseline_search_24)
        } else {
            sendCommandToScanService(BluetoothLeScanService.ACTION_START_OR_RESUME_SCAN)
            scanMenuItem.setIcon(R.drawable.ic_baseline_search_off_24)
        }
    }

    /**
     * Отправляем команду сервису.
     */
    private fun sendCommandToScanService(action: String) {
        requireContext().startService(Intent(requireContext(), BluetoothLeScanService::class.java).apply{
            this.action = action
        })
    }
}