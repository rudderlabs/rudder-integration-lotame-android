# What is RudderStack?

[RudderStack](https://rudderstack.com/) is a **customer data pipeline** tool for collecting, routing and processing data from your websites, apps, cloud tools, and data warehouse.

More information on RudderStack can be found [here](https://github.com/rudderlabs/rudder-server).

## Integrating Lotame with RudderStack's Android SDK

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
implementation 'com.rudderstack.android.integration:lotame:1.0.4'
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
        .withFactory(LotameIntegrationFactory.FACTORY)
        .build()
)
```

## Register your `onSync` callback
You can get notified about the Pixel syncs by registering a callback. The code snippet below shows the example:
```
rudderClient!!.onIntegrationReady("Lotame Mobile") {
    (it as LotameIntegration).registerCallback { urlType, url ->
        // urlType => "bcp", "dsp"
        // url => complete url with all values replaced
        println("LotameSync: $urlType : $url")
    }
}
```

## Send Events
Follow the steps from our [RudderStack Android SDK](https://github.com/rudderlabs/rudder-sdk-android).

## Contact Us

If you come across any issues while configuring or using this integration, please feel free to start a conversation on our [Slack](https://resources.rudderstack.com/join-rudderstack-slack) channel. We will be happy to help you.
