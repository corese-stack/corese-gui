package fr.inria.corese.gui.feature.query.support;

import fr.inria.corese.gui.component.notification.NotificationWidget;
import fr.inria.corese.gui.core.model.QueryResultRef;

/**
 * Query execution helpers (classification and user feedback messages).
 */
public final class QueryExecutionSupport {

	private QueryExecutionSupport() {
		throw new AssertionError("Utility class");
	}

	public static boolean looksLikeReadQuery(String queryContent) {
		if (queryContent == null || queryContent.isBlank()) {
			return false;
		}
		String normalized = " " + queryContent.toLowerCase().replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
				+ " ";
		return normalized.contains(" select ") || normalized.contains(" ask ") || normalized.contains(" construct ")
				|| normalized.contains(" describe ");
	}

	public static void showAskOutcomeNotification(QueryResultRef resultRef) {
		Boolean askResult = resultRef == null ? null : resultRef.getAskResult();
		if (Boolean.TRUE.equals(askResult)) {
			NotificationWidget.getInstance().showSuccess("ASK", "True");
			return;
		}
		if (Boolean.FALSE.equals(askResult)) {
			NotificationWidget.getInstance().showError("ASK", "False");
			return;
		}
		NotificationWidget.getInstance().showWarning("ASK", "Result unavailable.");
	}

	public static void showUpdateSummaryNotification(QueryResultRef resultRef) {
		int inserted = resultRef == null ? 0 : resultRef.getInsertedTriples();
		int deleted = resultRef == null ? 0 : resultRef.getDeletedTriples();
		if (inserted > 0 && deleted > 0) {
			NotificationWidget.getInstance().showSuccess("Update",
					countLabel(inserted, "triple") + " inserted, " + countLabel(deleted, "triple") + " deleted.");
			return;
		}
		if (inserted > 0) {
			NotificationWidget.getInstance().showSuccess("Insert", countLabel(inserted, "triple") + " inserted.");
			return;
		}
		if (deleted > 0) {
			NotificationWidget.getInstance().showSuccess("Delete", countLabel(deleted, "triple") + " deleted.");
			return;
		}
		NotificationWidget.getInstance().showSuccess("Update", "No graph change detected.");
	}

	private static String countLabel(int count, String noun) {
		if (count == 1) {
			return "1 " + noun;
		}
		return count + " " + noun + "s";
	}
}
