package de.digural.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import de.digural.app.R
import de.digural.app.ui.event.NavigationEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@HiltViewModel
class SplashScreenViewModel @Inject constructor() :
    ViewModel() {

    fun onSplashScreenFinished() {
        viewModelScope.launch {
            //Remove Splash Screen from stack
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.splashScreen, true).build()
            EventBus.getDefault()
                .post(NavigationEvent(R.id.action_splashScreenFinished, navOptions))
        }
    }
}