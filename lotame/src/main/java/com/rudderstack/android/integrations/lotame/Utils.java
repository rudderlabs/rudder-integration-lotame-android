package com.rudderstack.android.integrations.lotame;

import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class Utils {

    private static ArrayList<String> convertLinkedTreeMapListToArrayList(
            String configType,
            ArrayList<LinkedTreeMap<String, String>> linkedTreeMapList
    ) {
        ArrayList<String> list = new ArrayList<>();
        String key = String.format("%sUrlTemplate", configType);
        String value;
        if (linkedTreeMapList != null) {
            for (LinkedTreeMap<String, String> ltm : linkedTreeMapList) {
                value = ltm.get(key);
                if (value != null && !value.isEmpty()) {
                    list.add(value);
                }
            }
        }
        return !list.isEmpty() ? list : null;// check if we can instead return empty list and use that
    }

    private static Map<String, String> convertLinkedTreeMapListToMap(
            ArrayList<LinkedTreeMap<String, String>> linkedTreeMapList
    ) {
        Map<String, String> map = new HashMap<>();
        String key, value;

        if (linkedTreeMapList != null) {
            for (LinkedTreeMap<String, String> node : linkedTreeMapList) {
                key = node.get("key");
                value = node.get("value");
                if ((key != null && !key.isEmpty()) && (value != null && !value.isEmpty())) {
                    map.put(key, value);
                }
            }
        }

        return !map.isEmpty() ? map : null;
    }

    static ArrayList<String> getUrlConfig(String configType, Map<String, Object> configMap) {
        ArrayList<LinkedTreeMap<String, String>> pixels, iFrames, list;

        String pixelKey = String.format("%sUrlSettingsPixel", configType);
        String iFrameKey = String.format("%sUrlSettingsIframe", configType);

        pixels = (ArrayList<LinkedTreeMap<String, String>>) configMap.get(pixelKey);
        iFrames = (ArrayList<LinkedTreeMap<String, String>>) configMap.get(iFrameKey);

        list = null;
        if (pixels != null && iFrames != null) {
            list = pixels;
            list.addAll(iFrames);
        } else if (pixels != null && iFrames == null) {
            list = pixels;
        } else if (pixels == null && iFrames != null) {
            list = iFrames;
        }

        return convertLinkedTreeMapListToArrayList(configType, list);
    }

    static Map<String, String> getMappingConfig(Map<String, Object> configMap) {
        if (!configMap.containsKey("mappings")) return null;

        ArrayList<LinkedTreeMap<String, String>> ltm =
                (ArrayList<LinkedTreeMap<String, String>>) configMap.get("mappings");

        return convertLinkedTreeMapListToMap(ltm);
    }

    static Runnable getRunnable(final String url) {
        Runnable req = new Runnable() {
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
                        Logger.logDebug(String.format(Locale.US, "GET request to %s returned" +
                                "a %d response", url, responseCode));
                    } else {
                        Logger.logError(String.format(Locale.US, "GET request to %s returned" +
                                "a %d response", url, responseCode));
                    }// TODO: check this
                } catch (MalformedURLException ex) {
                    Logger.logError(String.format(Locale.US, "Malformed URL %s : %s",
                            url, ex.getLocalizedMessage()));
                } catch (IOException ex) {
                    Logger.logError(String.format(Locale.US, "Error while making request to %s : %s",
                            url, ex.getLocalizedMessage()));
                }
            }
        };
        return req;
    }

    static long getCurrentTime() {
        return new Date().getTime();
    }
}
