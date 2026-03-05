package fr.inria.corese.gui.feature.result.table.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight parser for SPARQL TSV result payloads.
 *
 * <p>
 * SPARQL TSV values are RDF terms serialized as text, with tab-separated fields
 * and line-separated rows.
 */
public final class TsvTableParser {

	private TsvTableParser() {
	}

	/**
	 * Parses TSV content into rows of fields.
	 *
	 * @param tsvContent
	 *            TSV payload to parse.
	 * @return parsed rows, empty when input is null/empty.
	 */
	public static List<String[]> parse(String tsvContent) {
		if (tsvContent == null || tsvContent.isEmpty()) {
			return List.of();
		}

		List<String[]> rows = new ArrayList<>();
		List<String> currentRow = new ArrayList<>();
		StringBuilder currentField = new StringBuilder();
		boolean fieldStarted = false;

		for (int i = 0; i < tsvContent.length(); i++) {
			char current = tsvContent.charAt(i);

			// Ignore an optional UTF-8 BOM at start of content.
			if (i == 0 && current == '\ufeff') {
				continue;
			}

			if (current == '\t') {
				addField(currentRow, currentField);
				fieldStarted = false;
				continue;
			}

			if (current == '\n' || current == '\r') {
				addField(currentRow, currentField);
				addRow(rows, currentRow);
				fieldStarted = false;
				if (current == '\r' && i + 1 < tsvContent.length() && tsvContent.charAt(i + 1) == '\n') {
					i++;
				}
				continue;
			}

			currentField.append(current);
			fieldStarted = true;
		}

		boolean endsWithTab = !tsvContent.isEmpty() && tsvContent.charAt(tsvContent.length() - 1) == '\t';
		if (fieldStarted || !currentRow.isEmpty() || endsWithTab) {
			addField(currentRow, currentField);
			addRow(rows, currentRow);
		}

		return rows;
	}

	private static void addField(List<String> currentRow, StringBuilder currentField) {
		currentRow.add(currentField.toString());
		currentField.setLength(0);
	}

	private static void addRow(List<String[]> rows, List<String> currentRow) {
		rows.add(currentRow.toArray(String[]::new));
		currentRow.clear();
	}
}
