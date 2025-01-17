# diGuRaL Sensing App 
![Project Logo][project-logo]

![Made with love in Germany](https://madewithlove.now.sh/de?heart=true&colorA=%23000000&colorB=%23299fc7&template=for-the-badge)

## Motivation

Android App for connecting Bluetooth environment sensors to an Android smartphone and transmitting values in real-time via MQTT and OAuth2 authentication. See the [App - Quickstart Guide](documentation/quickstart-digural-sense-app.pdf)

Currently the app is used in the research project [diGuRaL](https://bmdv.bund.de/SharedDocs/DE/Artikel/DG/mfund-projekte/digural.html) (Digitale Gestaltung des urbanen Raums in Leipzig) funded by the German Federal Ministry for Digital and Transport (19FS2038F).



![App Screenshots][app-teaser]

<a href='https://play.google.com/store/apps/details?id=de.lll.digural.senseapp&utm_source=github&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width='300'/></a>

Developed by [Logistics Living Lab / Leipzig University](https://logistics-living-lab.de/) in Leipzig, Germany.

## Features
- Real-time data connection using MQTT
- OAuth2 Token authentication for MQTT and automatic token refresh
- MQTT message buffering using Room when connection to MQTT server is lost
- Multiple simultaneous sensor connections (Classic BT, BLE, BLE Beacon)   
- In case Bluetooth connection to sensor is lost, reconnect procedure is initiated
- Support of multiple sensors
  - [HabitatMap's AirBeam2 + AirBeam3](https://www.habitatmap.org/airbeam)
  - [Ruuvi Tag Sensor Beacon](https://ruuvi.com/ruuvitag/)
    

## Technology Overview
- [Paho MQTT Client](https://github.com/eclipse/paho.mqtt.android)
- [Okta OIDC Android](https://github.com/okta/okta-oidc-android) for OpenID Connect Authentication
- [Hilt](https://dagger.dev/hilt/) for Dependency Injection
- [Android Jetpack](https://developer.android.com/jetpack) (e.g. Room and Livedata)
- [Google Guava](https://github.com/google/guava)
- [GreenRobot EventBus](https://greenrobot.org/eventbus/)
- Kotlin [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html) and [Flows](https://kotlinlang.org/docs/flow.html)


## Project setup

Include the following properties in your `local.properties` file and set the properties accordingly:

```
# Authentication
AUTH_DISCOVERY_URI="{DISCOVERY_URI}"                            #EXAMPLE https://auth.example.org/auth/realms/my-awesome-realm
AUTH_CLIENT_ID="{CLIENT_ID}"
AUTH_REDIRECT_URI="com.example.app:/start-app"                  #EXAMPLE VALUE
AUTH_REDIRECT_URI_END_SESSION="com.example.app:/session-ended"  #EXAMPLE VALUE
AUTH_USERNAME_CLAIM_KEY="preferred_username"                    #USE TO GET USERNAME FROM KEYCLOAK TOKEN
AUTH_MQTT_CLAIM_RESOURCE="{RESOURCE_ID}"                        #USE TO ACCESS KEYCLOAK RESOURCE ROLES
AUTH_MQTT_CLAIM_ROLE="{RESOURCE_ROLE}"                          #REQUIRED RESOURCE ROLE

# MQTT
MQTT_SERVER_URL="{MQTT-BROKER-URL}"                             #EXAMPLE: ssl://broker.example.com:8883
TRACKING_ONLY_ROLE="{CLIENT-ROLE}"                              #Enables location tracking only mode / no sensors needed                          
APP_CLIENT_RESOURCE="{CLIENT-RESOURCE}"                         #App Oauth Resource Client
```

## Known Issues (might be fixed in future releases)
- [AIRBEAM2] AirBeam2 sensor occasionally produces unparseable lines
- [PAHO] If MQTT client is connected to a mobile network and device is connected to WiFi, MQTT connection stays in mobile network


[project-logo]: documentation/logos/project-logo_1080p.png "diGuRaL Project Logo"
[app-teaser]: documentation/files/app-teaser_full_1080p.png "App Teaser"

## License 
```text
MIT License

Copyright (c) 2024 Universität Leipzig

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Legal

- Google Play and the Google Play logo are trademarks of Google LLC.
- All trademarks are property of their respective owners.