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
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.grandfatherpikhto.ledstrip.LedstripApplication
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.ActivityMainBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.model.MainActivityModel
import com.grandfatherpikhto.ledstrip.service.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@RequiresApi(Build.VERSION_CODES.M)
@InternalCoroutinesApi
@DelicateCoroutinesApi
class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    enum class Current(val value : Int) {
        Devices(R.id.devicesFragment),
        Ledstrip(R.id.ledstripFragment),
        Settings(R.id.settingsFragment)
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val mainActivityModel: MainActivityModel by viewModels<MainActivityModel>()
    private val preferences:SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences (applicationContext)
    }

    enum class State(val value: Int) {
        Scan(0x01),
        Connect(0x02),
        Settings(0x03)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")

        loadPreferences()

        mainActivityModel.fragment.observe(this, { current ->
            Log.d(TAG, "Current: $current")
            doNavigate(current)
        })

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        bindNavBar()

        requestPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
        doBindServices()

        if(preferences.getString(AppConst.DEVICE_ADDRESS, getString(R.string.default_device_address))
            == getString(R.string.default_device_address)) {
            mainActivityModel.changeFragment(Current.Devices)
        }
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
            R.id.action_devices -> {
                mainActivityModel.changeFragment(Current.Devices)
                true
            }
            R.id.action_settings -> {
                mainActivityModel.changeFragment(Current.Settings)
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
        Log.d(TAG, "onStart()")
        doBindServices()
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

    private fun doNavigate(current: Current) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment?.findNavController()
        if(navController.currentDestination?.id != current.value) {
            navController.navigate(current.value)
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

    private fun bindNavBar() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment?.findNavController()
        if(navController != null) {
            appBarConfiguration = AppBarConfiguration(navController.graph)
            setupActionBarWithNavController(navController, appBarConfiguration)
        }
    }

    private fun loadPreferences() {
        Log.d(TAG, "loadPreferences:")
        preferences.apply {
            mainActivityModel.changeAddress(getString(AppConst.DEVICE_ADDRESS,
                getString(R.string.default_device_address)).toString())
            mainActivityModel.changeName(getString(AppConst.DEFAULT_NAME,
                getString(R.string.default_device_name)).toString())
        }
    }

    private fun savePreferences() {
        preferences.edit {
            putString(AppConst.DEVICE_ADDRESS, mainActivityModel.address.value)
            putString(AppConst.DEFAULT_NAME, mainActivityModel.name.value)
        }
    }
}