package com.example.myapplication1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

public class RateActivity extends RateScreenActivity {
    static final String KEY_DOLLAR = "key_dollar";
    static final String KEY_EURO = "key_euro";
    static final String KEY_WON = "key_won";
    static final String PREFERENCES = "myrate";
    private static final String KEY_AMOUNT = "yuan_amount";
    private float dollarRate = 0.14f;
    private float euroRate = 0.13f;
    private float wonRate = 190f;
    private EditText amountView;
    private SharedPreferences preferences;
    private static final String KEY_SOURCE = "rate_source";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Thread fetchThread;
    private boolean loading;
    private boolean destroyed;

    // Registered in a stable order before the Activity reaches STARTED.
    private final ActivityResultLauncher<Intent> rateLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() != RESULT_OK || data == null
                        || !data.hasExtra(KEY_DOLLAR) || !data.hasExtra(KEY_EURO)
                        || !data.hasExtra(KEY_WON)) {
                    return;
                }
                try {
                    float dollar = checkedRate(data.getFloatExtra(KEY_DOLLAR, 0));
                    float euro = checkedRate(data.getFloatExtra(KEY_EURO, 0));
                    float won = checkedRate(data.getFloatExtra(KEY_WON, 0));
                    dollarRate = dollar;
                    euroRate = euro;
                    wonRate = won;
                } catch (IllegalArgumentException invalid) {
                    return;
                }
                preferences.edit().putString(KEY_SOURCE, getString(R.string.rate_manual)).apply();
                showSource();
                saveRates();
                showRates();
                calculate(false);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRateLayout(R.layout.activity_rate);
        amountView = findViewById(R.id.rate_amount);
        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        readRates(savedInstanceState);
        showSource();
        findViewById(R.id.rate_fetch_button).setOnClickListener(view -> fetchRates());
        if (savedInstanceState != null) {
            amountView.setText(savedInstanceState.getString(KEY_AMOUNT, ""));
        }
        showRates();
        findViewById(R.id.rate_convert_button).setOnClickListener(view -> calculate(true));
        findViewById(R.id.rate_edit_button).setOnClickListener(view -> openRateSettings());
        amountView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculate(false);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        calculate(false);
        if (RateCache.needsUpdate(preferences)) fetchRates();
    }

    @Override protected void onResume() {
        super.onResume();
        if (preferences != null && !loading) {
            readRates(null);
            showRates();
            showSource();
            calculate(false);
        }
    }

    private void showSource() {
        ((TextView) findViewById(R.id.rate_network_status)).setText(
                preferences.getString(KEY_SOURCE, getString(R.string.rate_fetch_idle)));
    }

    private void setLoading(boolean value) {
        loading = value;
        findViewById(R.id.rate_fetch_button).setEnabled(!value);
        findViewById(R.id.rate_edit_button).setEnabled(!value);
        invalidateOptionsMenu();
    }

    private void fetchRates() {
        if (loading) return;
        setLoading(true);
        ((TextView) findViewById(R.id.rate_network_status)).setText(R.string.rate_fetch_loading);
        // Worker thread performs I/O; Handler dispatches view changes to the main Looper.
        fetchThread = new Thread(() -> {
            try {
                WebRates rates = WebRates.fetch();
                boolean databaseSynced = RateDatabaseClient.save(rates);
                mainHandler.post(() -> {
                    if (destroyed) return;
                    dollarRate = rates.dollar;
                    euroRate = rates.euro;
                    wonRate = rates.won;
                    RateCache.save(this, rates, databaseSynced);
                    showRates();
                    showSource();
                    calculate(false);
                    setLoading(false);
                });
            } catch (IOException | RuntimeException error) {
                Log.w("WebRates", "Failed to fetch rates", error);
                mainHandler.post(() -> {
                    if (destroyed) return;
                    ((TextView) findViewById(R.id.rate_network_status)).setText(R.string.rate_fetch_error);
                    setLoading(false);
                });
            }
        }, "exchange-rate-network");
        fetchThread.start();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_edit_rates).setEnabled(!loading);
        menu.findItem(R.id.menu_rate_list).setEnabled(!loading);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (fetchThread != null) fetchThread.interrupt();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private static float checkedRate(float value) {
        return RateMath.parseRate(Float.toString(value));
    }

    private void readRates(Bundle state) {
        try {
            dollarRate = checkedRate(state == null ? preferences.getFloat(KEY_DOLLAR, 0.14f)
                    : state.getFloat(KEY_DOLLAR, 0.14f));
            euroRate = checkedRate(state == null ? preferences.getFloat(KEY_EURO, 0.13f)
                    : state.getFloat(KEY_EURO, 0.13f));
            wonRate = checkedRate(state == null ? preferences.getFloat(KEY_WON, 190f)
                    : state.getFloat(KEY_WON, 190f));
        } catch (IllegalArgumentException invalid) {
            dollarRate = 0.14f;
            euroRate = 0.13f;
            wonRate = 190f;
        }
    }

    private void openRateSettings() {
        Intent intent = new Intent(this, RateSettingsActivity.class);
        Bundle rates = new Bundle();
        rates.putFloat(KEY_DOLLAR, dollarRate);
        rates.putFloat(KEY_EURO, euroRate);
        rates.putFloat(KEY_WON, wonRate);
        intent.putExtras(rates);
        rateLauncher.launch(intent);
    }

    private void saveRates() {
        preferences.edit()
                .putFloat(KEY_DOLLAR, dollarRate)
                .putFloat(KEY_EURO, euroRate)
                .putFloat(KEY_WON, wonRate)
                .apply();
    }

    private void showRates() {
        ((TextView) findViewById(R.id.rate_current_values)).setText(getString(R.string.rate_current_values,
                RateMath.formatRate(dollarRate), RateMath.formatRate(euroRate), RateMath.formatRate(wonRate)));
    }

    private void calculate(boolean showError) {
        String amount = amountView.getText().toString();
        amountView.setError(null);
        try {
            String dollar = RateMath.convert(amount, dollarRate);
            String euro = RateMath.convert(amount, euroRate);
            String won = RateMath.convert(amount, wonRate);
            showResults(dollar, euro, won);
        } catch (IllegalArgumentException invalid) {
            String empty = getString(R.string.rate_empty_result);
            showResults(empty, empty, empty);
            if (showError) {
                amountView.setError(getString(R.string.rate_amount_invalid));
                amountView.requestFocus();
            }
        }
    }

    private void showResults(String dollar, String euro, String won) {
        ((TextView) findViewById(R.id.rate_dollar_result)).setText(getString(R.string.rate_dollar_result, dollar));
        ((TextView) findViewById(R.id.rate_euro_result)).setText(getString(R.string.rate_euro_result, euro));
        ((TextView) findViewById(R.id.rate_won_result)).setText(getString(R.string.rate_won_result, won));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rate, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_rate_database) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(RateDatabaseClient.BASE_URL + "/database")));
            } catch (android.content.ActivityNotFoundException missingBrowser) {
                android.widget.Toast.makeText(this, R.string.rate_database_browser_missing, android.widget.Toast.LENGTH_LONG).show();
            }
            return true;
        }
        if (item.getItemId() == R.id.menu_rate_list) {
            if (!loading) startActivity(new Intent(this, RateListActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.menu_edit_rates) {
            if (!loading) openRateSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putFloat(KEY_DOLLAR, dollarRate);
        outState.putFloat(KEY_EURO, euroRate);
        outState.putFloat(KEY_WON, wonRate);
        outState.putString(KEY_AMOUNT, amountView.getText().toString());
        super.onSaveInstanceState(outState);
    }
}
