package com.example.myapplication1;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String STATE_TEAM_A = "score_team_a";
    private static final String STATE_TEAM_B = "score_team_b";
    private int scoreA;
    private int scoreB;
    private TextView scoreAView;
    private TextView scoreBView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.main));

        scoreAView = findViewById(R.id.score_a);
        scoreBView = findViewById(R.id.score_b);
        if (savedInstanceState != null) {
            scoreA = savedInstanceState.getInt(STATE_TEAM_A, 0);
            scoreB = savedInstanceState.getInt(STATE_TEAM_B, 0);
        }
        showScores();

        findViewById(R.id.add_three_a).setOnClickListener(view -> addPoints(true, 3));
        findViewById(R.id.add_two_a).setOnClickListener(view -> addPoints(true, 2));
        findViewById(R.id.free_throw_a).setOnClickListener(view -> addPoints(true, 1));
        findViewById(R.id.add_three_b).setOnClickListener(view -> addPoints(false, 3));
        findViewById(R.id.add_two_b).setOnClickListener(view -> addPoints(false, 2));
        findViewById(R.id.free_throw_b).setOnClickListener(view -> addPoints(false, 1));
        findViewById(R.id.reset_button).setOnClickListener(view -> {
            scoreA = 0;
            scoreB = 0;
            showScores();
        });
    }

    private void addPoints(boolean teamA, int points) {
        if (teamA && scoreA <= Integer.MAX_VALUE - points) {
            scoreA += points;
        } else if (!teamA && scoreB <= Integer.MAX_VALUE - points) {
            scoreB += points;
        }
        showScores();
    }

    private void showScores() {
        scoreAView.setText(String.valueOf(scoreA));
        scoreBView.setText(String.valueOf(scoreB));
        scoreAView.setContentDescription(getString(R.string.team_score, getString(R.string.team_a), scoreA));
        scoreBView.setContentDescription(getString(R.string.team_score, getString(R.string.team_b), scoreB));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_TEAM_A, scoreA);
        outState.putInt(STATE_TEAM_B, scoreB);
        super.onSaveInstanceState(outState);
    }
}
