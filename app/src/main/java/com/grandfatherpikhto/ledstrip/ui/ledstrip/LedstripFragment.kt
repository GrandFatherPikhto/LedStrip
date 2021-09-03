package com.grandfatherpikhto.ledstrip.ui.ledstrip

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.Switch
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentLedstripBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.BluetoothLeService
import top.defaults.colorpicker.ColorPickerView

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class LedstripFragment : Fragment() {
    /** */
    companion object {
        const val TAG: String = "LedstripFragment"
        const val showLog = true
        const val regimeOff = 0
        const val regimeAll = 1
        const val regimeTag = 2
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


    /** Объект сервиса, к которому подключаемся */
    private var bluetoothLeService: BluetoothLeService? = null

    /** */
    private var isBond: Boolean = false

    /** */
    private lateinit var cpViewLeds: ColorPickerView

    /** */
    private var regime: Int = regimeOff

    /** */
    private var regimePrev: Int = regimeAll

    /** Объект подключения к сервису */
    private val serviceBluetoothLeConnection = object : ServiceConnection {
        /**
         *
         */
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.LocalLeServiceBinder
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

        /**
         *
         */
        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            Log.d(TAG, "Привязка пала $name")
        }

        /**
         *
         */
        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            Log.e(TAG, "Нулевой биндинг $name")
        }
    }

    /**
     * Получатель широковещательных сообщений
     **/
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action!!) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    if (showLog) {
                        Log.d(TAG, "Сервис GATT подключён")
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    if (showLog) {
                        Log.d(TAG, "Сервис GATT доступны данные")
                    }
                    intent.extras?.keySet()?.forEach {
                        when (it) {
                            BluetoothLeService.REGIME_DATA -> {
                                regime =
                                    intent.getIntExtra(BluetoothLeService.REGIME_DATA, 0)
                                initRegime()
                                Log.d(TAG, "Получено значение режима $regime")
                            }
                            BluetoothLeService.COLOR_DATA -> {
                                val color = intent.getIntExtra(BluetoothLeService.COLOR_DATA, -1)
                                setColor(color)
                            }
                            else -> {

                            }
                        }
                    }
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    if (showLog) {
                        Log.d(TAG, "Сервис GATT отключён")
                    }
                    if (::cpViewLeds.isInitialized) {
                        cpViewLeds.isEnabled = false
                    }
                }

                BluetoothLeService.ACTION_GATT_DISCOVERED -> {
                    if (showLog) {
                        Log.d(TAG, "Сервис GATT исследован")
                    }
                    bluetoothLeService?.readCharRegime()
                    bluetoothLeService?.readCharColor()
                }
            }
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

        binding.apply {
            cpViewLeds = colorPickerView
            colorPickerView.isEnabled = false

            /** Неиспользуемые параметры: "_" */
            cpViewLeds.subscribe { color, _, _ ->
                bluetoothLeService?.writeColor(color)
            }

            regimePicker.displayedValues = arrayOf("Выключить", "Все", "Пятнашки", "Вода", "Хвост", "Мерцание")
            regimePicker.minValue = 0
            regimePicker.maxValue = 5

            regimePicker.setOnValueChangedListener { _, oldVal, newVal ->
                Log.d(TAG, "Режим: $newVal")
                regime = newVal
                regimePrev = oldVal
                bluetoothLeService?.writeCharRegime(regime)
            }

            swEnable.setOnCheckedChangeListener { _, isChecked ->
                Log.d(TAG, "On $isChecked")
                if(isChecked) {
                    regime = regimePrev
                } else {
                    regime = regimeOff
                }
                regimePicker.value = regime
                bluetoothLeService?.writeCharRegime(regime)
            }
        }

        doBindBluetoothLeService()

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

    override fun onStart() {
        super.onStart()
        Log.e(TAG, "onStart()")
    }

    /**
     * Здесь, не понятно, почему если регистрацию сервиса перенести сюда
     * то блютуз автоматически выключается. Если оставить его в onStart(),
     * то автоотключение не происходит
     */
    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume()")
        requireContext().registerReceiver(broadcastReceiver, makeIntentFilter())
    }

    /**
     *
     */
    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(broadcastReceiver)
        Log.e(TAG, "Событие OnPause()")
    }

    /**
     *
     */
    override fun onDestroyView() {
        super.onDestroyView()
        Log.e(TAG, "onDestroyView()")
        bluetoothLeService?.close()
        doUnbindBluetoothLeService()
        _binding = null
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
                        TAG,
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
            Log.e(TAG, "doUnbindBluetoothLeService: $isBond")
        }
    }

    /**
     * https://kotlinlang.ru/docs/reference/lambdas.html
     * https://kotlinlang.org/docs/lambdas.html#function-types
     */
    private fun initRegime() {
        view?.findViewById<NumberPicker>(R.id.regimePicker)?.apply {
            value = regime
        }
        view?.findViewById<Switch>(R.id.swEnable)?.apply {
            isChecked = regime != regimeOff
        }
    }

    /**
     * Может быть, лучше сделать лямбду?
     * https://kotlinlang.org/docs/lambdas.html#function-types
     */
    private fun setColor(color: Int) {
        view?.findViewById<ColorPickerView>(R.id.colorPickerView).also { ledColorPicker ->
            ledColorPicker?.isEnabled = true
            ledColorPicker?.setInitialColor(color)
        }
    }
}