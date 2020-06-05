package com.rudderstack.android.integrations.lotame;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

// TODO: add a java doc (added only here. applicable for all other classes), code formatting : cmd+option+l
// TODO: add some more debug logs
// TODO: remove RuddeLogger
class GetRequest implements Runnable {
    public String url; // TODO : can we make it private? can we make it public so that anyone using it can get the URL from the object too

    GetRequest(String url) {
        this.url = url;
    }

    @Override
    public void run() {
        int responseCode;
        try {
            // create and configure the http connection
            URL _url = new URL(url);
            HttpURLConnection httpConnection = (HttpURLConnection) _url.openConnection();
            httpConnection.setRequestMethod("GET");

            // make the get request
            Logger.logDebug(String.format("Sending GET request to %s", url));
            httpConnection.connect();

            // get the response code
            responseCode = httpConnection.getResponseCode();
            if (responseCode < 400) {
                Logger.logDebug(String.format(Locale.US, "GET request to %s returned" +
                        "a %d response", url, responseCode));
            } else {
                Logger.logError(String.format(Locale.US, "GET request to %s returned" +
                        "a %d response", url, responseCode));
            }// TODO: check this
        } catch (MalformedURLException ex) {
            Logger.logError(String.format(Locale.US, "Malformed URL %s : %s",
                    url,ex.getLocalizedMessage()));
        } catch (IOException ex) {
            Logger.logError(String.format(Locale.US, "Error while making request to %s : %s",
                    url, ex.getLocalizedMessage()));
        }
    }
}
