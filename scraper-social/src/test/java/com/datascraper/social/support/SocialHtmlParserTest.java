package com.datascraper.social.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SocialHtmlParserTest {

    private final SocialHtmlParser parser = new SocialHtmlParser();

    @Test
    void extractsSocialLinksFromAnchorHrefs() {
        Document document = Jsoup.parse("""
                <html><body>
                  <a href="https://www.linkedin.com/company/acme-corp">LinkedIn</a>
                  <a href="https://twitter.com/acme/status/123">Twitter</a>
                  <a href="https://x.com/acme">X</a>
                  <a href="https://facebook.com/acmepage">Facebook</a>
                  <a href="https://instagram.com/acme/">Instagram</a>
                  <a href="https://youtube.com/@acmechannel">YouTube</a>
                  <a href="https://github.com/acme-org">GitHub</a>
                  <a href="https://example.com/about">Internal</a>
                </body></html>
                """);

        List<Map<String, Object>> items = parser.parse(document, "https://acme.example", 25);

        assertThat(items).hasSize(7);
        assertThat(items).extracting(item -> item.get("platform"))
                .containsExactlyInAnyOrder(
                        "linkedin", "twitter", "twitter", "facebook", "instagram", "youtube", "github"
                );
        assertThat(items.stream().map(item -> item.get("url")))
                .contains(
                        "https://www.linkedin.com/company/acme-corp",
                        "https://twitter.com/acme",
                        "https://x.com/acme",
                        "https://facebook.com/acmepage",
                        "https://instagram.com/acme",
                        "https://youtube.com/@acmechannel",
                        "https://github.com/acme-org"
                );
    }

    @Test
    void ignoresShareAndIntentLinks() {
        Document document = Jsoup.parse("""
                <html><body>
                  <a href="https://twitter.com/intent/tweet?url=x">Share</a>
                  <a href="https://facebook.com/sharer/sharer.php?u=x">Share</a>
                  <a href="https://github.com/features">Features</a>
                </body></html>
                """);

        List<Map<String, Object>> items = parser.parse(document, "https://acme.example", 25);

        assertThat(items).isEmpty();
    }
}
