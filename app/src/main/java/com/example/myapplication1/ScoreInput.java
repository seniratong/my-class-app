package com.example.myapplication1;

final class ScoreInput {
    private ScoreInput() { }

    static int parse(String text) {
        int score = Integer.parseInt(text.trim());
        if (score < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }
        return score;
    }
}
