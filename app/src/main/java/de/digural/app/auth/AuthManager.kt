package de.digural.app.auth

import android.app.Activity
import android.app.Application
import android.util.Log
import com.auth0.android.jwt.JWT
import com.okta.oidc.*
import com.okta.oidc.clients.web.WebAuthClient
import com.okta.oidc.storage.SharedPreferenceStorage
import com.okta.oidc.util.AuthorizationException
import de.digural.app.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AuthManager(val application: Application) {
    private val LOG_TAG: String = AuthManager::class.java.name

    var webClient: WebAuthClient? = null
    var user = MutableStateFlow<UserInfo?>(null)

    init {
        Log.i(LOG_TAG, "Init")

        val oidcConfig = OIDCConfig.Builder()
            .clientId(de.digural.app.AppConstants.AUTH_CLIENT_ID)
            .redirectUri(de.digural.app.AppConstants.AUTH_REDIRECT_URI)
            .endSessionRedirectUri(de.digural.app.AppConstants.AUTH_REDIRECT_URI_END_SESSION)
            .scopes(*de.digural.app.AppConstants.AUTH_SCOPES)
            .discoveryUri(de.digural.app.AppConstants.AUTH_DISCOVERY_URI)
            .create()

        webClient = Okta.WebAuthBuilder()
            .withConfig(oidcConfig)
            .withContext(application)
            .withStorage(SharedPreferenceStorage(application))
            .create()
    }

    suspend fun refreshToken() {
        if (webClient?.sessionClient?.tokens == null) {
            clearTokens()
            throw TokenRefreshException("User login needed!")
        } else {
            val tokens = webClient!!.sessionClient!!.tokens
            val refreshToken = JWT(tokens.refreshToken.toString())

            if (refreshToken.isExpired(0)) {
                clearTokens()
                throw TokenRefreshException("Refresh token is expired - User login needed!")
            } else {
                try {
                    callRefreshTokenApiSync()
                    onAuthAuthorized()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, e.message, e)
                    throw TokenRefreshException(e.message)
                }
            }
        }
    }

    suspend fun login(activity: Activity): AuthorizationStatus? {
        try {
            webClient?.unregisterCallback()
        } catch (e: Exception) {
            Log.v(LOG_TAG, "${e.message}")
        }
        return suspendCoroutine { cont ->
            webClient?.let { it ->
                it.registerCallback(object :
                    ResultCallback<AuthorizationStatus, AuthorizationException> {

                    override fun onSuccess(status: AuthorizationStatus) {
                        when (status) {
                            AuthorizationStatus.AUTHORIZED -> onAuthAuthorized()
                            AuthorizationStatus.SIGNED_OUT -> onAuthSignedOut()
                            AuthorizationStatus.CANCELED -> onAuthCanceled()
                            AuthorizationStatus.ERROR -> onAuthError()
                            AuthorizationStatus.EMAIL_VERIFICATION_AUTHENTICATED -> onAuthEmailVerificationAuthenticated()
                            AuthorizationStatus.EMAIL_VERIFICATION_UNAUTHENTICATED -> onAuthEmailVerificationUnAuthenticated()
                        }
                        safeContinuationResume(cont, status)
                    }

                    override fun onCancel() {
                        Log.e(LOG_TAG, "Cancelled by user")
                        safeContinuationResume(cont, AuthorizationStatus.CANCELED)
                    }

                    override fun onError(msg: String?, exception: AuthorizationException?) {
                        Log.e(LOG_TAG, msg, exception)
                        safeContinuationResume(cont, AuthorizationStatus.ERROR)
                    }


                }, activity)
            }
            webClient?.signIn(activity, null)
        }
    }

    private fun onAuthEmailVerificationUnAuthenticated() {
        Log.v(LOG_TAG, "onAuthEmailVerificationUnAuthenticated")
    }

    private fun onAuthEmailVerificationAuthenticated() {
        Log.v(LOG_TAG, "onAuthEmailVerificationAuthenticated")
    }

    private fun onAuthError() {
        Log.v(LOG_TAG, "onAuthError")
    }

    private fun onAuthCanceled() {
        Log.v(LOG_TAG, "onAuthCanceled")
    }

    private fun onAuthSignedOut() {
        Log.v(LOG_TAG, "onAuthSignedOut")
    }

    private fun onAuthAuthorized() {
        //client is authorized.
        val tokens: Tokens = webClient!!.sessionClient.tokens

        if (tokens.idToken != null) {
            val jwt = JWT(tokens.idToken!!)

            val accessToken =
                tokens.accessToken ?: throw TokenPermissionException("Access Token is missing")

            val usernameFromToken =
                jwt.getClaim(de.digural.app.AppConstants.AUTH_USERNAME_CLAIM_KEY).asString()
            val roles = getResourceRolesFromToken(JWT(accessToken))
            if (usernameFromToken != null && tokens.accessToken != null && tokens.refreshToken != null) {
                val userInfo = UserInfo(usernameFromToken, roles)

                //CHECK MQTT ACCESS
                if (!userInfo.hasResourceRole(
                        de.digural.app.AppConstants.AUTH_MQTT_CLAIM_RESOURCE,
                        de.digural.app.AppConstants.AUTH_MQTT_CLAIM_ROLE
                    )
                ) {
                    throw TokenPermissionException("User has no access for MQTT.")
                }

                user.value = userInfo
                Log.i(LOG_TAG, "User $usernameFromToken successfully authenticated.")
            }
        }
    }

    fun dispose() {
//        authService.dispose()
    }

    private suspend fun callRefreshTokenApiSync(): Tokens {
        return suspendCoroutine { cont ->
            webClient?.sessionClient?.refreshToken(TokenRequestCallback(cont))
        }
    }

    suspend fun getValidAccessToken(timeout: Long = 60000): String? =
        withContext(Dispatchers.Default) {
            try {
                withTimeout(timeout) {
                    refreshToken()
                    Log.i(LOG_TAG, "Token refreshed!")
                }
                Log.i(LOG_TAG, "Returning token")
                return@withContext getAccessToken()
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.message ?: e.javaClass.name)
            }
            return@withContext null
        }

    fun getAccessToken(): String? {
        return webClient?.sessionClient?.tokens?.accessToken
    }

    fun isAccessTokenExpired(): Boolean {
        return webClient?.sessionClient?.tokens?.isAccessTokenExpired == true
    }

    private class TokenRequestCallback(private val cont: Continuation<Tokens>) :
        RequestCallback<Tokens, AuthorizationException> {

        private val LOG_TAG: String = AuthManager::class.java.name

        override fun onSuccess(result: Tokens) {
            Log.i(LOG_TAG, "${Thread.currentThread().name}: Success")
            cont.resume(result)
        }

        override fun onError(error: String?, exception: AuthorizationException?) {
            Log.i(LOG_TAG, "${Thread.currentThread().name}: Error - ${exception?.message}")
            cont.resumeWithException(exception!!)
        }

    }

    fun logout(activity: Activity) {
        webClient?.signOut(activity, object : RequestCallback<Int, AuthorizationException> {
            override fun onSuccess(result: Int) {
                clearTokens()
            }

            override fun onError(error: String?, exception: AuthorizationException?) {
                Log.i(LOG_TAG, "onError")
            }
        })
    }

    private fun clearTokens() {
        webClient?.sessionClient?.cancel()
        webClient?.sessionClient?.clear()
        user.value = null
    }

    private fun getResourceRolesFromToken(token: JWT): Map<String, List<String>> {
        val roles: MutableMap<String, List<String>> = HashMap()

        val resourcesAccessMap =
            token.getClaim("resource_access").asObject(HashMap::class.java) ?: emptyMap()

        resourcesAccessMap.entries.forEach {
            roles.put(
                it.key as String,
                (it.value as Map<String, List<String>>)["roles"] ?: emptyList()
            )
        }

        return roles
    }

    private fun hasTokenMqttAccess(token: JWT): Boolean {
        val resourcesAccessMap = token.getClaim("resource_access").asObject(HashMap::class.java)
        val resourceRolesObject =
            resourcesAccessMap?.get(de.digural.app.AppConstants.AUTH_MQTT_CLAIM_RESOURCE)

        if (resourceRolesObject != null) {
            val resourceRolesMap = resourceRolesObject as Map<String, List<String>>
            val roles = resourceRolesMap["roles"]
            return roles?.contains(de.digural.app.AppConstants.AUTH_MQTT_CLAIM_ROLE) == true
        }
        return false
    }

    private fun safeContinuationResume(
        cont: Continuation<AuthorizationStatus>,
        status: AuthorizationStatus
    ) {
        try {
            cont.resume(status)
        } catch (e: Exception) {
            try {
                cont.resumeWithException(e)
            } catch (resumeException: IllegalStateException) {
                //Might happen when continuation has been cancelled
                Log.v(
                    LOG_TAG,
                    "Continuation Resume Exception - Could be ignored: " + resumeException.message
                )
            }
        }
    }


}