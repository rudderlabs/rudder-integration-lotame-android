package com.rudderstack.android.integrations.lotame.sdk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

class LotameStorage {
    private static final String LOTAME_PREFS = "rl_lotame";
    private static final String LOTAME_LAST_SYNC_TIME = "rl_lotame_last_sync";
    private static SharedPreferences sharedPref = null;
    private static LotameStorage instance = null;

    private LotameStorage(Application application) {
        sharedPref = application.getSharedPreferences(LOTAME_PREFS, Context.MODE_PRIVATE);
    }

    static LotameStorage getInstance(Application application) {
        if (instance == null) {
            instance = new LotameStorage(application);
        }
        return instance;
    }

    void setLastSyncTime(long syncTime) {
        if (sharedPref != null) {
            sharedPref.edit().putLong(LOTAME_LAST_SYNC_TIME, syncTime).apply();
        }
    }

    long getLastSyncTime() {
        if (sharedPref != null) {
            return sharedPref.getLong(LOTAME_LAST_SYNC_TIME, -1);
        } else {
            return -1;
        }
    }

    void reset() {
        if (sharedPref != null) {
            sharedPref.edit().clear().apply();
        }
    }
}
