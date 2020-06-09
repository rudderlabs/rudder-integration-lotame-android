package com.rudderstack.android.integrations.lotame;

import com.google.gson.internal.LinkedTreeMap;
import com.rudderstack.android.integrations.lotame.sdk.Logger;

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
        return list;
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
        ArrayList<LinkedTreeMap<String, String>> pixels, iFrames, list = new ArrayList<>();

        String pixelKey = String.format("%sUrlSettingsPixel", configType);
        String iFrameKey = String.format("%sUrlSettingsIframe", configType);

        pixels = (ArrayList<LinkedTreeMap<String, String>>) configMap.get(pixelKey);
        iFrames = (ArrayList<LinkedTreeMap<String, String>>) configMap.get(iFrameKey);

        if (pixels != null) {
            list.addAll(pixels);
        }

        if (iFrames != null) {
            list.addAll(iFrames);
        }

        return convertLinkedTreeMapListToArrayList(configType, list);
    }

    static Map<String, String> getMappingConfig(Map<String, Object> configMap) {
        if (!configMap.containsKey("mappings")) return null;

        ArrayList<LinkedTreeMap<String, String>> ltm =
                (ArrayList<LinkedTreeMap<String, String>>) configMap.get("mappings");

        return convertLinkedTreeMapListToMap(ltm);
    }
}
