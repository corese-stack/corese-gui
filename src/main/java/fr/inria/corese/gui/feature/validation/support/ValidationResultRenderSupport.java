package fr.inria.corese.gui.feature.validation.support;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.enums.SerializationFormat;
import fr.inria.corese.gui.core.service.GraphProjectionService;
import fr.inria.corese.gui.feature.result.ResultController;
import fr.inria.corese.gui.feature.validation.ValidationModel;
import fr.inria.corese.gui.utils.AppExecutors;
import javafx.application.Platform;

/**
 * Asynchronous rendering helpers for validation result views.
 */
public final class ValidationResultRenderSupport {

	private ValidationResultRenderSupport() {
		throw new AssertionError("Utility class");
	}

	public static void bindOnFormatChanged(ResultController controller, ValidationModel model) {
		if (controller == null || model == null) {
			return;
		}
		controller.setOnFormatChanged(format -> AppExecutors.execute(() -> {
			NotificationWidget.LoadingHandle loadingHandle = NotificationWidget.getInstance().showLoading("Validation",
					"Formatting validation report...");
			try {
				String formattedReport = model.formatLastReport(format);
				if (formattedReport != null) {
					Platform.runLater(() -> controller.updateText(formattedReport));
				}
			} finally {
				loadingHandle.close();
			}
		}));
	}

	public static void loadGraphAndTextAsync(ResultController controller, ValidationModel model,
			SerializationFormat textFormat, Runnable onComplete) {
		if (controller == null || model == null || textFormat == null) {
			if (onComplete != null) {
				onComplete.run();
			}
			return;
		}

		AppExecutors.execute(() -> {
			try {
				String jsonLdReport = model.formatLastReport(SerializationFormat.JSON_LD);
				String sanitizedJsonLdReport = GraphProjectionService.getInstance()
						.sanitizeJsonLdForDisplay(jsonLdReport);
				String textReport = model.formatLastReport(textFormat);
				int tripleCount = model.getLastReportTripleCount();
				String graphPayload = normalizeJsonPayload(sanitizedJsonLdReport);
				Platform.runLater(() -> {
					controller.displayGraph(graphPayload, tripleCount);
					if (textReport != null) {
						controller.updateText(textReport);
					}
				});
			} finally {
				if (onComplete != null) {
					onComplete.run();
				}
			}
		});
	}

	private static String normalizeJsonPayload(String payload) {
		if (payload == null || payload.isBlank()) {
			return null;
		}
		String trimmed = payload.trim();
		if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
			return payload;
		}
		return null;
	}
}
