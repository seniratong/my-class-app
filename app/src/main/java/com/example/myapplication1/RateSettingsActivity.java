package com.example.myapplication1;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

public class RateSettingsActivity extends RateScreenActivity {
    private EditText dollarView;
    private EditText euroView;
    private EditText wonView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRateLayout(R.layout.activity_rate_settings);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        dollarView = findViewById(R.id.edit_dollar_rate);
        euroView = findViewById(R.id.edit_euro_rate);
        wonView = findViewById(R.id.edit_won_rate);
        if (savedInstanceState == null) {
            Bundle rates = getIntent().getExtras();
            dollarView.setText(RateMath.formatRate(rates == null ? 0.14f
                    : rates.getFloat(RateActivity.KEY_DOLLAR, 0.14f)));
            euroView.setText(RateMath.formatRate(rates == null ? 0.13f
                    : rates.getFloat(RateActivity.KEY_EURO, 0.13f)));
            wonView.setText(RateMath.formatRate(rates == null ? 190f
                    : rates.getFloat(RateActivity.KEY_WON, 190f)));
        }
        findViewById(R.id.save_rates).setOnClickListener(view -> saveAndReturn());
        findViewById(R.id.cancel_rates).setOnClickListener(view -> finish());
    }

    private Float readRate(EditText view) {
        view.setError(null);
        try {
            return RateMath.parseRate(view.getText().toString());
        } catch (IllegalArgumentException invalid) {
            view.setError(getString(R.string.rate_invalid));
            return null;
        }
    }

    private void saveAndReturn() {
        Float newDollar = readRate(dollarView);
        Float newEuro = readRate(euroView);
        Float newWon = readRate(wonView);
        if (newDollar == null || newEuro == null || newWon == null) {
            (newDollar == null ? dollarView : newEuro == null ? euroView : wonView).requestFocus();
            return;
        }
        Intent result = new Intent();
        result.putExtra(RateActivity.KEY_DOLLAR, newDollar.floatValue());
        result.putExtra(RateActivity.KEY_EURO, newEuro.floatValue());
        result.putExtra(RateActivity.KEY_WON, newWon.floatValue());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
