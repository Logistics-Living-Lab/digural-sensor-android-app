package de.digural.app.auth

import de.digural.app.BuildConfig

class UserInfo(val username: String, val roles: Map<String, List<String>>) {

    fun hasResourceRole(resource: String, role: String): Boolean {
        return roles[resource]?.contains(role) == true
    }

    fun isTrackOnlyUser(): Boolean {
        return hasResourceRole(de.digural.app.BuildConfig.APP_CLIENT_RESOURCE, de.digural.app.BuildConfig.TRACKING_ONLY_ROLE)
    }
}