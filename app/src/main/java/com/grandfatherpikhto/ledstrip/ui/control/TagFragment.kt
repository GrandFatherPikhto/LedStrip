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
import com.grandfatherpikhto.ledstrip.databinding.FragmentTagBinding
import com.grandfatherpikhto.ledstrip.model.Regime
import com.grandfatherpikhto.ledstrip.model.LedstripViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class TagFragment : Fragment() {
    companion object {
        const val TAG = "TagFragment"
        const val NAME = "TagFragment"
        const val REGIME = "tagRegime"
        const val SPEED = "tagSpeed"
        const val BRIGHTNESS="tagBrightness"
        @JvmStatic
        fun newInstance():TagFragment = TagFragment()
    }

    private var _binding: FragmentTagBinding?= null
    private val binding:FragmentTagBinding get() = _binding!!
    private val ledstripViewModel:LedstripViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)

        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_tag, container, false)
        _binding = FragmentTagBinding.inflate(inflater, container, false)
        binding.apply {
            swEnableTag.setOnCheckedChangeListener { _, enabled ->
                if( enabled != ledstripViewModel.regime.value?.enabled ) {
                    if (enabled) {
                        ledstripViewModel.changeRegime(Regime.Tag)
                    } else {
                        ledstripViewModel.changeRegime(Regime.Off)
                    }
                }
            }
            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(swEnableTag.isChecked != value.enabled) {
                        swEnableTag.isChecked = value.enabled
                    }
                }
            })
            slSpeedTag.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeSpeed(value)
                }
            }
            ledstripViewModel.speed.observe(viewLifecycleOwner, { value ->
                if(slSpeedTag.value !== value) {
                    slSpeedTag.value = value
                }
            })
            slTagBrightness.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeBrightness(value)
                }
            }
            ledstripViewModel.brightness.observe(viewLifecycleOwner, { value ->
                if(slTagBrightness.value != value) {
                    slTagBrightness.value = value
                }
            })
        }
        return  binding.root
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
        Log.d(TAG, "onResume()")
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