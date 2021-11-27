package com.grandfatherpikhto.ledstrip.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarItemView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentLedstripBinding
import com.grandfatherpikhto.ledstrip.model.Regime
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import com.grandfatherpikhto.ledstrip.ui.adapter.ContainerPagerAdapter
import com.grandfatherpikhto.ledstrip.model.LedstripViewModel
import com.grandfatherpikhto.ledstrip.model.MainActivityModel
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class LedstripFragment : Fragment() {

    companion object {
        const val TAG = "LedstripFragment"
        const val NAME = "LedstrpFragment"
        fun newInstance() = LedstripFragment()
    }

    enum class Current(val value: Int) {
        Regime(R.id.regimeFragment),
        Pages(R.id.pagesFragment)
    }

    private val ledstripViewModel: LedstripViewModel by viewModels()
    private val mainActivityModel: MainActivityModel by activityViewModels<MainActivityModel>()

    private var _binding: FragmentLedstripBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _binding = FragmentLedstripBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        val navHostFragment = childFragmentManager.findFragmentById(R.id.ledstrip_container) as NavHostFragment
        val navController   = navHostFragment?.findNavController()

        bindBottomMenu()

        lifecycleScope.launch {
            BtLeServiceConnector.state.collect { state ->
                when(state) {
                    BtLeService.State.Discovered -> {
                        navController?.navigate(ledstripViewModel.fragment.value!!.value)
                    }
                    else -> {
                        navController?.navigate(R.id.splashFragment)
                    }
                }
            }
        }

        lifecycleScope.launch {
            BtLeServiceConnector.bond.collect { bond ->
                if (bond) {
                    Log.d(TAG, "Подключаемся к ${mainActivityModel.address.value}")
                    BtLeServiceConnector.service?.connect(mainActivityModel.address.value!!)
                } else {

                }
            }
        }

        lifecycleScope.launch {
            mainActivityModel.control.observe(viewLifecycleOwner, { current ->
                Log.d(TAG, "Current: $current")
                if(ledstripViewModel.state.value == BtLeService.State.Discovered) {
                    navController.navigate(current.value)
                } else {
                    navController.navigate(R.id.splashFragment)
                }
            })
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        BtLeServiceConnector.service?.close()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        mainActivityModel.address.value?.let { BtLeServiceConnector.service?.connect(it) }
    }

    override fun onStop() {
        Log.e(TAG, "onStop()")
        BtLeServiceConnector.service?.close()
        super.onStop()
    }

    private fun bindBottomMenu() {
        binding.apply {
            bnLedstrip.setOnItemSelectedListener(
                NavigationBarView.OnItemSelectedListener { menuItem ->
                    Log.d(TAG, "$menuItem")
                    when(menuItem.itemId) {
                        R.id.action_devices -> {

                        }
                        R.id.action_regimes -> {
                            navControl(Current.Regime)
                        }
                        R.id.action_pages -> {
                            navControl(Current.Pages)
                        }
                    }
                    true
                }
            )
        }
    }

    private fun navControl(current: Current) {
        val navHostFragment = childFragmentManager.findFragmentById(R.id.ledstrip_container) as NavHostFragment
        val navController   = navHostFragment?.findNavController()
        if(ledstripViewModel.state.value == BtLeService.State.Discovered) {
            navController.navigate(current.value)
        } else {
            navController.navigate(R.id.splashFragment)
        }
    }

    private fun <F: Fragment>changeFragment(fragmentClass: Class<F>, tag: String) {
        childFragmentManager.commit {
            replace(R.id.ledstrip_container, fragmentClass.newInstance(),tag)
            setReorderingAllowed(true)
            addToBackStack("replacement")
        }
    }
}
