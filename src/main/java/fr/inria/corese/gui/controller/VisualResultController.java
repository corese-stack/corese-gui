package fr.inria.corese.gui.controller;

import fr.inria.corese.gui.model.ValidationReportItem;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller for visual SHACL validation report display.
 *
 * <p>This controller manages:
 *
 * <ul>
 *   <li>TableView for displaying SHACL validation results
 *   <li>Columns for severity, focus node, result path, value, and message
 *   <li>Styled presentation of validation issues
 * </ul>
 *
 * <p><b>Usage example:</b>
 *
 * <pre>{@code
 * VisualResultController controller = new VisualResultController();
 * controller.displayReport(validationItems);
 * Node view = controller.getView();
 * }</pre>
 */
public class VisualResultController {

  // ==============================================================================================
  // Fields
  // ==============================================================================================

  /** Table view for displaying validation report items. */
  private final TableView<ValidationReportItem> tableView;

  // ==============================================================================================
  // Constructor
  // ==============================================================================================

  /** Constructs a new VisualResultController with configured table columns. */
  public VisualResultController() {
    this.tableView = new TableView<>();
    initialize();
  }

  // ==============================================================================================
  // Initialization
  // ==============================================================================================

  /** Initializes table columns for SHACL validation reports. */
  private void initialize() {
    // Severity column
    TableColumn<ValidationReportItem, String> severityCol = new TableColumn<>("Severity");
    severityCol.setCellValueFactory(new PropertyValueFactory<>("severity"));

    // Focus Node column
    TableColumn<ValidationReportItem, String> focusNodeCol = new TableColumn<>("Focus Node");
    focusNodeCol.setCellValueFactory(new PropertyValueFactory<>("focusNode"));

    // Result Path column
    TableColumn<ValidationReportItem, String> resultPathCol = new TableColumn<>("Result Path");
    resultPathCol.setCellValueFactory(new PropertyValueFactory<>("resultPath"));

    // Value column
    TableColumn<ValidationReportItem, String> valueCol = new TableColumn<>("Value");
    valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));

    // Message column
    TableColumn<ValidationReportItem, String> messageCol = new TableColumn<>("Message");
    messageCol.setCellValueFactory(new PropertyValueFactory<>("message"));

    tableView.getColumns().addAll(List.of(severityCol, focusNodeCol, resultPathCol, valueCol, messageCol));
    tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
  }

  // ==============================================================================================
  // Public API
  // ==============================================================================================

  /**
   * Displays SHACL validation report items in the table.
   *
   * <p>This method replaces all existing items with the provided list. Each item represents a
   * validation violation or warning.
   *
   * @param items List of validation report items to display (null-safe)
   */
  public void displayReport(List<ValidationReportItem> items) {
    Platform.runLater(
        () -> {
          tableView.getItems().clear();
          if (items != null) {
            tableView.getItems().addAll(items);
          }
        });
  }

  /**
   * Clears all report items from the table.
   */
  public void clear() {
    Platform.runLater(() -> tableView.getItems().clear());
  }

  /**
   * Returns the root view node.
   *
   * @return The TableView containing validation results
   */
  public Node getView() {
    return tableView;
  }

  /**
   * Returns the number of validation items currently displayed.
   *
   * @return The item count
   */
  public int getItemCount() {
    return tableView.getItems().size();
  }
}
