package com.rudderstack.android.integrations.lotame;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.Map;

class Utils {

    // to be used later
    private static ArrayList<String> convertLinkedTreeMapListToArrayList(
            String configType,
            ArrayList<LinkedTreeMap<String, String>> linkedTreeMapList
    ) {
        ArrayList<String> list = null;
        String key = String.format("%sUrlTemplate", configType);
        String value;
        if (linkedTreeMapList != null) {
            list = new ArrayList<>();
            for (LinkedTreeMap<String, String> ltm : linkedTreeMapList) {
                value = ltm.get(key);
                if (value != null) {
                    list.add(value);
                }
            }
        }
        return list;
    }

    static ArrayList<LinkedTreeMap<String, String>> getBcpConfig(Map<String, Object> config) {
        ArrayList<LinkedTreeMap<String, String>> bcpPixels, bcpIframes, list;
        bcpPixels = (ArrayList<LinkedTreeMap<String, String>>) config.get("bcpUrlSettingsPixel");
        bcpIframes = (ArrayList<LinkedTreeMap<String, String>>) config.get("bcpUrlSettingsIframe");
        list = null;
        if (bcpPixels != null && bcpIframes != null) {
            list = bcpPixels;
            list.addAll(bcpIframes);
        }
        else if(bcpPixels != null && bcpIframes == null) {
            list = bcpPixels;
        }
        else if(bcpPixels == null && bcpIframes != null) {
            list = bcpIframes;
        }
        return list;
    }

    static ArrayList<LinkedTreeMap<String, String>> getDspConfig(Map<String, Object> config) {
        ArrayList<LinkedTreeMap<String, String>> dspPixels, dspIframes, list;
        dspPixels = (ArrayList<LinkedTreeMap<String, String>>) config.get("dspUrlSettingsPixel");
        dspIframes = (ArrayList<LinkedTreeMap<String, String>>) config.get("dspUrlSettingsIframe");
        list = null;
        if (dspPixels != null && dspIframes != null) {
            list = dspPixels;
            list.addAll(dspIframes);
        }
        else if(dspPixels != null && dspIframes == null) {
            list = dspPixels;
        }
        else if(dspPixels == null && dspIframes != null) {
            list = dspIframes;
        }
        return list;
    }
}
