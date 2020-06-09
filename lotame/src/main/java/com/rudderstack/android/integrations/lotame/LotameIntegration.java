package com.rudderstack.android.integrations.lotame;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.PatternSyntaxException;

/**
 * RudderStack's native SDK implementation for Lotame
 */
public class LotameIntegration {
    private static final long SYNC_INTERVAL = 1000 * 60 * 60 * 24 * 7; // 7 days in milliseconds

    private static Map<String, String> mappings;
    private static LotameStorage storage;
    private static ExecutorService es;
    private static LotameSyncCallback callback;
    private static LotameIntegration instance;


    /**
     * Returns an instance of {@code LotameIntegration}.
     * Creates and returns a new instance if it has been called for the first time.
     * Modifies and returns a pre-existing instance, if otherwise.
     *
     * @param application The parent application using the SDK
     * @param mappings    Contains the fields you would like to replace in your urls
     * @return An instance of {@code LotameIntegration}
     */
    public static LotameIntegration getInstance(
            @NonNull Application application,
            @Nullable Map<String, String> mappings
    ) {
        if (instance == null) {
            instance = new LotameIntegration(application, mappings);
        } else {
            instance.storage = LotameStorage.getInstance(application);
            instance.mappings = mappings;
        }
        return instance;
    }

    private LotameIntegration(Application application, Map<String, String> mappings) {
        this.storage = LotameStorage.getInstance(application);
        this.mappings = mappings;
        this.es = Executors.newSingleThreadExecutor();
    }

    /**
     * Registers the onSync callback.
     *
     * @param cb the onSync callback, must implement the interface {@code LotameSyncCallback}
     */
    public void registerCallback(LotameSyncCallback cb) {
        callback = cb;
        Logger.logInfo("onSync callback successfully registered");
    }

    private void executeCallback() {
        if (callback != null) {
            callback.onSync();
        } else {
            Logger.logDebug("No onSync callback registered");
        }
    }

    /**
     * Creates a new GET request and sends it.
     * The GET request is sent over the network from a background thread.
     *
     * @param url The url that would be used to create the GET request
     */
    public void makeGetRequest(String url) {
        // create and configure the GET request
        Logger.logDebug(String.format(Locale.US, "Creating a GET request with url %s", url));
        Runnable req = Utils.getRunnable(url);

        // make the get request
        if (es != null) {
            es.submit(req);
        }
    }

    // TODO: Add random support. {{random}} should be replaced with a random number
    private String compileUrl(String url, String userId, String randomValue) {
        String replacePattern = "\\{\\{%s\\}\\}";
        String key = null, value = null;
        try {
            url = url.replaceAll(String.format(replacePattern, "random"), URLEncoder.encode(randomValue));
            if (userId != null) {
                url = url.replaceAll(String.format(replacePattern, "userId"), URLEncoder.encode(userId));
            }
            if (mappings != null) {
                for (Map.Entry<String, String> entry : mappings.entrySet()) {
                    key = entry.getKey();
                    value = URLEncoder.encode(entry.getValue());
                    url = url.replaceAll(String.format(replacePattern, key), value);
                }
            }
        } catch (PatternSyntaxException ex) {
            Logger.logError(String.format("Error while compiling url %s." +
                            "Failed to replace {{%s}} with %s : %s"
                    , url, key, value, ex.getLocalizedMessage()));
        }
        return url;
    }

    private boolean areDspUrlsToBeSynced() {
        long lastSyncTime = storage.getLastSyncTime();
        long currentTime = Utils.getCurrentTime();
        if (lastSyncTime == -1) {
            return true;
        } else {
            return (currentTime - lastSyncTime) >= SYNC_INTERVAL;
        }
        // CHECK: can we simplify the return ..ask?
    }

    private void processUrls(
            String urlType,
            ArrayList<String> urls,
            String userId
    ) {
        String currentTime = String.valueOf(Utils.getCurrentTime());
        if (urls != null) {
            for (String url : urls) {
                url = compileUrl(url, userId, currentTime);
                makeGetRequest(url);
            }
        } else {
            Logger.logWarn(String.format("no %sUrls found in config", urlType));
        }
    }

    /**
     * Syncs the urls in {@code dspUrls} by sending a GET request for each one of them.
     * Sets the last sync time and executes the onSync callback.
     *
     * @param userId  Your userId
     * @param dspUrls the list of the urls to be synced
     */
    public void syncDspUrls(
            @NonNull String userId,
            @Nullable ArrayList<String> dspUrls
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

    /**
     * Sends a GET request for each url in {@code bcpUrls}.
     * Syncs the DSP urls if 7 days have passed since the last sync.
     *
     * @param bcpUrls the list of bcpUrls
     * @param dspUrls the list of dspUrls
     * @param userId  Your userId
     */
    public void processBcpUrls(
            @Nullable ArrayList<String> bcpUrls,
            @Nullable ArrayList<String> dspUrls,
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
     *
     * @param userId  Your userId
     * @param dspUrls the list of dspUrls
     */
    public void processDspUrls(
            @NonNull String userId,
            @Nullable ArrayList<String> dspUrls
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
