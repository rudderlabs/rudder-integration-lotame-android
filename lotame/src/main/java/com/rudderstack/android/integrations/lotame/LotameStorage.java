package com.rudderstack.android.integrations.lotame;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

class LotameStorage {
    private static final String LOTAME_PREFS = "rl_lotame";
    private static final String LOTAME_LAST_SYNC_TIME = "rl_lotame_last_sync";
    private static SharedPreferences storage = null;
    private static LotameStorage storageInstance = null;

    private LotameStorage(Application application) {
        storage = application.getSharedPreferences(LOTAME_PREFS, Context.MODE_PRIVATE);
    }

    static LotameStorage getInstance(Application application) {
        if (storageInstance == null) {
            storageInstance = new LotameStorage(application);
        }
        return storageInstance;
    }

    // CHECK: do we need static here? 
    static void setLastSyncTime(long syncTime) {
        if (storage != null) {
            storage.edit().putLong(LOTAME_LAST_SYNC_TIME, syncTime).apply();
        }
    }

    // CHECK: do we need static here? 
    static long getLastSyncTime() {
        if (storage != null) {
            return storage.getLong(LOTAME_LAST_SYNC_TIME, -1);
        } else {
            return -1;
        }
    }
}
