package fr.inria.corese.gui.feature.result.table;

import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.config.ButtonConfig;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.component.pagination.TablePaginationBar;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/**
 * View for displaying tabular SPARQL results with pagination.
 *
 * <p>This view provides:
 * <ul>
 *   <li>A TableView for displaying rows of data
 *   <li>A comprehensive pagination bar (Rows/page, Navigation, Total)
 *   <li>A dedicated sidebar for actions (Copy, Export)
 * </ul>
 */
public class TableResultView extends AbstractView {

    private final TableView<String[]> tableView;
    private final TablePaginationBar paginationBar;
    private final ToolbarWidget toolbarWidget;
    
    private int currentPageIndex = 0;
    private int rowsPerPage = 50;

    /**
     * Constructs a new TableResultView.
     *
     * @param pageChangeHandler Callback for when the page index changes
     */
    public TableResultView(Consumer<Integer> pageChangeHandler) {
        super(new BorderPane(), null); // Stylesheet handled by components

        this.tableView = new TableView<>();
        this.paginationBar = new TablePaginationBar(pageChangeHandler);
        this.toolbarWidget = new ToolbarWidget();

        setupLayout();
        setupListeners();
    }

    private void setupLayout() {
        BorderPane root = (BorderPane) getRoot();
        root.setCenter(tableView);
        root.setBottom(paginationBar);
        root.setRight(toolbarWidget);
        
        // Pagination is always visible to allow configuration (rows per page)
        paginationBar.setVisible(true);
        paginationBar.setManaged(true);
    }

    private void setupListeners() {
        paginationBar.rowsPerPageTextProperty().addListener((obs, oldVal, newVal) -> {
            try {
                this.rowsPerPage = Integer.parseInt(newVal);
            } catch (NumberFormatException e) {
                // Keep existing value
            }
        });
    }

    // ==============================================================================================
    // Public API - Data & Table
    // ==============================================================================================

    public void setColumns(String[] headers) {
        Platform.runLater(() -> {
            tableView.getColumns().clear();
            tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            
            // 1. Add Row Number Column (#)
            TableColumn<String[], String> indexColumn = new TableColumn<>("#");
            indexColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null) {
                        setText(null);
                    } else {
                        int absoluteIndex = (currentPageIndex * rowsPerPage) + getTableRow().getIndex() + 1;
                        setText(String.valueOf(absoluteIndex));
                    }
                }
            });
            // Fix index column width
            indexColumn.setPrefWidth(50);
            indexColumn.setMinWidth(50);
            indexColumn.setMaxWidth(50);
            indexColumn.setResizable(false);
            tableView.getColumns().add(indexColumn);

            if (headers == null) return;

            // 2. Add Data Columns
            for (int col = 0; col < headers.length; col++) {
                final int colIndex = col;
                TableColumn<String[], String> column = new TableColumn<>(headers[col].trim());
                column.setCellValueFactory(cellData -> {
                    String[] row = cellData.getValue();
                    String value = (row != null && colIndex < row.length) ? row[colIndex] : "";
                    return new SimpleStringProperty(value);
                });
                
                // Distribute width evenly among data columns, accounting for the index column and scrollbar margin
                column.prefWidthProperty().bind(
                    tableView.widthProperty()
                        .subtract(indexColumn.widthProperty())
                        .subtract(20) // Safety margin for vertical scrollbar
                        .divide(Math.max(1, headers.length))
                );
                
                tableView.getColumns().add(column);
            }
        });
    }

    public void setTableData(List<String[]> rows) {
        Platform.runLater(() -> {
            tableView.getItems().clear();
            if (rows != null) {
                tableView.getItems().setAll(rows);
            }
        });
    }

    public void clearTable() {
        Platform.runLater(() -> {
            tableView.getItems().clear();
            tableView.getColumns().clear();
        });
    }

    // ==============================================================================================
    // Public API - Pagination & Controls
    // ==============================================================================================

    public void setTotalRowsLabel(String text) {
        Platform.runLater(() -> paginationBar.setTotalRows(text));
    }

    public void setRowsPerPageText(String text) {
        try {
            this.rowsPerPage = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            // Keep existing value
        }
        Platform.runLater(() -> paginationBar.setRowsPerPage(text));
    }

    public StringProperty getRowsPerPageProperty() {
        return paginationBar.rowsPerPageTextProperty();
    }

    public void updatePagination(int pageCount) {
        Platform.runLater(() -> paginationBar.setPageCount(pageCount));
    }

    public void setCurrentPageIndex(int index) {
        this.currentPageIndex = index;
        Platform.runLater(() -> paginationBar.setCurrentPageIndex(index));
    }

    public void setToolbarActions(List<ButtonConfig> buttons) {
        toolbarWidget.setButtons(buttons);
    }
}
