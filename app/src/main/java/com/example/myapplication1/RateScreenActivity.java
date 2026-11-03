package com.example.myapplication1;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

abstract class RateScreenActivity extends AppCompatActivity {
    protected void setRateLayout(@LayoutRes int layout) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(layout);
        setSupportActionBar(findViewById(R.id.rate_toolbar));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rate_root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.rate_root));
    }
}
