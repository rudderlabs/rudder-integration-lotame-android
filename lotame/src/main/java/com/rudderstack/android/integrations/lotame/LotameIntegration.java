package com.rudderstack.android.integrations.lotame;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.PatternSyntaxException;

/**
 * RudderStack's native SDK implementation for Lotame
 */
public class LotameIntegration {
    private static final long SYNC_INTERVAL = 1000 * 60 * 60 * 24 * 7; // 7 days in milliseconds

    private ArrayList<LinkedTreeMap<String, String>> mappings;
    private LotameStorage storage;
    private ExecutorService es;
    private LotameSyncCallback callback;

//    // singleton
//    public LotameIntegration getInstance(
//            @NonNull Context context,
//            @Nullable List<String> bcpUrls,
//            @Nullable List<String> dspUrls,
//            @Nullable Map<String, String> keys
//    ) {
//        // get application from provided context
//        Application application = (Application) context.getApplicationContext();
//    }

    public LotameIntegration(Application application, ArrayList<LinkedTreeMap<String, String>> mappings) {
        this.storage = LotameStorage.getInstance(application);
        this.mappings = mappings;
        this.es = Executors.newSingleThreadExecutor();
    }

    /**
     * Registers the onSync callback.
     * @param cb the onSync callback, must implement the interface {@code LotameSyncCallback}
     */
    public void registerCallback(LotameSyncCallback cb) {
        callback = cb;
        Logger.logDebug("onSync callback successfully registered");
    }

    private void executeCallback() {
        if (callback != null) {
            callback.onSync();
        } else {
            Logger.logDebug("No onSync callback registered");
        }
    }

    /**
     * Creates a new {@code GetRequest} and sends it.
     * The {@code GetRequest} is sent over the network from a background thread.
     * @param url The url that would be used to create the {@code GetRequest}
     */
    public void makeGetRequest(String url) {
        // create and configure the GET request
        Logger.logDebug(String.format(Locale.US, "Creating a GET request with url %s", url));
        GetRequest req = new GetRequest(url);

        // make the get request
        es.submit (req);
    }

    // TODO: Add random support. {{random}} should be replaced with a random number
    private String compileUrl(String url, String userId, String randomValue) {
        String replacePattern = "\\{\\{%s\\}\\}";
        String key = null, value = null;
        try {
            for (LinkedTreeMap<String, String> mapping : mappings) {
                key = mapping.get("key");
                value = mapping.get("value");
                url = url.replaceAll(String.format(replacePattern, key), value);
            }
            url = url.replaceAll(String.format(replacePattern, "random"), randomValue);
            if (userId != null) {
                url = url.replaceAll(String.format(replacePattern, "userId"), userId);
            }
        } catch (PatternSyntaxException ex) {
            Logger.logError(String.format("Error while compiling url %s." +
                    "Failed to replace {{%s}} with %s : %s"
                    ,url, key, value, ex.getLocalizedMessage()));
        }
        return url;
    }

    // CHECK: do we need the static method here?
    private long getCurrentTime() {
        return new Date().getTime();
    }

    private boolean areDspUrlsToBeSynced() {
        long lastSyncTime = storage.getLastSyncTime();
        long currentTime = getCurrentTime();
        if (lastSyncTime == -1) {
            return true;
        } else {
            return (currentTime - lastSyncTime) >= SYNC_INTERVAL;
        }
        // CHECK: can we simplify the return ..ask?
    }

    /**
     * Syncs the urls in {@code dspUrls} by sending a GET request for each one of them.
     * Sets the last sync time and executes the onSync callback.
     * @param userId ?
     * @param dspUrls the list of the urls to be synced
     */
    public void syncDspUrls(
            @NonNull String userId,
            @Nullable ArrayList<LinkedTreeMap<String, String>> dspUrls
    ) {
        Logger.logDebug(String.format(Locale.US, "Syncing DSP Urls : %s", dspUrls));
        processDspUrls(userId, dspUrls);

        // set last sync time
        Logger.logDebug("Updating last sync time with current time");
        storage.setLastSyncTime(new Date().getTime());

        // execute onSync callback
        Logger.logDebug("Executing onSync callback");
        executeCallback();
    }

    private void processUrls(
            String urlType,
            ArrayList<LinkedTreeMap<String, String>> urls,
            String userId
    ) {
        String url;
        String currentTime = String.valueOf(getCurrentTime());
        String urlKey = String.format("%sUrlTemplate", urlType);
        if (urls != null && !urls.isEmpty()) {
            for (LinkedTreeMap<String, String> _url : urls) {
                url = _url.get(urlKey);
                if (url != null) {
                    url = compileUrl(url, userId, currentTime);
                    makeGetRequest(url);
                }
            }
        } else {
            Logger.logWarn(String.format("no %sUrls found in config", urlType));
        }
    }

    /**
     * Sends a GET request for each url in {@code bcpUrls}.
     * Syncs the DSP urls if 7 days have passed since the last sync.
     * @param bcpUrls the list of bcpUrls
     * @param dspUrls the list of dspUrls
     * @param userId ?
     */
    public void processBcpUrls(
            @Nullable ArrayList<LinkedTreeMap<String, String>> bcpUrls,
            @Nullable ArrayList<LinkedTreeMap<String, String>> dspUrls,
            @Nullable String userId
    ) {
        Logger.logDebug(String.format(Locale.US, "Processing BCP Urls : %s", bcpUrls));
        processUrls("bcp", bcpUrls, null);
        // sync dsp urls if 7 days have passed since they were last synced
        if (userId != null && areDspUrlsToBeSynced()) {
            Logger.logDebug("Last DSP url sync was 7 days ago,syncing again");
            syncDspUrls(userId, dspUrls);
        }
    }

    /**
     * Sends a GET request for each url in {@code dspUrls}.
     * @param userId ?
     * @param dspUrls the list of dspUrls
     */
    public void processDspUrls(
            @NonNull String  userId,
            @Nullable ArrayList<LinkedTreeMap<String, String>> dspUrls
    ) {
        processUrls("dsp", dspUrls, userId);
    }

    /**
     * Resets the last sync time
     */
    public void reset() {
        Logger.logDebug("Resetting Storage");
        storage.reset();
    }
}
