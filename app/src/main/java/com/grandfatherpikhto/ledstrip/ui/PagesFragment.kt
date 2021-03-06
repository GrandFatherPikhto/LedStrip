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
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.grandfatherpikhto.ledstrip.databinding.FragmentPagesBinding
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import com.grandfatherpikhto.ledstrip.ui.adapter.ContainerPagerAdapter
import com.grandfatherpikhto.ledstrip.ui.control.*
import com.grandfatherpikhto.ledstrip.model.ContainerViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class PagesFragment : Fragment() {

    companion object {
        const val TAG = "ContainerFragment"
        const val NAME = "ContainerFragment"
        const val CURRENT_PAGE = "ContainerFragmentPage"
        fun newInstance() = PagesFragment()
    }

    private lateinit var viewModel: ContainerViewModel
    private var _binding: FragmentPagesBinding? = null
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
            AllFragment.newInstance(),
            TagFragment.newInstance(),
            WaterFragment.newInstance(),
            TailFragment.newInstance(),
            BlinkFragment.newInstance()
        )

        pagerAdapter = ContainerPagerAdapter(fragments, requireActivity().supportFragmentManager, requireActivity().lifecycle)
        sharedPreferences = requireContext().getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _binding = FragmentPagesBinding.inflate(inflater, container, false)

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
        sharedPreferences.apply {
            containerViewModel.changePage(getInt(CURRENT_PAGE, 0))
        }
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStop() {
        super.onStop()
        sharedPreferences.edit {
            putInt(CURRENT_PAGE, containerViewModel.page.value!!)
            commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView ${BtLeServiceConnector.service}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy()")
    }
}