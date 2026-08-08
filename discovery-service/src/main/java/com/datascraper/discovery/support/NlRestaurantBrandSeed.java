package com.datascraper.discovery.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Priority NL restaurant / QSR brands for isolated leadership lookups.
 * Not used by category discovery — kept separate so scrape jobs stay unchanged.
 */
public final class NlRestaurantBrandSeed {

    public static final List<String> BRANDS = List.of(
            "FEBO",
            "Smullers",
            "Kwalitaria",
            "Bram Ladage",
            "Manneken Pis",
            "Vlaams Friteshuis Vleminckx",
            "La Place",
            "McDonald's",
            "Burger King",
            "KFC",
            "Subway",
            "Five Guys",
            "Taco Bell",
            "Pizza Hut",
            "Domino's Pizza",
            "New York Pizza",
            "Papa John's",
            "Work to Go",
            "Bagels & Beans",
            "Dunkin'"
    );

    private static final Map<String, String> OFFICIAL_WEBSITES = Map.ofEntries(
            Map.entry("febo", "https://www.febo.nl"),
            Map.entry("smullers", "https://www.smullers.nl"),
            Map.entry("kwalitaria", "https://www.kwalitaria.nl"),
            Map.entry("bram ladage", "https://www.bramladage.nl"),
            Map.entry("manneken pis", "https://www.mannekenpis.be"),
            Map.entry("vlaams friteshuis vleminckx", "https://www.vleminckx.com"),
            Map.entry("la place", "https://www.laplace.com"),
            Map.entry("la palace", "https://www.laplace.com"),
            Map.entry("mcdonald's", "https://www.mcdonalds.com/nl/nl-nl.html"),
            Map.entry("mcdonalds", "https://www.mcdonalds.com/nl/nl-nl.html"),
            Map.entry("burger king", "https://www.burgerking.nl"),
            Map.entry("kfc", "https://www.kfc.nl"),
            Map.entry("subway", "https://www.subway.com"),
            Map.entry("five guys", "https://www.fiveguys.nl"),
            Map.entry("taco bell", "https://www.tacobell.nl"),
            Map.entry("pizza hut", "https://www.pizzahut.nl"),
            Map.entry("domino's pizza", "https://www.dominos.nl"),
            Map.entry("dominos pizza", "https://www.dominos.nl"),
            Map.entry("new york pizza", "https://www.newyorkpizza.nl"),
            Map.entry("papa john's", "https://www.papajohns.nl"),
            Map.entry("papa johns", "https://www.papajohns.nl"),
            Map.entry("work to go", "https://www.worktogo.nl"),
            Map.entry("bagels & beans", "https://www.bagelsbeans.nl"),
            Map.entry("bagels and beans", "https://www.bagelsbeans.nl"),
            Map.entry("dunkin'", "https://www.dunkin.nl"),
            Map.entry("dunkin", "https://www.dunkin.nl")
    );

    private static final Map<String, String> WIKIPEDIA_TITLES = Map.ofEntries(
            Map.entry("febo", "FEBO"),
            Map.entry("smullers", "Smullers"),
            Map.entry("kwalitaria", "Kwalitaria"),
            Map.entry("bram ladage", "Bram Ladage"),
            Map.entry("la place", "La Place (restaurant chain)"),
            Map.entry("la palace", "La Place (restaurant chain)"),
            Map.entry("mcdonald's", "McDonald's"),
            Map.entry("burger king", "Burger King"),
            Map.entry("kfc", "KFC"),
            Map.entry("subway", "Subway (restaurant)"),
            Map.entry("five guys", "Five Guys"),
            Map.entry("taco bell", "Taco Bell"),
            Map.entry("pizza hut", "Pizza Hut"),
            Map.entry("domino's pizza", "Domino's"),
            Map.entry("papa john's", "Papa John's"),
            Map.entry("dunkin'", "Dunkin' Donuts"),
            Map.entry("dunkin", "Dunkin' Donuts"),
            Map.entry("bagels & beans", "Bagels & Beans"),
            Map.entry("new york pizza", "New York Pizza"),
            Map.entry("manneken pis", "Manneken Pis")
    );

    /**
     * Public ticker symbols for FMP Company Executives (public companies only).
     * Private NL brands intentionally omitted.
     */
    private static final Map<String, String> STOCK_TICKERS = Map.ofEntries(
            Map.entry("mcdonald's", "MCD"),
            Map.entry("mcdonalds", "MCD"),
            Map.entry("burger king", "QSR"),
            Map.entry("kfc", "YUM"),
            Map.entry("taco bell", "YUM"),
            Map.entry("pizza hut", "YUM"),
            Map.entry("domino's pizza", "DPZ"),
            Map.entry("dominos pizza", "DPZ"),
            Map.entry("papa john's", "PZZA"),
            Map.entry("papa johns", "PZZA")
    );
    private static final Map<String, KnownLeader> KNOWN_LEADERS = Map.ofEntries(
            Map.entry("febo", new KnownLeader("Johan de Borst", "Founder")),
            Map.entry("bram ladage", new KnownLeader("Bram Ladage", "Founder")),
            Map.entry("bagels & beans", new KnownLeader("Ton van Beek", "Founder")),
            Map.entry("bagels and beans", new KnownLeader("Ton van Beek", "Founder")),
            Map.entry("manneken pis", new KnownLeader("Daniel Haneman", "Founder")),
            Map.entry("mcdonald's", new KnownLeader("Chris Kempczinski", "CEO")),
            Map.entry("mcdonalds", new KnownLeader("Chris Kempczinski", "CEO")),
            Map.entry("burger king", new KnownLeader("Joshua Kobza", "CEO")),
            Map.entry("kfc", new KnownLeader("Sabir Sami", "CEO")),
            Map.entry("subway", new KnownLeader("John Chidsey", "CEO")),
            Map.entry("taco bell", new KnownLeader("Sean Tresvant", "CEO")),
            Map.entry("pizza hut", new KnownLeader("David Gibbs", "CEO")),
            Map.entry("domino's pizza", new KnownLeader("Russell Weiner", "CEO")),
            Map.entry("dominos pizza", new KnownLeader("Russell Weiner", "CEO")),
            Map.entry("papa john's", new KnownLeader("Todd Penegor", "CEO")),
            Map.entry("papa johns", new KnownLeader("Todd Penegor", "CEO")),
            Map.entry("dunkin'", new KnownLeader("Scott Murphy", "CEO")),
            Map.entry("dunkin", new KnownLeader("Scott Murphy", "CEO")),
            Map.entry("five guys", new KnownLeader("Jerry Murrell", "Founder"))
    );

    private NlRestaurantBrandSeed() {
    }

    public static String normalizeKey(String brandName) {
        if (brandName == null) {
            return "";
        }
        return brandName.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public static String officialWebsite(String brandName) {
        return OFFICIAL_WEBSITES.get(normalizeKey(brandName));
    }

    public static String wikipediaTitle(String brandName) {
        return WIKIPEDIA_TITLES.get(normalizeKey(brandName));
    }

    public static KnownLeader knownLeader(String brandName) {
        return KNOWN_LEADERS.get(normalizeKey(brandName));
    }

    public static String stockTicker(String brandName) {
        return STOCK_TICKERS.get(normalizeKey(brandName));
    }

    public static String canonicalBrandName(String brandName) {
        String key = normalizeKey(brandName);
        if (key.equals("la palace")) {
            return "La Place";
        }
        for (String brand : BRANDS) {
            if (normalizeKey(brand).equals(key)) {
                return brand;
            }
        }
        return brandName == null ? "" : brandName.trim();
    }

    public static String domain(String brandName) {
        String website = officialWebsite(brandName);
        if (website == null || website.isBlank()) {
            return null;
        }
        String value = website.trim().toLowerCase(Locale.ROOT)
                .replaceFirst("^https?://", "")
                .replaceFirst("^www\\.", "");
        int slash = value.indexOf('/');
        if (slash >= 0) {
            value = value.substring(0, slash);
        }
        return value.isBlank() ? null : value;
    }

    public static Map<String, String> brandWebsiteMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String brand : BRANDS) {
            map.put(brand, officialWebsite(brand));
        }
        return map;
    }

    public static Map<String, String> brandTickerMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String brand : BRANDS) {
            String ticker = stockTicker(brand);
            if (ticker != null) {
                map.put(brand, ticker);
            }
        }
        return map;
    }

    public record KnownLeader(String name, String title) {
    }
}
