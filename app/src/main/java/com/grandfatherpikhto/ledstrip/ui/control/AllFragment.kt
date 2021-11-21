package com.grandfatherpikhto.ledstrip.ui.control

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.grandfatherpikhto.ledstrip.R

/**
 * A simple [Fragment] subclass.
 * Use the [AllFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AllFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AllFragment.
         */
        // TODO: Rename and change types and number of parameters
        const val TAG    = "allFragment"
        const val NAME   = "allFragment"
        const val COLOR  = "allColor"
        const val REGIME = "allRegime"
        @JvmStatic
        fun newInstance() = AllFragment()
    }
}