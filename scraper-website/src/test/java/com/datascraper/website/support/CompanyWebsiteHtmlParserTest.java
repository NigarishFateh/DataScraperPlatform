/**
 * Tests that the company website HTML parser extracts export-relevant fields.
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
    void parsesWebsiteEmailPhoneAndFounder() {
        String html = """
                <!doctype html>
                <html>
                <head>
                  <title>Acme Cloud GmbH</title>
                  <link rel="canonical" href="https://acme.example/"/>
                </head>
                <body>
                  <p>Founder: Jane Founder</p>
                  <a href="mailto:hello@acme.example">Email us</a>
                  <a href="tel:+49301234567">Call us</a>
                  <a href="https://linkedin.com/company/acme">LinkedIn</a>
                </body>
                </html>
                """;

        Document document = Jsoup.parse(html, "https://acme.example/");
        List<Map<String, Object>> items = parser.parse(document, "https://acme.example/", 20);

        assertThat(items).anyMatch(item ->
                "identity".equals(item.get("section")) && "Acme Cloud GmbH".equals(item.get("title")));
        assertThat(items).anyMatch(item ->
                "contact".equals(item.get("section")) && "hello@acme.example".equals(item.get("title")));
        assertThat(items).anyMatch(item ->
                "contact".equals(item.get("section")) && "phone".equals(item.get("field")));
        assertThat(items).anyMatch(item ->
                "people".equals(item.get("section")) && "Jane Founder".equals(item.get("title")));
        assertThat(items).noneMatch(item -> "presence".equals(item.get("section")));
    }

    @Test
    void parsesBranchManagerSeparatelyFromFounder() {
        String html = """
                <!doctype html>
                <html>
                <body>
                  <p>Founder: Jane Founder</p>
                  <p>Vestigingsmanager: Piet Jansen</p>
                </body>
                </html>
                """;

        Document document = Jsoup.parse(html, "https://febo.example/vestigingen/amsterdam");
        List<Map<String, Object>> items = parser.parse(document, "https://febo.example/vestigingen/amsterdam", 20);

        assertThat(items).anyMatch(item ->
                "people".equals(item.get("section"))
                        && "founder".equals(item.get("field"))
                        && "Jane Founder".equals(item.get("title")));
        assertThat(items).anyMatch(item ->
                "people".equals(item.get("section"))
                        && "branchManager".equals(item.get("field"))
                        && "Piet Jansen".equals(item.get("title")));
    }
}
