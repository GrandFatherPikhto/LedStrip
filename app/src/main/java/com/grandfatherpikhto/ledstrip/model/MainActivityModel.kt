package com.grandfatherpikhto.ledstrip.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.ui.LedstripFragment
import com.grandfatherpikhto.ledstrip.ui.MainActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
@DelicateCoroutinesApi
class MainActivityModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>().applicationContext
    companion object {
        const val TAG = "MainActivityModel"
    }

    private val _fragment = MutableLiveData<MainActivity.Current>(MainActivity.Current.Ledstrip)
    val fragment: LiveData<MainActivity.Current> get() = _fragment
    private val _control  = MutableLiveData<LedstripFragment.Current>(LedstripFragment.Current.Regime)
    val control:LiveData<LedstripFragment.Current> get() = _control
    private val _address = MutableLiveData<String>(context.getString(R.string.default_device_address))
    val address: LiveData<String> get() = _address
    private val _name = MutableLiveData<String>(context.getString(R.string.default_device_name))
    val name: LiveData<String> get() = _name

    fun changeFragment(fragment : MainActivity.Current) {
        _fragment.postValue(fragment)
    }

    fun changeControl(control: LedstripFragment.Current) {
        _control.postValue(control)
    }

    fun changeName(name:String) {
        _name.postValue(name)
    }

    fun changeAddress(address:String) {
        _address.postValue(address)
    }
}