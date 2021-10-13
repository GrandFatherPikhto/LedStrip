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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.grandfatherpikhto.ledstrip.LedstripApplication
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.ActivityMainBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeScanServiceConnector
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val ADDRESS = "52:D6:00:67:CC:0E"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController:NavController
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var deviceAddress:String
    private lateinit var deviceName:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(AppConst.PREFERENCES, Context.MODE_PRIVATE).apply {
            deviceAddress = getString(AppConst.DEVICE_ADDRESS, getString(R.string.default_device_address)).toString()
            deviceAddress = ADDRESS
            deviceName    = getString(AppConst.DEVICE_NAME, getString(R.string.default_device_name)).toString()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        requestPermissions(
            arrayListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )

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
                if(navController.currentDestination?.id != R.id.ScanFragment) {
                    navController.navigate(R.id.ScanFragment)
                }
                true
            }
            R.id.action_settings -> {
                if(navController.currentDestination?.id != R.id.SettingsFragment) {
                    navController.navigate(R.id.SettingsFragment)
                }
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
        Intent(this, BtLeService::class.java).also { intent ->
            Log.d(LedstripApplication.TAG, "Привязываем сервис")
            bindService(intent, BtLeServiceConnector, Context.BIND_AUTO_CREATE)
        }
        Intent(this, BtLeScanService::class.java).also { intent ->
            Log.d(LedstripApplication.TAG, "Привязываем сервис")
            bindService(intent, BtLeScanServiceConnector, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(BtLeServiceConnector)
        unbindService(BtLeScanServiceConnector)
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

    private fun navigateStart() {
        lifecycleScope.launch {
            BtLeServiceConnector.state.collect { state ->
                if (navController.currentDestination?.id != R.id.ScanFragment) {
                    if (state == BtLeService.State.Discovered) {
//                        if (navController.currentDestination?.id != R.id.ContainerFragment) {
//                            navController.popBackStack()
//                            navController.navigate(R.id.ContainerFragment)
//                        }
                    } else {
                        if (navController.currentDestination?.id != R.id.SplashFragment) {
                            navController.navigate(R.id.SplashFragment)
                        }
                    }
                }
            }
        }
        if(deviceAddress != getString(R.string.default_device_address)) {
            if ( navController.currentDestination?.id != R.id.ScanFragment
                && BtLeServiceConnector.state.value != BtLeService.State.Discovered
                && navController.currentDestination?.id != R.id.SplashFragment
            ) {
                navController.navigate(R.id.SplashFragment)
            }
        }
   }
}