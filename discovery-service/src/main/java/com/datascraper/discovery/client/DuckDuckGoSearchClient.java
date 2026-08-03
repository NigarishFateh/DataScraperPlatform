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
    private static final int MAX_CITIES = 6;
    private static final int MAX_KEYWORDS = 2;
    private static final int MAX_QUERIES = 12;
    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "facebook.com", "instagram.com", "twitter.com", "x.com", "youtube.com",
            "wikipedia.org", "reddit.com", "tiktok.com", "duckduckgo.com",
            "github.com", "linkedin.com", "crunchbase.com"
    );

    public List<WebSearchHit> discover(ResolvedDiscoveryCriteria criteria) {
        List<WebSearchHit> hits = new ArrayList<>();
        List<CityQuery> queries = buildQueries(criteria);
        log.info("DuckDuckGo running {} queries for categories={}", queries.size(), criteria.categoryNames());
        for (CityQuery query : queries) {
            try {
                List<WebSearchHit> batch = search(query, criteria);
                log.info("DuckDuckGo '{}' -> {} links", query.text(), batch.size());
                hits.addAll(batch);
            } catch (Exception ex) {
                log.warn("DuckDuckGo search failed for '{}': {}", query.text(), ex.getMessage());
            }
            if (hits.size() >= criteria.maxResults()) {
                break;
            }
            sleepQuietly(500);
        }
        return hits;
    }

    private List<WebSearchHit> search(CityQuery query, ResolvedDiscoveryCriteria criteria) throws Exception {
        String url = "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query.text(), StandardCharsets.UTF_8);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .timeout(25_000)
                .followRedirects(true)
                .get();

        Elements links = doc.select("a.result__a");
        if (links.isEmpty()) {
            // Fallback selector used by some DDG HTML variants
            links = doc.select("a[href*=uddg], a.result-link");
        }
        List<WebSearchHit> hits = new ArrayList<>();
        for (Element link : links) {
            String title = cleanTitle(link.text());
            String href = unwrapDuckDuckGoUrl(link.attr("href"));
            if (href.isBlank()) {
                href = unwrapDuckDuckGoUrl(link.absUrl("href"));
            }
            if (title.isBlank() || href.isBlank() || isBlocked(href) || looksLikeListicle(title, href)) {
                continue;
            }
            hits.add(new WebSearchHit(
                    title,
                    href,
                    href,
                    firstCountry(criteria),
                    query.cityId(),
                    query.cityName(),
                    "duckduckgo"
            ));
        }
        return hits;
    }

    private static List<CityQuery> buildQueries(ResolvedDiscoveryCriteria criteria) {
        List<CityQuery> queries = new ArrayList<>();
        List<String> keywords = limit(criteria.searchKeywords(), MAX_KEYWORDS);
        List<String> countries = criteria.countryNames();
        String country = countries.isEmpty() ? "" : countries.get(0);

        List<CityGeo> geos = new ArrayList<>();
        int cityLimit = Math.min(criteria.cityNames().size(), MAX_CITIES);
        for (int i = 0; i < cityLimit; i++) {
            String cityName = criteria.cityNames().get(i);
            if (cityName == null || cityName.isBlank()) {
                continue;
            }
            String cityId = i < criteria.cityIds().size() ? criteria.cityIds().get(i) : null;
            geos.add(new CityGeo(cityId, cityName));
        }

        if (geos.isEmpty() && !country.isBlank()) {
            geos.add(new CityGeo(null, country));
        }

        Set<String> seen = new LinkedHashSet<>();
        for (CityGeo geo : geos) {
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                String cityPart = geo.cityName();
                String withCountry = country.isBlank() || country.equalsIgnoreCase(cityPart)
                        ? cityPart
                        : cityPart + " " + country;

                // Keep queries short and local: industry + city (+ country).
                addQuery(queries, seen, keyword + " " + withCountry, geo);
                addQuery(queries, seen, "\"" + keyword + "\" " + cityPart, geo);

                if (queries.size() >= MAX_QUERIES) {
                    return queries;
                }
            }
        }

        if (queries.isEmpty()) {
            for (String keyword : keywords) {
                addQuery(queries, seen, keyword, new CityGeo(null, null));
            }
        }
        return queries;
    }

    private static void addQuery(List<CityQuery> queries, Set<String> seen, String text, CityGeo geo) {
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank() || !seen.add(normalized.toLowerCase(Locale.ROOT))) {
            return;
        }
        if (queries.size() >= MAX_QUERIES) {
            return;
        }
        queries.add(new CityQuery(normalized, geo.cityId(), geo.cityName()));
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

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of("company");
        }
        return values.size() <= max ? values : values.subList(0, max);
    }

    private static String firstCountry(ResolvedDiscoveryCriteria criteria) {
        return criteria.countryCodes().isEmpty() ? null : criteria.countryCodes().get(0);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record CityGeo(String cityId, String cityName) {
    }

    private record CityQuery(String text, String cityId, String cityName) {
    }
}
