package com.datascraper.export.client;

import java.util.UUID;

public record CompleteJobRequest(String exportId) {

    public CompleteJobRequest {
        if (exportId == null || exportId.isBlank()) {
            throw new IllegalArgumentException("exportId is required");
        }
    }

    public static CompleteJobRequest of(UUID exportId) {
        return new CompleteJobRequest(exportId.toString());
    }
}
