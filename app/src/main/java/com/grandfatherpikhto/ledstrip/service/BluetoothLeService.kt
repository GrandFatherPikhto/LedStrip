package com.grandfatherpikhto.ledstrip.service

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter.BtLeDevice
import java.lang.StringBuilder
import java.util.*

class BluetoothLeService: Service() {
    companion object {
        const val TAG:String = "BluetoothLeService"

        const val STATE_DISCONNECTING =  1
        const val STATE_DISCONNECTED  =  0
        const val STATE_CONNECTING    =  2
        const val STATE_CONNECTED     =  3
        const val STATE_DISCOVERED    =  4
        const val STATE_SCANNING      =  5
        const val STATE_UNKNOWN       = -1

        const val ACTION_GATT_DISCONNECTING:String = "com.grandfatherpikhto.service.STATE_DISCONNECTING"
        const val ACTION_GATT_DISCONNECTED:String = "com.grandfatherpikhto.service.STATE_DISCONNECTED"
        const val ACTION_GATT_CONNECTING:String = "com.grandfatherpikhto.service.STATE_CONNECTING"
        const val ACTION_GATT_CONNECTED:String = "com.grandfatherpikhto.service.STATE_CONNECTED"
        const val ACTION_GATT_DISCOVERED:String = "com.grandfatherpikhto.service.STATE_DISCOVERED"
        const val ACTION_GATT_UNKNOWN:String = "com.grandfatherpikhto.service.STATE_UNKNOWN"
        const val ACTION_DATA_AVAILABLE:String = "com.grandfatherpikhto.service.ACTION_DATA_AVAILABLE"
        const val ACTION_DEVICE_SCAN_FIND:String ="com.grandfatherpikhto.service.ACTION_DEVICE_SCAN"
        const val ACTION_DEVICE_SCAN_START:String="com.grandfatherpikhto.service.ACTION_DEVICE_SCAN_START"
        const val ACTION_DEVICE_SCAN_STOP:String="com.grandfatherpikhto.service.ACTION_DEVICE_SCAN_STOP"
        const val ACTION_DEVICE_SCAN_BATCH_FIND:String ="com.grandfatherpikhto.service.ACTION_DEVICE_BATCH_SCAN"

        const val EXTRA_DATA:String = "com.grandfatherpikhto.service.EXTRA_DATA"
        const val REGIME_DATA:String = "com.grandfatherpikhto.service.REGIME_DATA"
        const val COLOR_DATA:String = "com.grandfatherpikhto.service.COLOR_DATA"

        val UUID_SERVICE_BLINKER: UUID by lazy { UUID.fromString("000000ff-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_COLOR: UUID by lazy { UUID.fromString("0000ff01-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_REGIME: UUID by lazy { UUID.fromString("0000ff02-6418-5c4b-a046-0101910b5ad4") }

        const val CHAR_COLOR  = "color"
        const val CHAR_REGIME = "regime"

        const val serviceUUID1:String = "00001801-0000-1000-8000-00805F9B34FB"
        const val serviceUUID2:String = "00002A05-0000-1000-8000-00805F9B34FB"
        const val serviceUUID3:String = "00002A00-0000-1000-8000-00805F9B34FB"
        const val serviceUUID4:String = "00002A37-0000-1000-8000-00805F9B34FB"
        const val serviceUUID5:String = "0000180D-0000-1000-8000-00805F9B34FB"
    }

    /** */
    private val binder = LocalLeServiceBinder()
    /** */
    private lateinit var bluetoothManager: BluetoothManager
    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter
    /** */
    private var bluetoothGatt: BluetoothGatt? = null
    /** */
    private var serviceBlinker: BluetoothGattService? = null
    /** */
    private var charColor: BluetoothGattCharacteristic?  = null
    /** */
    private var charRegime: BluetoothGattCharacteristic? = null
    /** */
    private lateinit var bluetoothDeviceAddress: String
    /** */
    private var serviceState: Int = STATE_DISCONNECTED
    /** Очередь на чтение. Состоит из строкового имени характеристики */
    private lateinit var queueRead: Queue<String>
    /** Очередь на запись. Состоит из строкового имени характеристики и byteArray */
    private lateinit var queueWrite: Queue<Pair<String, ByteArray>>
    /** Идёт чтение характеристики. Не трогать следующее значение в очереди */
    private var isReading:Boolean = false
    /** Идёт запись характеристики. Не трогать следующее значение в очереди */
    private var isWriting:Boolean = false
    /** */
    private var isScan:Boolean = false
    /** Объект сканнера Bluetooth Low Energy устройств */
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    /** Список обнаруженных устройств */
    private val bluetoothLeDevices = mutableListOf<BtLeDevice>()
    /** Получить список обнаруженных устройств */
    val devices:List<BtLeDevice>
        get() = bluetoothLeDevices.toList()
    /** */
    private lateinit var settings:SharedPreferences


    /** Получить текущее состояние сервиса */
    val state : Int
        get() = this.serviceState



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
                broadcastFindDevice(bluetoothDevice)
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

    /** Inner класс для обработки обратных вызовов событий Блютуз
     * После того, как функция широковещательной передачи установлена,
     * она используется внутри BluetoothGattCallbackдля отправки информации
     * о состоянии соединения с сервером GATT. Константы и текущее состояние
     * подключения службы объявляются в службе, представляющей Intentдействия.
     * https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server
     **/
    private val bluetoothGattCallback = object: BluetoothGattCallback() {
        private val gatCallBackTag:String = "bluetoothGattCallback"

        /**
         *
         */
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            var action: String;
            when (newState) {
                /** */
                BluetoothProfile.STATE_DISCONNECTING -> {
                    action = ACTION_GATT_DISCONNECTING
                    serviceState = STATE_DISCONNECTING
                }
                /** */
                BluetoothProfile.STATE_DISCONNECTED -> {
                    action = ACTION_GATT_DISCONNECTED
                    Log.d(TAG, "Закрываем сервис")
                    close()
                    serviceState = STATE_DISCONNECTED

                }
                /** */
                BluetoothProfile.STATE_CONNECTING -> {
                    action = ACTION_GATT_CONNECTING
                    serviceState = STATE_CONNECTING
                }
                /** */
                BluetoothProfile.STATE_CONNECTED -> {
                    action = ACTION_GATT_CONNECTED
                    serviceState = STATE_CONNECTED
                    Handler(Looper.getMainLooper()).postDelayed({
                        if(gatt!!.discoverServices()) {
                            Log.d(TAG, "Начали исследовать сервисы")
                        } else {
                            Log.d(TAG, "Ошибка. Не можем исследовать сервисы")
                        }
                    }, 1000)
                }
                /** */
                else -> {
                    action = ACTION_GATT_UNKNOWN
                    serviceState = STATE_UNKNOWN
                }
            }
            Log.d(gatCallBackTag, "Состояние подключения изменено на $action, статус: $status, новое состояние $newState")
            broadcastUpdate(action)
        }

        /**
         *
         */
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic != null) {
                    logChar(characteristic)
                    isReading = false
                    nextReadQueue()
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }
            }
        }

        /**
         *
         */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic != null) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        /**
         *
         */
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Сервисы исследованы, status == GATT_SUCCESS, gatt = ${gatt.toString()}")
                if(gatt != null) {
                    Log.d(TAG, "Получаем сервисы")
                    serviceBlinker = gatt.getService(UUID_SERVICE_BLINKER)
                    if(serviceBlinker != null) {
                        serviceBlinker!!.characteristics.forEach { char ->
                            Log.d(TAG, "${char.uuid.toString()}")
                        }
                        charColor   = serviceBlinker!!.getCharacteristic(UUID_SERVICE_CHAR_COLOR)
                        charRegime  = serviceBlinker!!.getCharacteristic(UUID_SERVICE_CHAR_REGIME)
                        Log.d(TAG, "Сервис: ${serviceBlinker!!.uuid}, Цвет: ${charColor!!.uuid}, свойства ${charColor?.properties.toString()}")
                        Log.d(TAG, "Сервис: ${serviceBlinker!!.uuid}, Режим: ${charRegime!!.uuid}")
                        serviceState = STATE_DISCOVERED
                        if((charColor?.properties?.and(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
                            == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT ) {
                            Log.d(TAG, "Запись по умолчанию ${BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT}, signed: ${BluetoothGattCharacteristic.WRITE_TYPE_SIGNED}, ${BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE}")
                        }
                        charColor?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                        broadcastUpdate(ACTION_GATT_DISCOVERED)
                    }
                } else {
                    Log.d(TAG, "gatt == null")
                }
            } else {
                Log.d(TAG, "Сервисы не исселдованы, status = $status")
            }
        }

        /**
         *
         */
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if(characteristic != null) {
                isWriting = false
                nextWriteQueue()
            }
        }

        /**
         *
         */
        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)
        }

        /**
         *
         */
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
        }
    }

    /**
     *
     */
    inner class LocalLeServiceBinder: Binder() {
        fun getService() : BluetoothLeService = this@BluetoothLeService
    }

    /**
     *
     */
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    /** Когда сервер подключается к серверу GATT или отключается от него, он должен уведомить
     *  об активности о новом состоянии. Есть несколько способов добиться этого. В следующем
     *  примере широковещательные рассылки используются для отправки информации из службы в действие.
     *  Сервис объявляет функцию для трансляции нового состояния. Эта функция принимает строку
     *  действия, которая передается Intent объекту перед трансляцией в систему.
     *  https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server
     */
    private fun broadcastUpdate(action:String) {
        val intent: Intent = Intent(action)
        sendBroadcast(intent)
    }

    /**
     *
     */
    private fun broadcastFindDevice(bluetoothLeDevice: BluetoothDevice) {
        val intent = Intent(ACTION_DEVICE_SCAN_FIND)
        intent.putExtra(AppConst.btAddress, bluetoothLeDevice.address)
        intent.putExtra(AppConst.btName, bluetoothLeDevice.name)
        intent.putExtra(AppConst.btBound, bluetoothLeDevice.bondState)
        sendBroadcast(intent)
    }

    /**
     *
     */
    private fun broadcastUpdate(action:String, bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)
        if(UUID_SERVICE_CHAR_REGIME == bluetoothGattCharacteristic.uuid) {
            intent.putExtra(REGIME_DATA, bluetoothGattCharacteristic.value)
            sendBroadcast(intent)
        } else if (UUID_SERVICE_CHAR_COLOR == bluetoothGattCharacteristic.uuid) {
            intent.putExtra(COLOR_DATA, bluetoothGattCharacteristic.value.toInt())
            sendBroadcast(intent)
        }
    }

    /**
     *
     */
    override fun onCreate() {
        initialize()
        super.onCreate()
    }

    /**
     * Инициализирует ссылку на локальный менеджер Блютуз и на устройство Блютуз
     */
    private fun initialize():Boolean {
        settings =
            PreferenceManager.getDefaultSharedPreferences(applicationContext)

        if(!::queueRead.isInitialized) {
            queueRead = LinkedList()
        }

        if(!::queueWrite.isInitialized) {
            queueWrite = LinkedList()
        }

        /**
         * https://stackoverflow.com/questions/37618738/how-to-check-if-a-lateinit-variable-has-been-initialized
         */
        if(!::bluetoothManager.isInitialized) {
            Log.w(TAG, "Инициализирую локальный блютуз менеджер")
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if(::bluetoothManager.isInitialized) {
                Log.d(TAG, "Инициализирую локальный адаптер")
                bluetoothAdapter   = bluetoothManager.adapter
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

                if(!::bluetoothAdapter.isInitialized) {
                    Log.e(TAG, "Не могу получить Адаптер Блютуз")
                    return false
                }
            } else {
                Log.e(TAG, "Не могу инициализировать менеджер Блютуз")
                return false
            }
        }

        Log.d(TAG, "Инициализация прошла успешно")

        return true
    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun writeColor(color: Int) {
        writeCharColor(color.toByteArray())
    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun writeCharColor(color: ByteArray) {
        queueWrite.add(Pair(CHAR_COLOR, color))
        nextWriteQueue()
    }


    /**
     *
     */
    fun readCharColor() {
        queueRead.add(CHAR_COLOR)
        nextReadQueue()
    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun writeCharRegime(regime: ByteArray) {
        queueWrite.add(Pair(CHAR_REGIME, regime))
        nextWriteQueue()
    }

    /**
     *
     */
    fun readCharRegime() {
        queueRead.add(CHAR_REGIME)
        nextReadQueue()
    }

    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun writeCharRegime(regime: Int) {
        writeCharRegime(byteArrayOf(regime.toByte()))
        nextWriteQueue()
    }

    /**
     *
     */
    private fun logChar(bluetoothGattCharacteristic: BluetoothGattCharacteristic) {
        val out = StringBuilder()
        bluetoothGattCharacteristic.value.forEach { bt ->
            out.append("0x${bt.toUByte().toString(16).uppercase()} ")
        }

        when (bluetoothGattCharacteristic.uuid) {
            UUID_SERVICE_CHAR_COLOR -> {
                Log.d(TAG, "Characteristic Color ${bluetoothGattCharacteristic.uuid}: $out")
            }
            UUID_SERVICE_CHAR_REGIME -> {
                Log.d(TAG, "Characteristic Regime: ${bluetoothGattCharacteristic.uuid}: $out")
            }
            else -> {
                Log.d(TAG, "Unknown characteristic: ${bluetoothGattCharacteristic.uuid}: $out")
            }
        }
    }

    /**
     * Подключается к серверу GATT, размещенному на устройстве Bluetooth LE.
     *
     * @param address Адрес удалённого устройства.
     *
     * @return Возвращает "Истину", если подключение удалось
     * результат подключения возвращается асинхронно, через
     * функцию обратного вызова
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun connect(address:String):Boolean {
        Log.d(TAG, "Подключаемся к $address")
        if(
            !::bluetoothAdapter.isInitialized
            || address.isBlank()) {
            Log.e(TAG, "Адаптер Блютуз не инициализирован или не задан адрес подключения")
            return false
        }

        if(
            ::bluetoothDeviceAddress.isInitialized
            && address == bluetoothDeviceAddress
            && bluetoothGatt != null) {
            Log.w(TAG, "Пробую использовать существующий bluetoothGATT для подключения")
            return if(bluetoothGatt!!.connect()) {
                Log.w(TAG, "Подключились")
                serviceState = STATE_CONNECTING
                true
            } else {
                Log.w(TAG, "Не подключился")
                false
            }
        }

        if(::bluetoothAdapter.isInitialized) {
            val bluetoothDevice: BluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            /** Если устройство не сопряжено, сопрячь */
            Log.d(TAG, "Сопрячь устройство ${AppConst.boundDevice}: ${settings.getBoolean(AppConst.boundDevice, true)}")
            if(settings.getBoolean(AppConst.boundDevice, true)) {
                if(bluetoothDevice.bondState == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "Пытаемся связать устройство ${bluetoothDevice.address}")
                    bluetoothDevice.createBond()
                }
            }

            Log.w(TAG, "Обнаружено стройство $address")
            Handler(Looper.getMainLooper()).postDelayed({
                if(bluetoothDevice.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                    bluetoothGatt = bluetoothDevice.connectGatt(
                        baseContext
                        , true
                        , bluetoothGattCallback
                        , BluetoothDevice.TRANSPORT_LE)
                    Log.d(TAG, "Устройство не закэшировано")
                } else {
                    bluetoothGatt = bluetoothDevice.connectGatt(
                        baseContext
                        , true
                        , bluetoothGattCallback
                        , BluetoothDevice.TRANSPORT_LE)
                    Log.d(TAG, "Устройство закэшировано")
                }
                Log.d(TAG, "Пробую создать новое подключение / Ответ должен прийти асинхронно в широковещательном событии")
                bluetoothDeviceAddress = address
                serviceState = STATE_CONNECTING
            }, 1000)
        } else {
            Log.e(TAG, "Адаптер не инициализирован, или нет соответствующих разрешений. Не могу подключиться")
            return false
        }

        return true
    }


    /**
     * Событие асинхронное. Дожидаемся получения сообщения DISCONNECTED
     * и уже там вызываем close()
     */
    fun disconnect() {
        if(!::bluetoothAdapter.isInitialized || bluetoothGatt != null) {
            Log.w(TAG, "Адаптер Блютуз не инициализирован")
        }
        Log.d(TAG, "Запрос на отключение")
        bluetoothGatt!!.disconnect()
    }

    /**
     * Вызывается внутри сервиса, поэтому, private, чтобы не дёргать её снаружи
     */
    fun close() {
        if(bluetoothGatt == null) {
            Log.w(TAG, "GATT не инициализирован")
            return
        }
        Log.w(TAG, "Отключаемся от устройства")
        bluetoothGatt!!.close()
        serviceBlinker = null
        charColor      = null
        charRegime     = null
        bluetoothGatt  = null
    }

    /**
     * Чтение следующего запроса на чтение и запрос нужной характеристики
     * Вообще-то, надо реализовать, как здесь https://habr.com/ru/post/538768/
     * Чтобы запрос повторялся сколько-то раз и если ничего не получилось,
     * сбрасывался бы
     */
    private fun nextReadQueue () {
        if(isReading) {
            return
        }

        if(queueRead.size == 0) {
            return
        }

        isReading = true

        when(queueRead.poll()) {
            CHAR_COLOR -> {
                bluetoothGatt!!.readCharacteristic(charColor)
            }
            CHAR_REGIME -> {
                bluetoothGatt!!.readCharacteristic(charRegime)
            }
            else -> {
                Log.w(TAG, "Неизвестная характеристика $this")
            }
        }
    }

    /**
     *
     */
    private fun getNoResponse(bluetoothGattCharacteristic: BluetoothGattCharacteristic) :Boolean {
        return (
                bluetoothGattCharacteristic.properties.and(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                        == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
    }
    /**
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun writeCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic?, byteArray: ByteArray): Boolean {
        if(state != STATE_CONNECTED) {
            if(bluetoothGatt != null && bluetoothGattCharacteristic != null) {
                isWriting = true
                bluetoothGattCharacteristic.value = byteArray
                val res = bluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristic)

                if(!getNoResponse(bluetoothGattCharacteristic)) {
                    nextWriteQueue()
                }
                return res
            }
        } else {
            this.connect(bluetoothDeviceAddress)
        }

        return false
    }

    /**
     * Чтение следующего запроса на чтение и запрос нужной характеристики
     * Вообще-то, надо реализовать, как здесь https://habr.com/ru/post/538768/
     * Чтобы запрос повторялся сколько-то раз и если ничего не получилось,
     * сбрасывался бы
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun nextWriteQueue () {
        if(isWriting) {
            return
        }

        if(queueWrite.size == 0) {
            return
        }

        val next = queueWrite.poll()
        when(next?.first) {
            CHAR_COLOR -> {
                writeCharacteristic(charColor, next.second)
            }
            CHAR_REGIME -> {
                writeCharacteristic(charRegime, next.second)
            }
            else -> {
                Log.w(TAG, "Неизвестная характеристика $this")
            }
        }
    }

    /** Получить список подключённых Блютуз устройств
     *  Paired Devices
     */
    fun getPairedDevices(): List<BtLeDevice> {
        if(isScan) {
            isScan = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
        if(bluetoothAdapter.isEnabled) {
            val pairedDevices:Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if(pairedDevices.isNotEmpty()) {
                return pairedDevices.toBtList()
            }
        }

        return listOf()
    }

    /**
     * Запуск сканирования BLE-устройств
     * Фильтр по сервисам, пока не работает.
     */
    fun scanLeDevices() {
        if(isScan) {
            stopScan()
            startScan()
        } else {
            startScan()
        }
    }

    /**
     * Запуск процесса сканирования
     */
    private fun startScan() {
        broadcastUpdate(ACTION_DEVICE_SCAN_START)

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
        serviceState = STATE_SCANNING

        /**
         * После истечения периода LSHelper.scanPeriod
         * сканирование будет остановлено
         */
        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
        }, AppConst.scanPeriod)
    }

    /**
     * Остановка процесса сканирования
     * Забыл, зачем я тут задержку выставил?
     */
    fun stopScan() {
        isScan = false
        broadcastUpdate(ACTION_DEVICE_SCAN_STOP)
        bluetoothLeScanner.stopScan(leScanCallback)
        serviceState = STATE_UNKNOWN
        Log.d(TAG, "Сканирование окончено")
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
     * Byte array to int
     *
     * @param byteArray
     * @return
     */
    private fun ByteArray.toInt():Int {
        var result = 0
        for(i in this.indices) {
            result = result.or(this[i].toInt().shl(i * 8).and(0xFF.shl(i * 8)))
        }
        return result
    }

    /**
     * Byte array to hex string
     *
     * @param byteArray
     * @return
     */
    fun ByteArray.toHexString(): String {
        val out = StringBuilder()
        this.forEach { bt ->
            out.insert(0, String.format("%02x", bt))
        }
        return out.toString()
    }
}