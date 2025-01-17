package de.digural.app.util

import android.util.Log

class LoggingHelper {

    companion object {
        fun logWithCurrentThread(tag: String, message: String) {
            Log.v(tag, Thread.currentThread().name + " | " + message)
        }
    }
}