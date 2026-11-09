package com.example.myapplication1;

import org.junit.Test;
import static org.junit.Assert.*;

public class WebRatesTest {
    @Test public void includesOtherCurrenciesInSourceOrderWithOriginalQuoteUnits() throws Exception {
        WebRates rates = WebRates.parse(html("700", "800", "0.5").replace("</table>",
                row("英镑", "900") + row("无报价币种", "--") + "</table>"));
        assertEquals(java.util.Arrays.asList("韩国元 => 0.5", "欧元 => 800", "美元 => 700", "英镑 => 900"), rates.rows);
    }

    private String row(String name, String price) {
        return "<tr><td>" + name + "</td><td>1</td><td>" + price + "</td></tr>";
    }
    private String html(String usd, String eur, String krw) {
        return "<table><tr><th>货币名称</th><th>现汇买入价</th><th>现汇卖出价</th></tr>"
                + row("韩国元", krw) + row("欧元", eur) + row("美元", usd) + "</table>";
    }
    @Test public void findsNamedColumnAndCurrenciesAndConvertsPerHundred() throws Exception {
        WebRates rates = WebRates.parse("<table><tr><td>导航</td></tr></table>" + html("700", "800", "0.5"));
        assertEquals(100f / 700, rates.dollar, 0.000001f);
        assertEquals(0.125f, rates.euro, 0.000001f);
        assertEquals(200f, rates.won, 0.001f);
    }
    @Test public void rejectsMissingCurrency() {
        assertThrows(java.io.IOException.class, () -> WebRates.parse(html("700", "800", "0.5").replace("美元", "英镑")));
    }
    @Test public void rejectsInvalidPrices() {
        for (String invalid : new String[]{"0", "-1", "NaN", "Infinity", "--", ""}) {
            assertThrows(java.io.IOException.class, () -> WebRates.parse(html(invalid, "800", "0.5")));
        }
    }
    @Test public void rejectsChangedPage() {
        assertThrows(java.io.IOException.class, () -> WebRates.parse("<html>系统维护</html>"));
    }
    @Test public void parsesActualChlTableWithLinksAndIgnoresOtherQuoteTable() throws Exception {
        try (java.io.InputStream input = getClass().getResourceAsStream("/chl-rates.html")) {
            assertNotNull(input);
            WebRates rates = WebRates.parse(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(100f / 666.49f, rates.dollar, 0.000001f);
            assertEquals(100f / 747.92f, rates.euro, 0.000001f);
            assertEquals(100f / 0.5017f, rates.won, 0.001f);
            assertEquals("https://chl.cn/huilv/?jinri", WebRates.URL);
        }
    }

}
