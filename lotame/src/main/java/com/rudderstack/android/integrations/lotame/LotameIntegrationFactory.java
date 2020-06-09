package com.rudderstack.android.integrations.lotame;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rudderstack.android.integrations.lotame.sdk.LotameIntegration;
import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.RudderMessage;

import java.util.ArrayList;
import java.util.Map;

/**
 * RudderStack's Integration Factory for Lotame
 */
public class LotameIntegrationFactory extends RudderIntegration<LotameIntegration> {

    private static final String LOTAME_KEY = "Lotame";
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
            bcpUrls = Utils.getUrlConfig("bcp", configMap);
            dspUrls = Utils.getUrlConfig("dsp", configMap);
            lotameClient = LotameIntegration.getInstance(
                    client.getApplication(),
                    Utils.getMappingConfig(configMap),
                    rudderConfig.getLogLevel()
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
                    lotameClient.processBcpUrls(
                            this.bcpUrls,
                            this.dspUrls,
                            message.getUserId()
                    );
                    break;
                case MessageType.IDENTIFY:
                    if (message.getUserId() != null) {
                        lotameClient.syncDspUrls(message.getUserId(), this.dspUrls);
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
}
