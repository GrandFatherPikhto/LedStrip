package com.grandfatherpikhto.ledstrip.ui.control

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import com.grandfatherpikhto.ledstrip.LedstripApplication
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentTagBinding
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.ui.model.LedstripViewModel

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
            swEnableTag.setOnCheckedChangeListener { _, state ->
                if(state) {
                    ledstripViewModel.changeRegime(BtLeService.Regime.Tag)
                } else {
                    ledstripViewModel.changeRegime(BtLeService.Regime.Off)
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
                    ledstripViewModel.changeTagSpeed(value)
                }
            }
            ledstripViewModel.tagSpeed.observe(viewLifecycleOwner, { value ->
                if(slSpeedTag.value !== value) {
                    slSpeedTag.value = value
                }
            })
            slTagBrightness.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeTagBrightness(value)
                }
            }
            ledstripViewModel.tagBrightness.observe(viewLifecycleOwner, { value ->
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
            ledstripViewModel.regime.value?.let { putInt(TailFragment.REGIME, ledstripViewModel.regime.value!!.value) }
            ledstripViewModel.tagSpeed.value?.let { putFloat(SPEED, it) }
            ledstripViewModel.tagBrightness.value?.let { putFloat(BRIGHTNESS, it) }
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.apply {
            BtLeService.Regime.getByValue(getInt(TailFragment.REGIME, BtLeService.Regime.Off.value))?.let {
                ledstripViewModel.changeRegime(it)
            }
            ledstripViewModel.changeTagSpeed(getFloat(SPEED, 50F))
            ledstripViewModel.changeTagBrightness(getFloat(BRIGHTNESS, 100F))
        }
    }
}