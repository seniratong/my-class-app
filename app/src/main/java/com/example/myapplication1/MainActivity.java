package com.example.myapplication1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private ScoreStore scoreStore;
    private final ActivityResultLauncher<Intent> scoreSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() != RESULT_OK || data == null
                        || !data.hasExtra(ScoreStore.KEY_TEAM_A)
                        || !data.hasExtra(ScoreStore.KEY_TEAM_B)) {
                    return;
                }
                int updatedA = data.getIntExtra(ScoreStore.KEY_TEAM_A, -1);
                int updatedB = data.getIntExtra(ScoreStore.KEY_TEAM_B, -1);
                if (updatedA < 0 || updatedB < 0) {
                    return;
                }
                scoreA = updatedA;
                scoreB = updatedB;
                saveAndShowScores();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.title));
        scoreStore = new ScoreStore(this);
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
        } else {
            scoreA = scoreStore.getTeamA();
            scoreB = scoreStore.getTeamB();
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
            saveAndShowScores();
        });
    }

    private void addPoints(boolean teamA, int points) {
        if (teamA && scoreA <= Integer.MAX_VALUE - points) {
            scoreA += points;
        } else if (!teamA && scoreB <= Integer.MAX_VALUE - points) {
            scoreB += points;
        }
        saveAndShowScores();
    }

    private void saveAndShowScores() {
        scoreStore.save(scoreA, scoreB);
        showScores();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scoreboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_set) {
            Intent intent = new Intent(this, ScoreSettingsActivity.class);
            Bundle scores = new Bundle();
            scores.putInt(ScoreStore.KEY_TEAM_A, scoreA);
            scores.putInt(ScoreStore.KEY_TEAM_B, scoreB);
            intent.putExtras(scores);
            scoreSettingsLauncher.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showScores() {
        scoreAView.setText(String.valueOf(scoreA));
        scoreBView.setText(String.valueOf(scoreB));
        scoreAView.setContentDescription(getString(R.string.team_score, getString(R.string.team_a), scoreA));
        scoreBView.setContentDescription(getString(R.string.team_score, getString(R.string.team_b), scoreB));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        scoreA = savedInstanceState.getInt(STATE_TEAM_A, 0);
        scoreB = savedInstanceState.getInt(STATE_TEAM_B, 0);
        showScores();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_TEAM_A, scoreA);
        outState.putInt(STATE_TEAM_B, scoreB);
        super.onSaveInstanceState(outState);
    }
}
