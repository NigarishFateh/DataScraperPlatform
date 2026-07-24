/**
 * Tests that the company website HTML parser extracts the right fields.
 */
package com.datascraper.website.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyWebsiteHtmlParserTest {

    private final CompanyWebsiteHtmlParser parser = new CompanyWebsiteHtmlParser();

    @Test
    void parsesIdentityMetaHeadingsAndContactLinks() {
        String html = """
                <!doctype html>
                <html>
                <head>
                  <title>Acme Cloud GmbH</title>
                  <meta name="description" content="European cloud consultancy"/>
                  <meta property="og:site_name" content="Acme Cloud"/>
                  <link rel="canonical" href="https://acme.example/"/>
                </head>
                <body>
                  <h1>We build cloud platforms</h1>
                  <p>This paragraph is intentionally ignored because it is too short.</p>
                  <p>Acme Cloud helps European enterprises migrate to modern cloud infrastructure with security and observability built in from day one.</p>
                  <a href="mailto:hello@acme.example">Email us</a>
                  <a href="https://linkedin.com/company/acme">LinkedIn</a>
                  <a href="/careers">Careers</a>
                </body>
                </html>
                """;

        Document document = Jsoup.parse(html, "https://acme.example/");
        List<Map<String, Object>> items = parser.parse(document, "https://acme.example/", 20);

        assertThat(items).isNotEmpty();
        assertThat(items).anyMatch(item ->
                "identity".equals(item.get("section")) && "Acme Cloud GmbH".equals(item.get("title")));
        assertThat(items).anyMatch(item ->
                "contact".equals(item.get("section")) && "hello@acme.example".equals(item.get("title")));
        assertThat(items).anyMatch(item ->
                "presence".equals(item.get("section")) && "linkedin".equals(item.get("field")));
    }
}
