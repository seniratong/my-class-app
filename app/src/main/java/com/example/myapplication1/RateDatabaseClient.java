package com.example.myapplication1;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Local HTTP API; MySQL credentials exist only on the host computer. */
final class RateDatabaseClient {
    static final String BASE_URL = "http://127.0.0.1:8787";

    static boolean save(WebRates rates) {
        HttpURLConnection connection = null;
        try {
            JSONArray rows = new JSONArray();
            for (String row : rates.rows) {
                int separator = row.indexOf(" => ");
                if (separator <= 0) throw new IOException("Invalid currency row");
                rows.put(new JSONObject().put("currency_name", row.substring(0, separator))
                        .put("quote", row.substring(separator + 4)));
            }
            JSONObject snapshot = new JSONObject().put("update_date", RateCache.today())
                    .put("source_url", WebRates.URL).put("rows", rows);
            byte[] body = snapshot.toString().getBytes(StandardCharsets.UTF_8);
            // adb reverse is a local connection and must bypass emulator HTTP proxies.
            connection = (HttpURLConnection) new URL(BASE_URL + "/api/rates").openConnection(java.net.Proxy.NO_PROXY);
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(body.length);
            connection.setDoOutput(true);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(body); }
            if (connection.getResponseCode() != 200)
                throw new IOException("Database API rejected snapshot: HTTP " + connection.getResponseCode());
            JSONObject receipt;
            try (java.io.InputStream input = connection.getInputStream()) {
                receipt = new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
            return receipt.optBoolean("saved") && receipt.optInt("count") == rates.rows.size()
                    && snapshot.getString("update_date").equals(receipt.optString("update_date"));
        } catch (IOException | JSONException error) {
            Log.w("RateDatabase", "MySQL snapshot was not confirmed", error);
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
