package com.datascraper.export.exception;

import java.util.UUID;

public class ExportNotFoundException extends RuntimeException {

    public ExportNotFoundException(UUID id) {
        super("Export not found: " + id);
    }
}
