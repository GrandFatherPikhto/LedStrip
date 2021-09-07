package com.grandfatherpikhto.ledstrip.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.helper.AppConst
import com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter.BtLeDevice

open class BluetoothLeReceiver(private val listener: BluetoothLeCallback) : BroadcastReceiver() {
    /**
     *
     */
    interface BluetoothLeCallback {
        fun gattConnected() {}
        fun gattDisconnected() {}
        fun gattDiscovered() {}
        fun receiveColor(color:Int) {}
        fun receiveRegime(regime:Int) {}
        fun scanStart() {}
        fun scanStop() {}
        fun scanFind(btLeDevice: BtLeDevice) {}
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action!!) {
            BluetoothLeService.ACTION_GATT_CONNECTED -> {
                listener.gattConnected()
            }
            BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                intent.extras?.keySet()?.forEach {
                    when (it) {
                        BluetoothLeService.REGIME_DATA -> {
                            val regime =
                                intent.getIntExtra(BluetoothLeService.REGIME_DATA, 0)
                            listener.receiveRegime(regime)
                        }
                        BluetoothLeService.COLOR_DATA -> {
                            val color = intent.getIntExtra(BluetoothLeService.COLOR_DATA, -1)
                            listener.receiveColor(color)
                        }
                        else -> {

                        }
                    }
                }
            }
            BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                listener.gattDisconnected()
            }

            BluetoothLeService.ACTION_GATT_DISCOVERED -> {
                listener.gattDiscovered()
            }

            BluetoothLeService.ACTION_DEVICE_SCAN_START -> {
                listener.scanStart()
            }

            BluetoothLeService.ACTION_DEVICE_SCAN_STOP -> {
                listener.scanStop()
            }

            BluetoothLeService.ACTION_DEVICE_SCAN_FIND -> {
                val btDevice = BtLeDevice(
                    intent.getStringExtra(AppConst.btAddress) ?: context!!.getString(R.string.default_bt_device_address),
                    intent.getStringExtra(AppConst.btName) ?: context!!.getString(R.string.default_bt_device_name),
                    intent.getIntExtra(AppConst.btBound, -1)
                )
                listener.scanFind(btDevice)
            }
        }
    }
}