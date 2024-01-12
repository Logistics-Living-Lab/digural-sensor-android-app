package de.digural.app

import de.digural.app.auth.AuthManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AuthManagerTest {

    private lateinit var authManager: AuthManager

    init {
        val application: DiguralApplication = Mockito.mock(DiguralApplication::class.java)
        authManager = AuthManager(application)
    }


    @Test
    fun test() {
        runBlocking {
            assertNotNull(authManager)
            authManager.refreshToken()
        }
    }

}