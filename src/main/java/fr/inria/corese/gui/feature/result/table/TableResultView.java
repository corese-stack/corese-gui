package fr.inria.corese.gui.feature.result.table;

import java.util.List;
import java.util.function.Consumer;

import fr.inria.corese.gui.component.button.config.ButtonConfig;
import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.pagination.TablePaginationWidget;
import fr.inria.corese.gui.component.toolbar.ToolbarWidget;
import fr.inria.corese.gui.core.view.AbstractView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/**
 * View for displaying tabular SPARQL results with pagination.
 *
 * <p>
 * This view comprises:
 * <ul>
 * <li>A {@link TableView} for displaying data rows.
 * <li>A {@link TablePaginationWidget} for navigation and configuration.
 * <li>A {@link ToolbarWidget} for actions.
 * </ul>
 *
 * <p>
 * Note: Methods in this class assume they are called on the JavaFX Application
 * Thread.
 */
public class TableResultView extends AbstractView {

	private static final int INDEX_COLUMN_WIDTH = 50;
	private static final int SCROLLBAR_MARGIN = 20;

	private final TableView<String[]> tableView;
	private final TablePaginationWidget paginationBar;
	private final ToolbarWidget toolbarWidget;

	/**
	 * Constructs a new TableResultView.
	 *
	 * @param pageChangeHandler
	 *            Callback for when the page index changes via the pagination bar.
	 */
	public TableResultView(Consumer<Integer> pageChangeHandler) {
		super(new BorderPane(), null);

		this.tableView = new TableView<>();
		this.paginationBar = new TablePaginationWidget(pageChangeHandler);
		this.toolbarWidget = new ToolbarWidget();

		setupLayout();
	}

	private void setupLayout() {
		BorderPane root = (BorderPane) getRoot();
		root.setCenter(tableView);
		root.setBottom(paginationBar);
		root.setRight(toolbarWidget);

		tableView.getSelectionModel().setCellSelectionEnabled(true);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// Ensure pagination is visible
		paginationBar.setVisible(true);
		paginationBar.setManaged(true);
	}

	// ==============================================================================================
	// Public API - Data & Table
	// ==============================================================================================

	/**
	 * Configures the table columns based on the provided headers. Automatically
	 * adds a row index column as the first column.
	 *
	 * @param headers
	 *            The column headers.
	 */
	public void setColumns(String[] headers) {
		tableView.getColumns().clear();
		tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

		// 1. Add Row Number Column (#)
		createIndexColumn();

		if (headers == null)
			return;

		// 2. Add Data Columns
		double totalWidthFactor = Math.max(1, headers.length);

		for (int col = 0; col < headers.length; col++) {
			final int colIndex = col;
			TableColumn<String[], String> column = new TableColumn<>(headers[col].trim());
			column.setUserData(colIndex);

			column.setCellValueFactory(cellData -> {
				String[] row = cellData.getValue();
				String value = (row != null && colIndex < row.length) ? row[colIndex] : "";
				return new SimpleStringProperty(value);
			});

			// Distribute width evenly among data columns
			// Formula: (TableWidth - IndexColumnWidth - ScrollbarMargin) / NumberOfColumns
			column.prefWidthProperty().bind(tableView.widthProperty().subtract(INDEX_COLUMN_WIDTH)
					.subtract(SCROLLBAR_MARGIN).divide(totalWidthFactor));

			tableView.getColumns().add(column);
		}
	}

	private void createIndexColumn() {
		TableColumn<String[], String> indexColumn = new TableColumn<>("#");
		indexColumn.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || getTableRow() == null) {
					setText(null);
				} else {
					int absoluteIndex = (paginationBar.getCurrentPageIndex() * paginationBar.getRowsPerPage())
							+ getTableRow().getIndex() + 1;
					setText(String.valueOf(absoluteIndex));
				}
			}
		});

		indexColumn.setPrefWidth(INDEX_COLUMN_WIDTH);
		indexColumn.setMinWidth(INDEX_COLUMN_WIDTH);
		indexColumn.setMaxWidth(INDEX_COLUMN_WIDTH);
		indexColumn.setResizable(false);
		indexColumn.setReorderable(false);
		indexColumn.setSortable(false);
		tableView.getColumns().add(indexColumn);
	}

	/**
	 * Updates the data displayed in the table.
	 *
	 * @param rows
	 *            The list of rows to display.
	 */
	public void setTableData(List<String[]> rows) {
		tableView.getItems().clear();
		if (rows != null) {
			tableView.getItems().setAll(rows);
		}
	}

	/**
	 * Clears all data and columns from the table.
	 */
	public void clearTable() {
		tableView.getItems().clear();
		tableView.getColumns().clear();
	}

	// ==============================================================================================
	// Public API - Pagination & Controls
	// ==============================================================================================

	public void setTotalRowsLabel(String text) {
		paginationBar.setTotalRows(text);
	}

	public void setRowsPerPageText(String text) {
		paginationBar.setRowsPerPage(text);
		tableView.refresh();
	}

	public StringProperty getRowsPerPageProperty() {
		return paginationBar.rowsPerPageTextProperty();
	}

	public void updatePagination(int pageCount) {
		paginationBar.setPageCount(pageCount);
	}

	public void setCurrentPageIndex(int index) {
		paginationBar.setCurrentPageIndex(index);
		tableView.refresh();
	}

	public void setToolbarActions(List<ButtonConfig> buttons) {
		toolbarWidget.setButtons(buttons);
	}

	public TableView<String[]> getTableView() {
		return tableView;
	}

	public void setToolbarButtonDisabled(ButtonIcon type, boolean disabled) {
		toolbarWidget.setButtonDisabled(type, disabled);
	}
}
