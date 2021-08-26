package com.grandfatherpikhto.ledstrip.rvbtdadapter

import android.view.View

interface RvBtDeviceCallback<T> {
    fun onDeviceClick(model: T, view: View)
    fun onDeviceLongClick(model: T, view: View)
}