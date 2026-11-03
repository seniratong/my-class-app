package com.example.myapplication1;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

/** Quotes on the source page are CNY per 100 foreign currency units. */
final class WebRates {
    static final String URL = "https://chl.cn/huilv/?jinri";
    final float dollar, euro, won;

    private WebRates(float dollar, float euro, float won) {
        this.dollar = dollar;
        this.euro = euro;
        this.won = won;
    }

    static WebRates fetch() throws IOException {
        return parse(Jsoup.connect(URL).userAgent("Mozilla/5.0 (Android; RateClassAssignment)")
                .timeout(15000).maxBodySize(2 * 1024 * 1024).get().outerHtml());
    }

    static WebRates parse(String html) throws IOException {
        for (Element table : Jsoup.parse(html).select("table")) {
            int nameColumn = -1, priceColumn = -1;
            float dollar = 0, euro = 0, won = 0;
            for (Element row : table.select("tr")) {
                Elements cells = row.select("> th, > td");
                for (int i = 0; i < cells.size(); i++) {
                    String text = cells.get(i).text().trim();
                    if ("货币名称".equals(text)) nameColumn = i;
                    if ("现汇卖出价".equals(text)) priceColumn = i;
                }
                if (nameColumn < 0 || priceColumn < 0
                        || cells.size() <= Math.max(nameColumn, priceColumn)) continue;
                String name = cells.get(nameColumn).text().trim();
                if (!"美元".equals(name) && !"欧元".equals(name) && !"韩国元".equals(name)) continue;
                float rate;
                try {
                    float quote = Float.parseFloat(cells.get(priceColumn).text().trim());
                    rate = 100f / quote;
                    if (!Float.isFinite(quote) || quote <= 0 || !Float.isFinite(rate) || rate <= 0)
                        throw new NumberFormatException("Invalid quote");
                } catch (NumberFormatException invalid) {
                    throw new IOException("网页报价无效", invalid);
                }
                if ("美元".equals(name)) dollar = rate;
                if ("欧元".equals(name)) euro = rate;
                if ("韩国元".equals(name)) won = rate;
            }
            if (dollar > 0 && euro > 0 && won > 0) return new WebRates(dollar, euro, won);
        }
        throw new IOException("网页缺少美元、欧元、韩元或现汇卖出价列");
    }
}
