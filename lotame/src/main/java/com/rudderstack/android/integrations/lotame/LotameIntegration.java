package com.rudderstack.android.integrations.lotame;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.internal.LinkedTreeMap;
import com.rudderstack.android.sdk.core.RudderLogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.PatternSyntaxException;

public class LotameIntegration {
    private static final long SYNC_INTERVAL = 1000 * 60 * 60 * 24 * 7; // 7 days in milliseconds

    private ArrayList<LinkedTreeMap<String, String>> mappings;
    private LotameStorage storage;
    private ExecutorService es;
    private LotameSyncCallback callback;

    // singleton
    public LotameIntegration getInstance(
            @NonNull Context context,
            @Nullable List<String> bcpUrls,
            @Nullable List<String> dspUrls,
            @Nullable Map<String, String> keys
    ) {

    }

    LotameIntegration(Application application, ArrayList<LinkedTreeMap<String, String>> mappings) {
        this.storage = LotameStorage.getInstance(application);
        this.mappings = mappings;
        this.es = Executors.newSingleThreadExecutor();
    }

    public void registerCallback(LotameSyncCallback cb) {
        callback = cb;
    }

    private void executeCallback() {
        if (callback != null) {
            callback.onSync();
        }
    }

    public void makeGetRequest(String _url) {
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

    // TODO: Add random support. {{random}} should be replaced with a random number
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

    // CHECK: do we need the static method here?
    static long getCurrentTime() {
        return new Date().getTime();
    }

    public boolean areDspUrlsToBeSynced() {
        long lastSyncTime = storage.getLastSyncTime();
        long currentTime = getCurrentTime();
        if (lastSyncTime == -1) {
            return true;
        } else {
            return (currentTime - lastSyncTime) >= syncInterval;
        }

        // CHECK: can we simplify the return
    }

    public void syncDspUrls(@NonNull String userId, @Nullable ArrayList<LinkedTreeMap<String, String>> dspUrls) {
        processDspUrls(dspUrls, userId);
        // set last sync time
        storage.setLastSyncTime(new Date().getTime());
        // execute onSync callback
        executeCallback();
    }

    private void processUrls(String urlType, ArrayList<LinkedTreeMap<String, String>> urls, @NonNull String userId) {
        String url;
        String urlKey = String.format("%sUrlTemplate", urlType);
        if (urls != null && !urls.isEmpty()) { // call to native SDK
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

    public void processBcpUrls(ArrayList<LinkedTreeMap<String, String>> bcpUrls,
                               ArrayList<LinkedTreeMap<String, String>> dspUrls,
                               String userId) {
        processUrls("bcp", bcpUrls, null);
        // sync dsp urls if 7 days have passed since they were last synced
        if (userId != null && areDspUrlsToBeSynced()) {
            syncDspUrls(dspUrls, userId);
        }
    }

    public void processDspUrls(ArrayList<LinkedTreeMap<String, String>> dspUrls, @NonNull String userId) {
        processUrls("dsp", dspUrls, userId);
    }
}
