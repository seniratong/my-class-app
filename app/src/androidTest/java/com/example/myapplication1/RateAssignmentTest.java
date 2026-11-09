package com.example.myapplication1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RateAssignmentTest {
    @After
    public void clearExampleRates() {
        InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences(RateActivity.PREFERENCES, 0).edit().clear().commit();
    }

    @Before public void seedTodayDefaults() {
        clearExampleRates();
        // These interaction tests use local defaults; network behavior is tested separately.
        InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences(RateActivity.PREFERENCES, 0).edit()
                .putFloat(RateActivity.KEY_DOLLAR, 0.14f).putFloat(RateActivity.KEY_EURO, 0.13f)
                .putFloat(RateActivity.KEY_WON, 190f).putString(RateCache.KEY_DATE, RateCache.today())
                .putString(RateCache.KEY_ROWS, "美元 => 714.2857\n欧元 => 769.2308\n韩国元 => 0.526316").commit();
    }

    private void enter(int id, String text) {
        onView(withId(id)).perform(scrollTo(), replaceText(text), closeSoftKeyboard());
    }

    private void results(String dollar, String euro, String won) {
        onView(withId(R.id.rate_dollar_result)).check(matches(withText("美元（USD）：" + dollar)));
        onView(withId(R.id.rate_euro_result)).check(matches(withText("欧元（EUR）：" + euro)));
        onView(withId(R.id.rate_won_result)).check(matches(withText("韩元（KRW）：" + won)));
    }

    @Test
    public void launcherUsesSeparateTaskFromCounter() {
        try (ActivityScenario<MainActivity> counter = ActivityScenario.launch(MainActivity.class)) {
            int[] counterTask = {0};
            counter.onActivity(activity -> counterTask[0] = activity.getTaskId());
            Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    RateActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try (ActivityScenario<RateActivity> rates = ActivityScenario.launch(intent)) {
                rates.onActivity(activity -> assertNotEquals(counterTask[0], activity.getTaskId()));
            }
            counter.onActivity(activity -> assertEquals(MainActivity.class, activity.getClass()));
        }
    }

    @Test
    public void assignmentWorkflowPassesRatesReturnsNewRatesAndCapturesEvidence() throws IOException {
        try (ActivityScenario<RateActivity> scenario = ActivityScenario.launch(RateActivity.class)) {
            capture("01-layout-conversion.png");
            enter(R.id.rate_amount, "100");
            onView(withId(R.id.rate_convert_button)).perform(scrollTo(), click());
            results("14.00", "13.00", "19000.00");
            capture("03-runtime-before.png");
            onView(withId(R.id.rate_edit_button)).perform(scrollTo(), click());
            onView(withId(R.id.edit_dollar_rate)).check(matches(withText("0.14")));
            onView(withId(R.id.edit_euro_rate)).check(matches(withText("0.13")));
            onView(withId(R.id.edit_won_rate)).check(matches(withText("190.0")));
            capture("02-layout-editor.png");
            enter(R.id.edit_dollar_rate, "0.2");
            enter(R.id.edit_euro_rate, "0.15");
            enter(R.id.edit_won_rate, "200");
            capture("04-runtime-edit.png");
            onView(withId(R.id.save_rates)).perform(scrollTo(), click());
            results("20.00", "15.00", "20000.00");
            onView(withId(R.id.rate_amount)).perform(scrollTo());
            capture("05-runtime-after.png");
            scenario.recreate();
            onView(withId(R.id.rate_amount)).check(matches(withText("100")));
            results("20.00", "15.00", "20000.00");
        }
        try (ActivityScenario<RateActivity> scenario = ActivityScenario.launch(RateActivity.class)) {
            enter(R.id.rate_amount, "100");
            onView(withId(R.id.rate_convert_button)).perform(scrollTo(), click());
            results("20.00", "15.00", "20000.00");
            onView(withId(R.id.menu_edit_rates)).perform(click());
            onView(withId(R.id.edit_dollar_rate)).check(matches(withText("0.2")));
            onView(withId(R.id.cancel_rates)).perform(scrollTo(), click());
        }
    }

    @Test
    public void cancelAndBackDoNotChangeRates() {
        try (ActivityScenario<RateActivity> scenario = ActivityScenario.launch(RateActivity.class)) {
            enter(R.id.rate_amount, "100");
            onView(withId(R.id.rate_edit_button)).perform(scrollTo(), click());
            enter(R.id.edit_dollar_rate, "0.99");
            onView(withId(R.id.cancel_rates)).perform(scrollTo(), click());
            results("14.00", "13.00", "19000.00");
            onView(withId(R.id.menu_edit_rates)).perform(click());
            enter(R.id.edit_euro_rate, "0.99");
            pressBack();
            results("14.00", "13.00", "19000.00");
        }
    }

    @Test
    public void editorPreservesDraftAndRejectsEmptyAndZeroRates() {
        try (ActivityScenario<RateSettingsActivity> scenario = ActivityScenario.launch(RateSettingsActivity.class)) {
            enter(R.id.edit_dollar_rate, "0.27");
            enter(R.id.edit_euro_rate, "");
            scenario.recreate();
            onView(withId(R.id.edit_dollar_rate)).check(matches(withText("0.27")));
            onView(withId(R.id.edit_euro_rate)).check(matches(withText("")));
            onView(withId(R.id.save_rates)).perform(scrollTo(), click());
            onView(withId(R.id.edit_euro_rate)).check(matches(hasErrorText("请输入有效且大于 0 的汇率")));
            enter(R.id.edit_euro_rate, "0");
            onView(withId(R.id.save_rates)).perform(scrollTo(), click());
            onView(withId(R.id.edit_euro_rate)).check(matches(hasErrorText("请输入有效且大于 0 的汇率")));
        }
    }

    @Test
    public void amountValidationAndZeroConversion() {
        try (ActivityScenario<RateActivity> scenario = ActivityScenario.launch(RateActivity.class)) {
            onView(withId(R.id.rate_convert_button)).perform(scrollTo(), click());
            onView(withId(R.id.rate_amount)).check(matches(hasErrorText("请输入有效且不小于 0 的人民币金额")));
            enter(R.id.rate_amount, "0");
            onView(withId(R.id.rate_convert_button)).perform(scrollTo(), click());
            results("0.00", "0.00", "0.00");
        }
    }

    private void capture(String filename) throws IOException {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(250); // Let IME hide animation finish before recording evidence.
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir("rate-assignment");
        assertNotNull(directory);
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, filename))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
    }
}
