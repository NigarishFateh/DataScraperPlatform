package com.datascraper.export.exception;

import java.util.UUID;

public class ExportNotFoundException extends RuntimeException {

    public ExportNotFoundException(UUID id) {
        super("Export file is missing or could not be regenerated: " + id);
    }
}
