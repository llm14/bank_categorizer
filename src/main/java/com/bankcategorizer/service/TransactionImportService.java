package com.bankcategorizer.service;

import com.bankcategorizer.dto.ImportResultResponse;
import com.bankcategorizer.dto.ParsedTransactionRow;
import com.bankcategorizer.exception.InvalidFileFormatException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.model.Transaction;
import com.bankcategorizer.repository.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses uploaded bank-transaction files (CSV or XLSX) into {@link Transaction} entities and
 * persists them. Each parsed row is matched against existing category keywords via
 * {@link CategorizationService}; transactions with no matching category are saved
 * uncategorized ({@code category} left {@code null}).
 *
 * <p><b>Assumption:</b> for now only a simple, single common layout is supported: the first
 * row is a header row containing (in any order, any casing) a date column, a description
 * column, and an amount column. Header aliases are matched case-insensitively:
 * <ul>
 *     <li>date: "date"</li>
 *     <li>description: "description", "desc"</li>
 *     <li>amount: "amount", "value"</li>
 * </ul>
 * Bank-specific export formats are not handled yet. Rows that cannot be parsed (bad date,
 * non-numeric amount, blank description, etc.) are skipped rather than failing the whole
 * import; the number of skipped rows is reported back to the caller.
 */
@Service
public class TransactionImportService {

    private static final Logger log = LoggerFactory.getLogger(TransactionImportService.class);

    private static final Set<String> DATE_HEADERS = Set.of("date");
    private static final Set<String> DESCRIPTION_HEADERS = Set.of("description", "desc");
    private static final Set<String> AMOUNT_HEADERS = Set.of("amount", "value");

    // Only whitespace and common currency symbols are treated as harmless noise around an
    // amount (e.g. "$ 45.30", "45,30 €"). Anything else left over after stripping those is
    // presumed to make the value genuinely malformed, so it's rejected rather than silently
    // dropped (dropping arbitrary characters, e.g. a stray letter, could corrupt the value
    // instead of causing the row to be skipped).
    private static final Pattern CURRENCY_NOISE = Pattern.compile("[\\s$€£¥]");
    private static final Pattern VALID_NUMERIC_AMOUNT = Pattern.compile("^-?[0-9]+([.,][0-9]+)*$");

    // Minimal set of date formats we accept; note this as an assumption rather than an
    // attempt to cover every bank's export locale/format.
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    );

    private final TransactionRepository transactionRepository;
    private final CategorizationService categorizationService;

    public TransactionImportService(TransactionRepository transactionRepository,
                                     CategorizationService categorizationService) {
        this.transactionRepository = transactionRepository;
        this.categorizationService = categorizationService;
    }

    public ImportResultResponse importTransactions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileFormatException("Uploaded file is empty");
        }

        FileType fileType = detectFileType(file);
        List<List<String>> rawRows = readRawRows(file, fileType);

        if (rawRows.isEmpty()) {
            return new ImportResultResponse(file.getOriginalFilename(), 0, 0, 0, 0, 0);
        }

        ColumnIndexes columnIndexes = resolveColumnIndexes(rawRows.get(0));

        List<ParsedTransactionRow> parsedRows = new ArrayList<>();
        int skippedCount = 0;
        List<List<String>> dataRows = rawRows.subList(1, rawRows.size());

        for (List<String> row : dataRows) {
            ParsedTransactionRow parsedRow = tryParseRow(row, columnIndexes);
            if (parsedRow != null) {
                parsedRows.add(parsedRow);
            } else {
                skippedCount++;
            }
        }

        List<Category> categories = parsedRows.isEmpty()
                ? List.of()
                : categorizationService.loadCategories();

        List<Transaction> transactions = new ArrayList<>(parsedRows.size());
        int categorizedCount = 0;
        for (ParsedTransactionRow row : parsedRows) {
            Category matchedCategory = categorizationService.match(row.description(), categories).orElse(null);
            if (matchedCategory != null) {
                categorizedCount++;
            }
            transactions.add(toTransaction(row, matchedCategory));
        }
        transactionRepository.saveAll(transactions);

        int uncategorizedCount = transactions.size() - categorizedCount;

        log.info("Imported {} transactions ({} categorized, {} uncategorized, {} skipped) from file '{}'",
                transactions.size(), categorizedCount, uncategorizedCount, skippedCount, file.getOriginalFilename());

        return new ImportResultResponse(file.getOriginalFilename(), dataRows.size(), transactions.size(),
                skippedCount, categorizedCount, uncategorizedCount);
    }

    private Transaction toTransaction(ParsedTransactionRow row, Category category) {
        return Transaction.builder()
                .date(row.date())
                .description(row.description())
                .amount(row.amount())
                .category(category)
                .build();
    }

    private FileType detectFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);

        if (lowerName.endsWith(".csv")) {
            return FileType.CSV;
        }
        if (lowerName.endsWith(".xlsx")) {
            return FileType.XLSX;
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.equalsIgnoreCase("text/csv")) {
                return FileType.CSV;
            }
            if (contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                return FileType.XLSX;
            }
        }

        throw new InvalidFileFormatException(
                "Unsupported file type for '%s'. Only .csv and .xlsx files are supported".formatted(filename));
    }

    private List<List<String>> readRawRows(MultipartFile file, FileType fileType) {
        try (InputStream inputStream = file.getInputStream()) {
            return switch (fileType) {
                case CSV -> readCsvRows(inputStream);
                case XLSX -> readXlsxRows(inputStream);
            };
        } catch (IOException | UncheckedIOException e) {
            throw new InvalidFileFormatException("Unable to read file '%s': %s"
                    .formatted(file.getOriginalFilename(), e.getMessage()), e);
        }
    }

    private List<List<String>> readCsvRows(InputStream inputStream) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder().setTrim(true).build();
        try (CSVParser parser = CSVParser.parse(inputStream, java.nio.charset.StandardCharsets.UTF_8, format)) {
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                record.forEach(row::add);
                rows.add(row);
            }
        }
        return rows;
    }

    private List<List<String>> readXlsxRows(InputStream inputStream) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return rows;
            }
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    cells.add(dataFormatter.formatCellValue(cell).trim());
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private ColumnIndexes resolveColumnIndexes(List<String> headerRow) {
        Integer dateIndex = null;
        Integer descriptionIndex = null;
        Integer amountIndex = null;

        for (int i = 0; i < headerRow.size(); i++) {
            String normalized = headerRow.get(i) == null ? "" : headerRow.get(i).trim().toLowerCase(Locale.ROOT);
            if (DATE_HEADERS.contains(normalized)) {
                dateIndex = i;
            } else if (DESCRIPTION_HEADERS.contains(normalized)) {
                descriptionIndex = i;
            } else if (AMOUNT_HEADERS.contains(normalized)) {
                amountIndex = i;
            }
        }

        if (dateIndex == null || descriptionIndex == null || amountIndex == null) {
            throw new InvalidFileFormatException(
                    "File header is missing one or more required columns (date, description, amount)");
        }

        return new ColumnIndexes(dateIndex, descriptionIndex, amountIndex);
    }

    private ParsedTransactionRow tryParseRow(List<String> row, ColumnIndexes columnIndexes) {
        String rawDate = cellAt(row, columnIndexes.dateIndex());
        String rawDescription = cellAt(row, columnIndexes.descriptionIndex());
        String rawAmount = cellAt(row, columnIndexes.amountIndex());

        if (rawDescription == null || rawDescription.isBlank()) {
            log.debug("Skipping row: blank description ({})", row);
            return null;
        }

        LocalDate date = parseDate(rawDate);
        if (date == null) {
            log.debug("Skipping row: unparseable date '{}' ({})", rawDate, row);
            return null;
        }

        BigDecimal amount = parseAmount(rawAmount);
        if (amount == null) {
            log.debug("Skipping row: unparseable amount '{}' ({})", rawAmount, row);
            return null;
        }

        return new ParsedTransactionRow(date, rawDescription.trim(), amount);
    }

    private String cellAt(List<String> row, int index) {
        return index < row.size() ? row.get(index) : null;
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        String trimmed = rawDate.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        // Strip harmless surrounding noise only (whitespace, currency symbols); anything else
        // remaining (e.g. a stray letter) makes the value genuinely malformed.
        String cleaned = CURRENCY_NOISE.matcher(rawAmount.trim()).replaceAll("");
        if (cleaned.isEmpty() || !VALID_NUMERIC_AMOUNT.matcher(cleaned).matches()) {
            return null;
        }

        boolean hasComma = cleaned.indexOf(',') >= 0;
        boolean hasDot = cleaned.indexOf('.') >= 0;

        if (hasComma && hasDot) {
            // Both separators present: whichever one appears last is the decimal separator
            // (e.g. "1.200,50" -> ',' is last -> European decimal comma -> 1200.50;
            // "1,200.50" -> '.' is last -> US decimal point -> 1200.50). The other separator
            // is treated as a thousands grouping separator and stripped.
            int lastComma = cleaned.lastIndexOf(',');
            int lastDot = cleaned.lastIndexOf('.');
            if (lastComma > lastDot) {
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (hasComma) {
            // Assume ',' is used as the decimal separator, e.g. "1234,56".
            cleaned = cleaned.replace(",", ".");
        }

        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private enum FileType {
        CSV, XLSX
    }

    private record ColumnIndexes(int dateIndex, int descriptionIndex, int amountIndex) {
    }
}
