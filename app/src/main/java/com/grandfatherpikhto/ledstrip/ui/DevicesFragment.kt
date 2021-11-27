package com.grandfatherpikhto.ledstrip.ui

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentDevicesBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.model.MainActivityModel
import com.grandfatherpikhto.ledstrip.service.BtLeDevice
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeScanServiceConnector
import com.grandfatherpikhto.ledstrip.ui.adapter.RvBtDevicesAdapter
import com.grandfatherpikhto.ledstrip.ui.adapter.RvBtDevicesCallback
import com.grandfatherpikhto.ledstrip.model.DevicesViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@RequiresApi(Build.VERSION_CODES.M)
@InternalCoroutinesApi
@DelicateCoroutinesApi
class DevicesFragment : Fragment() {
    companion object {
        const val TAG = "ScanFragment"
    }

    enum class Action(val value: Int) {
        None(0x00),
        Scan(0x01),
        Paired(0x02)
    }

    private val preferences:SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences (requireContext())
    }

    private var menuItemScanStart: MenuItem?= null

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var rvBtDevicesAdapter: RvBtDevicesAdapter
    private val devicesViewModel:DevicesViewModel by viewModels()
    private val mainActivityModel: MainActivityModel by activityViewModels<MainActivityModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        initRvAdapter()
        bindAction()

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_scan, menu)
        menuItemScanStart = menu.findItem(R.id.itemStartScan)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.itemStartScan -> {
                Log.d(TAG, "Scan")
                devicesViewModel.changeAction(Action.Scan)
                true
            }
            R.id.itemPairedDevices -> {
                devicesViewModel.changeAction(Action.Paired)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
    }

    override fun onPause() {
        super.onPause()
        BtLeScanServiceConnector.service?.stopScan()
        Log.d(TAG, "onPause()")
    }

    override fun onDestroyView() {
        BtLeScanServiceConnector.service?.stopScan()
        super.onDestroyView()
        _binding = null
    }

    private fun initRvAdapter() {
        rvBtDevicesAdapter = RvBtDevicesAdapter()
        rvBtDevicesAdapter.setOnItemClickListener(object : RvBtDevicesCallback<BtLeDevice> {
            override fun onDeviceClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Подключаемся к ${model.address}",
                    Toast.LENGTH_LONG).show()
                connectBTDevice(model)

            }

            override fun onDeviceLongClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Подключаемся к ${model.address}",
                    Toast.LENGTH_LONG).show()
                connectBTDevice(model)
            }

        })

        bindRvAdapter()
    }

    private fun bindRvAdapter () {
        binding.apply {
            rvBtDevices.adapter = rvBtDevicesAdapter
            rvBtDevices.layoutManager = LinearLayoutManager(requireContext())
            devicesViewModel.devices.observe(viewLifecycleOwner, { devices ->
                rvBtDevicesAdapter.setBtDevices(devices.toSet())
            })
            devicesViewModel.state.observe(viewLifecycleOwner, { state ->
                if(state == BtLeScanService.State.Scan) {
                    menuItemScanStart?.setIcon(R.drawable.ic_baseline_search_off_24)
                    menuItemScanStart?.setTitle(R.string.stop_scan)
                } else {
                    menuItemScanStart?.setIcon(R.drawable.ic_baseline_search_24)
                    menuItemScanStart?.setTitle(R.string.start_scan)
                }
            })
            devicesViewModel.bound.observe(viewLifecycleOwner, { isBond ->
                // btLeScanService = BtLeScanServiceConnector.service
                // btLeScanService?.scanLeDevices(name = AppConst.DEFAULT_NAME)
            })
        }
    }

    private fun bindAction () {
        devicesViewModel.bound.observe(viewLifecycleOwner, { isBond ->
            Log.e(TAG, "Bind Scan Service $isBond")
            if(isBond) {
                devicesViewModel.action.observe(viewLifecycleOwner, { action ->
                    Log.d(TAG, "$action")
                    when(action) {
                        Action.None -> {
                            devicesViewModel.clean()
                            BtLeScanServiceConnector.service?.stopScan()
                        }
                        Action.Scan -> {
                            Log.d(TAG, "State: $action")
                            BtLeScanServiceConnector.service?.scanLeDevices()
                        }
                        Action.Paired -> {
                            devicesViewModel.clean()
                            BtLeScanServiceConnector.service?.stopScan()
                            BtLeScanServiceConnector.service?.pairedDevices()
                        }
                        else -> {}
                    }
                })
            }
        })
    }

    private fun connectBTDevice(btLeDevice: BtLeDevice) {
        BtLeScanServiceConnector.service?.stopScan()
        preferences.edit {
            putString(AppConst.DEVICE_ADDRESS, btLeDevice.address)
            putString(AppConst.DEVICE_NAME, btLeDevice.name)
            commit()
        }
        mainActivityModel.changeName(btLeDevice.name)
        mainActivityModel.changeAddress(btLeDevice.address)
        mainActivityModel.changeFragment(MainActivity.Current.Ledstrip)
    }
}