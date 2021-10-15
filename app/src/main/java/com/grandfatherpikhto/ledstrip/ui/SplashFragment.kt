package com.grandfatherpikhto.ledstrip.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentSplashBinding
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import com.grandfatherpikhto.ledstrip.ui.model.SplashViewModel
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@DelicateCoroutinesApi
class SplashFragment : Fragment() {
    companion object {
        const val TAG = "SplashFragment"
    }

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    private val splashViewModel:SplashViewModel by viewModels()
    private lateinit var sharedPreferences:SharedPreferences
    private lateinit var deviceAddress:String
    private lateinit var deviceName:String

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        sharedPreferences = requireActivity().getSharedPreferences(AppConst.PREFERENCES, Context.MODE_PRIVATE).apply {
            deviceAddress = getString(AppConst.DEVICE_ADDRESS, getString(R.string.default_device_address)).toString()
            deviceName = getString(AppConst.DEVICE_NAME, getString(R.string.default_device_name)).toString()
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        splashViewModel.service.observe(viewLifecycleOwner, { service ->
            if(service != null) {
                Log.d(TAG, "Пытаемся подключиться: $deviceAddress, ${BtLeServiceConnector.service.value}")
                BtLeServiceConnector.service.value?.close()
                BtLeServiceConnector.service.value?.connect(deviceAddress)
            }
        })
        splashViewModel.state.observe(viewLifecycleOwner, { state ->
            if(state == BtLeService.State.Discovered) {
                findNavController().navigate(R.id.ContainerFragment)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}