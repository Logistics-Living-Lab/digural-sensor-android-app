package de.digural.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import de.digural.app.R
import de.digural.app.bluetooth.BluetoothConnectionState
import de.digural.app.device.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@AndroidEntryPoint
class DevicesFragment : Fragment() {
    private val LOG_TAG = DevicesFragment::class.java.name

    private val viewModel: DevicesViewModel by viewModels()

    private lateinit var rvDevices: RecyclerView

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view.findViewById<View>(R.id.btnAddDevice) as FloatingActionButton).setOnClickListener {
            findNavController().navigate(R.id.action_devices_to_deviceScanResultsFragment)
        }

        rvDevices = view.findViewById(R.id.rvDevices)

        rvDevices.layoutManager = LinearLayoutManager(context);

        viewModel._devices.observe(viewLifecycleOwner) {
            rvDevices.adapter = DeviceAdapter(it)
        }

//        viewModel.connectionState.observe(viewLifecycleOwner) {
//            rvDevices.adapter?.let { adapter ->
//                (adapter as DeviceAdapter).notifyDataSetChanged()
//            }
//        }

    }

    inner class DeviceAdapter(private var _devices: List<DeviceEntity>) :
        RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {


        inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
            lateinit var item: DeviceEntity

            val textView = listItemView.findViewById<View>(R.id.label) as TextView
            val tvDescription = listItemView.findViewById<View>(R.id.description) as TextView
            val btnConnectDevice = listItemView.findViewById(R.id.btnConnect) as Button
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var deviceView: View
            var viewHolder: ViewHolder? = null

            deviceView = layoutInflater.inflate(
                R.layout.device_element,
                parent,
                false
            )

            deviceView.setOnLongClickListener {
                viewModel.onLongPress(it, viewHolder!!.item, this@DevicesFragment)
                return@setOnLongClickListener true
            }

            viewHolder = ViewHolder(deviceView)
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = _devices[position]
            holder.item = device

            device._actualConnectionState.asLiveData().observe(viewLifecycleOwner) {
                when (it) {
                    BluetoothConnectionState.CONNECTED -> {
                        holder.btnConnectDevice.text = getString(R.string.devices_disconnect)
                        holder.btnConnectDevice.isEnabled = true
                    }

                    BluetoothConnectionState.CONNECTING -> {
                        holder.btnConnectDevice.text = getString(R.string.devices_connecting)
                        holder.btnConnectDevice.isEnabled = false
                    }

                    BluetoothConnectionState.RECONNECTING -> {
                        holder.btnConnectDevice.text = getString(R.string.devices_reconnecting)
                        holder.btnConnectDevice.isEnabled = false
                    }

                    BluetoothConnectionState.DISCONNECTED -> {
                        holder.btnConnectDevice.text = getString(R.string.devices_connect)
//                    holder.btnConnectDevice.isEnabled =
//                        viewModel.connectionState.value == BluetoothConnectionState.DISCONNECTED
                    }

                    else -> null //Throws error w/o else branch
                }
            }



            device.let {
                holder.textView.text = it._name.value
                holder.tvDescription.text = it._macAddress.value
            }

            holder.btnConnectDevice.setOnClickListener {
                viewModel.onDeviceConnectClicked(device)
            }

        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            super.onDetachedFromRecyclerView(recyclerView)
        }

        override fun getItemCount(): Int {
            return _devices.size
        }

        fun setItems(devices: List<DeviceEntity>) {
            _devices = devices
            notifyDataSetChanged()
        }
    }

}