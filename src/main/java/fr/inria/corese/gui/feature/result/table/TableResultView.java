package fr.inria.corese.gui.feature.result.table;

import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.feature.rule.CustomPagination;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * View for displaying tabular SPARQL results with pagination.
 *
 * <p>This view provides:
 *
 * <ul>
 *   <li>A TableView for displaying rows of data
 *   <li>Pagination controls (custom widget)
 *   <li>Rows per page configuration
 *   <li>A dedicated sidebar for actions (Copy, Export)
 * </ul>
 */
public class TableResultView extends AbstractView {

  // ==============================================================================================
  // Constants
  // ==============================================================================================

  private static final String STYLESHEET_PATH = "/styles/table-result.css"; // Placeholder if needed

  // ==============================================================================================
  // UI Components
  // ==============================================================================================

  private final TableView<String[]> tableView;
  private final CustomPagination pagination;
  private final TextField rowsPerPageField;
  private final Label totalRowsLabel;
  private final ToolbarWidget toolbarWidget;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /**
   * Constructs a new TableResultView.
   *
   * @param pageChangeHandler Callback for when the page index changes
   */
  public TableResultView(Consumer<Integer> pageChangeHandler) {
    super(new BorderPane(), null); // Null stylesheet for now or add one

    this.tableView = new TableView<>();
    this.rowsPerPageField = new TextField();
    this.totalRowsLabel = new Label("total rows: 0");
    this.pagination = new CustomPagination(1, pageChangeHandler);
    this.toolbarWidget = new ToolbarWidget();

    setupLayout();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  private void setupLayout() {
    // 1. Configure Components
    pagination.setVisible(false);
    pagination.setManaged(false);
    rowsPerPageField.setPrefWidth(60);

    // 2. Bottom Bar (Controls + Pagination + Total rows)
    Label perPageLabel = new Label("Rows/page:");

    // Left: Rows per page
    HBox leftBox = new HBox(5, perPageLabel, rowsPerPageField);
    leftBox.setAlignment(Pos.CENTER_LEFT);

    // Right: Total rows
    HBox rightBox = new HBox(totalRowsLabel);
    rightBox.setAlignment(Pos.CENTER_RIGHT);

    // Spacers to center pagination
    Region leftSpacer = new Region();
    HBox.setHgrow(leftSpacer, Priority.ALWAYS);

    Region rightSpacer = new Region();
    HBox.setHgrow(rightSpacer, Priority.ALWAYS);

    HBox bottomBar = new HBox(5);
    bottomBar.setAlignment(Pos.CENTER);
    bottomBar.setPadding(new Insets(5));
    bottomBar.getChildren().addAll(leftBox, leftSpacer, pagination, rightSpacer, rightBox);

    // 3. Main Layout
    BorderPane root = (BorderPane) getRoot();
    root.setCenter(tableView);
    root.setBottom(bottomBar);
    root.setRight(toolbarWidget);
  }

  // ==============================================================================================
  // Public API - Data & Table
  // ==============================================================================================

  /**
   * Sets the columns of the table view.
   *
   * @param headers The column header names
   */
  public void setColumns(String[] headers) {
    Platform.runLater(
        () -> {
          tableView.getColumns().clear();
          if (headers == null) return;

          for (int col = 0; col < headers.length; col++) {
            final int colIndex = col;
            TableColumn<String[], String> column = new TableColumn<>(headers[col].trim());
            column.setCellValueFactory(
                cellData -> {
                  String[] row = cellData.getValue();
                  String value = (row != null && colIndex < row.length) ? row[colIndex] : "";
                  return new SimpleStringProperty(value);
                });
            // Distribute width evenly initially
            column
                .prefWidthProperty()
                .bind(tableView.widthProperty().divide(Math.max(1, headers.length)));
            tableView.getColumns().add(column);
          }
        });
  }

  /**
   * Updates the rows displayed in the table.
   *
   * @param rows The list of rows to display
   */
  public void setTableData(List<String[]> rows) {
    Platform.runLater(
        () -> {
          tableView.getItems().clear();
          if (rows != null) {
            tableView.getItems().setAll(rows);
          }
        });
  }

  public void clearTable() {
    Platform.runLater(
        () -> {
          tableView.getItems().clear();
          tableView.getColumns().clear();
        });
  }

  // ==============================================================================================
  // Public API - Pagination & Controls
  // ==============================================================================================

  public void setTotalRowsLabel(String text) {
    Platform.runLater(() -> totalRowsLabel.setText(text));
  }

  public void setRowsPerPageText(String text) {
    Platform.runLater(() -> rowsPerPageField.setText(text));
  }

  public TextField getRowsPerPageField() {
    return rowsPerPageField;
  }

  /**
   * Updates the pagination control state.
   *
   * @param pageCount Total number of pages
   * @param visible Whether pagination should be visible
   */
  public void updatePagination(int pageCount, boolean visible) {
    Platform.runLater(
        () -> {
          pagination.setPageCount(pageCount);
          pagination.setVisible(visible);
          pagination.setManaged(visible);
          // Reset to first page if needed, or maintain state?
          // Usually reset when data changes significantly.
          // Controller decides when to call setCurrentPageIndex.
        });
  }

  public void setCurrentPageIndex(int index) {
    Platform.runLater(() -> pagination.setCurrentPageIndex(index));
  }

  // ==============================================================================================
  // Public API - Toolbar & Actions
  // ==============================================================================================

  public void setToolbarActions(List<ButtonConfig> buttons) {
    toolbarWidget.setButtons(buttons);
  }
}
