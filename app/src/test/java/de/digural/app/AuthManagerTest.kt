package de.digural.app

import de.digural.app.auth.AuthManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class AuthManagerTest {

    private lateinit var authManager: AuthManager

    init {
        val application: De4lApplication = Mockito.mock(De4lApplication::class.java)
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