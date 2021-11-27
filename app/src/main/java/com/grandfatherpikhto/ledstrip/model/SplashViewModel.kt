package com.grandfatherpikhto.ledstrip.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.M)
@DelicateCoroutinesApi
@InternalCoroutinesApi
class SplashViewModel:ViewModel() {
    private val sharedBond = MutableLiveData<Boolean>(false)
    val bond:LiveData<Boolean> get() = sharedBond
    private val sharedState = MutableLiveData<BtLeService.State>()
    val state: LiveData<BtLeService.State> get() = sharedState
    private var btLeService:BtLeService ?= null
    val service:BtLeService? get() = btLeService

    init {
        viewModelScope.launch {
            BtLeServiceConnector.bond.collect{ value ->
                sharedBond.postValue(value)
                btLeService = BtLeServiceConnector.service
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.state.collect { value ->
                sharedState.postValue(value)
            }
        }
    }
}