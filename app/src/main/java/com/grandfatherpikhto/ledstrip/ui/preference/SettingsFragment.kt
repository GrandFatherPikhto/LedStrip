package com.grandfatherpikhto.ledstrip.ui.preference

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.grandfatherpikhto.ledstrip.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.ledstrip_preferences, rootKey)
    }
}