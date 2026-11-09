package com.example.myapplication1;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ScoreboardTest {
    @Before
    @After
    public void clearPersistedScores() {
        InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("scoreboard", 0).edit().clear().commit();
    }

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

    @Test
    public void scoresSurviveClosingAndRelaunchingAndResetIsPersistent() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.add_three_a)).perform(scrollTo(), click());
            onView(withId(R.id.add_two_b)).perform(scrollTo(), click());
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.score_a)).check(matches(withText("3")));
            onView(withId(R.id.score_b)).check(matches(withText("2")));
            onView(withId(R.id.reset_button)).perform(scrollTo(), click());
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.score_a)).check(matches(withText("0")));
            onView(withId(R.id.score_b)).check(matches(withText("0")));
        }
    }

    @Test
    public void scoresSurvivePortraitLandscapeAndPortraitAgain() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.add_three_a).performClick();
                activity.findViewById(R.id.add_two_b).performClick();
            });
            assertOrientationAndScores(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                    Configuration.ORIENTATION_LANDSCAPE);
            assertOrientationAndScores(scenario, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    Configuration.ORIENTATION_PORTRAIT);
        }
    }

    private void assertOrientationAndScores(ActivityScenario<MainActivity> scenario,
                                            int requested, int expected) {
        scenario.onActivity(activity -> activity.setRequestedOrientation(requested));
        long deadline = SystemClock.uptimeMillis() + 5000;
        int[] orientation = {0};
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> orientation[0] = activity.getResources().getConfiguration().orientation);
            if (orientation[0] == expected) {
                break;
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        assertEquals(expected, orientation[0]);
        scenario.recreate();
        onView(withId(R.id.score_a)).check(matches(withText("3")));
        onView(withId(R.id.score_b)).check(matches(withText("2")));
    }
}
