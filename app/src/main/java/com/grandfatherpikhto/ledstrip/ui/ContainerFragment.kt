package com.grandfatherpikhto.ledstrip.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.FragmentContainerBinding
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import com.grandfatherpikhto.ledstrip.ui.adapter.ContainerPagerAdapter
import com.grandfatherpikhto.ledstrip.ui.control.*
import com.grandfatherpikhto.ledstrip.ui.model.ContainerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ContainerFragment : Fragment() {

    companion object {
        const val TAG = "ContainerFragment"
        const val NAME = "ContainerFragment"
        const val CURRENT_PAGE = "ContainerFragmentPage"
        fun newInstance() = ContainerFragment()
    }

    private lateinit var viewModel: ContainerViewModel
    private var _binding: FragmentContainerBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private var pagerAdapter: ContainerPagerAdapter ?= null
    private val containerViewModel:ContainerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // return inflater.inflate(R.layout.container_fragment, container, false)
        val fragments = arrayListOf<Fragment>(
            ColorFragment.newInstance(),
            TagFragment.newInstance(),
            WaterFragment.newInstance(),
            TailFragment.newInstance(),
            BlinkFragment.newInstance()
        )

        pagerAdapter = ContainerPagerAdapter(fragments, requireActivity().supportFragmentManager, requireActivity().lifecycle)
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _binding = FragmentContainerBinding.inflate(inflater, container, false)

        binding.apply {
            vpContainer.adapter = pagerAdapter
            vpContainer.registerOnPageChangeCallback (object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if(containerViewModel.page.value != position) {
                        containerViewModel.changePage(position)
                    }
                }
            })
            containerViewModel.page.observe(viewLifecycleOwner, { page ->
                if(vpContainer.currentItem != page) {
                    lifecycleScope.launch {
                        delay(50)
                        vpContainer.currentItem = page
                    }
                }
            })
        }


        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        sharedPreferences.apply {
            containerViewModel.changePage(getInt(CURRENT_PAGE, 0))
        }
    }


    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop()")
        sharedPreferences.edit {
            putInt(CURRENT_PAGE, containerViewModel.page.value!!)
            commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView ${BtLeServiceConnector.service.value}")
        //
    }

    override fun onDestroy() {
        super.onDestroy()
        BtLeServiceConnector.service.value?.close()
        Log.d(TAG, "onDestroy()")
    }
}