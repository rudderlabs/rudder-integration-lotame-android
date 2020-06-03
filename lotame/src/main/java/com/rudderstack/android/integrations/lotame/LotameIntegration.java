package com.rudderstack.android.integrations.lotame;

import android.app.Application;

import androidx.annotation.NonNull;

import com.google.gson.internal.LinkedTreeMap;
import com.rudderstack.android.sdk.core.RudderLogger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.PatternSyntaxException;

class GetRequest implements Runnable {
    HttpURLConnection httpConnection;
    String url;

    GetRequest(String url) throws IOException{
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

class LotameIntegration {

    private ArrayList<LinkedTreeMap<String, String>> mappings = null;
    private LotameStorage storage;
    private static final long syncInterval = 7 * 1000 * 3600 * 24;// 7 days in milliseconds
    private static ExecutorService es;

    LotameIntegration(Application application, ArrayList<LinkedTreeMap<String, String>> mappings) {
        this.storage = LotameStorage.getInstance(application);
        this.mappings = mappings;
        this.es = Executors.newSingleThreadExecutor();
    }

    void makeGetRequest(String _url) {
        try {
            // create and configure get request
            GetRequest req = new GetRequest(_url);
            // make the get request
            es.submit(req);
        } catch (MalformedURLException ex) {
            RudderLogger.logError(String.format("RudderIntegration: Lotame: Malformed Url: %s", _url));
        } catch (IOException ex) {
            RudderLogger.logError(String.format("RudderIntegration: Lotame: Error while making request to %s", _url));
            RudderLogger.logError(String.format("RudderIntegration: Lotame: %s", ex.getLocalizedMessage()));
        }
    }

    String compileUrl(String url, String userId) {
        String replacePattern = "\\{\\{%s\\}\\}";
        String key = null, value = null;
        try {
            for (LinkedTreeMap<String, String> mapping : this.mappings) {
                key = mapping.get("key");
                value = mapping.get("value");
                url = url.replaceAll(String.format(replacePattern, key), value);
            }
            if (userId != null) {
                url = url.replaceAll(String.format(replacePattern, "userId"), userId);
            }
        } catch (PatternSyntaxException ex) {
            RudderLogger.logError(String.format("RudderIntegration: Lotame: Invalid field mapping value for %s : %s", key, value));
        }
        return url;
    }

    static long getCurrentTime() {
        return new Date().getTime();
    }

    boolean areDspUrlsToBeSynced() {
        long lastSyncTime = storage.getLastSyncTime();
        long currentTime = getCurrentTime();
        if(lastSyncTime == -1) {
            return true;
        } else {
            return (currentTime - lastSyncTime) >= syncInterval;
        }
    }

    void syncDspUrls(ArrayList<LinkedTreeMap<String, String>> dspUrls, @NonNull String userId) {
        processDspUrls(dspUrls, userId);
        // set last sync time
        storage.setLastSyncTime(new Date().getTime());
        // figure out callbacks - call callback if provided
//        trigger callback
    }

    void processBcpUrls(ArrayList<LinkedTreeMap<String, String>> bcpUrls,
                        ArrayList<LinkedTreeMap<String, String>> dspUrls,
                        String userId) {
        String url;
        if(bcpUrls!= null && !bcpUrls.isEmpty()) { // call to native SDK
            for (LinkedTreeMap<String, String> bcpUrl:bcpUrls) {
                url = bcpUrl.get("bcpUrlTemplate");
                if (url!= null) {
                    url = compileUrl(url, null);
                    makeGetRequest(url);
                }
            }
        } else {
            RudderLogger.logWarn("RudderIntegration: Lotame: no bcpUrls found in config");
        }

        // sync dsp urls if 7 days have passed since they were last synced
        if(userId!= null && areDspUrlsToBeSynced()) {
            syncDspUrls(dspUrls, userId);
        }
    }

    void processDspUrls(ArrayList<LinkedTreeMap<String, String>> dspUrls, @NonNull String userId) {
        String url;
        if(dspUrls!= null && !dspUrls.isEmpty()) { // call to native SDK
            for (LinkedTreeMap<String, String> dspUrl : dspUrls) {
                url = dspUrl.get("dspUrlTemplate");
                if (url != null) {
                    url = compileUrl(url, userId);
                     makeGetRequest(url);
                }
            }
        } else {
            RudderLogger.logWarn("RudderIntegration: Lotame: no dspUrls found in config");
        }
    }
}
