package com.example.myapplication1;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class ScoreSettingsActivity extends AppCompatActivity {
    private EditText scoreAView;
    private EditText scoreBView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_score_settings);
        setSupportActionBar(findViewById(R.id.settings_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.score_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.main));

        scoreAView = findViewById(R.id.edit_score_a);
        scoreBView = findViewById(R.id.edit_score_b);
        // Unique EditText IDs let Android restore unfinished input automatically.
        if (savedInstanceState == null) {
            scoreAView.setText(Integer.toString(Math.max(0,
                    getIntent().getIntExtra(ScoreStore.KEY_TEAM_A, 0))));
            scoreBView.setText(Integer.toString(Math.max(0,
                    getIntent().getIntExtra(ScoreStore.KEY_TEAM_B, 0))));
        }
        findViewById(R.id.save_scores).setOnClickListener(view -> saveScores());
        findViewById(R.id.cancel_scores).setOnClickListener(view -> finish());
    }

    private Integer readScore(EditText view) {
        view.setError(null);
        try {
            return ScoreInput.parse(view.getText().toString());
        } catch (IllegalArgumentException invalid) {
            view.setError(getString(R.string.invalid_score));
            return null;
        }
    }

    private void saveScores() {
        Integer scoreA = readScore(scoreAView);
        Integer scoreB = readScore(scoreBView);
        if (scoreA == null || scoreB == null) {
            (scoreA == null ? scoreAView : scoreBView).requestFocus();
            return;
        }
        Intent result = new Intent();
        result.putExtra(ScoreStore.KEY_TEAM_A, scoreA.intValue());
        result.putExtra(ScoreStore.KEY_TEAM_B, scoreB.intValue());
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
