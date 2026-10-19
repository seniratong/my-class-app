package com.example.myapplication1;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class ScoreboardTest {
    @Test
    public void startsWithTwoZeroScores() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.score_a)).check(matches(withText("0")));
            onView(withId(R.id.score_b)).check(matches(withText("0")));
        }
    }

    @Test
    public void scoresAreIndependentAndAllButtonsAddCorrectPoints() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.add_three_a)).perform(scrollTo(), click());
            onView(withId(R.id.add_two_a)).perform(scrollTo(), click());
            onView(withId(R.id.free_throw_a)).perform(scrollTo(), click());
            onView(withId(R.id.score_a)).check(matches(withText("6")));
            onView(withId(R.id.score_b)).check(matches(withText("0")));
            onView(withId(R.id.add_three_b)).perform(scrollTo(), click());
            onView(withId(R.id.add_two_b)).perform(scrollTo(), click());
            onView(withId(R.id.free_throw_b)).perform(scrollTo(), click());
            onView(withId(R.id.score_b)).check(matches(withText("6")));
            onView(withId(R.id.score_a)).check(matches(withText("6")));
        }
    }

    @Test
    public void resetClearsBothTeamsAndScoringCanContinue() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.add_three_a)).perform(scrollTo(), click());
            onView(withId(R.id.add_two_b)).perform(scrollTo(), click());
            onView(withId(R.id.reset_button)).perform(scrollTo(), click());
            onView(withId(R.id.score_a)).check(matches(withText("0")));
            onView(withId(R.id.score_b)).check(matches(withText("0")));
            onView(withId(R.id.free_throw_b)).perform(scrollTo(), click());
            onView(withId(R.id.score_b)).check(matches(withText("1")));
        }
    }

    @Test
    public void bothScoresSurviveRecreation() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.add_three_a)).perform(scrollTo(), click());
            onView(withId(R.id.add_two_b)).perform(scrollTo(), click());
            scenario.recreate();
            onView(withId(R.id.score_a)).check(matches(withText("3")));
            onView(withId(R.id.score_b)).check(matches(withText("2")));
        }
    }
}
