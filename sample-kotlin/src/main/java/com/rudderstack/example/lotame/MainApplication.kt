package com.rudderstack.example.lotame

import android.app.Application
import com.rudderstack.android.integrations.lotame.LotameIntegrationFactory
import com.rudderstack.android.integrations.lotame.sdk.LotameIntegration
import com.rudderstack.android.sdk.core.RudderClient
import com.rudderstack.android.sdk.core.RudderConfig
import com.rudderstack.android.sdk.core.RudderLogger

class MainApplication : Application() {
    companion object {
        const val DATA_PLANE_URL = "https://hosted.rudderlabs.com"
        const val WRITE_KEY = "1d4rBcsSZXNFETnoVEIvaXY98vm"

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

        rudderClient!!.onIntegrationReady("Lotame") {
            (it as LotameIntegration).registerCallback {
                // your custom code
                println("Lotame sync callback fired")
                rudderClient!!.track("sync pixels fired")
            }
        }
    }

}