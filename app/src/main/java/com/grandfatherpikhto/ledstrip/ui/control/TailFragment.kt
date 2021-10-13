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
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentTailBinding
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.ui.model.LedstripViewModel
import com.larswerkman.holocolorpicker.ColorPicker

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
            swEnableTail.setOnCheckedChangeListener { _, state ->
                if(state) {
                    ledstripViewModel.changeRegime(BtLeService.Regime.Tail)
                } else {
                    ledstripViewModel.changeRegime(BtLeService.Regime.Off)
                }
            }

            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(swEnableTail.isChecked != value.enabled) {
                        swEnableTail.isChecked = value.enabled
                    }
                }
            })

            pickerTail.addSVBar(svbarTail)
            pickerTail.onColorChangedListener = ColorPicker.OnColorChangedListener{ color ->
                if(ledstripViewModel.color.value != color) {
                    ledstripViewModel.changeColor(color)
                }
            }
            ledstripViewModel.color.observe(viewLifecycleOwner, { color ->
                if(pickerTail.color != color) {
                    pickerTail.color = color
                }
            })

            slTailSpeed.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeTailSpeed(value)
                }
            }

            ledstripViewModel.tailSpeed.observe(viewLifecycleOwner, { value ->
                if(slTailSpeed.value != value) {
                    slTailSpeed.value = value
                }
            })

            slTailWidth.addOnChangeListener { _, value, fromUser ->
                if(fromUser) {
                    ledstripViewModel.changeTailLength(value)
                }
            }

            ledstripViewModel.tailLength.observe(viewLifecycleOwner, { value ->
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
            ledstripViewModel.tailSpeed.value?.let { putFloat(SPEED, ledstripViewModel.tailSpeed.value!!)}
            ledstripViewModel.tailLength.value?.let { putFloat(LENGTH, ledstripViewModel.tailLength.value!!)}
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPreferences.apply {
            BtLeService.Regime.getByValue(getInt(REGIME, BtLeService.Regime.Off.value))?.let {
                ledstripViewModel.changeRegime(it)
            }
            ledstripViewModel.changeColor(getInt(COLOR, 0xff80ff))
            ledstripViewModel.changeTailSpeed(getFloat(SPEED, 20F))
            ledstripViewModel.changeTailLength(getFloat(LENGTH, 25F))
        }
    }
}