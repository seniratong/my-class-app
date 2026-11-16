package com.example.myapplication1;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RateDetailActivity extends RateScreenActivity {
    static final String EXTRA_CURRENCY = "currency";
    static final String EXTRA_QUOTE = "quote";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String currency = getIntent().getStringExtra(EXTRA_CURRENCY);
        String quoteText = getIntent().getStringExtra(EXTRA_QUOTE);
        BigDecimal quote;
        try {
            if (currency == null || currency.isBlank() || quoteText == null) {
                throw new IllegalArgumentException();
            }
            RateMath.parseRate(quoteText);
            quote = new BigDecimal(quoteText);
        } catch (IllegalArgumentException invalid) {
            finish();
            return;
        }

        setRateLayout(R.layout.activity_rate_detail);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.rate_toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationContentDescription(R.string.rate_list_back);
        toolbar.setNavigationOnClickListener(view -> finish());

        TextView title = findViewById(R.id.rate_detail_currency);
        title.setText(currency);
        TextView quoteView = findViewById(R.id.rate_detail_quote);
        quoteView.setText(getString(R.string.rate_detail_quote, currency, quoteText));
        EditText amount = findViewById(R.id.rate_detail_amount);
        TextView result = findViewById(R.id.rate_detail_result);
        findViewById(R.id.rate_detail_convert).setOnClickListener(view ->
                convert(amount, result, currency, quote));
    }

    private void convert(EditText input, TextView result, String currency, BigDecimal quote) {
        try {
            BigDecimal amount = new BigDecimal(input.getText().toString().trim());
            if (amount.signum() < 0) throw new IllegalArgumentException();
            BigDecimal foreignAmount = amount.movePointRight(2).divide(quote, 2, RoundingMode.HALF_UP);
            input.setError(null);
            result.setText(getString(R.string.rate_detail_result, foreignAmount.toPlainString(), currency));
            getSystemService(android.view.inputmethod.InputMethodManager.class)
                    .hideSoftInputFromWindow(input.getWindowToken(), 0);
        } catch (IllegalArgumentException invalid) {
            input.setError(getString(R.string.rate_amount_invalid));
            result.setText("");
        }
    }
}
