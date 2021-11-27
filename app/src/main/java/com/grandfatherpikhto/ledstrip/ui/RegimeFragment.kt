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
import androidx.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentLedstripBinding
import com.grandfatherpikhto.ledstrip.databinding.FragmentRegimeBinding
import com.grandfatherpikhto.ledstrip.model.LedstripViewModel
import com.grandfatherpikhto.ledstrip.model.MainActivityModel
import com.grandfatherpikhto.ledstrip.model.Regime
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [RegimeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class RegimeFragment : Fragment() {
    companion object {
        const val TAG:String  = "RegimeFragment"
        const val NAME:String = "LedstripFragment"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment DeviceFragment.
         */
        @JvmStatic
        fun newInstance() = RegimeFragment()
    }

    private var _binding: FragmentRegimeBinding? = null
    private val binding get() = _binding!!
    private val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences (requireContext())
    }

    private val ledstripViewModel:LedstripViewModel by viewModels<LedstripViewModel>()
    private val mainActivityModel: MainActivityModel by activityViewModels<MainActivityModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegimeBinding.inflate(inflater, container, false)
        binding.apply {
            npRegime.displayedValues = Regime.toStringList().toTypedArray()
            npRegime.minValue = 0
            npRegime.maxValue = Regime.values().size - 1
            npRegime.isEnabled = true
            npRegime.setOnValueChangedListener { numberPicker, i, i2 ->
                Log.d(TAG, "$i2")
                ledstripViewModel.changeRegime(Regime.valueOf(numberPicker.displayedValues[i2]))
            }
            npRegime.setOnLongClickListener {
                Log.d(TAG, "change current: Pages")
                mainActivityModel.changeControl(LedstripFragment.Current.Pages)
                true
            }
        }

        return binding.root
    }
}