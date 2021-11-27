package com.grandfatherpikhto.ledstrip.ui.control

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
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import com.grandfatherpikhto.ledstrip.databinding.FragmentWaterBinding
import com.grandfatherpikhto.ledstrip.model.Regime
import com.grandfatherpikhto.ledstrip.model.LedstripViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class WaterFragment : Fragment() {
    companion object {
        const val TAG = "WaterFragment"
        const val NAME = "WaterFragment"
        const val REGIME = "waterRegime"
        const val BRIGHTNESS = "waterBrightness"
        const val SPEED = "waterSpeed"
        fun newInstance():WaterFragment = WaterFragment()
    }

    private var _binding: FragmentWaterBinding? = null
    private val binding get() = _binding!!
    private val ledstripViewModel: LedstripViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_water, container, false)
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _binding = FragmentWaterBinding.inflate(inflater, container, false)
        binding.apply {
            swWaterEnable.setOnCheckedChangeListener { _, enabled ->
                if( enabled != ledstripViewModel.regime.value?.enabled ) {
                    if (enabled) {
                        ledstripViewModel.changeRegime(Regime.Water)
                    } else {
                        ledstripViewModel.changeRegime(Regime.Off)
                    }
                }
            }
            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(swWaterEnable.isChecked != value.enabled) {
                        swWaterEnable.isChecked = value.enabled
                    }
                }
            })
            slWaterSpeed.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeSpeed(value)
                }
            }
            ledstripViewModel.speed.observe(viewLifecycleOwner, { value ->
                if(slWaterSpeed.value !== value) {
                    slWaterSpeed.value = value
                }
            })
            slWaterBrightness.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeBrightness(value)
                }
            }
            ledstripViewModel.brightness.observe(viewLifecycleOwner, { value ->
                if(slWaterBrightness.value != value) {
                    slWaterBrightness.value = value
                }
            })
        }
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit {
            ledstripViewModel.regime.value?.let { putInt(REGIME, ledstripViewModel.regime.value!!.value) }
            ledstripViewModel.speed.value?.let { putFloat(SPEED, it) }
            ledstripViewModel.brightness.value?.let { putFloat(BRIGHTNESS, it) }
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.apply {
            Regime.getByValue(getInt(REGIME, Regime.Off.value))?.let { regime ->
                Log.d(TAG, "onResume() regime: $regime")
                ledstripViewModel.changeRegime(regime)
            }
            ledstripViewModel.changeSpeed(getFloat(SPEED, 50F))
            ledstripViewModel.changeBrightness(getFloat(BRIGHTNESS, 100F))
        }
    }
}