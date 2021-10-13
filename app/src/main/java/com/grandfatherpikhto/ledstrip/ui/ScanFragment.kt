package com.grandfatherpikhto.ledstrip.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
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
        BtLeServiceConnector.service.value?.close()
        sharedPreferences = requireActivity().getSharedPreferences(AppConst.PREFERENCES, Context.MODE_PRIVATE)
        rvBtDevicesAdapter = RvBtDevicesAdapter()
        rvBtDevicesAdapter.setOnItemClickListener(object : RvBtDevicesCallback<BtLeDevice> {
            override fun onDeviceClick(model: BtLeDevice, view: View) {

            }

            override fun onDeviceLongClick(model: BtLeDevice, view: View) {
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
                    BtLeServiceConnector.close()
                } else {
                    menuItemScanStart?.setIcon(R.drawable.ic_baseline_search_24)
                    menuItemScanStart?.setTitle(R.string.start_scan)
                }
            })
            scanViewModel.service.observe(viewLifecycleOwner, { service ->
                btLeScanService = service
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.itemStartScan -> {
                scanViewModel.clean()
                BtLeServiceConnector.service.value?.close()
                btLeScanService?.scanLeDevices()
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

    override fun onDestroyView() {
        super.onDestroyView()
        BtLeScanServiceConnector.stop()
        _binding = null
    }


    private fun connectBTDevice(btLeDevice: BtLeDevice) {
        BtLeScanServiceConnector.stop()
        sharedPreferences.edit {
            putString(AppConst.DEVICE_ADDRESS, btLeDevice.address)
            putString(AppConst.DEVICE_NAME, btLeDevice.name)
        }
        findNavController().navigate(R.id.action_ScanFragment_to_SplashFragment)
    }
}