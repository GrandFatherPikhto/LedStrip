package com.grandfatherpikhto.ledstrip.ui.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeScanServiceConnector
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SplashViewModel:ViewModel() {
    private val _state = MutableLiveData<BtLeService.State>()
    val state: LiveData<BtLeService.State> get() = _state
    private val _service = MutableLiveData<BtLeService>()
    val service:LiveData<BtLeService> get() =  _service

    init {
        viewModelScope.launch {
            BtLeServiceConnector.service.collect{ value ->
                _service.postValue(value)
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.state.collect { value ->
                _state.postValue(value)
            }
        }
    }
}