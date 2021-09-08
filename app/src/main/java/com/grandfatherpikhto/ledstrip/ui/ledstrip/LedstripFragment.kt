package com.grandfatherpikhto.ledstrip.ui.ledstrip

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.NumberPicker
import android.widget.Switch
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentLedstripBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.BluetoothLeScanService
import com.grandfatherpikhto.ledstrip.service.BluetoothLeService
import top.defaults.colorpicker.ColorPickerView

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class LedstripFragment : Fragment() {
    /** */
    companion object {
        const val TAG: String = "LedstripFragment"
        const val showLog     = true
        const val regimeOff   = 0
        const val regimeAll   = 1
        const val regimeTag   = 2
        const val regimeColor = 3
        const val regimeTail  = 4
        const val regimeBlink = 5
    }

    /** */
    private var _binding: FragmentLedstripBinding? = null

    /** Это свойство валидно только между onCreateView и onDestroyView */
    private val binding get() = _binding!!

    /** Настройки приложения. Прихватываем отсюда адрес устройства, к которому подключаемся */
    private lateinit var preferences: SharedPreferences

    /** */
    private lateinit var btDeviceName: String

    /** */
    private lateinit var btDeviceAddress: String

    /** */
    private lateinit var cpViewLeds: ColorPickerView

    /** */
    private var regime: Int = regimeOff

    /** */
    private var regimePrev: Int = regimeAll
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
            val binder = service as BluetoothLeService.LocalBinder
            bluetoothLeService = binder.getService()

            if (showLog) {
                Log.d(TAG, "Связь $name с устройством $btDeviceAddress установлена")
            }

            if (btDeviceAddress != context!!.getString(R.string.default_bt_device_address)) {
                bluetoothLeService!!.connect(btDeviceAddress)
            }
        }

        /**
         *
         */
        override fun onServiceDisconnected(name: ComponentName?) {
            this@LedstripFragment.bluetoothLeService?.close()
            Log.d(TAG, "Отвязка сервиса ${name.toString()}")
        }
    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView")
        _binding = FragmentLedstripBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        loadPreferences()
        setObservers()
        bindData()

        return binding.root
    }

    /**
     *
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_ledstrip, menu)
    }

    /**
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "Выбрано ${item.itemId}")
        return when (item.itemId) {
            R.id.itemBtDevicesList -> {
                findNavController().navigate(R.id.action_LedstripFragment_to_ScanFragment)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     *
     */
    override fun onDestroyView() {
        super.onDestroyView()
        Log.e(TAG, "onDestroyView()")
        _binding = null
    }

    /**
     *
     */
    override fun onPause() {
        super.onPause()
        if (isBond) {
            context?.unbindService(serviceBluetoothLeConnection)
            isBond = false
            Log.e(TAG, "doUnbindBluetoothLeService: $isBond")
        }
    }

    /**
     *
     */
    override fun onResume() {
        super.onResume()
        if (!isBond) {
            Intent(context, BluetoothLeService::class.java).also { intent ->
                isBond = requireContext().bindService(
                    intent,
                    serviceBluetoothLeConnection,
                    Context.BIND_AUTO_CREATE
                )
            }
        }
    }

    /**
     *
     */
    private fun loadPreferences() {
        preferences = context?.getSharedPreferences(AppConst.btPrefs, Context.MODE_PRIVATE)!!
        btDeviceAddress = preferences.getString(
            AppConst.btAddress,
            getString(R.string.default_bt_device_address)
        )!!
        btDeviceName = preferences.getString(
            AppConst.btName,
            getString(R.string.default_bt_device_address)
        )!!
    }


    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun bindData() {
        binding.apply {
            colorPicker.isEnabled = false

            /** Неиспользуемые параметры: "_" */
            colorPicker.subscribe { color, _, _ ->
                bluetoothLeService?.writeColor(color)
            }

            regimePicker.displayedValues = arrayOf(
                getString(R.string.regimeOff),
                getString(R.string.regimeAll),
                getString(R.string.regimeTag),
                getString(R.string.regimeWater),
                getString(R.string.regimeTail),
                getString(R.string.regimeBlink)
            )

            regimePicker.minValue = 0
            regimePicker.maxValue = 5

            regimePicker.setOnValueChangedListener { _, _, newVal ->
                Log.d(TAG, "Режим: $newVal")
                regime = newVal
                if(regime != regimeOff) {
                    regimePrev = regime
                }
                bluetoothLeService?.writeCharRegime(regime)
            }

            btnDisable.setOnClickListener {
                Log.d(TAG, "Disable")
                regime = regimeOff
                regimePicker.value = regime
                bluetoothLeService?.writeCharRegime(regime)
            }
        }
    }

    /**
     * Прицепляемся к событиям
     */
    private fun setObservers() {
        BluetoothLeService.stateChanged.observe(viewLifecycleOwner, { state ->
            when (state) {
                BluetoothLeService.STATE_GATT_DISCOVERED-> {
                    bluetoothLeService?.readCharColor()
                    bluetoothLeService?.readCharRegime()
                }
                else -> {
                }
            }
        })

        BluetoothLeService.getRegime.observe(viewLifecycleOwner, { regime ->
            binding.apply {
                regimePicker.value = regime
            }
        })

        BluetoothLeService.getColor.observe(viewLifecycleOwner, { color ->
            binding.apply {
                colorPicker.setInitialColor(color)
            }
        })
    }
}