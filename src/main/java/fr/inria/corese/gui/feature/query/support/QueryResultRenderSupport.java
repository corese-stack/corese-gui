package fr.inria.corese.gui.feature.query.support;

import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.QueryService;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.utils.AppExecutors;
import javafx.application.Platform;

/**
 * Asynchronous rendering helpers for query result views.
 */
public final class QueryResultRenderSupport {

	private QueryResultRenderSupport() {
		throw new AssertionError("Utility class");
	}

	public static void bindOnFormatChanged(ResultController controller, String resultId, QueryService queryService) {
		if (controller == null || resultId == null || queryService == null) {
			return;
		}
		controller.setOnFormatChanged(format -> AppExecutors.execute(() -> {
			String formattedResult = queryService.formatResult(resultId, format);
			Platform.runLater(() -> controller.updateText(formattedResult));
		}));
	}

	public static void loadTableAndTextAsync(ResultController controller, String resultId,
			SerializationFormat textFormat, QueryService queryService) {
		if (controller == null || resultId == null || textFormat == null || queryService == null) {
			return;
		}
		AppExecutors.execute(() -> {
			String csvResult = queryService.formatResult(resultId, SerializationFormat.CSV);
			String textResult = queryService.formatResult(resultId, textFormat);
			Platform.runLater(() -> {
				controller.updateTableView(csvResult);
				controller.updateText(textResult);
			});
		});
	}

	public static void loadGraphAndTextAsync(ResultController controller, String resultId,
			SerializationFormat textFormat, QueryService queryService) {
		if (controller == null || resultId == null || textFormat == null || queryService == null) {
			return;
		}
		AppExecutors.execute(() -> {
			String jsonLdResult = queryService.formatResult(resultId, SerializationFormat.JSON_LD);
			String textResult = queryService.formatResult(resultId, textFormat);
			Platform.runLater(() -> {
				controller.displayGraph(jsonLdResult);
				controller.updateText(textResult);
			});
		});
	}
}
