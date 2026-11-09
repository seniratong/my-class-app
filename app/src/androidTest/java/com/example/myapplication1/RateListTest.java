package com.example.myapplication1;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class RateListTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final SharedPreferences prefs = context.getSharedPreferences(RateActivity.PREFERENCES, 0);
    private final String html = "<table><tr><th>货币名称</th><th>现汇卖出价</th></tr>"
            + "<tr><td>美元</td><td>700</td></tr><tr><td>欧元</td><td>800</td></tr>"
            + "<tr><td>韩国元</td><td>0.5</td></tr><tr><td>英镑</td><td>900</td></tr></table>";

    @Before @After public void clear() { prefs.edit().clear().commit(); }

    @Test public void cacheRequiresTodayAndCompleteData() throws Exception {
        assertTrue(RateCache.needsUpdate(prefs));
        RateCache.save(context, WebRates.parse(html));
        assertFalse(RateCache.needsUpdate(prefs));
        assertEquals(RateCache.today(), prefs.getString(RateCache.KEY_DATE, ""));
        assertEquals(4, RateCache.rows(prefs).size());
        assertEquals(0.125f, prefs.getFloat(RateActivity.KEY_EURO, 0), 0);
        prefs.edit().putString(RateCache.KEY_DATE, "2020-01-01").commit();
        assertTrue(RateCache.needsUpdate(prefs));
        prefs.edit().putString(RateCache.KEY_DATE, RateCache.today()).remove(RateCache.KEY_ROWS).commit();
        assertTrue(RateCache.needsUpdate(prefs));
    }

    @Test public void listDisplaysTodayCacheWithoutStartingNetworkAndSurvivesRecreation() throws Exception {
        RateCache.save(context, WebRates.parse(html));
        try (ActivityScenario<RateListActivity> scenario = ActivityScenario.launch(RateListActivity.class)) {
            scenario.onActivity(activity -> {
                assertEquals(4, activity.getListAdapter().getCount());
                assertEquals("美元 => 700", activity.getListAdapter().getItem(0));
                assertTrue(activity.findViewById(R.id.rate_list_refresh).isEnabled());
                assertTrue(((TextView) activity.findViewById(R.id.rate_list_status)).getText().toString().contains("缓存"));
            });
            scenario.recreate();
            scenario.onActivity(activity -> assertEquals(4, activity.getListAdapter().getCount()));
        }
    }

    @Test public void converterUsesTodayCacheAndListEntryReturns() throws Exception {
        RateCache.save(context, WebRates.parse(html));
        try (ActivityScenario<RateActivity> scenario = ActivityScenario.launch(RateActivity.class)) {
            scenario.onActivity(activity -> {
                assertTrue(activity.findViewById(R.id.rate_fetch_button).isEnabled());
                assertTrue(((TextView) activity.findViewById(R.id.rate_current_values)).getText().toString().contains("0.125"));
            });
            androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu(context);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.rate_list_title))
                    .perform(androidx.test.espresso.action.ViewActions.click());
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.rate_list_status))
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
            scenario.onActivity(activity -> assertTrue(activity.findViewById(R.id.rate_fetch_button).isEnabled()));
        }
    }

    @Test public void realNetworkPopulatesListAndThenUsesDailyCache() throws Exception {
        String networkMode = InstrumentationRegistry.getArguments().getString("listNetwork");
        org.junit.Assume.assumeTrue("online".equals(networkMode) || "mysql".equals(networkMode));
        try (ActivityScenario<RateListActivity> scenario = ActivityScenario.launch(RateListActivity.class)) {
            awaitRequest(scenario);
            scenario.onActivity(activity -> {
                assertTrue("Expected all network currencies", activity.getListAdapter().getCount() > 3);
                assertFalse(RateCache.needsUpdate(prefs));
                assertTrue(((TextView) activity.findViewById(R.id.rate_list_status)).getText().toString().contains("获取时间"));
            });
            capture("01-network-list.png");
            scenario.recreate();
            scenario.onActivity(activity -> assertTrue(((TextView) activity.findViewById(R.id.rate_list_status))
                    .getText().toString().contains("缓存")));
            capture("02-daily-cache.png");
        }
    }

    @Test public void failedCrossDayUpdatePreservesCacheAndOldDate() throws Exception {
        org.junit.Assume.assumeTrue("offline".equals(InstrumentationRegistry.getArguments().getString("listNetwork")));
        RateCache.save(context, WebRates.parse(html));
        prefs.edit().putString(RateCache.KEY_DATE, "2020-01-01").commit();
        try (ActivityScenario<RateListActivity> scenario = ActivityScenario.launch(RateListActivity.class)) {
            awaitRequest(scenario);
            scenario.onActivity(activity -> {
                assertEquals(4, activity.getListAdapter().getCount());
                assertTrue(((TextView) activity.findViewById(R.id.rate_list_status)).getText().toString().contains("获取失败"));
            });
            assertEquals("2020-01-01", prefs.getString(RateCache.KEY_DATE, ""));
            assertEquals(0.125f, prefs.getFloat(RateActivity.KEY_EURO, 0), 0);
            assertTrue(RateCache.needsUpdate(prefs));
            capture("03-offline-cache-fixture.png");
        }
    }

    @Test public void realPhoneSnapshotMatchesMySqlQuery() throws Exception {
        org.junit.Assume.assumeTrue("mysql".equals(InstrumentationRegistry.getArguments().getString("listNetwork")));
        try (ActivityScenario<RateListActivity> scenario = ActivityScenario.launch(RateListActivity.class)) {
            awaitRequest(scenario);
            assertTrue("MySQL save must be confirmed", prefs.getBoolean(RateCache.KEY_DATABASE_SYNCED, false));
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection)
                    new java.net.URL(RateDatabaseClient.BASE_URL + "/api/rates").openConnection(java.net.Proxy.NO_PROXY);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            org.json.JSONObject data;
            try (java.io.InputStream input = connection.getInputStream()) {
                data = new org.json.JSONObject(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            } finally { connection.disconnect(); }
            assertEquals(RateCache.today(), data.getString("update_date"));
            org.json.JSONArray rows = data.getJSONArray("rows");
            java.util.List<String> cached = RateCache.rows(prefs);
            assertTrue(rows.length() > 3);
            assertEquals(cached.size(), rows.length());
            for (int i = 0; i < cached.size(); i++) {
                String[] pair = cached.get(i).split(" => ", 2);
                assertEquals(pair[0], rows.getJSONObject(i).getString("currency_name"));
                assertEquals(0, new java.math.BigDecimal(pair[1])
                        .compareTo(new java.math.BigDecimal(rows.getJSONObject(i).getString("quote"))));
            }
            capture("04-mysql-saved.png");
        }
    }

    private void awaitRequest(ActivityScenario<RateListActivity> scenario) {
        long deadline = android.os.SystemClock.elapsedRealtime() + 25000;
        boolean[] finished = {false};
        while (!finished[0] && android.os.SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> finished[0] = activity.findViewById(R.id.rate_list_refresh).isEnabled());
            android.os.SystemClock.sleep(100);
        }
        assertTrue("Request timed out", finished[0]);
    }

    private void capture(String name) throws Exception {
        android.os.SystemClock.sleep(300);
        android.graphics.Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        java.io.File directory = context.getExternalFilesDir("rate-list");
        assertNotNull(directory);
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(new java.io.File(directory, name))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output));
        }
        bitmap.recycle();
    }
}
