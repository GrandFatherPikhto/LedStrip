package com.grandfatherpikhto.ledstrip.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.grandfatherpikhto.ledstrip.LedstripApplication
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.ActivityMainBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var deviceAddress: String
    private lateinit var deviceName: String
    private var currentFragment:Fragments = Fragments.Splash
    private var currentState:State = State.Connect

    enum class Fragments(val value: Int) {
        Scan(R.id.ScanFragment),
        Splash(R.id.SplashFragment),
        Container(R.id.ContainerFragment),
        Settings(R.id.SettingsFragment);

        fun isScan():Boolean {
            return this.value == Scan.value
        }

        fun isSplah():Boolean {
            return this.value == Splash.value
        }

        fun isContainer():Boolean {
            return this.value == Container.value
        }

        fun isSettings():Boolean {
            return this.value == Settings.value
        }
    }

    enum class State(val value: Int) {
        Scan(0x01),
        Connect(0x02),
        Settings(0x03)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        sharedPreferences = getSharedPreferences(AppConst.PREFERENCES, Context.MODE_PRIVATE).apply {
            deviceAddress = getString(
                AppConst.DEVICE_ADDRESS,
                getString(R.string.default_device_address)
            ).toString()
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

        requestPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
        doBindServices()
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
                doScan()
                true
            }
            R.id.action_settings -> {
                doSettings()
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        readPreferences()
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

    private fun navigateFragment(fragment: Fragments) {
        if(navController.currentDestination?.id != fragment.value) {
            navController.navigate(fragment.value)
            currentFragment = fragment
        }
    }

    @DelicateCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.M)
    private fun bindNavigate() {
        lifecycleScope.launch {
            BtLeServiceConnector.bond.collect { bond ->
                if (bond) {
                    BtLeServiceConnector.state.collect { state ->
                        readPreferences()
                        Log.d(TAG, "State: $state $deviceAddress")
                        doNavigate(state, BtLeServiceConnector.service!!)
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun doNavigate(state:BtLeService.State, service:BtLeService) {
        when(state) {
            BtLeService.State.Disconnected -> {
                when(currentState) {
                    State.Connect -> {
                        readPreferences()
                        service?.connect(deviceAddress)
                        navigateFragment(Fragments.Splash)
                    }
                    State.Scan -> {
                        service?.close()
                        navigateFragment(Fragments.Scan)
                    }
                    State.Settings -> {
                        service?.close()
                        navigateFragment(Fragments.Settings)
                    }
                }
            }
            BtLeService.State.Connecting -> {
                currentState = State.Connect
                navigateFragment(Fragments.Splash)
            }
            BtLeService.State.Discovered -> {
                navigateFragment(Fragments.Container)
            }
            BtLeService.State.RequestScan -> {
                currentState = State.Scan
                navigateFragment(Fragments.Scan)
            }
            else -> {

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

    private fun doScan() {
        currentState = State.Scan
        BtLeServiceConnector.close()
        deviceAddress = getString(R.string.default_device_address)
        navigateFragment(Fragments.Scan)
    }

    private fun doSettings() {
        currentState = State.Settings
        BtLeServiceConnector.close()
        navigateFragment(Fragments.Settings)
    }

    private fun readPreferences() {
        sharedPreferences.apply {
            deviceAddress = getString(AppConst.DEVICE_ADDRESS, getString(R.string.default_device_address)).toString()
        }
    }
}