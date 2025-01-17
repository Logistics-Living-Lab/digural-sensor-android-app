package de.digural.app.ui

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import de.digural.app.AppConstants
import de.digural.app.BuildConfig
import de.digural.app.R
import de.digural.app.auth.AuthManager
import de.digural.app.auth.TokenRefreshException
import de.digural.app.permission.PermissionsManager
import de.digural.app.ui.event.NavigationEvent
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val LOG_TAG: String = MainActivity::class.java.name

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var permissionsManager: PermissionsManager

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private lateinit var bottomNavView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val time = measureTimeMillis {
            buildLayout()
            checkPermissions()
            loadConfigFromKeycloak()
        }
        Log.v(LOG_TAG, "On create in ${time}ms.")
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        Log.i(LOG_TAG, "onStop")
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    private fun checkPermissions() {
        this.permissionsManager.requestRequiredPermissions(this)

    }

    private fun buildLayout() {
        setContentView(R.layout.activity_main)

        bottomNavView = findViewById<View>(R.id.bottomNavView) as BottomNavigationView
        val navController = findNavController(R.id.nav_host_fragment)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id == R.id.splashScreen) {
                bottomNavView.visibility = View.GONE
            } else {
                bottomNavView.visibility = View.VISIBLE
            }
        }

        if (!de.digural.app.BuildConfig.DEBUG) {
            bottomNavView.menu.removeItem(R.id.debugFragment)
        }

        // Setting Navigation Controller with the BottomNavigationView
        bottomNavView.setupWithNavController(
            navController
        )
    }

    private fun loadConfigFromKeycloak() {
        coroutineScope.launch {
            val progressJob = launch { logTokenProgress() }
            try {
                authManager.getValidAccessToken()
            } catch (e: TokenRefreshException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            } finally {
                progressJob.cancel()
            }
        }
    }

    private suspend fun logTokenProgress() {
        while (true) {
            Log.i(LOG_TAG, "${Thread.currentThread().name}: Waiting for Token ...")
            delay(1000)
        }
    }

    @Subscribe
    fun onNavigationEvent(event: NavigationEvent) {
        coroutineScope.launch {
            try {
                findNavController(R.id.nav_host_fragment).navigate(
                    event.action,
                    null,
                    event.navOptions
                )
            } catch (e: IllegalArgumentException) {
                Log.w(LOG_TAG, "Navigation: " + e.message)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSIONS) {
            if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder
                    .setMessage(getString(R.string.main_permission_dialog_message))
                    .setTitle(getString(R.string.main_permission_dialog_title))
                    .setPositiveButton(getString(R.string.main_permissions_dialog_ok_button)) { dialog, _ ->
                        dialog.dismiss()
                        this.checkPermissions()
                    }
                    .setNegativeButton(getString(R.string.main_permissions_dialog_title_close_button)) { dialog, _ ->
                        dialog.dismiss()
                        this.finish()
                        exitProcess(0)
                    }

                val dialog: AlertDialog = builder.create()
                dialog.show()

            }
        }
    }

}