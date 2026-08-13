package com.datascraper.export.service;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.enums.JobPhase;
import com.datascraper.common.enums.JobStatus;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExportWriterTest {

    private final ExcelExportWriter writer = new ExcelExportWriter();

    @Test
    void writesNonEmptyWorkbookWithSampleCompanies(@TempDir Path tempDir) throws Exception {
        UUID jobId = UUID.randomUUID();
        Instant scrapedAt = Instant.parse("2026-01-15T10:30:00Z");
        Instant generatedAt = Instant.parse("2026-08-01T09:00:00Z");

        EnrichedCompany first = new EnrichedCompany(
                "alpha-1",
                "Alpha Analytics GmbH",
                "Software",
                "Information Technology",
                "DE",
                "Germany",
                "Berlin",
                "Berlin",
                "https://alpha.example.com",
                "contact@alpha.example.com",
                "+49 30 1234567",
                "Jane Founder",
                "John CEO",
                "Enterprise analytics platform",
                "Consulting, Implementation",
                "Analytics Suite",
                List.of("Java", "PostgreSQL", "Kafka"),
                "https://linkedin.com/company/alpha",
                "https://github.com/alpha",
                null,
                "https://twitter.com/alpha",
                null,
                null,
                2015,
                "51-200",
                "Friedrichstrasse 1, Berlin",
                "https://alpha.example.com/contact",
                "https://alpha.example.com",
                scrapedAt,
                0.92,
                "website-scraper",
                "Verified via website crawl",
                List.of("cat-software"),
                null
        );

        EnrichedCompany second = new EnrichedCompany(
                "beta-2",
                "Beta Systems AG",
                "Hardware",
                "Manufacturing",
                "CH",
                "Switzerland",
                "Zurich",
                "Zurich",
                "https://beta.example.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("Python"),
                null,
                null,
                null,
                null,
                null,
                null,
                2008,
                "201-500",
                null,
                null,
                "https://beta.example.com",
                scrapedAt,
                0.41,
                "discovery-provider",
                null,
                List.of("cat-hardware"),
                null
        );

        JobResponse job = new JobResponse(
                jobId,
                JobStatus.COMPLETED,
                JobPhase.EXPORT,
                "user-123",
                List.of("cat-software", "cat-hardware"),
                List.of("DE", "CH"),
                List.of("city-berlin", "city-zurich"),
                120,
                95,
                90,
                5,
                100,
                0L,
                null,
                null,
                null,
                generatedAt,
                generatedAt,
                generatedAt,
                generatedAt
        );

        Path outputFile = tempDir.resolve("sample-export.xlsx");
        ExcelExportWriter.ExportWriteResult result = writer.writeWorkbook(
                outputFile,
                List.of(first, second),
                job,
                "1.0.0",
                generatedAt
        );

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.size(outputFile) > 0);
        assertEquals(2, result.rowCount());
        assertTrue(result.fileSizeBytes() > 0);

        try (InputStream in = Files.newInputStream(outputFile);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            // Multi-category job → one sheet partition per category (plus none unassigned here)
            assertEquals(2, workbook.getNumberOfSheets());
            assertEquals("Cat-Software", workbook.getSheetAt(0).getSheetName());
            assertEquals("Cat-Hardware", workbook.getSheetAt(1).getSheetName());

            assertEquals("Category: Cat-Software", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Company Name", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("Branch ID", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
            assertEquals("City", workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue());
            assertEquals("Founder / CEO", workbook.getSheetAt(0).getRow(1).getCell(7).getStringCellValue());
            assertEquals("Branch Manager", workbook.getSheetAt(0).getRow(1).getCell(8).getStringCellValue());
            assertEquals("Alpha Analytics GmbH", workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
            assertNotNull(workbook.getSheetAt(0).getCTWorksheet().getAutoFilter());

            assertEquals("Category: Cat-Hardware", workbook.getSheetAt(1).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Beta Systems AG", workbook.getSheetAt(1).getRow(2).getCell(0).getStringCellValue());

            assertEquals("Pakistan_Artificial-Intelligence.xlsx",
                    ExcelExportWriter.buildDownloadFileName(new JobResponse(
                            jobId,
                            JobStatus.COMPLETED,
                            JobPhase.EXPORT,
                            "user-123",
                            List.of("ai"),
                            List.of("PK"),
                            List.of("pk-islamabad"),
                            8, 8, 8, 0, 100, 0L, null, null, null,
                            generatedAt, generatedAt, generatedAt, generatedAt
                    )));
        }
    }

    @Test
    void writesClassicThreeSheetsForSingleCategory(@TempDir Path tempDir) throws Exception {
        Instant generatedAt = Instant.parse("2026-08-01T09:00:00Z");
        Instant scrapedAt = Instant.parse("2026-01-15T10:30:00Z");
        EnrichedCompany company = new EnrichedCompany(
                "alpha-1",
                "Alpha Analytics GmbH",
                "Software",
                "Information Technology",
                "DE",
                "Germany",
                "Berlin",
                "Berlin",
                "https://alpha.example.com",
                "contact@alpha.example.com",
                "+49 30 1234567",
                "Jane Founder",
                "John CEO",
                null, null, null, List.of(), null, null, null, null, null, null,
                null, null, "Friedrichstrasse 1, Berlin", null, "https://alpha.example.com",
                scrapedAt, 0.9, "website-scraper", null,
                List.of("software"),
                null
        );
        JobResponse job = new JobResponse(
                UUID.randomUUID(),
                JobStatus.COMPLETED,
                JobPhase.EXPORT,
                "user-123",
                List.of("software"),
                List.of("DE"),
                List.of(),
                1, 1, 1, 0, 100, 0L, null, null, null,
                generatedAt, generatedAt, generatedAt, generatedAt
        );

        Path outputFile = tempDir.resolve("single-category.xlsx");
        writer.writeWorkbook(outputFile, List.of(company), job, "1.0.0", generatedAt);

        try (InputStream in = Files.newInputStream(outputFile);
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            assertEquals(3, workbook.getNumberOfSheets());
            assertEquals("Companies", workbook.getSheetAt(0).getSheetName());
            assertEquals("With Emails", workbook.getSheetAt(1).getSheetName());
            assertEquals("Without Emails", workbook.getSheetAt(2).getSheetName());
            assertEquals("Company Name", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("Alpha Analytics GmbH", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            assertEquals("Branch ID", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            assertEquals("City", workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue());
            assertEquals("Address", workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue());
            assertEquals("Founder / CEO", workbook.getSheetAt(0).getRow(0).getCell(7).getStringCellValue());
            assertEquals("Branch Manager", workbook.getSheetAt(0).getRow(0).getCell(8).getStringCellValue());
            assertEquals("Friedrichstrasse 1, Berlin",
                    workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertNotNull(workbook.getSheetAt(0).getCTWorksheet().getAutoFilter());
        }
    }
}
