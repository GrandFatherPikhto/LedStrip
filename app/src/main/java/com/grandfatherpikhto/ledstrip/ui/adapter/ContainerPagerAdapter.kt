package com.grandfatherpikhto.ledstrip.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ContainerPagerAdapter(private val fragments: ArrayList<Fragment>,
                            fm: FragmentManager,
                            lifecycle: Lifecycle
            ): FragmentStateAdapter(fm, lifecycle) {
    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}