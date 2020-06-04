# What is Rudder?

**Short answer:** 
Rudder is an open-source Segment alternative written in Go, built for the enterprise. .

**Long answer:** 
Rudder is a platform for collecting, storing and routing customer event data to dozens of tools. Rudder is open-source, can run in your cloud environment (AWS, GCP, Azure or even your data-centre) and provides a powerful transformation framework to process your event data on the fly.

Released under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Getting Started with Lotame Integration of Android SDK
1. Add [Lotame](https://www.lotame.com) as a destination in the [Dashboard](https://app.rudderlabs.com/)

2. Add these lines to your ```app/build.gradle```
```
repositories {
  maven { url "https://dl.bintray.com/rudderstack/rudderstack" }
}
```
3. Add the dependency under ```dependencies```
```
implementation 'com.rudderstack.android.sdk:core:1.0.1'
implementation 'com.rudderstack.android.integration:lotame:0.1.0'
```
4. If your lotame urls follow the HTTP protocol,add `android:usesCleartextTraffic="true"` to the `<application>` tag of your application's `Android Maifest` file.
After adding the above text, the file would look something like:
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
```
val rudderClient: RudderClient = RudderClient.getInstance(
    this,
    <WRITE_KEY>,
    RudderConfig.Builder()
        .withDataPlaneUrl(<DATA_PLANE_URL>)
        .withFactory(FirebaseIntegrationFactory.FACTORY)
        .build()
)
```

## Register your `onSync` callback
You can register a callback, which would get executed everytime `dspUrls` are synced.
To do so, follow these steps:
1. Create the `onSync` callback by implementing the `LotameSyncCallback` interface.
```
  class OnSyncCallback : LotameSyncCallback {
    override fun onSync(instance: LotameIntegration) {
        RudderLogger.logInfo("dspUrl synced!")
    }
  }
```
Replace `RudderLogger.logInfo("dspUrl synced!")` with your own custom code.

2. Use the `onIntegrationReady` method to register another callback that would be executed after the nativeSDK has been successfully initialized.The method takes two arguments - the Integration's name(`Lotame` in this case) and the callback.
You can use this callback to register the `onSync` callback you created in Step 1.

```
  class IntegrationReadyCallback : RudderClient.Callback{
    override fun onReady(instance: Any?) {
        (instance as LotameIntegration).registerCallback(OnSyncCallback());
    }
  }

  rudderClient.onIntegrationReady("Lotame", IntegrationReadyCallback())
```
Note: Make sure you register the callback before sending any events.

## Send Events
Follow the steps from [Rudder Android SDK](https://github.com/rudderlabs/rudder-sdk-android)

## Contact Us
If you come across any issues while configuring or using RudderStack, please feel free to [contact us](https://rudderstack.com/contact/) or start a conversation on our [Discord](https://discordapp.com/invite/xNEdEGw) channel. We will be happy to help you.
