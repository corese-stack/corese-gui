package fr.inria.corese.gui.feature.data;

import fr.inria.corese.gui.core.io.FileTypeSupport;
import java.util.Collection;
import java.util.List;

/**
 * Shared message-formatting helpers for Data page user feedback.
 */
final class DataUiMessageUtils {

	private DataUiMessageUtils() {
		throw new AssertionError("Utility class");
	}

	static String countLabel(int count, String noun) {
		if (count == 1) {
			return "1 " + noun;
		}
		return count + " " + noun + "s";
	}

	static String buildExpectedExtensionsHint(Collection<String> allowedExtensions) {
		List<String> normalizedExtensions = FileTypeSupport.normalizeExtensions(allowedExtensions);
		if (normalizedExtensions.isEmpty()) {
			return "Expected extensions are configured by this action.";
		}
		return "Expected extensions: " + String.join(", ", normalizedExtensions);
	}
}
