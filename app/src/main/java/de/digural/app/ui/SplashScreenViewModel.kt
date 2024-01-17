package de.digural.app.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digural.app.AppConstants
import de.digural.app.R
import de.digural.app.permission.PermissionsManager
import de.digural.app.ui.event.NavigationEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor(
    private val permissionsManager: PermissionsManager
) :
    ViewModel() {

    private val LOG_TAG = SplashScreenViewModel::class.java.name

    private var permissionCheckJob: Job? = null

    fun startPermissionCheck(context: Context) {
        this.stopPermissionCheck()
        this.permissionCheckJob = viewModelScope.launch {
            Log.w(LOG_TAG, "Run coroutine")
            while (true) {
                delay(AppConstants.SPLASH_SCREEN_DELAY_IN_SECONDS * 1000)
                if (hasAllPermissions(context)) {
                    onSplashScreenFinished()
                } else {
                    Log.w(LOG_TAG, "Missing permissions")
                }
            }
        }
    }

    fun onSplashScreenFinished() {
        viewModelScope.launch {
            //Remove Splash Screen from stack
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.splashScreen, true).build()
            EventBus.getDefault()
                .post(NavigationEvent(R.id.action_splashScreenFinished, navOptions))
        }
    }

    private fun hasAllPermissions(context: Context): Boolean {
        return this.permissionsManager.hasAllPermissions(context)
    }

    fun stopPermissionCheck() {
        permissionCheckJob?.apply {
            this.cancel()
        }
        permissionCheckJob = null
    }
}