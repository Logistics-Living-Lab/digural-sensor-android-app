package de.digural.app


class AppConstants {
    companion object {

        const val REQUEST_CODE_PERMISSIONS: Int = 1000

        // AUTH
        const val AUTH_CLIENT_ID = de.digural.app.BuildConfig.AUTH_CLIENT_ID
        const val AUTH_REDIRECT_URI = de.digural.app.BuildConfig.AUTH_REDIRECT_URI
        const val AUTH_REDIRECT_URI_END_SESSION =
            de.digural.app.BuildConfig.AUTH_REDIRECT_URI_END_SESSION
        val AUTH_SCOPES = arrayOf("openid", "profile")
        const val AUTH_DISCOVERY_URI = de.digural.app.BuildConfig.AUTH_DISCOVERY_URI
        const val AUTH_USERNAME_CLAIM_KEY = de.digural.app.BuildConfig.AUTH_USERNAME_CLAIM_KEY

        //        const val AUTH_MQTT_CLAIM_RESOURCE = de.digural.app.BuildConfig.AUTH_MQTT_CLAIM_RESOURCE
        const val AUTH_MQTT_CLAIM_ROLE = de.digural.app.BuildConfig.AUTH_MQTT_CLAIM_ROLE

        // BLUETOOTH
        const val BT_AIRBEAM2_SOCKET_RF_COMM_UUID = "00001101-0000-1000-8000-00805F9B34FB"


        // ROOM DB
        const val ROOM_DB_NAME = "app-database"

        // LOCATION
        const val LOCATION_INTERVAL_IN_SECONDS = 5
        val LOCATION_MIN_DISTANCE: Float? = 0.0f

        //MQTT
        const val MQTT_SERVER_URL = de.digural.app.BuildConfig.MQTT_SERVER_URL

        val MQTT_TOPIC_PATTERN_SENSOR_VALUES =
            if (de.digural.app.BuildConfig.DEBUG) "sensors/%s/digural-app-v1-debug" else "sensors/%s/digural-app-v1"

        val MQTT_TOPIC_PATTERN_LOCATION_VALUES =
            if (de.digural.app.BuildConfig.DEBUG) "locations/%s/digural-app-debug" else "locations/%s/digural-app"

        val HEARTBEAT_TOPIC_PATTERN_LOCATION_VALUES =
            if (de.digural.app.BuildConfig.DEBUG) "heartbeats/%s/digural-app-debug" else "heartbeats/%s/digural-app"

        //UI
        const val SPLASH_SCREEN_DELAY_IN_SECONDS = 1L

        //SERVICE NOTIFICATION
        const val TRACKING_SERVICE_NOTIFICATION_ID = 1001
        const val TRACKING_NOTIFICATION_STOP_ACTION = "notification-stop-action"
        const val FORCE_RECONNECT_ACTION = "force-reconnect-action"

        const val TRACKING_NOTIFICATION_CODE = 2000
        const val TRACKING_NOTIFICATION_CHANNEL_ID = "digural-tracking"
        const val TRACKING_NOTIFICATION_CHANNEL_NAME = "diGuRaL Tracking"

        //WEB VIEW
        const val DIGURAL_INFO_URL =
            "https://bmdv.bund.de/SharedDocs/DE/Artikel/DG/mfund-projekte/digural.html"

        //HEARTBEAT
        const val HEARTBEAT_INTERVAL_SECONDS = 60L

        //UPDATE CHECK
        //check only once every XX hours
        const val UPDATE_CHECK_MINIMUM_INTERVAL_HOURS = 12
        const val UPDATE_FLOW_REQUEST_CODE = 3000

    }
}