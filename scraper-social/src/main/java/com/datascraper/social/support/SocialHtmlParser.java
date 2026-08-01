/**
 * Extracts social profile links from page HTML href attributes.
 */
package com.datascraper.social.support;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class SocialHtmlParser {

    private static final Set<String> GITHUB_RESERVED = Set.of(
            "about", "apps", "collections", "contact", "customer-stories", "enterprise",
            "events", "explore", "features", "issues", "login", "marketplace", "new",
            "notifications", "orgs", "organizations", "pricing", "pulls", "search",
            "security", "settings", "signup", "site", "sponsors", "topics", "trending"
    );

    private static final Set<String> TWITTER_RESERVED = Set.of(
            "home", "i", "intent", "login", "privacy", "search", "settings", "share", "signup", "tos"
    );

    private static final Set<String> FACEBOOK_RESERVED = Set.of(
            "dialog", "help", "login", "privacy", "sharer", "share", "watch"
    );

    public List<Map<String, Object>> parse(Document document, String sourceUrl, int maxItems) {
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (Element link : document.select("a[href]")) {
            String href = link.absUrl("href");
            if (href.isBlank()) {
                continue;
            }
            classifyProfile(href).ifPresent(profile -> addProfile(items, seen, profile, sourceUrl));
            if (items.size() >= maxItems) {
                break;
            }
        }

        return items.size() > maxItems ? items.subList(0, maxItems) : items;
    }

    Optional<ProfileLink> classifyProfile(String url) {
        try {
            URI uri = URI.create(stripFragment(url));
            String host = normalizeHost(uri.getHost());
            if (host == null) {
                return Optional.empty();
            }
            String path = uri.getPath() == null ? "" : uri.getPath();

            if (host.endsWith("linkedin.com")) {
                return linkedInProfile(uri, path);
            }
            if (host.equals("twitter.com") || host.equals("x.com") || host.endsWith(".twitter.com")
                    || host.endsWith(".x.com")) {
                return twitterProfile(uri, path);
            }
            if (host.endsWith("facebook.com") || host.equals("fb.com")) {
                return facebookProfile(uri, path);
            }
            if (host.endsWith("instagram.com")) {
                return instagramProfile(uri, path);
            }
            if (host.endsWith("youtube.com")) {
                return youtubeProfile(uri, path);
            }
            if (host.equals("youtu.be")) {
                return Optional.empty();
            }
            if (host.endsWith("github.com")) {
                return githubProfile(uri, path);
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<ProfileLink> linkedInProfile(URI uri, String path) {
        if (path.matches("/(company|in|showcase)/[^/]+/?")) {
            return Optional.of(new ProfileLink("linkedin", canonicalUrl(uri)));
        }
        return Optional.empty();
    }

    private Optional<ProfileLink> twitterProfile(URI uri, String path) {
        String[] segments = path.split("/");
        if (segments.length >= 2 && !segments[1].isBlank()) {
            String handle = segments[1].toLowerCase(Locale.ROOT);
            if (!TWITTER_RESERVED.contains(handle)) {
                return Optional.of(new ProfileLink("twitter", profileRoot(uri, "/" + handle)));
            }
        }
        return Optional.empty();
    }

    private Optional<ProfileLink> facebookProfile(URI uri, String path) {
        String[] segments = path.split("/");
        if (segments.length >= 2 && !segments[1].isBlank()) {
            String page = segments[1].toLowerCase(Locale.ROOT);
            if (!FACEBOOK_RESERVED.contains(page) && !page.equals("pages")) {
                return Optional.of(new ProfileLink("facebook", canonicalUrl(uri)));
            }
            if (page.equals("pages") && segments.length >= 3 && !segments[2].isBlank()) {
                return Optional.of(new ProfileLink("facebook", canonicalUrl(uri)));
            }
        }
        return Optional.empty();
    }

    private Optional<ProfileLink> instagramProfile(URI uri, String path) {
        String[] segments = path.split("/");
        if (segments.length >= 2 && !segments[1].isBlank()) {
            String handle = segments[1].toLowerCase(Locale.ROOT);
            if (!handle.equals("p") && !handle.equals("reel") && !handle.equals("stories")
                    && !handle.equals("explore") && !handle.equals("accounts")) {
                return Optional.of(new ProfileLink("instagram", profileRoot(uri, "/" + handle)));
            }
        }
        return Optional.empty();
    }

    private Optional<ProfileLink> youtubeProfile(URI uri, String path) {
        if (path.startsWith("/@") && path.length() > 2) {
            return Optional.of(new ProfileLink("youtube", canonicalUrl(uri)));
        }
        if (path.matches("/(channel|c|user)/[^/]+/?")) {
            return Optional.of(new ProfileLink("youtube", canonicalUrl(uri)));
        }
        return Optional.empty();
    }

    private Optional<ProfileLink> githubProfile(URI uri, String path) {
        String[] segments = path.split("/");
        if (segments.length >= 2 && !segments[1].isBlank()) {
            String org = segments[1].toLowerCase(Locale.ROOT);
            if (!GITHUB_RESERVED.contains(org)) {
                return Optional.of(new ProfileLink("github", profileRoot(uri, "/" + org)));
            }
        }
        return Optional.empty();
    }

    private void addProfile(
            List<Map<String, Object>> items,
            Set<String> seen,
            ProfileLink profile,
            String sourceUrl
    ) {
        String key = profile.platform() + ":" + profile.url().toLowerCase(Locale.ROOT);
        if (!seen.add(key)) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("platform", profile.platform());
        item.put("url", profile.url());
        item.put("sourceUrl", sourceUrl);
        items.add(item);
    }

    private static String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        return host.toLowerCase(Locale.ROOT);
    }

    private static String stripFragment(String url) {
        int hash = url.indexOf('#');
        return hash >= 0 ? url.substring(0, hash) : url;
    }

    private static String canonicalUrl(URI uri) {
        return profileRoot(uri, uri.getPath() == null ? "" : uri.getPath());
    }

    private static String profileRoot(URI uri, String path) {
        String normalizedPath = path == null ? "" : path;
        while (normalizedPath.endsWith("/") && normalizedPath.length() > 1) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() > 0) {
            builder.append(':').append(uri.getPort());
        }
        builder.append(normalizedPath.isEmpty() ? "/" : normalizedPath);
        return builder.toString();
    }

    private record ProfileLink(String platform, String url) {
    }
}
