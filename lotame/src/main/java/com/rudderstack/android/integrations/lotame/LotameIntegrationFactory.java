package com.rudderstack.android.integrations.lotame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

        if (configMap != null && RudderClient.getApplication() != null) {
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

    private ArrayList<String> convertMapListToArrayList(
            String configType,
            ArrayList<Map<String, String>> mapList
    ) {
        ArrayList<String> list = new ArrayList<>();
        String key = String.format("%sUrlTemplate", configType);
        String value;
        if (mapList != null) {
            for (Map<String, String> ltm : mapList) {
                value = ltm.get(key);
                if (value != null && !value.isEmpty()) {
                    list.add(value);
                }
            }
        }
        return !list.isEmpty() ? list : null;
    }

    private Map<String, String> convertMapListToMap(
            ArrayList<Map<String, String>> mapList
    ) {
        Map<String, String> map = new HashMap<>();
        String key, value;

        if (mapList != null) {
            for (Map<String, String> node : mapList) {
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
        ArrayList<Map<String, String>> pixels, iFrames, list = new ArrayList<>();

        String pixelKey = String.format("%sUrlSettingsPixel", configType);
        String iFrameKey = String.format("%sUrlSettingsIframe", configType);

        pixels = (ArrayList<Map<String, String>>) configMap.get(pixelKey);
        iFrames = (ArrayList<Map<String, String>>) configMap.get(iFrameKey);

        if (pixels != null) {
            list.addAll(pixels);
        }

        if (iFrames != null) {
            list.addAll(iFrames);
        }

        return convertMapListToArrayList(configType, list);
    }

    private Map<String, String> getMappingConfig(Map<String, Object> configMap) {
        if (!configMap.containsKey("mappings")) return null;

        ArrayList<Map<String, String>> mapList =
                (ArrayList<Map<String, String>>) configMap.get("mappings");

        return convertMapListToMap(mapList);
    }
}
