package de.digural.app.ui

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainer
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import de.digural.app.R
import de.digural.app.auth.UserInfo
import de.digural.app.bluetooth.BluetoothConnectionState
import de.digural.app.bluetooth.BluetoothDeviceType
import de.digural.app.device.DeviceEntity
import de.digural.app.location.LocationValue
import de.digural.app.theming.ThemingManager
import de.digural.app.tracking.TrackingState

@AndroidEntryPoint
class HomeFragment : Fragment(), OnMapReadyCallback {

    private val LOG_TAG = HomeFragment::class.java.name

    private val viewModel: HomeViewModel by viewModels()
    private var mapView: MapView? = null

    private lateinit var constraintLayout: ConstraintLayout

    private lateinit var tvBtConnectHeader: TextView
    private lateinit var btnBtConnect: FloatingActionButton
    private lateinit var tvBtConnectFooter: TextView

    private lateinit var tvUserHeader: TextView
    private lateinit var btnUser: FloatingActionButton
    private lateinit var tvUserFooter: TextView

    private lateinit var tvTrackingHeader: TextView
    private lateinit var btnTracking: FloatingActionButton
    private lateinit var tvTrackingFooter: TextView

    //    private lateinit var rvDevices: RecyclerView
    private lateinit var rvDeviceLabels: RecyclerView

    private lateinit var tvVersionInfo: TextView
    private lateinit var fragmentContainer: FragmentContainer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.location.observe(viewLifecycleOwner) { location ->
            updateLocation(location)
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            updateUser(user)
        }

        viewModel.trackingEnabled.observe(viewLifecycleOwner) { trackingEnabled ->
            btnTracking.isEnabled = trackingEnabled
            if (!trackingEnabled) {
                btnTracking.backgroundTintList
            }

            if (trackingEnabled && viewModel.trackingState.value == TrackingState.NOT_TRACKING) {
                styleFabButtonInactive(btnTracking)
            }
        }

        viewModel.trackingState.observe(viewLifecycleOwner) { trackingState ->
            when (trackingState) {
                TrackingState.TRACKING, TrackingState.LOCATION_ONLY -> onStartTracking()
                TrackingState.NOT_TRACKING -> onStopTracking()
            }
        }

        viewModel.connectedDevices.observe(viewLifecycleOwner) { connectedDevices ->
            if (connectedDevices.isNotEmpty()) {
                tvBtConnectFooter.text = connectedDevices.size.toString() + getString(R.string.home_sensors)
                styleFabButtonActive(btnBtConnect)
            } else {
                tvBtConnectFooter.text = getString(R.string.home_no_sensors_connected)
                styleFabButtonInactive(btnBtConnect)
            }
        }

        constraintLayout = view.findViewById(R.id.homeParentLayout)

        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        tvBtConnectHeader = view.findViewById(R.id.tvBtConnectionHeader)
        btnBtConnect = view.findViewById(R.id.btnBtConnection)
        btnBtConnect.setOnClickListener {
            viewModel.onBtConnectClicked()
        }

        tvBtConnectFooter = view.findViewById(R.id.tvBtConnectionFooter)
        tvBtConnectFooter.isSelected = true

        tvUserHeader = view.findViewById(R.id.tvUserHeader)
        btnUser = view.findViewById(R.id.btnUser)
        btnUser.setOnClickListener {
            activity?.let {
                viewModel.onUserButtonClicked(it)
            }
        }

        tvUserFooter = view.findViewById(R.id.tvUserFooter)
        tvUserFooter.isSelected = true

        tvTrackingHeader = view.findViewById(R.id.tvDataTransmissionHeader)
        btnTracking = view.findViewById(R.id.btnDataTransmission)
        btnTracking.setOnClickListener {
            Log.v(LOG_TAG, "btnTracking Clicked!")
            viewModel.onToggleTrackingClicked()
        }
        tvTrackingFooter = view.findViewById(R.id.tvDataTransmissionFooter)

//        rvDevices = view.findViewById(R.id.rvDevices)
//        rvDevices.layoutManager = LinearLayoutManager(context)
//        viewModel.connectedDevices.observe(viewLifecycleOwner) {
//            rvDevices.adapter = DeviceAdapter(it, viewLifecycleOwner)
//        }

        rvDeviceLabels = view.findViewById(R.id.rvSensorId)
        rvDeviceLabels.layoutManager = LinearLayoutManager(context)
        (rvDeviceLabels.layoutManager as LinearLayoutManager).orientation =
            LinearLayoutManager.HORIZONTAL

        viewModel.linkedDevices.observe(viewLifecycleOwner) { connectedDevices ->
            rvDeviceLabels.adapter = DeviceTabsAdapter(connectedDevices, viewLifecycleOwner)
            if (connectedDevices.isEmpty()) {
                viewModel.selectedDevice.value = null
            } else {
                if (viewModel.selectedDevice.value == null) {
                    viewModel.selectedDevice.value = connectedDevices[0]
                }
            }
        }

        tvVersionInfo = view.findViewById(R.id.tvVersionInfo)
        tvVersionInfo.text = getString(R.string.home_version, viewModel.versionInfo)

        renderUiForDevice(null)

        viewModel.selectedDevice.asLiveData().observe(viewLifecycleOwner) {
            renderUiForDevice(it)
        }
    }

    fun onStartTracking() {
        tvTrackingFooter.text = getString(R.string.home_active)
        styleFabButtonActive(btnTracking)
    }

    fun onStopTracking() {
        tvTrackingFooter.text = getString(R.string.home_inactive)
        if (btnTracking.isEnabled) {
            styleFabButtonInactive(btnTracking)
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()

        try {
            viewModel.checkForUpdates(this.requireActivity())
        } catch (e: IllegalStateException) {
            Log.e(LOG_TAG, "Error HomeFragment check for updates: ${e.message}")
        }
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    private fun updateUser(user: UserInfo?) {
        val userCapture = user

        if (userCapture != null) {
            tvUserFooter.text = userCapture.username
            styleFabButtonActive(btnUser)
        } else {
            tvUserFooter.text = getString(R.string.home_not_logged_in)
            styleFabButtonInactive(btnUser)
        }

        if (userCapture?.isTrackOnlyUser() == true) {
            viewModel.startLocationUpdates()
        }
    }

    private fun updateLocation(location: LocationValue?) {
        location?.let {
            mapView?.getMapAsync {
                it.clear()
                it.addMarker(
                    MarkerOptions()
                        .position(
                            LatLng(
                                location.latitude,
                                location.longitude
                            )
                        )
                )

                val cameraPosition = CameraPosition.Builder()
                    .target(
                        LatLng(
                            location.latitude,
                            location.longitude
                        )
                    )
                    .zoom(18f)
                    .build()

                val cameraUpdate =
                    CameraUpdateFactory
                        .newCameraPosition(cameraPosition)

                it.moveCamera(cameraUpdate)
            }
        }

    }

    override fun onMapReady(map: GoogleMap) {
        val isNightMode =
            requireContext()
                .resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            map?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_night_style
                )
            )
        }
    }

    private fun styleFabButtonInactive(fabButton: FloatingActionButton) {
        fabButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.grey))
    }

    private fun styleFabButtonActive(fabButton: FloatingActionButton) {
        fabButton.backgroundTintList =
            ColorStateList.valueOf(
                viewModel.themingManager.getThemeColor(
                    context,
                    R.attr.colorPrimary
                )
            )
    }

    inner class DeviceTabsAdapter(
        private var _devices: List<DeviceEntity>,
        private val lifecycleOwner: LifecycleOwner
    ) :
        RecyclerView.Adapter<DeviceTabsAdapter.ViewHolder>() {


        inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
            lateinit var item: DeviceEntity
            var btnDevice: Button = listItemView.findViewById(R.id.btnDevice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            var viewHolder: ViewHolder? = null

            val deviceView: View = layoutInflater.inflate(
                R.layout.layout_sensor_selection,
                parent,
                false
            )

            deviceView.setOnLongClickListener {
                return@setOnLongClickListener true
            }


            viewHolder = ViewHolder(deviceView)
            return viewHolder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val device = _devices[position]
            holder.item = device
            holder.btnDevice.text = getDeviceButtonText(device)


            holder.btnDevice.setOnClickListener {
                viewModel.onDeviceButtonClicked(device)
            }

            device._actualConnectionState.asLiveData().observe(viewLifecycleOwner) {
                if (it == BluetoothConnectionState.DISCONNECTED) {
                    setButtonColor(holder, R.color.design_default_color_error)
                } else if (it == BluetoothConnectionState.CONNECTED) {
                    setButtonColor(holder, R.color.light_green)
                } else {
                    setButtonColor(
                        holder,
                        viewModel.themingManager.getThemeColorResourceId(
                            context,
                            R.attr.colorPrimary
                        )
                    )
                }
            }
        }

        protected fun getDeviceButtonText(device: DeviceEntity): String {
            var text = (device._name.value ?: getString(
                R.string.home_unknown,
                device.getBluetoothDeviceType()
            ))
            val actualConnectionState = device._actualConnectionState.value
            if (actualConnectionState == BluetoothConnectionState.CONNECTING || actualConnectionState == BluetoothConnectionState.RECONNECTING) {
                text += "..."
            }
            return text;
        }

        protected fun setButtonColor(holder: ViewHolder, color: Int) {
            context?.let { context ->
                holder.btnDevice.setBackgroundColor(
                    viewModel.themingManager.getColor(context, color)
                )
            }
        }

        override fun getItemCount(): Int {
            return _devices.size
        }

        fun setItems(devices: List<DeviceEntity>) {
            _devices = devices
            notifyDataSetChanged()
        }
    }

    fun renderUiForDevice(deviceEntity: DeviceEntity?) {
        val sensorValueFragment = when (deviceEntity?.getBluetoothDeviceType()) {
            BluetoothDeviceType.NONE -> EmptySensorValueFragment()
            BluetoothDeviceType.AIRBEAM2 -> AirBeamSensorValueFragment(deviceEntity)
            BluetoothDeviceType.AIRBEAM3 -> AirBeamSensorValueFragment(deviceEntity)
            BluetoothDeviceType.RUUVI_TAG -> RuuviSensorValueFragment(deviceEntity)
            null -> {
                EmptySensorValueFragment()
            }
        }

        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerView, sensorValueFragment)
            .commit()
    }
}