package com.example.myapplication1;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NetworkRateTest {
    private void capture(String filename) throws Exception {
        onView(withId(R.id.rate_network_status)).perform(scrollTo());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(300);
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("network-rates");
        assertNotNull(dir);
        assertTrue(dir.isDirectory() || dir.mkdirs());
        try (FileOutputStream stream = new FileOutputStream(new File(dir, filename))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
        }
        image.recycle();
    }

    @Test public void realWebFetchUpdatesConversionAndSurvivesRecreation() throws Exception {
        org.junit.Assume.assumeFalse("offline".equals(InstrumentationRegistry.getArguments().getString("networkMode")));
        try (ActivityScenario<RateActivity> scenario = ActivityScenario.launch(RateActivity.class)) {
            onView(withId(R.id.rate_amount)).perform(scrollTo(), replaceText("100"), closeSoftKeyboard());
            onView(withId(R.id.rate_fetch_button)).perform(scrollTo());
            capture("01-before.png");
            onView(withId(R.id.rate_fetch_button)).perform(click());
            long deadline = SystemClock.elapsedRealtime() + 25000;
            boolean[] finished = {false};
            while (!finished[0] && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> finished[0] = activity.findViewById(R.id.rate_fetch_button).isEnabled());
                SystemClock.sleep(100);
            }
            assertTrue("Network request did not finish", finished[0]);
            String[] status = {""};
            float[] rates = new float[3];
            scenario.onActivity(activity -> {
                status[0] = ((TextView) activity.findViewById(R.id.rate_network_status)).getText().toString();
                android.content.SharedPreferences prefs = activity.getSharedPreferences(RateActivity.PREFERENCES, 0);
                rates[0] = prefs.getFloat(RateActivity.KEY_DOLLAR, 0);
                rates[1] = prefs.getFloat(RateActivity.KEY_EURO, 0);
                rates[2] = prefs.getFloat(RateActivity.KEY_WON, 0);
                assertTrue(rates[0] > 0 && rates[1] > 0 && rates[2] > 0);
                assertEquals("美元（USD）：" + RateMath.convert("100", rates[0]),
                        ((TextView) activity.findViewById(R.id.rate_dollar_result)).getText().toString());
            });
            assertTrue("Expected real chl.cn response: " + status[0], status[0].contains("获取时间"));
            capture("02-network-success.png");
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals(status[0],
                    ((TextView) activity.findViewById(R.id.rate_network_status)).getText().toString()));
            onView(withId(R.id.rate_fetch_button)).perform(scrollTo());
            capture("03-restored.png");
        }
    }
    @Test public void offlineRequestKeepsPreviousRates() throws Exception {
        // Run explicitly with the emulator network disabled; not part of the normal suite.
        org.junit.Assume.assumeTrue("offline".equals(InstrumentationRegistry.getArguments().getString("networkMode")));
        android.content.SharedPreferences prefs = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getSharedPreferences(RateActivity.PREFERENCES, 0);
        float previous = prefs.getFloat(RateActivity.KEY_DOLLAR, 0.14f);
        try (ActivityScenario<RateActivity> scenario = ActivityScenario.launch(RateActivity.class)) {
            onView(withId(R.id.rate_fetch_button)).perform(scrollTo(), click());
            long deadline = SystemClock.elapsedRealtime() + 25000;
            boolean[] finished = {false};
            while (!finished[0] && SystemClock.elapsedRealtime() < deadline) {
                scenario.onActivity(activity -> finished[0] = activity.findViewById(R.id.rate_fetch_button).isEnabled());
                SystemClock.sleep(100);
            }
            assertTrue(finished[0]);
            scenario.onActivity(activity -> assertEquals(activity.getString(R.string.rate_fetch_error),
                    ((TextView) activity.findViewById(R.id.rate_network_status)).getText().toString()));
            assertEquals(previous, prefs.getFloat(RateActivity.KEY_DOLLAR, 0.14f), 0f);
            capture("04-offline-preserved.png");
        }
    }

}
