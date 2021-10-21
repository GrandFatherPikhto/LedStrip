package com.grandfatherpikhto.ledstrip.ui.control

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import com.grandfatherpikhto.ledstrip.databinding.FragmentColorBinding
import com.grandfatherpikhto.ledstrip.helper.toHex
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.ui.model.LedstripViewModel
import com.larswerkman.holocolorpicker.ColorPicker
import com.larswerkman.holocolorpicker.ValueBar
import kotlinx.coroutines.DelicateCoroutinesApi

class ColorFragment : Fragment() {
    companion object {
        const val TAG = "ColorFragment"
        const val NAME = "ColorFragment"
        const val COLOR  = "colorColor"
        const val REGIME = "colorRegime"
        @JvmStatic
        fun newInstance():ColorFragment = ColorFragment()
    }

    private var _binding: FragmentColorBinding?= null
    private val binding:FragmentColorBinding get() = _binding!!

    private val ledstripViewModel: LedstripViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate()")
    }

    @DelicateCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_color, container, false)
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _binding = FragmentColorBinding.inflate(inflater, container, false)
        binding.apply {
            switchEnableColor.setOnCheckedChangeListener { _, enabled ->
                if( enabled != ledstripViewModel.regime.value?.enabled ) {
                    if (enabled) {
                        ledstripViewModel.changeRegime(BtLeService.Regime.Color)
                    } else {
                        ledstripViewModel.changeRegime(BtLeService.Regime.Off)
                    }
                }
            }

            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(switchEnableColor.isChecked != value.enabled) {
                        switchEnableColor.isChecked = value.enabled
                    }
                }
            })

            ledstripViewModel.color.observe(viewLifecycleOwner, { color ->
                if(pickerColor.color != color) {
                    pickerColor.color = color
                }
            })

            pickerColor.addValueBar(valueColor)
            pickerColor.showOldCenterColor = true

            pickerColor.onColorChangedListener = ColorPicker.OnColorChangedListener { value ->
                val color = valueColor.color
                if(value == color) {
                    if (ledstripViewModel.color.value != color) {
                        ledstripViewModel.changeColor(color)
                    }
                } else {
                    pickerColor.oldCenterColor = value
                }
            }

            valueColor.onValueChangedListener = ValueBar.OnValueChangedListener { color ->
                if (ledstripViewModel.color.value != color) {
                    ledstripViewModel.changeColor(color)
                }
            }
        }
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        sharedPreferences.apply {
            BtLeService.Regime.getByValue(getInt(REGIME, BtLeService.Regime.Off.value))?.let { regime ->
                Log.d(TAG, "onResume() regime: $regime")
                ledstripViewModel.changeRegime(regime)
            }
            ledstripViewModel.changeColor(getInt(COLOR, 0xff80ff))
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause()")
        sharedPreferences.edit {
            ledstripViewModel.color.value?.let { putInt(COLOR, it) }
            ledstripViewModel.regime.value?.let { putInt(REGIME, ledstripViewModel.regime.value!!.value) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }
}