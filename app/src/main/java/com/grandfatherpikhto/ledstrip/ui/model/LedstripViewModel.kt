package com.grandfatherpikhto.ledstrip.ui.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class LedstripViewModel:ViewModel() {
    companion object {
        const val TAG = "LedstripViewModel"
    }

    private var service:BtLeService ?= null

    private val _state = MutableLiveData<BtLeService.State> ()
    val state
        get() = _state
    private val _regime = MutableLiveData<BtLeService.Regime> ()
    val regime
        get() = _regime
    private val _color = MutableLiveData<Int> ()
    val color
        get() = _color
    private val _frequency = MutableLiveData<Float>(20F)
    val frequency: LiveData<Float>
        get() = _frequency
    private val _speed = MutableLiveData<Float>(50F)
    val speed: LiveData<Float>
        get() = _speed
    private val _length = MutableLiveData<Float>(25F)
    val length: LiveData<Float>
        get() = _length
    private val _brightness = MutableLiveData<Float>(100F)
    val brightness: LiveData<Float>
        get() = _brightness

    init {
        viewModelScope.launch {
            BtLeServiceConnector.service.collect { value ->
                service = value
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.state.collect { value ->
                _state.value = value
                state.postValue(value)
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.regime.collect { value ->
                _regime.postValue(value)
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.color.collect { value ->
                _color.postValue(value)
            }
        }
    }

    @DelicateCoroutinesApi
    fun changeRegime(value:BtLeService.Regime) {
        _regime.value = value
        service?.writeRegime(value)
    }

    @DelicateCoroutinesApi
    fun changeColor(value:Int) {
        _color.value = value
        service?.writeColor(value)
    }

    fun changeFrequency(value:Float) {
        _frequency.value = value
        service?.writeFrequency(value)
    }

    fun changeSpeed(value:Float) {
        _speed.value = value
        service?.writeSpeed(value)
    }

    fun changeLength(value:Float) {
        _length.value = value
        service?.writeLength(value)
    }

    fun changeBrightness(value:Float) {
        _brightness.value = value
        service?.writeBrightness(value)
    }
}