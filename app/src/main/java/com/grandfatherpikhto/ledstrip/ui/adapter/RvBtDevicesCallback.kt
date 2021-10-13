package com.grandfatherpikhto.ledstrip.ui.adapter

import android.view.View

interface RvBtDevicesCallback<T> {
    fun onDeviceClick(model: T, view: View)
    fun onDeviceLongClick(model: T, view: View)
}