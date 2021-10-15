package com.grandfatherpikhto.ledstrip.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.grandfatherpikhto.ledstrip.LedstripApplication
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.ActivityMainBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val ADDRESS = "52:D6:00:67:CC:0E"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var deviceAddress: String
    private lateinit var deviceName: String
    private var state:State = State.Splash

    enum class State(val value: Int) {
        Scan(R.id.ScanFragment),
        Splash(R.id.SplashFragment),
        Container(R.id.ContainerFragment),
        Settings(R.id.SettingsFragment);

        fun isScan():Boolean {
            return this.value == R.id.ScanFragment
        }

        fun isSplah():Boolean {
            return this.value == R.id.SplashFragment
        }

        fun isContainer():Boolean {
            return this.value == R.id.ContainerFragment
        }

        fun isSettings():Boolean {
            return this.value == R.id.SettingsFragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        sharedPreferences = getSharedPreferences(AppConst.PREFERENCES, Context.MODE_PRIVATE).apply {
            deviceAddress = getString(
                AppConst.DEVICE_ADDRESS,
                getString(R.string.default_device_address)
            ).toString()
            deviceAddress = ADDRESS
            deviceName =
                getString(AppConst.DEVICE_NAME, getString(R.string.default_device_name)).toString()
        }
//        deviceAddress = getString(R.string.default_device_address)
//        sharedPreferences.edit {
//            putString(AppConst.DEVICE_ADDRESS, deviceAddress)
//        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        doBindServices()
        navigateStart()
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
            R.id.itemDevicesList -> {
                BtLeServiceConnector.close()
                deviceAddress = getString(R.string.default_device_address)
                setState(State.Scan)
                true
            }
            R.id.action_settings -> {
                setState(State.Settings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        doBindServices()
        bindNavigate()
    }

    override fun onStop() {
        super.onStop()
        doUnbindServices()
    }

    private fun requestPermission(permission: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val launcher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { result: Boolean ->
                    if (result) {
                        Toast.makeText(
                            this,
                            "Разрешение на $permission получено",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        Toast.makeText(
                            this,
                            "Разрешение на $permission не дано",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            launcher.launch(permission)
        }
    }

    /**
     * Проверка группы разрешений
     */
    private fun requestPermissions(permissions: MutableList<String>) {
        var launchPermissions: MutableList<String> = arrayListOf()
        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
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
    private fun permissionsLauncher(permissions: List<String>) {
        if (permissions.isNotEmpty()) {
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

    private fun bindNavigate() {
        lifecycleScope.launch {
            BtLeServiceConnector.service.collect { service ->
                if (service != null) {
                    Log.d(TAG, "Сервис виден")
                    BtLeServiceConnector.state.collect { state ->
                        when (state) {
                            BtLeService.State.Disconnected -> {
                                Log.d(TAG, "Disconnected")
                                if (navController.currentDestination?.id != R.id.SplashFragment
                                    && deviceAddress != getString(R.string.default_device_address)
                                ) {
                                    setState(State.Splash)
                                }
                            }
                            BtLeService.State.Discovered -> {
                                if (navController.currentDestination?.id != R.id.ContainerFragment
                                    && deviceAddress != getString(R.string.default_device_address)
                                ) {
                                    setState(State.Container)
                                }
                            }
                            else -> {
                                if(navController.currentDestination?.id != R.id.ScanFragment
                                    && deviceAddress == getString(R.string.default_device_address)) {
                                    setState(State.Splash)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setState(value: State) {
        when (value) {
            State.Scan -> {
                if(!state.isScan()) {
                    navController.navigate(State.Scan.value)
                }
            }
            State.Splash -> {
                if(!state.isSplah()) {
                    navController.navigate(State.Splash.value)
                }
            }
            State.Container -> {
                if(!state.isContainer()) {
                    navController.navigate(State.Container.value)
                }
            }
            State.Settings -> {
                if(!state.isSettings()) {
                    navController.navigate(State.Settings.value)
                }
            }
        }
        state = value
    }

    private fun navigateStart() {
        Log.d(TAG, deviceAddress)
        lifecycleScope.launch {
            if (deviceAddress == getString(R.string.default_device_address)
                && navController.currentDestination?.id != R.id.ScanFragment
            ) {
                if (navController.currentDestination?.id != R.id.ScanFragment) {
                    navController.navigate(R.id.ScanFragment)
                }
            }
        }
    }

    private fun doBindServices() {
        Log.d(TAG, "doBindServices()")
        Intent(this, BtLeService::class.java).also { intent ->
            Log.d(LedstripApplication.TAG, "Привязываем сервис")
            bindService(intent, BtLeServiceConnector, Context.BIND_AUTO_CREATE)
        }
        Intent(this, BtLeScanService::class.java).also { intent ->
            Log.d(LedstripApplication.TAG, "Привязываем сервис")
            bindService(intent, BtLeScanServiceConnector, Context.BIND_AUTO_CREATE)
        }
    }

    private fun doUnbindServices() {
        Log.d(TAG, "doUnbindServices()")
        unbindService(BtLeServiceConnector)
        unbindService(BtLeScanServiceConnector)
    }
}