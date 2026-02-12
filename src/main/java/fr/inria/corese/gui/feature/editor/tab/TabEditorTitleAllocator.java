package fr.inria.corese.gui.feature.editor.tab;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Allocates unique tab titles for editor tabs.
 */
final class TabEditorTitleAllocator {

	private final String defaultRawTitle;
	private final String defaultVisibleTitlePrefix;
	private int untitledCounter;

	TabEditorTitleAllocator(String defaultRawTitle, String defaultVisibleTitlePrefix) {
		this.defaultRawTitle = defaultRawTitle;
		this.defaultVisibleTitlePrefix = defaultVisibleTitlePrefix;
	}

	String resolveTitle(String requestedTitle, Collection<String> existingTitles) {
		Set<String> safeTitles = normalizeTitles(existingTitles);
		if (defaultRawTitle.equals(requestedTitle)) {
			return nextUntitledTitle(safeTitles);
		}
		return ensureUniqueTitle(requestedTitle, safeTitles);
	}

	private String nextUntitledTitle(Set<String> existingTitles) {
		int next = untitledCounter + 1;
		String candidate = defaultVisibleTitlePrefix + " " + next;
		while (existingTitles.contains(candidate)) {
			next++;
			candidate = defaultVisibleTitlePrefix + " " + next;
		}
		untitledCounter = next;
		return candidate;
	}

	private String ensureUniqueTitle(String baseTitle, Set<String> existingTitles) {
		if (baseTitle == null || baseTitle.isBlank()) {
			return nextUntitledTitle(existingTitles);
		}
		if (!existingTitles.contains(baseTitle)) {
			return baseTitle;
		}
		int suffix = 2;
		String candidate = baseTitle + " (" + suffix + ")";
		while (existingTitles.contains(candidate)) {
			suffix++;
			candidate = baseTitle + " (" + suffix + ")";
		}
		return candidate;
	}

	private static Set<String> normalizeTitles(Collection<String> existingTitles) {
		Set<String> titles = new HashSet<>();
		if (existingTitles == null) {
			return titles;
		}
		for (String title : existingTitles) {
			if (title != null && !title.isBlank()) {
				titles.add(title);
			}
		}
		return titles;
	}
}
