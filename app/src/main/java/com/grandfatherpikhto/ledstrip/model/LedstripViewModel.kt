package com.grandfatherpikhto.ledstrip.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grandfatherpikhto.ledstrip.service.BtLeService
import com.grandfatherpikhto.ledstrip.service.BtLeServiceConnector
import com.grandfatherpikhto.ledstrip.ui.LedstripFragment
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@DelicateCoroutinesApi
@InternalCoroutinesApi
class LedstripViewModel:ViewModel() {
    companion object {
        const val TAG = "LedstripViewModel"
    }

    private var service:BtLeService ?= null

    private val sharedBond = MutableLiveData<Boolean>(false)
    val bond:LiveData<Boolean> = sharedBond

    private val sharedState = MutableLiveData<BtLeService.State> ()
    val state:LiveData<BtLeService.State>
        get() = sharedState

    private val sharedFragment = MutableLiveData<LedstripFragment.Current>(LedstripFragment.Current.Regime)
    val fragment: LiveData<LedstripFragment.Current> get() = sharedFragment

    private val sharedRegime = MutableLiveData<Regime> ()
    val regime
        get() = sharedRegime
    private val sharedColor = MutableLiveData<Int> ()
    val color
        get() = sharedColor
    private val sharedFrequency = MutableLiveData<Float>(20F)
    val frequency: LiveData<Float>
        get() = sharedFrequency
    private val sharedSpeed = MutableLiveData<Float>(50F)
    val speed: LiveData<Float>
        get() = sharedSpeed
    private val sharedLength = MutableLiveData<Float>(25F)
    val length: LiveData<Float>
        get() = sharedLength
    private val sharedBrightness = MutableLiveData<Float>(100F)
    val brightness: LiveData<Float>
        get() = sharedBrightness

    init {
        viewModelScope.launch {
            BtLeServiceConnector.bond.collect { bond ->
                service = BtLeServiceConnector.service
                sharedBond.postValue(bond)
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.state.collect { value ->
                Log.d(TAG, "State: $value")
                sharedState.postValue(value)
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.regime.collect { value ->
                sharedRegime.postValue(value)
            }
        }
        viewModelScope.launch {
            BtLeServiceConnector.color.collect { value ->
                sharedColor.postValue(value)
            }
        }
    }

    @DelicateCoroutinesApi
    fun changeRegime(value: Regime) {
        sharedRegime.value = value
        service?.writeRegime(value)
    }

    @DelicateCoroutinesApi
    fun changeColor(value:Int) {
        sharedColor.value = value
        service?.writeColor(value)
    }

    fun changeFrequency(value:Float) {
        sharedFrequency.value = value
        service?.writeFrequency(value)
    }

    fun changeSpeed(value:Float) {
        sharedSpeed.value = value
        service?.writeSpeed(value)
    }

    fun changeLength(value:Float) {
        sharedLength.value = value
        service?.writeLength(value)
    }

    fun changeBrightness(value:Float) {
        sharedBrightness.value = value
        service?.writeBrightness(value)
    }

    fun changeFragment(value: LedstripFragment.Current) {
        sharedFragment.postValue(value)
    }
}