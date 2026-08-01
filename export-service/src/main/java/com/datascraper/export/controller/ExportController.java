package com.datascraper.export.controller;

import com.datascraper.common.dto.export.ExportRequest;
import com.datascraper.common.dto.export.ExportResponse;
import com.datascraper.export.service.ExportService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping
    public ExportResponse createExport(@Valid @RequestBody ExportRequest request) {
        return exportService.createExport(request);
    }

    @GetMapping("/{id}")
    public ExportResponse getExport(@PathVariable UUID id) {
        return exportService.getExport(id);
    }

    @GetMapping
    public List<ExportResponse> listExports(@RequestParam(required = false) UUID jobId) {
        if (jobId != null) {
            return exportService.listByJobId(jobId);
        }
        return exportService.listRecent(100);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadExport(@PathVariable UUID id) {
        ExportResponse export = exportService.getExport(id);
        Resource resource = exportService.downloadExport(id);
        String fileName = export.fileName() != null ? export.fileName() : "export.xlsx";
        String safeAscii = fileName.replace("\"", "");
        long contentLength;
        try {
            contentLength = resource.contentLength();
        } catch (Exception ex) {
            contentLength = export.fileSizeBytes() > 0 ? export.fileSizeBytes() : -1;
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeAscii + "\"; filename*=UTF-8''"
                                + java.net.URLEncoder.encode(safeAscii, java.nio.charset.StandardCharsets.UTF_8)
                                .replace("+", "%20"))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        if (contentLength >= 0) {
            builder = builder.contentLength(contentLength);
        }
        return builder.body(resource);
    }
}
