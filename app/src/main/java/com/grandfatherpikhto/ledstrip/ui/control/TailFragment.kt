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
import com.grandfatherpikhto.ledstrip.databinding.FragmentTailBinding
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.ui.model.LedstripViewModel
import com.larswerkman.holocolorpicker.ColorPicker
import com.larswerkman.holocolorpicker.ValueBar

class TailFragment : Fragment() {
    companion object {
        const val TAG = "TailFragment"
        const val NAME = "TailFragment"
        const val SPEED = "tailSpeed"
        const val LENGTH = "tailLength"
        const val COLOR = "tailColor"
        const val REGIME = "tailRegime"
        fun newInstance():TailFragment = TailFragment()
    }

    private var _binding: FragmentTailBinding?= null
    private val binding: FragmentTailBinding get() = _binding!!
    private val ledstripViewModel: LedstripViewModel by viewModels()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_tail, container, false)
        _binding = FragmentTailBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        binding.apply {
            swEnableTail.setOnCheckedChangeListener { _, enabled ->
                if( enabled != ledstripViewModel.regime.value?.enabled ) {
                    if (enabled) {
                        ledstripViewModel.changeRegime(BtLeService.Regime.Tail)
                    } else {
                        ledstripViewModel.changeRegime(BtLeService.Regime.Off)
                    }
                }
            }

            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(swEnableTail.isChecked != value.enabled) {
                        swEnableTail.isChecked = value.enabled
                    }
                }
            })

            pickerTail.addValueBar(valueTail)
            pickerTail.onColorChangedListener = ColorPicker.OnColorChangedListener { value ->
                val color = valueTail.color
                if (color == value) {
                    if( ledstripViewModel.color.value != value ) {
                        ledstripViewModel.changeColor(value)
                    }
                } else {
                    pickerTail.oldCenterColor = value
                }
            }

            valueTail.onValueChangedListener = ValueBar.OnValueChangedListener { value ->
                if( ledstripViewModel.color.value != value ) {
                    ledstripViewModel.changeColor(value)
                }
            }

            ledstripViewModel.color.observe(viewLifecycleOwner, { value ->
                if(pickerTail.color != value) {
                    pickerTail.color = value
                }
            })


            slTailSpeed.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeSpeed(value)
                }
            }

            ledstripViewModel.speed.observe(viewLifecycleOwner, { value ->
                if(slTailSpeed.value != value) {
                    slTailSpeed.value = value
                }
            })

            slTailWidth.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeLength(value)
                }
            }

            ledstripViewModel.length.observe(viewLifecycleOwner, { value ->
                if(slTailWidth.value != value) {
                    slTailWidth.value = value
                }
            })
        }
        return  binding.root
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit {
            ledstripViewModel.regime.value?.let { putInt(REGIME, ledstripViewModel.regime.value!!.value) }
            ledstripViewModel.color.value?.let { putInt(COLOR, it) }
            ledstripViewModel.speed.value?.let { putFloat(SPEED, ledstripViewModel.speed.value!!)}
            ledstripViewModel.length.value?.let { putFloat(LENGTH, ledstripViewModel.length.value!!)}
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.apply {
            BtLeService.Regime.getByValue(getInt(REGIME, BtLeService.Regime.Off.value))?.let { regime ->
                Log.d(TAG, "onResume() regime: $regime")
                ledstripViewModel.changeRegime(regime)
            }
            ledstripViewModel.changeColor(getInt(COLOR, 0xff80ff))
            ledstripViewModel.changeSpeed(getFloat(SPEED, 20F))
            ledstripViewModel.changeLength(getFloat(LENGTH, 25F))
        }
    }
}