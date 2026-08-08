package com.datascraper.discovery.client;

import com.datascraper.discovery.support.NlRestaurantBrandSeed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Free leadership lookup (no paid people API): Wikidata → Wikipedia → DuckDuckGo.
 * Used only by the isolated NL restaurant leadership API.
 */
@Component
public class OpenLeadershipClient {

    private static final Logger log = LoggerFactory.getLogger(OpenLeadershipClient.class);
    private static final String WD_API = "https://www.wikidata.org/w/api.php";
    private static final Map<String, LeadershipProp> LEADERSHIP_PROPS = Map.of(
            "P169", new LeadershipProp("CEO", 100),
            "P112", new LeadershipProp("Founder", 85),
            "P1037", new LeadershipProp("Director", 70),
            "P488", new LeadershipProp("Chairperson", 60)
    );

    private static final Pattern KEY_PEOPLE = Pattern.compile(
            "(?i)(ceo|chief executive|founder|co-founder|owner|managing director|directeur|algemeen directeur|"
                    + "eigenaar|chairman|president|manager)\\b[^\\n]{0,40}?"
                    + "([A-ZÀ-ÖØ-Þ][A-Za-zÀ-öø-ÿ''\\-]+(?:\\s+[A-ZÀ-ÖØ-Þ][A-Za-zÀ-öø-ÿ''\\-]+){0,3})"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper;

    public OpenLeadershipClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Lead> findLeadership(String companyName, String countryHint) {
        if (companyName == null || companyName.isBlank()) {
            return Optional.empty();
        }
        // Prefer Wikidata structured claims (current CEO) over noisy Wikipedia text.
        Optional<Lead> wikidata = fromWikidata(companyName.trim());
        if (wikidata.isPresent() && wikidata.get().score() >= 90) {
            return wikidata;
        }
        Optional<Lead> wiki = fromWikipedia(companyName.trim());
        if (wiki.isPresent() && wiki.get().score() >= 90) {
            return wiki;
        }
        if (wikidata.isPresent()) {
            return wikidata;
        }
        if (wiki.isPresent()) {
            return wiki;
        }
        return fromDuckDuckGo(companyName.trim(), countryHint);
    }

    private Optional<Lead> fromWikipedia(String companyName) {
        try {
            String preferred = NlRestaurantBrandSeed.wikipediaTitle(companyName);
            List<String> titles = new ArrayList<>();
            if (preferred != null) {
                titles.add(preferred);
            }
            titles.add(companyName);
            for (String lang : List.of("en", "nl")) {
                for (String title : titles) {
                    Optional<Lead> lead = fetchWikipediaPage(lang, title);
                    if (lead.isPresent()) {
                        log.info("Wikipedia leadership for {} -> {} ({})", companyName, lead.get().name(), lead.get().title());
                        return lead;
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Wikipedia leadership failed for {}: {}", companyName, ex.getMessage());
        }
        return Optional.empty();
    }

    private Optional<Lead> fetchWikipediaPage(String lang, String title) throws Exception {
        String encoded = URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8).replace("+", "_");
        String url = "https://" + lang + ".wikipedia.org/w/api.php"
                + "?action=parse&page=" + encoded
                + "&prop=text&format=json&redirects=1";
        JsonNode root = getJson(url);
        String html = root.path("parse").path("text").path("*").asText("");
        if (html.isBlank()) {
            return Optional.empty();
        }
        Document document = Jsoup.parse(html);
        Map<String, String> infobox = parseInfobox(document);
        Lead fromBox = extractLeadership(infobox, document.text());
        return Optional.ofNullable(fromBox);
    }

    private static Map<String, String> parseInfobox(Document document) {
        Map<String, String> values = new LinkedHashMap<>();
        Element table = document.selectFirst("table.infobox");
        if (table == null) {
            table = document.selectFirst("table.infobox_v2");
        }
        if (table == null) {
            return values;
        }
        for (Element row : table.select("tr")) {
            Element th = row.selectFirst("th");
            Element td = row.selectFirst("td");
            if (th == null || td == null) {
                continue;
            }
            String key = th.text().trim().toLowerCase(Locale.ROOT);
            String value = td.text().trim();
            if (!key.isBlank() && !value.isBlank()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static Lead extractLeadership(Map<String, String> infobox, String pageText) {
        List<Lead> candidates = new ArrayList<>();
        for (Map.Entry<String, String> entry : infobox.entrySet()) {
            String key = entry.getKey();
            if (!(key.contains("key people")
                    || key.contains("founder")
                    || key.contains("ceo")
                    || key.contains("owner")
                    || key.contains("directeur")
                    || key.contains("eigenaar")
                    || key.contains("chairman")
                    || key.contains("president")
                    || key.contains("chief executive"))) {
                continue;
            }
            for (Lead lead : parsePersonRoles(key, entry.getValue())) {
                if (isPlausiblePerson(lead.name(), "")) {
                    candidates.add(lead);
                }
            }
        }
        Optional<Lead> bestBox = candidates.stream().max(Comparator.comparingInt(Lead::score));
        if (bestBox.isPresent() && bestBox.get().score() >= 85) {
            return bestBox.get();
        }
        Matcher matcher = KEY_PEOPLE.matcher(pageText == null ? "" : pageText);
        while (matcher.find()) {
            String title = normalizeTitle(matcher.group(1));
            String name = matcher.group(2).trim();
            if (isPlausiblePerson(name, "")) {
                candidates.add(new Lead(name, title, scoreTitle(title), "wikipedia"));
            }
        }
        return candidates.stream().max(Comparator.comparingInt(Lead::score)).orElse(null);
    }

    private static List<Lead> parsePersonRoles(String key, String value) {
        List<Lead> leads = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return leads;
        }
        String cleaned = value
                .replaceAll("\\[[0-9]+]", "")
                .replaceAll("(?i)\\(.*?died.*?\\)", " ")
                .replaceAll("(?i)died [a-z]+ \\d{4}", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // "Chris Kempczinski (CEO)", "Brian Niccol (CEO)"
        Matcher withRole = Pattern.compile(
                "([A-ZÀ-ÖØ-Þ][A-Za-zÀ-öø-ÿ''\\-]+(?:\\s+[A-ZÀ-ÖØ-Þ][A-Za-zÀ-öø-ÿ''\\-]+){1,3})"
                        + "(?:\\s*[\\(,–-]\\s*([^\\)]{2,40}))?",
                Pattern.CASE_INSENSITIVE
        ).matcher(cleaned);
        while (withRole.find()) {
            String name = withRole.group(1).trim();
            String rolePart = withRole.group(2);
            String title;
            if (rolePart != null && !rolePart.isBlank()) {
                title = normalizeTitle(rolePart);
            } else if (key.contains("ceo") || key.contains("chief executive")) {
                title = "CEO";
            } else if (key.contains("founder")) {
                title = "Founder";
            } else {
                title = normalizeTitle(key);
            }
            // Key-people cells often list CEO first — boost CEO-like paren roles.
            int score = scoreTitle(title);
            if (key.contains("key people") && score < 40) {
                continue;
            }
            leads.add(new Lead(name, title, score, "wikipedia"));
        }
        return leads;
    }

    private Optional<Lead> fromWikidata(String companyName) {
        try {
            List<String> ids = searchEntities(companyName);
            for (String entityId : ids) {
                Optional<Lead> lead = leadershipFromEntity(entityId);
                if (lead.isPresent()) {
                    log.info("Wikidata leadership for {} ({}) -> {} ({})",
                            companyName, entityId, lead.get().name(), lead.get().title());
                    return lead;
                }
            }
        } catch (Exception ex) {
            log.debug("Wikidata leadership failed for {}: {}", companyName, ex.getMessage());
        }
        return Optional.empty();
    }

    private List<String> searchEntities(String companyName) throws Exception {
        List<String> ids = new ArrayList<>();
        for (String language : List.of("en", "nl")) {
            String url = WD_API
                    + "?action=wbsearchentities"
                    + "&search=" + URLEncoder.encode(companyName, StandardCharsets.UTF_8)
                    + "&language=" + language
                    + "&type=item&limit=5&format=json";
            JsonNode root = getJson(url);
            for (JsonNode hit : root.path("search")) {
                String id = text(hit, "id");
                if (!id.isBlank() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private Optional<Lead> leadershipFromEntity(String entityId) throws Exception {
        String url = WD_API
                + "?action=wbgetentities&ids=" + URLEncoder.encode(entityId, StandardCharsets.UTF_8)
                + "&props=claims&format=json";
        JsonNode claims = getJson(url).path("entities").path(entityId).path("claims");
        if (!claims.isObject()) {
            return Optional.empty();
        }
        List<Lead> leads = new ArrayList<>();
        for (Map.Entry<String, LeadershipProp> entry : LEADERSHIP_PROPS.entrySet()) {
            JsonNode claimList = claims.path(entry.getKey());
            if (!claimList.isArray()) {
                continue;
            }
            for (JsonNode claim : claimList) {
                String personId = claim.path("mainsnak").path("datavalue").path("value").path("id").asText("");
                if (personId.isBlank()) {
                    continue;
                }
                boolean ended = claim.path("qualifiers").path("P582").isArray()
                        && !claim.path("qualifiers").path("P582").isEmpty();
                // Skip ended roles when looking for current CEO/director.
                if (ended && "P169".equals(entry.getKey())) {
                    continue;
                }
                String personName = entityLabel(personId);
                if (personName.isBlank() || looksLikeOrgName(personName) || !isPlausiblePerson(personName, "")) {
                    continue;
                }
                int score = entry.getValue().score();
                // Undated P169 claims are often historical leftovers (e.g. Ray Kroc).
                boolean hasStart = claim.path("qualifiers").path("P580").isArray()
                        && !claim.path("qualifiers").path("P580").isEmpty();
                if ("P169".equals(entry.getKey()) && !hasStart) {
                    score = Math.min(score, 45);
                }
                leads.add(new Lead(personName, entry.getValue().title(), score, "wikidata"));
            }
        }
        return leads.stream().max(Comparator.comparingInt(Lead::score));
    }

    private String entityLabel(String entityId) throws Exception {
        String url = WD_API
                + "?action=wbgetentities&ids=" + URLEncoder.encode(entityId, StandardCharsets.UTF_8)
                + "&props=labels&languages=en|nl&format=json";
        JsonNode labels = getJson(url).path("entities").path(entityId).path("labels");
        String en = labels.path("en").path("value").asText("").trim();
        if (!en.isBlank()) {
            return en;
        }
        return labels.path("nl").path("value").asText("").trim();
    }

    private Optional<Lead> fromDuckDuckGo(String companyName, String countryHint) {
        List<String> queries = List.of(
                "\"" + companyName + "\" CEO",
                "\"" + companyName + "\" directeur OR oprichter OR eigenaar",
                "\"" + companyName + "\" founder OR \"managing director\""
                        + (countryHint == null || countryHint.isBlank() ? "" : " " + countryHint)
        );
        List<Lead> leads = new ArrayList<>();
        for (String query : queries) {
            try {
                leads.addAll(searchDuckDuckGo(query, companyName));
                if (leads.stream().anyMatch(l -> l.score() >= 90)) {
                    break;
                }
                Thread.sleep(350);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.debug("DuckDuckGo leadership query failed '{}': {}", query, ex.getMessage());
            }
        }
        Optional<Lead> best = leads.stream().max(Comparator.comparingInt(Lead::score));
        best.ifPresent(lead -> log.info(
                "DuckDuckGo leadership for {} -> {} ({})",
                companyName,
                lead.name(),
                lead.title()
        ));
        return best;
    }

    private List<Lead> searchDuckDuckGo(String query, String companyName) throws Exception {
        String url = "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .timeout(8_000)
                .followRedirects(true)
                .get();
        List<Lead> leads = new ArrayList<>();
        Elements results = doc.select("div.result, div.results_links");
        for (Element result : results) {
            String blob = (result.select("a.result__a").text() + " "
                    + result.select(".result__snippet").text()).replaceAll("\\s+", " ").trim();
            parseSnippet(blob, companyName).ifPresent(leads::add);
        }
        return leads;
    }

    private Optional<Lead> parseSnippet(String text, String companyName) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String companyLower = companyName.toLowerCase(Locale.ROOT);
        Matcher matcher = KEY_PEOPLE.matcher(text);
        List<Lead> candidates = new ArrayList<>();
        while (matcher.find()) {
            String title = normalizeTitle(matcher.group(1));
            String name = matcher.group(2).trim();
            if (isPlausiblePerson(name, companyLower)) {
                candidates.add(new Lead(name, title, scoreTitle(title), "duckduckgo"));
            }
        }
        return candidates.stream().max(Comparator.comparingInt(Lead::score));
    }

    private static boolean isPlausiblePerson(String name, String companyLower) {
        if (name == null || name.length() < 4) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("died")
                || lower.contains("insta-")
                || lower.contains("inc.")
                || lower.contains("llc")
                || lower.startsWith("the ")) {
            return false;
        }
        if (!companyLower.isBlank() && (lower.contains(companyLower) || companyLower.contains(lower))) {
            return false;
        }
        if (looksLikeOrgName(name)) {
            return false;
        }
        String[] parts = name.split("\\s+");
        if (parts.length < 2 || parts.length > 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isBlank() || !Character.isUpperCase(part.charAt(0))) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeOrgName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("inc") || lower.contains("ltd") || lower.contains("company")
                || lower.contains("restaurant") || lower.contains("holding") || lower.contains("group")
                || lower.contains(" bv") || lower.endsWith(" bv") || lower.contains("n.v");
    }

    static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Leader";
        }
        String t = title.trim().toLowerCase(Locale.ROOT);
        if (t.contains("ceo") || t.contains("chief executive")) {
            return "CEO";
        }
        if (t.contains("founder") || t.contains("oprichter")) {
            return "Founder";
        }
        if (t.contains("owner") || t.contains("eigenaar")) {
            return "Owner";
        }
        if (t.contains("managing director") || t.contains("algemeen directeur")) {
            return "Managing Director";
        }
        if (t.contains("directeur") || t.contains("director")) {
            return "Director";
        }
        if (t.contains("manager")) {
            return "Manager";
        }
        if (t.contains("chair") || t.contains("president")) {
            return Character.toUpperCase(title.charAt(0)) + title.substring(1);
        }
        return Character.toUpperCase(title.charAt(0)) + title.substring(1);
    }

    /** Public alias for sibling leadership clients. */
    public static String normalizeTitlePublic(String title) {
        return normalizeTitle(title);
    }

    public static int scoreTitle(String title) {
        return ApolloPeopleLeadershipClient.scoreTitle(title);
    }

    private JsonNode getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("User-Agent", "DataScraperPlatform/0.1 (nl-restaurant-leadership)")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private record LeadershipProp(String title, int score) {
    }

    public record Lead(String name, String title, int score, String source) {
    }
}
