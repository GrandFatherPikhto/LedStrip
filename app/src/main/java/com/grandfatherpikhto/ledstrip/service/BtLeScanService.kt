package com.grandfatherpikhto.ledstrip.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.helper.AppConst
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class BtLeScanService: Service() {
    companion object {
        const val TAG:String = "BtLeScanService"
        const val DEFAULT_DEVICE_NAME = "LED_STRIP"
        const val SCAN_PERIOD = 10000L
        const val SHARED_DEVICE_BUFFER = 0x10
    }

    enum class State(val value:Int) {
        Stop(0x02),
        Scan(0x01);
    }

    /** */
    private lateinit var bluetoothManager: BluetoothManager
    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter
    /** Объект сканнера Bluetooth Low Energy устройств */
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    /** Список найденных устройств */
    private var bluetoothLeDevices = mutableListOf<BtLeDevice>()
    /** Адрес устройства для краткого пересканирования */
    private lateinit var bluetoothAddress:String
    /** */
    private lateinit var bluetoothName:String
    /** */
    private lateinit var preferences: SharedPreferences
    /** */
    private val sharedState = MutableStateFlow<State>( State.Stop )
    /** */
    val state:StateFlow<State> = sharedState
    /** */
    private val sharedDevice = MutableSharedFlow<BtLeDevice>(
        replay = SHARED_DEVICE_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val device: SharedFlow<BtLeDevice> = sharedDevice

    /**
     * Объект-получатель сообщений от процесса сканирования Блютуз-устройств
     * Такой вариант работает для версий >= 6.0
     *
     */
    @DelicateCoroutinesApi
    private val leScanCallback = object: ScanCallback() {
        private val tag:String = "leScanCallback"

        /**
         * Добавляет в список устройство, если его нет в списке
         * Если в списке устройства нет, вызывает broadcastFindDevice
         * Чтобы оповестить все подписанные активности и фрагменты
         * о том, что найдено устройство
         */
        fun addBtDevice(bluetoothDevice: BluetoothDevice) {
            if(!bluetoothLeDevices.contains(bluetoothDevice)) {
                bluetoothLeDevices.add(bluetoothDevice)
                GlobalScope.launch {
                    sharedDevice.tryEmit(bluetoothDevice.toBtLeDevice())
                }
                if(bluetoothAddress == bluetoothDevice.address) {
                    Log.d(TAG, "Найдено устройство, соответствующее фильтру $bluetoothAddress")
                    stopScan()
                }

                if(bluetoothName != applicationContext.getString(R.string.default_device_name)
                    && bluetoothName == bluetoothDevice.name) {
                    stopScan()
                    saveDevice(bluetoothDevice.toBtLeDevice())
                }
            }
        }

        /**
         * Ошибка сканирования. Пока, никак не обрабатывается
         */
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            GlobalScope.launch {
                sharedState.tryEmit(State.Stop)
            }
            Log.d(tag, "Ошибка сканирования: $errorCode")
        }

        /**
         * Пакетный режим (сразу несколько устройств)
         * Честно говоря, ни разу не видел, чтобы этот режим отрабатывал.
         */
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                Log.d(tag, "[BatchScan] Найдено устройство: ${result.device.address}")
                // addBtDevice(result.device)
                GlobalScope.launch {
                    sharedDevice.tryEmit(result.device.toBtLeDevice())
                }
            }
        }

        /**
         * Найдено одно устройство.
         */
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(tag, "[Scan] Найдено устройство: ${result?.device?.address}")
            if(result != null && result.device != null) {
                // addBtDevice(result.device)
                GlobalScope.launch {
                    sharedDevice.tryEmit(result.device.toBtLeDevice())
                }
            }
        }
    }

    /**
     * Класс, используемый для клиента Binder. Поскольку мы знаем, что эта служба всегда
     * выполняется в том же процессе, что и ее клиенты, нам не нужно иметь дело с IPC.
     */
    inner class LocalBinder : Binder() {
        /** Возвращает экземпляр LocalService, чтобы можно было использовать общедоступные методы */
        fun getService(): BtLeScanService = this@BtLeScanService
    }

    /** Binder given to clients */
    private val binder = LocalBinder()

    /**
     * Привязывание сервиса "штатным" BindService
     * Вызывается, когда клиент (MainActivity в случае этого приложения) выходит на передний план
     * и связывается с этой службой. Когда это произойдет, служба должна перестать быть службой
     * переднего плана.
     */
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind")
        return binder
    }

    /**
     *
     */
    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.e(TAG, "Rebind")
    }

    /**
     * Сервис создан (Lifecycle Service Event)
     */
    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "Сервис создан")
        preferences = applicationContext.getSharedPreferences(AppConst.PREFERENCES, Context.MODE_PRIVATE)!!
        bluetoothAddress = applicationContext.getString(R.string.default_device_address)
        bluetoothName    = applicationContext.getString(R.string.default_device_name)
        if(!::bluetoothManager.isInitialized) {
            Log.w(TAG, "Инициализирую локальный блютуз менеджер")
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if(::bluetoothManager.isInitialized) {
                Log.d(TAG, "Инициализирую локальный адаптер")
                bluetoothAdapter   = bluetoothManager.adapter
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

                if(!::bluetoothAdapter.isInitialized) {
                    Log.e(TAG, "Не могу получить Адаптер Блютуз")
                }
            } else {
                Log.e(TAG, "Не могу инициализировать менеджер Блютуз")
            }
        }
    }

    /**
     * Запуск сканирования
     */
    @DelicateCoroutinesApi
    private fun startScan(address:String = applicationContext.getString(R.string.default_device_address),
                          name:String = applicationContext.getString(R.string.default_device_name)) {
        bluetoothLeDevices.clear()

        val scanSettings: ScanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val filters:MutableList<ScanFilter> = mutableListOf(
            // ScanFilter.Builder().setDeviceName(DEFAULT_DEVICE_NAME).build()
        )

        Log.d(TAG, "Rescan with address $bluetoothAddress")
        if(address != applicationContext.getString(R.string.default_device_address)) {
            Log.d(TAG, "Filter: bluetoothAddress: $address")
            filters.add(ScanFilter.Builder().setDeviceAddress(address).build())
        }

        if (name != applicationContext.getString(R.string.default_device_name)) {
            filters.add(ScanFilter.Builder().setDeviceName(name).build())
        }

        Log.d(TAG, "Запуск сканирования, фильтры: $filters")

        bluetoothLeScanner.startScan(filters, scanSettings, leScanCallback)
        sharedState.tryEmit(State.Scan)
    }

    /**
     * Запуск сканирования BLE-устройств
     * Фильтр по сервисам, пока не работает.
     */
    @DelicateCoroutinesApi
    fun scanLeDevices(address:String = applicationContext.getString(R.string.default_device_address),
              name:String = applicationContext.getString(R.string.default_device_name)) {
        Log.d(TAG, "scanLeDevices $address, $name")
        if(address != applicationContext.getString(R.string.default_device_address)) {
            bluetoothAddress = address
        }

        if(name != applicationContext.getString(R.string.default_device_name)) {
            bluetoothName = name
        }

        Log.d(TAG, "scanLeDevices ${sharedState.value}")
        if(sharedState.value == State.Scan) {
            stopScan()
            startScan(address, name)
        } else {
            startScan(address, name)
        }
    }

    /**
     * Остановка процесса сканирования
     * Забыл, зачем я тут задержку выставил?
     */
    @DelicateCoroutinesApi
    fun stopScan() {
        Log.d(TAG, "stopScan() sharedState=${sharedState.value}")
        if(sharedState.value == State.Scan) {
            bluetoothAddress = applicationContext.getString(R.string.default_device_address)
            bluetoothLeScanner.stopScan(leScanCallback)
            Log.d(TAG, "stopScan: Сканирование остановлено")
            GlobalScope.launch {
                sharedState.tryEmit(State.Stop)
            }
        }
    }

    /**
     * Добавление метода add, так, чтобы преобразовывать BluetoothDevice
     * в более простой BtLeDevice
     */
    fun MutableList<BtLeDevice>.add(bluetoothLeDevice: BluetoothDevice) {
        this.add(BtLeDevice(bluetoothLeDevice.address, bluetoothLeDevice.name ?: applicationContext.getString(
            R.string.default_device_name), bluetoothLeDevice.bondState))
    }

    /**
     * Добавление метода contains, чтобы можно было сравнивать BluetoothDevice и
     * BtLeDevice
     */
    fun MutableList<BtLeDevice>.contains(bluetoothLeDevice: BluetoothDevice): Boolean {
        this.forEach { btLeDevice ->
            if (btLeDevice.address == bluetoothLeDevice.address.toString()) {
                return true
            }
        }
        return false
    }

    /**
     *
     */
    private fun Set<BluetoothDevice>.toBtList(): List<BtLeDevice> {
        val devicesList = mutableListOf<BtLeDevice>()
        forEach { device -> devicesList.add(BtLeDevice(device.address, device.name ?: "Unknown Device", device.bondState)) }
        return devicesList.toList()
    }

    /**
     *
     */
    private fun BluetoothDevice.toBtLeDevice(): BtLeDevice {
        return BtLeDevice(this.address, this.name ?: applicationContext.getString(
            R.string.default_device_name), this.bondState)
    }

    private fun saveDevice(btLeDevice: BtLeDevice) {
        preferences.edit {
            putString(AppConst.DEVICE_NAME, btLeDevice.name)
            putString(AppConst.DEVICE_ADDRESS, btLeDevice.address)
            commit()
        }
    }

    fun pairedDevices() {
        Log.d(TAG, "Сопряжённые устройства")
        bluetoothAdapter.bondedDevices.forEach { device ->
            GlobalScope.launch {
                sharedDevice.tryEmit(device.toBtLeDevice())
                Log.d(TAG, "Сопряжённое устройство $device")
            }
        }
    }
}