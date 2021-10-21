package com.grandfatherpikhto.ledstrip.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentScanBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.BtLeDevice
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeScanServiceConnector
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import com.grandfatherpikhto.ledstrip.ui.adapter.RvBtDevicesAdapter
import com.grandfatherpikhto.ledstrip.ui.adapter.RvBtDevicesCallback
import com.grandfatherpikhto.ledstrip.ui.model.ScanViewModel
import kotlinx.coroutines.DelicateCoroutinesApi

class ScanFragment : Fragment() {
    companion object {
        const val TAG = "ScanFragment"
    }

    private var menuItemScanStart: MenuItem?= null

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!

    private lateinit var rvBtDevicesAdapter: RvBtDevicesAdapter

    private val scanViewModel:ScanViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    private var btLeScanService:BtLeScanService? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        _binding = FragmentScanBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences(AppConst.PREFERENCES, Context.MODE_PRIVATE)
        rvBtDevicesAdapter = RvBtDevicesAdapter()
        rvBtDevicesAdapter.setOnItemClickListener(object : RvBtDevicesCallback<BtLeDevice> {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onDeviceClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Подключаемся к ${model.address}",
                    Toast.LENGTH_LONG).show()
                connectBTDevice(model)

            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onDeviceLongClick(model: BtLeDevice, view: View) {
                Toast.makeText(
                    requireContext(),
                    "Подключаемся к ${model.address}",
                    Toast.LENGTH_LONG).show()
                connectBTDevice(model)
            }

        })
        binding.apply {
            rvBtDevices.adapter = rvBtDevicesAdapter
            rvBtDevices.layoutManager = LinearLayoutManager(requireContext())
            scanViewModel.devices.observe(viewLifecycleOwner, { devices ->
                rvBtDevicesAdapter.setBtDevices(devices.toSet())
            })
            scanViewModel.state.observe(viewLifecycleOwner, { state ->
                if(state == BtLeScanService.State.Scan) {
                    menuItemScanStart?.setIcon(R.drawable.ic_baseline_search_off_24)
                    menuItemScanStart?.setTitle(R.string.stop_scan)
                } else {
                    menuItemScanStart?.setIcon(R.drawable.ic_baseline_search_24)
                    menuItemScanStart?.setTitle(R.string.start_scan)
                }
            })
            scanViewModel.bound.observe(viewLifecycleOwner, { isBond ->
                btLeScanService = BtLeScanServiceConnector.service
                btLeScanService?.scanLeDevices(name = AppConst.DEFAULT_NAME)
            })
        }


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

    @DelicateCoroutinesApi
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.itemStartScan -> {
                scanViewModel.clean()
                btLeScanService?.scanLeDevices(name = AppConst.DEFAULT_NAME)
                true
            }
            R.id.itemPairedDevices -> {
                scanViewModel.clean()
                btLeScanService?.pairedDevices()
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

    @DelicateCoroutinesApi
    override fun onDestroyView() {
        BtLeScanServiceConnector.service?.stopScan()
        super.onDestroyView()
        _binding = null
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun connectBTDevice(btLeDevice: BtLeDevice) {
        BtLeScanServiceConnector.service?.stopScan()
        sharedPreferences.edit {
            putString(AppConst.DEVICE_ADDRESS, btLeDevice.address)
            putString(AppConst.DEVICE_NAME, btLeDevice.name)
            commit()
        }
        Log.d(TAG, "Подключаемся к устройству ${btLeDevice.address}")
        BtLeServiceConnector.service?.connect(btLeDevice.address)
    }
}