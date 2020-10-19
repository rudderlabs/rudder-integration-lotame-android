package com.rudderstack.android.integrations.lotame.sdk;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
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

    private final Map<String, String> mappings;
    private final String advertisingId;
    private final LotameStorage storage;
    private final ExecutorService es;
    private LotameSyncCallback callback;
    private static LotameIntegration instance;

    /**
     * Returns an instance of {@code LotameIntegration}.
     * Creates and returns a new instance if it has been called for the first time.
     * Modifies and returns a pre-existing instance, if otherwise.
     *
     * @param application   The parent application using the SDK
     * @param mappings      Contains the fields you would like to replace in your urls
     * @param advertisingId Your device's advertising ID
     * @return An instance of {@code LotameIntegration}
     */
    public static LotameIntegration getInstance(
            @NonNull Application application,
            @Nullable Map<String, String> mappings,
            int logLevel,
            String advertisingId
    ) {
        if (instance == null) {
            instance = new LotameIntegration(application, mappings, logLevel, advertisingId);
        }
        return instance;
    }

    private LotameIntegration(Application application, Map<String, String> mappings, int logLevel, String advertisingId) {
        this.storage = LotameStorage.getInstance(application);
        this.mappings = mappings;
        this.es = Executors.newSingleThreadExecutor();
        this.advertisingId = advertisingId;
        Logger.init(logLevel);
    }

    /**
     * Registers the onSync callback.
     *
     * @param cb the onSync callback, must implement the interface {@code LotameSyncCallback}
     */
    public void registerCallback(LotameSyncCallback cb) {
        this.callback = cb;
        Logger.logInfo("onSync callback successfully registered");
    }

    /**
     * Syncs the urls in {@code bcpUrls} by sending a GET request for each one of them.
     *
     * @param userId  Your userId
     * @param bcpUrls The list of the urls to be synced
     */
    public void syncBcpUrls(
            @NonNull String userId,
            @Nullable List<String> bcpUrls
    ) {
        Logger.logDebug(String.format(Locale.US, "Syncing BCP Urls : %s", bcpUrls));
        this.processUrls("bcp", userId, bcpUrls);
    }

    /**
     * Syncs the urls in {@code dspUrls} by sending a GET request for each one of them.
     * Sets the last sync time and executes the onSync callback.
     *
     * @param userId  Your userId
     * @param dspUrls The list of the urls to be synced
     */
    public void syncDspUrls(
            @NonNull String userId,
            @Nullable List<String> dspUrls,
            boolean force
    ) {
        Logger.logDebug(String.format(Locale.US, "Syncing DSP Urls : %s", dspUrls));

        // sync dsp urls if 7 days have passed since they were last synced
        if (force || areDspUrlsToBeSynced()) {
            Logger.logDebug("Last DSP url sync was 7 days ago,syncing again");
            this.processUrls("dsp", userId, dspUrls);

            // set last sync time
            Logger.logDebug("Updating last sync time with current time");
            storage.setLastSyncTime(new Date().getTime());
        }
    }

    /**
     * Syncs the urls in {@code bcpUrls} and {@code dspRules} by sending a GET request for each one of them.
     *
     * @param userId  Your userId
     * @param bcpUrls The list of BCP urls
     * @param dspUrls The list of DSP urls
     */
    public void syncBcpAndDspUrls(
            @NonNull String userId,
            @Nullable List<String> bcpUrls,
            @Nullable List<String> dspUrls
    ) {
        this.syncBcpUrls(userId, bcpUrls);
        this.syncDspUrls(userId, dspUrls, false);
    }

    /**
     * Creates a new GET request and sends it.
     * The GET request is sent over the network from a background thread.
     *
     * @param url The url that would be used to create the GET request
     */
    public void makeGetRequest(@NonNull String url) {
        // create and configure the GET request
        Logger.logDebug(String.format(Locale.US, "Creating a GET request with url %s", url));

        // make the get request
        if (es != null) {
            es.submit(this.getRunnable(url));
        }
    }

    /**
     * Resets the last sync time
     */
    public void reset() {
        Logger.logDebug("Resetting Storage");
        storage.reset();
    }

    @Nullable
    private String compileUrl(
            @NonNull String url,
            @NonNull String userId,
            @NonNull String randomValue
    ) {
        String replacePattern = "\\{\\{%s\\}\\}";
        String key = null, value = null;
        try {
            String compiledUrl = url.replaceAll(
                    String.format(replacePattern, "random"),
                    randomValue
            ).replaceAll(
                    String.format(replacePattern, "userId"),
                    URLEncoder.encode(userId, "UTF-8")
            );
            if (advertisingId != null) {
                compiledUrl = compiledUrl.replaceAll(
                        String.format(replacePattern, "DEVICE_ID"),
                        advertisingId
                ).replaceAll(
                        String.format(replacePattern, "DEVICE_TYPE"),
                        "GAID"
                );
            }
            if (this.mappings != null) {
                for (Map.Entry<String, String> entry : this.mappings.entrySet()) {
                    key = entry.getKey();
                    value = entry.getValue();
                    compiledUrl = compiledUrl.replaceAll(String.format(replacePattern, key), value);
                }
            }
            return compiledUrl;
        } catch (PatternSyntaxException ex) {
            Logger.logError(String.format("Error while compiling url %s." +
                            "Failed to replace {{%s}} with %s : %s"
                    , url, key, value, ex.getLocalizedMessage()));
        } catch (UnsupportedEncodingException ex) {
            Logger.logError(String.format("Error while URL encoding userId %s: %s"
                    , userId, ex.getLocalizedMessage()));
        }
        return null;
    }

    private boolean areDspUrlsToBeSynced() {
        return (this.storage.getLastSyncTime() == -1) ||
                ((new Date().getTime() - this.storage.getLastSyncTime()) >= SYNC_INTERVAL);
    }

    private void processUrls(
            @NonNull String urlType,
            @NonNull String userId,
            @Nullable List<String> urls
    ) {
        String currentTime = String.valueOf(new Date().getTime());
        if (urls != null) {
            for (String url : urls) {
                url = compileUrl(url, userId, currentTime);

                if (url != null) {
                    makeGetRequest(url);
                    if (this.callback != null) {
                        // execute callback if present
                        this.callback.onSync(urlType, url);
                        Logger.logDebug("onSync callback executed");
                    }
                }
            }
        } else {
            Logger.logWarn(String.format("%sUrl list is empty", urlType));
        }
    }

    private Runnable getRunnable(@NonNull final String url) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    // create and configure the http connection
                    HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
                    httpConnection.setRequestMethod("GET");

                    // make the get request
                    Logger.logDebug(String.format("Sending GET request to %s", url));
                    httpConnection.connect();

                    // get the response code
                    int responseCode = httpConnection.getResponseCode();
                    if (responseCode < 400) {
                        Logger.logDebug(String.format(Locale.US, "GET request to %s returned a %d response", url, responseCode));
                    } else {
                        Logger.logError(String.format(Locale.US, "GET request to %s returned a %d response", url, responseCode));
                    }
                } catch (MalformedURLException ex) {
                    Logger.logError(String.format(Locale.US, "Malformed URL %s : %s",
                            url, ex.getLocalizedMessage()));
                } catch (IOException ex) {
                    Logger.logError(String.format(Locale.US, "Error while making request to %s : %s",
                            url, ex.getLocalizedMessage()));
                }
            }
        };
    }
}
