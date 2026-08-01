package com.datascraper.discovery.client;

import com.datascraper.discovery.dto.ResolvedDiscoveryCriteria;
import com.datascraper.discovery.dto.WebSearchHit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort DuckDuckGo HTML search. May be rate-limited; callers should treat as optional.
 */
@Component
public class DuckDuckGoSearchClient {

    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoSearchClient.class);
    private static final Pattern UDDG = Pattern.compile("uddg=([^&]+)");
    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "facebook.com", "instagram.com", "twitter.com", "x.com", "youtube.com",
            "wikipedia.org", "reddit.com", "tiktok.com", "duckduckgo.com"
    );

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        List<WebSearchHit> hits = new ArrayList<>();
        for (String query : buildQueries(criteria)) {
            try {
                hits.addAll(search(query, criteria));
            } catch (Exception ex) {
                log.warn("DuckDuckGo search failed for '{}': {}", query, ex.getMessage());
            }
            if (hits.size() >= criteria.maxResults()) {
                break;
            }
            sleepQuietly(800);
        }
        return hits;
    }

    private List<WebSearchHit> search(String query, ResolvedDiscoveryCriteria criteria) throws Exception {
        String url = "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .timeout(25_000)
                .followRedirects(true)
                .get();

        Elements links = doc.select("a.result__a");
        List<WebSearchHit> hits = new ArrayList<>();
        for (Element link : links) {
            String title = cleanTitle(link.text());
            String href = unwrapDuckDuckGoUrl(link.attr("href"));
            if (title.isBlank() || href.isBlank() || isBlocked(href) || looksLikeListicle(title, href)) {
                continue;
            }
            hits.add(new WebSearchHit(
                    title,
                    href,
                    href,
                    firstCountry(criteria),
                    firstCityId(criteria),
                    firstCityName(criteria),
                    "duckduckgo"
            ));
        }
        return hits;
    }

    private static List<String> buildQueries(ResolvedDiscoveryCriteria criteria) {
        Set<String> queries = new LinkedHashSet<>();
        String geo = String.join(" ", concat(criteria.cityNames(), criteria.countryNames()));
        for (String keyword : limit(criteria.searchKeywords(), 3)) {
            if (!geo.isBlank()) {
                queries.add(keyword + " company " + geo);
                queries.add(keyword + " software house " + geo);
            } else {
                queries.add(keyword + " company");
            }
        }
        return List.copyOf(queries);
    }

    private static String unwrapDuckDuckGoUrl(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        Matcher matcher = UDDG.matcher(href);
        if (matcher.find()) {
            return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (href.startsWith("//")) {
            return "https:" + href;
        }
        return "";
    }

    private static boolean looksLikeListicle(String title, String url) {
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        return lowerTitle.contains("top ")
                || lowerTitle.contains("best ")
                || lowerTitle.contains("list of")
                || lowerUrl.contains("clutch.co")
                || lowerUrl.contains("f6s.com")
                || lowerUrl.contains("designrush.com")
                || lowerUrl.contains("techbehemoths.com")
                || lowerUrl.contains("ensun.io")
                || lowerUrl.contains("goodfirms.co");
    }

    private static boolean isBlocked(String url) {
        try {
            String host = java.net.URI.create(url).getHost();
            if (host == null) {
                return true;
            }
            String normalized = host.toLowerCase(Locale.ROOT).replace("www.", "");
            return BLOCKED_HOSTS.stream().anyMatch(normalized::endsWith);
        } catch (Exception ex) {
            return true;
        }
    }

    private static String cleanTitle(String title) {
        if (title == null) {
            return "";
        }
        String cleaned = title.replace('\u00a0', ' ').trim();
        int dash = cleaned.indexOf(" - ");
        if (dash > 8) {
            cleaned = cleaned.substring(0, dash).trim();
        }
        int pipe = cleaned.indexOf(" | ");
        if (pipe > 8) {
            cleaned = cleaned.substring(0, pipe).trim();
        }
        return cleaned;
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> values = new ArrayList<>();
        if (a != null) {
            values.addAll(a);
        }
        if (b != null) {
            values.addAll(b);
        }
        return values;
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of("company");
        }
        return values.size() <= max ? values : values.subList(0, max);
    }

    private static String firstCountry(ResolvedDiscoveryCriteria criteria) {
        return criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
    }

    private static String firstCityId(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityIds().isEmpty() ? null : criteria.cityIds().get(0);
    }

    private static String firstCityName(ResolvedDiscoveryCriteria criteria) {
        return criteria.cityNames().isEmpty() ? null : criteria.cityNames().get(0);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
