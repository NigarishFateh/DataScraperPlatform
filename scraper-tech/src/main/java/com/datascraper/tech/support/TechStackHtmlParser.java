package com.datascraper.tech.support;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class TechStackHtmlParser {

    private static final List<String> TECH_SIGNALS = List.of(
            "react", "angular", "vue", "next.js", "nuxt", "svelte",
            "spring", "django", "flask", "rails", "laravel", "node.js",
            "typescript", "javascript", "python", "java", "kotlin", "go", "rust", "scala",
            "kubernetes", "docker", "terraform", "ansible",
            "aws", "amazon web services", "azure", "google cloud", "gcp",
            "postgresql", "postgres", "mysql", "mongodb", "redis", "elasticsearch", "kafka",
            "graphql", "grpc", "rabbitmq", "spark", "hadoop",
            "jenkins", "gitlab", "github actions", "circleci"
    );

    public List<Map<String, Object>> parse(Document document, String sourceUrl, int maxItems) {
        List<Map<String, Object>> items = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        Element generator = document.selectFirst("meta[name=generator]");
        if (generator != null) {
            addSignal(items, seen, "meta-generator", generator.attr("content"), sourceUrl);
        }

        for (Element script : document.select("script[src]")) {
            String src = script.attr("src").toLowerCase(Locale.ROOT);
            detectFromText(items, seen, src, "script-src", sourceUrl);
        }

        String html = document.html().toLowerCase(Locale.ROOT);
        for (String signal : TECH_SIGNALS) {
            if (html.contains(signal)) {
                addSignal(items, seen, "technology", signal, sourceUrl);
            }
        }

        return items.size() > maxItems ? items.subList(0, maxItems) : items;
    }

    private void detectFromText(
            List<Map<String, Object>> items,
            Set<String> seen,
            String text,
            String field,
            String sourceUrl
    ) {
        for (String signal : TECH_SIGNALS) {
            if (text.contains(signal.replace(" ", "")) || text.contains(signal)) {
                addSignal(items, seen, field, signal, sourceUrl);
            }
        }
    }

    private void addSignal(
            List<Map<String, Object>> items,
            Set<String> seen,
            String field,
            String value,
            String sourceUrl
    ) {
        if (value == null || value.isBlank() || !seen.add(field + ":" + value.toLowerCase(Locale.ROOT))) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("section", "technology");
        item.put("field", field);
        item.put("value", value);
        item.put("sourceUrl", sourceUrl);
        items.add(item);
    }
}
