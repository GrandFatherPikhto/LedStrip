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
import com.grandfatherpikhto.ledstrip.databinding.FragmentBlinkBinding
import com.grandfatherpikhto.ledstrip.model.Regime
import com.grandfatherpikhto.ledstrip.model.LedstripViewModel
import com.larswerkman.holocolorpicker.ColorPicker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * A simple [Fragment] subclass.
 * Use the [BlinkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class BlinkFragment : Fragment() {
    companion object {
        const val TAG = "BlinkFragment"
        const val NAME = "BlinkFragment"
        const val REGIME = "blinkRegime"
        const val COLOR  = "blinkColor"
        const val FREQUENCY = "blinkFrequency"
        @JvmStatic
        fun newInstance() = BlinkFragment()
    }

    private var _binding: FragmentBlinkBinding? = null
    private val binding get() = _binding!!

    private val ledstripViewModel: LedstripViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_blink, container, false)
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _binding = FragmentBlinkBinding.inflate(inflater, container, false)
        binding.apply {
            swEnableBlink.setOnCheckedChangeListener { _, enabled ->
                if( enabled != ledstripViewModel.regime.value?.enabled ) {
                    if (enabled) {
                        ledstripViewModel.changeRegime(Regime.Blink)
                    } else {
                        ledstripViewModel.changeRegime(Regime.Off)
                    }
                }
            }
            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(swEnableBlink.isChecked != value.enabled) {
                        swEnableBlink.isChecked = value.enabled
                    }
                }
            })

            ledstripViewModel.color.observe(viewLifecycleOwner, { color ->
                if(pickerBlink.color != color) {
                    pickerBlink.color = color
                }
            })

            pickerBlink.addValueBar(valueBlink)
            pickerBlink.addSaturationBar(saturationBlink)
            var i = 0
            pickerBlink.onColorChangedListener = ColorPicker.OnColorChangedListener { value ->
                if (i == 3) {
                    if (ledstripViewModel.color.value != value) {
                        if( ledstripViewModel.color.value != value ) {
                            ledstripViewModel.changeColor(value)
                        }
                    }
                    i = 0
                } else {
                    pickerBlink.oldCenterColor = value
                    i ++
                }
            }

            slBlinkFrequency.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeFrequency(value)
                }
            }

            ledstripViewModel.frequency.observe(viewLifecycleOwner, { frequency ->
                if(slBlinkFrequency.value != frequency) {
                    slBlinkFrequency.value = frequency
                }
            })
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.apply {
            ledstripViewModel.changeColor(getInt(COLOR, 0xff80ff))
            Regime.getByValue(getInt(REGIME, Regime.Off.value))?.let { regime ->
                Log.d(TAG, "onResume() regime: $regime")
                ledstripViewModel.changeRegime(regime)
            }
            ledstripViewModel.changeFrequency(getFloat(FREQUENCY, 20F))
        }
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit {
            ledstripViewModel.color.value?.let { putInt(COLOR, it) }
            ledstripViewModel.regime.value?.let { putInt(REGIME, ledstripViewModel.regime.value!!.value) }
            ledstripViewModel.frequency.value?.let { putFloat(FREQUENCY, ledstripViewModel.frequency.value!!)}
        }
    }
}