package com.grandfatherpikhto.ledstrip.ui

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.ActivityMainBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.BluetoothLeScanService
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
    private lateinit var navHost:NavHostFragment
    private lateinit var navController:NavController

    /**
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        loadPreferences()
        setStartNavigate()

        requestPermissions(
            arrayListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
    }

    /**
     *
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * Обработка событий меню. Если вернуть false, обработка будет передана дальше.  Например,
     * если выберем отображение списка  устройств,во фрагменте ScanFragment клик по этому меню
     * будет тоже обработан.
     * Здесь обрабатываются щелчки по элементам панели действий. Панель действий будет
     * автоматически обрабатывать нажатия кнопки «Домой/Вверх», если вы укажете родительское
     * действие в AndroidManifest.xml.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.itemOptions -> {
                navController.navigate(R.id.SettingsFragment)
                return true
            }
            R.id.itemScanBtDevices -> {
                navController.navigate(R.id.ScanFragment)
                return false
            }
            R.id.itemBlinker -> {
                Log.d(TAG, "Открыть панель мерцания")
                navController.navigate(R.id.BlinkFragment)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     *
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
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
        preferences     = getSharedPreferences(AppConst.btPrefs, Context.MODE_PRIVATE)!!
        btDeviceAddress = preferences.getString(AppConst.btAddress, getString(R.string.default_bt_device_address))!!
        btDeviceName    = preferences.getString(AppConst.btName, getString(R.string.default_bt_device_name))!!
    }

    /**
     * Если MAC-адрес установлен, переходим к фрагменту управления устройством,
     * если нет, сканируем устройства
     */
    private fun setStartNavigate() {
        navHost         = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController   = navHost.findNavController()
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        if(btDeviceAddress.isEmpty() || btDeviceAddress == getString(R.string.default_bt_device_address)) {
//            graph.startDestination = R.id.ScanFragment
//            navController.graph = graph
//            Log.d(TAG, "DeviceAddress: $btDeviceAddress")
        }
        appBarConfiguration = AppBarConfiguration(graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    /**
     * Find fragment by class
     *
     * @param T
     * @return
     */
    private inline fun <reified T: Fragment> FragmentManager.findFragmentByClass(): T? {
        (fragments.firstOrNull { navHostFragment ->
            navHostFragment is NavHostFragment
        } as NavHostFragment)
            .childFragmentManager.fragments.firstNotNullOf { fragment ->
                if ( fragment is T )  return fragment
            }

        return  null
    }
}