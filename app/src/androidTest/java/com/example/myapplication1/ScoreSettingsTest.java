package com.example.myapplication1;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class ScoreSettingsTest {
    @Before
    @After
    public void clearPersistedScores() {
        InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences("scoreboard", 0).edit().clear().commit();
    }

    private void enter(int id, String text) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
    }

    @Test
    public void menuPassesCurrentScoresAndLauncherReturnsAndPersistsChanges() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.add_three_a)).perform(scrollTo(), click());
            onView(withId(R.id.add_two_b)).perform(scrollTo(), click());
            onView(withId(R.id.menu_set)).perform(click());
            onView(withId(R.id.edit_score_a)).check(matches(withText("3")));
            onView(withId(R.id.edit_score_b)).check(matches(withText("2")));
            enter(R.id.edit_score_a, "21");
            enter(R.id.edit_score_b, "18");
            onView(withId(R.id.save_scores)).perform(scrollTo(), click());
            onView(withId(R.id.score_a)).check(matches(withText("21")));
            onView(withId(R.id.score_b)).check(matches(withText("18")));
            scenario.recreate();
            onView(withId(R.id.score_a)).check(matches(withText("21")));
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.score_a)).check(matches(withText("21")));
            onView(withId(R.id.score_b)).check(matches(withText("18")));
        }
    }

    @Test
    public void cancelAndSystemBackDoNotSaveEdits() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.menu_set)).perform(click());
            enter(R.id.edit_score_a, "99");
            onView(withId(R.id.cancel_scores)).perform(scrollTo(), click());
            onView(withId(R.id.score_a)).check(matches(withText("0")));
            onView(withId(R.id.menu_set)).perform(click());
            enter(R.id.edit_score_b, "99");
            pressBack();
            onView(withId(R.id.score_b)).check(matches(withText("0")));
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.score_a)).check(matches(withText("0")));
            onView(withId(R.id.score_b)).check(matches(withText("0")));
        }
    }

    @Test
    public void editorPreservesUnfinishedInputAndRejectsMissingAndOverflowScores() {
        try (ActivityScenario<ScoreSettingsActivity> scenario = ActivityScenario.launch(ScoreSettingsActivity.class)) {
            enter(R.id.edit_score_a, "12");
            enter(R.id.edit_score_b, "");
            scenario.recreate();
            onView(withId(R.id.edit_score_a)).check(matches(withText("12")));
            onView(withId(R.id.edit_score_b)).check(matches(withText("")));
            onView(withId(R.id.save_scores)).perform(scrollTo(), click());
            onView(withId(R.id.edit_score_b)).check(matches(hasErrorText("请输入 0～2147483647 的整数")));
            enter(R.id.edit_score_b, "2147483648");
            onView(withId(R.id.save_scores)).perform(scrollTo(), click());
            onView(withId(R.id.edit_score_b)).check(matches(hasErrorText("请输入 0～2147483647 的整数")));
        }
    }
}
