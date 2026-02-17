package fr.inria.corese.gui.feature.result.table;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.button.factory.ButtonFactory;
import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.io.ExportHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 * Controller for the tabular result view.
 *
 * <p>
 * This class manages the interaction between the data model (SPARQL results in
 * CSV format) and the {@link TableResultView}. It handles:
 * <ul>
 * <li>Parsing raw result data (CSV) into a structured format.
 * <li>Pagination logic (calculating pages and slicing data).
 * <li>Handling user actions like "Copy" and "Export" via a provided format
 * service.
 * </ul>
 */
public class TableResultController {

	private static final int DEFAULT_ROWS_PER_PAGE = 50;
	private static final KeyCodeCombination COPY_COMBINATION = new KeyCodeCombination(KeyCode.C,
			KeyCombination.SHORTCUT_DOWN);

	private final TableResultView view;
	private final List<String[]> allRows;
	private String[] headers = new String[0];
	private int rowsPerPage;
	private TablePosition<String[], ?> dragAnchor;

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
		view.setToolbarActions(List.of(ButtonFactory.copySelection(this::copySelection),
				ButtonFactory.custom(ButtonIcon.COPY, "Copy Table (TSV)", this::copyAll),
				ButtonFactory.export(this::exportContent)));
		view.setToolbarButtonDisabled(ButtonIcon.COPY_SELECTION, true);

		// Setup Rows Per Page Listener
		view.setRowsPerPageText(String.valueOf(DEFAULT_ROWS_PER_PAGE));
		view.getRowsPerPageProperty().addListener((obs, oldVal, newVal) -> handleRowsPerPageChange(newVal));

		TableView<String[]> tableView = view.getTableView();
		tableView.setOnKeyPressed(event -> {
			if (COPY_COMBINATION.match(event)) {
				copySelectionOrAll();
				event.consume();
			} else if (event.getCode() == KeyCode.ESCAPE) {
				tableView.getSelectionModel().clearSelection();
				updateCopySelectionState();
				event.consume();
			}
		});

		ListChangeListener<TablePosition<String[], ?>> selectionListener = change -> updateCopySelectionState();
		getSelectedCells(tableView).addListener(selectionListener);

		tableView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> handleMousePressed(event, tableView));
		tableView.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> handleMouseDragged(event, tableView));
		tableView.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> dragAnchor = null);
	}

	/**
	 * Sets the provider for formatting results. Used for Copy and Export actions to
	 * retrieve the full result in desired formats.
	 *
	 * @param formatProvider
	 *            A function that takes a format and returns the result string.
	 */
	public void setFormatProvider(Function<SerializationFormat, String> formatProvider) {
		this.formatProvider = formatProvider;
	}

	// ==============================================================================================
	// Logic - Data Processing
	// ==============================================================================================

	/**
	 * Updates the table with new SPARQL CSV results. Can be called from any thread.
	 *
	 * @param csvResult
	 *            The raw CSV string result from the SPARQL query.
	 */
	public void updateTable(String csvResult) {
		Platform.runLater(() -> parseAndPopulate(csvResult));
	}

	private void parseAndPopulate(String csvResult) {
		allRows.clear();
		view.clearTable();
		headers = new String[0];

		if (csvResult == null || csvResult.isEmpty()) {
			updatePagination();
			return;
		}

		// Split by line (handling both \r\n and \n)
		String[] lines = csvResult.split("\\r?\\n");
		if (lines.length == 0) {
			updatePagination();
			return;
		}

		// First line is headers
		headers = lines[0].split(",", -1);
		view.setColumns(headers);

		// Subsequent lines are data
		for (int i = 1; i < lines.length; i++) {
			allRows.add(lines[i].split(",", -1));
		}

		updatePagination();
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
				updatePagination();
			}
		} catch (NumberFormatException _) {
			// Ignore invalid input
		}
	}

	private void updatePagination() {
		view.setTotalRowsLabel("Total rows: " + allRows.size());

		if (allRows.isEmpty()) {
			view.updatePagination(1);
			view.setCurrentPageIndex(0);
			view.setTableData(Collections.emptyList());
			return;
		}

		int pageCount = (int) Math.ceil((double) allRows.size() / rowsPerPage);
		view.updatePagination(pageCount);

		view.setCurrentPageIndex(0);
		updateTableForPage(0);
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

	private void copySelectionOrAll() {
		if (hasSelection()) {
			copySelection();
			return;
		}
		copyAll();
	}

	private void copySelection() {
		if (allRows.isEmpty()) {
			return;
		}

		int selectedCells = getSelectedCells(view.getTableView()).size();
		String content = buildTsvFromSelection();
		if (content == null || content.isBlank()) {
			NotificationWidget.getInstance().showWarning("No selection to copy.");
			return;
		}

		writeClipboard(content);
		NotificationWidget.getInstance()
				.showSuccess("Copied " + countLabel(selectedCells, "selected cell") + " to clipboard (TSV).");
	}

	private void copyAll() {
		if (allRows.isEmpty()) {
			return;
		}

		String content = buildTsvFromAllRows();
		if (content == null || content.isBlank()) {
			NotificationWidget.getInstance().showWarning("No table data available to copy.");
			return;
		}

		writeClipboard(content);
		NotificationWidget.getInstance()
				.showSuccess("Copied table to clipboard (TSV, " + countLabel(allRows.size(), "row") + ").");
	}

	private void exportContent() {
		if (allRows.isEmpty()) {
			return;
		}

		if (formatProvider == null) {
			NotificationWidget.getInstance().showError("Export failed: no data provider available.");
			return;
		}

		ExportHelper.exportResult(view.getRoot().getScene().getWindow(),
				List.of(SerializationFormat.CSV, SerializationFormat.TSV, SerializationFormat.MARKDOWN),
				formatProvider);
	}

	public boolean exportFromShortcut() {
		exportContent();
		return true;
	}

	private String buildTsvFromSelection() {
		TableView<String[]> tableView = view.getTableView();
		List<TablePosition<String[], ?>> positions = getSelectedCells(tableView);
		if (positions == null || positions.isEmpty()) {
			return null;
		}

		List<TableColumn<String[], ?>> columns = tableView.getVisibleLeafColumns();
		Set<Integer> rows = new TreeSet<>();
		Set<Integer> cols = new TreeSet<>();
		Map<Integer, Map<Integer, String>> values = new HashMap<>();

		for (TablePosition<String[], ?> position : positions) {
			int colIndex = position.getColumn();
			if (colIndex == 0) {
				continue;
			}
			int rowIndex = position.getRow();
			rows.add(rowIndex);
			cols.add(colIndex);
			String value = getCellValue(columns, rowIndex, colIndex);
			values.computeIfAbsent(rowIndex, key -> new HashMap<>()).put(colIndex, value);
		}

		if (rows.isEmpty() || cols.isEmpty()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int rowIndex : rows) {
			boolean first = true;
			for (int colIndex : cols) {
				if (!first) {
					sb.append('\t');
				}
				first = false;
				String value = values.getOrDefault(rowIndex, Map.of()).getOrDefault(colIndex, "");
				sb.append(escapeTsv(value));
			}
			sb.append('\n');
		}

		return sb.toString();
	}

	private String buildTsvFromAllRows() {
		if (allRows.isEmpty()) {
			return null;
		}

		TableView<String[]> tableView = view.getTableView();
		ColumnSelection selection = resolveColumnSelection(tableView.getVisibleLeafColumns());
		if (selection == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		appendTsvHeaders(sb, selection);
		appendTsvRows(sb, selection);

		return sb.toString();
	}

	private ColumnSelection resolveColumnSelection(List<TableColumn<String[], ?>> columns) {
		List<Integer> columnIndices = new ArrayList<>();
		List<String> columnLabels = new ArrayList<>();

		for (int colIndex = 1; colIndex < columns.size(); colIndex++) {
			TableColumn<String[], ?> column = columns.get(colIndex);
			Integer dataIndex = extractDataIndex(column);
			if (dataIndex == null) {
				continue;
			}
			columnIndices.add(dataIndex);
			columnLabels.add(column.getText());
		}

		if (columnIndices.isEmpty()) {
			appendAllHeaders(columnIndices, columnLabels);
		}

		return columnIndices.isEmpty() ? null : new ColumnSelection(columnIndices, columnLabels);
	}

	private void appendAllHeaders(List<Integer> columnIndices, List<String> columnLabels) {
		for (int colIndex = 0; colIndex < headers.length; colIndex++) {
			columnIndices.add(colIndex);
			columnLabels.add(headers[colIndex]);
		}
	}

	private void appendTsvHeaders(StringBuilder sb, ColumnSelection selection) {
		List<Integer> columnIndices = selection.indices();
		List<String> columnLabels = selection.labels();
		for (int colIndex = 0; colIndex < columnIndices.size(); colIndex++) {
			if (colIndex > 0) {
				sb.append('\t');
			}
			String header = colIndex < columnLabels.size() ? columnLabels.get(colIndex) : "";
			sb.append(escapeTsv(header));
		}
		sb.append('\n');
	}

	private void appendTsvRows(StringBuilder sb, ColumnSelection selection) {
		List<Integer> columnIndices = selection.indices();
		for (String[] row : allRows) {
			for (int colIndex = 0; colIndex < columnIndices.size(); colIndex++) {
				if (colIndex > 0) {
					sb.append('\t');
				}
				int dataIndex = columnIndices.get(colIndex);
				String value = (row != null && dataIndex < row.length) ? row[dataIndex] : "";
				sb.append(escapeTsv(value));
			}
			sb.append('\n');
		}
	}

	private Integer extractDataIndex(TableColumn<String[], ?> column) {
		Object userData = column.getUserData();
		if (userData instanceof Integer index) {
			return index;
		}
		String header = column.getText();
		for (int i = 0; i < headers.length; i++) {
			if (Objects.equals(headers[i], header)) {
				return i;
			}
		}
		return null;
	}

	private static String getCellValue(List<TableColumn<String[], ?>> columns, int rowIndex, int colIndex) {
		if (colIndex < 0 || colIndex >= columns.size()) {
			return "";
		}
		Object value = columns.get(colIndex).getCellData(rowIndex);
		return value != null ? value.toString() : "";
	}

	private static String escapeTsv(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		boolean needsQuotes = value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
				|| value.indexOf('"') >= 0;
		if (!needsQuotes) {
			return value;
		}
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}

	private void writeClipboard(String content) {
		ClipboardContent clipboardContent = new ClipboardContent();
		clipboardContent.putString(content);
		Clipboard.getSystemClipboard().setContent(clipboardContent);
	}

	private boolean hasSelection() {
		TableView<String[]> tableView = view.getTableView();
		List<TablePosition<String[], ?>> positions = getSelectedCells(tableView);
		return positions != null && !positions.isEmpty();
	}

	private void updateCopySelectionState() {
		view.setToolbarButtonDisabled(ButtonIcon.COPY_SELECTION, !hasSelection());
	}

	private void handleMousePressed(MouseEvent event, TableView<String[]> tableView) {
		if (event.getButton() != MouseButton.PRIMARY) {
			return;
		}

		TableCell<String[], ?> cell = findCell(event.getTarget());
		if (cell == null || cell.isEmpty()) {
			tableView.getSelectionModel().clearSelection();
			updateCopySelectionState();
			return;
		}

		if (!event.isShiftDown() && !event.isShortcutDown()) {
			tableView.getSelectionModel().clearSelection();
			tableView.getSelectionModel().select(cell.getIndex(), cell.getTableColumn());
		}
		dragAnchor = new TablePosition<>(tableView, cell.getIndex(), cell.getTableColumn());
		updateCopySelectionState();
	}

	private void handleMouseDragged(MouseEvent event, TableView<String[]> tableView) {
		if (dragAnchor == null || !event.isPrimaryButtonDown()) {
			return;
		}

		TableCell<String[], ?> cell = findCell(event.getTarget());
		if (cell == null || cell.isEmpty()) {
			return;
		}

		List<TableColumn<String[], ?>> columns = tableView.getVisibleLeafColumns();
		int anchorColIndex = columns.indexOf(dragAnchor.getTableColumn());
		int currentColIndex = columns.indexOf(cell.getTableColumn());
		if (anchorColIndex < 1 || currentColIndex < 1) {
			return;
		}

		int startRow = Math.min(dragAnchor.getRow(), cell.getIndex());
		int endRow = Math.max(dragAnchor.getRow(), cell.getIndex());
		int startCol = Math.min(anchorColIndex, currentColIndex);
		int endCol = Math.max(anchorColIndex, currentColIndex);

		tableView.getSelectionModel().clearSelection();
		tableView.getSelectionModel().selectRange(startRow, columns.get(startCol), endRow, columns.get(endCol));
		updateCopySelectionState();
		event.consume();
	}

	@SuppressWarnings("unchecked")
	private TableCell<String[], ?> findCell(Object target) {
		if (!(target instanceof Node node)) {
			return null;
		}
		while (node != null && !(node instanceof TableCell)) {
			node = node.getParent();
		}
		if (node instanceof TableCell<?, ?> cell && cell.getTableView() == view.getTableView()) {
			return (TableCell<String[], ?>) cell;
		}
		return null;
	}

	private record ColumnSelection(List<Integer> indices, List<String> labels) {
	}

	private static String countLabel(int count, String noun) {
		if (count == 1) {
			return "1 " + noun;
		}
		return count + " " + noun + "s";
	}

	@SuppressWarnings({"unchecked", "java:S3740"})
	private static ObservableList<TablePosition<String[], ?>> getSelectedCells(TableView<String[]> tableView) {
		return (ObservableList<TablePosition<String[], ?>>) (ObservableList<?>) tableView.getSelectionModel()
				.getSelectedCells();
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
			updatePagination();
		});
	}

	public int getRowCount() {
		return allRows.size();
	}
}
