package fr.inria.corese.gui.feature.result.table;

import fr.inria.corese.gui.component.notification.NotificationManager;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.factory.ButtonFactory;
import fr.inria.corese.gui.utils.ExportHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for the tabular result view.
 *
 * <p>This class manages the interaction between the data model (SPARQL results in CSV format)
 * and the {@link TableResultView}. It handles:
 * <ul>
 *   <li>Parsing raw result data (CSV) into a structured format.
 *   <li>Pagination logic (calculating pages and slicing data).
 *   <li>Handling user actions like "Copy" and "Export" via a provided format service.
 * </ul>
 */
public class TableResultController {

    private static final int DEFAULT_ROWS_PER_PAGE = 50;

    private final TableResultView view;
    private final List<String[]> allRows;
    private int rowsPerPage;
    
    // Function to request formatted content from the backend (e.g. QueryService)
    private Function<SerializationFormat, String> formatProvider;

    /**
     * Creates a new TableResultController.
     */
    public TableResultController() {
        this.allRows = new ArrayList<>();
        this.rowsPerPage = DEFAULT_ROWS_PER_PAGE;
        this.view = new TableResultView(this::handlePageChange);
        initialize();
    }

    private void initialize() {
        // Setup Toolbar Actions
        view.setToolbarActions(List.of(
            ButtonFactory.copy(this::copyContent),
            ButtonFactory.export(this::exportContent)
        ));

        // Setup Rows Per Page Listener
        view.setRowsPerPageText(String.valueOf(DEFAULT_ROWS_PER_PAGE));
        view.getRowsPerPageProperty().addListener((obs, oldVal, newVal) -> handleRowsPerPageChange(newVal));
    }

    /**
     * Sets the provider for formatting results.
     * Used for Copy and Export actions to retrieve the full result in desired formats.
     *
     * @param formatProvider A function that takes a format and returns the result string.
     */
    public void setFormatProvider(Function<SerializationFormat, String> formatProvider) {
        this.formatProvider = formatProvider;
    }

    // ==============================================================================================
    // Logic - Data Processing
    // ==============================================================================================

    /**
     * Updates the table with new SPARQL CSV results.
     * Can be called from any thread.
     *
     * @param csvResult The raw CSV string result from the SPARQL query.
     */
    public void updateTable(String csvResult) {
        Platform.runLater(() -> parseAndPopulate(csvResult));
    }

    private void parseAndPopulate(String csvResult) {
        allRows.clear();
        view.clearTable();

        if (csvResult == null || csvResult.isEmpty()) {
            updatePagination(true);
            return;
        }

        // Split by line (handling both \r\n and \n)
        String[] lines = csvResult.split("\\r?\\n");
        if (lines.length == 0) {
            updatePagination(true);
            return;
        }

        // First line is headers
        String[] headers = lines[0].split(",", -1);
        view.setColumns(headers);

        // Subsequent lines are data
        for (int i = 1; i < lines.length; i++) {
            allRows.add(lines[i].split(",", -1));
        }

        updatePagination(true);
    }

    // ==============================================================================================
    // Logic - Pagination
    // ==============================================================================================

    private void handlePageChange(int newPageIndex) {
        updateTableForPage(newPageIndex);
    }

    private void handleRowsPerPageChange(String newValue) {
        try {
            int newRowsPerPage = Integer.parseInt(newValue);
            if (newRowsPerPage > 0) {
                this.rowsPerPage = newRowsPerPage;
                updatePagination(true);
            }
        } catch (NumberFormatException e) {
            // Ignore invalid input
        }
    }

    private void updatePagination(boolean resetPage) {
        view.setTotalRowsLabel("Total rows: " + allRows.size());

        if (allRows.isEmpty()) {
            view.updatePagination(1);
            view.setCurrentPageIndex(0);
            view.setTableData(Collections.emptyList());
            return;
        }

        int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
        view.updatePagination(pageCount);

        if (resetPage) {
            view.setCurrentPageIndex(0);
            updateTableForPage(0);
        } else {
            view.setCurrentPageIndex(0);
            updateTableForPage(0);
        }
    }

    private void updateTableForPage(int pageIndex) {
        if (allRows.isEmpty()) {
            view.setTableData(Collections.emptyList());
            return;
        }

        int fromIndex = pageIndex * rowsPerPage;
        if (fromIndex >= allRows.size()) {
             fromIndex = 0; 
        }
        
        int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
        
        view.setCurrentPageIndex(pageIndex);
        view.setTableData(allRows.subList(fromIndex, toIndex));
    }

    // ==============================================================================================
    // Actions
    // ==============================================================================================

    private void copyContent() {
        if (allRows.isEmpty()) return;
        
        if (formatProvider == null) {
            NotificationManager.getInstance().showError("Copy failed: No data provider available.");
            return;
        }

        // Use Corese's Markdown export by default for copy
        String content = formatProvider.apply(SerializationFormat.MARKDOWN);
        if (content == null || content.startsWith("Error")) {
             NotificationManager.getInstance().showError("Copy failed: " + content);
             return;
        }

        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(content);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
        
        NotificationManager.getInstance().showSuccess("Result copied to clipboard (Markdown)");
    }

    private void exportContent() {
        if (allRows.isEmpty()) return;
        
        if (formatProvider == null) {
             NotificationManager.getInstance().showError("Export failed: No data provider available.");
             return;
        }
        
        ExportHelper.exportResult(
            view.getRoot().getScene().getWindow(),
            Arrays.asList(SerializationFormat.CSV, SerializationFormat.MARKDOWN, SerializationFormat.JSON, SerializationFormat.XML, SerializationFormat.TSV),
            formatProvider
        );
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    public Node getView() {
        return view.getRoot();
    }

    public void clear() {
        Platform.runLater(() -> {
            allRows.clear();
            view.clearTable();
            updatePagination(true);
        });
    }

    public int getRowCount() {
        return allRows.size();
    }
}