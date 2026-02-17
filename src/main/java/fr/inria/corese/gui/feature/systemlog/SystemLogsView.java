package fr.inria.corese.gui.feature.systemlog;

import fr.inria.corese.gui.component.button.enums.ButtonIcon;
import fr.inria.corese.gui.component.emptystate.EmptyStateWidget;
import fr.inria.corese.gui.core.theme.CssUtils;
import fr.inria.corese.gui.core.view.AbstractView;
import fr.inria.corese.gui.utils.fx.RoundedClipSupport;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * View displaying the system activity timeline for graph-changing operations.
 */
public final class SystemLogsView extends AbstractView {

	@SuppressWarnings("java:S1075")
	private static final String COMMON_STYLESHEET_PATH = "/css/common/common.css";
	@SuppressWarnings("java:S1075")
	private static final String STYLESHEET_PATH = "/css/features/system-logs-view.css";
	private static final double CARD_RADIUS = 8.0;

	private final TableView<SystemLogTableRow> logTableView = new TableView<>();
	private final ObservableList<SystemLogTableRow> logItems = FXCollections.observableArrayList();
	private final EmptyStateWidget emptyStateWidget = new EmptyStateWidget(ButtonIcon.NOTIFICATION_INFO,
			"No graph activity yet", "Operations that modify the RDF graph will appear here.");

	public SystemLogsView() {
		super(new BorderPane(), STYLESHEET_PATH);
		CssUtils.applyViewStyles(getRoot(), COMMON_STYLESHEET_PATH);
		initializeLayout();
	}

	private void initializeLayout() {
		BorderPane root = (BorderPane) getRoot();
		root.getStyleClass().addAll("system-logs-root", "app-workspace-root");

		VBox workspaceCard = new VBox();
		workspaceCard.getStyleClass().addAll("system-logs-card", "app-workspace-card", "app-card", "app-card-default");
		RoundedClipSupport.applyRoundedClip(workspaceCard, CARD_RADIUS);

		Label titleLabel = new Label("System Logs");
		titleLabel.getStyleClass().add("system-logs-title");
		Label subtitleLabel = new Label("Timeline of operations that modified the RDF graph.");
		subtitleLabel.getStyleClass().add("system-logs-subtitle");
		VBox headerBox = new VBox(2, titleLabel, subtitleLabel);
		headerBox.getStyleClass().add("system-logs-header");

		configureTable();
		VBox.setVgrow(logTableView, Priority.ALWAYS);

		emptyStateWidget.getStyleClass().add("system-logs-empty-state");
		emptyStateWidget.setVisible(true);
		emptyStateWidget.setManaged(true);
		emptyStateWidget.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		StackPane tableContainer = new StackPane(logTableView, emptyStateWidget);
		tableContainer.getStyleClass().add("system-logs-table-container");
		VBox.setVgrow(tableContainer, Priority.ALWAYS);

		workspaceCard.getChildren().setAll(headerBox, tableContainer);
		root.setCenter(workspaceCard);
	}

	private void configureTable() {
		logTableView.getStyleClass().add("system-logs-table");
		logTableView.setItems(logItems);
		logTableView.setFocusTraversable(false);
		logTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		logTableView.setPlaceholder(new Label(""));
		List<TableColumn<SystemLogTableRow, ?>> columns = List.of(createTimeColumn(), createTypeColumn(),
				createActionColumn(), createDetailsColumn(), createDiffColumn(), createStateColumn());
		logTableView.getColumns().setAll(columns);
		logTableView.getSortOrder().setAll(java.util.Collections.singletonList(columns.get(0)));
	}

	private TableColumn<SystemLogTableRow, Number> createTimeColumn() {
		TableColumn<SystemLogTableRow, Number> timeColumn = new TableColumn<>("Time");
		timeColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().timestampMillis()));
		timeColumn.setCellFactory(ignored -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || getTableRow() == null || getTableRow().getItem() == null) {
					setText(null);
					return;
				}
				setText(getTableRow().getItem().time());
			}
		});
		timeColumn.setComparator((left, right) -> Long.compare(left.longValue(), right.longValue()));
		timeColumn.setSortType(TableColumn.SortType.DESCENDING);
		timeColumn.setPrefWidth(170);
		return timeColumn;
	}

	private static TableColumn<SystemLogTableRow, String> createTypeColumn() {
		TableColumn<SystemLogTableRow, String> typeColumn = new TableColumn<>("Type");
		typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().type()));
		typeColumn.setPrefWidth(120);
		return typeColumn;
	}

	private static TableColumn<SystemLogTableRow, String> createActionColumn() {
		TableColumn<SystemLogTableRow, String> actionColumn = new TableColumn<>("Action");
		actionColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().action()));
		actionColumn.setPrefWidth(210);
		return actionColumn;
	}

	private static TableColumn<SystemLogTableRow, String> createDetailsColumn() {
		TableColumn<SystemLogTableRow, String> detailsColumn = new TableColumn<>("Details");
		detailsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().details()));
		detailsColumn.setCellFactory(ignored -> new TableCell<>() {
			private final Tooltip tooltip = new Tooltip();

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null || item.isBlank()) {
					setText(null);
					setTooltip(null);
					return;
				}
				setText(item);
				tooltip.setText(item);
				setTooltip(tooltip);
			}
		});
		detailsColumn.setPrefWidth(430);
		return detailsColumn;
	}

	private static TableColumn<SystemLogTableRow, Number> createDiffColumn() {
		TableColumn<SystemLogTableRow, Number> diffColumn = new TableColumn<>("Diff");
		diffColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().netDelta()));
		diffColumn.setCellFactory(ignored -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || getTableRow() == null || getTableRow().getItem() == null) {
					setText(null);
					getStyleClass().removeAll("system-log-delta-positive", "system-log-delta-negative",
							"system-log-delta-mixed", "system-log-delta-neutral");
					return;
				}
				SystemLogTableRow row = getTableRow().getItem();
				setText(row.diffLabel());
				getStyleClass().removeAll("system-log-delta-positive", "system-log-delta-negative",
						"system-log-delta-mixed", "system-log-delta-neutral");
				if (row.insertedCount() > 0 && row.deletedCount() > 0) {
					getStyleClass().add("system-log-delta-mixed");
				} else if (row.insertedCount() > 0) {
					getStyleClass().add("system-log-delta-positive");
				} else if (row.deletedCount() > 0) {
					getStyleClass().add("system-log-delta-negative");
				} else {
					getStyleClass().add("system-log-delta-neutral");
				}
			}
		});
		diffColumn.setPrefWidth(110);
		return diffColumn;
	}

	private static TableColumn<SystemLogTableRow, Number> createStateColumn() {
		TableColumn<SystemLogTableRow, Number> stateColumn = new TableColumn<>("Graph State");
		stateColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().totalTripleCount()));
		stateColumn.setCellFactory(ignored -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || getTableRow() == null || getTableRow().getItem() == null) {
					setText(null);
					return;
				}
				setText(getTableRow().getItem().stateLabel());
			}
		});
		stateColumn.setComparator((left, right) -> Long.compare(left.longValue(), right.longValue()));
		stateColumn.setPrefWidth(260);
		return stateColumn;
	}

	public TableView<SystemLogTableRow> getLogTableView() {
		return logTableView;
	}

	public void setLogEntries(List<SystemLogTableRow> entries) {
		List<SystemLogTableRow> safeEntries = entries == null ? List.of() : entries;
		logItems.setAll(safeEntries);
		boolean empty = safeEntries.isEmpty();
		emptyStateWidget.setManaged(empty);
		emptyStateWidget.setVisible(empty);
		logTableView.setVisible(!empty);
		logTableView.setManaged(!empty);
	}
}
