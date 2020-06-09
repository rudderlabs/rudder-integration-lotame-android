package com.rudderstack.android.integrations.lotame.sdk;

import android.util.Log;

/*
 * Logger class for LotameIntegration
 * */
public class Logger {
    private static int logLevel = LogLevel.INFO;
    private static final String TAG = "LotameIntegration";

    static void init(int l) {
        if (l > LogLevel.VERBOSE) l = LogLevel.VERBOSE;
        else if (l < LogLevel.NONE) l = LogLevel.NONE;
        logLevel = l;
    }

    public static void logError(String message) {
        if (logLevel >= LogLevel.ERROR) {
            Log.e(TAG, "Error: " + message);
        }
    }

    public static void logWarn(String message) {
        if (logLevel >= LogLevel.WARN) {
            Log.w(TAG, "Warn: " + message);
        }
    }

    public static void logInfo(String message) {
        if (logLevel >= LogLevel.INFO) {
            Log.i(TAG, "Info: " + message);
        }
    }

    public static void logDebug(String message) {
        if (logLevel >= LogLevel.DEBUG) {
            Log.d(TAG, "Debug: " + message);
        }
    }

    public static class LogLevel {
        public static final int VERBOSE = 5;
        public static final int DEBUG = 4;
        public static final int INFO = 3;
        public static final int WARN = 2;
        public static final int ERROR = 1;
        public static final int NONE = 0;
    }
}
