package com.rudderstack.android.integrations.lotame;

import com.rudderstack.android.sdk.core.RudderLogger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class GetRequest implements Runnable {
    HttpURLConnection httpConnection;
    String url;

    GetRequest(String url) throws IOException {
        this.url = url;
        URL _url = new URL(url);
        httpConnection = (HttpURLConnection) _url.openConnection();
        httpConnection.setRequestMethod("GET");
    }

    @Override
    public void run() {
        int responseCode;
        if(httpConnection!= null) {
            try {
                // make the get request
                httpConnection.connect();
                // get the response code
                responseCode = httpConnection.getResponseCode();
                RudderLogger.logInfo(String.format("==== %s : %d", url, responseCode));
                if(responseCode>= 400) {
                    RudderLogger.logError(String.format("RudderIntegration: Lotame: Error while " +
                                    "processing url : the url %s returned a %i response",
                            url, responseCode));
                }
            } catch (IOException ex) {
                RudderLogger.logError(String.format("RudderIntegration: Lotame: Error while making request to %s", url));
                RudderLogger.logError(String.format("RudderIntegration: Lotame: %s", ex.getLocalizedMessage()));
            }
        } else {
            // possibly dead code..verify
            RudderLogger.logDebug("RudderIntegration: Lotame: couldn't connect to %s");
        }
    }
}
