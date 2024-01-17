package de.digural.app.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import de.digural.app.AppConstants
import de.digural.app.R
import de.digural.app.permission.PermissionsManager
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.math.log

@AndroidEntryPoint
class SplashScreen : Fragment() {

    private val LOG_TAG: String = SplashScreen::class.java.name

    private val viewModel: SplashScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash_screen, container, false)
    }

    override fun onResume() {
        super.onResume()
        Log.w(LOG_TAG, "On Resume")
        viewModel.startPermissionCheck(requireContext())
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPermissionCheck()
    }

//    override fun onStop() {
//        super.onStop()
//    }


}