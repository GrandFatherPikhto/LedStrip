package com.grandfatherpikhto.ledstrip.ui.scan.rvbtdadapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.BtDeviceBinding

class RvBtDeviceAdapter : RecyclerView.Adapter<RvBtDeviceAdapter.RvBtDeviceHolder>(){
    /** Список устройств */
    private val devices:MutableList<BtLeDevice> = mutableListOf()

    /** Коллбэк для обработки нажатия и долгого нажатия на элемент списка */
    var btDeviceAdapterCallback: RvBtDeviceCallback<BtLeDevice>? = null
    /** Холдер для лэйаута устройства */
    class RvBtDeviceHolder(item: View): RecyclerView.ViewHolder(item) {
        /** Привязка к элементам лэйаута устройства */
        private val binding = BtDeviceBinding.bind(item)

        fun bind(btDevice: BtLeDevice) {
            with(binding) {
                btDevice.name.also {
                    if(it == null) {
                        tvBtDeviceName.text = itemView.context.getString(R.string.default_bt_device_name)
                    } else {
                        tvBtDeviceName.text = it
                    }
                }
                btDevice.address.also { tvBtDeviceAddress.text = btDevice.address }
                if(btDevice.bondState == BluetoothDevice.BOND_NONE) {
                    ivBtPaired.setImageResource(R.drawable.bluetooth_not_paired_24)
                } else {
                    ivBtPaired.setImageResource(R.drawable.bluetooth_paired_24)
                }
            }
        }
    }

    /** Создаём очередной элемент лэйаута холдера. Операция ресурсоёмкая */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvBtDeviceHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bt_device, parent, false)
        return RvBtDeviceHolder(view)
    }

    /** Привязка холдера и события нажатия на элемент к обработчику события */
    override fun onBindViewHolder(holder: RvBtDeviceHolder, position: Int) {
        holder.itemView.setOnClickListener {
            btDeviceAdapterCallback?.onDeviceClick(devices[position], holder.itemView)
        }

        holder.itemView.setOnLongClickListener {
            btDeviceAdapterCallback?.onDeviceLongClick(devices[position], holder.itemView)
            return@setOnLongClickListener true
        }

        holder.bind(devices[position])
    }

    /** Вернуть количество элементов списка */
    override fun getItemCount(): Int {
        return devices.size
    }

    /** Добавить устройство в список с обновлением списка */
    public fun addBtDevice(device: BtLeDevice) {
        if(!devices.contains(device)) {
            val exist: BtLeDevice? = devices.find { it.address.equals(device.address) }
            if (exist == null) {
                devices.add(device)
                notifyDataSetChanged()
            }
        }
    }

    /** Очистить список с обновлением отображения */
    fun clearBtDevices() {
        devices.clear()
        notifyDataSetChanged()
    }

    /** Залить устройства списком */
    fun setBtDevicesList(devices: List<BtLeDevice>) {
        this.devices.clear()
        this.devices.addAll(devices)
        notifyDataSetChanged()

    }

    /** Это для работы со списком устройств, который возвращает  */
    fun setBtDevices(devices: Set<BtLeDevice>) {
        this.devices.clear()
        this.devices.addAll(devices)
        notifyDataSetChanged()
    }


    /** Привязка к событию Click */
    fun setOnItemClickListener(btDeviceAdapterCallback: RvBtDeviceCallback<BtLeDevice>) {
        this.btDeviceAdapterCallback = btDeviceAdapterCallback
    }
}