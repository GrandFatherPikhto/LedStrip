package com.grandfatherpikhto.ledstrip.service

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter.BtLeDevice
import java.lang.StringBuilder
import java.util.*

class BluetoothLeService: LifecycleService() {
    companion object {
        const val TAG:String = "BluetoothLeService"

        const val STATE_DISCONNECTING   =  1
        const val STATE_DISCONNECTED    =  0
        const val STATE_CONNECTING      =  2
        const val STATE_CONNECTED       =  3
        const val STATE_GATT_DISCOVERED =  4
        const val STATE_DATA_AVAILABLE  =  5
        const val STATE_DATA_READED     =  6
        const val STATE_UNKNOWN         = -1

        const val ACTION_GATT_DISCONNECTING:String = "com.grandfatherpikhto.service.STATE_DISCONNECTING"
        const val ACTION_GATT_DISCONNECTED:String = "com.grandfatherpikhto.service.STATE_DISCONNECTED"
        const val ACTION_GATT_CONNECTING:String = "com.grandfatherpikhto.service.STATE_CONNECTING"
        const val ACTION_GATT_CONNECTED:String = "com.grandfatherpikhto.service.STATE_CONNECTED"
        const val ACTION_GATT_DISCOVERED:String = "com.grandfatherpikhto.service.STATE_DISCOVERED"
        const val ACTION_GATT_UNKNOWN:String = "com.grandfatherpikhto.service.STATE_UNKNOWN"
        const val ACTION_DATA_AVAILABLE:String = "com.grandfatherpikhto.service.ACTION_DATA_AVAILABLE"

        val UUID_SERVICE_BLINKER: UUID by lazy { UUID.fromString("000000ff-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_COLOR: UUID by lazy { UUID.fromString("0000ff01-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_REGIME: UUID by lazy { UUID.fromString("0000ff02-6418-5c4b-a046-0101910b5ad4") }

        const val CHAR_COLOR  = "color"
        const val CHAR_REGIME = "regime"

        const val REPEAT_WRITE_CHAR = 10

        const val serviceUUID1:String = "00001801-0000-1000-8000-00805F9B34FB"
        const val serviceUUID2:String = "00002A05-0000-1000-8000-00805F9B34FB"
        const val serviceUUID3:String = "00002A00-0000-1000-8000-00805F9B34FB"
        const val serviceUUID4:String = "00002A37-0000-1000-8000-00805F9B34FB"
        const val serviceUUID5:String = "0000180D-0000-1000-8000-00805F9B34FB"

        val getColor  = MutableLiveData<Int>()
        val getRegime = MutableLiveData<Int>()
        val stateChanged = MutableLiveData<Int>()
    }

    /** */
    private lateinit var bluetoothManager: BluetoothManager
    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter
    /** */
    private var bluetoothGatt: BluetoothGatt? = null
    /** */
    private var serviceLedstrip: BluetoothGattService? = null
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
    /** Список обнаруженных устройств */
    private val bluetoothLeDevices = mutableListOf<BtLeDevice>()
    /** Объект устройства, к которому подключаемся */
    private var bluetoothDevice:BluetoothDevice? = null
    /** Получить список обнаруженных устройств */
    val devices:List<BtLeDevice>
        get() = bluetoothLeDevices.toList()
    /** */
    private lateinit var settings:SharedPreferences
    /** */
    private var isConnected = false


    /** Получить текущее состояние сервиса */
    val state : Int
        get() = this.serviceState

    /**
     *
     */
    private var broadcastReceiver = object: BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context?, intent: Intent?) {
            if(intent != null) {
                when(intent.action) {
                    BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                        Log.d(TAG, "Запрос на сопряжение")
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        Log.d(TAG, "Изменился статус сопряжения устройства")
                        if(serviceState != STATE_CONNECTED) {
                            connect(bluetoothDeviceAddress)
                        }
                    }
                    else -> {

                    }
                }
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
            var action: String
            when (newState) {
                /** */
                BluetoothProfile.STATE_DISCONNECTING -> {
                    serviceState = STATE_DISCONNECTING
                    isConnected = false
                }
                /** */
                BluetoothProfile.STATE_DISCONNECTED -> {
                    close()
                    Log.d(TAG, "Подключение закрыто")
                    serviceState = STATE_DISCONNECTED
                    isConnected = false
                }
                /** */
                BluetoothProfile.STATE_CONNECTING -> {
                    serviceState = STATE_CONNECTING
                    isConnected = false
                }

                /** */
                BluetoothProfile.STATE_CONNECTED -> {
                    serviceState = STATE_CONNECTED
                    Handler(Looper.getMainLooper()).postDelayed({
                        if(gatt!!.discoverServices()) {
                            Log.d(TAG, "Начали исследовать сервисы")
                        } else {
                            Log.d(TAG, "Ошибка. Не можем исследовать сервисы")
                        }
                    }, 1000)
                    isConnected = false
                }
                /** */
                else -> {
                    serviceState = STATE_UNKNOWN
                }
            }
            Log.d(gatCallBackTag, "Состояние подключения изменено на статус: $status, новое состояние $newState")
            stateChanged.postValue(serviceState)
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
                    stateChanged.postValue(STATE_DATA_READED)
                    when (characteristic.uuid) {
                        UUID_SERVICE_CHAR_COLOR -> {
                            getColor.postValue(characteristic.value.toInt())
                        }
                        UUID_SERVICE_CHAR_REGIME -> {
                            getRegime.postValue(characteristic.value.toInt())
                        }
                        else -> {
                        }
                    }
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
                stateChanged.postValue(STATE_DATA_AVAILABLE)
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
                    serviceLedstrip = gatt.getService(UUID_SERVICE_BLINKER)
                    if(serviceLedstrip != null) {
                        serviceLedstrip!!.characteristics.forEach { char ->
                            Log.d(TAG, "${char.uuid}")
                        }
                        charColor   = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_COLOR)
                        charRegime  = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_REGIME)
                        Log.d(TAG, "Сервис: ${serviceLedstrip!!.uuid}, Цвет: ${charColor!!.uuid}, свойства ${charColor?.properties.toString()}")
                        Log.d(TAG, "Сервис: ${serviceLedstrip!!.uuid}, Режим: ${charRegime!!.uuid}")
                        serviceState = STATE_GATT_DISCOVERED
                        if((charColor?.properties?.and(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
                            == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT ) {
                            Log.d(TAG, "Запись по умолчанию ${BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT}, signed: ${BluetoothGattCharacteristic.WRITE_TYPE_SIGNED}, ${BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE}")
                        }
                        charColor?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

                        stateChanged.postValue(STATE_GATT_DISCOVERED)
                        isConnected = true
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
            // Log.d(TAG, "Write characteristic: $characteristic")
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
    override fun  onCreate() {
        initialize()
        super.onCreate()
    }

    /**
     *
     */
    override fun onDestroy() {
        super.onDestroy()
        applicationContext.unregisterReceiver(broadcastReceiver)
    }

    /** Binder given to clients */
    private val binder = LocalBinder()

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): BluetoothLeService = this@BluetoothLeService
    }

    /**
     * Привязывание сервиса "штатным" BindService
     * Вызывается, когда клиент (MainActivity в случае этого приложения) выходит на передний план
     * и связывается с этой службой. Когда это произойдет, служба должна перестать быть службой
     * переднего плана.
     */
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        stopForeground(true)
        return binder
    }

    /**
     * Привязывание сервиса "штатным" BindService
     * Вызывается, когда клиент (MainActivity в случае этого приложения) выходит на передний план
     * и связывается с этой службой. Когда это произойдет, служба должна перестать быть службой
     * переднего плана.
     */
    override fun onRebind(intent: Intent) {
        Log.d(TAG, "in onRebind()")
        stopForeground(true)
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "Last client unbound from service")
        Log.d(TAG, "Starting foreground service")
        // https://developer.android.com/training/notify-user/build-notification
        // startForeground(NOTIFICATION_ID, getNotification())
        return true // Ensures onRebind() is called when a client re-binds.
    }

    /**
     *
     */
    private fun makeIntentFilter():IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        return intentFilter
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

                if(!::bluetoothAdapter.isInitialized) {
                    Log.e(TAG, "Не могу получить Адаптер Блютуз")
                    return false
                }
            } else {
                Log.e(TAG, "Не могу инициализировать менеджер Блютуз")
                return false
            }
        }

        applicationContext.registerReceiver(broadcastReceiver, makeIntentFilter())

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
        Log.d(TAG, "writeCharRegime ${regime.toByte()}")
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
     * Если в настройках установлено сопрягать устройство, если оно не сопряжено
     * сопрягаем перед подключением
     */
    private fun createBond():Boolean {
        Log.d(
            TAG,
            "Сопрячь устройство ${AppConst.boundDevice}: ${
                settings.getBoolean(
                    AppConst.boundDevice,
                    true
                )
            }"
        )
        if (settings.getBoolean(AppConst.boundDevice, true)) {
            if (bluetoothDevice!!.bondState == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "Пытаемся связать устройство ${bluetoothDevice!!.address}")
                bluetoothDevice!!.createBond()
                return true
            }
        }

        return false
    }

    /**
     * Подключаемся к сервису GATT
     * с небольшой задержкой, чтобы избежать неприятных моментов
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun gattConnect() {
        if(bluetoothDevice != null) {
            /**
             * TODO: Разберись, почему можно давать только false
             */
            Handler(Looper.getMainLooper()).postDelayed({
                bluetoothGatt = bluetoothDevice!!.connectGatt(
                    applicationContext,
                    bluetoothDevice!!.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                    bluetoothGattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
            }, AppConst.waitConnect)
            serviceState = STATE_CONNECTING
            // broadcastUpdate(ACTION_GATT_CONNECTING)
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
    fun connect(address:String = applicationContext.getString(R.string.default_bt_device_address)):Boolean {
        if(address == applicationContext.getString(R.string.default_bt_device_address)) {
            Log.d(TAG, "Адрес не определён")
            return false
        }

        bluetoothDeviceAddress = address
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)

        /**
         * Если устройство не сопряжено, сопрягаем и на момент сопряжения
         * прекращаем все действия.
         * Если сопряжение пройдёт удачно, снова пытаемся подключиться
         */
        if(createBond()) {
            return true
        }

        gattConnect()


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
        serviceLedstrip = null
        charColor       = null
        charRegime      = null
        bluetoothGatt   = null
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
        // Log.d(TAG, "isConnected: $isConnected")
        if(isConnected) {
            if(bluetoothGatt != null && bluetoothGattCharacteristic != null) {
                bluetoothGattCharacteristic.value = byteArray
                var res = bluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristic)
                // Log.d(TAG, "Записана характеристика $isWriting: $byteArray, размер очереди ${queueWrite.size}")
                var i = 0
                while(res) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        res = bluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristic)
                        i++
                    }, 10)
                    if(i >= REPEAT_WRITE_CHAR) {
                        Log.e(TAG, "Ошибка записи характеристики $bluetoothGattCharacteristic")
                        break
                    }

                    if(res) break
                }

                isWriting = res

                if(!getNoResponse(bluetoothGattCharacteristic) && res) {
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
                Log.e(TAG, "Неизвестная характеристика ${next?.first.toString()}")
            }
        }
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
    private fun Int.toColor():ByteArray {
        val byteArray = ByteArray(Int.SIZE_BYTES - 1)
        for(i in 0..2) {
            byteArray[i] = this.shr((i + 1)*0).and(0xFF).toByte()
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
     *
     */
    private fun ByteArray.toColor():Int {
        var result = 0
        for(i in 0..3) {
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