package fr.inria.corese.gui.feature.result.table;

import fr.inria.corese.gui.feature.rule.CustomPagination;






import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for tabular SPARQL result display with pagination.
 *
 * <p>This controller manages:
 *
 * <ul>
 *   <li>TableView for displaying SPARQL SELECT results
 *   <li>CSV parsing and column creation
 *   <li>Pagination controls for large result sets
 *   <li>Configurable rows per page
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * TableResultController controller = new TableResultController();
 * controller.updateTable(csvResults);
 * Node view = controller.getView();
 * }</pre>
 */
public class TableResultController {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  /** Default number of rows displayed per page. */
  private static final int DEFAULT_ROWS_PER_PAGE = 50;

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  /** Table view for displaying result rows. */
  private final TableView<String[]> tableView;

  /** Custom pagination control for navigating pages. */
  private final CustomPagination pagination;

  /** Text field for configuring rows per page. */
  private final TextField rowsPerPageField;

  /** Label showing total number of rows. */
  private final Label totalRowsLabel;

  /** All result rows (not paginated). */
  private final List<String[]> allRows;

  /** Current number of rows per page. */
  private int rowsPerPage;

  /** Root view node containing all UI components. */
  private final VBox rootView;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Constructs a new TableResultController with default pagination settings. */
  public TableResultController() {
    this.tableView = new TableView<>();
    this.allRows = new ArrayList<>();
    this.rowsPerPageField = new TextField(String.valueOf(DEFAULT_ROWS_PER_PAGE));
    this.totalRowsLabel = new Label("total rows: 0");
    this.pagination = new CustomPagination(1, this::updateTableForPage);
    this.rowsPerPage = DEFAULT_ROWS_PER_PAGE;
    this.rootView = new VBox(5);

    initialize();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /** Initializes UI components and event handlers. */
  private void initialize() {
    // Configure pagination (initially hidden)
    pagination.setVisible(false);
    pagination.setManaged(false);

    // Configure rows per page field
    rowsPerPageField.setPrefWidth(60);
    rowsPerPageField
        .textProperty()
        .addListener(
            (obs, oldVal, newVal) -> {
              try {
                rowsPerPage = Math.max(1, Integer.parseInt(newVal));
                updatePagination();
              } catch (NumberFormatException _) {
                // Ignore invalid input
              }
            });

    // Build controls panel
    Label perPageLabel = new Label("Rows per page:");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    HBox controlsPane = new HBox(10);
    controlsPane.setAlignment(Pos.CENTER_LEFT);
    controlsPane.getChildren().addAll(perPageLabel, rowsPerPageField, spacer, totalRowsLabel);

    // Build layout
    VBox.setVgrow(tableView, Priority.ALWAYS);
    rootView.getChildren().addAll(controlsPane, tableView, pagination);
  }

  // ==============================================================================================
  // Table Management
  // ==============================================================================================

  /**
   * Updates the page display when pagination changes.
   *
   * @param pageIndex The zero-based page index
   */
  private void updateTableForPage(int pageIndex) {
    int fromIndex = pageIndex * rowsPerPage;
    int toIndex = Math.min(fromIndex + rowsPerPage, allRows.size());

    if (fromIndex >= allRows.size()) {
      tableView.getItems().clear();
      return;
    }

    tableView.getItems().setAll(allRows.subList(fromIndex, toIndex));
  }

  /** Updates pagination controls based on current data. */
  private void updatePagination() {
    totalRowsLabel.setText("total rows: " + allRows.size());

    if (allRows.isEmpty()) {
      pagination.setVisible(false);
      pagination.setManaged(false);
      return;
    }

    int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
    pagination.setPageCount(pageCount);
    pagination.setVisible(pageCount > 1);
    pagination.setManaged(pageCount > 1);
    pagination.setCurrentPageIndex(0);
  }

  /**
   * Parses CSV data and creates table columns dynamically.
   *
   * @param csvResult The CSV formatted result string
   */
  private void parseAndPopulateTable(String csvResult) {
    tableView.getItems().clear();
    tableView.getColumns().clear();
    allRows.clear();

    if (csvResult == null || csvResult.isEmpty()) {
      updatePagination();
      return;
    }

    String[] lines = csvResult.split("\\r?\\n");
    if (lines.length == 0) {
      updatePagination();
      return;
    }

    // Parse header row
    String[] headers = lines[0].split(",", -1);
    for (int col = 0; col < headers.length; col++) {
      final int colIndex = col;
      TableColumn<String[], String> column = new TableColumn<>(headers[col].trim());
      column.setCellValueFactory(
          cellData ->
              new javafx.beans.property.SimpleStringProperty(
                  (colIndex < cellData.getValue().length) ? cellData.getValue()[colIndex] : ""));
      column
          .prefWidthProperty()
          .bind(tableView.widthProperty().divide(Math.max(1, headers.length)));
      tableView.getColumns().add(column);
    }

    // Parse data rows
    for (int i = 1; i < lines.length; i++) {
      allRows.add(lines[i].split(",", -1));
    }

    updatePagination();
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Updates the table with CSV formatted results.
   *
   * <p>This method parses the CSV data, creates columns from the header row, and populates the
   * table with data rows. Pagination is automatically configured based on the result size.
   *
   * @param csvResult The CSV formatted result string (header + data rows)
   */
  public void updateTable(String csvResult) {
    Platform.runLater(() -> parseAndPopulateTable(csvResult));
  }

  /**
   * Clears all table data and resets pagination.
   */
  public void clear() {
    Platform.runLater(
        () -> {
          tableView.getItems().clear();
          tableView.getColumns().clear();
          allRows.clear();
          updatePagination();
        });
  }

  /**
   * Returns the root view node.
   *
   * @return The VBox containing all UI components
   */
  public Node getView() {
    return rootView;
  }

  /**
   * Returns the number of result rows (excluding header).
   *
   * @return The total row count
   */
  public int getRowCount() {
    return allRows.size();
  }

  /**
   * Sets the number of rows displayed per page.
   *
   * @param rowsPerPage The desired rows per page (minimum 1)
   */
  public void setRowsPerPage(int rowsPerPage) {
    this.rowsPerPage = Math.max(1, rowsPerPage);
    Platform.runLater(
        () -> {
          rowsPerPageField.setText(String.valueOf(this.rowsPerPage));
          updatePagination();
        });
  }

  /**
   * Displays validation report items in the table.
   * 
   * @param items List of validation report items
   */
  public void displayReport(List<fr.inria.corese.gui.core.model.ValidationReportItem> items) {
      // Placeholder: Convert items to CSV-like structure or specialized table
      // For now, clear table to avoid errors
      clear(); 
      // TODO: Implement proper report display
  }
}
