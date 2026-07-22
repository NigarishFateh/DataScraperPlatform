package com.datascraper.news.support;

import com.datascraper.news.config.NewsScraperProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NewsRssFetcher {

    private final NewsScraperProperties properties;

    public NewsRssFetcher(NewsScraperProperties properties) {
        this.properties = properties;
    }

    public List<Map<String, Object>> fetchHeadlines(String companyName) throws IOException {
        String query = URLEncoder.encode(companyName, StandardCharsets.UTF_8);
        String feedUrl = properties.getRssBaseUrl()
                + "?q=" + query
                + "&hl=en-EU&gl=EU&ceid=EU:en";

        Document feed = Jsoup.connect(feedUrl)
                .userAgent(properties.getUserAgent())
                .timeout(properties.getTimeoutMs())
                .get();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Element item : feed.select("item")) {
            String title = item.selectFirst("title") != null ? item.selectFirst("title").text().trim() : "";
            String link = item.selectFirst("link") != null ? item.selectFirst("link").text().trim() : "";
            String published = item.selectFirst("pubDate") != null ? item.selectFirst("pubDate").text().trim() : "";
            if (title.isBlank()) {
                continue;
            }

            Map<String, Object> headline = new LinkedHashMap<>();
            headline.put("section", "presence");
            headline.put("field", "news-headline");
            headline.put("title", title);
            headline.put("url", link);
            headline.put("publishedAt", published);
            headline.put("source", "google-news-rss");
            items.add(headline);

            if (items.size() >= properties.getMaxItems()) {
                break;
            }
        }
        return items;
    }
}
