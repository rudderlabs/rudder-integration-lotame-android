package com.example.myapplication

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.rudderstack.android.integrations.lotame.sdk.LotameIntegration
import com.rudderstack.android.integrations.lotame.LotameIntegrationFactory
import com.rudderstack.android.sdk.core.RudderClient
import com.rudderstack.android.sdk.core.RudderConfig
import com.rudderstack.android.sdk.core.RudderLogger


import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val rudderClient = RudderClient.getInstance(
            this, "1YyzMKUnUZJ6XNRkdHJCayV5fzM", RudderConfig.Builder()
                .withDataPlaneUrl("https://hosted.rudderlabs.com")
                .withLogLevel(RudderLogger.RudderLogLevel.DEBUG)
                .withFactory(LotameIntegrationFactory.FACTORY)
        )

        rudderClient.onIntegrationReady("Lotame") {
            (it as LotameIntegration).registerCallback {

            }
        }

//        class LC : LotameSyncCallback {
//            override fun onSync(instance: LotameIntegration) {
//                RudderLogger.logInfo("SYnxx")
//            }
//
//        }
//
//        class C : RudderClient.Callback {
//            override fun onReady(instance: Any?) {
//                (instance as LotameIntegration).registerCallback(LC());
//            }
//        }

        rudderClient.onIntegrationReady("Lotame", C())
        rudderClient.track("new event")
        rudderClient.identify("new user");
        rudderClient.screen("new screen");
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
