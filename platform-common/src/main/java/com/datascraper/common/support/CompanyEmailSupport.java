package com.datascraper.common.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and ranks authentic public company emails; filters placeholders and tracking addresses.
 */
public final class CompanyEmailSupport {

    public static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);

    private static final Pattern OBFUSCATED_EMAIL = Pattern.compile(
            "(?i)\\b([A-Z0-9._%+-]+)\\s*(?:\\[\\s*at\\s*\\]|\\(\\s*at\\s*\\)|\\s+at\\s+|@)\\s*"
                    + "([A-Z0-9.-]+)\\s*(?:\\[\\s*dot\\s*\\]|\\(\\s*dot\\s*\\)|\\s+dot\\s+|\\.)\\s*"
                    + "([A-Z]{2,})\\b"
    );

    private static final Set<String> JUNK_LOCAL_PARTS = Set.of(
            "noreply", "no-reply", "donotreply", "do-not-reply", "mailer-daemon",
            "postmaster", "abuse", "webmaster", "hostmaster", "bounce", "bounces",
            "notifications", "notification", "automated", "auto", "daemon",
            "unsubscribe", "newsletter", "newsletters", "marketing-noreply"
    );

    private static final Set<String> JUNK_DOMAINS = Set.of(
            "example.com", "example.org", "example.net", "test.com", "localhost",
            "email.com", "domain.com", "yourdomain.com", "company.com",
            "sentry.io", "wixpress.com", "squarespace.com", "godaddy.com",
            "cloudflare.com", "googleusercontent.com", "gstatic.com",
            "schema.org", "w3.org", "github.com", "gitlab.com"
    );

    private static final Set<String> ROLE_LOCAL_PARTS = Set.of(
            "info", "contact", "hello", "sales", "support", "office", "enquiries",
            "enquiry", "inquiries", "inquiry", "team", "business", "admin",
            "reception", "mail", "general", "help", "service", "customerservice",
            "customer-service", "press", "media", "partners", "partner"
    );

    private CompanyEmailSupport() {
    }

    public static boolean hasEmail(String email) {
        return email != null && !email.isBlank();
    }

    public static String clean(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.toLowerCase(Locale.ROOT).startsWith("mailto:")) {
            value = value.substring("mailto:".length()).trim();
        }
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        value = value.replace("%40", "@").replace("%2E", ".");
        value = value.replaceAll("[<>\"']", "").trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            return null;
        }
        return isAuthentic(value, null) ? value : null;
    }

    public static List<String> extractFromText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> found = new LinkedHashSet<>();

        Matcher plain = EMAIL_PATTERN.matcher(text);
        while (plain.find()) {
            String cleaned = clean(plain.group());
            if (cleaned != null) {
                found.add(cleaned);
            }
        }

        Matcher obfuscated = OBFUSCATED_EMAIL.matcher(text);
        while (obfuscated.find()) {
            String candidate = obfuscated.group(1) + "@" + obfuscated.group(2) + "." + obfuscated.group(3);
            String cleaned = clean(candidate);
            if (cleaned != null) {
                found.add(cleaned);
            }
        }

        return new ArrayList<>(found);
    }

    /**
     * Rank emails for a company website: mailto / role / same-domain first.
     */
    public static List<String> rank(List<String> emails, String websiteUrl) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        String companyDomain = domainOf(websiteUrl);
        return emails.stream()
                .map(CompanyEmailSupport::clean)
                .filter(email -> email != null && isAuthentic(email, companyDomain))
                .distinct()
                .sorted(Comparator
                        .comparingInt((String email) -> -score(email, companyDomain))
                        .thenComparing(email -> email))
                .toList();
    }

    public static String prefer(String current, String candidate, String websiteUrl) {
        String next = clean(candidate);
        if (next == null) {
            return current;
        }
        String existing = clean(current);
        if (existing == null) {
            return next;
        }
        String domain = domainOf(websiteUrl);
        return score(next, domain) > score(existing, domain) ? next : existing;
    }

    public static boolean isAuthentic(String email, String companyDomain) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        if (at <= 0 || at == normalized.length() - 1) {
            return false;
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at + 1);

        if (local.length() > 64 || domain.length() > 255) {
            return false;
        }
        if (local.contains("..") || domain.contains("..")) {
            return false;
        }
        if (JUNK_LOCAL_PARTS.contains(local) || local.startsWith("noreply") || local.endsWith("noreply")) {
            return false;
        }
        if (JUNK_DOMAINS.contains(domain) || domain.endsWith(".test") || domain.endsWith(".invalid")) {
            return false;
        }
        if (domain.contains("sentry") || domain.contains("wixpress") || domain.contains("mailchimp")) {
            return false;
        }
        if (local.matches("(?i).+\\.(png|jpe?g|gif|svg|webp|css|js)$")) {
            return false;
        }
        // Keep role@company addresses; reject generic free-mail only when it is clearly a placeholder local part.
        if (companyDomain != null && !companyDomain.isBlank() && domainEquals(domain, companyDomain)) {
            return true;
        }
        return true;
    }

    public static int score(String email, String companyDomain) {
        if (email == null) {
            return Integer.MIN_VALUE;
        }
        String normalized = email.toLowerCase(Locale.ROOT);
        int at = normalized.indexOf('@');
        if (at <= 0) {
            return Integer.MIN_VALUE;
        }
        String local = normalized.substring(0, at);
        String domain = normalized.substring(at + 1);
        int score = 10;
        if (companyDomain != null && domainEquals(domain, companyDomain)) {
            score += 40;
        }
        if (ROLE_LOCAL_PARTS.contains(local)) {
            score += 25;
        } else if (local.contains(".")) {
            // person.name@company — still valuable
            score += 15;
        }
        if (local.startsWith("info") || local.startsWith("contact") || local.startsWith("hello")) {
            score += 5;
        }
        if (domain.endsWith(".png") || domain.contains("cdn")) {
            score -= 50;
        }
        return score;
    }

    public static String domainOf(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return null;
        }
        try {
            String value = websiteUrl.trim();
            if (!value.contains("://")) {
                value = "https://" + value;
            }
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean domainEquals(String emailDomain, String companyDomain) {
        if (emailDomain == null || companyDomain == null) {
            return false;
        }
        return emailDomain.equalsIgnoreCase(companyDomain)
                || emailDomain.endsWith("." + companyDomain)
                || companyDomain.endsWith("." + emailDomain);
    }
}
