package com.grandfatherpikhto.ledstrip.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ContainerViewModel : ViewModel() {
    private var _page: MutableLiveData<Int> = MutableLiveData<Int>(0)
    val page: LiveData<Int>
        get() = _page

    fun changePage(value:Int) {
        _page.value = value
    }
}