package com.grandfatherpikhto.ledstrip.ui.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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