package com.grandfatherpikhto.ledstrip.ui.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.grandfatherpikhto.ledstrip.R
import com.grandfatherpikhto.ledstrip.databinding.BtDeviceBinding
import com.grandfatherpikhto.ledstrip.service.BtLeDevice

class RvBtDevicesAdapter : RecyclerView.Adapter<RvBtDevicesAdapter.RvBtDeviceHolder>(){
    /** Список устройств */
    private val leDevices:MutableList<BtLeDevice> = mutableListOf()

    /** Коллбэк для обработки нажатия и долгого нажатия на элемент списка */
    private var btLeDeviceAdapterCallback: RvBtDevicesCallback<BtLeDevice>? = null
    /** Холдер для лэйаута устройства */
    class RvBtDeviceHolder(item: View): RecyclerView.ViewHolder(item) {
        /** Привязка к элементам лэйаута устройства */
        private val binding = BtDeviceBinding.bind(item)

        fun bind(btLeDevice: BtLeDevice) {
            with(binding) {
                btLeDevice.name.also {
                    if(it.isEmpty()) {
                        tvBtDeviceName.text = itemView.context.getString(R.string.default_device_name)
                    } else {
                        tvBtDeviceName.text = it
                    }
                }
                btLeDevice.address.also { tvBtDeviceAddress.text = btLeDevice.address }
                if(btLeDevice.bondState == BluetoothDevice.BOND_NONE) {
                    ivBtPaired.setImageResource(R.drawable.ic_baseline_bluetooth_48)
                } else {
                    ivBtPaired.setImageResource(R.drawable.ic_baseline_bluetooth_connected_48)
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
            btLeDeviceAdapterCallback?.onDeviceClick(leDevices[position], holder.itemView)
        }

        holder.itemView.setOnLongClickListener {
            btLeDeviceAdapterCallback?.onDeviceLongClick(leDevices[position], holder.itemView)
            return@setOnLongClickListener true
        }

        holder.bind(leDevices[position])
    }

    /** Вернуть количество элементов списка */
    override fun getItemCount(): Int {
        return leDevices.size
    }

    /** Добавить устройство в список с обновлением списка */
    fun addBtDevice(leDevice: BtLeDevice) {
        if(!leDevices.contains(leDevice)) {
            val exist: BtLeDevice? = leDevices.find { it.address.equals(leDevice.address) }
            if (exist == null) {
                leDevices.add(leDevice)
                notifyDataSetChanged()
            }
        }
    }

    /** Очистить список с обновлением отображения */
    fun clearBtDevices() {
        leDevices.clear()
        notifyDataSetChanged()
    }

    /** Залить устройства списком */
    fun setBtDevicesList(leDevices: List<BtLeDevice>) {
        this.leDevices.clear()
        this.leDevices.addAll(leDevices)
        notifyDataSetChanged()

    }

    /** Это для работы со списком устройств, который возвращает  */
    fun setBtDevices(leDevices: Set<BtLeDevice>) {
        this.leDevices.clear()
        this.leDevices.addAll(leDevices)
        notifyDataSetChanged()
    }


    /** Привязка к событию Click */
    fun setOnItemClickListener(btLeDeviceAdapterCallback: RvBtDevicesCallback<BtLeDevice>) {
        this.btLeDeviceAdapterCallback = btLeDeviceAdapterCallback
    }
}