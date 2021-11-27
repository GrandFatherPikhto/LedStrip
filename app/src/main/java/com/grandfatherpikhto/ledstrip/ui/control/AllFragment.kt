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
import androidx.fragment.app.viewModels
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentAllBinding
import com.grandfatherpikhto.ledstrip.model.Regime
import com.grandfatherpikhto.ledstrip.model.LedstripViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * A simple [Fragment] subclass.
 * Use the [AllFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class AllFragment : Fragment() {
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AllFragment.
         */
        // TODO: Rename and change types and number of parameters
        const val TAG    = "AllFragment"
        const val NAME   = "AllFragment"
        const val COLOR  = "allColor"
        const val REGIME = "allRegime"
        @JvmStatic
        fun newInstance() = AllFragment()
    }

    private var _binding: FragmentAllBinding?= null
    private val binding: FragmentAllBinding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private val ledstripViewModel: LedstripViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentAllBinding.inflate(inflater, container, false)
        sharedPreferences =
            requireContext().getSharedPreferences(TailFragment.NAME, Context.MODE_PRIVATE)
        binding.apply {
            Log.d(TAG, "Binding ${binding.swEnableAll}")
            swEnableAll.setOnCheckedChangeListener { _, enabled ->
                if( enabled != ledstripViewModel.regime.value?.enabled ) {
                    if (enabled) {
                        ledstripViewModel.changeRegime(Regime.Color)
                    } else {
                        ledstripViewModel.changeRegime(Regime.Off)
                    }
                }
            }

            ledstripViewModel.regime.observe (viewLifecycleOwner, { value ->
                if(value != null) {
                    if(swEnableAll.isChecked != value.enabled) {
                        swEnableAll.isChecked = value.enabled
                    }
                }
            })
        }
        return binding.root
    }
}