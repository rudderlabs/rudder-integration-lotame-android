package com.rudderstack.android.integrations.lotame;

import android.app.Application;

import androidx.annotation.NonNull;

import com.google.gson.internal.LinkedTreeMap;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderLogger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.PatternSyntaxException;

public class LotameIntegration {

    private ArrayList<LinkedTreeMap<String, String>> mappings = null;
    private LotameStorage storage;
    private static final long syncInterval = 7 * 1000 * 3600 * 24;// 7 days in milliseconds
    private static ExecutorService es;
    private LotameSyncCallback callback;

    LotameIntegration(Application application, ArrayList<LinkedTreeMap<String, String>> mappings) {
        this.storage = LotameStorage.getInstance(application);
        this.mappings = mappings;
        this.es = Executors.newSingleThreadExecutor();
    }

    public void registerCallback(LotameSyncCallback cb) {
        callback = cb;
    }

    private void executeCallback() {
        if(callback!= null) {
            callback.onSync(this);
        }
    }

    public void makeGetRequest(String _url) {
            try {
            // create and configure get request
            GetRequest req = new GetRequest(_url);
            // make the get request
                try {
                    es.submit(req).get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException ex) {
            RudderLogger.logError(String.format("RudderIntegration: Lotame: Malformed Url: %s", _url));
        } catch (IOException ex) {
            RudderLogger.logError(String.format("RudderIntegration: Lotame: Error while making request to %s", _url));
            RudderLogger.logError(String.format("RudderIntegration: Lotame: %s", ex.getLocalizedMessage()));
        }
    }

    public String compileUrl(String url, String userId) {
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

    public boolean areDspUrlsToBeSynced() {
        long lastSyncTime = storage.getLastSyncTime();
        long currentTime = getCurrentTime();
        if(lastSyncTime == -1) {
            return true;
        } else {
            return (currentTime - lastSyncTime) >= syncInterval;
        }
    }

    public void syncDspUrls(ArrayList<LinkedTreeMap<String, String>> dspUrls, @NonNull String userId) {
        processDspUrls(dspUrls, userId);
        // set last sync time
        storage.setLastSyncTime(new Date().getTime());
        // execute onSync callback
        executeCallback();
    }

    public void processBcpUrls(ArrayList<LinkedTreeMap<String, String>> bcpUrls,
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

    private void processUrls(String urlType, ArrayList<LinkedTreeMap<String, String>> urls, @NonNull String userId) {
        String url;
        String urlKey = String.format("%sUrlTemplate", urlType);
        if(urls!= null && !urls.isEmpty()) { // call to native SDK
            for (LinkedTreeMap<String, String> _url : urls) {
                url = _url.get(urlKey);
                if (url != null) {
                    url = compileUrl(url, userId);
                    makeGetRequest(url);
                }
            }
        } else {
            RudderLogger.logWarn(String.format("RudderIntegration: Lotame: no %sUrls found in config", urlType));
        }
    }

    public void processDspUrls(ArrayList<LinkedTreeMap<String, String>> dspUrls, @NonNull String userId) {
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
