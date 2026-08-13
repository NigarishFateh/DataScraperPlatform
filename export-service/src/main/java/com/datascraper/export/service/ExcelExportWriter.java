package com.datascraper.export.service;

import com.datascraper.common.dto.company.EnrichedCompany;
import com.datascraper.common.dto.job.JobResponse;
import com.datascraper.common.support.CompanyEmailSupport;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

@Component
public class ExcelExportWriter {

    private static final String CREATOR = "Global Business Intelligence Platform";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss' UTC'").withZone(ZoneOffset.UTC);

    private static final String SHEET_ALL = "Companies";
    private static final String SHEET_WITH_EMAILS = "With Emails";
    private static final String SHEET_WITHOUT_EMAILS = "Without Emails";

    private static final String[] COMPANY_HEADERS = {
            "Company Name",
            "Branch ID",
            "City",
            "Address",
            "Website",
            "Email",
            "Phone Number",
            "Founder / CEO"
    };

    private static final int COL_WEBSITE = 4;
    private static final int SAMPLE_AUTO_SIZE_ROWS = 100;
    private static final int STREAM_WINDOW_SIZE = 100;
    private static final int MAX_COLUMN_WIDTH = 256 * 55;
    private static final int HEADER_ROW_HEIGHT = 20;
    private static final int DATA_ROW_HEIGHT = 18;

    /** Preferred column widths (Excel character units) for company sheets. */
    private static final int[] COMPANY_COLUMN_WIDTH_CHARS = {28, 18, 16, 36, 32, 26, 16, 22};

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

        List<String> jobCategories = job == null || job.categoryIds() == null
                ? List.of()
                : job.categoryIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAM_WINDOW_SIZE)) {
            workbook.setCompressTempFiles(true);
            applyWorkbookProperties(workbook, appVersion, generatedAt, job);

            Styles styles = createStyles(workbook);

            if (jobCategories.size() > 1) {
                writeMultiCategoryPartitions(workbook, styles, companies, jobCategories);
            } else {
                List<EnrichedCompany> withEmails = new ArrayList<>();
                List<EnrichedCompany> withoutEmails = new ArrayList<>();
                for (EnrichedCompany company : companies) {
                    if (CompanyEmailSupport.hasEmail(company.email())) {
                        withEmails.add(company);
                    } else {
                        withoutEmails.add(company);
                    }
                }
                writeCompaniesSheet(workbook, styles, SHEET_ALL, companies, null);
                writeCompaniesSheet(workbook, styles, SHEET_WITH_EMAILS, withEmails, null);
                writeCompaniesSheet(workbook, styles, SHEET_WITHOUT_EMAILS, withoutEmails, null);
            }

            try (OutputStream out = Files.newOutputStream(outputFile)) {
                workbook.write(out);
            }
            workbook.dispose();

            long fileSize = Files.size(outputFile);
            return new ExportWriteResult(companies.size(), fileSize);
        }
    }

    /**
     * One sheet partition per selected category (label in sheet name + banner row).
     * Companies are placed in the first matching category partition only.
     */
    private void writeMultiCategoryPartitions(
            SXSSFWorkbook workbook,
            Styles styles,
            List<EnrichedCompany> companies,
            List<String> jobCategories
    ) {
        Map<String, List<EnrichedCompany>> byCategory = new LinkedHashMap<>();
        for (String categoryId : jobCategories) {
            byCategory.put(categoryId, new ArrayList<>());
        }
        List<EnrichedCompany> unassigned = new ArrayList<>();

        for (EnrichedCompany company : companies) {
            String matched = firstMatchingCategory(company, jobCategories);
            if (matched != null) {
                byCategory.get(matched).add(company);
            } else {
                unassigned.add(company);
            }
        }

        Set<String> usedSheetNames = new LinkedHashSet<>();
        for (String categoryId : jobCategories) {
            String label = categoryDisplayName(categoryId);
            String sheetName = uniqueSheetName(label, usedSheetNames);
            writeCompaniesSheet(workbook, styles, sheetName, byCategory.get(categoryId), label);
        }
        if (!unassigned.isEmpty()) {
            String sheetName = uniqueSheetName("Other", usedSheetNames);
            writeCompaniesSheet(workbook, styles, sheetName, unassigned, "Other / Unassigned");
        }
    }

    private static String firstMatchingCategory(EnrichedCompany company, List<String> jobCategories) {
        if (company.categoryIds() != null) {
            for (String jobCategory : jobCategories) {
                for (String companyCategory : company.categoryIds()) {
                    if (companyCategory != null && companyCategory.equalsIgnoreCase(jobCategory)) {
                        return jobCategory;
                    }
                }
            }
        }
        String haystack = ((company.category() == null ? "" : company.category()) + " "
                + (company.industry() == null ? "" : company.industry())).toLowerCase(Locale.ROOT);
        for (String jobCategory : jobCategories) {
            String label = categoryDisplayName(jobCategory).toLowerCase(Locale.ROOT).replace('-', ' ');
            String id = jobCategory.toLowerCase(Locale.ROOT).replace('-', ' ');
            if ((!label.isBlank() && haystack.contains(label)) || (!id.isBlank() && haystack.contains(id))) {
                return jobCategory;
            }
        }
        return null;
    }

    private static String categoryDisplayName(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return "Category";
        }
        String key = categoryId.trim().toLowerCase(Locale.ROOT);
        return CATEGORY_NAMES.getOrDefault(key, humanizeToken(categoryId));
    }

    private static String uniqueSheetName(String preferred, Set<String> used) {
        String base = sanitizeSheetName(preferred);
        String candidate = base;
        int suffix = 2;
        while (!used.add(candidate)) {
            String truncated = base.length() > 28 ? base.substring(0, 28) : base;
            candidate = truncated + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String sanitizeSheetName(String value) {
        if (value == null || value.isBlank()) {
            return "Sheet";
        }
        String cleaned = value.trim()
                .replaceAll("[\\\\/?*\\[\\]:]+", "-")
                .replaceAll("\\s+", " ");
        if (cleaned.length() > 31) {
            cleaned = cleaned.substring(0, 31);
        }
        return cleaned.isBlank() ? "Sheet" : cleaned;
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
            String sheetName,
            List<EnrichedCompany> companies,
            String categoryBanner
    ) {
        ColumnWidthTracker widthTracker = new ColumnWidthTracker(COMPANY_HEADERS.length);
        SXSSFSheet sheet = workbook.createSheet(sheetName);
        int headerRowIndex = 0;

        if (categoryBanner != null && !categoryBanner.isBlank()) {
            Row bannerRow = sheet.createRow(0);
            bannerRow.setHeightInPoints(HEADER_ROW_HEIGHT);
            Cell bannerCell = bannerRow.createCell(0);
            bannerCell.setCellValue("Category: " + categoryBanner);
            bannerCell.setCellStyle(styles.header());
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COMPANY_HEADERS.length - 1));
            headerRowIndex = 1;
        }

        sheet.createFreezePane(0, headerRowIndex + 1);
        sheet.setDisplayGridlines(false);

        Row headerRow = sheet.createRow(headerRowIndex);
        headerRow.setHeightInPoints(HEADER_ROW_HEIGHT);
        for (int col = 0; col < COMPANY_HEADERS.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(COMPANY_HEADERS[col]);
            cell.setCellStyle(styles.header());
            widthTracker.track(col, COMPANY_HEADERS[col], 0);
        }

        int rowIndex = headerRowIndex + 1;
        for (EnrichedCompany company : companies) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(DATA_ROW_HEIGHT);
            CellStyle rowStyle = rowIndex % 2 == 0 ? styles.evenRow() : styles.oddRow();
            writeCompanyRow(workbook, row, rowStyle, styles.linkFont(), widthTracker, company, rowIndex);
            rowIndex++;
        }

        int lastDataRow = Math.max(rowIndex - 1, headerRowIndex);
        sheet.setAutoFilter(new CellRangeAddress(
                headerRowIndex,
                lastDataRow,
                0,
                COMPANY_HEADERS.length - 1
        ));
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
                branchId(company),
                displayCity(company),
                company.address(),
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
            case 6 -> HorizontalAlignment.CENTER; // Phone Number
            default -> HorizontalAlignment.LEFT;
        };
    }

    /**
     * Google Places id when present; otherwise a stable BR-xxxxxxxx from name+city+address.
     */
    static String branchId(EnrichedCompany company) {
        if (company == null) {
            return "";
        }
        Object rawPlace = company.rawAttributes() == null ? null : company.rawAttributes().get("placeId");
        if (rawPlace == null) {
            rawPlace = company.rawAttributes() == null ? null : company.rawAttributes().get("branchId");
        }
        if (rawPlace != null && !rawPlace.toString().isBlank()) {
            String id = rawPlace.toString().trim();
            if (id.regionMatches(true, 0, "places/", 0, 7)) {
                id = id.substring(7);
            }
            return id;
        }
        String basis = (company.name() == null ? "" : company.name().trim())
                + "|" + (company.city() == null ? "" : company.city().trim())
                + "|" + (company.address() == null ? "" : company.address().trim());
        String uuid = java.util.UUID.nameUUIDFromBytes(
                basis.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ).toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "BR-" + uuid;
    }

    /**
     * City column must be a place name. URLs, country names, and postcodes look like
     * black/grey bars or hyperlinks in Excel when the column is narrow.
     */
    private static String displayCity(EnrichedCompany company) {
        String city = sanitizeCity(company.city(), company.countryName(), company.countryCode());
        if (city != null) {
            return city;
        }
        return sanitizeCity(cityFromAddress(company.address()), company.countryName(), company.countryCode());
    }

    private static String sanitizeCity(String raw, String countryName, String countryCode) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (looksLikeUrl(value) || value.contains("://") || value.contains("/")) {
            return null;
        }
        value = value.replaceFirst("(?i)^\\d{4}\\s*[A-Z]{2}\\s+", "");
        value = value.replaceFirst("^\\d{5}\\s+", "");
        if (value.isBlank() || isCountryLabel(value, countryName, countryCode) || value.length() > 40) {
            return null;
        }
        return value;
    }

    private static String cityFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String[] parts = address.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (part.isBlank()) {
                continue;
            }
            String cleaned = part.replaceFirst("(?i)^\\d{4}\\s*[A-Z]{2}\\s+", "").trim();
            cleaned = cleaned.replaceFirst("^\\d{5}\\s+", "").trim();
            if (cleaned.isBlank() || cleaned.matches("(?i)netherlands|nederland|germany|deutschland|belgium|belgie")) {
                continue;
            }
            if (cleaned.matches("(?i)\\d{4}\\s*[A-Z]{2}") || cleaned.matches("\\d+")) {
                continue;
            }
            return cleaned;
        }
        return null;
    }

    private static boolean isCountryLabel(String value, String countryName, String countryCode) {
        String lower = value.trim().toLowerCase(Locale.ROOT);
        if (countryName != null && lower.equals(countryName.trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (countryCode != null && lower.equals(countryCode.trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        return lower.equals("netherlands") || lower.equals("nederland") || lower.equals("holland");
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

        return new Styles(headerStyle, oddRowStyle, evenRowStyle, linkFont);
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

    private record Styles(
            CellStyle header,
            CellStyle oddRow,
            CellStyle evenRow,
            Font linkFont
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
