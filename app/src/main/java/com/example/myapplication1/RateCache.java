package com.example.myapplication1;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Shared successful network snapshot for both the converter and list. */
final class RateCache {
    static final String KEY_DATE = "update_date";
    static final String KEY_ROWS = "rate_rows";
    static final String KEY_SOURCE = "rate_source";
    static final String KEY_DATABASE_SYNCED = "database_synced";

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
    }

    static List<String> rows(SharedPreferences prefs) {
        String data = prefs.getString(KEY_ROWS, "");
        return data.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(data.split("\n")));
    }

    static boolean needsUpdate(SharedPreferences prefs) {
        if (!today().equals(prefs.getString(KEY_DATE, "")) || rows(prefs).isEmpty()) return true;
        for (String key : new String[]{RateActivity.KEY_DOLLAR, RateActivity.KEY_EURO, RateActivity.KEY_WON}) {
            float value = prefs.getFloat(key, 0);
            if (!Float.isFinite(value) || value <= 0) return true;
        }
        return false;
    }

    static void save(Context context, WebRates rates) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
        context.getSharedPreferences(RateActivity.PREFERENCES, Context.MODE_PRIVATE).edit()
                .putFloat(RateActivity.KEY_DOLLAR, rates.dollar)
                .putFloat(RateActivity.KEY_EURO, rates.euro)
                .putFloat(RateActivity.KEY_WON, rates.won)
                .putString(KEY_ROWS, String.join("\n", rates.rows))
                .putString(KEY_SOURCE, context.getString(R.string.rate_fetch_success, time))
                .remove(KEY_DATABASE_SYNCED)
                .putString(KEY_DATE, today()).apply();
    }

    static void save(Context context, WebRates rates, boolean databaseSynced) {
        save(context, rates);
        SharedPreferences prefs = context.getSharedPreferences(RateActivity.PREFERENCES, Context.MODE_PRIVATE);
        String source = prefs.getString(KEY_SOURCE, "") + "\n" + context.getString(databaseSynced
                ? R.string.rate_database_saved : R.string.rate_database_failed);
        prefs.edit().putBoolean(KEY_DATABASE_SYNCED, databaseSynced).putString(KEY_SOURCE, source).apply();
    }
}
