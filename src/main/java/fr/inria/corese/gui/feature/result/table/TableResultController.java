package fr.inria.corese.gui.feature.result.table;

import fr.inria.corese.gui.component.notification.NotificationManager;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.factory.ButtonFactory;
import fr.inria.corese.gui.core.adapter.ResultFormatter;
import fr.inria.corese.gui.core.model.ValidationReportItem;
import fr.inria.corese.gui.utils.ExportHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for the tabular result view.
 *
 * <p>This class manages the interaction between the data model (SPARQL results in CSV format
 * or Validation Reports) and the {@link TableResultView}. It handles:
 * <ul>
 *   <li>Parsing raw result data (CSV) into a structured format.
 *   <li>Pagination logic (calculating pages and slicing data).
 *   <li>Handling user actions like "Copy" and "Export".
 * </ul>
 */
public class TableResultController {

    private static final int DEFAULT_ROWS_PER_PAGE = 50;

    private final TableResultView view;
    private final List<String[]> allRows;
    private String[] headers;
    private int rowsPerPage;

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

    /**
     * Displays a validation report in the table.
     * Can be called from any thread.
     *
     * @param items The list of validation report items.
     */
    public void displayReport(List<ValidationReportItem> items) {
        Platform.runLater(() -> {
            allRows.clear();
            view.clearTable();

            if (items == null || items.isEmpty()) {
                updatePagination(true);
                return;
            }

            this.headers = new String[] {"Severity", "Message", "Focus Node", "Path", "Value"};
            view.setColumns(headers);

            for (ValidationReportItem item : items) {
                allRows.add(new String[] {
                    item.getSeverity(),
                    item.getMessage(),
                    item.getFocusNode(),
                    item.getResultPath(),
                    item.getValue()
                });
            }

            updatePagination(true);
        });
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
        // Note: usage of String.split(",") is basic and assumes no commas in values.
        // For standard SPARQL CSV results from Corese, this is generally acceptable
        // but replacing with a proper CSV parser is recommended if complex data is expected.
        this.headers = lines[0].split(",", -1);
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
        // View calls this when user clicks pagination buttons.
        // We just need to update the data displayed.
        updateTableForPage(newPageIndex);
    }

    private void handleRowsPerPageChange(String newValue) {
        try {
            int newRowsPerPage = Integer.parseInt(newValue);
            if (newRowsPerPage > 0) {
                this.rowsPerPage = newRowsPerPage;
                // Recalculate pagination without resetting to page 0 if possible,
                // but simpler to reset to ensure consistency.
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
            // If we are just resizing pages, we try to stay on a valid page.
            // But since the view's current index might be out of sync or invalid for new page count:
            // For now, simpler to reset to page 0 to avoid index out of bounds confusion.
            // Ideally, we would calculate: newPageIndex = (oldPageIndex * oldRowsPerPage) / newRowsPerPage
            // But without tracking old state explicitly, reset is safer.
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
        // Clamp fromIndex
        if (fromIndex >= allRows.size()) {
             fromIndex = 0; 
        }
        
        int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
        
        // Notify view of the current page index for correct row numbering
        view.setCurrentPageIndex(pageIndex);
        view.setTableData(allRows.subList(fromIndex, toIndex));
    }

    // ==============================================================================================
    // Actions
    // ==============================================================================================

    private void copyContent() {
        if (allRows.isEmpty()) return;
        
        String content = ResultFormatter.getInstance().formatTable(allRows, headers, SerializationFormat.MARKDOWN);
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(content);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
        
        NotificationManager.getInstance().showSuccess("Result copied to clipboard");
    }

    private void exportContent() {
        if (allRows.isEmpty()) return;
        
        ExportHelper.exportResult(
            view.getRoot().getScene().getWindow(),
            Arrays.asList(SerializationFormat.CSV, SerializationFormat.MARKDOWN),
            format -> ResultFormatter.getInstance().formatTable(allRows, headers, format)
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
