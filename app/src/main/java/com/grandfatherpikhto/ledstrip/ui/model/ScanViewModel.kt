package com.grandfatherpikhto.ledstrip.ui.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeDevice
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeScanServiceConnector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ScanViewModel:ViewModel() {
    companion object {
        const val TAG = "ScanViewModel"
    }
    private val _service = MutableLiveData<BtLeScanService>()
    val service:LiveData<BtLeScanService> get() = _service
    private val devicesList = mutableListOf<BtLeDevice>()
    private val _devices = MutableLiveData<List<BtLeDevice>>(listOf())
    val devices:LiveData<List<BtLeDevice>> get() = _devices
    private val _state = MutableLiveData<BtLeScanService.State>(BtLeScanService.State.Stop)
    val state get() = _state

    init {
        viewModelScope.launch {
            BtLeScanServiceConnector.device.collect { device ->
                Log.d(TAG, "Получено устройство $device")
                if(devicesList.find { it.address == device.address } == null) {
                    devicesList.add(device)
                    _devices.postValue(devicesList.toList())
                }
            }
        }

        viewModelScope.launch {
            BtLeScanServiceConnector.state.collect { state ->
                _state.postValue(state)
            }
        }

        viewModelScope.launch {
            BtLeScanServiceConnector.service.collect { service ->
                _service.postValue(service)
            }
        }
    }

    fun clean() {
        devicesList.clear()
        viewModelScope.launch {
            _devices.postValue(listOf())
        }
    }
}