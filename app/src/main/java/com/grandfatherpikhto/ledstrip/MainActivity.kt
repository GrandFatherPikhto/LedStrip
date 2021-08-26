package com.grandfatherpikhto.ledstrip

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.ledstrip.databinding.ActivityMainBinding
import com.grandfatherpikhto.ledstrip.helper.LSHelper
import com.grandfatherpikhto.ledstrip.service.BluetoothLeService

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG:String = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    /** Сюда запишется адрес выбранного устройства для того, чтобы потом к нему можно было подключиться */
    private lateinit var preferences: SharedPreferences
    /** Адрес уже выбранного устройства */
    private lateinit var btDeviceName:String
    /** Адрес уже выбранного устройства */
    private lateinit var btDeviceAddress:String

    /** */
    private var isBound:Boolean = false
    /** Объект сервиса, к которому подключаемся */
    private var bluetoothLeService: BluetoothLeService? = null

    /**
     * Получатель широковещательных сообщений
     **/
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
        }
    }

    /** Объект подключения к сервису */
    private val serviceBluetoothLeConnection = object : ServiceConnection {
        /**
         *
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothLeService.LocalLeServiceBinder
            bluetoothLeService = binder.getService()
            Log.e(TAG, "Сервис подключён $name")
        }

        /**
         *
         */
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothLeService = null
            Log.e(TAG, "Сервис отключён $name")
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
            Log.d(TAG, "Нулевой биндинг $name")
        }

    }

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        loadPreferences()
        setNavigate()
        doBindBluetoothLeService()

        binding.fab.setOnClickListener { view ->
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            bluetoothLeService?.scanLeDevices()
            when(navController.currentDestination?.id) {
                R.id.LedstripFragment -> {
                    navController.navigate(R.id.action_LedstripFragment_to_ScanFragment)
                }
                R.id.ScanFragment -> {
                    /** Запустить повторное сканирование */
                    val fragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment_content_main)
                        ?.childFragmentManager
                        ?.findFragmentById(R.id.nav_host_fragment_content_main)
                    if(fragment != null) {
                        Snackbar.make(view, "Запущено повторное сканирование", Snackbar.LENGTH_LONG)
                            .setAction("Сканирование", null).show()
                    }
                }
            }
        }

        requestPermissions(
            arrayListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, makeIntentFilter())
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        doUnbindBluetoothLeService()
    }


    /**
     * Проверка группы разрешений
     */
    private fun requestPermissions(permissions: MutableList<String>) {
        var launchPermissions: MutableList<String> = arrayListOf()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Разрешение на $permission уже есть")
            } else {
                launchPermissions.add(permission)
            }
        }

        permissionsLauncher(launchPermissions)
    }

    /**
     * Запрос группы разрешений
     */
    private fun permissionsLauncher(permissions:List<String>) {
        if(permissions.isNotEmpty()) {
            val launcher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                    results?.entries?.forEach { result ->
                        val name = result.key
                        val isGranted = result.value
                        if (isGranted) {
                            Toast.makeText(this, "Разрешение на $name получено", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(this, "Разрешение на $name не дано", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            launcher.launch(permissions.toTypedArray())
        }
    }

    /**
     * Загрузить сохранённые данные
     */
    private fun loadPreferences() {
        preferences     = getSharedPreferences(LSHelper.btPrefs, Context.MODE_PRIVATE)!!
        btDeviceAddress = preferences?.getString(LSHelper.btAddress, getString(R.string.default_bt_device_address))!!
        btDeviceName    = preferences?.getString(LSHelper.btName, getString(R.string.default_bt_device_name))!!
    }

    /**
     * Если MAC-адрес установлен, переходим к фрагменту управления устройством,
     * если нет, сканируем устройства
     */
    private fun setNavigate() {
        val navHost         = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        val navController   = navHost?.findNavController()
        if(navController != null) {
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            if(::btDeviceAddress.isInitialized && btDeviceAddress != getString(R.string.default_bt_device_address)) {
                graph.startDestination = R.id.LedstripFragment
            } else {
                graph.startDestination = R.id.ScanFragment
            }
            navController.graph    = graph
            appBarConfiguration    = AppBarConfiguration(graph)
            setupActionBarWithNavController(navController!!, appBarConfiguration)
        }
    }

    /**
     * Привязывание сервиса
     */
    private fun doBindBluetoothLeService() {
        if (!isBound) {
            Intent(this, BluetoothLeService::class.java).also { intent ->
                isBound = bindService(
                    intent,
                    serviceBluetoothLeConnection,
                    Context.BIND_AUTO_CREATE
                )
                Log.d(TAG, "Привязка сервиса serviceBluetoothLeConnection")
            }
            registerReceiver(broadcastReceiver, makeIntentFilter())
        }
    }

    /**
     * Отвязывание сервиса
     */
    private fun doUnbindBluetoothLeService() {
        Log.d(TAG, "Сервис связан: $isBound")
        if (isBound) {
            unbindService(serviceBluetoothLeConnection)
            isBound = false
        }
    }

    /**
     * Заглушка
     */
    private fun makeIntentFilter(): IntentFilter {
        return IntentFilter()
    }
}