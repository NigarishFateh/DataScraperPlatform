package com.datascraper.export.service;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.job.JobResponse;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ooxml.POIXMLProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

@Component
public class ExcelExportWriter {

    private static final String CREATOR = "Global Business Intelligence Platform";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss' UTC'").withZone(ZoneOffset.UTC);

    private static final String[] COMPANY_HEADERS = {
            "Company Name", "Category", "Industry", "Country", "State", "City",
            "Website", "Email", "Phone", "Founder", "CEO", "Description", "Services", "Products",
            "Technology Stack", "LinkedIn", "GitHub", "Facebook", "Twitter/X", "Instagram", "YouTube",
            "Founded Year", "Employee Count", "Address", "Contact Page", "Source URL",
            "Scraped Timestamp", "Confidence Score", "Provider Name", "Notes"
    };

    private static final int CONFIDENCE_COLUMN_INDEX = 27;
    private static final int SAMPLE_AUTO_SIZE_ROWS = 100;
    private static final int STREAM_WINDOW_SIZE = 100;
    private static final int MAX_COLUMN_WIDTH = 256 * 60;

    public ExportWriteResult writeWorkbook(
            Path outputFile,
            List<EnrichedCompany> companies,
            JobResponse job,
            String appVersion,
            Instant generatedAt
    ) throws IOException {
        Files.createDirectories(outputFile.getParent());

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAM_WINDOW_SIZE)) {
            workbook.setCompressTempFiles(true);
            applyWorkbookProperties(workbook, appVersion, generatedAt);

            Styles styles = createStyles(workbook);
            ColumnWidthTracker widthTracker = new ColumnWidthTracker(COMPANY_HEADERS.length);

            writeCompaniesSheet(workbook, styles, widthTracker, companies);
            writeSearchCriteriaSheet(workbook, styles, job);
            writeExportSummarySheet(workbook, styles, companies, appVersion, generatedAt);
            writeJobStatisticsSheet(workbook, styles, job);

            try (OutputStream out = Files.newOutputStream(outputFile)) {
                workbook.write(out);
            }
            workbook.dispose();

            long fileSize = Files.size(outputFile);
            return new ExportWriteResult(companies.size(), fileSize);
        }
    }

    private void applyWorkbookProperties(SXSSFWorkbook workbook, String appVersion, Instant generatedAt) {
        XSSFWorkbook xssfWorkbook = workbook.getXSSFWorkbook();
        var core = xssfWorkbook.getProperties().getCoreProperties();
        core.setCreator(CREATOR);
        core.setTitle("Company Export");
        core.setDescription("Generated company intelligence export");
        core.setCreated(Optional.of(Date.from(generatedAt)));

        var extended = xssfWorkbook.getProperties().getExtendedProperties();
        extended.getUnderlyingProperties().setApplication(CREATOR);
        extended.getUnderlyingProperties().setAppVersion(appVersion);

        POIXMLProperties.CustomProperties customProperties =
                xssfWorkbook.getProperties().getCustomProperties();
        customProperties.addProperty("GeneratedAt", TIMESTAMP_FORMAT.format(generatedAt));
    }

    private void writeCompaniesSheet(
            SXSSFWorkbook workbook,
            Styles styles,
            ColumnWidthTracker widthTracker,
            List<EnrichedCompany> companies
    ) {
        SXSSFSheet sheet = workbook.createSheet("Companies");
        sheet.createFreezePane(0, 1);

        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < COMPANY_HEADERS.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(COMPANY_HEADERS[col]);
            cell.setCellStyle(styles.header());
            widthTracker.track(col, COMPANY_HEADERS[col], 0);
        }

        int rowIndex = 1;
        for (EnrichedCompany company : companies) {
            Row row = sheet.createRow(rowIndex);
            CellStyle rowStyle = rowIndex % 2 == 0 ? styles.evenRow() : styles.oddRow();
            writeCompanyRow(workbook, row, rowStyle, widthTracker, company, rowIndex);
            rowIndex++;
        }

        int lastDataRow = Math.max(rowIndex - 1, 0);
        sheet.setAutoFilter(new CellRangeAddress(0, lastDataRow, 0, COMPANY_HEADERS.length - 1));
        applyConfidenceConditionalFormatting(sheet, lastDataRow);
        widthTracker.applyToSheet(sheet);
    }

    private void writeCompanyRow(
            SXSSFWorkbook workbook,
            Row row,
            CellStyle rowStyle,
            ColumnWidthTracker widthTracker,
            EnrichedCompany company,
            int rowIndex
    ) {
        CreationHelper helper = workbook.getCreationHelper();
        String[] values = {
                company.name(),
                company.category(),
                company.industry(),
                company.countryName(),
                company.state(),
                company.city(),
                company.website(),
                company.email(),
                company.phone(),
                company.founder(),
                company.ceo(),
                company.description(),
                company.services(),
                company.products(),
                joinList(company.technologyStack()),
                company.linkedIn(),
                company.github(),
                company.facebook(),
                company.twitter(),
                company.instagram(),
                company.youtube(),
                company.foundedYear() != null ? company.foundedYear().toString() : null,
                company.employeeCount(),
                company.address(),
                company.contactPage(),
                company.sourceUrl(),
                company.scrapedAt() != null ? TIMESTAMP_FORMAT.format(company.scrapedAt()) : null,
                null,
                company.providerName(),
                company.notes()
        };

        for (int col = 0; col < values.length; col++) {
            Cell cell = row.createCell(col);
            cell.setCellStyle(rowStyle);

            if (col == CONFIDENCE_COLUMN_INDEX) {
                cell.setCellValue(company.confidenceScore());
                if (rowIndex <= SAMPLE_AUTO_SIZE_ROWS) {
                    widthTracker.track(col, String.valueOf(company.confidenceScore()), rowIndex);
                }
                continue;
            }

            String value = values[col];
            if (value == null || value.isBlank()) {
                cell.setBlank();
            } else if (isUrlColumn(col)) {
                cell.setCellValue(value);
                Hyperlink link = helper.createHyperlink(HyperlinkType.URL);
                link.setAddress(value);
                cell.setHyperlink(link);
                CellStyle linkStyle = workbook.createCellStyle();
                linkStyle.cloneStyleFrom(rowStyle);
                Font linkFont = workbook.createFont();
                linkFont.setUnderline(Font.U_SINGLE);
                linkFont.setColor(IndexedColors.BLUE.index);
                linkFont.setFontName("Calibri");
                linkStyle.setFont(linkFont);
                cell.setCellStyle(linkStyle);
            } else {
                cell.setCellValue(value);
            }

            if (rowIndex <= SAMPLE_AUTO_SIZE_ROWS) {
                widthTracker.track(col, value, rowIndex);
            }
        }
    }

    private boolean isUrlColumn(int col) {
        return col == 6 || col == 15 || col == 16 || col == 17 || col == 18 || col == 19 || col == 20
                || col == 24 || col == 25;
    }

    private void applyConfidenceConditionalFormatting(SXSSFSheet sheet, int lastDataRow) {
        if (lastDataRow < 1) {
            return;
        }

        SheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
        String columnLetter = columnLetter(CONFIDENCE_COLUMN_INDEX);
        CellRangeAddress[] ranges = {
                CellRangeAddress.valueOf(columnLetter + "2:" + columnLetter + (lastDataRow + 1))
        };

        ConditionalFormattingRule lowRule =
                cf.createConditionalFormattingRule(ComparisonOperator.LT, "0.5");
        PatternFormatting lowFill = lowRule.createPatternFormatting();
        lowFill.setFillBackgroundColor(IndexedColors.ROSE.index);
        lowFill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        ConditionalFormattingRule highRule =
                cf.createConditionalFormattingRule(ComparisonOperator.GE, "0.8");
        PatternFormatting highFill = highRule.createPatternFormatting();
        highFill.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
        highFill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        cf.addConditionalFormatting(ranges, lowRule, highRule);
    }

    private void writeSearchCriteriaSheet(SXSSFWorkbook workbook, Styles styles, JobResponse job) {
        Sheet sheet = workbook.createSheet("Search Criteria");
        int rowIndex = 0;

        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Job ID", job.id() != null ? job.id().toString() : "");
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "User ID", nullToEmpty(job.userId()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Status", job.status() != null ? job.status().name() : "");
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Phase", job.phase() != null ? job.phase().name() : "");
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Category IDs", joinList(job.categoryIds()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Country Codes", joinList(job.countryCodes()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "City IDs", joinList(job.cityIds()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Created At",
                job.createdAt() != null ? TIMESTAMP_FORMAT.format(job.createdAt()) : "");
        writeLabelValueRow(sheet, rowIndex, styles, "Started At",
                job.startedAt() != null ? TIMESTAMP_FORMAT.format(job.startedAt()) : "");

        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 48 * 256);
    }

    private void writeExportSummarySheet(
            SXSSFWorkbook workbook,
            Styles styles,
            List<EnrichedCompany> companies,
            String appVersion,
            Instant generatedAt
    ) {
        Sheet sheet = workbook.createSheet("Export Summary");
        Set<String> categories = new HashSet<>();
        Set<String> countries = new HashSet<>();
        for (EnrichedCompany company : companies) {
            if (company.category() != null && !company.category().isBlank()) {
                categories.add(company.category());
            }
            if (company.countryName() != null && !company.countryName().isBlank()) {
                countries.add(company.countryName());
            } else if (company.countryCode() != null && !company.countryCode().isBlank()) {
                countries.add(company.countryCode());
            }
        }

        int rowIndex = 0;
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Company Count", String.valueOf(companies.size()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Category Count", String.valueOf(categories.size()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Country Count", String.valueOf(countries.size()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Generated At", TIMESTAMP_FORMAT.format(generatedAt));
        writeLabelValueRow(sheet, rowIndex, styles, "Version", appVersion);

        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 32 * 256);
    }

    private void writeJobStatisticsSheet(SXSSFWorkbook workbook, Styles styles, JobResponse job) {
        Sheet sheet = workbook.createSheet("Job Statistics");
        int rowIndex = 0;

        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Discovered Count", String.valueOf(job.discoveredCount()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Enriched Count", String.valueOf(job.enrichedCount()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Persisted Count", String.valueOf(job.persistedCount()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Failed Count", String.valueOf(job.failedCount()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Progress Percent", String.valueOf(job.progressPercent()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Estimated Remaining Seconds",
                job.estimatedRemainingSeconds() != null ? job.estimatedRemainingSeconds().toString() : "");
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Checkpoint", nullToEmpty(job.checkpoint()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Error Message", nullToEmpty(job.errorMessage()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Updated At",
                job.updatedAt() != null ? TIMESTAMP_FORMAT.format(job.updatedAt()) : "");
        writeLabelValueRow(sheet, rowIndex, styles, "Completed At",
                job.completedAt() != null ? TIMESTAMP_FORMAT.format(job.completedAt()) : "");

        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 24 * 256);
    }

    private int writeLabelValueRow(Sheet sheet, int rowIndex, Styles styles, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.header());

        Cell valueCell = row.createCell(1);
        if (value == null || value.isBlank()) {
            valueCell.setBlank();
        } else {
            valueCell.setCellValue(value);
        }
        valueCell.setCellStyle(styles.evenRow());
        return rowIndex + 1;
    }

    private Styles createStyles(SXSSFWorkbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontName("Calibri");
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.WHITE.index);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.index);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);
        applyBorders(headerStyle);

        Font dataFont = workbook.createFont();
        dataFont.setFontName("Calibri");
        dataFont.setFontHeightInPoints((short) 11);

        CellStyle oddRowStyle = workbook.createCellStyle();
        oddRowStyle.setFont(dataFont);
        oddRowStyle.setFillForegroundColor(IndexedColors.WHITE.index);
        oddRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        oddRowStyle.setWrapText(true);
        oddRowStyle.setVerticalAlignment(VerticalAlignment.TOP);
        applyBorders(oddRowStyle);

        CellStyle evenRowStyle = workbook.createCellStyle();
        evenRowStyle.setFont(dataFont);
        evenRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        evenRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        evenRowStyle.setWrapText(true);
        evenRowStyle.setVerticalAlignment(VerticalAlignment.TOP);
        applyBorders(evenRowStyle);

        return new Styles(headerStyle, oddRowStyle, evenRowStyle);
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_50_PERCENT.index);
        style.setBottomBorderColor(IndexedColors.GREY_50_PERCENT.index);
        style.setLeftBorderColor(IndexedColors.GREY_50_PERCENT.index);
        style.setRightBorderColor(IndexedColors.GREY_50_PERCENT.index);
    }

    private static String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value);
            }
        }
        return joiner.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String columnLetter(int columnIndex) {
        StringBuilder builder = new StringBuilder();
        int index = columnIndex;
        while (index >= 0) {
            builder.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        }
        return builder.toString();
    }

    private record Styles(CellStyle header, CellStyle oddRow, CellStyle evenRow) {
    }

    public record ExportWriteResult(long rowCount, long fileSizeBytes) {
    }

    static final class ColumnWidthTracker {
        private final int[] maxWidths;

        ColumnWidthTracker(int columnCount) {
            this.maxWidths = new int[columnCount];
        }

        void track(int column, String value, int rowIndex) {
            if (value == null || value.isBlank()) {
                return;
            }
            int length = Math.min(value.length() + 2, 60);
            if (rowIndex <= SAMPLE_AUTO_SIZE_ROWS) {
                maxWidths[column] = Math.max(maxWidths[column], length);
            } else if (maxWidths[column] == 0) {
                maxWidths[column] = Math.min(length, 30);
            } else {
                maxWidths[column] = Math.max(maxWidths[column], Math.min(length, maxWidths[column] + 1));
            }
        }

        void applyToSheet(Sheet sheet) {
            for (int col = 0; col < maxWidths.length; col++) {
                int width = maxWidths[col] > 0 ? maxWidths[col] : 12;
                sheet.setColumnWidth(col, Math.min(width * 256, MAX_COLUMN_WIDTH));
            }
        }
    }
}
