package com.rudderstack.android.integrations.lotame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.internal.LinkedTreeMap;
import com.rudderstack.android.integrations.lotame.sdk.LotameIntegration;
import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderContext;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.RudderMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * RudderStack's Integration Factory for Lotame
 */
public class LotameIntegrationFactory extends RudderIntegration<LotameIntegration> {

    private static final String LOTAME_KEY = "Lotame Mobile";
    private ArrayList<String> bcpUrls = null;
    private ArrayList<String> dspUrls = null;
    private LotameIntegration lotameClient;

    public static RudderIntegration.Factory FACTORY = new Factory() {
        @Override
        public RudderIntegration<LotameIntegration> create(
                Object settings,
                RudderClient client,
                RudderConfig config
        ) {
            return new LotameIntegrationFactory(settings, client, config);
        }

        @Override
        public String key() {
            return LOTAME_KEY;
        }
    };

    private LotameIntegrationFactory(
            @Nullable Object config,
            @NonNull RudderClient client,
            @NonNull RudderConfig rudderConfig
    ) {
        Map<String, Object> configMap = (Map<String, Object>) config;

        if (configMap != null && client.getApplication() != null) {
            bcpUrls = getUrlConfig("bcp", configMap);
            dspUrls = getUrlConfig("dsp", configMap);
            RudderContext rudderContext = client.getRudderContext();
            lotameClient = LotameIntegration.getInstance(
                    client.getApplication(),
                    getMappingConfig(configMap),
                    rudderConfig.getLogLevel(),
                    rudderContext.getAdvertisingId()
            );
        }
    }

    @Override
    public void reset() {
        lotameClient.reset();
    }

    @Override
    public LotameIntegration getUnderlyingInstance() {
        return this.lotameClient;
    }

    @Override
    public void dump(@Nullable RudderMessage element) {
        try {
            if (element != null) {
                this.processEvent(element);
            }
        } catch (Exception ex) {
            RudderLogger.logError(ex);
        }
    }

    private void processEvent(@NonNull RudderMessage message) {
        String eventType = message.getType();
        if (eventType != null) {
            switch (eventType) {
                case MessageType.SCREEN:
                    if (message.getUserId() != null) {
                        lotameClient.syncBcpAndDspUrls(
                                message.getUserId(),
                                this.bcpUrls,
                                this.dspUrls
                        );
                    }

                    break;
                case MessageType.IDENTIFY:
                    if (message.getUserId() != null) {
                        lotameClient.syncDspUrls(message.getUserId(), this.dspUrls, true);
                    } else {
                        RudderLogger.logWarn("RudderIntegration: Lotame: identify: no userId found, not syncing any pixels");
                    }
                    break;
                default:
                    RudderLogger.logWarn(String.format("RudderIntegration: Lotame: Message type %s is not supported", eventType));
            }
        } else {
            RudderLogger.logDebug("RudderIntegration: Lotame: processEvent: eventType null");
        }
    }

    private ArrayList<String> convertLinkedTreeMapListToArrayList(
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
        return !list.isEmpty() ? list : null;
    }

    private Map<String, String> convertLinkedTreeMapListToMap(
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

    private ArrayList<String> getUrlConfig(String configType, Map<String, Object> configMap) {
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

    private Map<String, String> getMappingConfig(Map<String, Object> configMap) {
        if (!configMap.containsKey("mappings")) return null;

        ArrayList<LinkedTreeMap<String, String>> ltm =
                (ArrayList<LinkedTreeMap<String, String>>) configMap.get("mappings");

        return convertLinkedTreeMapListToMap(ltm);
    }
}
