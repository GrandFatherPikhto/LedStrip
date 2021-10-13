package com.grandfatherpikhto.ledstrip.ui.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class LedstripViewModel:ViewModel() {
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
    private val _blinkFrequency = MutableLiveData<Float>(20F)
    val blinkFrequency: LiveData<Float>
        get() = _blinkFrequency
    private val _tailSpeed = MutableLiveData<Float>(50F)
    val tailSpeed: LiveData<Float>
        get() = _tailSpeed
    private val _tailLength = MutableLiveData<Float>(25F)
    val tailLength: LiveData<Float>
        get() = _tailLength
    private val _tagSpeed = MutableLiveData<Float>(50F)
    val tagSpeed: LiveData<Float>
        get() = _tagSpeed
    private val _tagBrightness = MutableLiveData<Float>(100F)
    val tagBrightness: LiveData<Float>
        get() = _tagBrightness
    private val _waterSpeed = MutableLiveData<Float>(50F)
    val waterSpeed: LiveData<Float>
        get() = _waterSpeed
    private val _waterBrightness = MutableLiveData<Float>(100F)
    val waterBrightness: LiveData<Float>
        get() = _waterBrightness


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

    fun changeRegime(value:BtLeService.Regime) {
        if(_regime.value != value) {
            _regime.value = value
            service?.writeRegime(value)
        }
    }

    fun changeColor(value:Int) {
        if(_color.value != value) {
            _color.value = value
            service?.writeColor(value)
        }
    }

    fun changeBlinkFrequency(value:Float) {
        _blinkFrequency.value = value
    }

    fun changeTailSpeed(value:Float) {
        _tailSpeed.value = value
    }

    fun changeTailLength(value:Float) {
        _tailLength.value = value
    }

    fun changeTagSpeed(value:Float) {
        _tagSpeed.value = value
    }

    fun changeTagBrightness(value:Float) {
        _tagBrightness.value = value
    }

    fun changeWaterSpeed(value:Float) {
        _waterSpeed.value = value
    }

    fun changeWaterBrightness(value:Float) {
        _waterBrightness.value = value
    }
}