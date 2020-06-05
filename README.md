# What is Rudder?

**Short answer:** 
Rudder is an open-source Segment alternative written in Go, built for the enterprise. .

**Long answer:** 
Rudder is a platform for collecting, storing and routing customer event data to dozens of tools. Rudder is open-source, can run in your cloud environment (AWS, GCP, Azure or even your data-centre) and provides a powerful transformation framework to process your event data on the fly.

Released under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Getting Started with Lotame Integration of Android SDK
1. Add [Lotame](https://www.lotame.com) as a destination in the [Dashboard](https://app.rudderstack.com/)

2. Add these lines to your `app/build.gradle`
```
repositories {
  maven { url "https://dl.bintray.com/rudderstack/rudderstack" }
}
```
3. Add the dependency under ```dependencies```
```
implementation 'com.rudderstack.android.sdk:core:1.+'
implementation 'com.rudderstack.android.integration:lotame:0.1.0'
```
4. If your lotame urls follow the HTTP protocol, you need to allow the ClearTextTraffic for your App. Add `android:usesCleartextTraffic="true"` in your `<application>` tag of your app's `Android Maifest` file.
After adding the above text, the file would look something like below:
```
<application
  ...
  android:usesCleartextTraffic="true"
  ...>
    <activity>
      ...
    </activity>
</application>
```

## Initialize ```RudderClient```
Add the following code under the `onCreate` method of your `Application` class.
```
val rudderClient: RudderClient = RudderClient.getInstance(
    this,
    WRITE_KEY,
    RudderConfig.Builder()
        .withDataPlaneUrl(DATA_PLANE_URL)
        .withFactory(LotameIntegrationFctory.FACTORY)
        .build()
)
```

## Register your `onSync` callback
DSP pixels are always synced after an `identify` call and we sync the pixels once in every 7 days. We check for the time elapsed since last sync in every `screen` call.
You can get notified about the DSP Pixels syncs by registering a callback. The code snippet below shows the examle:

```
rudderClient.onIntegrationReady("Lotame") {
    (it as LotameIntegration).registerCallback {
        // implement your logic
    }
}
```

## Send Events
Follow the steps from [Rudder Android SDK](https://github.com/rudderlabs/rudder-sdk-android)

## Contact Us
If you come across any issues while configuring or using RudderStack, please feel free to [contact us](https://rudderstack.com/contact/) or start a conversation on our [Discord](https://discordapp.com/invite/xNEdEGw) channel. We will be happy to help you.
