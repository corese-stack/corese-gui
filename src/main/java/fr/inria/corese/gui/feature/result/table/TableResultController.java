package fr.inria.corese.gui.feature.result.table;

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
 *
 * <ul>
 *   <li>Parsing CSV results into rows and columns
 *   <li>Pagination logic (calculating pages, slicing data)
 *   <li>Interactions with the {@link TableResultView}
 *   <li>Export and Copy actions
 * </ul>
 */
public class TableResultController {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  private static final int DEFAULT_ROWS_PER_PAGE = 50;

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  private final TableResultView view;
  private final List<String[]> allRows;
  private String[] headers;
  private int rowsPerPage;

  // ==============================================================================================
  // Constructor & Initialization
  // ==============================================================================================

  public TableResultController() {
    this.allRows = new ArrayList<>();
    this.rowsPerPage = DEFAULT_ROWS_PER_PAGE;
    this.view = new TableResultView(this::updateTableForPage);

    initialize();
  }

  private void initialize() {
    // 1. Setup Toolbar
    view.setToolbarActions(
        List.of(ButtonFactory.copy(this::copyContent), ButtonFactory.export(this::exportContent)));

    // 2. Setup Rows Per Page Listener
    view.setRowsPerPageText(String.valueOf(DEFAULT_ROWS_PER_PAGE));
    view.getRowsPerPageField()
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              try {
                rowsPerPage = Math.max(1, Integer.parseInt(newVal));
                updatePagination(false); // Don't reset to page 0 if just changing density
              } catch (NumberFormatException e) {
                // Ignore invalid input
              }
            });
  }

  // ==============================================================================================
  // Logic - Parsing & Pagination
  // ==============================================================================================

  /**
   * Updates the table with CSV formatted results.
   *
   * @param csvResult The CSV formatted result string (header + data rows)
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

    String[] lines = csvResult.split("\\r?\\n");
    if (lines.length == 0) {
      updatePagination(true);
      return;
    }

    // 1. Headers
    this.headers = lines[0].split(",", -1);
    view.setColumns(headers);

    // 2. Data Rows
    for (int i = 1; i < lines.length; i++) {
      allRows.add(lines[i].split(",", -1));
    }

    // 3. Refresh UI
    updatePagination(true);
  }

  private void updatePagination(boolean resetPage) {
    view.setTotalRowsLabel("total rows: " + allRows.size());

    if (allRows.isEmpty()) {
      view.updatePagination(0, false);
      return;
    }

    int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
    view.updatePagination(pageCount, pageCount > 1);

    if (resetPage) {
      view.setCurrentPageIndex(0);
      updateTableForPage(0); // Explicitly load first page
    } else {
      // Refresh current view logic could be added here if needed
      updateTableForPage(0);
    }
  }

  private void updateTableForPage(int pageIndex) {
    if (allRows.isEmpty()) return;

    int fromIndex = pageIndex * rowsPerPage;
    int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());

    if (fromIndex >= allRows.size()) {
      view.setTableData(new ArrayList<>());
      return;
    }

    view.setTableData(allRows.subList(fromIndex, toIndex));
  }

  /**
   * Displays a list of validation report items in the table.
   *
   * @param items The list of validation report items.
   */
  public void displayReport(List<ValidationReportItem> items) {
    if (items == null || items.isEmpty()) {
      clear();
      return;
    }

    // Define columns for validation report
    this.headers = new String[] {"Severity", "Message", "Focus Node", "Path", "Value"};
    allRows.clear();
    view.clearTable();
    view.setColumns(headers);

    for (ValidationReportItem item : items) {
      allRows.add(
          new String[] {
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

    // Use Markdown for clipboard as it is more versatile for documentation
    String content =
        ExportManager.getInstance().formatTableData(allRows, headers, SerializationFormat.MARKDOWN);

    ClipboardContent clipboardContent = new ClipboardContent();
    clipboardContent.putString(content);
    Clipboard.getSystemClipboard().setContent(clipboardContent);
  }

  private void exportContent() {
    if (allRows.isEmpty()) return;

    // Support CSV and Markdown export
    ExportHelper.exportResult(
        view.getRoot().getScene().getWindow(),
        Arrays.asList(SerializationFormat.CSV, SerializationFormat.MARKDOWN),
        format -> ExportManager.getInstance().formatTableData(allRows, headers, format));
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
