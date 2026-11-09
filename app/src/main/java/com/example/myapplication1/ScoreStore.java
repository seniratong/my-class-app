package com.example.myapplication1;

import android.content.Context;
import android.content.SharedPreferences;

final class ScoreStore {
    static final String KEY_TEAM_A = "score_team_a";
    static final String KEY_TEAM_B = "score_team_b";
    private final SharedPreferences preferences;

    ScoreStore(Context context) {
        preferences = context.getSharedPreferences("scoreboard", Context.MODE_PRIVATE);
    }

    int getTeamA() {
        return Math.max(0, preferences.getInt(KEY_TEAM_A, 0));
    }

    int getTeamB() {
        return Math.max(0, preferences.getInt(KEY_TEAM_B, 0));
    }

    void save(int scoreA, int scoreB) {
        if (scoreA < 0 || scoreB < 0) {
            throw new IllegalArgumentException("Scores cannot be negative");
        }
        preferences.edit()
                .putInt(KEY_TEAM_A, scoreA)
                .putInt(KEY_TEAM_B, scoreB)
                .apply();
    }
}
