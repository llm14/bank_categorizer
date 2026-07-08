package com.bankcategorizer.service;

import com.bankcategorizer.dto.ImportResultResponse;
import com.bankcategorizer.exception.InvalidFileFormatException;
import com.bankcategorizer.model.Category;
import com.bankcategorizer.model.Transaction;
import com.bankcategorizer.repository.TransactionRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
class TransactionImportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategorizationService categorizationService;

    private TransactionImportService transactionImportService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        transactionImportService = new TransactionImportService(transactionRepository, categorizationService);
        // Default: no categories configured, every row stays uncategorized unless a test overrides this.
        when(categorizationService.loadCategories()).thenReturn(List.of());
        when(categorizationService.match(anyString(), any())).thenReturn(Optional.empty());
    }

    @Test
    void importTransactions_validCsv_savesTransactionsAndReportsCounts() {
        String csv = """
                Date,Description,Amount
                2024-01-15,Grocery Store,45.30
                2024-01-16,Salary,2000.00
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse result = transactionImportService.importTransactions(file);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(0);
        assertThat(result.categorizedCount()).isEqualTo(0);
        assertThat(result.uncategorizedCount()).isEqualTo(2);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        List<Transaction> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(tx -> assertThat(tx.getCategory()).isNull());
        assertThat(saved.get(0).getDescription()).isEqualTo("Grocery Store");
        assertThat(saved.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("45.30"));
    }

    @Test
    void importTransactions_descriptionMatchesCategoryKeyword_assignsCategoryAndReportsCounts() {
        Category groceries = Category.builder().id(1L).name("Groceries").build();
        String csv = """
                Date,Description,Amount
                2024-05-01,Local Supermarket Purchase,25.00
                2024-05-02,Salary,2000.00
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(categorizationService.loadCategories()).thenReturn(List.of(groceries));
        when(categorizationService.match(eq("Local Supermarket Purchase"), eq(List.of(groceries))))
                .thenReturn(Optional.of(groceries));
        when(categorizationService.match(eq("Salary"), eq(List.of(groceries))))
                .thenReturn(Optional.empty());

        ImportResultResponse result = transactionImportService.importTransactions(file);

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.categorizedCount()).isEqualTo(1);
        assertThat(result.uncategorizedCount()).isEqualTo(1);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        List<Transaction> saved = captor.getValue();
        assertThat(saved).filteredOn(tx -> tx.getDescription().equals("Local Supermarket Purchase"))
                .extracting(Transaction::getCategory).containsExactly(groceries);
        assertThat(saved).filteredOn(tx -> tx.getDescription().equals("Salary"))
                .extracting(Transaction::getCategory).containsExactly((Category) null);
    }

    @Test
    void importTransactions_noCategoriesConfigured_leavesAllTransactionsUncategorized() {
        String csv = """
                Date,Description,Amount
                2024-05-03,Random Purchase,12.00
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse result = transactionImportService.importTransactions(file);

        assertThat(result.categorizedCount()).isEqualTo(0);
        assertThat(result.uncategorizedCount()).isEqualTo(1);
    }

    @Test
    void importTransactions_validXlsx_savesTransactionsAndReportsCounts() throws IOException {
        byte[] xlsxBytes = buildXlsx(
                new String[]{"Date", "Description", "Amount"},
                new String[][]{
                        {"2024-02-01", "Rent", "750.00"},
                        {"2024-02-05", "Electricity Bill", "60.15"}
                });
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes);

        ImportResultResponse result = transactionImportService.importTransactions(file);

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(0);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void importTransactions_malformedRows_areSkippedButValidRowsAreImported() {
        String csv = """
                Date,Description,Amount
                2024-03-01,Valid Row,10.00
                not-a-date,Bad Date Row,10.00
                2024-03-02,Bad Amount Row,not-a-number
                2024-03-03,,5.00
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse result = transactionImportService.importTransactions(file);

        assertThat(result.totalRows()).isEqualTo(4);
        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(3);
    }

    @Test
    void importTransactions_amountWithBothSeparators_usesLastSeparatorAsDecimalPoint() {
        // Amount values containing a comma must be quoted, otherwise the CSV parser itself
        // (not our amount-parsing logic) would split them into extra columns.
        String csv = """
                Date,Description,Amount
                2024-06-01,European Style,"1.200,50"
                2024-06-02,US Style,"1,200.50"
                2024-06-03,Plain Dot,1200.50
                2024-06-04,Plain Comma,"1200,50"
                2024-06-05,Genuinely Malformed,12x50
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse result = transactionImportService.importTransactions(file);

        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        List<Transaction> saved = captor.getValue();

        assertThat(saved).filteredOn(tx -> tx.getDescription().equals("European Style"))
                .extracting(Transaction::getAmount)
                .containsExactly(new BigDecimal("1200.50"));
        assertThat(saved).filteredOn(tx -> tx.getDescription().equals("US Style"))
                .extracting(Transaction::getAmount)
                .containsExactly(new BigDecimal("1200.50"));
        assertThat(saved).filteredOn(tx -> tx.getDescription().equals("Plain Dot"))
                .extracting(Transaction::getAmount)
                .containsExactly(new BigDecimal("1200.50"));
        assertThat(saved).filteredOn(tx -> tx.getDescription().equals("Plain Comma"))
                .extracting(Transaction::getAmount)
                .containsExactly(new BigDecimal("1200.50"));
        assertThat(saved).noneMatch(tx -> tx.getDescription().equals("Genuinely Malformed"));
        assertThat(result.importedCount()).isEqualTo(4);
        assertThat(result.skippedCount()).isEqualTo(1);
    }

    @Test
    void importTransactions_caseInsensitiveHeaderAliases_areRecognized() {
        String csv = """
                DATE,Desc,Value
                2024-04-10,Coffee Shop,3.50
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse result = transactionImportService.importTransactions(file);

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(0);
    }

    @Test
    void importTransactions_unsupportedFileType_throwsInvalidFileFormatException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.txt", "text/plain", "irrelevant".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> transactionImportService.importTransactions(file))
                .isInstanceOf(InvalidFileFormatException.class);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void importTransactions_emptyFile_throwsInvalidFileFormatException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> transactionImportService.importTransactions(file))
                .isInstanceOf(InvalidFileFormatException.class);
    }

    @Test
    void importTransactions_missingRequiredColumn_throwsInvalidFileFormatException() {
        String csv = """
                Date,Description
                2024-01-15,Grocery Store
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "transactions.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> transactionImportService.importTransactions(file))
                .isInstanceOf(InvalidFileFormatException.class);
    }

    private byte[] buildXlsx(String[] headers, String[][] rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
