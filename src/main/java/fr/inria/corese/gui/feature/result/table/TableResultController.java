package fr.inria.corese.gui.feature.result.table;

import fr.inria.corese.gui.component.notification.NotificationManager;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.factory.ButtonFactory;
import fr.inria.corese.gui.core.manager.ExportManager;
import fr.inria.corese.gui.core.model.ValidationReportItem;
import fr.inria.corese.gui.utils.ExportHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Controller for tabular SPARQL result display with pagination.
 *
 * <p>This controller manages:
 * <ul>
 *   <li>Parsing CSV results into rows and columns
 *   <li>Pagination logic (calculating pages, slicing data)
 *   <li>Interactions with the {@link TableResultView}
 *   <li>Export and Copy actions
 * </ul>
 */
public class TableResultController {

    private static final int DEFAULT_ROWS_PER_PAGE = 50;

    private final TableResultView view;
    private final List<String[]> allRows;
    private String[] headers;
    private int rowsPerPage;

    public TableResultController() {
        this.allRows = new ArrayList<>();
        this.rowsPerPage = DEFAULT_ROWS_PER_PAGE;
        this.view = new TableResultView(this::updateTableForPage);
        initialize();
    }

    private void initialize() {
        // Setup Toolbar
        view.setToolbarActions(List.of(
            ButtonFactory.copy(this::copyContent), 
            ButtonFactory.export(this::exportContent)
        ));

        // Setup Rows Per Page Listener
        view.setRowsPerPageText(String.valueOf(DEFAULT_ROWS_PER_PAGE));
        view.getRowsPerPageProperty().addListener((obs, oldVal, newVal) -> {
            try {
                int newRowsPerPage = Integer.parseInt(newVal);
                if (newRowsPerPage > 0) {
                    rowsPerPage = newRowsPerPage;
                    updatePagination(false); 
                }
            } catch (NumberFormatException e) {
                // Ignore invalid input
            }
        });
    }

    // ==============================================================================================
    // Logic - Parsing & Pagination
    // ==============================================================================================

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

        String[] lines = csvResult.split("\\r?\\n");
        if (lines.length == 0) {
            updatePagination(true);
            return;
        }

        // Headers
        this.headers = lines[0].split(",", -1);
        view.setColumns(headers);

        // Data Rows
        for (int i = 1; i < lines.length; i++) {
            allRows.add(lines[i].split(",", -1));
        }

        updatePagination(true);
    }

    private void updatePagination(boolean resetPage) {
        view.setTotalRowsLabel("Total rows: " + allRows.size());

        if (allRows.isEmpty()) {
            view.updatePagination(1); // Default to 1 page even if empty
            view.setCurrentPageIndex(0);
            view.setTableData(new ArrayList<>());
            return;
        }

        int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
        view.updatePagination(pageCount);

        if (resetPage) {
            view.setCurrentPageIndex(0);
            updateTableForPage(0);
        } else {
            // If current view index is out of bounds after resize, view handles it or we can clamp
            // Here we just refresh the current page which View callback will handle effectively 
            // but we need to trigger data update.
            // Since View doesn't expose current index easily, we rely on View resetting or maintaining valid state.
            // However, TableResultView.setCurrentPageIndex calls listener? No, it just updates UI.
            // We need to fetch current index or just reset to 0 to be safe, OR calculate current based on logic.
            // For simplicity in "density change", we usually stay on page 0 or try to calculate relative position.
            // Let's reload page 0 for safety or implement complex logic if requested. 
            // The previous code didn't reload data if !resetPage?
            // "updateTableForPage(0)" was called in the previous 'else' block? No, it said "Refresh current view logic could be added here".
            // So previously, changing density didn't update the table? That seems like a bug.
            // I will fix this: changing density MUST update the table data.
            updateTableForPage(0); 
            view.setCurrentPageIndex(0);
        }
    }

    private void updateTableForPage(int pageIndex) {
        // Ensure the view knows the current page for absolute row numbering calculation (# column)
        view.setCurrentPageIndex(pageIndex);

        if (allRows.isEmpty()) {
            view.setTableData(new ArrayList<>());
            return;
        }

        int fromIndex = pageIndex * rowsPerPage;
        if (fromIndex >= allRows.size()) {
             fromIndex = 0; // Fallback
        }
        
        int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());
        view.setTableData(allRows.subList(fromIndex, toIndex));
    }

    public void displayReport(List<ValidationReportItem> items) {
        if (items == null || items.isEmpty()) {
            clear();
            return;
        }

        this.headers = new String[] {"Severity", "Message", "Focus Node", "Path", "Value"};
        allRows.clear();
        view.clearTable();
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
    }

    // ==============================================================================================
    // Actions
    // ==============================================================================================

    private void copyContent() {
        if (allRows.isEmpty()) return;
        String content = ExportManager.getInstance().formatTableData(allRows, headers, SerializationFormat.MARKDOWN);
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
            format -> ExportManager.getInstance().formatTableData(allRows, headers, format)
        );
    }

    // ==============================================================================================
    // Public API
    // ==============================================================================================

    public Node getView() {
        return view.getRoot();
    }

    public void clear() {
        allRows.clear();
        view.clearTable();
        updatePagination(true);
    }

    public int getRowCount() {
        return allRows.size();
    }
}
