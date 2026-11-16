package com.example.myapplication1;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Fetches network quotes and binds them to custom, selectable list rows. */
public class RateListActivity extends ListActivity implements Runnable {
    private static final int SUCCESS = 5;
    private static final int FAILURE = 6;
    private static final String CURRENCY = "currency";
    private static final String QUOTE = "quote";
    private final List<Map<String, String>> items = new ArrayList<>();
    private SimpleAdapter adapter;
    private SharedPreferences preferences;
    private TextView status;
    private Thread worker;
    private boolean loading;
    private volatile boolean destroyed;
    private boolean databaseSynced;
    private final Handler handler = new Handler(Looper.getMainLooper(), this::handleMessage);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_rate_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rate_list_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.rate_list_root));
        preferences = getSharedPreferences(RateActivity.PREFERENCES, MODE_PRIVATE);
        status = findViewById(R.id.rate_list_status);
        adapter = new SimpleAdapter(this, items, R.layout.rate_list_item,
                new String[]{CURRENCY, QUOTE}, new int[]{R.id.rate_row_currency, R.id.rate_row_quote});
        setListAdapter(adapter);
        getListView().setOnItemClickListener((parent, view, position, id) -> openConverter(position));
        showCache();
        findViewById(R.id.rate_list_back).setOnClickListener(view -> finish());
        findViewById(R.id.rate_list_refresh).setOnClickListener(view -> refresh());
        if (RateCache.needsUpdate(preferences)) refresh();
    }

    private void openConverter(int position) {
        Map<String, String> item = items.get(position);
        Intent intent = new Intent(this, RateDetailActivity.class);
        intent.putExtra(RateDetailActivity.EXTRA_CURRENCY, item.get(CURRENCY));
        intent.putExtra(RateDetailActivity.EXTRA_QUOTE, item.get(QUOTE));
        startActivity(intent);
    }

    private void showRows(List<String> rows) {
        items.clear();
        for (String row : rows) {
            String[] fields = row.split(" => ", 2);
            if (fields.length == 2) items.add(Map.of(CURRENCY, fields[0], QUOTE, fields[1]));
        }
        adapter.notifyDataSetChanged();
    }

    private void showCache() {
        showRows(RateCache.rows(preferences));
        status.setText(adapter.isEmpty() ? getString(R.string.rate_list_empty)
                : getString(R.string.rate_list_cached, preferences.getString(RateCache.KEY_DATE, "")));
    }

    private void refresh() {
        if (loading) return;
        loading = true;
        findViewById(R.id.rate_list_refresh).setEnabled(false);
        status.setText(R.string.rate_fetch_loading);
        worker = new Thread(this, "rate-list-network");
        worker.start();
    }

    @Override public void run() {
        try {
            WebRates rates = WebRates.fetch();
            databaseSynced = RateDatabaseClient.save(rates);
            if (!destroyed) handler.obtainMessage(SUCCESS, rates).sendToTarget();
        } catch (IOException | RuntimeException error) {
            Log.w("RateList", "Failed to fetch rates", error);
            if (!destroyed) handler.sendEmptyMessage(FAILURE);
        }
    }

    private boolean handleMessage(Message message) {
        if (destroyed) return true;
        if (message.what == SUCCESS) {
            WebRates rates = (WebRates) message.obj;
            RateCache.save(this, rates, databaseSynced);
            showRows(rates.rows);
            status.setText(preferences.getString(RateCache.KEY_SOURCE, ""));
        } else if (message.what == FAILURE) {
            status.setText(adapter.isEmpty() ? getString(R.string.rate_list_failed_empty)
                    : getString(R.string.rate_list_failed_cached, preferences.getString(RateCache.KEY_DATE, "")));
        }
        loading = false;
        findViewById(R.id.rate_list_refresh).setEnabled(true);
        return true;
    }

    @Override protected void onDestroy() {
        destroyed = true;
        if (worker != null) worker.interrupt();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
