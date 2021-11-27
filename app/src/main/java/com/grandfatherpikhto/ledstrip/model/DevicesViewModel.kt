package com.grandfatherpikhto.ledstrip.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeDevice
import com.grandfatherpikhto.ledstrip.service.BtLeScanService
import com.grandfatherpikhto.ledstrip.service.BtLeScanServiceConnector
import com.grandfatherpikhto.ledstrip.ui.DevicesFragment
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@InternalCoroutinesApi
class DevicesViewModel:ViewModel() {
    companion object {
        const val TAG = "ScanViewModel"
    }
    private val devicesList = mutableListOf<BtLeDevice>()
    private val _bound = MutableLiveData<Boolean>(false)
    val bound:LiveData<Boolean> get() = _bound
    private val _devices = MutableLiveData<List<BtLeDevice>>(listOf())
    val devices:LiveData<List<BtLeDevice>> get() = _devices
    private val _state = MutableLiveData<BtLeScanService.State>(BtLeScanService.State.Stop)
    val state get() = _state
    private val _action = MutableLiveData<DevicesFragment.Action>(DevicesFragment.Action.Scan)
    val action: LiveData<DevicesFragment.Action> get() = _action

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
            BtLeScanServiceConnector.bound.collect { bond ->
                _bound.postValue(bond)
            }
        }
    }

    fun clean() {
        devicesList.clear()
        viewModelScope.launch {
            _devices.postValue(listOf())
        }
    }

    fun changeAction(action : DevicesFragment.Action) {
        _action.value = action
        Log.d(TAG, "Action: $action")
        _action.postValue(action)
    }
}