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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}