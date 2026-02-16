package fr.inria.corese.gui.feature.systemlog;

import fr.inria.corese.gui.core.service.GraphActivityLogEntry;
import fr.inria.corese.gui.core.service.GraphActivityLogService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;

/**
 * Controller for {@link SystemLogsView}.
 */
public final class SystemLogsController {

	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private final SystemLogsView view;
	private final GraphActivityLogService logService;

	public SystemLogsController(SystemLogsView view) {
		this.view = view;
		this.logService = GraphActivityLogService.getInstance();
		initialize();
	}

	private void initialize() {
		subscribeToActivityLogs();
	}

	private void subscribeToActivityLogs() {
		logService.subscribe(entries -> {
			if (Platform.isFxApplicationThread()) {
				applyEntries(entries);
				return;
			}
			Platform.runLater(() -> applyEntries(entries));
		});
	}

	private void applyEntries(List<GraphActivityLogEntry> entries) {
		List<GraphActivityLogEntry> safeEntries = entries == null ? List.of() : entries;
		List<SystemLogTableRow> rows = safeEntries.stream().map(entry -> SystemLogTableRow.from(entry,
				formatTimestamp(entry.timestampMillis()), formatSource(entry.source()))).toList();
		view.setLogEntries(rows);
	}

	private static String formatTimestamp(long timestampMillis) {
		return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(Math.max(1L, timestampMillis)));
	}

	private static String formatSource(GraphActivityLogEntry.Source source) {
		return switch (source) {
			case DATA_WORKSPACE -> "Data";
			case QUERY_SERVICE -> "Query";
			case REASONING_SERVICE -> "Reasoning";
			case RULE_FILE_SERVICE -> "Rule Files";
			case SYSTEM -> "System";
		};
	}
}
