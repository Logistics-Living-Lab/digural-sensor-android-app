package io.de4l.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import dagger.hilt.android.AndroidEntryPoint
import io.de4l.app.R
import kotlinx.coroutines.delay

@AndroidEntryPoint
abstract class SensorValueFragment : Fragment() {
    private val LOG_TAG = SensorValueFragment::class.java.name

    protected val viewModel: HomeViewModel by viewModels({ requireParentFragment() })

    lateinit var tvDeviceAddress: TextView
    lateinit var tvConnectionState: TextView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvDeviceAddress = view.findViewById(R.id.tvDeviceAddress)
        tvConnectionState = view.findViewById(R.id.tvConnectionState)

        viewModel.selectedDevice.asLiveData().observe(viewLifecycleOwner) {
            it?.let {
                tvDeviceAddress.text = it._macAddress.value
                tvConnectionState.text =
                    "Actual: ${it._actualConnectionState.value} --> Target: ${it._targetConnectionState.value}"
            }

        }
    }

    protected open fun clearUi() {
        tvDeviceAddress.text = "-"
        tvConnectionState.text = "-"
    }
}