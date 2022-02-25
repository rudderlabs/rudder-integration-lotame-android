package com.rudderstack.example.lotame

import android.app.Application
import com.rudderstack.android.integrations.lotame.LotameIntegrationFactory
import com.rudderstack.android.integrations.lotame.sdk.LotameIntegration
import com.rudderstack.android.sdk.core.RudderClient
import com.rudderstack.android.sdk.core.RudderConfig
import com.rudderstack.android.sdk.core.RudderLogger

class MainApplication : Application() {
    companion object {
        const val DATA_PLANE_URL = "https://98fa-175-101-36-4.ngrok.io"
        const val WRITE_KEY = "1pTxG1Tqxr7FCrqIy7j0p28AENV"

        var rudderClient: RudderClient? = null
    }

    override fun onCreate() {
        super.onCreate()

        rudderClient = RudderClient.getInstance(
            this,
            WRITE_KEY,
            RudderConfig.Builder().withDataPlaneUrl(DATA_PLANE_URL)
                .withLogLevel(RudderLogger.RudderLogLevel.DEBUG)
                .withTrackLifecycleEvents(true)
                .withRecordScreenViews(true)
                .withFactory(LotameIntegrationFactory.FACTORY)
                .build()
        )

        rudderClient!!.onIntegrationReady("Lotame Mobile") {
            (it as LotameIntegration).registerCallback { urlType, url ->
                // urlType => "bcp", "dsp"
                // url => complete url with all values replaced
                println("LotameSync: $urlType : $url")
            }
        }
    }

}