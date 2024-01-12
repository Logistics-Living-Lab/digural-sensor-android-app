package de.digural.app.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionsManager {

    private val LOG_TAG: String = PermissionsManager::class.java.name

    fun getRequiredPermissions(): Array<String> {
        var requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions += Manifest.permission.BLUETOOTH_SCAN
            requiredPermissions += Manifest.permission.BLUETOOTH_CONNECT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions += Manifest.permission.POST_NOTIFICATIONS
        }

        return requiredPermissions
    }

    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = this.getRequiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        return missingPermissions
    }

    fun requestRequiredPermissions(activity: Activity) {
        val missingPermissions = this.getMissingPermissions(activity)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                de.digural.app.AppConstants.REQUEST_CODE_PERMISSIONS
            )
        }
    }

    fun hasAllPermissions(context: Context): Boolean {
        return this.getMissingPermissions(context).isEmpty()
    }

}