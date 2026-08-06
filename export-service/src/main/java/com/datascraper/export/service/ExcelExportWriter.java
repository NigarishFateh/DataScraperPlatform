package com.datascraper.export.service;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.job.JobResponse;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@Component
public class ExcelExportWriter {

    private static final String CREATOR = "Global Business Intelligence Platform";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss' UTC'").withZone(ZoneOffset.UTC);

    private static final String[] COMPANY_HEADERS = {
            "Company Name",
            "City",
            "Website",
            "Email",
            "Phone Number",
            "Founder Name"
    };

    private static final int COL_WEBSITE = 2;
    private static final int SAMPLE_AUTO_SIZE_ROWS = 100;
    private static final int STREAM_WINDOW_SIZE = 100;
    private static final int MAX_COLUMN_WIDTH = 256 * 55;
    private static final int HEADER_ROW_HEIGHT = 20;
    private static final int DATA_ROW_HEIGHT = 18;
    private static final int META_ROW_HEIGHT = 17;

    /** Preferred column widths (Excel character units) for the Companies sheet. */
    private static final int[] COMPANY_COLUMN_WIDTH_CHARS = {32, 18, 36, 28, 16, 24};

    private static final Map<String, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry("PK", "Pakistan"),
            Map.entry("US", "United-States"),
            Map.entry("GB", "United-Kingdom"),
            Map.entry("IN", "India"),
            Map.entry("DE", "Germany"),
            Map.entry("AE", "United-Arab-Emirates"),
            Map.entry("SA", "Saudi-Arabia"),
            Map.entry("CA", "Canada"),
            Map.entry("AU", "Australia"),
            Map.entry("FR", "France"),
            Map.entry("NL", "Netherlands"),
            Map.entry("SG", "Singapore")
    );

    private static final Map<String, String> CATEGORY_NAMES = Map.ofEntries(
            Map.entry("ai", "Artificial-Intelligence"),
            Map.entry("ml", "Machine-Learning"),
            Map.entry("software", "Software"),
            Map.entry("software-dev", "Software-Development"),
            Map.entry("cybersecurity", "Cybersecurity"),
            Map.entry("fintech", "Fintech"),
            Map.entry("cleaning", "Cleaning"),
            Map.entry("automation", "Automation"),
            Map.entry("it", "IT")
    );

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
            applyWorkbookProperties(workbook, appVersion, generatedAt, job);

            Styles styles = createStyles(workbook);
            ColumnWidthTracker widthTracker = new ColumnWidthTracker(COMPANY_HEADERS.length);

            writeCompaniesSheet(workbook, styles, widthTracker, companies);
            writeSearchCriteriaSheet(workbook, styles, job);
            writeExportSummarySheet(workbook, styles, companies, job, appVersion, generatedAt);

            try (OutputStream out = Files.newOutputStream(outputFile)) {
                workbook.write(out);
            }
            workbook.dispose();

            long fileSize = Files.size(outputFile);
            return new ExportWriteResult(companies.size(), fileSize);
        }
    }

    public static String buildDownloadFileName(JobResponse job) {
        String country = resolveCountryLabel(job);
        String category = resolveCategoryLabel(job);
        String base = sanitizeFileToken(country) + "_" + sanitizeFileToken(category);
        if (base.equals("_") || base.isBlank()) {
            base = "Company-Export";
        }
        return base + ".xlsx";
    }

    private static String resolveCountryLabel(JobResponse job) {
        if (job == null || job.countryCodes() == null || job.countryCodes().isEmpty()) {
            return "Global";
        }
        StringJoiner joiner = new StringJoiner("-");
        for (String code : job.countryCodes()) {
            if (code == null || code.isBlank()) {
                continue;
            }
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            joiner.add(COUNTRY_NAMES.getOrDefault(normalized, normalized));
        }
        String value = joiner.toString();
        return value.isBlank() ? "Global" : value;
    }

    private static String resolveCategoryLabel(JobResponse job) {
        if (job == null || job.categoryIds() == null || job.categoryIds().isEmpty()) {
            return "Companies";
        }
        StringJoiner joiner = new StringJoiner("-");
        for (String id : job.categoryIds()) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String key = id.trim().toLowerCase(Locale.ROOT);
            joiner.add(CATEGORY_NAMES.getOrDefault(key, humanizeToken(id)));
        }
        String value = joiner.toString();
        return value.isBlank() ? "Companies" : value;
    }

    private static String humanizeToken(String value) {
        String cleaned = value.trim().replace('_', '-');
        String[] parts = cleaned.split("[-\\s]+");
        StringJoiner joiner = new StringJoiner("-");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            joiner.add(Character.toUpperCase(part.charAt(0)) + part.substring(1).toLowerCase(Locale.ROOT));
        }
        return joiner.toString();
    }

    private static String sanitizeFileToken(String value) {
        if (value == null || value.isBlank()) {
            return "Export";
        }
        String cleaned = value.trim()
                .replaceAll("[\\\\/:*?\"<>|]+", "-")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
        while (cleaned.startsWith("-") || cleaned.endsWith("-")) {
            if (cleaned.startsWith("-")) {
                cleaned = cleaned.substring(1);
            }
            if (cleaned.endsWith("-")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
        }
        return cleaned.isBlank() ? "Export" : cleaned;
    }

    private void applyWorkbookProperties(
            SXSSFWorkbook workbook,
            String appVersion,
            Instant generatedAt,
            JobResponse job
    ) {
        XSSFWorkbook xssfWorkbook = workbook.getXSSFWorkbook();
        var core = xssfWorkbook.getProperties().getCoreProperties();
        core.setCreator(CREATOR);
        core.setTitle(buildDownloadFileName(job).replace(".xlsx", ""));
        core.setDescription("Scraped company export");
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
        sheet.setDisplayGridlines(false);

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(HEADER_ROW_HEIGHT);
        for (int col = 0; col < COMPANY_HEADERS.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(COMPANY_HEADERS[col]);
            cell.setCellStyle(styles.header());
            widthTracker.track(col, COMPANY_HEADERS[col], 0);
        }

        int rowIndex = 1;
        for (EnrichedCompany company : companies) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(DATA_ROW_HEIGHT);
            CellStyle rowStyle = rowIndex % 2 == 0 ? styles.evenRow() : styles.oddRow();
            writeCompanyRow(workbook, row, rowStyle, styles.linkFont(), widthTracker, company, rowIndex);
            rowIndex++;
        }

        int lastDataRow = Math.max(rowIndex - 1, 0);
        sheet.setAutoFilter(new CellRangeAddress(0, lastDataRow, 0, COMPANY_HEADERS.length - 1));
        applyCompanyColumnWidths(sheet, widthTracker);
    }

    private void applyCompanyColumnWidths(SXSSFSheet sheet, ColumnWidthTracker widthTracker) {
        for (int col = 0; col < COMPANY_HEADERS.length; col++) {
            int minChars = COMPANY_COLUMN_WIDTH_CHARS[col];
            int trackedChars = widthTracker.charsForColumn(col);
            int widthChars = Math.max(minChars, trackedChars);
            sheet.setColumnWidth(col, Math.min(widthChars * 256, MAX_COLUMN_WIDTH));
        }
    }

    private void writeCompanyRow(
            SXSSFWorkbook workbook,
            Row row,
            CellStyle rowStyle,
            Font linkFont,
            ColumnWidthTracker widthTracker,
            EnrichedCompany company,
            int rowIndex
    ) {
        CreationHelper helper = workbook.getCreationHelper();
        String founder = firstNonBlank(company.founder(), company.ceo());
        String[] values = {
                company.name(),
                company.city(),
                company.website(),
                company.email(),
                company.phone(),
                founder
        };

        for (int col = 0; col < values.length; col++) {
            Cell cell = row.createCell(col);
            HorizontalAlignment alignment = horizontalAlignmentForColumn(col);
            CellStyle cellStyle = rowStyle;
            if (alignment != HorizontalAlignment.LEFT) {
                cellStyle = workbook.createCellStyle();
                cellStyle.cloneStyleFrom(rowStyle);
                cellStyle.setAlignment(alignment);
            }
            cell.setCellStyle(cellStyle);
            String value = values[col];
            if (value == null || value.isBlank()) {
                cell.setBlank();
            } else if (isUrlColumn(col) && looksLikeUrl(value)) {
                cell.setCellValue(value);
                Hyperlink link = helper.createHyperlink(HyperlinkType.URL);
                link.setAddress(normalizeUrl(value));
                cell.setHyperlink(link);
                CellStyle linkStyle = workbook.createCellStyle();
                linkStyle.cloneStyleFrom(cellStyle);
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

    private static HorizontalAlignment horizontalAlignmentForColumn(int col) {
        return switch (col) {
            case 4 -> HorizontalAlignment.CENTER;
            default -> HorizontalAlignment.LEFT;
        };
    }

    private boolean isUrlColumn(int col) {
        return col == COL_WEBSITE;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static boolean looksLikeUrl(String value) {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("www.");
    }

    private static String normalizeUrl(String value) {
        String trimmed = value.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("www.")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private void writeSearchCriteriaSheet(SXSSFWorkbook workbook, Styles styles, JobResponse job) {
        Sheet sheet = workbook.createSheet("Search Criteria");
        sheet.setDisplayGridlines(false);
        int rowIndex = 0;
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Countries", resolveCountryLabel(job).replace('-', ' '));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Categories", resolveCategoryLabel(job).replace('-', ' '));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Country Codes", joinList(job.countryCodes()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Category IDs", joinList(job.categoryIds()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "City IDs", joinList(job.cityIds()));
        writeLabelValueRow(sheet, rowIndex, styles, "Job ID", job.id() != null ? job.id().toString() : "");

        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 52 * 256);
    }

    private void writeExportSummarySheet(
            SXSSFWorkbook workbook,
            Styles styles,
            List<EnrichedCompany> companies,
            JobResponse job,
            String appVersion,
            Instant generatedAt
    ) {
        Sheet sheet = workbook.createSheet("Export Summary");
        sheet.setDisplayGridlines(false);
        int rowIndex = 0;
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "File Scope",
                resolveCountryLabel(job).replace('-', ' ') + " / " + resolveCategoryLabel(job).replace('-', ' '));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Company Count", String.valueOf(companies.size()));
        rowIndex = writeLabelValueRow(sheet, rowIndex, styles, "Generated At", TIMESTAMP_FORMAT.format(generatedAt));
        writeLabelValueRow(sheet, rowIndex, styles, "Version", appVersion);

        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 52 * 256);
    }

    private int writeLabelValueRow(Sheet sheet, int rowIndex, Styles styles, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(META_ROW_HEIGHT);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.metaLabel());

        Cell valueCell = row.createCell(1);
        if (value == null || value.isBlank()) {
            valueCell.setBlank();
        } else {
            valueCell.setCellValue(value);
        }
        valueCell.setCellStyle(rowIndex % 2 == 0 ? styles.metaValueEven() : styles.metaValueOdd());
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
        headerStyle.setFillForegroundColor(IndexedColors.TEAL.index);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(false);
        applyBorders(headerStyle);

        Font dataFont = workbook.createFont();
        dataFont.setFontName("Calibri");
        dataFont.setFontHeightInPoints((short) 11);

        Font linkFont = workbook.createFont();
        linkFont.setFontName("Calibri");
        linkFont.setFontHeightInPoints((short) 11);
        linkFont.setUnderline(Font.U_SINGLE);
        linkFont.setColor(IndexedColors.BLUE.index);

        CellStyle oddRowStyle = workbook.createCellStyle();
        oddRowStyle.setFont(dataFont);
        oddRowStyle.setFillForegroundColor(IndexedColors.WHITE.index);
        oddRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        oddRowStyle.setWrapText(false);
        oddRowStyle.setAlignment(HorizontalAlignment.LEFT);
        oddRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(oddRowStyle);

        CellStyle evenRowStyle = workbook.createCellStyle();
        evenRowStyle.setFont(dataFont);
        evenRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        evenRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        evenRowStyle.setWrapText(false);
        evenRowStyle.setAlignment(HorizontalAlignment.LEFT);
        evenRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(evenRowStyle);

        Font metaLabelFont = workbook.createFont();
        metaLabelFont.setBold(true);
        metaLabelFont.setFontName("Calibri");
        metaLabelFont.setFontHeightInPoints((short) 11);
        metaLabelFont.setColor(IndexedColors.GREY_80_PERCENT.index);

        CellStyle metaLabelStyle = workbook.createCellStyle();
        metaLabelStyle.setFont(metaLabelFont);
        metaLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        metaLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        metaLabelStyle.setAlignment(HorizontalAlignment.LEFT);
        metaLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        metaLabelStyle.setIndention((short) 1);
        applyBorders(metaLabelStyle);

        CellStyle metaValueOddStyle = workbook.createCellStyle();
        metaValueOddStyle.setFont(dataFont);
        metaValueOddStyle.setFillForegroundColor(IndexedColors.WHITE.index);
        metaValueOddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        metaValueOddStyle.setAlignment(HorizontalAlignment.LEFT);
        metaValueOddStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        metaValueOddStyle.setWrapText(true);
        applyBorders(metaValueOddStyle);

        CellStyle metaValueEvenStyle = workbook.createCellStyle();
        metaValueEvenStyle.setFont(dataFont);
        metaValueEvenStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        metaValueEvenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        metaValueEvenStyle.setAlignment(HorizontalAlignment.LEFT);
        metaValueEvenStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        metaValueEvenStyle.setWrapText(true);
        applyBorders(metaValueEvenStyle);

        return new Styles(
                headerStyle,
                oddRowStyle,
                evenRowStyle,
                linkFont,
                metaLabelStyle,
                metaValueOddStyle,
                metaValueEvenStyle
        );
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.index);
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.index);
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.index);
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.index);
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

    private record Styles(
            CellStyle header,
            CellStyle oddRow,
            CellStyle evenRow,
            Font linkFont,
            CellStyle metaLabel,
            CellStyle metaValueOdd,
            CellStyle metaValueEven
    ) {
    }

    public record ExportWriteResult(long rowCount, long fileSizeBytes) {
    }

    private static final class ColumnWidthTracker {
        private final int[] maxWidths;

        ColumnWidthTracker(int columnCount) {
            this.maxWidths = new int[columnCount];
        }

        void track(int column, String value, int rowIndex) {
            int length = value == null ? 0 : Math.min(value.length(), 80);
            maxWidths[column] = Math.max(maxWidths[column], length);
        }

        int charsForColumn(int column) {
            return maxWidths[column] + 2;
        }
    }
}
