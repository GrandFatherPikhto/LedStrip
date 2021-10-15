package com.grandfatherpikhto.ledstrip.service

import android.app.Service
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.helper.toByteArray
import com.grandfatherpikhto.ledstrip.helper.toHex
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import java.nio.ByteBuffer
import java.util.*

class BtLeService:Service() {
    companion object {
        const val TAG = "BtLeService"
        const val DEFAULT_ADDRESS = "00:00:00:00:00:00"
        const val WAIT_BEFORE_CONNECT   = 1000L
        const val WAIT_BEFORE_DISCOVERY = 500L
        const val SHARED_REGIME_BUFFER_SIZE = 0x10
        const val SHARED_COLOR_BUFFER_SIZE = 0x100

        const val BUFFER_COLOR_CAPACITY      = 0x100
        const val BUFFER_REGIME_CAPACITY     = 0x02
        const val BUFFER_LENGTH_CAPACITY     = 0x100
        const val BUFFER_SPEED_CAPACITY      = 0x100
        const val BUFFER_BRIGHTNESS_CAPACITY = 0x100
        const val BUFFER_FREQUENCY_CAPACITY  = 0x100

        val UUID_SERVICE_LEDSTRIP:        UUID by lazy { UUID.fromString("000000ff-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_REGIME:     UUID by lazy { UUID.fromString("0000ff01-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_COLOR:      UUID by lazy { UUID.fromString("0000ff02-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_BRIGHTNESS: UUID by lazy { UUID.fromString("0000ff03-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_SPEED:      UUID by lazy { UUID.fromString("0000ff04-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_LENGTH:     UUID by lazy { UUID.fromString("0000ff05-6418-5c4b-a046-0101910b5ad4") }
        val UUID_SERVICE_CHAR_FREQUENCY:  UUID by lazy { UUID.fromString("0000ff06-6418-5c4b-a046-0101910b5ad4") }
    }

    private val defaultAddress:String by lazy {
        applicationContext.getString(R.string.default_device_address)
    }

    private val defaultName:String by lazy {
        applicationContext.getString(R.string.default_device_name)
    }

    enum class State(val value:Int) {
        Disconnected(0x0),
        Connecting(0x01),
        Connected(0x02),
        Discovering(0x03),
        Discovered(0x04),
        Disconnecting(0x05),
        UnknownState(0xFF)
    }

    enum class Pairing(val value:Int) {
        NotPaired(0x00),
        PairingRequest(0x01),
        Paired(0x02)
    }

    enum class Regime constructor(val value:Int) {
        Off(0x00),
        Color(0x01),
        Tag(0x02),
        Water(0x03),
        Tail(0x04),
        Blink(0x05);

        val enabled:Boolean get() {
            if(this == Off) {
                return false
            }

            return true
        }

        companion object {
            private val VALUES = values()
            fun getByValue(value: Int) = VALUES.firstOrNull { it.value == value }
        }
    }

    /** */
    private lateinit var bluetoothManager: BluetoothManager
    /** */
    private lateinit var bluetoothAdapter: BluetoothAdapter
    /** */
    private var bluetoothGatt: BluetoothGatt? = null
    /** Объект устройства, к которому подключаемся */
    private var bluetoothDevice:BluetoothDevice? = null
    /** */
    private var serviceLedstrip: BluetoothGattService? = null
    /** */
    private var charRegime: BluetoothGattCharacteristic? = null
    /** */
    private var charColor: BluetoothGattCharacteristic?  = null
    /** */
    private var charBrightness: BluetoothGattCharacteristic? = null
    /** */
    private var charSpeed: BluetoothGattCharacteristic? = null
    /** */
    private var charLength: BluetoothGattCharacteristic? = null
    /** */
    private var charFrequency: BluetoothGattCharacteristic? = null


    /** Адрес подключённого устройства */
    private val _address = MutableStateFlow(DEFAULT_ADDRESS)
    val address:StateFlow<String> = _address

    private val sharedState = MutableStateFlow<State>(State.Disconnected)
    val state:SharedFlow<State> = sharedState

    private val sharedPairing = MutableStateFlow<Pairing>(Pairing.NotPaired)
    val pairing:StateFlow<Pairing> = sharedPairing

    private val sharedRegime = MutableSharedFlow<Regime>(replay = SHARED_REGIME_BUFFER_SIZE)
    val regime:SharedFlow<Regime> = sharedRegime

    private val sharedColor = MutableSharedFlow<Int>(replay = SHARED_REGIME_BUFFER_SIZE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val color:SharedFlow<Int> = sharedColor

    /** Binder given to clients */
    private val binder = LocalBinder()

    private val regimeChannel     = Channel<Regime> (capacity = BUFFER_REGIME_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val colorChannel      = Channel<Int>    (capacity = BUFFER_COLOR_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val brightnessChannel = Channel<Float>  (capacity = BUFFER_BRIGHTNESS_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val speedChannel      = Channel<Float>  (capacity = BUFFER_SPEED_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val lengthChannel     = Channel<Float>  (capacity = BUFFER_LENGTH_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val frequencyChannel  = Channel<Float>  (capacity = BUFFER_FREQUENCY_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val charWriteMutex = Mutex()

    /**
     *
     */
    @DelicateCoroutinesApi
    private var broadcastReceiver = object: BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "broadcastReceiver: ${intent?.action}")
            if(intent != null) {
                when(intent.action) {
                    BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                        GlobalScope.launch {
                            sharedPairing.tryEmit(Pairing.PairingRequest)
                        }
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        GlobalScope.launch {
                            sharedPairing.tryEmit(Pairing.Paired)
                        }
                        doConnect()
                    }
                    BluetoothDevice.ACTION_FOUND -> {
                        Log.d(TAG, "Устройство найдено")
                        doConnect()
                    }
                    else -> {

                    }
                }
            }
        }
    }

    /**
     * Обратные вызовы работы с GATT
     */
    @DelicateCoroutinesApi
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothProfile.STATE_DISCONNECTED -> {
                    close()
                    GlobalScope.launch {
                        sharedState.tryEmit(State.Disconnected)
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    GlobalScope.launch {
                        sharedState.tryEmit(State.Connecting)
                    }
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    GlobalScope.launch {
                        sharedState.tryEmit(State.Connected)
                        delay(WAIT_BEFORE_DISCOVERY)
                        if(gatt!!.discoverServices()) {
                            sharedState.tryEmit(State.Discovering)
                            Log.d(TAG, "Начали исследовать сервисы")
                        } else {
                            Log.d(TAG, "Ошибка. Не можем исследовать сервисы")
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    GlobalScope.launch {
                        sharedState.tryEmit(State.Disconnecting)
                    }
                }
                else -> {
                }
            }
            if(status == 6 || status == 133) {
                Log.d(TAG, "onConnectionStateChange $status $newState")
                rescanDevicesByAddress()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if(status == BluetoothGatt.GATT_SUCCESS) {
                GlobalScope.launch {
                    bluetoothGatt = gatt
                    if(gatt != null) {
                        Log.d(TAG, "Сервисы исследованы, status == GATT_SUCCESS, gatt = ${gatt.toString()}")
                        if(gatt != null) {
                            Log.d(TAG, "Получаем сервисы")
                            serviceLedstrip = gatt.getService(UUID_SERVICE_LEDSTRIP)
                            if(serviceLedstrip != null) {
                                serviceLedstrip!!.characteristics.forEach { char ->
                                    Log.d(TAG, "${char.uuid}")
                                }
                                charRegime     = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_REGIME)
                                charColor      = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_COLOR)
                                charBrightness = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_BRIGHTNESS)
                                charSpeed      = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_SPEED)
                                charLength     = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_LENGTH)
                                charFrequency  = serviceLedstrip!!.getCharacteristic(UUID_SERVICE_CHAR_FREQUENCY)

                                Log.d(TAG, "Сервис: ${serviceLedstrip!!.uuid}")
                                Log.d(TAG, "1. Режим: ${charRegime!!.uuid}")
                                Log.d(TAG, "2. Цвет: ${charColor!!.uuid}, свойства ${charColor?.properties.toString()}")
                                Log.d(TAG, "3. Яркость: ${charBrightness?.uuid}")
                                Log.d(TAG, "4. Скорость: ${charSpeed?.uuid}")
                                Log.d(TAG, "5. Длина: ${charLength?.uuid}")
                                Log.d(TAG, "6. Частота: ${charFrequency?.uuid}")

                                if((charColor?.properties?.and(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT))
                                    == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT ) {
                                    Log.d(TAG, "Запись по умолчанию ${BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT}, signed: ${BluetoothGattCharacteristic.WRITE_TYPE_SIGNED}, ${BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE}")
                                }
                                charColor?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

                                GlobalScope.launch {
                                    sharedState.tryEmit(State.Discovered)
                                }
                            }
                        } else {
                            Log.d(TAG, "gatt == null")
                        }

                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if(charWriteMutex.isLocked) charWriteMutex.unlock()
        }
    }

    /**
     * Создание сервиса. Первичная инициализация
     */
    @DelicateCoroutinesApi
    override fun onCreate() {
        super.onCreate()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        applicationContext.registerReceiver(broadcastReceiver, makeIntentFilter())
        GlobalScope.launch {
            charWriteMutex.lock()
        }

        GlobalScope.launch {
            while(true) {
                regimeChannel.consumeEach { value ->
                    writeCharacteristic(charRegime, byteArrayOf(value.value.toByte()))
                }
            }
        }

        GlobalScope.launch {
            while(true) {
                colorChannel.consumeEach { value ->
                    Log.d(TAG, "write color: ${value.toByteArray()} ${value.toHex()}")
                    writeCharacteristic(charColor, value.toByteArray())
                }
            }
        }

        GlobalScope.launch {
            while(true) {
                brightnessChannel.consumeEach { value ->
                    writeCharacteristic(charBrightness, value.toByteArray())
                }
            }
        }

        GlobalScope.launch {
            while(true) {
                speedChannel.consumeEach { value ->
                    writeCharacteristic(charSpeed, value.toByteArray())
                }
            }
        }

        GlobalScope.launch {
            while(true) {
                lengthChannel.consumeEach { value ->
                    writeCharacteristic(charLength, value.toByteArray())
                }
            }
        }

        GlobalScope.launch {
            while(true) {
                frequencyChannel.consumeEach { value ->
                    Log.d(TAG, "Frequency: $value")
                    writeCharacteristic(charFrequency, value.toByteArray())
                }
            }
        }
    }

    /**
     *
     */
    private suspend fun writeCharacteristic(bluetoothGattCharacteristic: BluetoothGattCharacteristic?, value: ByteArray):Boolean {
        if(sharedState.value == State.Discovered) {
            if(bluetoothGatt != null && bluetoothGattCharacteristic != null) {
                charWriteMutex.lock()
                bluetoothGattCharacteristic.value = value
                val res = bluetoothGatt!!.writeCharacteristic(bluetoothGattCharacteristic)
                Log.d(TAG, "Записана характеристика ${bluetoothGattCharacteristic.uuid}: $res")
                return res
            }
        }

        return false
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): BtLeService = this@BtLeService
    }

    @DelicateCoroutinesApi
    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "Сервис связан")
        GlobalScope.launch {
            sharedState.tryEmit(State.Disconnected)
        }
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "Сервис отвязан")
        close()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        close()
        super.onDestroy()
        applicationContext.unregisterReceiver(broadcastReceiver)
        Log.d(TAG, "Сервис уничтожен")
    }

    @DelicateCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    fun connect(newAddress:String = address.value) {
        if(newAddress.isNotEmpty() && newAddress != defaultAddress) {
            if(newAddress != address.value) {
                GlobalScope.launch {
                    _address.tryEmit(newAddress)
                }
                Log.d(TAG, "Пробуем подключиться к $newAddress")
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(newAddress)
                if(bluetoothDevice != null) {
                    if(bluetoothDevice!!.bondState == BluetoothDevice.BOND_NONE) {
                        bluetoothDevice!!.createBond()
                    } else {
                        doConnect()
                    }
                } else {
                    Log.d(TAG, "Не могу получить удалённое устройство ${address.value}")
                }
            }
        }
    }

    /**
     *
     */
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    /**
     *
     */
    fun close() {
        Log.d(TAG, "Close ${address.value}")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        sharedState.tryEmit(State.Disconnected)
        bluetoothGatt = null
        serviceLedstrip = null

        charRegime = null
        charColor  = null
        charBrightness = null
        charLength = null
        charSpeed = null
        charFrequency = null
    }

    /**
     * Внутренняя функция для подключения к удалённому устройству
     */
    @DelicateCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    private fun doConnect() {
        GlobalScope.launch {
            delay(WAIT_BEFORE_CONNECT)
            Log.d(TAG, "Пробую подключиться к GATT ${address.value}")
            bluetoothGatt = bluetoothDevice!!.connectGatt(
                applicationContext,
                bluetoothDevice!!.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @DelicateCoroutinesApi
    fun rescanDevicesByAddress() {
        BtLeScanServiceConnector.service.value?.scanLeDevices(address.value)
        GlobalScope.launch {
            delay(100)
            doConnect()
        }
    }

    /**
     * Создаём фильтр перехвата для различных широковещательных событий
     * В данном случае, нужны только фильтры для перехвата
     * запроса на сопряжение устройства и завершения сопряжения
     * И интересует момент "Устройство найдено" на случай рескана устройств
     * по адресу или имени
     */
    private fun makeIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)

        return intentFilter
    }

    @DelicateCoroutinesApi
    fun writeRegime(regime:Regime) {
        GlobalScope.launch {
            regimeChannel.send(regime)
        }
    }

    @DelicateCoroutinesApi
    fun writeColor(color:Int) {
        val launch = GlobalScope.launch {
            colorChannel.send(color)
        }
    }

    @DelicateCoroutinesApi
    fun writeBrightness(brightness: Float) {
        GlobalScope.launch {
            brightnessChannel.send(brightness)
        }
    }

    @DelicateCoroutinesApi
    fun writeSpeed(speed: Float) {
        GlobalScope.launch {
            speedChannel.send(speed)
        }
    }

    @DelicateCoroutinesApi
    fun writeLength(length: Float) {
        GlobalScope.launch {
            lengthChannel.send(length)
        }
    }

    @DelicateCoroutinesApi
    fun writeFrequency(frequency: Float) {
        GlobalScope.launch {
            frequencyChannel.send(frequency)
        }
    }
}