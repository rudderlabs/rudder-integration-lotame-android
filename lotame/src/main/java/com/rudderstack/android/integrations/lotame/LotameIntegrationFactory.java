package com.rudderstack.android.integrations.lotame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.google.gson.internal.LinkedTreeMap;
import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.RudderMessage;

import java.util.ArrayList;
import java.util.Map;

public class LotameIntegrationFactory extends RudderIntegration<LotameIntegration> {

    private static final String LOTAME_KEY = "Lotame";
    private ArrayList<LinkedTreeMap<String, String>> bcpUrls = null;
    private ArrayList<LinkedTreeMap<String, String>> dspUrls = null;
    private LotameIntegration lotameClient;

    public static RudderIntegration.Factory FACTORY = new Factory() {
        @Override
        public RudderIntegration<LotameIntegration> create(Object settings, RudderClient client, RudderConfig config) {
            return new LotameIntegrationFactory(settings, client);
        }

        @Override
        public String key() {
            return LOTAME_KEY;
        }
    };

    private LotameIntegrationFactory(@Nullable Object config, @NonNull RudderClient client) {
        Map<String, Object> configMap = (Map<String, Object>) config;
        ArrayList<LinkedTreeMap<String, String>> mappings = null;

        if (configMap != null && client.getApplication() != null) {
            if(configMap.containsKey("bcpUrlSettings")) {
                bcpUrls = (ArrayList<LinkedTreeMap<String, String>>) configMap.get("bcpUrlSettings");
            }
            if(configMap.containsKey("dspUrlSettings")) {
                dspUrls = (ArrayList<LinkedTreeMap<String, String>>) configMap.get("dspUrlSettings");
            }
            if(configMap.containsKey("mappings")) {
                mappings = (ArrayList<LinkedTreeMap<String, String>>) configMap.get("mappings");
            }

            lotameClient = new LotameIntegration(client.getApplication(), mappings);// will get init only when config is present
        }
    }

    @Override
    public void reset() {
//        bugSnagClient.clearBreadcrumbs();
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
                    this.screen(message);
                    break;
                case MessageType.IDENTIFY:
                    this.identify(message);
                    break;
                default:
                    RudderLogger.logWarn(String.format("RudderIntegration: Lotame: Message type %s is not supported", eventType));
            }
        } else {
            RudderLogger.logDebug("RudderIntegration: Lotame: processEvent: eventType null");
        }
    }

    private void identify(@NonNull RudderMessage message) {
        String userId = message.getUserId();
        if (userId!= null) {
            lotameClient.syncDspUrls(this.dspUrls, userId);
        } else {
            RudderLogger.logWarn("RudderIntegration: Lotame: identify: no userId found, " +
                    "not syncing any pixels");
        }
    }

    private void screen(@NonNull RudderMessage message) {
        String userId = message.getUserId();
        lotameClient.processBcpUrls(this.bcpUrls, this.dspUrls, userId);
    }


}
