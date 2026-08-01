package com.datascraper.export.exception;

import com.datascraper.common.enums.ExportStatus;

import java.util.UUID;

public class ExportNotReadyException extends RuntimeException {

    public ExportNotReadyException(UUID id, ExportStatus status) {
        super("Export " + id + " is not ready for download (status=" + status + ")");
    }
}
