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

    // ==============================================================================================
    // Public API - Data & Table
    // ==============================================================================================

    public void setColumns(String[] headers) {
        Platform.runLater(() -> {
            tableView.getColumns().clear();
            if (headers == null) return;

            for (int col = 0; col < headers.length; col++) {
                final int colIndex = col;
                TableColumn<String[], String> column = new TableColumn<>(headers[col].trim());
                column.setCellValueFactory(cellData -> {
                    String[] row = cellData.getValue();
                    String value = (row != null && colIndex < row.length) ? row[colIndex] : "";
                    return new SimpleStringProperty(value);
                });
                
                // Distribute width evenly initially
                column.prefWidthProperty().bind(tableView.widthProperty().divide(Math.max(1, headers.length)));
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
        Platform.runLater(() -> paginationBar.setRowsPerPage(text));
    }

    public StringProperty getRowsPerPageProperty() {
        return paginationBar.rowsPerPageTextProperty();
    }

    public void updatePagination(int pageCount) {
        Platform.runLater(() -> paginationBar.setPageCount(pageCount));
    }

    public void setCurrentPageIndex(int index) {
        Platform.runLater(() -> paginationBar.setCurrentPageIndex(index));
    }

    public void setToolbarActions(List<ButtonConfig> buttons) {
        toolbarWidget.setButtons(buttons);
    }
}
