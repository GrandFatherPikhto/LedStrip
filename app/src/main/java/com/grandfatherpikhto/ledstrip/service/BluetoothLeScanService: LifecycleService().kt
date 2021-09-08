package com.grandfatherpikhto.ledstrip.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter.BtLeDevice
import java.util.*

class BluetoothLeScanService : LifecycleService() {
    companion object {
        const val TAG:String = "BluetoothLeScanService"

        const val SCAN_PERIOD = 10000L

        const val STATE_STARTED_SCAN = 1
        const val STATE_STOPPED_SCAN = 2

        const val ACTION_START_SERVICE        = "com.grandfatherpikhto.testlifecycle.service.ACTION_START_SERVICE"
        const val ACTION_START_OR_RESUME_SCAN = "com.grandfatherpikhto.testlifecycle.service.ACTION_START_OR_RESUME_SCAN"
        const val ACTION_STOP_SCAN            = "com.grandfatherpikhto.testlifecycle.service.ACTION_STOP_SCAN"
        const val ACTION_PAIRED_DEVICES       = "com.grandfatherpikhto.testlifecycle.service.ACTION_PAIRED_DEVICES"

        val findDevice  = MutableLiveData<BtLeDevice>()
        val changeState = MutableLiveData<Int>()
        val devicesList = MutableLiveData<Set<BtLeDevice>>()
        val pairedDevicesList = MutableLiveData<Set<BtLeDevice>>()
    }

    /** Процесс сканирования остановлен/запущен */
    private var isScan = false
    /** */
    private lateinit var bluetoothManager: BluetoothManager
    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter
    /** Объект сканнера Bluetooth Low Energy устройств */
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    /** Список найденных устройств */
    private var bluetoothLeDevices = mutableListOf<BtLeDevice>()
    /**
     * Объект-получатель сообщений от процесса сканирования Блютуз-устройств
     * Такой вариант работает для версий >= 6.0
     *
     */
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
                findDevice.postValue(bluetoothDevice.toBtLeDevice())
                devicesList.postValue(bluetoothLeDevices.toSet())
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d(tag, "Ошибка сканирования: $errorCode")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                Log.d(tag, "[BatchScan] Найдено устройство: ${result.device.address}")
                addBtDevice(result.device)
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.d(tag, "[Scan] Найдено устройство: ${result?.device?.address}")
            if(result != null && result.device != null) {
                addBtDevice(result.device)
            }
        }
    }

    /**
     *
     */
    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "Сервис создан")
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

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "Сервис уничтожен")
    }


    /**
     *
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action) {
                ACTION_START_SERVICE -> {
                    Log.d(TAG, "Сервис запущен")
                }
                ACTION_START_OR_RESUME_SCAN -> {
                    Log.d(TAG, "Запуск сканирования")
                    scanLeDevices()
                }
                ACTION_STOP_SCAN -> {
                    Log.d(TAG, "Остановка сканирования")
                    stopScan()
                }
                ACTION_PAIRED_DEVICES -> {
                    Log.d(TAG, "Получить список сопряжённых устройств")
                    stopScan()
                    pairedDevicesList.postValue(bluetoothAdapter.bondedDevices.toBtList().toSet())
                }
                else -> {
                    Log.d(TAG, "Неизвестное действие")
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }


    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.e(TAG, "Rebind")
    }

    /**
     * Запуск сканирования
     */
    private fun startScan() {
        val filterUUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
        Log.d(TAG, "Запуск сканирования $filterUUID")
        bluetoothLeDevices.clear()

        val scanSettings: ScanSettings = ScanSettings.Builder()
            // .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            // .setScanMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            // .setReportDelay(0L)
            .build()
        // bluetoothLeScanner.startScan( leScanCallback!!)
        val filters:List<ScanFilter> = listOf(
            // ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB"))).build()
            // ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID2))).build()
            // ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID3))).build()
            // ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID4))).build()
            // ScanFilter.Builder().setDeviceName("ESP_BLE_SECURITY").build()
            // ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB"))).build()
        )

        bluetoothLeScanner.startScan(filters, scanSettings, leScanCallback)
        isScan = true
        changeState.postValue(STATE_STARTED_SCAN)

        /**
         * После истечения периода LSHelper.scanPeriod
         * сканирование будет остановлено
         */
        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
        }, SCAN_PERIOD)
    }

    /**
     * Запуск сканирования BLE-устройств
     * Фильтр по сервисам, пока не работает.
     */
    private fun scanLeDevices() {
        if(isScan) {
            stopScan()
            startScan()
        } else {
            startScan()
        }
    }

    /**
     * Остановка процесса сканирования
     * Забыл, зачем я тут задержку выставил?
     */
    private fun stopScan() {
        isScan = false
        bluetoothLeScanner.stopScan(leScanCallback)
        changeState.postValue(STATE_STOPPED_SCAN)
        Log.d(TAG, "Сканирование остановлено")
    }

    /**
     * Добавление метода add, так, чтобы преобразовывать BluetoothDevice
     * в более простой BtLeDevice
     */
    fun MutableList<BtLeDevice>.add(bluetoothLeDevice: BluetoothDevice) {
        this.add(BtLeDevice(bluetoothLeDevice.address, bluetoothLeDevice.name ?: applicationContext.getString(
            R.string.default_bt_device_name), bluetoothLeDevice.bondState))
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
     * Int to byte array
     *
     * @param num
     * @return
     */
    private fun Int.toByteArray():ByteArray {
        val byteArray = ByteArray(Int.SIZE_BYTES)
        for(i in byteArray.indices) {
            byteArray[i] = this.shr(i * 8).and(0xFF).toByte()
        }
        return byteArray
    }

    /**
     *
     */
    private fun BluetoothDevice.toBtLeDevice(): BtLeDevice {
        return BtLeDevice(this.address, this.name ?: applicationContext.getString(
            R.string.default_bt_device_name), this.bondState)
    }
}