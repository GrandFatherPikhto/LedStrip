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
import com.grandfatherpikhto.ledstrip.databinding.FragmentColorBinding
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.ui.model.LedstripViewModel
import com.larswerkman.holocolorpicker.ColorPicker

class ColorFragment : Fragment() {
    companion object {
        const val TAG = "ColorFragment"
        const val NAME = "ColorFragment"
        const val COLOR_STATE  = "ColorFragmentColor"
        const val REGIME_STATE = "ColorFragmentRegime"
        @JvmStatic
        fun newInstance():ColorFragment = ColorFragment()
    }

    private var _binding: FragmentColorBinding?= null
    private val binding:FragmentColorBinding get() = _binding!!

    private val ledstripViewModel: LedstripViewModel by viewModels()

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_color, container, false)
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _binding = FragmentColorBinding.inflate(inflater, container, false)
        binding.apply {
            switchEnableColor.setOnCheckedChangeListener { _, state ->
                if(state) {
                    ledstripViewModel.changeRegime(BtLeService.Regime.Color)
                } else {
                    ledstripViewModel.changeRegime(BtLeService.Regime.Off)
                }
            }

            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(switchEnableColor.isChecked != value.enabled) {
                        switchEnableColor.isChecked = value.enabled
                    }
                }
            })

            pickerColor.addSVBar(svbarColor)

            ledstripViewModel.color.observe(viewLifecycleOwner, { color ->
                if(pickerColor.color != color) {
                    pickerColor.color = color
                }
            })

            pickerColor.onColorChangedListener = ColorPicker.OnColorChangedListener { value ->
                if( ledstripViewModel.color.value != value ) {
                    ledstripViewModel.changeColor(value)
                }
            }
        }
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        sharedPreferences.apply {
            BtLeService.Regime.getByValue(getInt(TailFragment.REGIME, BtLeService.Regime.Off.value))?.let {
                ledstripViewModel.changeRegime(it)
            }
            ledstripViewModel.changeColor(getInt(COLOR_STATE, 0xff80ff))
        }
    }

    override fun onPause() {
        super.onPause()
        sharedPreferences.edit {
            ledstripViewModel.color.value?.let { putInt(COLOR_STATE, it) }
            ledstripViewModel.regime.value?.let { putInt(TailFragment.REGIME, ledstripViewModel.regime.value!!.value) }
        }
    }
}